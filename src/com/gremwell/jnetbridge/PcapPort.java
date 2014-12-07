package com.gremwell.jnetbridge;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import org.jnetpcap.Pcap;
import org.jnetpcap.PcapBpfProgram;
import org.jnetpcap.PcapStat;
import org.jnetpcap.nio.JBuffer;
import org.jnetpcap.nio.JMemory;
import org.jnetpcap.packet.PcapPacket;
import org.jnetpcap.protocol.JProtocol;
import org.jnetpcap.winpcap.WinPcap;
import org.jnetpcap.winpcap.WinPcapRmtAuth;

/**
 * This class implements a port receiving and sending network packets
 * with libpcap, through jnetpcap library.
 *
 * Upon instantiation PCAP handle gets initialized, but the packet pumping thread
 * does not get started until start().
 *
 * The clients of this class can invoke start()/stop() several times.
 * 
 * The PCAP handle gets closed upon invocation of close() method. After that it
 * does not get reopened by start().
 *
 * @author Alexandre Bezroutchko
 * @author Gremwell bvba
 */
public class PcapPort extends Port {

    private Thread pumpThread = null;
    private Pcap pcap;
    private boolean please_stop = false;
    private final Object pcapLock = new Object();
    private final PcapPacket pcapPacket;
    private BlockingQueue<JBuffer> egressPackets = new LinkedBlockingQueue<JBuffer>();
    
    private PcapBpfProgram packet_filter_program;
    
    // This flag is used to prevent capturing locally generated traffic on a windows platform (winpcap)
    private final static int PCAP_OPENFLAG_NOCAPTURE_LOCAL = 0x8;
    
    private final static int snaplen = 64 * 1024;
    private final static int flags = Pcap.MODE_PROMISCUOUS;
    private static int timeoutMs = 1;
    private final static int bufsizeBytes = 16 * 1024 * 1024;

    private static Pcap openPcap(String ifaceName) throws PcapException {
        Pcap pcap;
        StringBuilder errbuf = new StringBuilder();

        if (!Pcap.isPcap100Loaded()) {
            throw new PcapException("jnetpcap1.4 + libpcap1.0.0+ are required");
        }
        
        if (WinPcap.isSupported() == true)
        //if (false)
        {
            // This code will be used on a windows platform (winpcap)
        	
        	WinPcapRmtAuth auth = null;
        	pcap = WinPcap.open(ifaceName, snaplen, flags | PCAP_OPENFLAG_NOCAPTURE_LOCAL, timeoutMs, auth, errbuf);
        	
        }
        else
        {
        	// This code will be used on all other platforms
        	
        	pcap = Pcap.create(ifaceName, errbuf);
            if (pcap == null) {
                throw new PcapException("failed to create pcap interface " + ifaceName + ": " + errbuf.toString());
            }
        	
        	// standard properties
            pcap.setSnaplen(snaplen);
            pcap.setPromisc(flags);
            pcap.setTimeout(timeoutMs);

            // specific to libpcap 1.0.0
            
            // pcap.setDirection is not implemented on a windows platform (winpcap)
            // the flag method must be used
            pcap.setDirection(Pcap.Direction.IN);
            pcap.setBufferSize(bufsizeBytes);

            pcap.activate();	
        }

        return pcap;
    }

    /**
     * Prepare the interface to receive and send packets. May throw an exception if an
     * error occurs.
     *
     * @param name Name of the interface to open
     * @throws PcapException
     */
    public PcapPort(String name) throws PcapException {
        super(name);

        pcap = openPcap(name);
        pcapPacket = new PcapPacket(JMemory.Type.POINTER);
    }
    
    public PcapPort(String name, int timeout) throws PcapException {
        super(name);
        
        timeoutMs = timeout;

        pcap = openPcap(name);
        pcapPacket = new PcapPacket(JMemory.Type.POINTER);
    }

    public synchronized void setFilter(String filter_str)throws PcapException {
    	
    	// pcap.compile to define packet filter operation
        final int optimize = 1;   // 1 = true  
        final int netmask = 0; 	// 0
        
    	// We setup the packet filter 
    	packet_filter_program = new PcapBpfProgram();
    	
    	// We compile the filter
    	if (pcap.compile(packet_filter_program, filter_str, optimize, netmask) != Pcap.OK) 
        {  
    		throw new PcapException("failed to compile filter: " + filter_str + pcap.getErr()); 
        }
    	
    	// We set the filter
		if (pcap.setFilter(packet_filter_program) != Pcap.OK) 
		{
			throw new PcapException("failed to set filter: " + filter_str + pcap.getErr()); 
		}
    }
    
    @Override
    public void send(JBuffer packet) {
        egressPackets.add(packet);
    }

    /**
     * Start packet pumping thread. The thread will run until stop() or
     * close().
     *
     */
    public synchronized void start() {
        if (pumpThread == null) {
        	please_stop = false;
            pumpThread = new Thread(new PcapPortPump(this));
            pumpThread.start();
        }
    }

    /**
     * Stop packet pumping thread. Invoked by close().
     *
     */
    public synchronized void stop() {
        if (pumpThread != null) 
        {
        	please_stop = true;
            pumpThread.interrupt();
            pumpThread = null;
        }
    }

    public void close() {
        stop();

        synchronized (pcapLock) {
            if (pcap != null) {
            	
            	if(packet_filter_program != null)
            	{
            		Pcap.freecode(packet_filter_program);
            		packet_filter_program =null;
            	}
            	
                pcap.close();
                pcap = null;
            }
        }
    }

    /**
     *
     * @see Port#getStat()
     */
    @Override
    public String getStat() {
        if (pcap == null) {
            return "closed";
        }

        PcapStat ps = new PcapStat();
        pcap.stats(ps);

        return super.getStat()
                + ", egQueue=" + egressPackets.size()
                + ", pcapDrop=" + ps.getDrop()
                + ", pcapIfDrop=" + ps.getIfDrop()
                + ", pcapRecv=" + ps.getRecv();
    }

    private class PcapPortPump implements Runnable {

        public PcapPortPump(PcapPort port) {
        }

        public void run() {
            while ( !(Thread.interrupted() && please_stop)) 
            {          	
                // check if jnetpcap has a packet for us
                int res;
                synchronized (pcapLock) {
                    if (pcap == null) {
                        return;
                    }
                    res = pcap.nextEx(pcapPacket);
                }
                if (res == Pcap.NEXT_EX_OK) {
                    received++;

                    PcapPacket pcapPacketCopy = new PcapPacket(pcapPacket); // deep copy
                    pcapPacketCopy.scan(JProtocol.ETHERNET_ID); // parse the headers

                    ingress(pcapPacketCopy);
                }

                // flush the egress queue
                while (!egressPackets.isEmpty()) {
                    JBuffer packet = egressPackets.poll();

                    synchronized (pcapLock) {
                        if (pcap == null) {
                            return;
                        }
                        pcap.sendPacket(packet);
                    }

                    sent++;
                }
            }
        }
    }
}
