package org.lab.mars.onem2m.java;

import java.lang.reflect.Field;

import org.junit.Test;

public class TraversalAllFields {
    public static void getObjAttr(Object obj) {
        // 获取对象obj的所有属性域
        Field[] fields = obj.getClass().getDeclaredFields();

        for (Field field : fields) {
            // 对于每个属性，获取属性名
            String varName = field.getName();
            try {
                boolean access = field.isAccessible();
                if (!access)
                    field.setAccessible(true);

                // 从obj中获取field变量
                Object o = field.get(obj);
                System.out.println("变量： " + varName + " = " + o);

                if (!access)
                    field.setAccessible(false);
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }
    }

    public static void printAllFields(PersonDO object) {

        Field[] fields = object.getClass().getDeclaredFields();
        for (Field field : fields) {
            String name = field.getName();
            try {
                boolean isAccess = field.isAccessible();
                if (!isAccess)
                    field.setAccessible(true);
                String value = (String) field.get(name);
                System.out.println(value + "");
                if (!isAccess)
                    field.setAccessible(false);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

    }

    @Test
    public void testPrintAllFields() {

        PersonDO personDO = new PersonDO();
        personDO.setId(1111);
        personDO.setName("mr.yao");
        getObjAttr(personDO);
    }

}
