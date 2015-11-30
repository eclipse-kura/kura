package org.eclipse.kura.core.deployment.util;

public class FileUtilities {

	public static String getFileName(String dpName, String dpVersion, String extension) {
		String packageFilename = null;
		packageFilename = new StringBuilder().append(dpName).append("-")
				.append(dpVersion)
				.append(extension).toString();
		return packageFilename;
	}
}
