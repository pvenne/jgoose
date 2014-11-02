package com.gremwell.jnetbridge;

import org.jnetpcap.nio.JBuffer;
import org.jnetpcap.packet.JPacket;

/**
 * This class represents an abstract port.
 *
 * Subclasses are expected to do the necessary to obtain received packets
 * and to pass it to ingress() method, which will pass them to the listener.
 *
 * Subclasses have to override send() method and make necessary arrangements
 * to send given packet.
 *
 * Subclasses are expected to increment corresponding counters to
 * provide information about port activity.
 *
 * @author Alexandre Bezroutchko
 * @author Gremwell bvba
 */
public abstract class Port {

    protected final String name;
    protected PortListener listener = null;
    // counters
    protected int received = 0;
    protected int sent = 0;

    Port(String name) {
        this.name = name;
    }

    /**
     * Set a <code>PortListener</code> the port has to pass ingress packets to.
     *
     * @param listener
     */
    public void setListener(PortListener listener) {
        this.listener = listener;
    }

    /**
     * The clients have to invoke this method to release resources allocated
     * for this port, if any.
     */
    public abstract void close();

    @Override
    public String toString() {
        return "name=" + name;
    }

    public String getName() {
        return name;
    }

    /**
     * @return A string containing current packet and error counters.
     */
    public String getStat() {
        return "received=" + received + ", sent=" + sent;
    }

    /**
     * This method is invoked by subclasses, asynchronously.
     *
     */
    void ingress(JPacket packet) {
        if (listener != null) {
            listener.ingress(this, packet);
        }
    }

    /**
     * Invoked by the clients. The subclasses of <code>Port</code> have
     * to send the packet immediately or enqueue it.
     * 
     */
    public abstract void send(JBuffer packet);
}
