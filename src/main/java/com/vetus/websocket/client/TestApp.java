package com.vetus.websocket.client;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.text.SimpleDateFormat;
import java.util.Calendar;

public class TestApp {

    public static void main(String[] args) throws InterruptedException {
        for (int i = 0; i < 12; i++) {
            RunnableDemo runnableDemo = new RunnableDemo("Thread" + i);
            runnableDemo.start();
        }

        Thread.sleep(10000);
    }

    static class RunnableDemo implements Runnable {
        private Thread t;
        private String threadName;

        RunnableDemo(String name) {

            threadName = name;
            System.out.println("Creating " + threadName);

            try {
                if (Files.exists(Paths.get("/tmp/" + threadName))) {
                    Files.delete(Paths.get("/tmp/" + threadName));
                }
                Files.createFile(Paths.get("/tmp/" + threadName));
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        public void run() {
            System.out.println("Running " + threadName);
            try {
                // open websocket
                final WebsocketClientEndpoint clientEndPoint = new WebsocketClientEndpoint(new URI("ws://localhost:8080/time/" + threadName + "/websocket"), threadName);

                // add listener
                clientEndPoint.addMessageHandler(new WebsocketClientEndpoint.MessageHandler() {
                    public void handleMessage(String message) {
                        System.out.println(threadName + message);
                        printFile(message);
                    }
                });

                // send message to websocket
                clientEndPoint.sendMessage("Message from thread - " + threadName + "to user #Thread4#");

                // wait 5 seconds for messages from websocket
                Thread.sleep(2000);

            } catch (InterruptedException ex) {
                System.err.println("InterruptedException exception: " + ex.getMessage());
            } catch (URISyntaxException ex) {
                System.err.println("URISyntaxException exception: " + ex.getMessage());
            }
            System.out.println("Thread " + threadName + " exiting.");
        }

        public void start() {
            System.out.println("Starting " + threadName);
            if (t == null) {
                t = new Thread(this, threadName);
                t.start();
            }
        }

        private synchronized void printFile(String message) {
            BufferedWriter writer = null;
            try {
                Files.write(Paths.get("/tmp/" + threadName), (message + "\n").getBytes(), StandardOpenOption.APPEND);
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                try {
                    // Close the writer regardless of what happens...
                    writer.close();
                } catch (Exception e) {
                }
            }
        }

    }
}