/**
 * Copyright (c) 2011, 2015 Eurotech and/or its affiliates
 *
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *   Eurotech
 */

package org.eclipse.kura.core.deployment.download;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import org.apache.commons.io.output.CountingOutputStream;
import org.eclipse.kura.KuraException;
import org.eclipse.kura.core.deployment.CloudDeploymentHandlerV2.DOWNLOAD_STATUS;
import org.eclipse.kura.core.deployment.progress.ProgressEvent;
import org.eclipse.kura.core.deployment.progress.ProgressListener;
import org.eclipse.kura.ssl.SslManagerService;

public abstract class DownloadCountingOutputStream extends CountingOutputStream {

	private static int PROP_RESOLUTION = 1024 * 4;
	private static int PROP_BUFFER_SIZE = 1024 * 4;
	private static int PROP_CONNECT_TIMEOUT = 5000;
	private static int PROP_READ_TIMEOUT = 6000;
	private static int PROP_BLOCK_DELAY = 1000;

	long totalBytes;

	final DeploymentPackageDownloadOptions options;
	final SslManagerService m_sslManagerService;
	final ProgressListener pl;
	final int m_alreadyDownloaded;
	final String m_downloadURL;

	InputStream is = null;

	private long m_currentStep = 1;
	private long previous;
	private DOWNLOAD_STATUS m_downloadStatus = DOWNLOAD_STATUS.FAILED;
	
	
	public DownloadCountingOutputStream(OutputStream out, DeploymentPackageDownloadOptions options, ProgressListener callback,
			SslManagerService m_sslManagerService, String downloadURL, int alreadyDownloaded) {
		super(out);
		this.options = options;
		this.m_sslManagerService = m_sslManagerService;
		this.pl = callback;
		this.m_downloadURL = downloadURL;
		this.m_alreadyDownloaded = alreadyDownloaded;
	}

	public abstract void cancelDownload() throws Exception;
	
	public abstract void startWork() throws KuraException;
	
	public DOWNLOAD_STATUS getDownloadTransferStatus(){
		return m_downloadStatus;
	}
	
	public Long getDownloadTransferProgressPercentage(){
		return (long) Math.floor((((Long) getByteCount()).doubleValue() / ((Long) totalBytes).doubleValue()) * 100);
	}
	
	public Long getTotalBytes(){
		return totalBytes;
	}
	
	public void setTotalBytes(long totalBytes){
		this.totalBytes= totalBytes;
	}

	@Override
	protected void afterWrite(int n) throws IOException {
		super.afterWrite(n);
		if (getByteCount() >= m_currentStep * PROP_RESOLUTION) {
			System.out.println("Bytes read: "+ (getByteCount() - previous));
			previous = getByteCount();
			m_currentStep++;
			postProgressEvent(options.getClientId(), getByteCount(), totalBytes, DOWNLOAD_STATUS.IN_PROGRESS, null);
		}
		try {
			Thread.sleep(PROP_BLOCK_DELAY);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	protected void postProgressEvent(String clientId, long progress, long total, DOWNLOAD_STATUS status, String errorMessage) {
		Long perc = getDownloadTransferProgressPercentage();
		m_downloadStatus = status;
		ProgressEvent pe= new ProgressEvent(this, options, ((Long) total).intValue(), ((Long) perc).intValue(), 
											getDownloadTransferStatus().getStatusString(), m_alreadyDownloaded);
		if(errorMessage != null){
			pe.setExceptionMessage(errorMessage);
		}
		pl.progressChanged(pe);
		
	}
	
	protected void setResolution(int resolution) {
		PROP_RESOLUTION = resolution;
	}

	protected void setBufferSize(int size) {
		PROP_BUFFER_SIZE = size;
	}

	protected void setConnectTimeout(int timeout) {
		PROP_CONNECT_TIMEOUT = timeout;
	}

	protected void setReadTimeout(int timeout) {
		PROP_READ_TIMEOUT = timeout;
	}
	
	protected void setBlockDelay(int delay) {
		PROP_BLOCK_DELAY = delay;
	}
	
	protected static int getResolution() {
		return PROP_RESOLUTION;
	}

	protected static int getBufferSize() {
		return PROP_BUFFER_SIZE;
	}
	
	protected static int getConnectTimeout() {
		return PROP_CONNECT_TIMEOUT;
	}

	protected static int getPROP_READ_TIMEOUT() {
		return PROP_READ_TIMEOUT;
	}

	protected static int getPROP_BLOCK_DELAY() {
		return PROP_BLOCK_DELAY;
	}
}
