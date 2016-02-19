package org.lab.mars.onem2m.reflection;

import java.math.BigDecimal;
import java.nio.ByteBuffer;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.lab.mars.onem2m.reflection.KryoConfiguration.ClassAccess;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;
import com.google.common.collect.Sets;

/**
 * Author:yaoalong. Date:2015/12/29. Email:yaoalong@foxmail.com
 */
public class ResourceReflection {

    private static Set<Class> defaultClz;

    static {
        defaultClz = Sets.newHashSet(Integer.class, Float.class, Double.class,
                Boolean.class, Long.class, BigDecimal.class, String.class,
                Date.class);
    }

    protected static final ThreadLocal<Kryo> kryo = new ThreadLocal<Kryo>() {
        protected com.esotericsoftware.kryo.Kryo initialValue() {
            Kryo k = new Kryo();
            KryoConfiguration.configure(k);
            return k;
        }
    };

    public static Object deserializeKryo(byte[] data) {
        try (Input in = new Input(data)) {
            return kryo.get().readClassAndObject(in);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public static Object deserializeKryo(byte[] data, int offset, int count) {
        try (Input in = new Input(data, offset, count)) {
            return kryo.get().readClassAndObject(in);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public static byte[] serializeKryo(Object obj) {
        try (Output out = new Output(4096, -1)) {
            try {
                kryo.get().writeClassAndObject(out, obj);
                return out.toBytes();
            } catch (Exception e) {
                e.printStackTrace();
                return null;
            }
        }
    }

    @SuppressWarnings("unchecked")
    public static <R> R deserialize(Class<R> cls, Map<String, Object> result) {
        ClassAccess clzAccess = KryoConfiguration.get(cls);
        if (clzAccess == null)
            return null;
        R instance = (R) clzAccess.constructorAccess.newInstance();
        if (result != null)
            result.forEach((k, v) -> {
                if (clzAccess.fieldIndex.containsKey(k)) {
                    Object fieldObj = v;
                    if (v instanceof ByteBuffer) {
                        ByteBuffer _v = (ByteBuffer) v;
                        fieldObj = deserializeKryo(_v.array(), _v.position(),
                                _v.remaining());
                    }
                    clzAccess.fieldAccess.set(instance,
                            clzAccess.fieldIndex.get(k), fieldObj);
                }
            });
        return instance;
    }

    public static Map<String, Object> serialize(Object instance) {
        Map<String, Object> result = new HashMap<>();
        Class<? extends Object> clz = instance.getClass();
        ClassAccess clzAccess = KryoConfiguration.get(clz);
        if (clzAccess == null) {
            return null;
        }
        clzAccess.fieldIndex.forEach((field, index) -> {
            Object fieldObj = clzAccess.fieldAccess.get(instance, index);
            if (fieldObj != null)
                result.put(field,
                        defaultClz.contains(fieldObj.getClass()) ? fieldObj
                                : ByteBuffer.wrap(serializeKryo(fieldObj)));
        });
        return result;
    }
}
