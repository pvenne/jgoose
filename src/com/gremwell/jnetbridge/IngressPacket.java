package com.gremwell.jnetbridge;

import org.jnetpcap.packet.JPacket;

/**
 * This class encapsulates an ingress packet. Instances of these objects
 * are created by subclasses of <code>Port</code> and have to be scanned with
 * JNetPcap's scan() before passing to ingress() method. It means that
 * <code>PortListener</code>s (or their client) are free to use hasHeader()
 * to directly access headers of Ethernet or higher level protocols.
 *
 * Each packet has a unique sequentially assigned id.
 * 
 * @author Alexandre Bezroutchko
 * @author Gremwell bvba
 */
public class IngressPacket {

    private static int nextId = 1;
    public final int id;
    public final Port port;
    public final JPacket packet;

    protected IngressPacket(Port port, JPacket packet) {
        id = nextId();
        this.port = port;
        this.packet = packet;
    }

    private synchronized static int nextId() {
        return nextId++;
    }
}
