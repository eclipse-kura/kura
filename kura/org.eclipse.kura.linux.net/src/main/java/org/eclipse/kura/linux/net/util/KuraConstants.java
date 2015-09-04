/**
 * Copyright (c) 2011, 2014 Eurotech and/or its affiliates
 *
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *   Eurotech
 */
package org.eclipse.kura.linux.net.util;

	
public enum KuraConstants {
	// image name, version
	Mini_Gateway ("yocto", "1.2.1", "mini-gateway"),
	ReliaGATE_15_10("yocto", "1.2.1", "reliagate-15-10"),
	ReliaGATE_10_05("yocto", "1.2.1", "reliagate-10-05"),
	Intel_Edison("yocto", "1.6.1", "edison"),
	Raspberry_Pi ("raspbian", "1.0.0", "raspberry-pi"),
	BeagleBone ("debian", "1.0.0", "beaglebone"),
	ReliaGATE_50_21_Ubuntu("ubuntu", "14.04", "reliagate-50-21");
	
	private String m_imageName;
	private String m_imageVersion;
	private String m_targetName;
	
	private KuraConstants (String imageName, String imageVersion, String targetName) {
		m_imageName = imageName;
		m_imageVersion = imageVersion;
		m_targetName = targetName;
	}
	
	public String getImageName () {
		return m_imageName;
	}
	
	public String getImageVersion () {
		return m_imageVersion;
	}
	
	public String getTargetName () {
		return m_targetName;
	}

}
