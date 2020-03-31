package com.company;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.concurrent.TimeUnit;

public class Main {
    public static void main(String[] args) throws Exception {
        long start = 0, time;
        if(args.length != 1) {
            System.out.println("Missing URL argument");
            System.exit(0);
        }

        try {
            URL url = new URL(args[0]);
            URLConnection conn = url.openConnection();
            start = System.nanoTime();
            conn.connect();
        } catch (MalformedURLException e) {
            System.out.println("Invalid URL Format");
            System.exit(0);
        } catch (IOException e) {
            System.out.println("Failed connect to the URL server");
            System.exit(0);
        }

        time = System.nanoTime() - start;
        System.out.println(time);
    }
}
