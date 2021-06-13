package ru.danilakondratenko.incubatorgate;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.InputStream;
import java.net.Socket;
import java.nio.ByteBuffer;
import com.fazecast.jSerialComm.*;

public class Server extends Thread {
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


    private final Socket s;
    private SerialPort port;

    Server(Socket s, SerialPort port) {
        this.s = s;
        this.port = port;
        this.setDaemon(true);
        System.out.println("Accepted: " + s.getLocalAddress() + " " + s.getLocalPort());
    }

    @Override
    public void run() {
        try {
            BufferedInputStream is = new BufferedInputStream(this.s.getInputStream());
            BufferedOutputStream os = new BufferedOutputStream(this.s.getOutputStream());

            int bodyOffset = 0;
            byte[] reqBuffer = new byte[is.available()];
            is.read(reqBuffer);
            String req = new String(reqBuffer, "UTF-8");
            System.out.print(req);

            String[] reqLines = req.split("\r\n");
            String request_method = reqLines[0].split(" ")[0];
            String request_url = reqLines[0].split(" ")[1];
            for (String reqLine : reqLines) {
                bodyOffset += reqLine.length() + 2;
                if (reqLine.length() == 0)
                    break;
            }

            if ((request_url.compareTo("/") == 0) || (request_url.compareTo("/index.html") == 0)) {
                os.write(buildResponse(
                        WELCOME_PAGE.getBytes("UTF-8"),
                        "200 OK",
                        "text/html; charset=utf-8"
                ));
            } else if (request_url.compareTo("/control") == 0) {
                if (request_method.compareTo("POST") == 0) {
                    port.writeBytes(reqBuffer, reqBuffer.length - bodyOffset, bodyOffset);
                    byte[] responseBuffer = new byte[port.bytesAvailable()];
                    System.out.println(new String(responseBuffer, "UTF-8"));
                    port.readBytes(responseBuffer, port.bytesAvailable());

                    os.write(buildResponse(responseBuffer, "200 OK", "text/plain"));
                } else if (request_method.compareTo("GET") == 0) {
                    os.write(buildResponse("method_get\r\n", "200 OK", "text/plain"));
                }
            }

            os.flush();
            is.close();
            os.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public byte[] buildResponse(byte[] buffer, String responseCode, String contentType) {
        try {
            String header = "HTTP/1.1 " +
                    responseCode +
                    "\r\n" +
                    "Server: IncubatorGate\r\n" +
                    "Content-Type: " +
                    contentType +
                    "\r\n" +
                    "Content-Length: " +
                    buffer.length +
                    "\r\n\r\n";
            byte[] headerBytes = header.getBytes("UTF-8");
            ByteBuffer resultBuffer = ByteBuffer.allocate(headerBytes.length + buffer.length);
            resultBuffer.put(headerBytes);
            resultBuffer.put(buffer);

            return resultBuffer.array();
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public byte[] buildResponse(String str, String responseCode, String contentType) {
        return buildResponse(str.getBytes(), responseCode, contentType);
    }
}
