/*******************************************************************************
 * Copyright (c) 2025 Eurotech and/or its affiliates and others
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *  Eurotech
 *******************************************************************************/
package org.eclipse.kura.core.deployment.download.impl;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLSocketFactory;

import org.eclipse.kura.core.deployment.DownloadStatus;
import org.eclipse.kura.core.deployment.download.DeploymentPackageDownloadOptions;
import org.eclipse.kura.core.deployment.download.DownloadOptions;
import org.eclipse.kura.core.deployment.progress.ProgressEvent;
import org.eclipse.kura.core.deployment.progress.ProgressListener;
import org.eclipse.kura.ssl.SslManagerService;
import org.junit.Test;

public class HttpDownloadCountingOutputStreamTest {

    private HttpDownloadCountingOutputStream httpDownloadCountingOutputStream;
    private DownloadOptions downloadOptions;
    private SslManagerService sslManagerService;
    private ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

    private Exception occurredException;
    private byte[] data;
    private String transferProgressStatus;
    private DeploymentPackageDownloadOptions deploymentPackageDownloadOpions;

    @Test
    public void shouldDownloadFile() {
        givenRandomData("test");
        givenDeploymentPackageDownloadOptions("fakeUri", "fakeDpName", "fakeDpVersion");
        givenOptions(0, this::progressChanged, "http://fakeUrl/fakeData");
        givenHttpDownloadCountingOutputStream();

        whenDownloadStart();

        thenNoExceptionOccured();
        thenTransferProgressStatusIs(DownloadStatus.COMPLETED);
        thenDownloadedDataAreCorrect();
    }

    @Test
    public void shouldCancelDownloadFile() {
        givenRandomData("test");
        givenDeploymentPackageDownloadOptions("fakeUri", "fakeDpName", "fakeDpVersion");
        givenOptions(0, this::progressChanged, "http://fakeUrl/fakeData");
        givenHttpDownloadCountingOutputStream();
        givenDownloadStarted();

        whenDownloadIsCancelled();

        thenNoExceptionOccured();
        thenTransferProgressStatusIs(DownloadStatus.CANCELLED);
        thenDownloadedDataAreCorrect();
    }

    @Test
    public void shouldDownloadFileWithSetSSLManagerService() {
        givenRandomData("test2");
        givenSslManagerService();
        givenDeploymentPackageDownloadOptions("fakeUri", "fakeDpName", "fakeDpVersion");
        givenOptions(0, this::progressChanged, "https://fakeUrl/fakeData");
        givenHttpDownloadCountingOutputStream();

        whenDownloadStart();

        thenNoExceptionOccured();
        thenTransferProgressStatusIs(DownloadStatus.COMPLETED);
        thenDownloadedDataAreCorrect();
        thenSSLSocketFactoryIsUsed();
    }

    private void givenSslManagerService() {
        this.sslManagerService = mock(SslManagerService.class);
        try {
            when(this.sslManagerService.getSSLSocketFactory()).thenReturn(mock(SSLSocketFactory.class));
        } catch (Exception e) {
            fail();
        }
    }

    private void givenDeploymentPackageDownloadOptions(String deployUri, String dpName, String dpVersion) {
        this.deploymentPackageDownloadOpions = new DeploymentPackageDownloadOptions(deployUri, dpName, dpVersion);
        this.deploymentPackageDownloadOpions.setJobId(0);
    }

    private void givenRandomData(String data) {
        this.data = data.getBytes();
    }

    private void givenOptions(int alreadyDownloaded, ProgressListener callback, String downloadUrl) {

        this.downloadOptions = new DownloadOptions();

        this.downloadOptions.setRequestOptions(this.deploymentPackageDownloadOpions);
        this.downloadOptions.setAlreadyDownloaded(alreadyDownloaded);
        this.downloadOptions.setCallback(callback);
        this.downloadOptions.setDownloadURL(downloadUrl);
        this.downloadOptions.setOut(this.outputStream);
        this.downloadOptions.setSslManagerService(this.sslManagerService);

    }

    private void givenHttpDownloadCountingOutputStream() {
        this.httpDownloadCountingOutputStream = new HttpDownloadCountingOutputStream(this.downloadOptions) {

            @Override
            protected HttpURLConnection openConnection(URL localUrl) throws IOException {
                HttpURLConnection urlConnection = null;
                switch (localUrl.getProtocol().toLowerCase()) { //
                case "http" -> urlConnection = mock(HttpURLConnection.class);
                case "https" -> urlConnection = mock(HttpsURLConnection.class);
                default -> throw new IllegalArgumentException("protocol not supported");
                }

                when(urlConnection.getInputStream())
                        .thenReturn(new ByteArrayInputStream(HttpDownloadCountingOutputStreamTest.this.data));
                when(urlConnection.getResponseCode()).thenReturn(HttpURLConnection.HTTP_OK);
                when(urlConnection.getHeaderField("Content-Length"))
                        .thenReturn(String.valueOf(HttpDownloadCountingOutputStreamTest.this.data.length));

                return urlConnection;
            }
        };
    }

    private void givenDownloadStarted() {
        startDownload();
    }

    private void whenDownloadStart() {
        startDownload();
    }

    private void whenDownloadIsCancelled() {
        cancelDownload();
    }

    private void thenSSLSocketFactoryIsUsed() {
        try {
            verify(this.sslManagerService, atLeastOnce()).getSSLSocketFactory();
        } catch (Exception e) {
            fail();
        }
    }

    private void thenNoExceptionOccured() {
        assertNull(this.occurredException);
    }

    private void thenTransferProgressStatusIs(DownloadStatus expected) {
        assertEquals(expected, DownloadStatus.valueOf(this.transferProgressStatus));
    }

    private void thenDownloadedDataAreCorrect() {
        assertArrayEquals(this.data, this.outputStream.toByteArray());
    }

    private void progressChanged(ProgressEvent progress) {
        this.transferProgressStatus = progress.getTransferStatus();
    }

    private void startDownload() {
        try {
            this.httpDownloadCountingOutputStream.startWork();
            Thread.sleep(500l);
        } catch (Exception e) {
            this.occurredException = e;
        }
    }

    private void cancelDownload() {
        try {
            this.httpDownloadCountingOutputStream.cancelDownload();
        } catch (Exception e) {
            this.occurredException = e;
        }
    }

}
