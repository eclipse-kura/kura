package org.eclipse.kura.core.deployment.download;

import java.io.IOException;

import org.eclipse.kura.KuraException;
import org.eclipse.kura.core.deployment.CloudDeploymentHandlerV2.DOWNLOAD_STATUS;

public interface DownloadCountingOutputStream {

	public void cancelDownload() throws Exception;
	
	public void startWork() throws KuraException;
	
	public DOWNLOAD_STATUS getDownloadTransferStatus();
	
	public Long getDownloadTransferProgressPercentage();
	
	public Long getTotalBytes();
	
	public void setTotalBytes(long totalBytes);
	
	public void close() throws IOException;
}
