/*******************************************************************************
 * Copyright (c) 2018 Eurotech and/or its affiliates and others
 *
 *   All rights reserved. This program and the accompanying materials
 *   are made available under the terms of the Eclipse Public License v1.0
 *   which accompanies this distribution, and is available at
 *   http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.eclipse.kura.core.deployment.download.impl;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.anyObject;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.File;
import java.util.HashMap;
import java.util.concurrent.CancellationException;

import org.eclipse.kura.KuraException;
import org.eclipse.kura.core.deployment.CloudDeploymentHandlerV2;
import org.eclipse.kura.core.deployment.CloudDeploymentHandlerV2Options;
import org.eclipse.kura.core.deployment.DownloadStatus;
import org.eclipse.kura.core.deployment.download.DeploymentPackageDownloadOptions;
import org.eclipse.kura.core.deployment.download.DownloadCountingOutputStream;
import org.eclipse.kura.core.deployment.download.DownloadOptions;
import org.eclipse.kura.core.deployment.progress.ProgressEvent;
import org.eclipse.kura.core.testutil.TestUtil;
import org.eclipse.kura.message.KuraResponsePayload;
import org.junit.Test;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

public class DownloadImplTest {

    @Test
    public void testProgressChanged() {
        String deployUri = "uri";
        String dpName = "name";
        String dpVersion = "version";
        long jobId = 1234L;

        DeploymentPackageDownloadOptions options = new DeploymentPackageDownloadOptions(deployUri, dpName, dpVersion);
        options.setJobId(jobId);
        CloudDeploymentHandlerV2 callback = mock(CloudDeploymentHandlerV2.class);

        DownloadImpl di = new DownloadImpl(options, callback);

        Object source = "src";
        int transferSize = 1234;
        int transferProgress = 75;
        String transferStatus = DownloadStatus.IN_PROGRESS.getStatusString();
        int downloadIndex = 1;
        String exceptionMessage = "test";
        ProgressEvent progressEvent = new ProgressEvent(source, options, transferSize, transferProgress, transferStatus,
                downloadIndex);
        progressEvent.setExceptionMessage(exceptionMessage);

        doAnswer(new Answer<Object>() {

            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable {
                Object[] args = invocation.getArguments();
                assertEquals(3, args.length);

                assertEquals(options, args[0]);
                assertEquals(DownloadImpl.RESOURCE_DOWNLOAD, args[2]);

                assertNotNull(args[1]);

                KuraNotifyPayload knp = (KuraNotifyPayload) args[1];

                assertNotNull(knp.getTimestamp());
                assertEquals(transferSize, knp.getTransferSize());
                assertEquals(transferProgress, knp.getTransferProgress());
                assertEquals(transferStatus, knp.getTransferStatus());
                assertEquals(jobId, (long) knp.getJobId());
                assertNotNull(knp.getErrorMessage());
                assertEquals(exceptionMessage, knp.getErrorMessage());
                assertEquals(downloadIndex, (int) knp.getMissingDownloads());

                return null;
            }
        }).when(callback).publishMessage(anyObject(), anyObject(), anyObject());

        di.progressChanged(progressEvent);

        verify(callback).publishMessage(anyObject(), anyObject(), anyObject());
    }

    @Test
    public void testDownloadDeploymentPackageInternalCancel() throws KuraException, NoSuchFieldException {
        String deployUri = "uri";
        String dpName = "name";
        String dpVersion = "version";
        long jobId = 1234L;

        DeploymentPackageDownloadOptions options = new DeploymentPackageDownloadOptions(deployUri, dpName, dpVersion);
        options.setJobId(jobId);

        CloudDeploymentHandlerV2 callback = mock(CloudDeploymentHandlerV2.class);
        TestUtil.setFieldValue(callback, "componentOptions", new CloudDeploymentHandlerV2Options(new HashMap<>()));

        DownloadImpl di = new DownloadImpl(options, callback) {

            @Override
            protected DownloadCountingOutputStream getDownloadInstance(String protocol,
                    DownloadOptions downloadOptions) {
                throw new CancellationException("test");
            }
        };

        di.downloadDeploymentPackageInternal();

        verify(callback, times(0)).publishMessage(anyObject(), anyObject(), anyObject());
        verify(callback, times(0)).installDownloadedFile(anyObject(), anyObject());
    }

    @Test
    public void testDownloadDeploymentPackageInternalFail() throws KuraException {
        String deployUri = "uri";
        String dpName = "name";
        String dpVersion = "version";
        long jobId = 1234L;

        DeploymentPackageDownloadOptions options = new DeploymentPackageDownloadOptions(deployUri, dpName, dpVersion);
        options.setJobId(jobId);
        CloudDeploymentHandlerV2 callback = mock(CloudDeploymentHandlerV2.class);

        DownloadImpl di = new DownloadImpl(options, callback) {

            @Override
            protected DownloadCountingOutputStream getDownloadInstance(String protocol,
                    DownloadOptions downloadOptions) {
                throw new RuntimeException("test");
            }
        };

        doAnswer(new Answer<Void>() {

            @Override
            public Void answer(InvocationOnMock invocation) throws Throwable {
                Object[] args = invocation.getArguments();
                assertEquals(3, args.length);

                assertEquals(options, args[0]);
                assertEquals(DownloadImpl.RESOURCE_DOWNLOAD, args[2]);

                assertNotNull(args[1]);

                KuraNotifyPayload knp = (KuraNotifyPayload) args[1];

                assertNotNull(knp.getTimestamp());
                assertEquals(0, knp.getTransferSize());
                assertEquals(0, knp.getTransferProgress());
                assertEquals(DownloadStatus.FAILED.getStatusString(), knp.getTransferStatus());
                assertEquals(jobId, (long) knp.getJobId());
                assertNotNull(knp.getErrorMessage());
                assertEquals("Error during download process and verification!", knp.getErrorMessage());
                assertEquals(0, (int) knp.getMissingDownloads());

                return null;
            }
        }).when(callback).publishMessage(anyObject(), anyObject(), anyObject());

        di.downloadDeploymentPackageInternal();

        verify(callback).publishMessage(anyObject(), anyObject(), anyObject());
        verify(callback, times(0)).installDownloadedFile(anyObject(), anyObject());
    }

    @Test
    public void testDownloadInProgressSyncMessage() {
        long jobId = 1234L;

        KuraResponsePayload respPayload = new KuraResponsePayload(KuraResponsePayload.RESPONSE_CODE_OK);
        DownloadCountingOutputStream downloadHelper = mock(DownloadCountingOutputStream.class);
        DeploymentPackageDownloadOptions downloadOptions = new DeploymentPackageDownloadOptions("", "", "");
        downloadOptions.setJobId(jobId);

        Long size = 1230L;
        Long progress = 75L;
        DownloadStatus statuss = DownloadStatus.IN_PROGRESS;
        when(downloadHelper.getTotalBytes()).thenReturn(size);
        when(downloadHelper.getDownloadTransferProgressPercentage()).thenReturn(progress);
        when(downloadHelper.getDownloadTransferStatus()).thenReturn(statuss);

        DownloadImpl.downloadInProgressSyncMessage(respPayload, downloadHelper, downloadOptions);

        assertNotNull(respPayload.getTimestamp());
        assertEquals(size.intValue(), respPayload.getMetric(KuraNotifyPayload.METRIC_TRANSFER_SIZE));
        assertEquals(progress.intValue(), respPayload.getMetric(KuraNotifyPayload.METRIC_TRANSFER_PROGRESS));
        assertEquals(statuss.getStatusString(), respPayload.getMetric(KuraNotifyPayload.METRIC_TRANSFER_STATUS));
        assertEquals(jobId, respPayload.getMetric(KuraNotifyPayload.METRIC_JOB_ID));
    }

    @Test
    public void testDownloadAlreadyDoneSyncMessage() {
        KuraResponsePayload respPayload = new KuraResponsePayload(KuraResponsePayload.RESPONSE_CODE_OK);

        DownloadImpl.downloadAlreadyDoneSyncMessage(respPayload);

        assertNotNull(respPayload.getTimestamp());
        assertEquals(0, respPayload.getMetric(KuraNotifyPayload.METRIC_TRANSFER_SIZE));
        assertEquals(100, respPayload.getMetric(KuraNotifyPayload.METRIC_TRANSFER_PROGRESS));
        assertEquals(DownloadStatus.ALREADY_DONE.getStatusString(),
                respPayload.getMetric(KuraNotifyPayload.METRIC_TRANSFER_STATUS));
        assertNull(respPayload.getMetric(KuraNotifyPayload.METRIC_JOB_ID));
    }

    @Test(expected = NullPointerException.class)
    public void testIncrementalDownloadFromURLWrongProtocol() throws Throwable {
        String deployUri = "uri";
        String dpName = "name";
        String dpVersion = "version";
        long jobId = 1234L;

        DeploymentPackageDownloadOptions options = new DeploymentPackageDownloadOptions(deployUri, dpName, dpVersion);
        options.setJobId(jobId);
        options.setDownloadProtocol("ftp");
        CloudDeploymentHandlerV2 callback = mock(CloudDeploymentHandlerV2.class);

        DownloadImpl di = new DownloadImpl(options, callback);

        File dpFile = new File("/tmp/dpfile.dp");
        String url = "url";
        int downloadIndex = 1;

        TestUtil.invokePrivate(di, "incrementalDownloadFromURL", dpFile, url, downloadIndex);
    }

    @Test
    public void testIncrementalDownloadFromURLNullHashAlgorithm() throws Throwable {
        String deployUri = "uri";
        String dpName = "name";
        String dpVersion = "version";
        long jobId = 1234L;

        DeploymentPackageDownloadOptions options = new DeploymentPackageDownloadOptions(deployUri, dpName, dpVersion);
        options.setJobId(jobId);
        options.setDownloadProtocol("HTTP");
        options.setHash("hash");
        CloudDeploymentHandlerV2 callback = mock(CloudDeploymentHandlerV2.class);

        DownloadCountingOutputStream dcosMock = mock(DownloadCountingOutputStream.class);

        DownloadImpl di = new DownloadImpl(options, callback) {

            @Override
            protected DownloadCountingOutputStream getDownloadInstance(String protocol,
                    DownloadOptions downloadOptions) {

                assertTrue(true);

                return dcosMock;
            }
        };

        File dpFile = new File("/tmp/dpfile.dp");
        dpFile.createNewFile();
        dpFile.deleteOnExit();
        String url = "http://localhost/nonexistingfile.dp";
        int downloadIndex = 1;

        doNothing().when(dcosMock).startWork();
        doNothing().when(dcosMock).close();

        try {
            TestUtil.invokePrivate(di, "incrementalDownloadFromURL", dpFile, url, downloadIndex);
        } catch (KuraException e) {
            assertTrue("Error should contain the expected message",
                    e.getMessage().contains("Failed to verify checksum with empty algorithm"));
        }
    }

    @Test
    public void testIncrementalDownloadFromURLWrongHash() throws Throwable {
        String deployUri = "uri";
        String dpName = "name";
        String dpVersion = "version";
        long jobId = 1234L;

        DeploymentPackageDownloadOptions options = new DeploymentPackageDownloadOptions(deployUri, dpName, dpVersion);
        options.setJobId(jobId);
        options.setDownloadProtocol("HTTP");
        options.setHash("MD5:hash");
        CloudDeploymentHandlerV2 callback = mock(CloudDeploymentHandlerV2.class);

        DownloadCountingOutputStream dcosMock = mock(DownloadCountingOutputStream.class);

        DownloadImpl di = new DownloadImpl(options, callback) {

            @Override
            protected DownloadCountingOutputStream getDownloadInstance(String protocol,
                    DownloadOptions downloadOptions) {

                assertTrue(true);

                return dcosMock;
            }
        };

        File dpFile = new File("/tmp/dpfile.dp");
        dpFile.createNewFile();
        dpFile.deleteOnExit();
        String url = "http://localhost/nonexistingfile.dp";
        int downloadIndex = 1;

        doNothing().when(dcosMock).startWork();
        doNothing().when(dcosMock).close();

        try {
            TestUtil.invokePrivate(di, "incrementalDownloadFromURL", dpFile, url, downloadIndex);
        } catch (KuraException e) {
            assertTrue("Error should contain the expected message",
                    e.getMessage().contains("Failed to verify checksum with algorithm: MD5"));
        }

        assertFalse(dpFile.exists());
    }

    @Test
    public void testIncrementalDownloadFromURL() throws Throwable {
        String deployUri = "uri";
        String dpName = "name";
        String dpVersion = "version";
        long jobId = 1234L;

        DeploymentPackageDownloadOptions options = new DeploymentPackageDownloadOptions(deployUri, dpName, dpVersion);
        options.setJobId(jobId);
        options.setDownloadProtocol("HTTP");
        options.setHash("MD5:d41d8cd98f00b204e9800998ecf8427e");
        CloudDeploymentHandlerV2 callback = mock(CloudDeploymentHandlerV2.class);

        DownloadCountingOutputStream dcosMock = mock(DownloadCountingOutputStream.class);

        DownloadImpl di = new DownloadImpl(options, callback) {

            @Override
            protected DownloadCountingOutputStream getDownloadInstance(String protocol,
                    DownloadOptions downloadOptions) {

                assertTrue(true);

                return dcosMock;
            }
        };

        File dpFile = new File("/tmp/dpfile.dp");
        dpFile.createNewFile();
        dpFile.deleteOnExit();
        String url = "http://localhost/nonexistingfile.dp";
        int downloadIndex = 1;

        doNothing().when(dcosMock).startWork();
        doNothing().when(dcosMock).close();

        TestUtil.invokePrivate(di, "incrementalDownloadFromURL", dpFile, url, downloadIndex);

        assertTrue(dpFile.exists());
    }

    @Test
    public void testAlreadyDownloadedAsync() throws Throwable {
        String deployUri = "uri";
        String dpName = "name";
        String dpVersion = "version";
        long jobId = 1234L;

        DeploymentPackageDownloadOptions options = new DeploymentPackageDownloadOptions(deployUri, dpName, dpVersion);
        options.setJobId(jobId);
        CloudDeploymentHandlerV2 callback = mock(CloudDeploymentHandlerV2.class);

        DownloadImpl di = new DownloadImpl(options, callback);

        doAnswer(new Answer<Void>() {

            @Override
            public Void answer(InvocationOnMock invocation) throws Throwable {
                Object[] args = invocation.getArguments();
                assertEquals(3, args.length);

                assertEquals(options, args[0]);
                assertEquals(DownloadImpl.RESOURCE_DOWNLOAD, args[2]);

                assertNotNull(args[1]);

                KuraNotifyPayload knp = (KuraNotifyPayload) args[1];

                assertNotNull(knp.getTimestamp());
                assertEquals(0, knp.getTransferSize());
                assertEquals(100, knp.getTransferProgress());
                assertEquals(DownloadStatus.COMPLETED.getStatusString(), knp.getTransferStatus());
                assertEquals(jobId, (long) knp.getJobId());
                assertNull(knp.getErrorMessage());

                return null;
            }
        }).when(callback).publishMessage(anyObject(), anyObject(), anyObject());

        TestUtil.invokePrivate(di, "alreadyDownloadedAsync");

        verify(callback).publishMessage(anyObject(), anyObject(), anyObject());
        verify(callback, times(0)).installDownloadedFile(anyObject(), anyObject());
    }

}
