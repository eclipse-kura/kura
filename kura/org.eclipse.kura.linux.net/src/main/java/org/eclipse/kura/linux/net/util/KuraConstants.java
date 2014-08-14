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
	Raspberry_Pi ("raspbian", "1.0.0", "raspberry-pi");
	
	private String m_imageName;
	private String m_imageVersion;
	private String m_buildName;
	
	private KuraConstants (String imageName, String imageVersion, String buildName) {
		m_imageName = imageName;
		m_imageVersion = imageVersion;
		m_buildName = buildName;
	}
	
	public String getImageName () {
		return m_imageName;
	}
	
	public String getImageVersion () {
		return m_imageVersion;
	}
	
	public String getBuildName () {
		return m_buildName;
	}

}
