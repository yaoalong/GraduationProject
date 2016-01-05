package org.lab.mars.onem2m.reflection;

import java.util.Map;

/**
 * Author:yaoalong.
 * Date:2015/12/29.
 * Email:yaoalong@foxmail.com
 */
public class Test {

    public static void main(String args[]){
        Person person = new Person();
        person.setId(11);
        person.setName("uestc");
        Map<String, Object> map = ResourceReflection.serialize(person);
        for(Map.Entry entry:map.entrySet()){
            System.out.println("key "+entry.getKey());
            System.out.println("value: "+entry.getValue());
        }
        Person person1 = ResourceReflection.deserialize(Person.class, map);
        System.out.println("id:" + person1.getId());
        System.out.println("name: " + person1.getName());
    }
}
