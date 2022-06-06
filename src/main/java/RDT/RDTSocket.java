package RDT;

import Packet.RDTPacket;

import java.io.IOException;
import java.net.*;
import java.util.Random;

public class RDTSocket {
    private final DatagramSocket datagramSocket;
    private final double lossChance;
    private final Random random;

    public RDTSocket(int port, double lossChance, int timeout) throws SocketException {
        try {
            datagramSocket = new DatagramSocket(port, InetAddress.getLocalHost());
        }
        catch (UnknownHostException ex) {
            throw new RuntimeException(ex);
        }
        random = new Random();
        this.lossChance = lossChance;
        datagramSocket.setSoTimeout(timeout);
    }

    public void sendRDTPacket(RDTPacket rdtPacket, int port) {
        byte[] packed = rdtPacket.pack();
        System.err.println("Sent: " + rdtPacket.getSeqNumber() + " " + rdtPacket.getAckNumber()
                + " isAck: " + rdtPacket.isACK() + " Syn: " + rdtPacket.isSyn()
                + " Fin: " + rdtPacket.isFin() + " Data: " + rdtPacket.getData());
        try {
            DatagramPacket datagramPacket = new DatagramPacket(packed, packed.length, InetAddress.getLocalHost(), port);
            datagramSocket.send(datagramPacket);
        }
        catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public RDTPacket receiveRDTPacket() {
        byte[] data = new byte[1024 * 1024];
        DatagramPacket datagramPacket = new DatagramPacket(data, data.length);
        try {
            datagramSocket.receive(datagramPacket);
        }
        catch (SocketTimeoutException ex) {
            return null;
        }
        catch (IOException ex) {
            throw new RuntimeException(ex);
        }
        RDTPacket rdtPacket = RDTPacket.unpack(datagramPacket.getData());
        if (random.nextDouble() < lossChance && !rdtPacket.isACK() && !rdtPacket.isFin() && !rdtPacket.isSyn()) {
            System.err.println("!!!!!!!!!!!!!!!RDTPacket was lost!!!!!!!!!!!!!");
            return null;
        }

        System.err.println("Received: " + rdtPacket.getSeqNumber() + " " + rdtPacket.getAckNumber()
                + " isAck: " + rdtPacket.isACK() + " Syn: " + rdtPacket.isSyn()
                + " Fin: " + rdtPacket.isFin() + " Data: " + rdtPacket.getData());
        return rdtPacket;
    }
}
