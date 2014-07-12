package org.eclipse.kura.cloud;

import org.eclipse.kura.KuraException;
import org.eclipse.kura.message.KuraPayload;

public interface CloudPayloadProtoBufEncoder {
	/**
	 * Encodes a {@link org.eclipse.kura.message.KuraPayload} to a Google Protocol Buffers encoded, optionally gzipped, binary payload.
	 * 
	 * @param kuraPayload
	 * @param gzipped
	 * @return
	 * @throws KuraException
	 */
	byte[] getBytes(KuraPayload kuraPayload, boolean gzipped) throws KuraException;
}
