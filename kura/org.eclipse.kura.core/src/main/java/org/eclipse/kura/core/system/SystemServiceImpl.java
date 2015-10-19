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
package org.eclipse.kura.core.system;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.net.URL;
import java.security.AccessController;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.PrivilegedAction;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;

import org.eclipse.kura.KuraException;
import org.eclipse.kura.core.util.IOUtil;
import org.eclipse.kura.core.util.NetUtil;
import org.eclipse.kura.core.util.ProcessUtil;
import org.eclipse.kura.core.util.SafeProcess;
import org.eclipse.kura.net.NetInterface;
import org.eclipse.kura.net.NetInterfaceAddress;
import org.eclipse.kura.net.NetworkService;
import org.eclipse.kura.system.SystemService;
import org.osgi.framework.Bundle;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.ComponentException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 */
public class SystemServiceImpl implements SystemService 
{
	private static final Logger s_logger = LoggerFactory.getLogger(SystemServiceImpl.class);

	private static boolean onCloudbees = false;

	private Properties       m_kuraProperties;
	private ComponentContext m_ctx;

	private NetworkService m_networkService;


	// ----------------------------------------------------------------
	//
	//   Dependencies
	//
	// ----------------------------------------------------------------
	public void setNetworkService(NetworkService networkService) {
		m_networkService = networkService;
	}

	public void unsetNetworkService(NetworkService networkService) {
		m_networkService = null;
	}


	// ----------------------------------------------------------------
	//
	//   Activation APIs
	//
	// ----------------------------------------------------------------

