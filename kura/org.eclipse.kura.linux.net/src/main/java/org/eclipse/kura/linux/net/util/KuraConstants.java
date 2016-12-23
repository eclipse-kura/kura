/*******************************************************************************
 * Copyright (c) 2011, 2016 Eurotech and others
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Eurotech
 *     Red Hat Inc - Add Fedora support
 *******************************************************************************/
package org.eclipse.kura.linux.net.util;

public enum KuraConstants {
    // image name, version
    Mini_Gateway("yocto", "1.2.1", "mini-gateway"),
    ReliaGATE_15_10("yocto", "1.2.1", "reliagate-15-10"),
    Reliagate_10_20("yocto", "1.2.1", "reliagate-10-20"),
    ReliaGATE_10_05("yocto", "1.2.1", "reliagate-10-05"),
    Intel_Edison("yocto", "1.6.1", "edison"),
    Raspberry_Pi("raspbian", "1.0.0", "raspberry-pi"),
    BeagleBone("debian", "1.0.0", "beaglebone"),
    ReliaGATE_50_21_Ubuntu("ubuntu", "14.04", "reliagate-50-21"),
    Reliagate_10_11("yocto", "1.2.1", "reliagate-10-11"),
    Reliagate_20_25("yocto", "1.2.1", "reliagate-20-25"),
    Reliagate_20_26("rhel", "7.3", "reliagate-20-26"),
    Fedora_Pi("fedora", "2x", "raspberry-pi");

    private String m_imageName;
    private String m_imageVersion;
    private String m_targetName;

    private KuraConstants(String imageName, String imageVersion, String targetName) {
        this.m_imageName = imageName;
        this.m_imageVersion = imageVersion;
        this.m_targetName = targetName;
    }

    public String getImageName() {
        return this.m_imageName;
    }

    public String getImageVersion() {
        return this.m_imageVersion;
    }

    public String getTargetName() {
        return this.m_targetName;
    }

}
