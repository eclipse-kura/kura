package org.eclipse.kura.core.deployment;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Matchers.anyObject;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import org.eclipse.kura.KuraErrorCode;
import org.eclipse.kura.KuraException;
import org.eclipse.kura.cloud.CloudletTopic;
import org.eclipse.kura.configuration.ConfigurationService;
import org.eclipse.kura.core.deployment.CloudDeploymentHandlerV2.DOWNLOAD_STATUS;
import org.eclipse.kura.core.deployment.download.DeploymentPackageDownloadOptions;
import org.eclipse.kura.core.deployment.download.DownloadCountingOutputStream;
import org.eclipse.kura.core.deployment.download.impl.DownloadImpl;
import org.eclipse.kura.core.deployment.hook.DeploymentHookManager;
import org.eclipse.kura.core.deployment.install.DeploymentPackageInstallOptions;
import org.eclipse.kura.core.deployment.uninstall.UninstallImpl;
import org.eclipse.kura.core.testutil.TestUtil;
import org.eclipse.kura.data.DataTransportService;
import org.eclipse.kura.deployment.hook.DeploymentHook;
import org.eclipse.kura.deployment.hook.RequestContext;
import org.eclipse.kura.message.KuraRequestPayload;
import org.eclipse.kura.message.KuraResponsePayload;
import org.junit.Test;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleException;
import org.osgi.framework.ServiceReference;

public class CloudDeploymentHandlerV2Test {

    @Test
    public void testDoGetNoResources() throws KuraException, NoSuchFieldException {
        CloudDeploymentHandlerV2 handler = new CloudDeploymentHandlerV2();
        TestUtil.setFieldValue(handler, "componentOptions", new CloudDeploymentHandlerV2Options(new HashMap<>()));

        CloudletTopic reqTopic = CloudletTopic.parseAppTopic("GET");
        KuraRequestPayload reqPayload = new KuraRequestPayload();
        KuraResponsePayload respPayload = new KuraResponsePayload(KuraResponsePayload.RESPONSE_CODE_OK);

        handler.doGet(reqTopic, reqPayload, respPayload);

        assertEquals("Response code should match expected", KuraResponsePayload.RESPONSE_CODE_BAD_REQUEST,
                respPayload.getResponseCode());
    }

    @Test
    public void testDoGetOtherwise() throws KuraException, NoSuchFieldException {
        CloudDeploymentHandlerV2 handler = new CloudDeploymentHandlerV2();
        TestUtil.setFieldValue(handler, "componentOptions", new CloudDeploymentHandlerV2Options(new HashMap<>()));

        CloudletTopic reqTopic = CloudletTopic.parseAppTopic("GET/test");
        KuraRequestPayload reqPayload = new KuraRequestPayload();
        KuraResponsePayload respPayload = new KuraResponsePayload(KuraResponsePayload.RESPONSE_CODE_OK);

        handler.doGet(reqTopic, reqPayload, respPayload);

        assertEquals("Response code should match expected", KuraResponsePayload.RESPONSE_CODE_NOTFOUND,
                respPayload.getResponseCode());
    }

    @Test
    public void testDoDelNoResources() throws KuraException, NoSuchFieldException {
        CloudDeploymentHandlerV2 handler = new CloudDeploymentHandlerV2();
        TestUtil.setFieldValue(handler, "componentOptions", new CloudDeploymentHandlerV2Options(new HashMap<>()));

        CloudletTopic reqTopic = CloudletTopic.parseAppTopic("DEL");
        KuraRequestPayload reqPayload = new KuraRequestPayload();
        KuraResponsePayload respPayload = new KuraResponsePayload(KuraResponsePayload.RESPONSE_CODE_OK);

        handler.doDel(reqTopic, reqPayload, respPayload);

        assertEquals("Response code should match expected", KuraResponsePayload.RESPONSE_CODE_BAD_REQUEST,
                respPayload.getResponseCode());
    }

    @Test
    public void testDoDelOtherwise() throws KuraException, NoSuchFieldException {
        CloudDeploymentHandlerV2 handler = new CloudDeploymentHandlerV2();
        TestUtil.setFieldValue(handler, "componentOptions", new CloudDeploymentHandlerV2Options(new HashMap<>()));

        CloudletTopic reqTopic = CloudletTopic.parseAppTopic("DEL/test");
        KuraRequestPayload reqPayload = new KuraRequestPayload();
        KuraResponsePayload respPayload = new KuraResponsePayload(KuraResponsePayload.RESPONSE_CODE_OK);

        handler.doDel(reqTopic, reqPayload, respPayload);

        assertEquals("Response code should match expected", KuraResponsePayload.RESPONSE_CODE_NOTFOUND,
                respPayload.getResponseCode());
    }

    @Test
    public void testDoDelNormal() throws Exception {
        CloudDeploymentHandlerV2 handler = new CloudDeploymentHandlerV2();
        TestUtil.setFieldValue(handler, "componentOptions", new CloudDeploymentHandlerV2Options(new HashMap<>()));

        DownloadImpl dlMock = mock(DownloadImpl.class);
        TestUtil.setFieldValue(handler, "downloadImplementation", dlMock);

        CloudletTopic reqTopic = CloudletTopic.parseAppTopic("DEL/" + CloudDeploymentHandlerV2.RESOURCE_DOWNLOAD);
        KuraRequestPayload reqPayload = new KuraRequestPayload();
        KuraResponsePayload respPayload = new KuraResponsePayload(KuraResponsePayload.RESPONSE_CODE_OK);

        DownloadCountingOutputStream stream = mock(DownloadCountingOutputStream.class);
        when(dlMock.getDownloadHelper()).thenReturn(stream);

        when(dlMock.deleteDownloadedFile()).thenReturn(true);

        handler.doDel(reqTopic, reqPayload, respPayload);

        verify(dlMock).getDownloadHelper();
        verify(dlMock).deleteDownloadedFile();
        verify(stream).cancelDownload();
    }

