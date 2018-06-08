/*******************************************************************************
 * Copyright (c) 2011, 2018 Eurotech and others
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Eurotech
 *     Red Hat Inc
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
    Reliagate_10_12("yocto", "1.2.1", "reliagate-10-12"),
    Reliagate_20_25("yocto", "1.2.1", "reliagate-20-25"),
    BoltGATE_20_25("yocto", "1.2.1", "boltgate-20-25"),
    Reliagate_20_26("rhel", "7.3", "reliagate-20-26"),
    Intel_Up2_Ubuntu("ubuntu", "16.0.4", "intel-up2-ubuntu-16"),
    Intel_Up2("centos", "7", "intel-up2-centos-7"),
    Fedora_Pi("fedora", "2x", "raspberry-pi");

    private String imageName;
    private String imageVersion;
    private String targetName;

    private KuraConstants(String imageName, String imageVersion, String targetName) {
        this.imageName = imageName;
        this.imageVersion = imageVersion;
        this.targetName = targetName;
    }

    public String getImageName() {
        return this.imageName;
    }

    public String getImageVersion() {
        return this.imageVersion;
    }

    public String getTargetName() {
        return this.targetName;
    }

}
