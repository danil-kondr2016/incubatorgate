package ru.danilakondratenko.incubatorgate;

import com.fazecast.jSerialComm.*;

import java.io.*;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.HttpURLConnection;
import java.util.Arrays;
import java.util.Date;
import java.util.ArrayList;

public class Archiver extends Thread {
    public static final String INCUBATOR_ARCHIVE_ADDRESS = "185.26.121.126";

    private Requestor requestor;
    private long lastRequestTime;

    Archiver(Requestor requestor) {
        this.requestor = requestor;
        this.lastRequestTime = System.currentTimeMillis();

        this.setDaemon(true);
        this.start();
    }

    private void writeToArchive(IncubatorData _data) throws IOException {
        IncubatorData data = new IncubatorData(_data);
        if (!data.isCorrect()) {
          if (Float.isNaN(data.currentTemperature))
            data.currentTemperature = 0;
          if (Float.isNaN(data.currentHumidity))
            data.currentHumidity = 0;
          if (Float.isNaN(data.neededTemperature))
            data.neededTemperature = 0;
          if (Float.isNaN(data.neededHumidity))
            data.neededHumidity = 0;
        }
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
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod("GET");
        connection.connect();

        BufferedInputStream reader = new BufferedInputStream(connection.getInputStream());
        byte[] answer = new byte[reader.available()];
        reader.read(answer);
    }

    @Override
    public void run() {
        try {
            long time;
            while (true) {
                time = System.currentTimeMillis();
                if ((time - lastRequestTime) >= 2000) {
                    IncubatorState state = requestor.requestState();
                    IncubatorConfig cfg = requestor.requestConfig();

                    writeToArchive(new IncubatorData(state, cfg));
                    lastRequestTime = System.currentTimeMillis();
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
