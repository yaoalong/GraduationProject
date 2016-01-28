/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.lab.mars.onem2m.jute;

import java.io.IOException;
import java.io.OutputStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashMap;

/**
 * Front-end for serializers. Also serves as a factory for serializers.
 *
 */
public class M2mRecordWriter {

    private M2mOutputArchive archive;

    static HashMap<String, Method> constructFactory() {
        HashMap<String, Method> factory = new HashMap<String, Method>();
        @SuppressWarnings("rawtypes")
        Class[] params = { OutputStream.class };
        try {
            factory.put("binary", M2mBinaryOutputArchive.class
                    .getDeclaredMethod("getArchive", params));
            factory.put("csv", M2mCsvOutputArchive.class.getDeclaredMethod(
                    "getArchive", params));
            factory.put("xml", M2mXmlOutputArchive.class.getDeclaredMethod(
                    "getArchive", params));
        } catch (SecurityException ex) {
            ex.printStackTrace();
        } catch (NoSuchMethodException ex) {
            ex.printStackTrace();
        }
        return factory;
    }

    static private HashMap<String, Method> archiveFactory = constructFactory();

    static private M2mOutputArchive createArchive(OutputStream out,
            String format) throws IOException {
        Method factory = (Method) archiveFactory.get(format);
        if (factory != null) {
            Object[] params = { out };
            try {
                return (M2mOutputArchive) factory.invoke(null, params);
            } catch (IllegalArgumentException ex) {
                ex.printStackTrace();
            } catch (InvocationTargetException ex) {
                ex.printStackTrace();
            } catch (IllegalAccessException ex) {
                ex.printStackTrace();
            }
        }
        return null;
    }

    /**
     * Creates a new instance of RecordWriter
     * 
     * @param out
     *            Output stream where the records will be serialized
     * @param format
     *            Serialization format ("binary", "xml", or "csv")
     */
    public M2mRecordWriter(OutputStream out, String format) throws IOException {
        archive = createArchive(out, format);
    }

    /**
     * Serialize a record
     * 
     * @param r
     *            record to be serialized
     */
    public void write(M2mRecord r) throws IOException {
        r.serialize(archive, "");
    }
}
