package ru.danilakondratenko.incubatorgate;

import com.fazecast.jSerialComm.*;

import java.io.*;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.HttpURLConnection;
import java.util.Arrays;
import java.util.Date;
import java.util.ArrayList;

public class Requestor extends Thread {
    private static final String INCUBATOR_ARCHIVE_ADDRESS = "185.26.121.126";

    private SerialPort port;
    private long lastRequestTime;

    private IncubatorData data;

    Requestor(SerialPort port) {
        this.port = port;
        this.lastRequestTime = System.currentTimeMillis();

        data = new IncubatorData();

        this.setDaemon(true);
        this.start();
    }

    private byte[] makeRequest(String str) {
        System.out.println("makeRequest " + str);
        try {
            byte[] reqBuffer = str.getBytes("UTF-8");
            port.writeBytes(reqBuffer, reqBuffer.length);
            byte[] oneByteBuffer = new byte[1];
            ArrayList<Byte> respList = new ArrayList<Byte>();
            while (port.bytesAvailable() > 0) {
                port.readBytes(oneByteBuffer, 1);
                respList.add(oneByteBuffer[0]);
            }
            byte[] respBuffer = new byte[respList.size()];
            for (int i = 0; i < respList.size(); i++)
                respBuffer[i] = respList.get(i);
            return respBuffer;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    private void writeToArchive() throws IOException {
        String urlString = "http://" + INCUBATOR_ARCHIVE_ADDRESS + "/archive/insert.php?";
        urlString += "timestamp=" + data.timestamp + "&";
        urlString += "curtemp=" + data.currentTemperature + "&";
        urlString += "curhumid=" + data.currentHumidity + "&";
        urlString += "needtemp=" + data.neededTemperature + "&";
        urlString += "needhumid=" + data.neededHumidity + "&";
        urlString += "heater=" + (data.heater ? 1 : 0) + "&";
        urlString += "wetter=" + (data.wetter ? 1 : 0) + "&";
        urlString += "chamber=" + data.chamber;

        URL url = new URL(urlString);
        HttpURLConnection connection = (HttpURLConnection)url.openConnection();
        connection.setRequestMethod("GET");
        connection.connect();

        BufferedInputStream reader = new BufferedInputStream(connection.getInputStream());
        byte[] answer = new byte[reader.available()];
        reader.read(answer);
        System.out.println(Arrays.toString(data.serializeState()));
        System.out.println(Arrays.toString(data.serializeConfig()));
        System.out.println(new String(answer, "UTF-8"));

    }

    public final IncubatorData getData() {
        return this.data;
    }

    @Override
    public void run() {
        try {
            long time;
            while (true) {
                time = System.currentTimeMillis();
                if ((time - lastRequestTime) >= 2000) {
                    byte[] respBufferState = makeRequest("request_state\r\n");
                    byte[] respBufferConfig = makeRequest("request_config\r\n");

                    String[] stateResponse = new String(respBufferState, "UTF-8").split("\r\n");
                    String[] configResponse = new String(respBufferConfig, "UTF-8").split("\r\n");

                    IncubatorData data1 = IncubatorData.deserialize(stateResponse);
                    IncubatorData data2 = IncubatorData.deserialize(configResponse);

                    data.timestamp = new Date().getTime();

                    data.currentTemperature = data1.currentTemperature;
                    data.currentHumidity = data1.currentHumidity;
                    data.heater = data1.heater;
                    data.wetter = data1.wetter;
                    data.cooler = data1.cooler;
                    data.chamber = data1.chamber;
                    data.uptime = data1.uptime;
                    data.overheat = data1.overheat;
                    data.power = data1.power;
                    data.isChanged = data1.isChanged;

                    data.neededTemperature = data2.neededTemperature;
                    data.neededHumidity = data2.neededHumidity;
                    data.rotationsPerDay = data2.rotationsPerDay;
                    data.currentProgram = data2.currentProgram;
                    data.numberOfPrograms = data2.numberOfPrograms;

                    data.isCorrect = data1.isCorrect && data2.isCorrect;

                    writeToArchive();
                    lastRequestTime = System.currentTimeMillis();
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
