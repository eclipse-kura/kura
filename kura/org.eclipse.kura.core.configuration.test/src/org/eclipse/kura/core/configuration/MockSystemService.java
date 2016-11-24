/*******************************************************************************
 * Copyright (c) 2016 Red Hat Inc and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Red Hat Inc
 *******************************************************************************/
package org.eclipse.kura.core.configuration;

import java.io.IOException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.Collections;
import java.util.List;
import java.util.Properties;

import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;

import org.eclipse.kura.system.SystemService;
import org.osgi.framework.Bundle;

final class MockSystemService implements SystemService {

    @Override
    public long getTotalMemory() {
        // TODO Auto-generated method stub
        return 0;
    }

    @Override
    public String getSerialNumber() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Properties getProperties() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public String getPrimaryNetworkInterfaceName() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public String getPrimaryMacAddress() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public String getPlatform() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public String getPartNumber() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public String getOsgiFwVersion() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public String getOsgiFwName() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public String getOsVersion() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public String getOsName() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public String getOsDistroVersion() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public String getOsDistro() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public String getOsArch() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public int getNumberOfProcessors() {
        // TODO Auto-generated method stub
        return 0;
    }

    @Override
    public String getModelName() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public String getModelId() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public int getKuraWifiTopChannel() {
        // TODO Auto-generated method stub
        return 0;
    }

    @Override
    public String getKuraWebEnabled() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public String getKuraVersion() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public String getKuraTemporaryConfigDirectory() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public String getKuraStyleDirectory() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public String getKuraSnapshotsDirectory() {
        return System.getProperty("java.io.tmpdir");
    }

    @Override
    public int getKuraSnapshotsCount() {
        return 0;
    }

    @Override
    public String getKuraHome() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public String getKuraDataDirectory() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public String getJavaVmVersion() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public String getJavaVmName() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public String getJavaVmInfo() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public String getJavaVersion() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public String getJavaVendor() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public char[] getJavaTrustStorePassword() throws InvalidKeyException, NoSuchAlgorithmException,
            NoSuchPaddingException, IllegalBlockSizeException, BadPaddingException, IOException {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public char[] getJavaKeyStorePassword() throws InvalidKeyException, NoSuchAlgorithmException,
            NoSuchPaddingException, IllegalBlockSizeException, BadPaddingException, IOException {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public String getJavaHome() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public long getFreeMemory() {
        // TODO Auto-generated method stub
        return 0;
    }

    @Override
    public String getFirmwareVersion() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public String getFileSeparator() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public int getFileCommandZipMaxUploadSize() {
        // TODO Auto-generated method stub
        return 0;
    }

    @Override
    public int getFileCommandZipMaxUploadNumber() {
        // TODO Auto-generated method stub
        return 0;
    }

    @Override
    public String getDeviceName() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public List<String> getDeviceManagementServiceIgnore() {
        return Collections.emptyList();
    }

    @Override
    public Bundle[] getBundles() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public String getBiosVersion() {
        // TODO Auto-generated method stub
        return null;
    }
}