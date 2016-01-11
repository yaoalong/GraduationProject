package org.lab.mars.onem2m.server.cassandra.impl;

import static com.datastax.driver.core.querybuilder.QueryBuilder.eq;
import static com.datastax.driver.core.querybuilder.QueryBuilder.gt;
import static com.datastax.driver.core.querybuilder.QueryBuilder.gte;
import static com.datastax.driver.core.querybuilder.QueryBuilder.set;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.lab.mars.onem2m.ZooDefs.OpCode;
import org.lab.mars.onem2m.jute.M2mBinaryInputArchive;
import org.lab.mars.onem2m.jute.M2mBinaryOutputArchive;
import org.lab.mars.onem2m.jute.M2mRecord;
import org.lab.mars.onem2m.reflection.ResourceReflection;
import org.lab.mars.onem2m.server.DataTree.ProcessTxnResult;
import org.lab.mars.onem2m.server.M2mDataNode;
import org.lab.mars.onem2m.server.cassandra.interface4.M2MDataBase;
import org.lab.mars.onem2m.txn.M2mCreateTxn;
import org.lab.mars.onem2m.txn.M2mDeleteTxn;
import org.lab.mars.onem2m.txn.M2mSetDataTxn;
import org.lab.mars.onem2m.txn.M2mTxnHeader;

import com.datastax.driver.core.Cluster;
import com.datastax.driver.core.ColumnDefinitions;
import com.datastax.driver.core.Host;
import com.datastax.driver.core.Metadata;
import com.datastax.driver.core.ResultSet;
import com.datastax.driver.core.Row;
import com.datastax.driver.core.Session;
import com.datastax.driver.core.Statement;
import com.datastax.driver.core.querybuilder.Delete;
import com.datastax.driver.core.querybuilder.Insert;
import com.datastax.driver.core.querybuilder.QueryBuilder;
import com.datastax.driver.core.querybuilder.Select;
import com.datastax.driver.core.querybuilder.Update;

public class M2MDataBaseImpl implements M2MDataBase {
	static ByteArrayOutputStream baos = new ByteArrayOutputStream();
	static M2mBinaryOutputArchive boa = M2mBinaryOutputArchive.getArchive(baos);
	private String keyspace;
	private String table;
	private String node;
	private Cluster cluster;
	private Session session;
	private boolean clean = false;

	public M2MDataBaseImpl() {
		this(false, "mars", "onem2m", "127.0.0.1");
	}

	public M2MDataBaseImpl(boolean clean, String keyspace, String table,
			String node) {
		this.clean = clean;
		this.keyspace = keyspace;
		this.table = table;
		this.node = node;
		connect();
	}

	public void connect() {
		cluster = Cluster.builder().addContactPoint(node).build();
		Metadata metadata = cluster.getMetadata();
		System.out.printf("Connected to cluster: %s\n",
				metadata.getClusterName());
		for (Host host : metadata.getAllHosts()) {
			System.out.printf("Datacenter: %s; Host: %s; Rack: %s\n",
					host.getDatacenter(), host.getAddress(), host.getRack());
		}
		session = cluster.connect();
		if (clean) {
			session.execute("use " + keyspace + ";");
			session.execute("truncate " + table + ";");
		}
	}

	/**
	 * 检索特定的key
	 */
	@Override
	public M2mDataNode retrieve(String key) {
		try {
			Select.Selection selection = query().select();
			Select select = selection.from(keyspace, table);
			select.where(eq("id",Integer.valueOf(key)));
			select.allowFiltering();
			ResultSet resultSet = session.execute(select);
			if (resultSet == null) {
				return null;
			}

			Map<String, Object> result = new HashMap<String, Object>();
			for (Row row : resultSet.all()) {
				ColumnDefinitions columnDefinitions = resultSet
						.getColumnDefinitions();
				columnDefinitions.forEach(d -> {
					String name = d.getName();
					Object object = row.getObject(name);
					result.put(name, object);
				});
			}

			M2mDataNode m2mDataNode = ResourceReflection.deserialize(
					M2mDataNode.class, result);
			return m2mDataNode;

		} catch (Exception ex) {
			ex.printStackTrace();
			return null;
		}
	}

	/**
	 * 插入数据
	 */
	@Override
	public Long create(Object object) {
		try {
			Map<String,Object> map=ResourceReflection.serialize(object);
			Insert insert = query().insertInto(keyspace, table);
			map.forEach(insert::value);
			session.execute(insert);
		} catch (Exception e) {
			e.printStackTrace();
			return Long.valueOf(0);
		}

		return Long.valueOf(1);
	}