    @Test
    public void testDoDelException() throws Exception {
        CloudDeploymentHandlerV2 handler = new CloudDeploymentHandlerV2();
        TestUtil.setFieldValue(handler, "componentOptions", new CloudDeploymentHandlerV2Options(new HashMap<>()));

        DownloadImpl dlMock = mock(DownloadImpl.class);
        String mesg = "test";
        when(dlMock.getDownloadHelper()).thenThrow(new RuntimeException(mesg));
        TestUtil.setFieldValue(handler, "downloadImplementation", dlMock);

        CloudletTopic reqTopic = CloudletTopic.parseAppTopic("DEL/" + CloudDeploymentHandlerV2.RESOURCE_DOWNLOAD);
        KuraRequestPayload reqPayload = new KuraRequestPayload();
        KuraResponsePayload respPayload = new KuraResponsePayload(KuraResponsePayload.RESPONSE_CODE_OK);

        handler.doDel(reqTopic, reqPayload, respPayload);

        verify(dlMock).getDownloadHelper();

        assertEquals("Response code should match expected", KuraResponsePayload.RESPONSE_CODE_ERROR,
                respPayload.getResponseCode());
        assertNotNull("Response timestamp should be set", respPayload.getTimestamp());
        assertEquals("Response exception message should match", mesg, respPayload.getExceptionMessage());
    }

    @Test
    public void testDoExecNoResources() throws KuraException, NoSuchFieldException {
        CloudDeploymentHandlerV2 handler = new CloudDeploymentHandlerV2();
        TestUtil.setFieldValue(handler, "componentOptions", new CloudDeploymentHandlerV2Options(new HashMap<>()));

        CloudletTopic reqTopic = CloudletTopic.parseAppTopic("EXEC");
        KuraRequestPayload reqPayload = new KuraRequestPayload();
        KuraResponsePayload respPayload = new KuraResponsePayload(KuraResponsePayload.RESPONSE_CODE_OK);

        handler.doExec(reqTopic, reqPayload, respPayload);

        assertEquals("Response code should match expected", KuraResponsePayload.RESPONSE_CODE_BAD_REQUEST,
                respPayload.getResponseCode());
    }

    @Test
    public void testDoExecOtherwise() throws KuraException, NoSuchFieldException {
        CloudDeploymentHandlerV2 handler = new CloudDeploymentHandlerV2();
        TestUtil.setFieldValue(handler, "componentOptions", new CloudDeploymentHandlerV2Options(new HashMap<>()));

        CloudletTopic reqTopic = CloudletTopic.parseAppTopic("EXEC/test");
        KuraRequestPayload reqPayload = new KuraRequestPayload();
        KuraResponsePayload respPayload = new KuraResponsePayload(KuraResponsePayload.RESPONSE_CODE_OK);

        handler.doExec(reqTopic, reqPayload, respPayload);

        assertEquals("Response code should match expected", KuraResponsePayload.RESPONSE_CODE_NOTFOUND,
                respPayload.getResponseCode());
    }

    @Test
    public void testDoExecInstallDeploymentOptionsException() throws KuraException, NoSuchFieldException {
        // fail immediately after calling doExecInstall

        CloudDeploymentHandlerV2 handler = new CloudDeploymentHandlerV2();
        TestUtil.setFieldValue(handler, "componentOptions", new CloudDeploymentHandlerV2Options(new HashMap<>()));

        CloudletTopic reqTopic = CloudletTopic.parseAppTopic("EXEC/" + CloudDeploymentHandlerV2.RESOURCE_INSTALL);
        KuraRequestPayload reqPayload = new KuraRequestPayload();
        KuraResponsePayload respPayload = new KuraResponsePayload(KuraResponsePayload.RESPONSE_CODE_OK);

        handler.doExec(reqTopic, reqPayload, respPayload);

        assertEquals("Response code should match expected", KuraResponsePayload.RESPONSE_CODE_ERROR,
                respPayload.getResponseCode());
        assertNotNull("Response timestamp should be set", respPayload.getTimestamp());
    }

    @Test
    public void testDoExecInstallPendingPackage() throws KuraException, NoSuchFieldException {
        // fail at pending test

        CloudDeploymentHandlerV2 handler = new CloudDeploymentHandlerV2();
        TestUtil.setFieldValue(handler, "componentOptions", new CloudDeploymentHandlerV2Options(new HashMap<>()));

        CloudletTopic reqTopic = CloudletTopic.parseAppTopic("EXEC/" + CloudDeploymentHandlerV2.RESOURCE_INSTALL);
        KuraRequestPayload reqPayload = new KuraRequestPayload();
        KuraResponsePayload respPayload = new KuraResponsePayload(KuraResponsePayload.RESPONSE_CODE_OK);

        reqPayload.addMetric(DeploymentPackageOptions.METRIC_DP_NAME, "");
        reqPayload.addMetric(DeploymentPackageOptions.METRIC_DP_VERSION, "");
        reqPayload.addMetric(DeploymentPackageOptions.METRIC_JOB_ID, 1234L);
        reqPayload.addMetric(DeploymentPackageInstallOptions.METRIC_DP_INSTALL_SYSTEM_UPDATE, false);

        DataTransportService dtsMock = mock(DataTransportService.class);
        handler.setDataTransportService(dtsMock);

        DownloadImpl dlMock = mock(DownloadImpl.class);
        String mesg = "test";
        KuraException ex = new KuraException(KuraErrorCode.OPERATION_NOT_SUPPORTED);
        when(dlMock.isAlreadyDownloaded()).thenThrow(ex);
        TestUtil.setFieldValue(handler, "downloadImplementation", dlMock);

        handler.doExec(reqTopic, reqPayload, respPayload);

        assertEquals("Response code should match expected", KuraResponsePayload.RESPONSE_CODE_ERROR,
                respPayload.getResponseCode());
        assertNotNull("Response timestamp should be set", respPayload.getTimestamp());
        assertEquals("Exception message should be the one expected", ex.getMessage(),
                respPayload.getExceptionMessage());
    }

