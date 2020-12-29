package com.sv.screencheck;

import com.sv.core.Utils;
import com.sv.core.config.DefaultConfigs;
import com.sv.core.logger.MyLogger;

import javax.swing.*;
import java.io.IOException;
import java.util.Date;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.TimeUnit;

import static com.sv.core.Constants.*;

public class ScreenCheck {

    enum Configs {
        AllowedMin, RewriteHours, TimerMin, OldTime, LastModified
    }

    private final MyLogger logger;
    private final DefaultConfigs configs;

    private long oldTimeInMin, lastModifiedTime, allowedMin, rewriteHours, timerMin;
    private final Timer TIMER = new Timer();

    private boolean reset = false;

    public static void main(String[] args) {
        new ScreenCheck();
    }

    private ScreenCheck() {
        logger = MyLogger.createLogger("screen-check.log");
        configs = new DefaultConfigs(logger, Utils.getConfigsAsArr(Configs.class));
        init();
        startTimer(this);
    }

    private String prepareFileString() {
        long currentM = getCurrentMillis();
        long min = convertToMin(currentM - lastModifiedTime);

        logger.log(String.format("CurrentTimeMilli = %s, lastModifiedTime = %s, result in min = %s, oldTimeInMin+min = %s",
                getDateForLong(currentM), getDateForLong(lastModifiedTime), min, (oldTimeInMin + min)));

        return prepareFileString(oldTimeInMin + min);
    }

    private String prepareFileString(long result) {
        return new StringBuilder().append("oldTime:")
                .append(result)
                .append(SEMI_COLON).append("lastModified:").append(getCurrentMillis()).toString();
    }

    private void saveConfig() {
        configs.saveConfig(this);
    }

    private String getDateForLong(long val) {
        return val + " [" + new Date(val) + "]";
    }

    public void init() {
        allowedMin = configs.getLongConfig(Configs.AllowedMin.name());
        rewriteHours = configs.getLongConfig(Configs.RewriteHours.name());
        timerMin = configs.getLongConfig(Configs.TimerMin.name());
        oldTimeInMin = configs.getIntConfig(Configs.OldTime.name());
        lastModifiedTime = configs.getLongConfig(Configs.LastModified.name());
        shutDownRequired();
        resetVars();
    }

    private void resetVars() {
        reset = false;
    }

    private void startTimer(ScreenCheck screenCheck) {
        long timer_min = TimeUnit.MINUTES.toMillis(configs.getIntConfig(Configs.TimerMin.name()));

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
        if (l >= convertHoursToMin(Configs.RewriteHours)) {
            reset = true;
        }
        saveConfig();
        if (reset) {
            logger.log("Resetting oldTimeInMin to 0");
            oldTimeInMin = 0;
        }
        logger.log("Reset required: " + reset);

        if (oldTimeInMin >= configs.getLongConfig(Configs.AllowedMin.name())) {
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

    private long convertHoursToMin(Configs config) {
        return TimeUnit.HOURS.toMinutes(configs.getIntConfig(config.name()));
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

    public String getAllowedMin() {
        return allowedMin + "";
    }

    public String getRewriteHours() {
        return rewriteHours + "";
    }

    public String getTimerMin() {
        return timerMin + "";
    }

    public String getLastModified() {
        return lastModifiedTime + "";
    }

    public String getOldTime() {
        return oldTimeInMin + "";
    }
}
