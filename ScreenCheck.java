import javax.swing.*;
import java.io.IOException;
import java.nio.file.*;
import java.time.Duration;
import java.util.*;
import java.util.Timer;
import java.util.concurrent.TimeUnit;

public class ScreenCheck {

    private final String CONFIG_PATH = "screen-check";
    private final Path PATH = Paths.get("screen-check.txt");
    private final Path LOG_PATH = Paths.get("screen-check.log");
    private final long ALLOWED_HOURS_IN_MIN;
    private final long REWRITE_HOURS_IN_MIN;
    private final long TIMER_MIN;
    private long startTime = System.currentTimeMillis();
    private long oldTimeInMin;
    private long lastModifiedTime;
    private boolean reset = false;
    private final static ScreenCheck SCREEN_CHECK = new ScreenCheck();
    private final static String LINE_SEP = System.lineSeparator();
    private final Timer TIMER = new Timer();
    private StringBuilder sb = new StringBuilder();
    private final static String OLD_TIME_STR = "oldTime:";
    private final static String SEP = ";";
    private final static String LAST_MOD_STR = "lastModified:";
    private String writeStr;
    private ResourceBundle configBundle = ResourceBundle.getBundle(CONFIG_PATH);
    private enum Constants { allowedHours, rewriteHours, timerMin};

    public static void main(String[] args) {
        SCREEN_CHECK.log("***Program start at " + new Date());
        SCREEN_CHECK.writeInitVars();
        SCREEN_CHECK.init();
        SCREEN_CHECK.startTimer(SCREEN_CHECK);
    }

    private ScreenCheck() {
        loadConfig();
        ALLOWED_HOURS_IN_MIN = getBundleLongVal(Constants.allowedHours.name()) * 60;
        REWRITE_HOURS_IN_MIN = getBundleLongVal(Constants.rewriteHours.name()) * 60;
        TIMER_MIN = Duration.ofMinutes(getBundleLongVal(Constants.timerMin.name())).toMillis() + 1000;
    }

    private long getBundleLongVal(String name) {
        return Long.valueOf(configBundle.getString(name));
    }

    private void loadConfig() {
        configBundle = ResourceBundle.getBundle(CONFIG_PATH);
    }

    private void writeInitVars() {
        log("ALLOWED_HOURS_IN_MIN = " + ALLOWED_HOURS_IN_MIN);
        log("REWRITE_HOURS_IN_MIN = " + REWRITE_HOURS_IN_MIN);
        log("TIMER_MIN = " + TIMER_MIN);
    }

    private String prepareFileString() {
        long currentM = getCurrentTimeMilli();
        long result = currentM - startTime;
        long min = TimeUnit.MILLISECONDS.toMinutes(result);
        log ("CurrentTimeMilli = " + getDateForLong(currentM));
        log ("startTime = " + getDateForLong(startTime));
        log ("result = " + result + ", min = " + min);
        return prepareFileString
                (oldTimeInMin + min);
    }

    private String getDateForLong(long val) {
        return val + ", as date " + new Date(val);
    }

    private String prepareFileString(long result) {
        return new StringBuilder().append(OLD_TIME_STR)
                .append(result)
                .append(SEP).append(LAST_MOD_STR).append(getCurrentTimeMilli()).toString();
    }

    public void init() {
        loadVars();
        checkIfShutDownRequired();
        writeToFile(makeWriteStr());
        flushLogs();
        resetVars ();
    }

    private void resetVars() {
        startTime = getCurrentTimeMilli();
        reset = false;
    }

    private String makeWriteStr() {
        String s = reset ? prepareFileString(0) : prepareFileString();
        log("Reset var [" + reset + "], writing to file [" + s + "]");

        return s;
    }

    private void startTimer(ScreenCheck screenCheck) {
        TIMER.schedule(new TimerTask() {

            @Override
            public void run() {
                log("Timer activated...");
                screenCheck.init();
            }
        }, TIMER_MIN, TIMER_MIN);
    }

    private void log(String s) {
        sb.append(s).append(LINE_SEP);
    }

    private long getCurrentTimeMilli() {
        return System.currentTimeMillis();
    }

    private void checkIfShutDownRequired() {
        long l = TimeUnit.MILLISECONDS.toMinutes(getCurrentTimeMilli() - lastModifiedTime);
        log("Time spent since last modified in min is " + l);
        if (l >= REWRITE_HOURS_IN_MIN) {
            reset = true;
            log("Reset required");
        } else if (oldTimeInMin >= ALLOWED_HOURS_IN_MIN) {
            showShutDownScreen();
            closeChromeWindows();
        }
    }

    private void closeChromeWindows() {
        try {
            log("Running screen check batch file");
            Runtime.getRuntime().exec("cmd /c start \"\" screen-check.bat");    
        }
        catch (IOException e) {
            log("Error in running batch file");
        }
    }

    private void showShutDownScreen() {
        JFrame frame = new JFrame();
        frame.setExtendedState(JFrame.MAXIMIZED_BOTH);
        frame.setUndecorated(true);
        frame.setVisible(true);
        frame.setAlwaysOnTop(true);
    }

    private void loadVars() {
        String val = readFile();
        oldTimeInMin = Long.valueOf(val.substring(OLD_TIME_STR.length(), val.indexOf(SEP)));
        lastModifiedTime = Long.valueOf(val.substring(val.indexOf(LAST_MOD_STR) + LAST_MOD_STR.length()));
        log("oldTimeInMin = " + oldTimeInMin + ", lastModifiedTime = " + getDateForLong(lastModifiedTime));
    }

    private String readFile() {
        String content;
        try {
            content = new String(Files.readAllBytes(PATH));
        } catch (IOException e) {
            content = prepareFileString(0);
            log("cannot read file, taking default. Writing to file [" + content + "]");
            writeToFile(content);
        }
        return content;

    }

    private void writeToFile(String content) {
        writeToFile(content, PATH, StandardOpenOption.TRUNCATE_EXISTING);
    }

    private void writeToFile(String content, Path path, StandardOpenOption option) {
        try {
            if (!Files.exists(path))
                Files.createFile(path);
            Files.write(path, content.getBytes(), option);
        } catch (IOException e) {
            log(Arrays.toString(e.getStackTrace()));
        }
    }

    private void flushLogs() {
        writeToFile(sb.toString(), LOG_PATH, StandardOpenOption.APPEND);
        sb = new StringBuilder();
    }

}
