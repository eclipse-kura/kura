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
package org.eclipse.kura.core.cloud;

import java.util.List;

import org.eclipse.kura.core.message.KuraBirthPayload;
import org.eclipse.kura.core.message.KuraDeviceProfile;
import org.eclipse.kura.core.message.KuraDisconnectPayload;
import org.eclipse.kura.core.message.KuraBirthPayload.KuraBirthPayloadBuilder;
import org.eclipse.kura.core.util.NetUtil;
import org.eclipse.kura.message.KuraPosition;
import org.eclipse.kura.net.NetInterface;
import org.eclipse.kura.net.NetInterfaceAddress;
import org.eclipse.kura.net.NetworkService;
import org.eclipse.kura.position.NmeaPosition;
import org.eclipse.kura.position.PositionService;
import org.eclipse.kura.system.SystemAdminService;
import org.eclipse.kura.system.SystemService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Utility class to build lifecycle payload messages.
 */
public class LifeCyclePayloadBuilder 
{
	private static final Logger s_logger = LoggerFactory.getLogger(LifeCyclePayloadBuilder.class);
	
	private static final String UNKNOWN = "UNKNOWN";

	private CloudServiceImpl m_cloudServiceImpl;
	
	LifeCyclePayloadBuilder(CloudServiceImpl cloudServiceImpl) {
		m_cloudServiceImpl = cloudServiceImpl;
	}
	
	
	public KuraBirthPayload buildBirthPayload()
	{
		// build device profile
		KuraDeviceProfile deviceProfile = buildDeviceProfile();

		// build application IDs
		String appIds = buildApplicationIDs();
		
		// build accept encoding
		String acceptEncoding = buildAcceptEncoding();
        
		// build device name
		CloudServiceOptions cso = m_cloudServiceImpl.getCloudServiceOptions();
		String deviceName = cso.getDeviceDisplayName();
		if (deviceName == null) {
			deviceName =  m_cloudServiceImpl.getSystemService().getDeviceName();
		}
		
		// build birth certificate
		KuraBirthPayloadBuilder birthPayloadBuilder = new KuraBirthPayloadBuilder();
		birthPayloadBuilder.withUptime(deviceProfile.getUptime())
		.withDisplayName(deviceName)
		.withModelName(deviceProfile.getModelName())
		.withModelId(deviceProfile.getModelId())
		.withPartNumber(deviceProfile.getPartNumber())
		.withSerialNumber(deviceProfile.getSerialNumber())
		.withFirmwareVersion(deviceProfile.getFirmwareVersion())
		.withBiosVersion(deviceProfile.getBiosVersion())
		.withOs(deviceProfile.getOs())
		.withOsVersion(deviceProfile.getOsVersion())
		.withJvmName(deviceProfile.getJvmName())
		.withJvmVersion(deviceProfile.getJvmVersion())
		.withJvmProfile(deviceProfile.getJvmProfile())
		.withKuraVersion(deviceProfile.getKuraVersion())
		.withConnectionInterface(deviceProfile.getConnectionInterface())
		.withConnectionIp(deviceProfile.getConnectionIp())
		.withAcceptEncoding(acceptEncoding)
		.withApplicationIdentifiers(appIds)
		.withAvailableProcessors(deviceProfile.getAvailableProcessors())
		.withTotalMemory(deviceProfile.getTotalMemory())
		.withOsArch(deviceProfile.getOsArch())
		.withOsgiFramework(deviceProfile.getOsgiFramework())
		.withOsgiFrameworkVersion(deviceProfile.getOsgiFrameworkVersion());
		
		if ((m_cloudServiceImpl.m_imei != null)
				&& (m_cloudServiceImpl.m_imei.length() > 0)
				&& !m_cloudServiceImpl.m_imei.equals("ERROR")) {
			birthPayloadBuilder.withModemImei(m_cloudServiceImpl.m_imei);
		}
		if ((m_cloudServiceImpl.m_iccid != null)
				&& (m_cloudServiceImpl.m_iccid.length() > 0)
				&& !m_cloudServiceImpl.m_iccid.equals("ERROR")) {
			birthPayloadBuilder.withModemIccid(m_cloudServiceImpl.m_iccid);
		}
		
		if ((m_cloudServiceImpl.m_imsi != null)
				&& (m_cloudServiceImpl.m_imsi.length() > 0)
				&& !m_cloudServiceImpl.m_imsi.equals("ERROR")) {
			birthPayloadBuilder.withModemImsi(m_cloudServiceImpl.m_imsi);
		}
		
        if (deviceProfile.getLatitude() != null &&
            deviceProfile.getLongitude() != null) {
            KuraPosition KuraPosition = new KuraPosition();
            KuraPosition.setLatitude(deviceProfile.getLatitude());
            KuraPosition.setLongitude(deviceProfile.getLongitude());
            KuraPosition.setAltitude(deviceProfile.getAltitude());
            birthPayloadBuilder.withPosition(KuraPosition);
        }
        
        return birthPayloadBuilder.build();
	}
	
	
	public KuraDisconnectPayload buildDisconnectPayload()
	{
		SystemService        systemService = m_cloudServiceImpl.getSystemService();
		SystemAdminService sysAdminService = m_cloudServiceImpl.getSystemAdminService();
		CloudServiceOptions   cloudOptions = m_cloudServiceImpl.getCloudServiceOptions();

		// build device name
		String deviceName = cloudOptions.getDeviceDisplayName();
		if (deviceName == null) {
			deviceName =  systemService.getDeviceName();
		}		
		
		// build payload
		KuraDisconnectPayload payload = new KuraDisconnectPayload(sysAdminService.getUptime(), 
															    deviceName);
		return payload;		
	}

	
	public KuraDeviceProfile buildDeviceProfile() 
	{
		SystemService        systemService = m_cloudServiceImpl.getSystemService();
		SystemAdminService sysAdminService = m_cloudServiceImpl.getSystemAdminService();
		NetworkService      networkService = m_cloudServiceImpl.getNetworkService();
		PositionService    positionService = m_cloudServiceImpl.getPositionService();		

		//
		// get the network information
		StringBuilder sbConnectionIp        = null;
		StringBuilder sbConnectionInterface = null;
		try {		
			List<NetInterface<? extends NetInterfaceAddress>> nis = networkService.getActiveNetworkInterfaces();
			if (!nis.isEmpty()) {
				sbConnectionIp        = new StringBuilder();
				sbConnectionInterface = new StringBuilder();

				for (NetInterface<? extends NetInterfaceAddress> ni : nis) {
					List<? extends NetInterfaceAddress> nias = ni.getNetInterfaceAddresses();
					if (nias != null && !nias.isEmpty()) {
						sbConnectionInterface.append(buildConnectionInterface(ni)).append(",");
						sbConnectionIp.append(buildConnectionIp(ni)).append(",");
					}
				}
				
				// Remove trailing comma
				sbConnectionIp.deleteCharAt(sbConnectionIp.length() - 1);
				sbConnectionInterface.deleteCharAt(sbConnectionInterface.length() - 1);
			}
		}
		catch (Exception se) {
			s_logger.warn("Error while getting ConnetionIP and ConnectionInterface", se);
		}

		String connectionIp        = sbConnectionIp != null ? sbConnectionIp.toString() : "UNKNOWN";
		String connectionInterface = sbConnectionInterface != null ? sbConnectionInterface.toString() : "UNKNOWN";
		
		//
		// get the network information
//		String primaryNetInterface = systemService.getPrimaryNetworkInterfaceName();
//		String connectionIp        = UNKNOWN;
//		String connectionInterface = UNKNOWN;
//		try {		
//			List<NetInterface<? extends NetInterfaceAddress>> nis = networkService.getActiveNetworkInterfaces();
//			if (!nis.isEmpty()) {
//				
//				// look for the primary network interface first
//				for (NetInterface<? extends NetInterfaceAddress> ni : nis) {
//					if (ni.getName().equals(primaryNetInterface)) {
//						List<? extends NetInterfaceAddress> nias = ni.getNetInterfaceAddresses();
//						if (nias != null && !nias.isEmpty()) {
//							connectionInterface = buildConnectionInterface(ni);				
//							connectionIp = buildConnectionIp(ni);
//							break;
//						}
//					}
//				}
//
//				// if not resolved, loop through all network interfaces until we find one with an address
//				if (UNKNOWN.equals(connectionIp) || UNKNOWN.equals(connectionInterface)) {			
//					s_logger.warn("Unresolved connectionIp for primary Network Interface. Looping through all interfaces...");
//					for (NetInterface<? extends NetInterfaceAddress> ni : nis) {
//						List<? extends NetInterfaceAddress> nias = ni.getNetInterfaceAddresses();
//						if (nias != null && !nias.isEmpty()) {
//							connectionInterface = buildConnectionInterface(ni);				
//							connectionIp = buildConnectionIp(ni);
//							break;
//						}
//					}
//				}
//			}
//			
//			if (UNKNOWN.equals(connectionIp) || UNKNOWN.equals(connectionInterface)) {			
//				s_logger.warn("Unresolved NetworkService reference or IP address. Defaulting to JVM Networking Information.");
//					InetAddress addr = NetUtil.getCurrentInetAddress();
//					if (addr != null) {   
//						connectionIp = addr.getHostAddress();
//						NetworkInterface netInterface = NetworkInterface.getByInetAddress(addr);
//						if (netInterface != null) {
//							connectionInterface = NetUtil.hardwareAddressToString(netInterface.getHardwareAddress());
//						}
//					}
//				}
//		}
//		catch (Exception se) {
//			s_logger.warn("Error while getting ConnetionIP and ConnectionInterface", se);
//		}			
		
		//
		// get the position information
		double latitude  = 0.0;
		double longitude = 0.0;
		double altitude  = 0.0;
		if (positionService != null) {
			NmeaPosition position = positionService.getNmeaPosition();
			if (position != null) {
				latitude  = position.getLatitude();
				longitude = position.getLongitude();
				altitude  = position.getAltitude();
			}
			else {
				s_logger.warn("Unresolved PositionService reference.");
			}
		}
		
		//
		// build the profile
		KuraDeviceProfile KuraDeviceProfile = new KuraDeviceProfile(
				sysAdminService.getUptime(),
				systemService.getDeviceName(),
				systemService.getModelName(),
				systemService.getModelId(),
				systemService.getPartNumber(),
				systemService.getSerialNumber(),
				systemService.getFirmwareVersion(),
				systemService.getBiosVersion(),
				systemService.getOsName(),
				systemService.getOsVersion(),
				systemService.getJavaVmName(),
				systemService.getJavaVmVersion() + " " + systemService.getJavaVmInfo(),
				systemService.getJavaVendor() + " " + systemService.getJavaVersion(),
				systemService.getKuraVersion(),
				connectionInterface, 
				connectionIp,
				latitude,
				longitude,
				altitude,
				String.valueOf(systemService.getNumberOfProcessors()),
				String.valueOf(systemService.getTotalMemory()),
				systemService.getOsArch(),
				systemService.getOsgiFwName(),
				systemService.getOsgiFwVersion()
		);		        
		return KuraDeviceProfile;		
	}


