package com.gremwell.jnetbridge;

import org.jnetpcap.packet.JPacket;

/**
 * Subclasses of this class are fit to receive ingress packets from <code>Port</code>s.
 * They have to be registered with a port by calling its setListener() method.
 * 
 * @author Alexandre Bezroutchko
 * @author Gremwell bvba
 */
public abstract class PortListener {

    abstract void ingress(Port port, JPacket packet);
}
