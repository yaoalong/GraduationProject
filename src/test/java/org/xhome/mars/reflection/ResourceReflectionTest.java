package org.xhome.mars.reflection;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import org.junit.Test;
import org.lab.mars.onem2m.reflection.Person;
import org.lab.mars.onem2m.reflection.ResourceReflection;
import org.lab.mars.onem2m.server.M2mDataNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ResourceReflectionTest {
    private static final Logger LOG = LoggerFactory
            .getLogger(ResourceReflectionTest.class);

    @Test
    public void testSerialize() {
        Person person = new Person();
        person.setId(11);
        person.setName("yaoalong");
        Map<String, Object> map = new HashMap<String, Object>();
        map.put("yaoalong", "name");
        for (Entry<String, Object> entry : map.entrySet()) {

            System.out.println(entry.getKey());
            System.out.println(entry.getValue());
            LOG.info("error:{}", entry.getKey());
        }

    }

    @Test
    public void testSerialToByte() {
        M2mDataNode m2mDataNode = new M2mDataNode();
        m2mDataNode.setId(11111 + "");
        m2mDataNode.setLabel(0);
        m2mDataNode.setZxid(999L);
        m2mDataNode.setData(1331);
        byte[] bytes = ResourceReflection.serializeKryo(m2mDataNode);
    }

}
