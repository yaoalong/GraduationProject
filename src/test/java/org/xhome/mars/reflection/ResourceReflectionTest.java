package org.xhome.mars.reflection;

import java.util.Map;
import java.util.Map.Entry;

import org.junit.Test;
import org.lab.mars.onem2m.reflection.Person;
import org.lab.mars.onem2m.reflection.ResourceReflection;

public class ResourceReflectionTest {
	
	@Test
	public void testSerialize(){
		Person person=new Person();
		person.setId(11);
		person.setName("yaoalong");
		Map<String, Object> map=ResourceReflection.serialize(person);
		for(Entry<String, Object> entry:map.entrySet()){
			System.out.println(entry.getKey());
			System.out.println(entry.getValue());
		}
		
	}

}
