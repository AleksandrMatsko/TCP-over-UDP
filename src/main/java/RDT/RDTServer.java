package RDT;

import Packet.RDTPacket;


import java.net.SocketException;
import java.util.ArrayList;
import java.util.SortedSet;
import java.util.TreeSet;

public class RDTServer extends RDTSocket implements Runnable {
    private final Object lock = new Object();
    private int seqNumber;
    private int ackNumber;
    private final int srcPort;
    private final SortedSet<RDTPacket> receivedPackets;
    private final ArrayList<String> dataParts;
    private boolean isRunning = false;

    public RDTServer(int srcPort, int serverPort, double lossChance, int timeout) throws SocketException {
        super(serverPort, lossChance, timeout);
        receivedPackets = new TreeSet<>();
        dataParts = new ArrayList<>();
        this.srcPort = srcPort;
    }

    private void handshake() throws SocketException {
        RDTPacket syn = receiveRDTPacket();
        if (syn.isSyn() && !syn.isACK()) {
            seqNumber = syn.getAckNumber();
            ackNumber = syn.getSeqNumber() + 1;
            RDTPacket ackSyn = new RDTPacket(true, true, false, seqNumber, ackNumber);
            sendRDTPacket(ackSyn, srcPort);
        }
        else {
            throw new SocketException("invalid packet received during handshake()");
        }
        RDTPacket ack = receiveRDTPacket();
        if (ack.isACK()) {
            seqNumber = ack.getAckNumber();
            ackNumber = ack.getSeqNumber() + 1;
            System.err.println("Server: connection established");
            System.err.println("Server: seq = " + seqNumber + ", ack = " + ackNumber);
        }
        else {
            throw new SocketException("Server: handshake not acked");
        }
    }

    private RDTPacket reliableReceive() {
        while (true) {
            RDTPacket rdtPacket = receiveRDTPacket();
            if (rdtPacket != null) {
                if (rdtPacket.getSeqNumber() < ackNumber) {
                    RDTPacket nak = new RDTPacket(true, false, false, seqNumber, rdtPacket.getSeqNumber());
                    sendRDTPacket(nak, srcPort);
                }
                else {
                    receivedPackets.add(rdtPacket);
                    seqNumber = rdtPacket.getAckNumber();
                    if (ackNumber == rdtPacket.getSeqNumber()) {
                        for (RDTPacket packet : receivedPackets) {
                            if (packet.getSeqNumber() == ackNumber) {
                                ackNumber += 1;
                                if (packet.getData() != null) {
                                    dataParts.add(packet.getData());
                                }
                            }
                        }
                        receivedPackets.removeIf(packet -> packet.getSeqNumber() < ackNumber);
                        return rdtPacket;
                    }
                }
            }
        }
    }

    private void disconnect() throws SocketException {
        RDTPacket ackFin = new RDTPacket(true, false, false, seqNumber, ackNumber);
        sendRDTPacket(ackFin, srcPort);

        seqNumber += 1;

        RDTPacket servFin = new RDTPacket(false, false, true, seqNumber, ackNumber);
        sendRDTPacket(servFin, srcPort);

        RDTPacket ack = receiveRDTPacket();
        if (!ack.isACK()) {
            throw new SocketException("not ack");
        }
    }

    public String[] getData() throws IllegalAccessException {
        if (!isRunning) {
            throw new IllegalAccessException();
        }
        synchronized (lock) {
            String[] res = new String[dataParts.size()];
            int index = 0;
            for (String str : dataParts) {
                res[index] = str;
                index += 1;
            }
            return res;
        }
    }


    @Override
    public void run() {
        synchronized (lock) {
            try {
                isRunning = true;

                handshake();

                while (true) {
                    RDTPacket rdtPacket = reliableReceive();
                    if (rdtPacket.isFin()) {
                        disconnect();
                        break;
                    }
                    else {
                        RDTPacket ack = new RDTPacket(true, false, false, seqNumber, ackNumber);
                        sendRDTPacket(ack, srcPort);
                    }
                }

            }
            catch (SocketException ex) {
                ex.printStackTrace();
            }
        }
    }
}
