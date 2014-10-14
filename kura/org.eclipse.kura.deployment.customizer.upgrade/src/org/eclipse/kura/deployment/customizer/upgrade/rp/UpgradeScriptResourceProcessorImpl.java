package org.eclipse.kura.deployment.customizer.upgrade.rp;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.apache.commons.io.FileUtils;
import org.osgi.framework.BundleContext;
import org.osgi.service.deploymentadmin.DeploymentPackage;
import org.osgi.service.deploymentadmin.spi.DeploymentSession;
import org.osgi.service.deploymentadmin.spi.ResourceProcessor;
import org.osgi.service.deploymentadmin.spi.ResourceProcessorException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class UpgradeScriptResourceProcessorImpl implements ResourceProcessor {
	
	private static Logger s_logger = LoggerFactory.getLogger(UpgradeScriptResourceProcessorImpl.class);

	private BundleContext		m_bundleContext;
	private DeploymentPackage 	m_sourceDP;
	private DeploymentPackage 	m_targetDP;
	private Map<String, File>	m_sourceResourceFiles = new HashMap<String, File>();
	private File				m_upgradeScript;
	
	protected void activate(BundleContext bundleContext) {
		m_bundleContext = bundleContext;
		s_logger.info("Activating -> " + m_bundleContext.getBundle().getSymbolicName());
	}
	
	protected void deactivate() {
		s_logger.info("Deactivating -> " + m_bundleContext.getBundle().getSymbolicName());
	}
	
	@Override
	public void begin(DeploymentSession session) {
		s_logger.debug("Upgrade script resource processor: begin");
		
		m_sourceDP = session.getSourceDeploymentPackage();
		m_targetDP = session.getTargetDeploymentPackage();
	}

	@Override
	public void process(String name, InputStream stream) throws ResourceProcessorException {
		s_logger.debug("Upgrade script resource processor: process");
		
		// Create temporary files for source resources
		File tmpFile = null;
		try {
			tmpFile = File.createTempFile("upgrade_", null);
		} catch (IOException ioe) {
			s_logger.error("Failed to create temporary file for resource: '{}'", name);
			throw new ResourceProcessorException(ResourceProcessorException.CODE_PREPARE,
					"Failed to create temporary file for resource: " + name, ioe);
		}
		
		try {
			FileUtils.copyInputStreamToFile(stream, tmpFile);
		} catch (IOException ioe) {
			s_logger.error("Failed to copy input stream for resource: '{}'", name);
			throw new ResourceProcessorException(ResourceProcessorException.CODE_PREPARE,
					"Failed to copy input stream for resource: "+ name, ioe);	
		}
		m_sourceResourceFiles.put(name, tmpFile);
	}

	@Override
	public void dropped(String resource) throws ResourceProcessorException {
		s_logger.debug("Upgrade script resource processor: dropped");
		
	}

	@Override
	public void dropAllResources() throws ResourceProcessorException {
		s_logger.debug("Upgrade script resource processor: droppedAllResources");
		
	}

	@Override
	public void prepare() throws ResourceProcessorException {
		s_logger.debug("Upgrade script resource processor: prepare");
		// Iterate over list of resources
		// TODO: Check for and execute pre-upgrade scripts here
		Set<String> sourceResources = m_sourceResourceFiles.keySet();
		for (String resource : sourceResources) {
			m_upgradeScript = m_sourceResourceFiles.get(resource);
		}
		try {
			executeScript(m_upgradeScript);
		} catch (Exception e) {
			s_logger.error("Failed to copy input stream for resource: '{}'");
			throw new ResourceProcessorException(ResourceProcessorException.CODE_PREPARE,
					"Failed to copy input stream for resource: "+ e);
		}
	}

	@Override
	public void commit() {
		s_logger.debug("Upgrade script resource processor: commit");
		
		
		
	}

	@Override
	public void rollback() {
		s_logger.debug("Upgrade script resource processor: rollback");
		
	}

	@Override
	public void cancel() {
		s_logger.debug("Upgrade script resource processor: cancel");
		
	}
	
	private void executeScript(File file) throws IOException {
		String path = file.getCanonicalPath();
		String[] cmdarray = {"/bin/bash", path};
		Runtime rt = Runtime.getRuntime();
		rt.exec(cmdarray);
	}

}
