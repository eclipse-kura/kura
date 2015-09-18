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
import java.net.Authenticator;
import java.net.HttpURLConnection;
import java.net.PasswordAuthentication;
import java.net.URL;
import java.net.URLConnection;
import java.security.GeneralSecurityException;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import javax.net.ssl.HttpsURLConnection;

import org.apache.commons.io.IOUtils;
import org.eclipse.kura.KuraConnectException;
import org.eclipse.kura.KuraErrorCode;
import org.eclipse.kura.KuraException;
import org.eclipse.kura.core.deployment.CloudDeploymentHandlerV2.DOWNLOAD_STATUS;
import org.eclipse.kura.core.deployment.progress.ProgressListener;
import org.eclipse.kura.ssl.SslManagerService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class HttpDownloadCountingOutputStream extends DownloadCountingOutputStream {
	private static final Logger s_logger = LoggerFactory.getLogger(HttpDownloadCountingOutputStream.class);

	InputStream is = null;
	
	private ExecutorService executor;
	private Future<Void> future;
	

	public HttpDownloadCountingOutputStream(OutputStream out, DeploymentPackageDownloadOptions options, ProgressListener callback,
			SslManagerService sslManagerService, String downloadURL, int alreadyDownloaded) {
		super(out, options, callback, sslManagerService, downloadURL, alreadyDownloaded);
		setBufferSize(options.getBlockSize());
		setResolution(options.getNotifyBlockSize());
		setBlockDelay(options.getBlockDelay());
		setConnectTimeout(options.getTimeout());
	}

	public void cancelDownload() throws Exception{
		if(executor != null){
			if(future != null){
				future.cancel(true);
				executor.shutdownNow();
				
				postProgressEvent(options.getClientId(), getByteCount(), totalBytes, DOWNLOAD_STATUS.FAILED, "Download cancelled");
			}
		}
	}
	
	public void startWork() throws KuraException {
		
		executor = Executors.newSingleThreadExecutor();
		
		future = executor.submit(new Callable<Void>(){

			@Override
			public Void call() throws Exception {
				URL localUrl = null;
				boolean shouldAuthenticate = false;
				try {
					shouldAuthenticate = (options.getUsername() != null) && (options.getPassword() != null)
							&& !(options.getUsername().trim().isEmpty() && !(options.getPassword().trim().isEmpty()));

					if (shouldAuthenticate) {
						Authenticator.setDefault(new Authenticator() {
							protected PasswordAuthentication getPasswordAuthentication() {
								return new PasswordAuthentication(options.getUsername(), options.getPassword().toCharArray());
							}
						});
					}

					localUrl = new URL(m_downloadURL);
					URLConnection urlConnection = localUrl.openConnection();
					int connectTimeout = getConnectTimeout();
					int readTimeout = getPROP_READ_TIMEOUT();
					urlConnection.setConnectTimeout(connectTimeout);
					urlConnection.setReadTimeout(readTimeout);

					try {
						if (urlConnection instanceof HttpsURLConnection) {
							((HttpsURLConnection) urlConnection).setSSLSocketFactory(m_sslManagerService.getSSLSocketFactory());
						} else if (!(urlConnection instanceof HttpURLConnection)) {
							postProgressEvent(options.getClientId(), getByteCount(), totalBytes, DOWNLOAD_STATUS.FAILED, "The request URL is not supported");
							throw new KuraConnectException("Unsupported protocol!");
						}
					} catch (GeneralSecurityException e) {
						postProgressEvent(options.getClientId(), getByteCount(), totalBytes, DOWNLOAD_STATUS.FAILED, e.getMessage());
						throw new KuraConnectException(e, "Unsupported protocol!");
					}

					is = localUrl.openStream();

					String s = urlConnection.getHeaderField("Content-Length");
					s_logger.info("Content-lenght: " + s);

					setTotalBytes(s != null ? Integer.parseInt(s) : -1);
					postProgressEvent(options.getClientId(), 0, totalBytes, DOWNLOAD_STATUS.IN_PROGRESS, null);
					
					int bufferSize = getBufferSize();
					
					if (bufferSize == 0 && getTotalBytes() > 0){
						int newSize= Math.round(totalBytes/100 * 1);
						bufferSize= newSize;
						setBufferSize(newSize);
					} else if (bufferSize == 0) {
						int newSize= 1024 * 4;
						bufferSize= newSize;
						setBufferSize(newSize);
					}
					
					long numBytes = IOUtils.copyLarge(is, HttpDownloadCountingOutputStream.this, new byte[bufferSize]);
					postProgressEvent(options.getClientId(), numBytes, totalBytes, DOWNLOAD_STATUS.COMPLETED, null);

				} catch (IOException e) {
					postProgressEvent(options.getClientId(), getByteCount(), totalBytes, DOWNLOAD_STATUS.FAILED, e.getMessage());
					throw new KuraConnectException(e);
				} finally {
					if (is != null) {
						try {
							is.close();
						} catch (IOException e) {
						}
					}
					try {
						HttpDownloadCountingOutputStream.this.close();
					} catch (IOException e) {
					}
					localUrl = null;
					if (shouldAuthenticate) {
						Authenticator.setDefault(null);
					}
				}
				
				return null;
			}
			
		});
		
		try{
			future.get();
		}catch(ExecutionException ex){
			throw new KuraException(KuraErrorCode.INTERNAL_ERROR, ex);
		}catch(InterruptedException ex){
			throw new KuraException(KuraErrorCode.INTERNAL_ERROR, ex);
		}
	}
}
