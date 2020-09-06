package com.sv.screencheck;

import javax.swing.*;
import java.io.IOException;
import java.util.Date;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.TimeUnit;

public class ScreenCheck {

    private final MyLogger logger;
    private final DefaultConfigs configs;

    private long oldTimeInMin;
    private long lastModifiedTime;
    private final Timer TIMER = new Timer();

    private boolean reset = false;

    public static void main(String[] args) {
        new ScreenCheck();
    }

    private ScreenCheck() {
        logger = MyLogger.createLogger("screen-check.log");
        configs = new DefaultConfigs(logger);
        init();
        startTimer(this);
    }

    private String prepareFileString() {
        long currentM = getCurrentMillis();
        long result = currentM - lastModifiedTime;
        long min = convertToMin(result);

        logger.log(String.format("CurrentTimeMilli = %s, lastModifiedTime = %s, result = %s, min = %s, oldTimeInMin+min = %s",
                getDateForLong(currentM), getDateForLong(lastModifiedTime), result, min, (oldTimeInMin + min)));

        return prepareFileString(oldTimeInMin + min);
    }

    private String prepareFileString(long result) {
        return new StringBuilder().append("oldTime:")
                .append(result)
                .append(Utils.SEMICOLON).append("lastModified:").append(getCurrentMillis()).toString();
    }

    private void saveConfig() {
        configs.saveConfig(this);
    }

    private String getDateForLong(long val) {
        return val + ", as date " + new Date(val);
    }

    public void init() {
        readStateConfig();
        shutDownRequired();
        resetVars();
    }

    private void readStateConfig() {
        String state = configs.getConfig(DefaultConfigs.Config.STATE);
        String[] vals = state.split(";");
        oldTimeInMin = convertToLong(vals[0].split(Utils.COLON)[1]);
        lastModifiedTime = convertToLong(vals[1].split(Utils.COLON)[1]);
    }

    private void resetVars() {
        reset = false;
    }

    private void startTimer(ScreenCheck screenCheck) {
        long timer_min = TimeUnit.MINUTES.toMillis(convertToLong(DefaultConfigs.Config.TIMER_MIN));

        TIMER.schedule(new TimerTask() {
            @Override
            public void run() {
                logger.log("Timer activated...");
                screenCheck.init();
            }
        }, timer_min, timer_min);
    }

    private long getCurrentMillis() {
        return System.currentTimeMillis();
    }

    private void shutDownRequired() {
        long l = convertToMin(getCurrentMillis() - lastModifiedTime);
        logger.log("Time spent since last modified in min is " + l);
        if (l >= convertHoursToMin(DefaultConfigs.Config.REWRITE_HOURS)) {
            reset = true;
        }
        saveConfig();
        logger.log("Reset required: " + reset);

        if (oldTimeInMin >= convertHoursToMin(DefaultConfigs.Config.ALLOWED_HOURS)) {
            logger.log("Shutdown required: true");
            showShutDownScreen();
            runExitCommand();
        } else {
            logger.log("Shutdown required: false");
        }
    }

    private long convertToMin(long millis) {
        return TimeUnit.MILLISECONDS.toMinutes(millis);
    }

    private long convertHoursToMin(DefaultConfigs.Config config) {
        return TimeUnit.HOURS.toMinutes(convertToLong(config));
    }

    private long convertToLong(DefaultConfigs.Config config) {
        return convertToLong(configs.getConfig(config));
    }

    private long convertToLong(String str) {
        return Long.parseLong(str);
    }

    private void runExitCommand() {
        try {
            logger.log("Running screen check batch file");
            Runtime.getRuntime().exec("screen-check.bat");
        } catch (IOException e) {
            logger.error(e);
        }
        saveConfig();
    }

    private void showShutDownScreen() {
        JFrame frame = new JFrame();
        frame.setExtendedState(JFrame.MAXIMIZED_BOTH);
        frame.setUndecorated(true);
        frame.setVisible(true);
        frame.setAlwaysOnTop(true);
    }

    public String getAllowedHours() {
        return configs.getConfig(DefaultConfigs.Config.ALLOWED_HOURS);
    }

    public String getRewriteHours() {
        return configs.getConfig(DefaultConfigs.Config.REWRITE_HOURS);
    }

    public String getTimerMin() {
        return configs.getConfig(DefaultConfigs.Config.TIMER_MIN);
    }

    public String getState() {
        if (reset) {
            return prepareFileString(0);
        }

        return prepareFileString();
    }

}
