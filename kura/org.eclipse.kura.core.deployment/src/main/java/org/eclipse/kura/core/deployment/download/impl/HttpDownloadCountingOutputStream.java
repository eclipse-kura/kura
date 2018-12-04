/*******************************************************************************
 * Copyright (c) 2011, 2018 Eurotech and/or its affiliates
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Eurotech
 *******************************************************************************/
package org.eclipse.kura.core.deployment.download.impl;

import java.io.IOException;
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
import org.eclipse.kura.core.deployment.DownloadStatus;
import org.eclipse.kura.core.deployment.download.DownloadCountingOutputStream;
import org.eclipse.kura.core.deployment.download.DownloadOptions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class HttpDownloadCountingOutputStream extends GenericDownloadCountingOutputStream
        implements DownloadCountingOutputStream {

    private static final Logger s_logger = LoggerFactory.getLogger(HttpDownloadCountingOutputStream.class);

    private ExecutorService executor;
    private Future<Void> future;

    public HttpDownloadCountingOutputStream(DownloadOptions downloadOptions) {
        super(downloadOptions);
        setBufferSize(this.options.getBlockSize());
        setResolution(this.options.getNotifyBlockSize());
        setBlockDelay(this.options.getBlockDelay());
        setConnectTimeout(this.options.getTimeout());
    }

    @Override
    public void cancelDownload() throws Exception {
        if (this.executor != null && this.future != null) {
            this.future.cancel(true);
            this.executor.shutdownNow();

            postProgressEvent(this.options.getClientId(), getByteCount(), this.totalBytes, DownloadStatus.CANCELLED,
                    "Download cancelled");
        }
    }

    @Override
    public void startWork() throws KuraException {

        this.executor = Executors.newSingleThreadExecutor();

        this.future = this.executor.submit(new Callable<Void>() {

            @Override
            public Void call() throws Exception {
                URL localUrl = null;
                boolean shouldAuthenticate = false;
                try {
                    shouldAuthenticate = HttpDownloadCountingOutputStream.this.options.getUsername() != null
                            && HttpDownloadCountingOutputStream.this.options.getPassword() != null
                            && !(HttpDownloadCountingOutputStream.this.options.getUsername().trim().isEmpty()
                                    && !HttpDownloadCountingOutputStream.this.options.getPassword().trim().isEmpty());

                    if (shouldAuthenticate) {
                        Authenticator.setDefault(new Authenticator() {

                            @Override
                            protected PasswordAuthentication getPasswordAuthentication() {
                                return new PasswordAuthentication(
                                        HttpDownloadCountingOutputStream.this.options.getUsername(),
                                        HttpDownloadCountingOutputStream.this.options.getPassword().toCharArray());
                            }
                        });
                    }

                    localUrl = new URL(HttpDownloadCountingOutputStream.this.downloadURL);
                    URLConnection urlConnection = localUrl.openConnection();
                    int connectTimeout = getConnectTimeout();
                    int readTimeout = getPropReadTimeout();
                    urlConnection.setConnectTimeout(connectTimeout);
                    urlConnection.setReadTimeout(readTimeout);

                    testConnectionProtocol(urlConnection);

                    HttpDownloadCountingOutputStream.this.is = urlConnection.getInputStream();

                    String s = urlConnection.getHeaderField("Content-Length");
                    s_logger.info("Content-lenght: " + s);

                    setTotalBytes(s != null ? Integer.parseInt(s) : -1);
                    postProgressEvent(HttpDownloadCountingOutputStream.this.options.getClientId(), 0,
                            HttpDownloadCountingOutputStream.this.totalBytes, DownloadStatus.IN_PROGRESS, null);

                    int bufferSize = getBufferSize();

                    if (bufferSize == 0 && getTotalBytes() > 0) {
                        int newSize = Math.round(HttpDownloadCountingOutputStream.this.totalBytes / 100F + 1F);
                        bufferSize = newSize;
                        setBufferSize(newSize);
                    } else if (bufferSize == 0) {
                        int newSize = 1024 * 4;
                        bufferSize = newSize;
                        setBufferSize(newSize);
                    }

                    long numBytes = IOUtils.copyLarge(HttpDownloadCountingOutputStream.this.is,
                            HttpDownloadCountingOutputStream.this, new byte[bufferSize]);
                    postProgressEvent(HttpDownloadCountingOutputStream.this.options.getClientId(), numBytes,
                            HttpDownloadCountingOutputStream.this.totalBytes, DownloadStatus.COMPLETED, null);

                } catch (IOException e) {
                    postProgressEvent(HttpDownloadCountingOutputStream.this.options.getClientId(), getByteCount(),
                            HttpDownloadCountingOutputStream.this.totalBytes, DownloadStatus.FAILED, e.getMessage());
                    throw new KuraConnectException(e);
                } finally {
                    if (HttpDownloadCountingOutputStream.this.is != null) {
                        try {
                            HttpDownloadCountingOutputStream.this.is.close();
                        } catch (IOException e) {
                        }
                    }
                    try {
                        close();
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

        try {
            this.future.get();
        } catch (ExecutionException ex) {
            throw new KuraException(KuraErrorCode.INTERNAL_ERROR, ex);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new KuraException(KuraErrorCode.INTERNAL_ERROR, ex);
        }
    }

    private void testConnectionProtocol(URLConnection urlConnection) throws IOException, KuraConnectException {
        try {
            if (urlConnection instanceof HttpsURLConnection) {
                ((HttpsURLConnection) urlConnection)
                        .setSSLSocketFactory(this.sslManagerService.getSSLSocketFactory());
            } else if (!(urlConnection instanceof HttpURLConnection)) {
                postProgressEvent(this.options.getClientId(), getByteCount(), this.totalBytes, DownloadStatus.FAILED,
                        "The request URL is not supported");
                throw new KuraConnectException("Unsupported protocol!");
            }
        } catch (GeneralSecurityException e) {
            postProgressEvent(this.options.getClientId(), getByteCount(), this.totalBytes, DownloadStatus.FAILED,
                    e.getMessage());
            throw new KuraConnectException(e, "Unsupported protocol!");
        }
    }
}