	@SuppressWarnings({ "rawtypes", "unchecked" })
	protected void activate(ComponentContext componentContext) 
	{
		m_ctx = componentContext;

		AccessController.doPrivileged(new PrivilegedAction()
		{
			public Object run()
			{
				try
				{
					// privileged code goes here, for example:
					onCloudbees = (new File("/private/eurotech/settings-security.xml")).exists();
					return null; // nothing to return
				}
				catch (Exception e)
				{
					System.out.println("Unable to execute privileged in SystemService");
					return null;
				}
			}
		});

		// load the defaults from the kura.properties files
		Properties kuraDefaults = new Properties();
		boolean updateTriggered = false;
		try {
			//special cases for older versions to fix relative path bug in v2.0.4 and earlier
			if(System.getProperty(KURA_CONFIG) != null && System.getProperty(KURA_CONFIG).trim().equals("file:kura/kura.properties")) {
				System.setProperty(KURA_CONFIG, "file:/opt/eclipse/kura/kura/kura.properties");
				updateTriggered = true;
				s_logger.warn("Overridding invalid kura.properties location");
			}
			if(System.getProperty("dpa.configuration") != null && System.getProperty("dpa.configuration").trim().equals("kura/dpa.properties")) {
				System.setProperty("dpa.configuration", "/opt/eclipse/kura/kura/dpa.properties");
				updateTriggered = true;
				s_logger.warn("Overridding invalid dpa.properties location");
			}
			if(System.getProperty("log4j.configuration") != null && System.getProperty("log4j.configuration").trim().equals("file:kura/log4j.properties")) {
				System.setProperty("log4j.configuration", "file:/opt/eclipse/kura/kura/log4j.properties");
				updateTriggered = true;
				s_logger.warn("Overridding invalid log4j.properties location");
			}

			// load the default kura.properties
			// look for kura.properties as resource in the classpath
			// if not found, look for such file in the kura.home directory
			String kuraHome = System.getProperty(KEY_KURA_HOME_DIR);
			String kuraConfig = System.getProperty(KURA_CONFIG);
			String kuraProperties = IOUtil.readResource(KURA_PROPS_FILE);

			if (kuraProperties != null) {
				kuraDefaults.load( new StringReader(kuraProperties));
				s_logger.info("Loaded Jar Resource kura.properties.");
			}
			else if (kuraConfig != null) {
				try {
					URL kuraConfigUrl = new URL(kuraConfig);
					kuraDefaults.load(kuraConfigUrl.openStream());
					s_logger.info("Loaded URL kura.properties: "+kuraConfig);
				}
				catch (Exception e) {
					s_logger.warn("Could not open kuraConfig URL", e);
				}
			}
			else if (kuraHome != null) {
				File kuraPropsFile = new File (kuraHome+File.separator+KURA_PROPS_FILE);
				if (kuraPropsFile.exists()) {
					FileReader fr= new FileReader(kuraPropsFile);
					kuraDefaults.load(fr);
					fr.close();
					s_logger.info("Loaded File kura.properties: "+kuraPropsFile);
				} else {
					s_logger.warn("File does not exist: " + kuraPropsFile);
				}

			}
			else {
				s_logger.error("Could not located kura.properties with kura.home "+kuraHome);
			}

			// load custom kura properties
			// look for kura_custom.properties as resource in the classpath
			// if not found, look for such file in the kura.home directory
			Properties kuraCustomProps = new Properties();
			String kuraCustomConfig = System.getProperty(KURA_CUSTOM_CONFIG);
			String kuraCustomProperties = IOUtil.readResource(KURA_CUSTOM_PROPS_FILE);

			if (kuraCustomProperties != null) {
				kuraCustomProps.load( new StringReader(kuraCustomProperties));
				s_logger.info("Loaded Jar Resource: " + KURA_CUSTOM_PROPS_FILE);
			}
			else if (kuraCustomConfig != null) {
				try {
					URL kuraConfigUrl = new URL(kuraCustomConfig);
					kuraCustomProps.load(kuraConfigUrl.openStream());
					s_logger.info("Loaded URL kura_custom.properties: "+kuraCustomConfig);
				}
				catch (Exception e) {
					s_logger.warn("Could not open kuraCustomConfig URL: " + e);
				}
			}
			else if (kuraHome != null) {
				File kuraCustomPropsFile = new File (kuraHome+File.separator+KURA_CUSTOM_PROPS_FILE);
				if (kuraCustomPropsFile.exists()) {
					kuraCustomProps.load( new FileReader(kuraCustomPropsFile));
					s_logger.info("Loaded File " + KURA_CUSTOM_PROPS_FILE + ": " + kuraCustomPropsFile);
				} else {
					s_logger.warn("File does not exist: " + kuraCustomPropsFile);
				}
			}
			else {
				s_logger.info("Did not locate a kura_custom.properties file in " + kuraHome);
			}

			// Override defaults with values from kura_custom.properties
			kuraDefaults.putAll(kuraCustomProps);

			//more path overrides based on earlier Kura problem with relative paths
			if(kuraDefaults.getProperty(KEY_KURA_HOME_DIR) != null && kuraDefaults.getProperty(KEY_KURA_HOME_DIR).trim().equals("kura")) {
				kuraDefaults.setProperty(KEY_KURA_HOME_DIR, "/opt/eclipse/kura/kura");
				updateTriggered = true;
				s_logger.warn("Overridding invalid kura.home location");
			}
			if(kuraDefaults.getProperty(KEY_KURA_PLUGINS_DIR) != null && kuraDefaults.getProperty(KEY_KURA_PLUGINS_DIR).trim().equals("kura/plugins")) {
				kuraDefaults.setProperty(KEY_KURA_PLUGINS_DIR, "/opt/eclipse/kura/kura/plugins");
				updateTriggered = true;
				s_logger.warn("Overridding invalid kura.plugins location");
			}	
			if(kuraDefaults.getProperty("kura.packages") != null && kuraDefaults.getProperty("kura.packages").trim().equals("kura/packages")) {
				kuraDefaults.setProperty("kura.packages", "/opt/eclipse/kura/kura/packages");
				updateTriggered = true;
				s_logger.warn("Overridding invalid kura.packages location");
			}


			if(updateTriggered) {
				File    directory;       // Desired current working directory

				directory = new File("/opt/eclipse/kura").getAbsoluteFile();
				if (directory.exists() || directory.mkdirs())
				{
					String oldDir = System.getProperty("user.dir");
					boolean result = (System.setProperty("user.dir", directory.getAbsolutePath()) != null);
					if(result) {
						s_logger.warn("Changed working directory to /opt/eclipse/kura from " + oldDir);
					}
				}



				/*
				// we were updated so we need to reload the DeploymentAdmin so it reads in the correct values
				s_logger.info("kura.home = " + kuraDefaults.getProperty(KEY_KURA_HOME_DIR));
				s_logger.info("kura.plugins = " + kuraDefaults.getProperty(KEY_KURA_PLUGINS_DIR));
				s_logger.info("kura.packages = " + kuraDefaults.getProperty("kura.packages"));

				//write out Kura defaults back to kuraPropsFile
				FileOutputStream fos = new FileOutputStream(kuraHome+File.separator+KURA_PROPS_FILE);
				kuraDefaults.store(fos, null);
				fos.flush();
				fos.getFD().sync();
				fos.close();

				s_logger.warn("Restarting DeploymentAgent to reload kura.properties");
				String bundleName = "org.eclipse.kura.deployment.agent";

				Bundle[] bundles = m_ctx.getBundleContext().getBundles();
				Long id = null;
				try {
					if(bundles != null && bundles.length > 0) {
						for(Bundle bundle : bundles) {
							if(bundle.getSymbolicName().equals(bundleName)) {
								id = bundle.getBundleId();
								break;
							}
						}
					}
				} catch (NumberFormatException e){
					throw new ComponentException("Error restarting org.eclipse.kura.deployment.agent.DeploymentAgentService to update Kura", e);
				}

				if (id != null) {
					Bundle bundle = m_ctx.getBundleContext().getBundle(id);
					if (bundle == null) {
						s_logger.error("Bundle ID {} not found", id);
						throw new ComponentException("Error restarting org.eclipse.kura.deployment.agent.DeploymentAgentService to update Kura");
					} else {
						try {
							bundle.stop();
							bundle.start();
						} catch (BundleException e) {
							s_logger.error("Failed to restart bundle ", e);
						}
					}
				}*/
			}	

			// build the m_kuraProperties instance with the defaults
			m_kuraProperties = new Properties(kuraDefaults);

			// take care of the CloudBees environment 
			// that is run in the continuous integration. 
			if (onCloudbees) m_kuraProperties.put(KEY_OS_NAME, OS_CLOUDBEES);

			// Put the Net Admin and Web interface availability property so that is available through a get.  
			Boolean hasNetAdmin = Boolean.valueOf(m_kuraProperties.getProperty(KEY_KURA_HAVE_NET_ADMIN, "true"));
			m_kuraProperties.put(KEY_KURA_HAVE_NET_ADMIN, hasNetAdmin);
			s_logger.info("Kura has net admin? " + hasNetAdmin);
			String hasWebInterface = m_kuraProperties.getProperty(KEY_KURA_HAVE_WEB_INTER, "true");
			m_kuraProperties.put(KEY_KURA_HAVE_WEB_INTER, hasWebInterface);
			s_logger.info("Kura has web interface? " + hasWebInterface);
			String kuraVersion = m_kuraProperties.getProperty(KEY_KURA_VERSION, "version-unknown");
			m_kuraProperties.put(KEY_KURA_VERSION, kuraVersion);
			s_logger.info("Kura version? " + kuraVersion);

			// load the System properties for what it makes sense
			//Properties systemProperties = System.getProperties();
			//m_kuraProperties.putAll(systemProperties);

			if(System.getProperty(KEY_KURA_NAME) != null){
				m_kuraProperties.put(KEY_KURA_NAME, System.getProperty(KEY_KURA_NAME));
			}
			if(System.getProperty(KEY_KURA_VERSION) != null){
				m_kuraProperties.put(KEY_KURA_VERSION, System.getProperty(KEY_KURA_VERSION));
			}
			if(System.getProperty(KEY_DEVICE_NAME) != null){
				m_kuraProperties.put(KEY_DEVICE_NAME, System.getProperty(KEY_DEVICE_NAME));
			}
			if(System.getProperty(KEY_PLATFORM) != null){
				m_kuraProperties.put(KEY_PLATFORM, System.getProperty(KEY_PLATFORM));
			}
			if(System.getProperty(KEY_MODEL_ID) != null){
				m_kuraProperties.put(KEY_MODEL_ID, System.getProperty(KEY_MODEL_ID));
			}
			if(System.getProperty(KEY_MODEL_NAME) != null){
				m_kuraProperties.put(KEY_MODEL_NAME, System.getProperty(KEY_MODEL_NAME));
			}
			if(System.getProperty(KEY_PART_NUMBER) != null){
				m_kuraProperties.put(KEY_PART_NUMBER, System.getProperty(KEY_PART_NUMBER));
			}
			if(System.getProperty(KEY_SERIAL_NUM) != null){
				m_kuraProperties.put(KEY_SERIAL_NUM, System.getProperty(KEY_SERIAL_NUM));
			}
			if(System.getProperty(KEY_BIOS_VERSION) != null){
				m_kuraProperties.put(KEY_BIOS_VERSION, System.getProperty(KEY_BIOS_VERSION));
			}
			if(System.getProperty(KEY_FIRMWARE_VERSION) != null){
				m_kuraProperties.put(KEY_FIRMWARE_VERSION, System.getProperty(KEY_FIRMWARE_VERSION));
			}
			if(System.getProperty(KEY_PRIMARY_NET_IFACE) != null){
				m_kuraProperties.put(KEY_PRIMARY_NET_IFACE, System.getProperty(KEY_PRIMARY_NET_IFACE));
			}
			if(System.getProperty(KEY_KURA_HOME_DIR) != null){
				m_kuraProperties.put(KEY_KURA_HOME_DIR, System.getProperty(KEY_KURA_HOME_DIR));
			}
			if(System.getProperty(KEY_KURA_PLUGINS_DIR) != null){
				m_kuraProperties.put(KEY_KURA_PLUGINS_DIR, System.getProperty(KEY_KURA_PLUGINS_DIR));
			}
			if(System.getProperty(KEY_KURA_DATA_DIR) != null){
				m_kuraProperties.put(KEY_KURA_DATA_DIR, System.getProperty(KEY_KURA_DATA_DIR));
			}
			if(System.getProperty(KEY_KURA_TMP_DIR) != null){
				m_kuraProperties.put(KEY_KURA_TMP_DIR, System.getProperty(KEY_KURA_TMP_DIR));
			}
			if(System.getProperty(KEY_KURA_SNAPSHOTS_DIR) != null){
				m_kuraProperties.put(KEY_KURA_SNAPSHOTS_DIR, System.getProperty(KEY_KURA_SNAPSHOTS_DIR));
			}
			if(System.getProperty(KEY_KURA_SNAPSHOTS_COUNT) != null){
				m_kuraProperties.put(KEY_KURA_SNAPSHOTS_COUNT, System.getProperty(KEY_KURA_SNAPSHOTS_COUNT));
			}
			if(System.getProperty(KEY_KURA_HAVE_NET_ADMIN) != null){
				m_kuraProperties.put(KEY_KURA_HAVE_NET_ADMIN, System.getProperty(KEY_KURA_HAVE_NET_ADMIN));
			}
			if(System.getProperty(KEY_KURA_HAVE_WEB_INTER) != null){
				m_kuraProperties.put(KEY_KURA_HAVE_WEB_INTER, System.getProperty(KEY_KURA_HAVE_WEB_INTER));
			}
			if(System.getProperty(KEY_KURA_STYLE_DIR) != null){
				m_kuraProperties.put(KEY_KURA_STYLE_DIR, System.getProperty(KEY_KURA_STYLE_DIR));
			}
			if(System.getProperty(KEY_KURA_WIFI_TOP_CHANNEL) != null){
				m_kuraProperties.put(KEY_KURA_WIFI_TOP_CHANNEL, System.getProperty(KEY_KURA_WIFI_TOP_CHANNEL));
			}
			if(System.getProperty(KEY_KURA_KEY_STORE_PWD) != null){
				m_kuraProperties.put(KEY_KURA_KEY_STORE_PWD, System.getProperty(KEY_KURA_KEY_STORE_PWD));
			}
			if(System.getProperty(KEY_KURA_TRUST_STORE_PWD) != null){
				m_kuraProperties.put(KEY_KURA_TRUST_STORE_PWD, System.getProperty(KEY_KURA_TRUST_STORE_PWD));
			}
			if(System.getProperty(KEY_FILE_COMMAND_ZIP_MAX_SIZE) != null){
				m_kuraProperties.put(KEY_FILE_COMMAND_ZIP_MAX_SIZE, System.getProperty(KEY_FILE_COMMAND_ZIP_MAX_SIZE));
			}
			if(System.getProperty(KEY_FILE_COMMAND_ZIP_MAX_NUMBER) != null){
				m_kuraProperties.put(KEY_FILE_COMMAND_ZIP_MAX_NUMBER, System.getProperty(KEY_FILE_COMMAND_ZIP_MAX_NUMBER));
			}
			if(System.getProperty(KEY_OS_ARCH) != null){
				m_kuraProperties.put(KEY_OS_ARCH, System.getProperty(KEY_OS_ARCH));
			}
			if(System.getProperty(KEY_OS_NAME) != null){
				m_kuraProperties.put(KEY_OS_NAME, System.getProperty(KEY_OS_NAME));
			}
			if(System.getProperty(KEY_OS_VER) != null){
				m_kuraProperties.put(KEY_OS_VER, System.getProperty(KEY_OS_VER));
			}
			if(System.getProperty(KEY_OS_DISTRO) != null){
				m_kuraProperties.put(KEY_OS_DISTRO, System.getProperty(KEY_OS_DISTRO));
			}
			if(System.getProperty(KEY_OS_DISTRO_VER) != null){
				m_kuraProperties.put(KEY_OS_DISTRO_VER, System.getProperty(KEY_OS_DISTRO_VER));
			}
			if(System.getProperty(KEY_JAVA_VERSION) != null){
				m_kuraProperties.put(KEY_JAVA_VERSION, System.getProperty(KEY_JAVA_VERSION));
			}
			if(System.getProperty(KEY_JAVA_VENDOR) != null){
				m_kuraProperties.put(KEY_JAVA_VENDOR, System.getProperty(KEY_JAVA_VENDOR));
			}
			if(System.getProperty(KEY_JAVA_VM_NAME) != null){
				m_kuraProperties.put(KEY_JAVA_VM_NAME, System.getProperty(KEY_JAVA_VM_NAME));
			}
			if(System.getProperty(KEY_JAVA_VM_VERSION) != null){
				m_kuraProperties.put(KEY_JAVA_VM_VERSION, System.getProperty(KEY_JAVA_VM_VERSION));
			}
			if(System.getProperty(KEY_JAVA_VM_INFO) != null){
				m_kuraProperties.put(KEY_JAVA_VM_INFO, System.getProperty(KEY_JAVA_VM_INFO));
			}
			if(System.getProperty(KEY_OSGI_FW_NAME) != null){
				m_kuraProperties.put(KEY_OSGI_FW_NAME, System.getProperty(KEY_OSGI_FW_NAME));
			}
			if(System.getProperty(KEY_OSGI_FW_VERSION) != null){
				m_kuraProperties.put(KEY_OSGI_FW_VERSION, System.getProperty(KEY_OSGI_FW_VERSION));
			}
			if(System.getProperty(KEY_JAVA_HOME) != null){
				m_kuraProperties.put(KEY_JAVA_HOME, System.getProperty(KEY_JAVA_HOME));
			}
			if(System.getProperty(KEY_FILE_SEP) != null){
				m_kuraProperties.put(KEY_FILE_SEP, System.getProperty(KEY_FILE_SEP));
			}
			if(System.getProperty(CONFIG_CONSOLE_DEVICE_MANAGE_SERVICE_IGNORE) != null){
				m_kuraProperties.put(CONFIG_CONSOLE_DEVICE_MANAGE_SERVICE_IGNORE, System.getProperty(CONFIG_CONSOLE_DEVICE_MANAGE_SERVICE_IGNORE));
			}


			if (getKuraHome() == null) {
				s_logger.error("Did not initialize kura.home");
			}
			else {
				s_logger.info("Kura home directory is " + getKuraHome());
				createDirIfNotExists(getKuraHome());
			}
			if (getKuraSnapshotsDirectory() == null) {
				s_logger.error("Did not initialize kura.snapshots");
			}
			else {
				s_logger.info("Kura snapshots directory is " + getKuraSnapshotsDirectory());
				createDirIfNotExists(getKuraSnapshotsDirectory());
			}
			if (getKuraTemporaryConfigDirectory() == null) {
				s_logger.error("Did not initialize kura.tmp");
			}
			else {
				s_logger.info("Kura tmp directory is " + getKuraTemporaryConfigDirectory());
				createDirIfNotExists(getKuraTemporaryConfigDirectory());
			}

			s_logger.info(new StringBuffer().append("Kura version ").append(getKuraVersion()).append(" is starting").toString());
		}
		catch (IOException e) {
			throw new ComponentException("Error loading default properties", e);
		}
	}

