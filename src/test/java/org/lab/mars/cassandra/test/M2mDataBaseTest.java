package org.lab.mars.cassandra.test;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import org.junit.Test;
import org.lab.mars.onem2m.ZooDefs.OpCode;
import org.lab.mars.onem2m.java.TraversalAllFields;
import org.lab.mars.onem2m.jute.M2mBinaryOutputArchive;
import org.lab.mars.onem2m.server.M2mDataNode;
import org.lab.mars.onem2m.server.cassandra.impl.M2MDataBaseImpl;
import org.lab.mars.onem2m.txn.M2mSetDataTxn;
import org.lab.mars.onem2m.txn.M2mTxnHeader;

public class M2mDataBaseTest {
    M2MDataBaseImpl m2mDataBase = new M2MDataBaseImpl(false, "tests",
            "onem2m1", "192.168.10.133");

    @Test
    public void test() {
        m2mDataBase.truncate((long) 4);
    }

    @Test
    public void testRetrieve() {

        M2mDataNode m2mDataNode = m2mDataBase.retrieve("3333431");
        TraversalAllFields.getObjAttr(m2mDataNode);
    }

    @Test
    public void testDelete() {
        m2mDataBase.delete("3333431");
    }

    @Test
    public void testRetrieve1() {

        List<M2mDataNode> m2mDataNodes = m2mDataBase.retrieve(634L);
        for (M2mDataNode m2mDataNode : m2mDataNodes) {

            TraversalAllFields.getObjAttr(m2mDataNode);
        }
    }

    @Test
    public void testUpdate() {
        Map<String, Object> map = new HashMap<String, Object>();
        map.put("data", 100);
        m2mDataBase.update("3333432", map);
    }

    @Test
    public void testCreate() {
        Random random = new Random();
        for (int i = 0; i < 10; i++) {
            M2mDataNode m2mDataNode = new M2mDataNode();

            m2mDataNode.setZxid(Long.valueOf(140200 + "") + random.nextLong());
            m2mDataNode.setData(2);
            m2mDataNode.setId((3333430 + i) + "");
            m2mDataNode.setLabel(0);
            m2mDataNode.setValue(11L + i);
            m2mDataNode.setFlag(0);
            m2mDataBase.create(m2mDataNode);
        }

    }

    @Test
    public void testGetCertainValue() {
        List<M2mDataNode> m2mDataNodes = m2mDataBase.getCertainData(12L, 15L);
        for (M2mDataNode m2mDataNode : m2mDataNodes) {
            TraversalAllFields.getObjAttr(m2mDataNode);
        }
    }

    @Test
    public void testProcessTxn() throws IOException {
        M2mTxnHeader m2mTxnHeader = new M2mTxnHeader();
        m2mTxnHeader.setType(OpCode.setData);
        M2mSetDataTxn m2mSetDataTxn = new M2mSetDataTxn();
        m2mSetDataTxn.setPath("11111");
        M2mDataNode m2mDataNode = new M2mDataNode();
        m2mDataNode.setId(11111 + "");
        m2mDataNode.setLabel(0);
        m2mDataNode.setZxid(Long.valueOf("9999"));
        m2mDataNode.setData(1331);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        M2mBinaryOutputArchive boa = M2mBinaryOutputArchive.getArchive(baos);
        m2mDataNode.serialize(boa, "m2mDataNode");
        m2mSetDataTxn.setData(baos.toByteArray());
        m2mDataBase.processTxn(m2mTxnHeader, m2mSetDataTxn);

    }

    @Test
    public void testGetMaxZxid() {
        System.out.println(m2mDataBase.getMaxZxid());
    }

    @Test
    public void testMod() {
        System.out.println((-1 + 6) % 6);

    }
}
