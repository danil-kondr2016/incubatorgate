package ru.danilakondratenko.incubatorgate;

import java.io.*;
import java.net.InetSocketAddress;
import java.net.URI;
import java.util.Scanner;
import com.fazecast.jSerialComm.*;
import com.sun.net.httpserver.*;


public class Main {
    public static Requestor requestor;
    public static Requestor lightsControlRequestor;

    public static String getSerialPortDescriptor() {
        SerialPort[] ports = SerialPort.getCommPorts();
        if (ports.length == 1) {
            return ports[0].getSystemPortName();
        }
        if (ports.length == 0) {
            return null;
        }
        int i = 0;
        for (SerialPort port : ports) {
            System.out.println("[" + i + "] " + port.getSystemPortName());
            i++;
        }

        System.out.print("Select port: ");
        Scanner scanner = new Scanner(System.in);
        int n_port = scanner.nextInt();

        return ports[n_port].getSystemPortName();
    }

    public static void main(String[] args) {
	    try {
	        String portDescriptor = getSerialPortDescriptor();
            if (portDescriptor != null) {
                requestor = new Requestor(portDescriptor);
                if (!requestor.isIncubatorLightController())
                    new Archiver(requestor);
                else {
                    System.out.println("Error: incorrect serial port");
                    System.exit(0);
                }
            } else {
                System.out.println("Error: serial port not found");
                System.exit(0);
            }

            System.out.println("Determining lights controller...");
            SerialPort[] ports = SerialPort.getCommPorts();
            for (SerialPort port : ports) {
                if (port.getSystemPortName().compareTo(portDescriptor) == 0)
                    continue;
                Requestor test = new Requestor(port.getSystemPortName());
                if (test.isIncubatorLightController()) {
                    System.out.printf("Lights controller found: port %s\n", port.getSystemPortName());
                    lightsControlRequestor = test;
                    break;
                } else {
                    lightsControlRequestor = null;
                }
            }

            if (lightsControlRequestor == null) {
                System.out.println("Error: lights controller not found");
                System.exit(0);
            }

            HttpServer server = HttpServer.create(new InetSocketAddress(80), 0);
            server.createContext("/", new HttpRequestHandler());
            server.start();
        } catch (Exception e) {
	        e.printStackTrace();
        }
    }

    public static class HttpRequestHandler implements HttpHandler {
        public static final String DOCTYPE_HTML =
                "<!DOCTYPE HTML PUBLIC \"-//W3C//DTD HTML 4.01//EN\" \"http://www.w3.org/TR/html4/strict.dtd\">\r\n";
        public static final String FOOTER =
                "<p><i>IncubatorGate Java</i></p>\r\n";
        public static final String WELCOME_PAGE =
                DOCTYPE_HTML +
                        "<html>\r\n" +
                        "<head><title>Incubator</title></head>\r\n" +
                        "<body>\r\n" +
                        "<h1 align=\"center\">The IoT-incubator</h1>\r\n" +
                        "<p align=\"center\">Created by Kondratenko Daniel in 2021</p>\r\n" +
                        "<hr>\r\n" +
                        FOOTER +
                        "</body>\r\n" +
                        "</html>\r\n";
        public static final String ERROR_404_PAGE =
                DOCTYPE_HTML +
                        "<html>\r\n" +
                        "<head><title>Error</title></head>\r\n" +
                        "<body>\r\n" +
                        "<h1 align=\"center\">Error 404: not found</h1>\r\n" +
                        "<hr>\r\n" +
                        FOOTER +
                        "</body>\r\n" +
                        "</html>\r\n";

        @Override
        public void handle(HttpExchange httpExchange) throws IOException {
            URI uri = httpExchange.getRequestURI();
            String path = uri.getPath();
            String method = httpExchange.getRequestMethod();
            System.out.println("Request " + method + " to " + uri.toString());

            DataInputStream is = new DataInputStream(httpExchange.getRequestBody());
            DataOutputStream os = new DataOutputStream(httpExchange.getResponseBody());
            byte[] answerBuf = null;
            int answerLen = 0;

            if (path.compareTo("/") == 0 || path.compareTo("/index.html") == 0) {
                answerBuf = WELCOME_PAGE.getBytes("UTF-8");
                answerLen = answerBuf.length;
                httpExchange.getResponseHeaders().add("Content-Type", "text/html; charset=utf-8");
                httpExchange.sendResponseHeaders(200, answerLen);
            } else if (path.compareTo("/control") == 0) {
                if (method.compareTo("GET") == 0) {
                    answerBuf = "method_get\r\n".getBytes();
                    answerLen = answerBuf.length;
                } else if (method.compareTo("POST") == 0) {
                    byte[] reqBuf = new byte[is.available()];
                    is.read(reqBuf);

                    answerBuf = new byte[Requestor.LEN_BYTES];
                    String[] reqString = new String(reqBuf).split("\r\n");
                    if (reqString[0].startsWith("lights_") || reqString[0].equals("reset") == 0) {
                        answerLen = lightsControlRequestor.makeRequest(reqBuf, answerBuf);
                    } else {
                        answerLen = requestor.makeRequest(reqBuf, answerBuf);
                    }
                }
                httpExchange.getResponseHeaders().add("Content-Type", "text/plain; charset=utf-8");
                httpExchange.sendResponseHeaders(200, answerLen);
            } else if (path.compareTo("/archive_address") == 0) {
                answerBuf = (Archiver.INCUBATOR_ARCHIVE_ADDRESS + "\r\n").getBytes();
                answerLen = answerBuf.length;
                httpExchange.getResponseHeaders().add("Content-Type", "text/plain; charset=utf-8");
                httpExchange.sendResponseHeaders(200, answerLen);
            } else {
                answerBuf = ERROR_404_PAGE.getBytes();
                answerLen = answerBuf.length;
                httpExchange.getResponseHeaders().add("Content-Type", "text/html; charset=utf-8");
                httpExchange.sendResponseHeaders(404, answerLen);
            }
            os.write(answerBuf, 0, answerLen);
            os.flush();
            os.close();
        }
    }
}
