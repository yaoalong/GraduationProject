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

package org.lab.mars.onem2m.server;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;

import org.lab.mars.onem2m.jute.BinaryOutputArchive;
import org.lab.mars.onem2m.jute.Record;

public class ByteBufferOutputStream extends OutputStream {
    ByteBuffer bb;
    public ByteBufferOutputStream(ByteBuffer bb) {
        this.bb = bb;
    }
    @Override
    public void write(int b) throws IOException {
        bb.put((byte)b);
    }
    @Override
    public void write(byte[] b) throws IOException {
        bb.put(b);
    }
    @Override
    public void write(byte[] b, int off, int len) throws IOException {
        bb.put(b, off, len);
    }
    static public void record2ByteBuffer(Record record, ByteBuffer bb)
    throws IOException {
        BinaryOutputArchive oa;
        oa = BinaryOutputArchive.getArchive(new ByteBufferOutputStream(bb));
        record.serialize(oa, "request");
    }
}
