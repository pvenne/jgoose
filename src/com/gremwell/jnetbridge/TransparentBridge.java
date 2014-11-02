package com.gremwell.jnetbridge;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import org.jnetpcap.PcapDLT;
import org.jnetpcap.PcapIf;

/**
 * This is an example how JNetBridge can be used to make a transparent
 * bridge forwarding packets between two network interfaces.
 *
 * @author Alexandre Bezroutchko
 * @author Gremwell bvba
 */
public class TransparentBridge {

    /**
     *
     * @param args
     * @throws PcapException
     * @throws InterruptedException
     */
    public static void main(String args[]) throws PcapException, InterruptedException {
        // parse command line parameters
        if (args.length == 0) {
            System.out.println("Usage: jnetpcap.sh IFACE1 IFACE2");
            System.out.println("Available Ethernet interfaces:");
            for (PcapIf pcapIf : PcapUtils.getPcapDevices(false, PcapDLT.CONST_EN10MB)) {
                System.out.println("\t" + pcapIf.getName());
            }
            return;
        } else if (args.length != 2) {
            System.out.println("Usage: jnetpcap.sh IFACE1 IFACE2");
            return;
        }

        // open ports
        final PcapPort port0 = new PcapPort(args[0]);
        final PcapPort port1 = new PcapPort(args[1]);

        // initialize port listener
        final QueueingPortListener portListener = new QueueingPortListener();
        port0.setListener(portListener);
        port1.setListener(portListener);

        // start port handling threads
        port0.start();
        port1.start();

        // print port statistics every second
        ScheduledExecutorService ses = Executors.newScheduledThreadPool(1);
        ses.scheduleAtFixedRate(new Runnable() {

            public void run() {
                System.err.println("port0: " + port0.getStat());
                System.err.println("port1: " + port1.getStat());
            }
        }, 1, 1, TimeUnit.SECONDS);

        // pump the packets from one interface to another
        for (;;) {
            // wait for an ingress packet
            IngressPacket ingressPacket = portListener.receive();

            // decide what port to forward it to
            PcapPort egressPort;
            if (ingressPacket.port == port0) {
                egressPort = port1;
            } else if (ingressPacket.port == port1) {
                egressPort = port0;
            } else {
                throw new IllegalStateException("bad egress packet port");
            }

            // forward the packet
            egressPort.send(ingressPacket.packet);
        }

        // port0.close();
        // port1.close();
    }
}