    @Test
    public void testDoExecInstallNotDownloaded() throws KuraException, NoSuchFieldException {
        // fail because not downloaded

        CloudDeploymentHandlerV2 handler = new CloudDeploymentHandlerV2();
        TestUtil.setFieldValue(handler, "componentOptions", new CloudDeploymentHandlerV2Options(new HashMap<>()));

        CloudletTopic reqTopic = CloudletTopic.parseAppTopic("EXEC/" + CloudDeploymentHandlerV2.RESOURCE_INSTALL);
        KuraRequestPayload reqPayload = new KuraRequestPayload();
        KuraResponsePayload respPayload = new KuraResponsePayload(KuraResponsePayload.RESPONSE_CODE_OK);

        reqPayload.addMetric(DeploymentPackageOptions.METRIC_DP_NAME, "");
        reqPayload.addMetric(DeploymentPackageOptions.METRIC_DP_VERSION, "");
        reqPayload.addMetric(DeploymentPackageOptions.METRIC_JOB_ID, 1234L);
        reqPayload.addMetric(DeploymentPackageInstallOptions.METRIC_DP_INSTALL_SYSTEM_UPDATE, false);

        DataTransportService dtsMock = mock(DataTransportService.class);
        handler.setDataTransportService(dtsMock);

        DownloadImpl dlMock = mock(DownloadImpl.class);
        when(dlMock.isAlreadyDownloaded()).thenReturn(false);
        TestUtil.setFieldValue(handler, "downloadImplementation", dlMock);

        handler.doExec(reqTopic, reqPayload, respPayload);

        assertEquals("Response code should match expected", KuraResponsePayload.RESPONSE_CODE_ERROR,
                respPayload.getResponseCode());
        assertNotNull("Response timestamp should be set", respPayload.getTimestamp());
        assertTrue(respPayload.getExceptionMessage().contains("An internal error occurred."));

    }

    @Test
    public void testDoExecInstallDownloaded() throws KuraException, NoSuchFieldException {
        // fail without file

        CloudDeploymentHandlerV2 handler = new CloudDeploymentHandlerV2() {

            @Override
            protected File getDpDownloadFile(DeploymentPackageInstallOptions options) throws IOException {
                throw new IOException("test");
            }
        };
        TestUtil.setFieldValue(handler, "componentOptions", new CloudDeploymentHandlerV2Options(new HashMap<>()));

        CloudletTopic reqTopic = CloudletTopic.parseAppTopic("EXEC/" + CloudDeploymentHandlerV2.RESOURCE_INSTALL);
        KuraRequestPayload reqPayload = new KuraRequestPayload();
        KuraResponsePayload respPayload = new KuraResponsePayload(KuraResponsePayload.RESPONSE_CODE_OK);

        reqPayload.addMetric(DeploymentPackageOptions.METRIC_DP_NAME, "");
        reqPayload.addMetric(DeploymentPackageOptions.METRIC_DP_VERSION, "");
        reqPayload.addMetric(DeploymentPackageOptions.METRIC_JOB_ID, 1234L);
        reqPayload.addMetric(DeploymentPackageInstallOptions.METRIC_DP_INSTALL_SYSTEM_UPDATE, false);

        DataTransportService dtsMock = mock(DataTransportService.class);
        handler.setDataTransportService(dtsMock);

        DownloadImpl dlMock = mock(DownloadImpl.class);
        when(dlMock.isAlreadyDownloaded()).thenReturn(true);
        TestUtil.setFieldValue(handler, "downloadImplementation", dlMock);

        TestUtil.setFieldValue(handler, "isInstalling", false);

        handler.doExec(reqTopic, reqPayload, respPayload);

        assertEquals("Response code should match expected", KuraResponsePayload.RESPONSE_CODE_ERROR,
                respPayload.getResponseCode());
        assertNotNull("Response timestamp should be set", respPayload.getTimestamp());
        assertEquals("Exception message should match", "test", respPayload.getExceptionMessage());
        assertEquals("Body should match", "Exception during install",
                new String(respPayload.getBody(), Charset.forName("UTF-8")));
    }

    @Test
    public void testDoExecDownloadOptionsException() throws KuraException, NoSuchFieldException {
        // fail soon after calling doExecDownload

        CloudDeploymentHandlerV2 handler = new CloudDeploymentHandlerV2();
        TestUtil.setFieldValue(handler, "componentOptions", new CloudDeploymentHandlerV2Options(new HashMap<>()));

        CloudletTopic reqTopic = CloudletTopic.parseAppTopic("EXEC/" + CloudDeploymentHandlerV2.RESOURCE_DOWNLOAD);
        KuraRequestPayload reqPayload = new KuraRequestPayload();
        KuraResponsePayload respPayload = new KuraResponsePayload(KuraResponsePayload.RESPONSE_CODE_OK);

        handler.doExec(reqTopic, reqPayload, respPayload);

        assertEquals("Response code should match expected", KuraResponsePayload.RESPONSE_CODE_ERROR,
                respPayload.getResponseCode());
        assertNotNull("Response timestamp should be set", respPayload.getTimestamp());
        assertEquals("Body should match", "Malformed download request",
                new String(respPayload.getBody(), Charset.forName("UTF-8")));
    }

