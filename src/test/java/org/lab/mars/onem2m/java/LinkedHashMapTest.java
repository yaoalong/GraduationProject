package org.lab.mars.onem2m.java;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map.Entry;

import org.junit.Test;

public class LinkedHashMapTest {

    public static void forEachList(HashMap<String, Integer> map) {
        for (Entry<String, Integer> entry : map.entrySet()) {
            System.out.print("key:" + entry.getKey());
            System.out.println("   value:" + entry.getValue());

        }
    }

    @Test
    public void testPut() {
        final LinkedHashMap<String, Integer> linkedHashMap = new LinkedHashMap<String, Integer>(
                16, 0.75f, true) {
            /**
                     * 
                     */
            private static final long serialVersionUID = 3033453005289310613L;

            @Override
            protected boolean removeEldestEntry(Entry<String, Integer> eldest) {
                if (size() > 10) {
                    return true;
                }
                return false;
            }
        };
        for (int i = 0; i < 10; i++) {
            linkedHashMap.put(i + ":Random", i);
        }
        forEachList(linkedHashMap);
        linkedHashMap.get(3 + ":Random");
        System.out.println("***************");
        forEachList(linkedHashMap);

        linkedHashMap.put("10" + ":Random", 10);
        System.out.println("***************");
        forEachList(linkedHashMap);

    }

}
