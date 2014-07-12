package org.eclipse.kura.cloud;

import org.eclipse.kura.KuraException;
import org.eclipse.kura.message.KuraPayload;

public interface CloudPayloadProtoBufDecoder {
	/**
	 * Decodes a Google Protocol Buffers encoded, optionally gzipped, binary payload to a {@link org.eclipse.kura.message.KuraPayload}.
	 * 
	 * @param payload
	 * @return
	 * @throws KuraException
	 */
	public KuraPayload buildFromByteArray(byte[] payload) throws KuraException;
}
