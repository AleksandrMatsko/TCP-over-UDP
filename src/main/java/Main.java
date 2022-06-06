import RDT.RDTClient;
import RDT.RDTServer;

import java.net.SocketException;

public class Main {

    public static void main(String[] args) {
        String[] dataToSend = {"first", "second", "third", "fourth", "fifth", "sixth", "seventh", "eighth", "ninth", "tenth",
                "first1", "second1", "third1", "fourth1", "fifth1", "sixth1", "seventh1", "eighth1", "ninth1", "tenth1",
                "first2", "second2", "third2", "fourth2", "fifth2", "sixth2", "seventh2", "eighth2", "ninth2", "tenth2"};

        try {
            RDTClient rdtClient = new RDTClient(15000, 15001, 0.8,
                    1000, dataToSend);
            RDTServer rdtServer = new RDTServer(15000, 15001, 0.8, 1000);
            Thread clientThread = new Thread(rdtClient);
            Thread serverThread = new Thread(rdtServer);
            serverThread.start();
            clientThread.start();



            String[] receivedData = null;
            try {
                receivedData = rdtServer.getData();
            }
            catch (IllegalAccessException ex) {
                System.err.println("Asked data before Server started");
            }
            Thread.sleep(1000);
            try {
                receivedData = rdtServer.getData();
            }
            catch (IllegalAccessException ex) {
                System.err.println("Asked data before Server started");
            }
            if (receivedData == null) {
                throw new RuntimeException();
            }

            System.err.println("Size of received data = " + receivedData.length);

            System.out.print("RECV: ");
            for (String str : receivedData) {
                System.out.print(str + " ");
            }
            System.out.print(System.lineSeparator());
            System.out.print("SENT: ");
            for (String str : dataToSend) {
                System.out.print(str + " ");
            }
            System.out.print(System.lineSeparator());
        }
        catch (SocketException | InterruptedException ex) {
            throw new RuntimeException(ex);
        }

    }

}
