package ru.danilakondratenko.incubatorgate;

import java.net.ServerSocket;
import java.util.Scanner;
import com.fazecast.jSerialComm.*;

public class Main {
    //public static Requestor requestor;

    public static SerialPort getSerialPort() {
        SerialPort[] ports = SerialPort.getCommPorts();
        if (ports.length == 1) {
            return ports[0];
        }
        if (ports.length == 0) {
            return null;
        }
        int i = 0;
        for (SerialPort port : ports) {
            System.out.println("[" + i + "]" + port.getSystemPortName());
            i++;
        }

        System.out.print("Select port: ");
        Scanner scanner = new Scanner(System.in);
        int n_port = scanner.nextInt();

        return ports[n_port];
    }

    public static void main(String[] args) {
	    try {
	        SerialPort port = getSerialPort();
            if (port != null) {
                port.openPort();
                port.setComPortTimeouts(
                    SerialPort.TIMEOUT_WRITE_BLOCKING
                  | SerialPort.TIMEOUT_READ_BLOCKING, 2000, 2000);
            } else {
                System.out.println("Error: serial port not found");
                System.exit(0);
            }

            //requestor = new Requestor(port);

            ServerSocket serverSocket = new ServerSocket(80);
            while (true) {
                System.out.println("Waiting socket...");
                Server server = new Server(serverSocket.accept(), port);
                server.start();
            }
        } catch (Exception e) {
	        e.printStackTrace();
        }
    }
}
