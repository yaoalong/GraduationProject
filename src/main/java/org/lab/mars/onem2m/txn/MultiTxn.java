// File generated by hadoop record compiler. Do not edit.
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

package org.lab.mars.onem2m.txn;

import org.lab.mars.onem2m.jute.BinaryInputArchive;
import org.lab.mars.onem2m.jute.BinaryOutputArchive;
import org.lab.mars.onem2m.jute.CsvOutputArchive;
import org.lab.mars.onem2m.jute.InputArchive;
import org.lab.mars.onem2m.jute.OutputArchive;
import org.lab.mars.onem2m.jute.Record;
public class MultiTxn implements Record {
  private java.util.List<Txn> txns;
  public MultiTxn() {
  }
  public MultiTxn(
        java.util.List<Txn> txns) {
    this.txns=txns;
  }
  public java.util.List<Txn> getTxns() {
    return txns;
  }
  public void setTxns(java.util.List<Txn> m_) {
    txns=m_;
  }
  public void serialize(OutputArchive a_, String tag) throws java.io.IOException {
    a_.startRecord(this,tag);
    a_.endRecord(this,tag);
  }
  public void deserialize(InputArchive a_, String tag) throws java.io.IOException {
    a_.startRecord(tag);
   
    a_.endRecord(tag);
}
  public String toString() {
    try {
      java.io.ByteArrayOutputStream s =
        new java.io.ByteArrayOutputStream();
      CsvOutputArchive a_ = 
        new CsvOutputArchive(s);
      a_.startRecord(this,"");
      a_.endRecord(this,"");
      return new String(s.toByteArray(), "UTF-8");
    } catch (Throwable ex) {
      ex.printStackTrace();
    }
    return "ERROR";
  }
  public void write(java.io.DataOutput out) throws java.io.IOException {
    BinaryOutputArchive archive = new BinaryOutputArchive(out);
    serialize(archive, "");
  }
  public void readFields(java.io.DataInput in) throws java.io.IOException {
    BinaryInputArchive archive = new BinaryInputArchive(in);
    deserialize(archive, "");
  }
  public int compareTo (Object peer_) throws ClassCastException {
    throw new UnsupportedOperationException("comparing MultiTxn is unimplemented");
  }
  public boolean equals(Object peer_) {
    if (!(peer_ instanceof MultiTxn)) {
      return false;
    }
    if (peer_ == this) {
      return true;
    }
    MultiTxn peer = (MultiTxn) peer_;
    boolean ret = false;
    ret = txns.equals(peer.txns);
    if (!ret) return ret;
     return ret;
  }
  public int hashCode() {
    int result = 17;
    int ret;
    ret = txns.hashCode();
    result = 37*result + ret;
    return result;
  }
  public static String signature() {
    return "LMultiTxn([LTxn(iB)])";
  }
}
