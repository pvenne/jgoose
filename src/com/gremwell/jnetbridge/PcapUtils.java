package com.gremwell.jnetbridge;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.jnetpcap.Pcap;
import org.jnetpcap.PcapIf;
import org.jnetpcap.packet.format.FormatUtils;

import java.io.IOException;

/**
 *
 * @author Alexandre Bezroutchko
 * @author Gremwell bvba
 */
public class PcapUtils {

    private static List<PcapIf> allPcapDevices = null;

    /**
     * This function returns a list of all available <code>PcapIf</code>s.
     *
     * @param rescan
     * @return
     * @throws PcapException
     */
    public static List<PcapIf> getAllPcapDevices(boolean rescan) throws PcapException {
        if (rescan || allPcapDevices == null) {
            allPcapDevices = new ArrayList<PcapIf>();

            StringBuilder errbuf = new StringBuilder();
            int r = Pcap.findAllDevs(allPcapDevices, errbuf);
            if (r != Pcap.OK) {
                throw new PcapException("Can't read list of devices: " + errbuf.toString());
            }
        }

        return Collections.unmodifiableList(allPcapDevices);
    }

    /**
     * This function returns a list of <code>PcapIf</code>s with certain
     * DLT.
     *
     * @param rescan If true, don't use cached data
     * @param dlt DLT constant, for example PcapDLT.CONST_EN10MB
     * @return
     * @throws PcapException
     */
    public static List<PcapIf> getPcapDevices(boolean rescan, int dlt) throws PcapException {
        List<PcapIf> pcapDevices = getAllPcapDevices(rescan);

        List<PcapIf> pcapDevicesWithRightDlt = new ArrayList<PcapIf>();
        for (PcapIf pcapIf : pcapDevices) {

            StringBuilder errbuf = new StringBuilder();
            Pcap pcap = Pcap.openLive(pcapIf.getName(), 0, 0, 0, errbuf);
            if (pcap != null && pcap.datalink() == dlt) {
                pcapDevicesWithRightDlt.add(pcapIf);
            }
        }

        return Collections.unmodifiableList(pcapDevicesWithRightDlt);
    }
    
    public static String macToName(String network_MAC) throws PcapException, IOException  {
    	// If the list is empty, we populate it
    	if (allPcapDevices == null)
    		getAllPcapDevices(true);
        
        for (PcapIf pcapIf : allPcapDevices) {
        	if(pcapIf.getHardwareAddress() != null &&
            	FormatUtils.mac(pcapIf.getHardwareAddress()).equals(network_MAC))
            	{
            		return pcapIf.getName();
            	}  
        
        }
        
        throw new PcapException("Can't find device matching MAC address ");
    }
}
