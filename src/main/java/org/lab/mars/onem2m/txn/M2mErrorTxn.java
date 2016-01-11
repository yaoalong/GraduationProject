package org.lab.mars.onem2m.txn;

import org.lab.mars.onem2m.jute.M2mBinaryInputArchive;
import org.lab.mars.onem2m.jute.M2mBinaryOutputArchive;
import org.lab.mars.onem2m.jute.M2mCsvOutputArchive;
import org.lab.mars.onem2m.jute.M2mInputArchive;
import org.lab.mars.onem2m.jute.M2mOutputArchive;
import org.lab.mars.onem2m.jute.M2mRecord;

public class M2mErrorTxn implements M2mRecord{

	/**
	 * 
	 */
	private static final long serialVersionUID = 5219755399710575386L;

	 private int err;
	  public M2mErrorTxn() {
	  }
	  public M2mErrorTxn(
	        int err) {
	    this.err=err;
	  }
	  public int getErr() {
	    return err;
	  }
	  public void setErr(int m_) {
	    err=m_;
	  }
	  public void serialize(M2mOutputArchive a_, String tag) throws java.io.IOException {
	    a_.startRecord(this,tag);
	    a_.writeInt(err,"err");
	    a_.endRecord(this,tag);
	  }
	  public void deserialize(M2mInputArchive a_, String tag) throws java.io.IOException {
	    a_.startRecord(tag);
	    err=a_.readInt("err");
	    a_.endRecord(tag);
	}
	  public String toString() {
	    try {
	      java.io.ByteArrayOutputStream s =
	        new java.io.ByteArrayOutputStream();
	      M2mCsvOutputArchive a_ = 
	        new M2mCsvOutputArchive(s);
	      a_.startRecord(this,"");
	    a_.writeInt(err,"err");
	      a_.endRecord(this,"");
	      return new String(s.toByteArray(), "UTF-8");
	    } catch (Throwable ex) {
	      ex.printStackTrace();
	    }
	    return "ERROR";
	  }
	  public void write(java.io.DataOutput out) throws java.io.IOException {
		  M2mBinaryOutputArchive archive = new M2mBinaryOutputArchive(out);
	    serialize(archive, "");
	  }
	  public void readFields(java.io.DataInput in) throws java.io.IOException {
		  M2mBinaryInputArchive archive = new M2mBinaryInputArchive(in);
	    deserialize(archive, "");
	  }
	  public int compareTo (Object peer_) throws ClassCastException {
         int ret=1;
	     return ret;
	  }
	  public boolean equals(Object peer_) {
         boolean ret=false;
	    if (!ret) return ret;
	     return ret;
	  }
	  public int hashCode() {
	    int result = 17;
	    int ret;
	    ret = (int)err;
	    result = 37*result + ret;
	    return result;
	  }
	  public static String signature() {
	    return "LErrorTxn(i)";
	  }

}
