package com.sv.screencheck;


import com.sv.core.Utils;
import com.sv.core.config.DefaultConfigs;
import com.sv.core.exception.AppException;
import com.sv.core.logger.MyLogger;
import com.sv.email.Email;
import com.sv.email.EmailDetails;

import javax.swing.*;
import java.net.ConnectException;
import java.time.LocalTime;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.TimeUnit;

public class ScreenCheck {

    enum Configs {
        AllowedMin, RewriteHours, TimerMin, OldTime, LastModified, SendEmail, ActionTime, ActionMode
    }

    enum ActionMode {
        time, interval
    }

    private final MyLogger logger;
    private final DefaultConfigs configs;
    private final Timer TIMER = new Timer();

    private long oldTimeInMin, lastModifiedTime, allowedMin, rewriteHours, timerMin;
    private String actionTime, actionMode;
    private boolean reset = false, takeAction = false, sendEmail;
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
        // in format oh HH:mm, action will be taken at that time
        actionTime = configs.getConfig(Configs.ActionTime.name());
        actionMode = configs.getConfig(Configs.ActionMode.name());
        String email = "temptempac1@gmail.com";
        details = new EmailDetails(email, "Screen check status: " + Utils.getFormattedDate());
        takeAction();
        resetVars();
    }

    private void resetVars() {
        reset = false;
        takeAction = false;
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

    private boolean isTimeMode() {
        return actionMode.equals(ActionMode.time.name());
    }

    private boolean isIntervalMode() {
        return actionMode.equals(ActionMode.interval.name());
    }

    private void takeAction() {
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
        takeAction = isIntervalMode() && oldTimeInMin >= allowedMin;
        if (!takeAction) {
            takeAction = isTimeMode() && isTimeForAction();
        }
        logger.info("takeAction " + Utils.addBraces(takeAction));
        if (takeAction) {
            startAction();
        }
        sendEmail();
    }

    private boolean isTimeForAction() {
        String[] time = actionTime.split(":");
        boolean result = false;
        LocalTime nw = LocalTime.now();
        if (time.length == 1) {
            result = nw.getHour() >= Integer.parseInt(time[0]);
        }
        if (time.length > 1) {
            result = nw.getHour() >= Integer.parseInt(time[0]) &&
                    nw.getMinute() >= Integer.parseInt(time[1]);
        }
        logger.info("Result for [" + nw.getHour() + "], [" + nw.getMinute() + "] is [" + result + "]");
        return result;
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
                    .append(takeAction)
                    .append(line)
                    .append(line)
                    .append("Thanks")
                    .append(line)
                    .append("ScreenCheck Team").toString();
            details.setBody(body);
            try {
                Email.send(details, true);
            } catch (AppException e) {
                logger.error("Unable to send email. ", e);
            }
        }
    }

    private void runAppCommand() {
        Utils.runCmd("cmds/screen-check.bat", logger);
        saveConfig();
        System.exit(0);
    }

    private void startAction() {
        /*if (frame == null) {
            frame = new JFrame();
            frame.setExtendedState(JFrame.MAXIMIZED_BOTH);
            frame.setUndecorated(true);
            frame.setVisible(true);
            frame.setAlwaysOnTop(true);
        }*/
        runAppCommand();
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

    public String getActionTime() {
        return actionTime + "";
    }

    public String getActionMode() {
        return actionMode;
    }
}
