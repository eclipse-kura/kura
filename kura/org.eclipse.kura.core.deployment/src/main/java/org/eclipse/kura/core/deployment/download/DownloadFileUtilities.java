package org.eclipse.kura.core.deployment.download;

import java.io.File;
import java.io.IOException;

import org.eclipse.kura.core.deployment.install.DeploymentPackageInstallOptions;
import org.eclipse.kura.core.deployment.util.FileUtilities;

public class DownloadFileUtilities extends FileUtilities{

	//File Management
	public static File getDpDownloadFile(DeploymentPackageInstallOptions options) throws IOException {
		// File dpFile = File.createTempFile("dpa", null);
		String packageFilename = null;
		if(!options.getSystemUpdate()){
			String dpName= FileUtilities.getFileName(options.getDpName(), options.getDpVersion(), ".dp");
			packageFilename = new StringBuilder().append(File.separator)
					.append("tmp")
					.append(File.separator)
					.append(dpName)
					.toString();
		} else {
			String shName= FileUtilities.getFileName(options.getDpName(), options.getDpVersion(), ".sh");
			packageFilename = new StringBuilder().append(File.separator)
					.append("tmp")
					.append(File.separator)
					.append(shName)
					.toString();
		}

		File dpFile = new File(packageFilename);
		return dpFile;
	}

	public static boolean deleteDownloadedFile(DeploymentPackageInstallOptions options) throws IOException {
		File file = getDpDownloadFile(options);
		
		if (file != null &&
			file.exists() &&
			file.isFile()){
			return file.delete();
		}
		
		return false;
	}
}
