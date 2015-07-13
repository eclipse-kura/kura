package org.eclipse.kura.core.deployment.progress;

import java.util.EventObject;

public class ProgressEvent extends EventObject {

	/**
	 * 
	 */
	private static final long serialVersionUID = -4316652505853478843L;

	String clientId;
	int transferSize;
	int transferProgress;
	String transferStatus;
	String requesterClientId;
	long jobId;
	
	public ProgressEvent(Object source, String requesterClientId, String clientId, int transferSize, int transferProgress, String trasnferStatus, long jobId) {
		super(source);
		this.clientId = clientId;
		this.transferSize = transferSize;
		this.transferProgress = transferProgress;
		this.transferStatus = trasnferStatus;
		this.requesterClientId = requesterClientId;
		this.jobId= jobId;
	}

	public String getClientId() {
		return clientId;
	}

	public int getTransferSize() {
		return transferSize;
	}

	public int getTransferProgress() {
		return transferProgress;
	}

	public String getTransferStatus() {
		return transferStatus;
	}

	public String getRequesterClientId() {
		return requesterClientId;
	}
	
	public long getJobId() {
		return jobId;
	}

}
