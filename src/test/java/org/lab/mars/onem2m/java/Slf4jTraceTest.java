package org.lab.mars.onem2m.java;

import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Slf4jTraceTest {

    private static final Logger LOG = LoggerFactory
            .getLogger(Slf4jTraceTest.class);

    public static void throwException() {
        try {
            int a = 2 / 0;
            int b = a + 2;

        } catch (Exception e) {
            e.getCause();
            LOG.error("error because of:{}", e);
        }
    }

    @Test
    public void test() {
        throwException();
    }

}
