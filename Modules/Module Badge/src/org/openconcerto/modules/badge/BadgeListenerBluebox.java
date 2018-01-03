package org.openconcerto.modules.badge;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;

public class BadgeListenerBluebox extends BadgeListener {

    private static final int SOH = 1;
    private static final char STX = 2;
    private static final char ETX = 3;
    private static final char EOT = 4;
    private static final char ENQ = 5;
    private static final char ACK = 0x06;
    private static final char NAK = 0x15;
    private static final char SYN = 0x16;
    private static final char CR = 0x0D;
    private static int PORT = 3000;

    public boolean openDoor(int seconds) {
        Socket socket = null;
        String ret = "NAK";
        System.out.println("OpenDoor:" + seconds + "s");

        try {
            System.out.println("BadgeListenerBluebox.openDoor() new sockect created " + getDoorIp() + " " + PORT);
            socket = new Socket(getDoorIp(), PORT);
            socket.setSoTimeout(2000);
        } catch (IOException e) {
            e.printStackTrace();
            return false;

        }
        System.out.println("BadgeListenerBluebox.openDoor() sending bytes");
        try

        {
            BufferedReader input = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            OutputStream output = socket.getOutputStream();

            byte[] message = new byte[] { SOH, 'F', 'F', STX, '3', '7', '0', '1', '0', (byte) ('0' + seconds), ETX, 0, CR };
            byte crc = message[0];
            for (int i = 1; i < message.length - 2; i++) {
                crc ^= message[i];
            }
            if (seconds == 4) {
                crc = 2;
            }
            message[message.length - 2] = crc;

            output.write(message);
            output.flush();

            ret = input.readLine();
            ret = decode(ret);

        } catch (IOException e) {
            System.out.println(e);
        }
        try {
            socket.close();
        } catch (IOException e) {
            System.out.println(e);
        }
        System.out.println("BadgeListenerBluebox.openDoor() " + seconds + "s sent");
        return !ret.contains("NAK");

    };

    private static String decode(String line) {
        System.out.println("BadgeListenerBluebox.decode(): " + line);
        final int length = line.length();
        final StringBuffer output = new StringBuffer(length * 3);
        for (int i = 0; i < length; i++) {
            final char c = line.charAt(i);
            switch (c) {
            case SOH:
                output.append("SOH ");
                break;
            case STX:
                output.append("STX ");
                break;
            case ETX:
                output.append("ETX ");
                break;
            case EOT:
                output.append("EOT ");
                break;
            case ENQ:
                output.append("ENQ ");
                break;
            case ACK:
                output.append("ACK ");
                break;
            case NAK:
                output.append("NAK ");
                break;
            case SYN:
                output.append("SYN ");
                break;
            case CR:
                output.append("CR ");
                break;
            default:
                if (Character.isLetterOrDigit(c)) {
                    output.append(c);
                } else
                    output.append(Byte.toString((byte) c));
                break;
            }

        }
        return output.toString();
    }

    @Override
    public void startDaemon() {
        Thread t = new Thread("TCP") {
            public void run() {
                System.out.println("BadgeListenerBluebox.startDaemon() started");
                try {
                    ServerSocket listener = new ServerSocket(3000);

                    while (true) {

                        final Socket server = listener.accept();
                        server.setSoTimeout(15000);
                        final Thread tReader = new Thread() {
                            public void run() {

                                System.out.println("BlueBox.run()");
                                try {
                                    // Get input from the client
                                    BufferedReader in = new BufferedReader(new InputStreamReader(server.getInputStream()));
                                    OutputStream out = server.getOutputStream();

                                    String line = in.readLine();
                                    out.close();
                                    server.close();
                                    line = decode(line);
                                    try {
                                        Thread.sleep(1000);
                                    } catch (InterruptedException e) {
                                        // TODO Auto-generated catch block
                                        e.printStackTrace();
                                    }
                                    cardIdReceived(getIdFromLine(line));

                                    System.out.println("BlueBox.run() done");
                                } catch (IOException ioe) {
                                    System.out.println("IOException on socket listen: " + ioe);
                                    ioe.printStackTrace();
                                }

                            };
                        };

                        tReader.start();
                    }
                } catch (IOException ioe) {
                    System.out.println("IOException on socket listen: " + ioe);
                    ioe.printStackTrace();
                }

            };
        };
        t.start();

    }

    String getIdFromLine(String line) {
        if (line.length() < 5)
            return "";
        int i1 = line.indexOf("ETX");
        if (i1 > 0) {
            return line.substring(4, i1);
        }
        return "";
    }

    public static void main(String[] args) {
        final BadgeListener bl = new BadgeListenerBluebox();
        bl.readConfiguration();
        bl.startDaemon();
    }
}
