package org.lab.mars.onem2m.java;

import java.util.ArrayList;
import java.util.List;

import org.junit.Test;

public class ListRemoveTest {

    @Test
    public void testRemove() {
        List<String> arrayList = new ArrayList<String>();
        for (int i = 0; i < 10; i++) {
            arrayList.add("result" + i);
        }
        for (int i = 0; i < arrayList.size(); i++) {
            String obj = arrayList.get(i);

            if (obj.equals("result" + 4)) {
                arrayList.remove(arrayList.indexOf(obj));
            }
        }

    }
}
