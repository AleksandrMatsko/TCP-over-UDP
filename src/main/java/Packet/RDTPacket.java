package Packet;

import java.io.*;

public class RDTPacket implements Serializable, Comparable<RDTPacket> {
     private final int seqNumber;
     private final int ackNumber;
     private final String data;
     private final boolean isACK;
     private final boolean isSyn;
     private final boolean isFin;

     public RDTPacket(boolean isACK, boolean isSyn, boolean isFin, int seqNumber, int ackNumber, String data)  {
         this.seqNumber = seqNumber;
         this.ackNumber = ackNumber;
         this.data = data;
         this.isACK = isACK;
         this.isFin = isFin;
         this.isSyn = isSyn;
     }

     public RDTPacket(boolean isACK, boolean isSyn, boolean isFin, int seqNumber, int ackNumber) {
         this.seqNumber = seqNumber;
         this.ackNumber = ackNumber;
         this.isACK = isACK;
         this.isFin = isFin;
         this.isSyn = isSyn;
         data = null;
     }

     public boolean isACK() {
         return isACK;
     }

     public byte[] pack() {
         ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
         try (ObjectOutputStream objectOutputStream = new ObjectOutputStream(byteArrayOutputStream)) {
             objectOutputStream.writeObject(this);
             return byteArrayOutputStream.toByteArray();
         }
         catch (IOException ex) {
             throw new RuntimeException(ex);
         }

     }

     public static RDTPacket unpack(byte[] bytes) {
         InputStream inputStream = new ByteArrayInputStream(bytes);
         try (ObjectInputStream objectInputStream = new ObjectInputStream(inputStream)) {
             return (RDTPacket) objectInputStream.readObject();
         }
         catch (IOException | ClassNotFoundException ex) {
             throw new RuntimeException(ex);
         }
     }

    public int getSeqNumber() {
        return seqNumber;
    }

    public int getAckNumber() {
        return ackNumber;
    }

    public String getData() {
        return data;
    }

    public boolean isSyn() {
        return isSyn;
    }

    public boolean isFin() {
        return isFin;
    }

    @Override
    public int compareTo(RDTPacket o) {
        return this.getSeqNumber() - o.getSeqNumber();
    }
}
