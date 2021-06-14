package ru.danilakondratenko.incubatorgate;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.URI;
import java.util.Scanner;
import com.fazecast.jSerialComm.*;
import com.sun.net.httpserver.*;

public class Main {
    //public static Requestor requestor;

    public static SerialPort port;

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
	        port = getSerialPort();
            if (port != null) {
                port.openPort();
                port.setComPortTimeouts(
                    SerialPort.TIMEOUT_WRITE_BLOCKING
                  | SerialPort.TIMEOUT_READ_SEMI_BLOCKING, 2000, 2000);
            } else {
                System.out.println("Error: serial port not found");
                System.exit(0);
            }

            HttpServer server = HttpServer.create(new InetSocketAddress(80), 0);
            server.createContext("/", new RequestHandler());
            server.createContext("/index.html", new RequestHandler());
            server.createContext("/control", new RequestHandler());
            server.start();
        } catch (Exception e) {
	        e.printStackTrace();
        }
    }

    public static class RequestHandler implements HttpHandler {
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

            DataInputStream is = new DataInputStream(httpExchange.getRequestBody());
            DataOutputStream os = new DataOutputStream(httpExchange.getResponseBody());
            byte[] answerBuf = null;

            if (path.compareTo("/") == 0 || path.compareTo("/index.html") == 0) {
                answerBuf = WELCOME_PAGE.getBytes("UTF-8");
                httpExchange.getResponseHeaders().add("Content-Type", "text/html; charset=utf-8");
                httpExchange.sendResponseHeaders(200, answerBuf.length);
                os.write(answerBuf);
            } else if (path.compareTo("/control") == 0) {
                if (method.compareTo("GET") == 0) {
                    answerBuf = "method_get\r\n".getBytes();
                } else if (method.compareTo("POST") == 0) {
                    byte[] reqBuf = new byte[is.available()];
                    is.read(reqBuf);

                    port.writeBytes(reqBuf, reqBuf.length);
                    byte[] respBuf = new byte[256];
                    System.out.println("" +
                            port.readBytes(respBuf, respBuf.length) + ", " + respBuf.length);

                    answerBuf = respBuf;
                }
                httpExchange.getResponseHeaders().add("Content-Type", "text/plain; charset=utf-8");
                httpExchange.sendResponseHeaders(200, answerBuf.length);
                os.write(answerBuf);
            } else {
                answerBuf = ERROR_404_PAGE.getBytes();
                httpExchange.sendResponseHeaders(404, answerBuf.length);
                os.write(answerBuf);
            }
            os.flush();
            os.close();
        }
    }
}
