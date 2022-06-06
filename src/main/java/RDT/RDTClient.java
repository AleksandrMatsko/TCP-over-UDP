package RDT;

import Packet.RDTPacket;

import java.net.SocketException;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;

public class RDTClient extends RDTSocket implements Runnable {
    private final int serverPort;
    private final int timeout;
    private int seqNumber;
    private int ackNumber;
    private final String[] dataToSend;
    private LinkedList<RDTPacket> packetsList;
    private final HashMap<RDTPacket, Long> timeouts;
    private int WINDOW_SIZE;


    public RDTClient(int clientPort, int serverPort, double lossChance, int timeout, String[] dataToSend) throws SocketException {
        super(clientPort, lossChance, timeout);

        System.err.println("Size of dataToSend: " + dataToSend.length);


        this.serverPort = serverPort;
        this.timeout = timeout;
        this.dataToSend = dataToSend;
        timeouts = new HashMap<>();
        seqNumber = 1;
        ackNumber = 1;
    }

    private void handshake() throws SocketException {
        RDTPacket synPacket = new RDTPacket(false, true, false, seqNumber, ackNumber);
        sendRDTPacket(synPacket, serverPort);

        RDTPacket synACKPacket = receiveRDTPacket();
        if (synACKPacket.isACK() && synACKPacket.isSyn() && synACKPacket.getAckNumber() == seqNumber + 1) {
            seqNumber = synACKPacket.getAckNumber();
            ackNumber = synACKPacket.getSeqNumber() + 1;
            RDTPacket ackPacket = new RDTPacket(true, false, false, seqNumber, ackNumber);
            sendRDTPacket(ackPacket, serverPort);
            System.err.println("Client: connection established");
            System.err.println("Client: seq = " + seqNumber + ", ack = " + ackNumber);
        }
        else {
            throw new SocketException("handshake() failed");
        }
    }

    private void disconnect() throws SocketException {
        RDTPacket finPacket = new RDTPacket(false, false, true, seqNumber, ackNumber);
        sendRDTPacket(finPacket, serverPort);

        RDTPacket first = receiveRDTPacket();
        seqNumber = first.getAckNumber();
        ackNumber = first.getSeqNumber() + 1;

        RDTPacket second = receiveRDTPacket();
        seqNumber = second.getAckNumber();
        ackNumber = second.getSeqNumber() + 1;
        if ((first.isACK() && second.isFin()) || (first.isFin() && second.isACK())) {
            RDTPacket ack = new RDTPacket(true, false, false, seqNumber, ackNumber);
            sendRDTPacket(ack, serverPort);
        }
        else {
            throw new SocketException("invalid server fin");
        }
    }

    private void sendReadyPackets() {
        if (timeouts.size() >= WINDOW_SIZE || packetsList.isEmpty()) {
            return;
        }
        for (int i = 0; i < WINDOW_SIZE - timeouts.size(); i++) {
            RDTPacket rdtPacket = packetsList.pollFirst();
            if (rdtPacket == null) {
                return;
            }
            sendRDTPacket(rdtPacket, serverPort);
            timeouts.put(rdtPacket, System.currentTimeMillis());
        }
    }

    private void reliableReceive() {
        while (true) {
            RDTPacket rdtPacket = receiveRDTPacket();
            if (rdtPacket == null) { //timeout
                long currentTime = System.currentTimeMillis();
                for (Map.Entry<RDTPacket, Long> entry : timeouts.entrySet()) {
                    if (currentTime - entry.getValue() >= timeout) {
                        sendRDTPacket(entry.getKey(), serverPort);
                    }
                }
            }
            else {
                if (rdtPacket.isACK() && rdtPacket.getAckNumber() > seqNumber) {
                    timeouts.entrySet().removeIf(entry -> entry.getKey().getSeqNumber() < rdtPacket.getAckNumber());
                    seqNumber = rdtPacket.getAckNumber();
                    ackNumber = rdtPacket.getSeqNumber() + 1;
                    return;
                }
            }
        }
    }

    @Override
    public void run() {
        try {
            handshake();

            int totalNumPackets = dataToSend.length;

            if (totalNumPackets < 2) {
                WINDOW_SIZE = totalNumPackets;
            }
            else {
                WINDOW_SIZE = totalNumPackets / 2;
            }
            System.err.println("Total number of packets = " + totalNumPackets);
            System.err.println("Window size = " + WINDOW_SIZE);

            packetsList = new LinkedList<>();

            for (int i = 0; i < totalNumPackets; i++) {
                RDTPacket rdtPacket = new RDTPacket(false, false, false, seqNumber + i + 1, ackNumber,
                        dataToSend[i]);
                packetsList.addLast(rdtPacket);
            }

            while (!packetsList.isEmpty() || !timeouts.isEmpty()) {
                sendReadyPackets();
                reliableReceive();
            }

            disconnect();
        }
        catch (SocketException ex) {
            ex.printStackTrace();
        }
    }
}
