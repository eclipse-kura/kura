package org.eclipse.kura.core.deployment.uninstall;

import java.io.File;
import java.net.URL;
import java.util.Date;

import org.eclipse.kura.KuraException;
import org.eclipse.kura.core.deployment.CloudDeploymentHandlerV2;
import org.eclipse.kura.core.deployment.CloudDeploymentHandlerV2.UNINSTALL_STATUS;
import org.osgi.service.deploymentadmin.DeploymentAdmin;
import org.osgi.service.deploymentadmin.DeploymentPackage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class UninstallImpl {
	private static final Logger s_logger = LoggerFactory.getLogger(UninstallImpl.class);
	
	public static final String RESOURCE_UNINSTALL = "uninstall";
	
	private CloudDeploymentHandlerV2 callback;
	private DeploymentAdmin m_deploymentAdmin;

	public UninstallImpl(CloudDeploymentHandlerV2 callback, DeploymentAdmin deploymentAdmin){
		this.callback= callback;
		this.m_deploymentAdmin= deploymentAdmin;
	}
	
	private void uninstallCompleteAsync(DeploymentPackageUninstallOptions options, String dpName) throws KuraException{
		KuraUninstallPayload notify = null;

		notify = new KuraUninstallPayload(options.getClientId());
		notify.setTimestamp(new Date());
		notify.setUninstallStatus(UNINSTALL_STATUS.COMPLETED.getStatusString());
		notify.setJobId(options.getJobId());
		notify.setDpName(dpName); //Probably split dpName and dpVersion?
		notify.setUninstallProgress(100);

		callback.publishMessage(options, notify, RESOURCE_UNINSTALL);
	}

	public void uninstallFailedAsync(DeploymentPackageUninstallOptions options, String dpName, Exception e) throws KuraException{
		KuraUninstallPayload notify = null;

		notify = new KuraUninstallPayload(options.getClientId());
		notify.setTimestamp(new Date());
		notify.setUninstallStatus(UNINSTALL_STATUS.FAILED.getStatusString());
		notify.setJobId(options.getJobId());
		notify.setDpName(dpName); //Probably split dpName and dpVersion?
		notify.setUninstallProgress(0);
		if (e != null){
			notify.setErrorMessage(e.getMessage());
		}
		callback.publishMessage(options, notify, RESOURCE_UNINSTALL);
	}
	
	public void uninstaller(DeploymentPackageUninstallOptions options, String packageName) throws KuraException {
		try{
			String name = packageName;
			if (name != null) {
				s_logger.info("About to uninstall package ", name);
				DeploymentPackage dp = null;

				dp = m_deploymentAdmin.getDeploymentPackage(name);
				if (dp != null) {
					dp.uninstall();

					String sUrl = callback.m_installImplementation.getDeployedPackages().getProperty(name);
					File dpFile = new File(new URL(sUrl).getPath());
					if (!dpFile.delete()) {
						s_logger.warn("Cannot delete file at URL: {}", sUrl);
					}
					callback.m_installImplementation.removePackageFromConfFile(name);
					uninstallCompleteAsync(options, name);
				}
			}
		} catch (Exception e) {
			throw KuraException.internalError(e.getMessage());
		}
	}

}
