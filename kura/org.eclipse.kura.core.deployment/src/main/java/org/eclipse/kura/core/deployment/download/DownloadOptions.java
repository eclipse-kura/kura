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

import java.io.OutputStream;

import org.eclipse.kura.core.deployment.progress.ProgressListener;
import org.eclipse.kura.ssl.SslManagerService;

public class DownloadOptions {
	private OutputStream out;
	private DeploymentPackageDownloadOptions options;
	private ProgressListener callback;
	private SslManagerService sslManagerService;
	private String downloadURL;
	private int alreadyDownloaded;
	public OutputStream getOut() {
		return out;
	}
	public void setOut(OutputStream out) {
		this.out = out;
	}
	public DeploymentPackageDownloadOptions getRequestOptions() {
		return options;
	}
	public void setRequestOptions(DeploymentPackageDownloadOptions options) {
		this.options = options;
	}
	public ProgressListener getCallback() {
		return callback;
	}
	public void setCallback(ProgressListener callback) {
		this.callback = callback;
	}
	public SslManagerService getSslManagerService() {
		return sslManagerService;
	}
	public void setSslManagerService(SslManagerService sslManagerService) {
		this.sslManagerService = sslManagerService;
	}
	public String getDownloadURL() {
		return downloadURL;
	}
	public void setDownloadURL(String downloadURL) {
		this.downloadURL = downloadURL;
	}
	public int getAlreadyDownloaded() {
		return alreadyDownloaded;
	}
	public void setAlreadyDownloaded(int alreadyDownloaded) {
		this.alreadyDownloaded = alreadyDownloaded;
	}
}
