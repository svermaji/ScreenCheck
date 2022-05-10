package com.sv.screencheck;


import com.sv.core.Utils;
import com.sv.core.config.DefaultConfigs;
import com.sv.core.logger.MyLogger;
import com.sv.email.Email;
import com.sv.email.EmailDetails;

import javax.swing.*;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.TimeUnit;

public class ScreenCheck {

    enum Configs {
        AllowedMin, RewriteHours, TimerMin, OldTime, LastModified, SendEmail
    }

    private final MyLogger logger;
    private final DefaultConfigs configs;
    private final Timer TIMER = new Timer();

    private long oldTimeInMin, lastModifiedTime, allowedMin, rewriteHours, timerMin;
    private boolean reset = false, shutdownReq = false, sendEmail;
    private EmailDetails details;
    private JFrame frame = null;

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
        sendEmail = configs.getBooleanConfig(Configs.SendEmail.name());
        String email = "temptempac1@gmail.com";
        details = new EmailDetails(email, "Screen check status: " + Utils.getFormattedDate());
        shutDownRequired();
        resetVars();
    }

    private void resetVars() {
        reset = false;
        shutdownReq = false;
    }

    private void startTimer(ScreenCheck screenCheck) {
        long timer_min = TimeUnit.MINUTES.toMillis(configs.getIntConfig(Configs.TimerMin.name()));

        TIMER.schedule(new TimerTask() {
            @Override
            public void run() {
                logger.info("Timer activated...");
                screenCheck.init();
            }
        }, timer_min, timer_min);
    }

    private void shutDownRequired() {
        // only consider if time diff is nearby value of TimerMin
        long diffMin = Utils.getTimeDiffMin(lastModifiedTime);
        logger.info("LastModifiedTime [" + Utils.getFormattedDate(lastModifiedTime)
                + "] difference in min " + Utils.addBraces(diffMin));
        if (diffMin <= timerMin) {
            oldTimeInMin = oldTimeInMin + diffMin;
        }
        lastModifiedTime = Utils.getNowMillis();

        long rewriteMins = TimeUnit.HOURS.toMinutes(rewriteHours);
        reset = oldTimeInMin >= rewriteMins
                || diffMin >= rewriteMins;
        logger.info("Reset required " + Utils.addBraces(reset));
        if (reset) {
            logger.info("Resetting oldTimeInMin to 0");
            oldTimeInMin = 0;
        }

        saveConfig();
        shutdownReq = oldTimeInMin >= allowedMin;
        logger.info("Shutdown required: " + Utils.addBraces(shutdownReq));
        if (shutdownReq) {
            showShutDownScreen();
        }
        sendEmail();
    }

    private void sendEmail() {
        logger.info("Send email check: " + Utils.addBraces(sendEmail));
        if (sendEmail) {
            String line = System.lineSeparator();
            long dt = lastModifiedTime + TimeUnit.MINUTES.toMillis(allowedMin - oldTimeInMin);
            String body = new StringBuilder()
                    .append("Hi")
                    .append(line)
                    .append(line)
                    .append("Status for screen check on: ")
                    .append(Utils.getHostname(logger))
                    .append(line)
                    .append(line)
                    .append("Machine time: ")
                    .append(Utils.getFormattedDate(lastModifiedTime))
                    .append(line)
                    .append("Estimated shutdown time: ")
                    .append(Utils.getFormattedDate(dt))
                    .append(line)
                    .append("Time spent till now in minutes is: ")
                    .append(oldTimeInMin)
                    .append(", of limit: ")
                    .append(getAllowedMin())
                    .append(line)
                    .append("Reset flag value: ")
                    .append(reset)
                    .append(line)
                    .append("Shutdown flag value: ")
                    .append(shutdownReq)
                    .append(line)
                    .append(line)
                    .append("Thanks")
                    .append(line)
                    .append("ScreenCheck Team").toString();
            details.setBody(body);
            Email.send(details, true);
        }
    }

    private void runAppCommand() {
        Utils.runCmd("screen-check.bat", logger);
        saveConfig();
    }

    private void showShutDownScreen() {
        if (frame == null) {
            frame = new JFrame();
            frame.setExtendedState(JFrame.MAXIMIZED_BOTH);
            frame.setUndecorated(true);
            frame.setVisible(true);
            frame.setAlwaysOnTop(true);
            runAppCommand();
        }
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

    public String getSendEmail() {
        return sendEmail + "";
    }
}