    @Test
    public void testDoExecDownloadPendingUrl() throws KuraException, NoSuchFieldException {
        // fail at pending url

        CloudDeploymentHandlerV2 handler = new CloudDeploymentHandlerV2();
        TestUtil.setFieldValue(handler, "componentOptions", new CloudDeploymentHandlerV2Options(new HashMap<>()));

        CloudletTopic reqTopic = CloudletTopic.parseAppTopic("EXEC/" + CloudDeploymentHandlerV2.RESOURCE_DOWNLOAD);
        KuraRequestPayload reqPayload = new KuraRequestPayload();
        KuraResponsePayload respPayload = new KuraResponsePayload(KuraResponsePayload.RESPONSE_CODE_OK);

        reqPayload.addMetric(DeploymentPackageDownloadOptions.METRIC_DP_DOWNLOAD_URI, "");
        reqPayload.addMetric(DeploymentPackageDownloadOptions.METRIC_DP_DOWNLOAD_PROTOCOL, "");
        reqPayload.addMetric(DeploymentPackageOptions.METRIC_DP_NAME, "");
        reqPayload.addMetric(DeploymentPackageOptions.METRIC_DP_VERSION, "");
        reqPayload.addMetric(DeploymentPackageOptions.METRIC_JOB_ID, 1234L);
        reqPayload.addMetric(DeploymentPackageInstallOptions.METRIC_DP_INSTALL_SYSTEM_UPDATE, false);

        DataTransportService dtsMock = mock(DataTransportService.class);
        handler.setDataTransportService(dtsMock);

        when(dtsMock.getClientId()).thenReturn("ClientId");

        TestUtil.setFieldValue(handler, "pendingPackageUrl", "url");

        handler.doExec(reqTopic, reqPayload, respPayload);

        TestUtil.setFieldValue(handler, "pendingPackageUrl", null);

        assertEquals("Response code should match expected", KuraResponsePayload.RESPONSE_CODE_ERROR,
                respPayload.getResponseCode());
        assertNotNull("Response timestamp should be set", respPayload.getTimestamp());
        assertEquals("Body should match", "Another resource is already in download",
                new String(respPayload.getBody(), Charset.forName("UTF-8")));
        assertEquals(DOWNLOAD_STATUS.IN_PROGRESS.getStatusString(),
                respPayload.getMetric(CloudDeploymentHandlerV2.METRIC_DOWNLOAD_STATUS));
    }

    @Test
    public void testDoExecDownloadIsDownloadedException() throws KuraException, NoSuchFieldException {
        // fail with DownloadImpl exception

        DownloadImpl dlMock = mock(DownloadImpl.class);

        CloudDeploymentHandlerV2 handler = new CloudDeploymentHandlerV2() {

            @Override
            protected DownloadImpl createDownloadImpl(DeploymentPackageDownloadOptions options) {
                return dlMock;
            }
        };
        TestUtil.setFieldValue(handler, "componentOptions", new CloudDeploymentHandlerV2Options(new HashMap<>()));

        CloudletTopic reqTopic = CloudletTopic.parseAppTopic("EXEC/" + CloudDeploymentHandlerV2.RESOURCE_DOWNLOAD);
        KuraRequestPayload reqPayload = new KuraRequestPayload();
        KuraResponsePayload respPayload = new KuraResponsePayload(KuraResponsePayload.RESPONSE_CODE_OK);

        reqPayload.addMetric(DeploymentPackageDownloadOptions.METRIC_DP_DOWNLOAD_URI, "");
        reqPayload.addMetric(DeploymentPackageDownloadOptions.METRIC_DP_DOWNLOAD_PROTOCOL, "");
        reqPayload.addMetric(DeploymentPackageOptions.METRIC_DP_NAME, "");
        reqPayload.addMetric(DeploymentPackageOptions.METRIC_DP_VERSION, "");
        reqPayload.addMetric(DeploymentPackageOptions.METRIC_JOB_ID, 1234L);
        reqPayload.addMetric(DeploymentPackageInstallOptions.METRIC_DP_INSTALL_SYSTEM_UPDATE, false);

        DataTransportService dtsMock = mock(DataTransportService.class);
        handler.setDataTransportService(dtsMock);

        when(dtsMock.getClientId()).thenReturn("ClientId");

        Exception ex = new KuraException(KuraErrorCode.NOT_CONNECTED);
        when(dlMock.isAlreadyDownloaded()).thenThrow(ex);

        handler.doExec(reqTopic, reqPayload, respPayload);

        assertEquals("Response code should match expected", KuraResponsePayload.RESPONSE_CODE_ERROR,
                respPayload.getResponseCode());
        assertNotNull("Response timestamp should be set", respPayload.getTimestamp());
        assertEquals("Body should match", "Error checking download status",
                new String(respPayload.getBody(), Charset.forName("UTF-8")));

        verify(dlMock).isAlreadyDownloaded();
    }

    @Test
    public void testDoExecDownloadDownloaderException() throws KuraException, NoSuchFieldException {
        // fail soon after calling doExecDownload

        DownloadImpl dlMock = mock(DownloadImpl.class);

        CloudDeploymentHandlerV2 handler = new CloudDeploymentHandlerV2() {

            @Override
            protected DownloadImpl createDownloadImpl(DeploymentPackageDownloadOptions options) {
                return dlMock;
            }
        };
        TestUtil.setFieldValue(handler, "componentOptions", new CloudDeploymentHandlerV2Options(new HashMap<>()));

        CloudletTopic reqTopic = CloudletTopic.parseAppTopic("EXEC/" + CloudDeploymentHandlerV2.RESOURCE_DOWNLOAD);
        KuraRequestPayload reqPayload = new KuraRequestPayload();
        KuraResponsePayload respPayload = new KuraResponsePayload(KuraResponsePayload.RESPONSE_CODE_OK);

        reqPayload.addMetric(DeploymentPackageDownloadOptions.METRIC_DP_DOWNLOAD_URI, "");
        reqPayload.addMetric(DeploymentPackageDownloadOptions.METRIC_DP_DOWNLOAD_PROTOCOL, "");
        reqPayload.addMetric(DeploymentPackageOptions.METRIC_DP_NAME, "");
        reqPayload.addMetric(DeploymentPackageOptions.METRIC_DP_VERSION, "");
        reqPayload.addMetric(DeploymentPackageOptions.METRIC_JOB_ID, 1234L);
        reqPayload.addMetric(DeploymentPackageInstallOptions.METRIC_DP_INSTALL_SYSTEM_UPDATE, false);

        DataTransportService dtsMock = mock(DataTransportService.class);
        handler.setDataTransportService(dtsMock);

        when(dtsMock.getClientId()).thenReturn("ClientId");

        when(dlMock.isAlreadyDownloaded()).thenReturn(true);

        doThrow(new RuntimeException("test")).when(dlMock).setSslManager(null);

        handler.doExec(reqTopic, reqPayload, respPayload);

        assertEquals("Response code should match expected", KuraResponsePayload.RESPONSE_CODE_ERROR,
                respPayload.getResponseCode());
        assertNotNull("Response timestamp should be set", respPayload.getTimestamp());
        assertEquals("Body should match", "test", new String(respPayload.getBody(), Charset.forName("UTF-8")));

        verify(dlMock).isAlreadyDownloaded();
        verify(dlMock).setSslManager(null);
    }

