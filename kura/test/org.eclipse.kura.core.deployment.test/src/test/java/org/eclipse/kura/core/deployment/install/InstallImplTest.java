/*******************************************************************************
 * Copyright (c) 2018 Eurotech and/or its affiliates and others
 *
 *   All rights reserved. This program and the accompanying materials
 *   are made available under the terms of the Eclipse Public License v1.0
 *   which accompanies this distribution, and is available at
 *   http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.eclipse.kura.core.deployment.install;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.anyObject;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Properties;

import org.eclipse.kura.KuraException;
import org.eclipse.kura.cloudconnection.publisher.CloudNotificationPublisher;
import org.eclipse.kura.core.deployment.CloudDeploymentHandlerV2;
import org.eclipse.kura.core.deployment.DeploymentPackageOptions;
import org.eclipse.kura.core.deployment.InstallStatus;
import org.eclipse.kura.core.deployment.download.DeploymentPackageDownloadOptions;
import org.eclipse.kura.core.testutil.TestUtil;
import org.eclipse.kura.message.KuraResponsePayload;
import org.junit.Test;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.osgi.service.deploymentadmin.DeploymentAdmin;
import org.osgi.service.deploymentadmin.DeploymentException;
import org.osgi.service.deploymentadmin.DeploymentPackage;

public class InstallImplTest {

    @Test
    public void testGetDeployedPackagesError() {
        CloudDeploymentHandlerV2 callbackMock = mock(CloudDeploymentHandlerV2.class);
        String kuraDataDir = "/tmp";

        InstallImpl ii = new InstallImpl(callbackMock, kuraDataDir);
        ii.setDpaConfPath("/tmp/dpagdpe.properties");

        Properties packages = ii.getDeployedPackages();

        assertTrue("Should be empty", packages.isEmpty());
    }

    @Test
    public void testGetDeployedPackages() throws IOException {
        String dpaConfPath = "/tmp/dpagdp.properties";
        File f = new File(dpaConfPath);
        FileWriter fw = new FileWriter(f);
        fw.write("a=b");
        fw.close();

        CloudDeploymentHandlerV2 callbackMock = mock(CloudDeploymentHandlerV2.class);
        String kuraDataDir = "/tmp";

        InstallImpl ii = new InstallImpl(callbackMock, kuraDataDir);
        ii.setDpaConfPath(dpaConfPath);

        Properties packages = ii.getDeployedPackages();

        f.delete();

        assertFalse("Should not be empty", packages.isEmpty());
    }

    @Test
    public void testInstallDpSuccessMessage() throws KuraException {
        CloudDeploymentHandlerV2 callbackMock = mock(CloudDeploymentHandlerV2.class);
        String kuraDataDir = "/tmp";
        InstallImpl ii = new InstallImpl(callbackMock, kuraDataDir);

        final String clientId = "clientid";
        final long jobid = 1234;

        DeploymentPackageInstallOptions options = new DeploymentPackageInstallOptions("dpname", "7.3.57");
        options.setClientId(clientId);
        options.setJobId(jobid);

        String dpFineName = "/tmp/dp.dp";
        File dpFile = new File(dpFineName);

        doAnswer(new Answer<Object>() {

            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable {
                Object[] arguments = invocation.getArguments();
                KuraInstallPayload kp = (KuraInstallPayload) arguments[1];

                assertEquals("", clientId, kp.getClientId());
                assertEquals("", 100, kp.getInstallProgress());
                assertEquals("", InstallStatus.COMPLETED.getStatusString(), kp.getInstallStatus());
                assertEquals("", jobid, kp.getJobId().longValue());

                return null;
            }
        }).when(callbackMock).publishMessage(eq(options), Mockito.anyObject(), eq(InstallImpl.RESOURCE_INSTALL));

        ii.installDp(options, dpFile);

        verify(callbackMock, times(1)).publishMessage(eq(options), Mockito.anyObject(),
                eq(InstallImpl.RESOURCE_INSTALL));
    }

    @Test
    public void testInstallDPFailMessage() throws Throwable {
        CloudDeploymentHandlerV2 callbackMock = mock(CloudDeploymentHandlerV2.class);
        String kuraDataDir = "/tmp";
        InstallImpl ii = new InstallImpl(callbackMock, kuraDataDir);

        DeploymentAdmin deploymentAdminMock = mock(DeploymentAdmin.class);
        ii.setDeploymentAdmin(deploymentAdminMock);

        ii.setPackagesPath(kuraDataDir);

        File persDir = new File(kuraDataDir, "persistance");
        persDir.mkdirs();
        File veriDir = new File(persDir, "verification");
        veriDir.mkdirs();

        final String clientId = "clientid";
        final long jobid = 1234;

        DeploymentPackageInstallOptions options = new DeploymentPackageInstallOptions("dpname", "7.3.57");
        options.setClientId(clientId);
        options.setJobId(jobid);

        String dpFineName = "/tmp/dp.dp";
        File dpFile = new File(dpFineName);
        dpFile.createNewFile();

        when(deploymentAdminMock.installDeploymentPackage(anyObject()))
                .thenThrow(new DeploymentException(DeploymentException.CODE_OTHER_ERROR, "test"));

        doAnswer(new Answer<Object>() {

            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable {
                Object[] arguments = invocation.getArguments();
                KuraInstallPayload kp = (KuraInstallPayload) arguments[1];

                assertEquals("", clientId, kp.getClientId());
                assertEquals("", 0, kp.getInstallProgress());
                assertEquals("", InstallStatus.FAILED.getStatusString(), kp.getInstallStatus());
                assertEquals("", jobid, kp.getJobId().longValue());
                assertEquals("", "test", kp.getErrorMessage());

                return null;
            }
        }).when(callbackMock).publishMessage(eq(options), Mockito.anyObject(), eq(InstallImpl.RESOURCE_INSTALL));

        ii.installDp(options, dpFile);

        verify(callbackMock, times(1)).publishMessage(eq(options), Mockito.anyObject(),
                eq(InstallImpl.RESOURCE_INSTALL));

        veriDir.delete();
        persDir.delete();
        dpFile.delete();
    }

    @Test
    public void testInstallDeploymentPackageInternal() throws Throwable {
        CloudDeploymentHandlerV2 callbackMock = mock(CloudDeploymentHandlerV2.class);
        String kuraDataDir = "/tmp";
        InstallImpl ii = new InstallImpl(callbackMock, kuraDataDir);

        DeploymentAdmin deploymentAdminMock = mock(DeploymentAdmin.class);
        ii.setDeploymentAdmin(deploymentAdminMock);

        ii.setPackagesPath(kuraDataDir);

        File persDir = new File(kuraDataDir, "persistance");
        persDir.mkdirs();
        File veriDir = new File(persDir, "verification");
        veriDir.mkdirs();

        String dpFineName = "/tmp/dp.dp";
        File dpFile = new File(dpFineName);
        dpFile.createNewFile();

        DeploymentPackage dpMock = mock(DeploymentPackage.class);
        when(deploymentAdminMock.installDeploymentPackage(anyObject())).thenReturn(dpMock);

        Object dp = TestUtil.invokePrivate(ii, "installDeploymentPackageInternal", dpFile);

        veriDir.delete();
        persDir.delete();
        dpFile.delete();

        verify(deploymentAdminMock, times(1)).installDeploymentPackage(anyObject());

        assertEquals("Should return our dp", dpMock, dp);
    }

    @Test
    public void testInstallDeploymentPackageInternalAddToConfig() throws Throwable {
        CloudDeploymentHandlerV2 callbackMock = mock(CloudDeploymentHandlerV2.class);
        String kuraDataDir = "/tmp";
        InstallImpl ii = new InstallImpl(callbackMock, kuraDataDir) {

            @Override
            public Properties getDeployedPackages() {
                return new Properties();
            }
        };

        DeploymentAdmin deploymentAdminMock = mock(DeploymentAdmin.class);
        ii.setDeploymentAdmin(deploymentAdminMock);

        ii.setPackagesPath(kuraDataDir);

        File pkgDir = new File(kuraDataDir, "packages");
        pkgDir.mkdirs();

        String dpFineName = "/tmp/dp.dp";
        File dpFile = new File(dpFineName);
        dpFile.createNewFile();

        DeploymentPackage dpMock = mock(DeploymentPackage.class);
        when(deploymentAdminMock.installDeploymentPackage(anyObject())).thenReturn(dpMock);

        when(dpMock.getName()).thenReturn("dpname");

        ii.setDpaConfPath(null); // make sure this is null, so that we don't test too much
        ii.setPackagesPath(pkgDir.getCanonicalPath());

        Object dp = TestUtil.invokePrivate(ii, "installDeploymentPackageInternal", dpFile);

        verify(deploymentAdminMock, times(1)).installDeploymentPackage(anyObject());

        assertEquals("Should return our dp", dpMock, dp);
        assertFalse("File should have been deleted", dpFile.exists());

        File persDir = new File(kuraDataDir, "persistance");
        File veriDir = new File(persDir, "verification");
        assertTrue("Directory should have been created", persDir.exists());
        assertTrue("Directory should have been created", veriDir.exists());

        veriDir.delete();
        persDir.delete();
        pkgDir.delete();
    }

    @Test
    public void testInstallInProgressSyncMessage() {
        CloudDeploymentHandlerV2 callbackMock = mock(CloudDeploymentHandlerV2.class);
        String kuraDataDir = "/tmp";

        InstallImpl ii = new InstallImpl(callbackMock, kuraDataDir);

        String dpName = "dpname";
        String dpVersion = "7.3.57";
        DeploymentPackageInstallOptions options = new DeploymentPackageInstallOptions(dpName, dpVersion);
        ii.setOptions(options);

        KuraResponsePayload respPayload = new KuraResponsePayload(KuraResponsePayload.RESPONSE_CODE_OK);

        ii.installInProgressSyncMessage(respPayload);

        assertNotNull(respPayload.getTimestamp());
        assertEquals(dpName, respPayload.getMetric(KuraInstallPayload.METRIC_DP_NAME));
        assertEquals(dpVersion, respPayload.getMetric(KuraInstallPayload.METRIC_DP_VERSION));
        assertEquals(InstallStatus.IN_PROGRESS.getStatusString(),
                respPayload.getMetric(KuraInstallPayload.METRIC_INSTALL_STATUS));
    }

    @Test
    public void testInstallIdleSyncMessage() {
        CloudDeploymentHandlerV2 callbackMock = mock(CloudDeploymentHandlerV2.class);
        String kuraDataDir = "/tmp";

        InstallImpl ii = new InstallImpl(callbackMock, kuraDataDir);

        String dpName = "dpname";
        String dpVersion = "7.3.57";
        DeploymentPackageInstallOptions options = new DeploymentPackageInstallOptions(dpName, dpVersion);
        ii.setOptions(options);

        KuraResponsePayload respPayload = new KuraResponsePayload(KuraResponsePayload.RESPONSE_CODE_OK);

        ii.installIdleSyncMessage(respPayload);

        assertNotNull(respPayload.getTimestamp());
        assertEquals(InstallStatus.IDLE.getStatusString(),
                respPayload.getMetric(KuraInstallPayload.METRIC_INSTALL_STATUS));
    }

    @Test
    public void testSendInstallConfirmations() throws IOException, KuraException {
        CloudDeploymentHandlerV2 callbackMock = mock(CloudDeploymentHandlerV2.class);
        String kuraDataDir = "/tmp";

        InstallImpl ii = new InstallImpl(callbackMock, kuraDataDir);

        File persDir = new File(kuraDataDir, "persistance");
        persDir.mkdirs();
        File veriDir = new File(persDir, "verification");
        veriDir.mkdirs();

        String fname = "test_script.sh";
        String cmds = "#!/bin/sh\nsleep 1\n";
        if (System.getProperty("os.name").contains("Windows")) {
            fname = "test_script.bat";
            cmds = "sleep 1";
        }
        File f = new File(veriDir, fname);
        FileWriter fw = new FileWriter(f);
        fw.write(cmds);
        fw.close();

        File f1 = new File(veriDir, "test_file");
        f1.createNewFile();
        f1.deleteOnExit();

        File fperf = new File(persDir, "test_script.sh_persistance");
        StringBuilder sb = new StringBuilder();
        sb.append(DeploymentPackageDownloadOptions.METRIC_DP_DOWNLOAD_URI).append("=DOWNLOAD_URI\n");
        sb.append(DeploymentPackageOptions.METRIC_DP_NAME).append("=NAME\n");
        sb.append(DeploymentPackageOptions.METRIC_DP_VERSION).append("=VERSION\n");
        sb.append(DeploymentPackageOptions.METRIC_DP_CLIENT_ID).append("=CLIENT_ID\n");
        sb.append(DeploymentPackageOptions.METRIC_JOB_ID).append("=1234\n");
        sb.append(InstallImpl.PERSISTANCE_FILE_NAME).append("=test_script.sh\n");
        sb.append(CloudDeploymentHandlerV2.METRIC_REQUESTER_CLIENT_ID).append("=REQUESTER_CLIENT_ID\n");
        sb.append(DeploymentPackageOptions.NOTIFICATION_PUBLISHER_PID_KEY).append("=org.eclipse.kura.cloud.publisher.CloudNotificationPublisher\n");
        fw = new FileWriter(fperf);
        fw.write(sb.toString());
        fw.close();

        doAnswer(new Answer<Object>() {

            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable {
                Object[] arguments = invocation.getArguments();
                KuraInstallPayload kp = (KuraInstallPayload) arguments[1];

                assertEquals("", "CLIENT_ID", kp.getClientId());
                assertEquals("", 100, kp.getInstallProgress());
                assertEquals("", InstallStatus.COMPLETED.getStatusString(), kp.getInstallStatus());
                assertEquals("", 1234, kp.getJobId().longValue());
                assertEquals("", "test_script.sh", kp.getMetric(KuraInstallPayload.METRIC_DP_NAME));

                return null;
            }
        }).when(callbackMock).publishMessage(Mockito.anyObject(), Mockito.anyObject(),
                eq(InstallImpl.RESOURCE_INSTALL));
        
        CloudNotificationPublisher notificationPublisher = mock(CloudNotificationPublisher.class);
        when(notificationPublisher.publish(anyObject())).thenReturn("12345");

        ii.sendInstallConfirmations("org.eclipse.kura.cloud.publisher.CloudNotificationPublisher", notificationPublisher);

        assertFalse("File should have been deleted.", f.exists());
        assertTrue("File should not have been deleted.", f1.exists());
        assertFalse("File should have been deleted.", fperf.exists());

        verify(callbackMock, times(1)).publishMessage(Mockito.anyObject(), Mockito.anyObject(), Mockito.anyObject());

        f1.delete();
        veriDir.delete();
        persDir.delete();
    }

    @Test
    public void testSendInstallConfirmationsError() throws IOException, KuraException {
        CloudDeploymentHandlerV2 callbackMock = mock(CloudDeploymentHandlerV2.class);
        String kuraDataDir = "/tmp";

        InstallImpl ii = new InstallImpl(callbackMock, kuraDataDir);

        File persDir = new File(kuraDataDir, "persistance");
        persDir.mkdirs();
        File veriDir = new File(persDir, "verification");
        veriDir.mkdirs();

        String fname = "test_scriptError.sh";
        String cmds = "#!/bin/sh\nsleep 1\nexit 1\n";
        if (System.getProperty("os.name").contains("Windows")) {
            fname = "test_scriptError.bat";
            cmds = "sleep 1\r\nexit 1";
        }
        File f = new File(veriDir, fname);
        FileWriter fw = new FileWriter(f);
        fw.write(cmds);
        fw.close();

        File fperf = new File(persDir, "test_scriptError.sh_persistance");
        StringBuilder sb = new StringBuilder();
        sb.append(DeploymentPackageDownloadOptions.METRIC_DP_DOWNLOAD_URI).append("=DOWNLOAD_URI\n");
        sb.append(DeploymentPackageOptions.METRIC_DP_NAME).append("=NAME\n");
        sb.append(DeploymentPackageOptions.METRIC_DP_VERSION).append("=VERSION\n");
        sb.append(DeploymentPackageOptions.METRIC_DP_CLIENT_ID).append("=CLIENT_ID\n");
        sb.append(DeploymentPackageOptions.METRIC_JOB_ID).append("=1234\n");
        sb.append(InstallImpl.PERSISTANCE_FILE_NAME).append("=test_scriptError.sh\n");
        sb.append(CloudDeploymentHandlerV2.METRIC_REQUESTER_CLIENT_ID).append("=REQUESTER_CLIENT_ID\n");
        sb.append(DeploymentPackageOptions.NOTIFICATION_PUBLISHER_PID_KEY).append("=org.eclipse.kura.cloud.publisher.CloudNotificationPublisher\n");
        fw = new FileWriter(fperf);
        fw.write(sb.toString());
        fw.close();

        doAnswer(new Answer<Object>() {

            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable {
                Object[] arguments = invocation.getArguments();
                KuraInstallPayload kp = (KuraInstallPayload) arguments[1];

                assertEquals("", "CLIENT_ID", kp.getClientId());
                assertEquals("", 0, kp.getInstallProgress());
                assertEquals("", InstallStatus.FAILED.getStatusString(), kp.getInstallStatus());
                assertEquals("", 1234, kp.getJobId().longValue());
                assertEquals("", "test_scriptError.sh", kp.getMetric(KuraInstallPayload.METRIC_DP_NAME));

                return null;
            }
        }).when(callbackMock).publishMessage(Mockito.anyObject(), Mockito.anyObject(),
                eq(InstallImpl.RESOURCE_INSTALL));

        CloudNotificationPublisher notificationPublisher = mock(CloudNotificationPublisher.class);
        when(notificationPublisher.publish(anyObject())).thenReturn("12345");
        
        ii.sendInstallConfirmations("org.eclipse.kura.cloud.publisher.CloudNotificationPublisher", notificationPublisher);

        assertFalse("File should have been deleted.", f.exists());

        veriDir.delete();
        persDir.delete();
    }

    @Test
    public void testRemovePackageFromConfFileNullDpaCPErrorMessage() {
        CloudDeploymentHandlerV2 callbackMock = mock(CloudDeploymentHandlerV2.class);
        String kuraDataDir = "/tmp";

        InstallImpl ii = new InstallImpl(callbackMock, kuraDataDir) {

            @Override
            public Properties getDeployedPackages() {
                return new Properties();
            }
        };

        ii.removePackageFromConfFile("test");

        // nothing else to test
    }

    @Test
    public void testRemovePackageFromConfFile() throws FileNotFoundException, IOException {
        CloudDeploymentHandlerV2 callbackMock = mock(CloudDeploymentHandlerV2.class);
        String kuraDataDir = "/tmp";

        final Properties properties = new Properties();
        properties.put("test", "testval");
        properties.put("test2", "testval2");

        InstallImpl ii = new InstallImpl(callbackMock, kuraDataDir) {

            @Override
            public Properties getDeployedPackages() {
                return properties;
            }
        };

        String dpaConfPath = "/tmp/dpaconfig_test";
        ii.setDpaConfPath(dpaConfPath);

        ii.removePackageFromConfFile("test");

        File f = new File(dpaConfPath);
        assertTrue("File is expected to be stored", f.exists());

        Properties p = new Properties();
        p.load(new FileReader(f));
        assertEquals(1, p.size());
        assertEquals("testval2", p.getProperty("test2"));
    }

}
