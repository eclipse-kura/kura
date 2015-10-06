package org.eclipse.kura.core.deployment.download;

import org.eclipse.kura.core.deployment.download.impl.HttpDownloadCountingOutputStream;

public class DownloadFactory {
	
	private static final String DOWNLOAD_PROTOCOL_HTTP= "HTTP";
	
	public static DownloadCountingOutputStream getDownloadInstance(String protocol, DownloadOptions downloadOptions){
		if(protocol.equals(DOWNLOAD_PROTOCOL_HTTP)){
			return new HttpDownloadCountingOutputStream(downloadOptions);
		}
		return null;
	}

}
