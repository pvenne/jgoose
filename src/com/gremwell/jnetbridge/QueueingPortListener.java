package com.gremwell.jnetbridge;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import org.jnetpcap.packet.JPacket;

/**
 * This subclass of <code>PortListener</code> places the ingress packets
 * into a queue. Its clients can take the packets one by calling receive()
 * method.
 *
 * @author Alexandre Bezroutchko
 * @author Gremwell bvba
 */
public class QueueingPortListener extends PortListener {

    private BlockingQueue<IngressPacket> ingressPackets = new LinkedBlockingQueue<IngressPacket>();

    /**
     * This method is invoked by the ports, asynchronously.
     * 
     * @param port
     * @param packet
     */
    @Override
    void ingress(Port port, JPacket packet) {
        ingressPackets.add(new IngressPacket(port, packet));
    }

    /**
     * This method is invoked by hub clients, to get an ingress packet.
     * Will block until a packet is available.
     *
     * @return An ingress packet
     * @throws InterruptedException
     */
    public IngressPacket receive() throws InterruptedException {
        return ingressPackets.take();
    }
}
