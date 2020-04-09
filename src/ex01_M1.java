import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;

public class ex01_M1 {

    /*
     Returns the current value of the most precise available system timer, in nanoseconds.
     */
    public static long getCurrentTime() {
        return System.nanoTime();
    }

    public static long checkResponseTime(String url) {
        long start = 0;
        try {
            URL site = new URL(url);
            URLConnection conn = site.openConnection();
            start = getCurrentTime();
            conn.connect();
        } catch (MalformedURLException e) {
            System.out.println("Invalid URL Format");
            return 0;
        } catch (IOException e) {
            System.out.println("Failed connect to the URL server");
            return 0;
        }
        return getCurrentTime() - start;
    }

    public static void main(String[] args) {
        if(args.length != 1) {
            System.out.println("Missing URL argument");
            System.exit(0);
        }

        long responseTime = checkResponseTime(args[0]);
        long responseTimeInMilliseconds = responseTime / 1000;
        System.out.println(responseTimeInMilliseconds);

    }
}
