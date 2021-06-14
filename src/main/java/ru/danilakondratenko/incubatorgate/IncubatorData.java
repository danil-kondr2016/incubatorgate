package ru.danilakondratenko.incubatorgate;

import java.util.Locale;

public class IncubatorData {
    public static final float NO_DATA_FLOAT = Float.NaN;
    public static final int NO_DATA_INT = Integer.MIN_VALUE;

    public static final int CHAMBER_LEFT = -1;
    public static final int CHAMBER_NEUTRAL = 0;
    public static final int CHAMBER_RIGHT = 1;
    public static final int CHAMBER_ERROR = 2;
    public static final int CHAMBER_UNDEF = 3;

    /* State */
    public float currentTemperature, currentHumidity;

    public boolean wetter, heater, cooler, overheat;
    public int chamber;
    public long uptime;

    public long timestamp;
    public boolean internet, power;

    public boolean isChanged;

    /* Config */
    public float neededTemperature, neededHumidity;
    public int rotationsPerDay, numberOfPrograms, currentProgram;

    public boolean isCorrect() {
        if (Float.isNaN(currentTemperature))
            return false;
        if (Float.isNaN(currentHumidity))
            return false;
        if (Float.isNaN(neededTemperature))
            return false;
        if (Float.isNaN(neededHumidity))
            return false;

        return true;
    }

    IncubatorData() {
        this.currentTemperature = NO_DATA_FLOAT;
        this.currentHumidity = NO_DATA_FLOAT;
        this.wetter = false;
        this.heater = false;
        this.cooler = false;
        this.chamber = CHAMBER_NEUTRAL;
        this.overheat = false;
        this.uptime = 0;
        this.internet = false;
        this.power = false;
        this.isChanged = false;
        this.timestamp = 0;

        this.neededTemperature = NO_DATA_FLOAT;
        this.neededHumidity = NO_DATA_FLOAT;
        this.rotationsPerDay = NO_DATA_INT;
        this.numberOfPrograms = NO_DATA_INT;
        this.currentProgram = NO_DATA_INT;
    }

    IncubatorData(boolean isCorrect) {
        this.currentTemperature = NO_DATA_FLOAT;
        this.currentHumidity = NO_DATA_FLOAT;
        this.wetter = false;
        this.heater = false;
        this.cooler = false;
        this.chamber = CHAMBER_NEUTRAL;
        this.overheat = false;
        this.uptime = 0;
        this.internet = false;
        this.power = false;
        this.isChanged = false;
        this.timestamp = 0;

        this.neededTemperature = NO_DATA_FLOAT;
        this.neededHumidity = NO_DATA_FLOAT;
        this.rotationsPerDay = NO_DATA_INT;
        this.numberOfPrograms = NO_DATA_INT;
        this.currentProgram = NO_DATA_INT;
    }

    IncubatorData(IncubatorState state, IncubatorConfig cfg) {
        this.currentTemperature = state.currentTemperature;
        this.currentHumidity = state.currentHumidity;
        this.wetter = state.wetter;
        this.heater = state.heater;
        this.cooler = state.cooler;
        this.chamber = state.chamber;
        this.overheat = state.overheat;
        this.uptime = state.uptime;
        this.internet = state.internet;
        this.power = state.power;
        this.isChanged = state.isChanged;
        this.timestamp = state.timestamp;

        this.neededTemperature = cfg.neededTemperature;
        this.neededHumidity = cfg.neededHumidity;
        this.rotationsPerDay = cfg.rotationsPerDay;
        this.numberOfPrograms = cfg.numberOfPrograms;
        this.currentProgram = cfg.currentProgram;
    }

    public String[] serializeState() {
        String result = "";

        if (this.power) {
            result += String.format(Locale.US, "current_temp %.2f\r\n|", this.currentTemperature);
            result += String.format(Locale.US, "current_humid %.2f\r\n|", this.currentHumidity);
            result += String.format(Locale.US, "heater %d\r\n|", this.heater ? 1 : 0);
            result += String.format(Locale.US, "cooler %d\r\n|", this.cooler ? 1 : 0);
            result += String.format(Locale.US, "wetter %d\r\n|", this.wetter ? 1 : 0);
            result += String.format(Locale.US, "chamber %d\r\n|", this.chamber);
            result += String.format(Locale.US, "uptime %d\r\n|", this.uptime);
            if (this.isChanged)
                result += "changed\r\n|";
            if (this.overheat)
                result += "overheat\r\n|";
        } else {
            result += "turned_off\r\n|";
        }

        return result.split("\\|");
    }

