package ru.danilakondratenko.incubatorgate;

import com.fazecast.jSerialComm.SerialPort;

import java.util.Date;

public class IncubatorControl extends Thread {
    public static final int BAUDRATE = 19200;
    public static final int READ_TIMEOUT = 100;
    public static final int STOP_BIT = 1;
    public static final int BYTE_FRAME = 8 + 1 + STOP_BIT;
    public static final int LEN_BYTES = (READ_TIMEOUT * (BAUDRATE / BYTE_FRAME)) / 1000;

    public Requestor incubatorRequestor;
    public Requestor lightsControlRequestor;
    private long lastRequestTime;

    IncubatorControl() {
        checkRequestors();
        this.lastRequestTime = System.currentTimeMillis();

        this.setDaemon(true);
        this.start();
    }

    private void checkRequestors() {
        SerialPort[] ports = SerialPort.getCommPorts();
        if (ports.length != 2) {
            System.out.println("Error: can't determine serial ports");
            System.exit(0);
        }
        this.lightsControlRequestor.port = null;
        this.incubatorRequestor.port = null;
        for (SerialPort port : ports) {
            System.out.println("Test port: " + port.getSystemPortName());
            Requestor test = new Requestor(port.getSystemPortName());
            if (isIncubatorLightController(test)) {
                this.lightsControlRequestor.port = test.port;
                System.out.println("Lights control: " + port.getSystemPortName());
            } else {
                this.incubatorRequestor.port = test.port;
                System.out.println("Requestor: " + port.getSystemPortName());
            }
        }
    }

    @Override
    public void run() {
        while (true) {
            if (System.currentTimeMillis() - lastRequestTime >= 2000) {
                if (!this.incubatorRequestor.portIsOpen() || !this.incubatorRequestor.portIsOpen())
                    checkRequestors();
            }
        }
    }

    public synchronized IncubatorState requestState() {
        byte[] respBytes = new byte[LEN_BYTES];
        int respLen = incubatorRequestor.makeRequest("request_state\r\n", respBytes);
        assert respLen > 0;

        String[] responseString = new String(respBytes).split("\r\n");
        IncubatorState result = IncubatorState.deserialize(responseString);
        result.timestamp = new Date().getTime();
        return result;
    }

    public synchronized IncubatorConfig requestConfig() {
        byte[] respBytes = new byte[LEN_BYTES];
        int respLen = incubatorRequestor.makeRequest("request_config\r\n", respBytes);
        assert respLen > 0;

        String[] responseString = new String(respBytes).split("\r\n");
        return IncubatorConfig.deserialize(responseString);
    }

    public synchronized boolean isIncubatorLightController(Requestor requestor) {
        byte[] respBytes = new byte[LEN_BYTES];
        int respLen = requestor.makeRequest("lightscontrol\r\n", respBytes);
        assert respLen > 0;

        String[] responseString = new String(respBytes).split("\r\n");
        return (responseString[0].compareTo("lightscontrol") == 0);
    }
}