    @Test
    public void testDoExecUninstallOptionsException() throws KuraException, NoSuchFieldException {
        // fail soon after calling doExecUninstall

        CloudDeploymentHandlerV2 handler = new CloudDeploymentHandlerV2();
        TestUtil.setFieldValue(handler, "componentOptions", new CloudDeploymentHandlerV2Options(new HashMap<>()));

        CloudletTopic reqTopic = CloudletTopic.parseAppTopic("EXEC/" + CloudDeploymentHandlerV2.RESOURCE_UNINSTALL);
        KuraRequestPayload reqPayload = new KuraRequestPayload();
        KuraResponsePayload respPayload = new KuraResponsePayload(KuraResponsePayload.RESPONSE_CODE_OK);

        handler.doExec(reqTopic, reqPayload, respPayload);

        assertEquals("Response code should match expected", KuraResponsePayload.RESPONSE_CODE_ERROR,
                respPayload.getResponseCode());
        assertNotNull("Response timestamp should be set", respPayload.getTimestamp());
        assertEquals("Body should match", "Malformed uninstall request",
                new String(respPayload.getBody(), Charset.forName("UTF-8")));
    }

    @Test
    public void testDoExecUninstallPendingUrl() throws KuraException, NoSuchFieldException {
        // fail at installing/pending package name

        CloudDeploymentHandlerV2 handler = new CloudDeploymentHandlerV2();
        TestUtil.setFieldValue(handler, "componentOptions", new CloudDeploymentHandlerV2Options(new HashMap<>()));

        CloudletTopic reqTopic = CloudletTopic.parseAppTopic("EXEC/" + CloudDeploymentHandlerV2.RESOURCE_UNINSTALL);
        KuraRequestPayload reqPayload = new KuraRequestPayload();
        KuraResponsePayload respPayload = new KuraResponsePayload(KuraResponsePayload.RESPONSE_CODE_OK);

        reqPayload.addMetric(DeploymentPackageOptions.METRIC_DP_NAME, "");
        reqPayload.addMetric(DeploymentPackageOptions.METRIC_JOB_ID, 1234L);

        DataTransportService dtsMock = mock(DataTransportService.class);
        handler.setDataTransportService(dtsMock);

        when(dtsMock.getClientId()).thenReturn("ClientId");

        TestUtil.setFieldValue(handler, "isInstalling", false);
        TestUtil.setFieldValue(handler, "pendingUninstPackageName", "");

        handler.doExec(reqTopic, reqPayload, respPayload);

        assertEquals("Response code should match expected", KuraResponsePayload.RESPONSE_CODE_ERROR,
                respPayload.getResponseCode());
        assertNotNull("Response timestamp should be set", respPayload.getTimestamp());
        assertEquals("Body should match", "Only one request at a time is allowed",
                new String(respPayload.getBody(), Charset.forName("UTF-8")));
    }

    @Test
    public void testDoExecUninstallIsDownloadedException() throws KuraException, NoSuchFieldException {
        // fail with UninstallImpl exception
        String testMesg = "testMesg";

        CloudDeploymentHandlerV2 handler = new CloudDeploymentHandlerV2() {

            @Override
            protected UninstallImpl createUninstallImpl() {
                throw new RuntimeException(testMesg);
            }
        };
        TestUtil.setFieldValue(handler, "componentOptions", new CloudDeploymentHandlerV2Options(new HashMap<>()));

        CloudletTopic reqTopic = CloudletTopic.parseAppTopic("EXEC/" + CloudDeploymentHandlerV2.RESOURCE_UNINSTALL);
        KuraRequestPayload reqPayload = new KuraRequestPayload();
        KuraResponsePayload respPayload = new KuraResponsePayload(KuraResponsePayload.RESPONSE_CODE_OK);

        reqPayload.addMetric(DeploymentPackageDownloadOptions.METRIC_DP_DOWNLOAD_URI, "");
        reqPayload.addMetric(DeploymentPackageDownloadOptions.METRIC_DP_DOWNLOAD_PROTOCOL, "");
        reqPayload.addMetric(DeploymentPackageOptions.METRIC_DP_NAME, "");
        reqPayload.addMetric(DeploymentPackageOptions.METRIC_DP_VERSION, "");
        reqPayload.addMetric(DeploymentPackageOptions.METRIC_JOB_ID, 1234L);
        reqPayload.addMetric(DeploymentPackageInstallOptions.METRIC_DP_INSTALL_SYSTEM_UPDATE, false);

        DataTransportService dtsMock = mock(DataTransportService.class);
        handler.setDataTransportService(dtsMock);

        when(dtsMock.getClientId()).thenReturn("ClientId");

        TestUtil.setFieldValue(handler, "isInstalling", true);

        handler.doExec(reqTopic, reqPayload, respPayload);

        assertEquals("Response code should match expected", KuraResponsePayload.RESPONSE_CODE_ERROR,
                respPayload.getResponseCode());
        assertNotNull("Response timestamp should be set", respPayload.getTimestamp());
        assertEquals("Body should match", testMesg, new String(respPayload.getBody(), Charset.forName("UTF-8")));

        assertFalse((boolean) TestUtil.getFieldValue(handler, "isInstalling"));
        assertNull(TestUtil.getFieldValue(handler, "pendingUninstPackageName"));
    }

