package org.lab.mars.onem2m.java;

import java.util.concurrent.TimeUnit;

public class VolatileTest {
    static class Person {
        static Boolean stopRequested = false;
    }

    private static Boolean stopRequested = false; // value: false

    public static void main(String... args) throws InterruptedException {

        Thread backgroundThread = new Thread(new Runnable() {

            @Override
            public void run() {
                int i = 0;
                synchronized (Person.class) {
                    while (!Person.stopRequested) {
                        try {
                            Person.class.wait();
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }
                }

            }
        });

        backgroundThread.start();

        TimeUnit.SECONDS.sleep(1);
        new Thread(new Runnable() {

            @Override
            public void run() {
                int i = 0;
                synchronized (Person.class) {
                    Person.stopRequested = true;
                    Person.class.notifyAll();
                }

            }
        }).start();
        ;
    }

}
