package ru.danilakondratenko.incubatorgate;

import com.fazecast.jSerialComm.*;

import java.util.Date;

public class Requestor {
    public static final int BAUDRATE = 9600;
    public static final int READ_TIMEOUT = 200;
    public static final int STOP_BIT = 1;
    public static final int BYTE_FRAME = 8 + 1 + STOP_BIT;
    public static final int LEN_BYTES = (READ_TIMEOUT * (BAUDRATE / BYTE_FRAME)) / 1000;

    private String portDescriptor;
    private SerialPort port;

    Requestor(String portDescriptor) {
        this.portDescriptor = portDescriptor;
        this.port = SerialPort.getCommPort(this.portDescriptor);
        if (!this.port.isOpen())
            this.port.openPort();
        this.port.setComPortTimeouts(
                SerialPort.TIMEOUT_WRITE_BLOCKING | SerialPort.TIMEOUT_READ_BLOCKING,
                Requestor.READ_TIMEOUT, 0);
    }

    public synchronized int makeRequest(byte[] reqBuf, byte[] respBuf) {
        try {
            System.out.println(new String(reqBuf));
            this.port.writeBytes(reqBuf, reqBuf.length);
            return this.port.readBytes(respBuf, respBuf.length);
        } catch (Exception e) {
            e.printStackTrace();
            return 0;
        }
    }

    public synchronized int makeRequest(String reqStr, byte[] respBuf) {
        try {
            return makeRequest(reqStr.getBytes("UTF-8"), respBuf);
        } catch (Exception e) {
            e.printStackTrace();
            return 0;
        }
    }

    public synchronized IncubatorState requestState() {
        byte[] respBytes = new byte[LEN_BYTES];
        int respLen = makeRequest("request_state\r\n", respBytes);
        assert respLen > 0;

        String[] responseString = new String(respBytes).split("\r\n");
        IncubatorState result = IncubatorState.deserialize(responseString);
        result.timestamp = new Date().getTime();
        return result;
    }

    public synchronized IncubatorConfig requestConfig() {
        byte[] respBytes = new byte[LEN_BYTES];
        int respLen = makeRequest("request_config\r\n", respBytes);
        assert respLen > 0;

        String[] responseString = new String(respBytes).split("\r\n");
        return IncubatorConfig.deserialize(responseString);
    }
}