    @Test
    public void testDoExecStopNoBundleId() throws KuraException, NoSuchFieldException {
        // don't fail before the actual call

        CloudDeploymentHandlerV2 handler = new CloudDeploymentHandlerV2();
        TestUtil.setFieldValue(handler, "componentOptions", new CloudDeploymentHandlerV2Options(new HashMap<>()));

        CloudletTopic reqTopic = CloudletTopic.parseAppTopic("EXEC/" + CloudDeploymentHandlerV2.RESOURCE_STOP);
        KuraRequestPayload reqPayload = new KuraRequestPayload();
        KuraResponsePayload respPayload = new KuraResponsePayload(KuraResponsePayload.RESPONSE_CODE_OK);

        handler.doExec(reqTopic, reqPayload, respPayload);

        assertEquals("Response code should match expected", KuraResponsePayload.RESPONSE_CODE_BAD_REQUEST,
                respPayload.getResponseCode());
        assertNotNull("Response timestamp should be set", respPayload.getTimestamp());
        assertNull("Response exception should not be set", respPayload.getExceptionMessage());
    }

    @Test
    public void testDoExecStartNoBundleId() throws KuraException, NoSuchFieldException {
        // fail because of missing/null bundle id

        CloudDeploymentHandlerV2 handler = new CloudDeploymentHandlerV2();
        CloudletTopic reqTopic = CloudletTopic.parseAppTopic("EXEC/" + CloudDeploymentHandlerV2.RESOURCE_START);
        KuraRequestPayload reqPayload = new KuraRequestPayload();
        KuraResponsePayload respPayload = new KuraResponsePayload(KuraResponsePayload.RESPONSE_CODE_OK);

        handler.doExec(reqTopic, reqPayload, respPayload);

        assertEquals("Response code should match expected", KuraResponsePayload.RESPONSE_CODE_BAD_REQUEST,
                respPayload.getResponseCode());
        assertNotNull("Response timestamp should be set", respPayload.getTimestamp());
        assertNull("Response exception should not be set", respPayload.getExceptionMessage());
    }

    @Test
    public void testDoExecStartNFE() throws KuraException, NoSuchFieldException {
        // fail because of String bundle id

        CloudDeploymentHandlerV2 handler = new CloudDeploymentHandlerV2();
        TestUtil.setFieldValue(handler, "componentOptions", new CloudDeploymentHandlerV2Options(new HashMap<>()));

        CloudletTopic reqTopic = CloudletTopic.parseAppTopic("EXEC/" + CloudDeploymentHandlerV2.RESOURCE_START + "/aa");
        KuraRequestPayload reqPayload = new KuraRequestPayload();
        KuraResponsePayload respPayload = new KuraResponsePayload(KuraResponsePayload.RESPONSE_CODE_OK);

        handler.doExec(reqTopic, reqPayload, respPayload);

        assertEquals("Response code should match expected", KuraResponsePayload.RESPONSE_CODE_BAD_REQUEST,
                respPayload.getResponseCode());
        assertNotNull("Response timestamp should be set", respPayload.getTimestamp());
        assertNotNull("Response exception should be set", respPayload.getExceptionMessage());
        assertEquals("Response exception should be as expected", "For input string: \"aa\"",
                respPayload.getExceptionMessage());
    }

    @Test
    public void testDoExecStartNullBundle() throws KuraException, NoSuchFieldException {
        // fail because of null bundle

        CloudDeploymentHandlerV2 handler = new CloudDeploymentHandlerV2();
        TestUtil.setFieldValue(handler, "componentOptions", new CloudDeploymentHandlerV2Options(new HashMap<>()));

        CloudletTopic reqTopic = CloudletTopic.parseAppTopic("EXEC/" + CloudDeploymentHandlerV2.RESOURCE_START + "/99");
        KuraRequestPayload reqPayload = new KuraRequestPayload();
        KuraResponsePayload respPayload = new KuraResponsePayload(KuraResponsePayload.RESPONSE_CODE_OK);

        BundleContext ctxMock = mock(BundleContext.class);
        TestUtil.setFieldValue(handler, "m_bundleContext", ctxMock);

        when(ctxMock.getBundle(99)).thenReturn(null);

        handler.doExec(reqTopic, reqPayload, respPayload);

        assertEquals("Response code should match expected", KuraResponsePayload.RESPONSE_CODE_NOTFOUND,
                respPayload.getResponseCode());
        assertNotNull("Response timestamp should be set", respPayload.getTimestamp());

        verify(ctxMock).getBundle(99);
    }

    @Test
    public void testDoExecStartBundleExc() throws KuraException, NoSuchFieldException, BundleException {
        // fail because of start exception

        CloudDeploymentHandlerV2 handler = new CloudDeploymentHandlerV2();
        TestUtil.setFieldValue(handler, "componentOptions", new CloudDeploymentHandlerV2Options(new HashMap<>()));

        CloudletTopic reqTopic = CloudletTopic.parseAppTopic("EXEC/" + CloudDeploymentHandlerV2.RESOURCE_START + "/99");
        KuraRequestPayload reqPayload = new KuraRequestPayload();
        KuraResponsePayload respPayload = new KuraResponsePayload(KuraResponsePayload.RESPONSE_CODE_OK);

        BundleContext ctxMock = mock(BundleContext.class);
        TestUtil.setFieldValue(handler, "m_bundleContext", ctxMock);

        Bundle bundleMock = mock(Bundle.class);
        when(ctxMock.getBundle(99)).thenReturn(bundleMock);

        BundleException ex = new BundleException("test");
        doThrow(ex).when(bundleMock).start();

        handler.doExec(reqTopic, reqPayload, respPayload);

        assertEquals("Response code should match expected", KuraResponsePayload.RESPONSE_CODE_ERROR,
                respPayload.getResponseCode());
        assertNotNull("Response timestamp should be set", respPayload.getTimestamp());

        verify(ctxMock).getBundle(99);
        verify(bundleMock).start();
    }

