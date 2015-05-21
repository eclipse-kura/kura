package org.eclipse.kura.core.linux.util;

public enum KuraConstants {

	// image name, version
	Mini_Gateway ("yocto", "1.2.1", "mini-gateway"),
	Raspberry_Pi ("raspbian", "1.0.0", "raspberry-pi"),
	BeagleBone ("debian", "1.0.0", "beaglebone"),
	Intel_Edison("yocto", "1.6.1", "edison");
		
	private String m_imageName;
	private String m_imageVersion;
	private String m_targetName;

	private KuraConstants(String imageName, String imageVersion, String targetName) {
		m_imageName = imageName;
		m_imageVersion = imageVersion;
		m_targetName = targetName;
	}

	public String getImageName() {
		return m_imageName;
	}

	public String getImageVersion() {
		return m_imageVersion;
	}

	public String getTargetName() {
		return m_targetName;
	}
}