    public String[] serializeConfig() {
        String result = "";

        result += String.format(Locale.US, "needed_temp %.2f\r\n|", this.neededTemperature);
        result += String.format(Locale.US, "needed_humid %.2f\r\n|", this.neededHumidity);
        result += String.format(Locale.US, "rotations_per_day %d\r\n|", this.rotationsPerDay);
        result += String.format(Locale.US, "number_of_programs %d\r\n|", this.numberOfPrograms);
        result += String.format(Locale.US, "current_program %d\r\n|", this.currentProgram);

        return result.split("\\|");
    }

    public String[] serializeConfigToSend() {
        String result = "";

        result += String.format(Locale.US, "needed_temp %.2f\r\n|", this.neededTemperature);
        result += String.format(Locale.US, "needed_humid %.2f\r\n|", this.neededHumidity);
        result += String.format(Locale.US, "rotations_per_day %d\r\n|", this.rotationsPerDay);
        result += String.format(Locale.US, "switch_to_program %d\r\n|", this.currentProgram);

        return result.split("\\|");
    }

    public static IncubatorData deserialize(String[] strs) {
        try {
            IncubatorData result = new IncubatorData();
            result.power = true;
            for (String x : strs) {
                String[] args = x.trim().split(" ");
                if (args[0].compareTo("current_temp") == 0) {
                    if (args[1].compareToIgnoreCase("nan") != 0)
                        result.currentTemperature = Float.parseFloat(args[1]);
                    else
                        result.currentTemperature = NO_DATA_FLOAT;
                } else if (args[0].compareTo("current_humid") == 0) {
                    if (args[1].compareToIgnoreCase("nan") != 0)
                        result.currentHumidity = Float.parseFloat(args[1]);
                    else
                        result.currentHumidity = NO_DATA_FLOAT;
                } else if (args[0].compareTo("heater") == 0) {
                    result.heater = (Integer.parseInt(args[1]) > 0);
                } else if (args[0].compareTo("cooler") == 0) {
                    result.cooler = (Integer.parseInt(args[1]) > 0);
                } else if (args[0].compareTo("wetter") == 0) {
                    result.wetter = (Integer.parseInt(args[1]) > 0);
                } else if (args[0].compareTo("chamber") == 0) {
                    result.chamber = Integer.parseInt(args[1]);
                } else if (args[0].compareTo("uptime") == 0) {
                    result.uptime = Long.parseLong(args[1]);
                } else if (args[0].compareTo("overheat") == 0) {
                    result.overheat = true;
                } else if (args[0].compareTo("turned_off") == 0) {
                    result.power = false;
                } else if (args[0].compareTo("changed") == 0) {
                    result.isChanged = true;
                } else if (args[0].compareTo("needed_temp") == 0) {
                    if (args[1].compareToIgnoreCase("nan") != 0)
                        result.neededTemperature = Float.parseFloat(args[1]);
                    else
                        result.neededTemperature = NO_DATA_FLOAT;
                } else if (args[0].compareTo("needed_humid") == 0) {
                    if (args[1].compareToIgnoreCase("nan") != 0)
                        result.neededHumidity = Float.parseFloat(args[1]);
                    else
                        result.neededHumidity = NO_DATA_FLOAT;
                } else if (args[0].compareTo("rotations_per_day") == 0) {
                    result.rotationsPerDay = Integer.parseInt(args[1]);
                } else if (args[0].compareTo("number_of_programs") == 0) {
                    result.numberOfPrograms = Integer.parseInt(args[1]);
                } else if (args[0].compareTo("current_program") == 0) {
                    result.currentProgram = Integer.parseInt(args[1]);
                }
            }

            return result;
        } catch (Exception e) {
            return new IncubatorData();
        }
    }
}