    @Test
    public void testDoExecStopBundleExc() throws KuraException, NoSuchFieldException, BundleException {
        // OK

        CloudDeploymentHandlerV2 handler = new CloudDeploymentHandlerV2();
        TestUtil.setFieldValue(handler, "componentOptions", new CloudDeploymentHandlerV2Options(new HashMap<>()));

        CloudletTopic reqTopic = CloudletTopic.parseAppTopic("EXEC/" + CloudDeploymentHandlerV2.RESOURCE_STOP + "/99");
        KuraRequestPayload reqPayload = new KuraRequestPayload();
        KuraResponsePayload respPayload = new KuraResponsePayload(KuraResponsePayload.RESPONSE_CODE_OK);

        BundleContext ctxMock = mock(BundleContext.class);
        TestUtil.setFieldValue(handler, "m_bundleContext", ctxMock);

        Bundle bundleMock = mock(Bundle.class);
        when(ctxMock.getBundle(99)).thenReturn(bundleMock);

        handler.doExec(reqTopic, reqPayload, respPayload);

        assertEquals("Response code should match expected", KuraResponsePayload.RESPONSE_CODE_OK,
                respPayload.getResponseCode());
        assertNotNull("Response timestamp should be set", respPayload.getTimestamp());

        verify(ctxMock).getBundle(99);
        verify(bundleMock).stop();
    }

    private ServiceReference<DeploymentHook> mockHookReference(String kuraServicePid, DeploymentHook deploymentHook) {
        @SuppressWarnings("unchecked")
        final ServiceReference<DeploymentHook> reference = mock(ServiceReference.class);
        when(reference.getProperty(anyString())).then(invocation -> {
            String arg = invocation.getArgumentAt(0, String.class);
            if (ConfigurationService.KURA_SERVICE_PID.equals(arg)) {
                return kuraServicePid;
            }
            return deploymentHook;
        });
        return reference;
    }

    private DeploymentHookManager getDeploymentHookManager() throws NoSuchFieldException {
        final DeploymentHookManager result = new DeploymentHookManager();
        final BundleContext mockBundleContext = mock(BundleContext.class);
        when(mockBundleContext.getService(anyObject())).then(invocation -> {
            final ServiceReference<?> ref = invocation.getArgumentAt(0, ServiceReference.class);
            return ref.getProperty("service");
        });
        TestUtil.setFieldValue(result, "bundleContext", mockBundleContext);
        return result;
    }

    @Test
    public void testDoExecDownloadNoRegisteredHookException() throws KuraException, NoSuchFieldException {

        CloudDeploymentHandlerV2 handler = new CloudDeploymentHandlerV2();
        TestUtil.setFieldValue(handler, "componentOptions", new CloudDeploymentHandlerV2Options(new HashMap<>()));

        final DeploymentHookManager manager = getDeploymentHookManager();
        handler.setDeploymentHookManager(manager);
        handler.setDataTransportService(mock(DataTransportService.class));

        CloudletTopic reqTopic = CloudletTopic.parseAppTopic("EXEC/" + CloudDeploymentHandlerV2.RESOURCE_DOWNLOAD);
        KuraRequestPayload reqPayload = new KuraRequestPayload();

        reqPayload.addMetric(DeploymentPackageDownloadOptions.METRIC_DP_DOWNLOAD_URI, "http://localhost:1234");
        reqPayload.addMetric(DeploymentPackageDownloadOptions.METRIC_DP_DOWNLOAD_PROTOCOL, "HTTP");
        reqPayload.addMetric(DeploymentPackageOptions.METRIC_DP_NAME, "test");
        reqPayload.addMetric(DeploymentPackageOptions.METRIC_DP_VERSION, "1.0");
        reqPayload.addMetric(DeploymentPackageOptions.METRIC_JOB_ID, 1234L);
        reqPayload.addMetric(DeploymentPackageInstallOptions.METRIC_DP_INSTALL_SYSTEM_UPDATE, false);
        reqPayload.addMetric(DeploymentPackageDownloadOptions.METRIC_REQUEST_TYPE, "someRequestType");

        KuraResponsePayload respPayload = new KuraResponsePayload(KuraResponsePayload.RESPONSE_CODE_OK);

        handler.doExec(reqTopic, reqPayload, respPayload);

        assertEquals("Response code should match expected", KuraResponsePayload.RESPONSE_CODE_ERROR,
                respPayload.getResponseCode());
        assertNotNull("Response timestamp should be set", respPayload.getTimestamp());
        assertEquals("Body should match",
                "No DeploymentHook is currently associated to request type someRequestType, aborting operation",
                new String(respPayload.getBody(), Charset.forName("UTF-8")));
    }

    @Test
    public void testDoExecInstallNoRegisteredHookException() throws KuraException, NoSuchFieldException {

        CloudDeploymentHandlerV2 handler = new CloudDeploymentHandlerV2();
        TestUtil.setFieldValue(handler, "componentOptions", new CloudDeploymentHandlerV2Options(new HashMap<>()));

        final DeploymentHookManager manager = getDeploymentHookManager();
        handler.setDeploymentHookManager(manager);
        handler.setDataTransportService(mock(DataTransportService.class));

        CloudletTopic reqTopic = CloudletTopic.parseAppTopic("EXEC/" + CloudDeploymentHandlerV2.RESOURCE_INSTALL);
        KuraRequestPayload reqPayload = new KuraRequestPayload();

        reqPayload.addMetric(DeploymentPackageOptions.METRIC_DP_NAME, "test");
        reqPayload.addMetric(DeploymentPackageOptions.METRIC_DP_VERSION, "1.0");
        reqPayload.addMetric(DeploymentPackageOptions.METRIC_JOB_ID, 1234L);
        reqPayload.addMetric(DeploymentPackageInstallOptions.METRIC_DP_INSTALL_SYSTEM_UPDATE, false);
        reqPayload.addMetric(DeploymentPackageDownloadOptions.METRIC_REQUEST_TYPE, "someRequestType");

        KuraResponsePayload respPayload = new KuraResponsePayload(KuraResponsePayload.RESPONSE_CODE_OK);

        handler.doExec(reqTopic, reqPayload, respPayload);

        assertEquals("Response code should match expected", KuraResponsePayload.RESPONSE_CODE_ERROR,
                respPayload.getResponseCode());
        assertNotNull("Response timestamp should be set", respPayload.getTimestamp());
        assertEquals("Body should match",
                "No DeploymentHook is currently associated to request type someRequestType, aborting operation",
                new String(respPayload.getBody(), Charset.forName("UTF-8")));
    }

