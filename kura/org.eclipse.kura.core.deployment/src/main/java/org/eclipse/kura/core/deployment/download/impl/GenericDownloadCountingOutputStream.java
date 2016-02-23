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

package org.eclipse.kura.core.deployment.download.impl;

import java.io.IOException;
import java.io.InputStream;

import org.apache.commons.io.output.CountingOutputStream;
import org.eclipse.kura.core.deployment.CloudDeploymentHandlerV2.DOWNLOAD_STATUS;
import org.eclipse.kura.core.deployment.download.DeploymentPackageDownloadOptions;
import org.eclipse.kura.core.deployment.download.DownloadOptions;
import org.eclipse.kura.core.deployment.progress.ProgressEvent;
import org.eclipse.kura.core.deployment.progress.ProgressListener;
import org.eclipse.kura.ssl.SslManagerService;

public class GenericDownloadCountingOutputStream extends CountingOutputStream {

	private int propResolution;
	private int propBufferSize;
	private int propConnectTimeout = 5000;
	private int propReadTimeout = 6000;
	private int propBlockDelay = 1000;

	long totalBytes;

	final DeploymentPackageDownloadOptions options;
	final SslManagerService m_sslManagerService;
	final ProgressListener pl;
	final int m_alreadyDownloaded;
	final String m_downloadURL;

	InputStream is = null;

	private long m_currentStep = 1;
	//private long previous;
	private DOWNLOAD_STATUS m_downloadStatus = DOWNLOAD_STATUS.FAILED;
	
	
	public GenericDownloadCountingOutputStream(DownloadOptions downloadOptions) {
		super(downloadOptions.getOut());
		this.options = downloadOptions.getRequestOptions();
		this.m_sslManagerService = downloadOptions.getSslManagerService();
		this.pl = downloadOptions.getCallback();
		this.m_downloadURL = downloadOptions.getDownloadURL();
		this.m_alreadyDownloaded = downloadOptions.getAlreadyDownloaded();
	}
	
	public DOWNLOAD_STATUS getDownloadTransferStatus(){
		return m_downloadStatus;
	}
	
	public Long getDownloadTransferProgressPercentage(){
		Long percentage= (long) Math.floor((((Long) getByteCount()).doubleValue() / ((Long) totalBytes).doubleValue()) * 100);
		if(percentage < 0){
			return (long)50;
		}
		return percentage;
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
		if(propResolution == 0 && getTotalBytes() > 0) {
			propResolution= Math.round((totalBytes / 100) * 5);
		} else if (propResolution == 0) {
			propResolution= 1024 * 256;
		}
		if (getByteCount() >= m_currentStep * propResolution) {
			//System.out.println("Bytes read: "+ (getByteCount() - previous));
			//previous = getByteCount();
			m_currentStep++;
			postProgressEvent(options.getClientId(), getByteCount(), totalBytes, DOWNLOAD_STATUS.IN_PROGRESS, null);
		}
		try {
			Thread.sleep(propBlockDelay);
		} catch (InterruptedException e) {
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
		propResolution = resolution;
	}

	protected void setBufferSize(int size) {
		propBufferSize = size;
	}

	protected void setConnectTimeout(int timeout) {
		propConnectTimeout = timeout;
	}

	protected void setReadTimeout(int timeout) {
		propReadTimeout = timeout;
	}
	
	protected void setBlockDelay(int delay) {
		propBlockDelay = delay;
	}
	
	protected int getResolution() {
		return propResolution;
	}

	protected int getBufferSize() {
		return propBufferSize;
	}
	
	protected int getConnectTimeout() {
		return propConnectTimeout;
	}

	protected int getPropReadTimeout() {
		return propReadTimeout;
	}

	protected int getPropBlockDelay() {
		return propBlockDelay;
	}
}