	@Override
	public Long delete(String key) {
		try {
			Statement delete = query().delete().from(keyspace, table)
					.where(eq("id", Integer.valueOf(key)));
			session.execute(delete);
		} catch (Exception ex) {
			ex.printStackTrace();
			return Long.valueOf(0);
		}
		return Long.valueOf(1);
	}

	@Override
	public Long update(String key, Map<String, Object> updated) {
		try {
			M2mDataNode m2mDataNode=retrieve(key);
			Update update = query().update(keyspace, table);
			update.with(set("data", updated.get("data")));
			update.where(eq("id", Integer.valueOf(key))).and(eq("zxid", m2mDataNode.getZxid())).and(eq("label", m2mDataNode.getLabel()));
			session.execute(update);
		} catch (Exception ex) {
			ex.printStackTrace();
			return Long.valueOf(0);
		}
		return Long.valueOf(1);
	}

	private QueryBuilder query() {
		return new QueryBuilder(cluster);
	}

	@Override
	public void close() {
		if (session != null) {
			session.close();
		}
		if (cluster != null) {
			cluster.close();
		}
	}

	/**
	 * 最终将事务请求应用到cassandra数据库上
	 */
	@Override
	public ProcessTxnResult processTxn(M2mTxnHeader header, M2mRecord m2mRecord) {
		ProcessTxnResult processTxnResult = new ProcessTxnResult();
		processTxnResult.cxid = header.getCxid();
		processTxnResult.zxid = header.getZxid();
		processTxnResult.err = 0;
		switch (header.getType()) {
		case OpCode.create:
			M2mCreateTxn createTxn = (M2mCreateTxn) m2mRecord;
			processTxnResult.path = createTxn.getPath();
			ByteArrayInputStream inbaos = new ByteArrayInputStream(createTxn.getData());
			DataInputStream dis = new DataInputStream( inbaos );
			M2mBinaryInputArchive inboa = M2mBinaryInputArchive.getArchive(dis);
			M2mDataNode m2mDataNode=new M2mDataNode();
			try {
				m2mDataNode.deserialize(inboa, "m2mData");
			} catch (IOException e) {
				e.printStackTrace();
				break;
			}
			m2mDataNode.setZxid(Integer.valueOf(header.getZxid()+""));
			create(m2mDataNode);
			break;
		case OpCode.delete:
			M2mDeleteTxn deleteTxn = (M2mDeleteTxn) m2mRecord;
			processTxnResult.path = deleteTxn.getPath();
			delete(deleteTxn.getPath());
			break;
		case OpCode.setData:
			M2mSetDataTxn m2mSetDataTxn = (M2mSetDataTxn) m2mRecord;
			processTxnResult.path = m2mSetDataTxn.getPath();
			
			M2mDataNode object = (M2mDataNode) ResourceReflection.deserializeKryo(m2mSetDataTxn
					.getData());
			
			update(m2mSetDataTxn.getPath(),
					ResourceReflection.serialize(object));
			break;
		}
		return processTxnResult;
	}



	@Override
	public boolean truncate(Long zxid) {
		try {
			Select.Selection selection = query().select();
			Select select = selection.from(keyspace, table);
			select.where(gte("zxid", zxid));
			select.allowFiltering();
			ResultSet resultSet = session.execute(select);
			if (resultSet == null) {
				return true;
			}
			for (Row row : resultSet.all()) {
				Integer idValue = (Integer) row.getObject("id");
				Integer zxidValue = (Integer) row.getObject("zxid");
				Delete deletion = query().delete().from(keyspace, table);
				Statement delete = deletion.where(eq("id", Integer.valueOf(idValue)))
						.and(eq("zxid", zxidValue));
				session.execute(delete);
			}

		} catch (Exception ex) {
			ex.printStackTrace();
			return false;
		}
		return true;
	}

	@Override
	public List<M2mDataNode> retrieve(Integer key) {
		List<M2mDataNode> m2mList=new ArrayList<>();
 		try {
			Select.Selection selection = query().select();
			Select select = selection.from(keyspace, table);
			select.where(gt("zxid", Integer.valueOf(key)));
			select.allowFiltering();
			ResultSet resultSet = session.execute(select);
			if (resultSet == null) {
				return m2mList;
			}

			Map<String, Object> result = new HashMap<String, Object>();
			for (Row row : resultSet.all()) {
				ColumnDefinitions columnDefinitions = resultSet
						.getColumnDefinitions();
				columnDefinitions.forEach(d -> {
					String name = d.getName();
					Object object = row.getObject(name);
					result.put(name, object);
					
				});
				m2mList.add(ResourceReflection.deserialize(
						M2mDataNode.class, result));
				result.clear();
			}


		} catch (Exception ex) {
			ex.printStackTrace();
			return m2mList;
		}
 		return m2mList;
	}
}
