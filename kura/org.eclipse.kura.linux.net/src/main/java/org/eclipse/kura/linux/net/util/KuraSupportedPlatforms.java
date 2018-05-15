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

public enum KuraSupportedPlatforms {
    // image name, version
    YOCTO_121("yocto", "1.2.1"),
    YOCTO_161("yocto", "1.6.1"),
    RASPBIAN_100("raspbian", "1.0.0"),
    DEBIAN_100("debian", "1.0.0"),
    RHEL_73("rhel", "7.3"),
    FEDORA_2X("fedora", "2x"),
    CENTOS_7("centos", "7"),
    UBUNTU_16("ubuntu", "16.0.4");

    private String imageName;
    private String imageVersion;

    private KuraSupportedPlatforms(String imageName, String imageVersion) {
        this.imageName = imageName;
        this.imageVersion = imageVersion;
    }

    public String getImageName() {
        return this.imageName;
    }

    public String getImageVersion() {
        return this.imageVersion;
    }
}
