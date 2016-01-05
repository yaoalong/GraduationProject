package org.lab.mars.onem2m.jute;

import java.io.IOException;
import java.io.Serializable;

public interface M2mRecord extends Serializable {
	public void serialize(M2mOutputArchive archive, String tag) throws IOException;

	public void deserialize(M2mInputArchive archive, String tag)
			throws IOException;

}