    @Test
    public void testDoExecDownloadShouldAbortOnPreDownload() throws KuraException, NoSuchFieldException {

        CloudDeploymentHandlerV2 handler = new CloudDeploymentHandlerV2();
        TestUtil.setFieldValue(handler, "componentOptions", new CloudDeploymentHandlerV2Options(new HashMap<>()));

        final DeploymentHookManager manager = getDeploymentHookManager();
        manager.bindHook(mockHookReference("testHook", new DeploymentHook() {

            @Override
            public void preDownload(RequestContext context, Map<String, Object> properties) throws KuraException {
                throw new IllegalStateException("Aborted by hook");
            }

            @Override
            public void postInstall(RequestContext context, Map<String, Object> properties) throws KuraException {
            }

            @Override
            public void postDownload(RequestContext context, Map<String, Object> properties) throws KuraException {
            }
        }));
        final Properties associations = new Properties();
        associations.setProperty("testRequest", "testHook");
        manager.updateAssociations(associations);

        handler.setDeploymentHookManager(manager);
        handler.setDataTransportService(mock(DataTransportService.class));

        CloudletTopic reqTopic = CloudletTopic.parseAppTopic("EXEC/" + CloudDeploymentHandlerV2.RESOURCE_DOWNLOAD);
        KuraRequestPayload reqPayload = new KuraRequestPayload();

        reqPayload.addMetric(DeploymentPackageDownloadOptions.METRIC_DP_DOWNLOAD_URI, "");
        reqPayload.addMetric(DeploymentPackageDownloadOptions.METRIC_DP_DOWNLOAD_PROTOCOL, "");
        reqPayload.addMetric(DeploymentPackageOptions.METRIC_DP_NAME, "");
        reqPayload.addMetric(DeploymentPackageOptions.METRIC_DP_VERSION, "");
        reqPayload.addMetric(DeploymentPackageOptions.METRIC_JOB_ID, 1234L);
        reqPayload.addMetric(DeploymentPackageInstallOptions.METRIC_DP_INSTALL_SYSTEM_UPDATE, true);
        reqPayload.addMetric(DeploymentPackageDownloadOptions.METRIC_REQUEST_TYPE, "testRequest");

        KuraResponsePayload respPayload = new KuraResponsePayload(KuraResponsePayload.RESPONSE_CODE_OK);

        handler.doExec(reqTopic, reqPayload, respPayload);

        assertEquals("Response code should match expected", KuraResponsePayload.RESPONSE_CODE_ERROR,
                respPayload.getResponseCode());
        assertNotNull("Response timestamp should be set", respPayload.getTimestamp());
        assertEquals("Exception should match", "Aborted by hook",
                new String(respPayload.getBody(), Charset.forName("UTF-8")));
    }

    @Test
    public void testDoExecInstallShouldAbortOnPostDownload() throws KuraException, NoSuchFieldException {

        CloudDeploymentHandlerV2 handler = new CloudDeploymentHandlerV2();
        TestUtil.setFieldValue(handler, "componentOptions", new CloudDeploymentHandlerV2Options(new HashMap<>()));

        final DeploymentHookManager manager = getDeploymentHookManager();
        manager.bindHook(mockHookReference("testHook", new DeploymentHook() {

            @Override
            public void preDownload(RequestContext context, Map<String, Object> properties) throws KuraException {
            }

            @Override
            public void postInstall(RequestContext context, Map<String, Object> properties) throws KuraException {
            }

            @Override
            public void postDownload(RequestContext context, Map<String, Object> properties) throws KuraException {
                throw new IllegalStateException("Aborted by hook");
            }
        }));
        final Properties associations = new Properties();
        associations.setProperty("testRequest", "testHook");
        manager.updateAssociations(associations);
        assertTrue(manager.getHook("testRequest") != null);

        handler.setDeploymentHookManager(manager);
        handler.setDataTransportService(mock(DataTransportService.class));
        final DownloadImpl mockDownloadImpl = mock(DownloadImpl.class);
        when(mockDownloadImpl.isAlreadyDownloaded()).thenReturn(true);
        TestUtil.setFieldValue(handler, "downloadImplementation", mockDownloadImpl);

        CloudletTopic reqTopic = CloudletTopic.parseAppTopic("EXEC/" + CloudDeploymentHandlerV2.RESOURCE_INSTALL);
        KuraRequestPayload reqPayload = new KuraRequestPayload();

        reqPayload.addMetric(DeploymentPackageOptions.METRIC_DP_NAME, "test");
        reqPayload.addMetric(DeploymentPackageOptions.METRIC_DP_VERSION, "1.0");
        reqPayload.addMetric(DeploymentPackageOptions.METRIC_JOB_ID, 1237L);
        reqPayload.addMetric(DeploymentPackageInstallOptions.METRIC_DP_INSTALL_SYSTEM_UPDATE, true);
        reqPayload.addMetric(DeploymentPackageInstallOptions.METRIC_REQUEST_TYPE, "testRequest");

        KuraResponsePayload respPayload = new KuraResponsePayload(KuraResponsePayload.RESPONSE_CODE_OK);

        handler.doExec(reqTopic, reqPayload, respPayload);

        assertEquals("Response code should match expected", KuraResponsePayload.RESPONSE_CODE_ERROR,
                respPayload.getResponseCode());
        assertNotNull("Response timestamp should be set", respPayload.getTimestamp());
        assertEquals("Exception should match", "Aborted by hook", respPayload.getExceptionMessage());
    }

    public void testPublishMessage() {
        fail("Not yet implemented");
    }

    public void testInstallDownloadedFile() {
        fail("Not yet implemented");
    }

}