	protected void deactivate(ComponentContext componentContext) 
	{
		m_ctx = null;
		m_kuraProperties = null;
	}


	public void updated(Map<String,Object> properties)
	{
		// nothing to do
		// all properties of the System service are read-only
	}


	// ----------------------------------------------------------------
	//
	//   Service APIs
	//
	// ----------------------------------------------------------------

	/**
	 * Returns all KuraProperties for this system. The returned instances is 
	 * initialized by loading the kura.properties file. Properties defined at 
	 * the System level - for example using the java -D command line flag - 
	 * are used to overwrite the values loaded from the kura.properties file
	 * in a hierarchical configuration fashion.  
	 */
	public Properties getProperties() {
		return m_kuraProperties;
	}

	public String getPrimaryMacAddress() {
		String primaryNetworkInterfaceName = getPrimaryNetworkInterfaceName();
		String macAddress = null;
		InetAddress ip;

		if (OS_MAC_OSX.equals(getOsName())) {
			SafeProcess proc = null;
			try {
				s_logger.info("executing: ifconfig and looking for " + primaryNetworkInterfaceName);
				proc = ProcessUtil.exec("ifconfig");
				BufferedReader br = null;
				try {
					proc.waitFor();
					br = new BufferedReader(new InputStreamReader(proc.getInputStream()));
					String line = null;
					while((line = br.readLine()) != null) {
						if(line.startsWith(primaryNetworkInterfaceName)) {
							//get the next line and save the MAC
							line = br.readLine();
							if (!line.trim().startsWith("ether")) {
								line = br.readLine();
							}
							String[] splitLine = line.split(" ");
							if (splitLine.length > 0) {
								return splitLine[1].toUpperCase();
							}
						}
					}
				} catch(InterruptedException e) {
					e.printStackTrace();
				} finally {
					if(br != null){
						try{
							br.close();
						}catch(IOException ex){
							s_logger.error("I/O Exception while closing BufferedReader!");
						}
					}
				}
			} catch(Exception e) {
				s_logger.error("Failed to get network interfaces", e);
			}
			finally {
				if (proc != null) ProcessUtil.destroy(proc);
			}
		}else if(getOsName().contains("Windows")){
			try {
				s_logger.info("executing: InetAddress.getLocalHost " + primaryNetworkInterfaceName);
				ip = InetAddress.getLocalHost();
				Enumeration<NetworkInterface> networks = NetworkInterface.getNetworkInterfaces();
			    while(networks.hasMoreElements()) {
			        NetworkInterface network = networks.nextElement();
			        if(network.getIndex()==0){
			        	ip=network.getInetAddresses().nextElement();
			        }
			    }
				NetworkInterface network = NetworkInterface.getByInetAddress(ip);
				byte[] mac = network.getHardwareAddress();
				macAddress = NetUtil.hardwareAddressToString(mac);
				s_logger.info("macAddress " + macAddress);
			} catch (UnknownHostException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (SocketException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}else {
			try {
				List<NetInterface<? extends NetInterfaceAddress>> interfaces = m_networkService.getNetworkInterfaces();
				if (interfaces != null) {
					for (NetInterface<? extends NetInterfaceAddress> iface : interfaces) {
						if (iface.getName() != null && getPrimaryNetworkInterfaceName().equals(iface.getName())) {
							macAddress = NetUtil.hardwareAddressToString(iface.getHardwareAddress());
							break;
						}
					}
				}
			} catch (KuraException e) {
				s_logger.error("Failed to get network interfaces", e);
			}
		}

		return macAddress;
	}

	public String getPrimaryNetworkInterfaceName()
	{
		if(m_kuraProperties.getProperty(KEY_PRIMARY_NET_IFACE) != null) {
			return this.m_kuraProperties.getProperty(KEY_PRIMARY_NET_IFACE);
		} else {
			if (OS_MAC_OSX.equals(getOsName())) {
        	                return "en0";
        	        } else if (OS_LINUX.equals(getOsName())) {
        	                return "eth0";
        	        } else if (getOsName().contains("Windows")) {
        	                return "windows";
			} else {
				s_logger.error("Unsupported platform");
				return null;
			}
		}
	}

	public String getPlatform() {
		return this.m_kuraProperties.getProperty(KEY_PLATFORM);
	}

	public String getOsArch() {
		String override = this.m_kuraProperties.getProperty(KEY_OS_ARCH);
		if(override != null) return override;

		return System.getProperty(KEY_OS_ARCH);
	}

	public String getOsName() {
		String override = this.m_kuraProperties.getProperty(KEY_OS_NAME);
		if(override != null) return override;

		return System.getProperty(KEY_OS_NAME);
	}

	public String getOsVersion() {
		String override = this.m_kuraProperties.getProperty(KEY_OS_VER);
		if(override != null) return override;

		return System.getProperty(KEY_OS_VER);
	}

	public String getOsDistro() {
		return this.m_kuraProperties.getProperty(KEY_OS_DISTRO);
	}

	public String getOsDistroVersion() {
		return this.m_kuraProperties.getProperty(KEY_OS_DISTRO_VER);
	}

	public String getJavaVendor() {
		String override = this.m_kuraProperties.getProperty(KEY_JAVA_VENDOR);
		if(override != null) return override;

		return System.getProperty(KEY_JAVA_VENDOR);
	}

	public String getJavaVersion() {
		String override = this.m_kuraProperties.getProperty(KEY_JAVA_VERSION);
		if(override != null) return override;

		return System.getProperty(KEY_JAVA_VERSION);
	}

	public String getJavaVmName() {
		String override = this.m_kuraProperties.getProperty(KEY_JAVA_VM_NAME);
		if(override != null) return override;

		return System.getProperty(KEY_JAVA_VM_NAME);
	}

	public String getJavaVmVersion() {
		String override = this.m_kuraProperties.getProperty(KEY_JAVA_VM_VERSION);
		if(override != null) return override;

		return System.getProperty(KEY_JAVA_VM_VERSION);
	}

	public String getJavaVmInfo() {
		String override = this.m_kuraProperties.getProperty(KEY_JAVA_VM_INFO);
		if(override != null) return override;

		return System.getProperty(KEY_JAVA_VM_INFO);
	}

	public String getOsgiFwName() {
		String override = this.m_kuraProperties.getProperty(KEY_OSGI_FW_NAME);
		if(override != null) return override;

		return System.getProperty(KEY_OSGI_FW_NAME);
	}

	public String getOsgiFwVersion() {
		String override = this.m_kuraProperties.getProperty(KEY_OSGI_FW_VERSION);
		if(override != null) return override;

		return System.getProperty(KEY_OSGI_FW_VERSION);
	}

	public int getNumberOfProcessors() 
	{
		try {
			return Runtime.getRuntime().availableProcessors();
		}
		catch ( Throwable t ) {
			// NoSuchMethodError on pre-1.4 runtimes
		}
		return -1;
	}

	public long getTotalMemory() {
		return Runtime.getRuntime().totalMemory() / 1024;
	}

	public long getFreeMemory() {
		return Runtime.getRuntime().freeMemory() / 1024;
	}

	public String getFileSeparator() {
		String override = this.m_kuraProperties.getProperty(KEY_FILE_SEP);
		if(override != null) return override;

		return System.getProperty(KEY_FILE_SEP);
	}

	public String getJavaHome() {
		String override = this.m_kuraProperties.getProperty(KEY_JAVA_HOME);
		if(override != null) return override;

		return System.getProperty(KEY_JAVA_HOME);
	}

	public String getKuraName() {
		return this.m_kuraProperties.getProperty(KEY_KURA_NAME);
	}

	public String getKuraVersion() {
		return this.m_kuraProperties.getProperty(KEY_KURA_VERSION);
	}

	public String getKuraHome() 
	{
		return this.m_kuraProperties.getProperty(KEY_KURA_HOME_DIR);
	}

	public String getKuraPluginsDirectory() {
		return this.m_kuraProperties.getProperty(KEY_KURA_PLUGINS_DIR);
	}

	public String getKuraDataDirectory() {
		return this.m_kuraProperties.getProperty(KEY_KURA_DATA_DIR);
	}

	public String getKuraTemporaryConfigDirectory() {
		return this.m_kuraProperties.getProperty(KEY_KURA_TMP_DIR);
	}

	public String getKuraSnapshotsDirectory() {
		return this.m_kuraProperties.getProperty(KEY_KURA_SNAPSHOTS_DIR);
	}

	public int getKuraSnapshotsCount() {
		int iMaxCount   = 10;
		String maxCount = this.m_kuraProperties.getProperty(KEY_KURA_SNAPSHOTS_COUNT);
		if (maxCount != null && maxCount.trim().length() > 0) {
			try {
				iMaxCount = Integer.parseInt(maxCount);
			}
			catch (NumberFormatException nfe) {
				s_logger.error("Error - Invalid kura.snapshots.count setting. Using default.", nfe);
			}
		}
		return iMaxCount;
	}

	public int getKuraWifiTopChannel() {
		String topWifiChannel = m_kuraProperties.getProperty(KEY_KURA_WIFI_TOP_CHANNEL);
		if(topWifiChannel != null && topWifiChannel.trim().length() > 0) {
			return Integer.parseInt(topWifiChannel);
		}

		s_logger.warn("The last wifi channel is not defined for this system - setting to lowest common value of 11");
		return 11;
	}

	public String getKuraStyleDirectory() {
		return this.m_kuraProperties.getProperty(KEY_KURA_STYLE_DIR);
	}

	public String getKuraWebEnabled() {
		return this.m_kuraProperties.getProperty(KEY_KURA_HAVE_WEB_INTER);
	}
	
	public int getFileCommandZipMaxUploadSize(){
		String commandMaxUpload= this.m_kuraProperties.getProperty(KEY_FILE_COMMAND_ZIP_MAX_SIZE);
		if(commandMaxUpload != null && commandMaxUpload.trim().length() > 0){
			return Integer.parseInt(commandMaxUpload);
		}
		s_logger.warn("Maximum command line upload size not available. Set default to 100 MB");
		return 100;
	}
	
	public int getFileCommandZipMaxUploadNumber(){
		String commandMaxFilesUpload= this.m_kuraProperties.getProperty(KEY_FILE_COMMAND_ZIP_MAX_NUMBER);
		if(commandMaxFilesUpload != null && commandMaxFilesUpload.trim().length() > 0){
			return Integer.parseInt(commandMaxFilesUpload);
		}
		s_logger.warn("Missing the parameter that specifies the maximum number of files uploadable using the command servlet. Set default to 1024 files");
		return 1024;
	}

	public String getBiosVersion() {
		String override = this.m_kuraProperties.getProperty(KEY_BIOS_VERSION);
		if(override != null) return override;

		String biosVersion = UNSUPPORTED;

		if(OS_LINUX.equals(this.getOsName())) {
			if("2.6.34.9-WR4.2.0.0_standard".equals(getOsVersion()) || "2.6.34.12-WR4.3.0.0_standard".equals(getOsVersion())) {
				biosVersion = runSystemInfoCommand("eth_vers_bios");
			} else {
				String biosTmp = runSystemInfoCommand("dmidecode -s bios-version");
				if(biosTmp.length() > 0 && !biosTmp.contains("Permission denied")) {
					biosVersion = biosTmp;
				}
			}
		} else if (OS_MAC_OSX.equals(this.getOsName())) {
			String[] cmds = {"/bin/sh", "-c", "system_profiler SPHardwareDataType | grep 'Boot ROM'"};
			String biosTmp = runSystemInfoCommand(cmds);
			if(biosTmp.contains(": ")) {
				biosVersion = biosTmp.split(":\\s+")[1];
			}
		}

		return biosVersion;	
	}

	public String getDeviceName() 
	{
		String override = this.m_kuraProperties.getProperty(KEY_DEVICE_NAME);
		if(override != null) return override;

		String deviceName = UNKNOWN;		
		if(OS_MAC_OSX.equals(this.getOsName())) {
			String displayTmp = runSystemInfoCommand("scutil --get ComputerName");
			if(displayTmp.length() > 0) {
				deviceName = displayTmp;
			}
		} else if (OS_LINUX.equals(this.getOsName()) || OS_CLOUDBEES.equals(this.getOsName())) {
			String displayTmp = runSystemInfoCommand("hostname");
			if(displayTmp.length() > 0) {
				deviceName = displayTmp;
			}
		}		
		return deviceName;
	}

	public String getFirmwareVersion() {
		String override = this.m_kuraProperties.getProperty(KEY_FIRMWARE_VERSION);
		if(override != null) return override;

		String fwVersion = UNSUPPORTED;

		if(OS_LINUX.equals(this.getOsName())) {
			if("2.6.34.9-WR4.2.0.0_standard".equals(this.getOsVersion()) || "2.6.34.12-WR4.3.0.0_standard".equals(getOsVersion())) {
				fwVersion = runSystemInfoCommand("eth_vers_cpld") + " " + runSystemInfoCommand("eth_vers_uctl");
			}
		}

		return fwVersion;
	}

	public String getModelId() {
		String override = this.m_kuraProperties.getProperty(KEY_MODEL_ID);
		if(override != null) return override;

		String modelId = UNKNOWN;

		if(OS_MAC_OSX.equals(this.getOsName())) {
			String modelTmp = runSystemInfoCommand("sysctl -b hw.model");
			if(modelTmp.length() > 0) {
				modelId = modelTmp;
			}
		} else if (OS_LINUX.equals(this.getOsName())) {
			String modelTmp = runSystemInfoCommand("dmidecode -t system");
			if(modelTmp.contains("Version: ")) {
				modelId = modelTmp.split("Version:\\s+")[1].split("\n")[0];
			}			
		}

		return modelId;
	}

	public String getModelName() {
		String override = this.m_kuraProperties.getProperty(KEY_MODEL_NAME);
		if(override != null) return override;

		String modelName = UNKNOWN;

		if(OS_MAC_OSX.equals(this.getOsName())) {
			String[] cmds = {"/bin/sh", "-c", "system_profiler SPHardwareDataType | grep 'Model Name'"};
			String modelTmp = runSystemInfoCommand(cmds);
			if(modelTmp.contains(": ")) {
				modelName = modelTmp.split(":\\s+")[1];
			}
		} else if (OS_LINUX.equals(this.getOsName())) {
			String modelTmp = runSystemInfoCommand("dmidecode -t system");
			if(modelTmp.contains("Product Name: ")) {
				modelName = modelTmp.split("Product Name:\\s+")[1].split("\n")[0];
			}			
		}

		return modelName;
	}

	public String getPartNumber() {
		String override = this.m_kuraProperties.getProperty(KEY_PART_NUMBER);
		if(override != null) return override;

		String partNumber = UNSUPPORTED;

		if(OS_LINUX.equals(this.getOsName())) {
			if("2.6.34.9-WR4.2.0.0_standard".equals(getOsVersion()) || "2.6.34.12-WR4.3.0.0_standard".equals(getOsVersion())) {
				partNumber = runSystemInfoCommand("eth_partno_bsp") + " " + runSystemInfoCommand("eth_partno_epr");
			}
		}

		return partNumber;
	}

	public String getSerialNumber() {
		String override = this.m_kuraProperties.getProperty(KEY_SERIAL_NUM);
		if(override != null) return override;

		String serialNum = UNKNOWN;

		if(OS_MAC_OSX.equals(this.getOsName())) {
			String[] cmds = {"/bin/sh", "-c", "system_profiler SPHardwareDataType | grep 'Serial Number'"};
			String serialTmp = runSystemInfoCommand(cmds);
			if(serialTmp.contains(": ")) {
				serialNum = serialTmp.split(":\\s+")[1];
			}
		} else if (OS_LINUX.equals(this.getOsName())) {
			String serialTmp = runSystemInfoCommand("dmidecode -t system");
			if(serialTmp.contains("Serial Number: ")) {
				serialNum = serialTmp.split("Serial Number:\\s+")[1].split("\n")[0];
			}			
		}

		return serialNum;
	}

	public char[] getJavaKeyStorePassword() throws InvalidKeyException, NoSuchAlgorithmException, NoSuchPaddingException, IllegalBlockSizeException, BadPaddingException, IOException {
		String keyStorePwd = this.m_kuraProperties.getProperty(KEY_KURA_KEY_STORE_PWD);
		if (keyStorePwd != null) {
			return keyStorePwd.toCharArray();
		}
		return null;
	}

	public char[] getJavaTrustStorePassword() throws InvalidKeyException, NoSuchAlgorithmException, NoSuchPaddingException, IllegalBlockSizeException, BadPaddingException, IOException {
		String trustStorePwd = this.m_kuraProperties.getProperty(KEY_KURA_TRUST_STORE_PWD);
		if (trustStorePwd != null) {
			return trustStorePwd.toCharArray();
		}
		return null;
	}

	public Bundle[] getBundles() {
		if (m_ctx == null) {
			return null;
		}
		return m_ctx.getBundleContext().getBundles();
	}

	public List<String> getDeviceManagementServiceIgnore() {
		String servicesToIgnore = m_kuraProperties.getProperty(CONFIG_CONSOLE_DEVICE_MANAGE_SERVICE_IGNORE);
		if(servicesToIgnore != null && !servicesToIgnore.trim().isEmpty()) {
			String[] servicesArray = servicesToIgnore.split(",");
			if(servicesArray != null && servicesArray.length > 0) {
				List<String> services = new ArrayList<String>();
				for(String service : servicesArray) {
					services.add(service);
				}
				return services;
			}
		}

		return null;
	}

	// ----------------------------------------------------------------
	//
	//   Private Methods
	//
	// ----------------------------------------------------------------


	private String runSystemInfoCommand(String command) {
		return this.runSystemInfoCommand(command.split("\\s+"));
	}


	private String runSystemInfoCommand(String[] commands) {
		StringBuffer response = new StringBuffer(); 
		SafeProcess proc = null;
		BufferedReader br = null;
		try {
			proc = ProcessUtil.exec(commands);
			proc.waitFor();
			br = new BufferedReader(new InputStreamReader(proc.getInputStream()));
			String line = null;
			String newLine = "";
			while ((line = br.readLine()) != null) {
				response.append(newLine);
				response.append(line);
				newLine = "\n";
			}
		} catch(Exception e) {
			String command = "";
			String delim = "";
			for(int i=0; i<commands.length; i++) {
				command += delim + commands[i];
				delim = " ";
			}
			s_logger.error("failed to run commands " + command, e);
		}
		finally {
			if(br != null){
				if(br != null){
					try{
						br.close();
					}catch(IOException ex){
						s_logger.error("I/O Exception while closing BufferedReader!");
					}
				}
			}
			if (proc != null) ProcessUtil.destroy(proc);
		}
		return response.toString();
	}


	private void createDirIfNotExists(String fileName) 
	{
		// Make sure the configuration directory exists - create it if not		
		File file = new File(fileName);
		if(!file.exists()) {
			if(!file.mkdirs()) {
				s_logger.error("Failed to create the temporary configuration directory: " + fileName);
				System.exit(-1);
			}
		}
	}	
}
