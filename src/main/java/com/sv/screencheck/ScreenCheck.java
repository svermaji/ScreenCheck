package com.sv.screencheck;

import com.sv.core.Utils;
import com.sv.core.config.DefaultConfigs;
import com.sv.core.logger.MyLogger;

import javax.swing.*;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.TimeUnit;

public class ScreenCheck {

    enum Configs {
        AllowedMin, RewriteHours, TimerMin, OldTime, LastModified
    }

    private final MyLogger logger;
    private final DefaultConfigs configs;
    private final Timer TIMER = new Timer();

    private long oldTimeInMin, lastModifiedTime, allowedMin, rewriteHours, timerMin;
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

    private void saveConfig() {
        configs.saveConfig(this);
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

    private void shutDownRequired() {
        // only consider if time diff is nearby value of TimerMin
        long diffMin = Utils.getTimeDiffMin(lastModifiedTime);
        logger.log("LastModifiedTime difference in min " + Utils.addBraces(diffMin));
        if (diffMin <= timerMin) {
            oldTimeInMin = oldTimeInMin + diffMin;
        }
        lastModifiedTime = Utils.getNowMillis();

        saveConfig();

        long rewriteMins = TimeUnit.HOURS.toMinutes(rewriteHours);
        reset = oldTimeInMin >= rewriteMins
                || diffMin >= rewriteMins;
        logger.log("Reset required " + Utils.addBraces(reset));
        if (reset) {
            logger.log("Resetting oldTimeInMin to 0");
            oldTimeInMin = 0;
        }

        boolean result = oldTimeInMin >= allowedMin;
        logger.log("Shutdown required: " + Utils.addBraces(result));
        if (result) {
            showShutDownScreen();
            runExitCommand();
        }
    }

    private void runExitCommand() {
        Utils.runCmd("screen-check.bat", logger);
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