	private String buildConnectionIp(NetInterface<? extends NetInterfaceAddress> ni) 
	{
		String connectionIp = UNKNOWN;
		List<? extends NetInterfaceAddress> nias = ni.getNetInterfaceAddresses();
		if (nias != null && !nias.isEmpty()) {
			if (nias.get(0).getAddress() != null) {
				connectionIp = nias.get(0).getAddress().getHostAddress();
			}
		}
		return connectionIp;
	}


	private String buildConnectionInterface(NetInterface<? extends NetInterfaceAddress> ni) 
	{
		String connectionInterface = UNKNOWN;
		StringBuilder sb = new StringBuilder();
		sb.append(ni.getName())
		  .append(" (")
		  .append(NetUtil.hardwareAddressToString(ni.getHardwareAddress()))
		  .append(")");					
		connectionInterface = sb.toString();
		return connectionInterface;
	}
	
	
	private String buildApplicationIDs() 
	{
		String[] appIdArray = m_cloudServiceImpl.getCloudApplicationIdentifiers();
		StringBuilder sbAppIDs = new StringBuilder();
		for (int i=0; i<appIdArray.length; i++) {
			if (i != 0) {
				sbAppIDs.append(",");
			}
			sbAppIDs.append(appIdArray[i]);
		}
		return sbAppIDs.toString();
	}


	private String buildAcceptEncoding() 
	{
		String acceptEncoding = "";
		CloudServiceOptions options = m_cloudServiceImpl.getCloudServiceOptions();
		if (options.getEncodeGzip()) {
			acceptEncoding = "gzip";
		}
		return acceptEncoding;
	}
}
