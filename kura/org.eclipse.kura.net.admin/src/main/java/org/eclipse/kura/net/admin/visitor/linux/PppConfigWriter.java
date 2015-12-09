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
package org.eclipse.kura.net.admin.visitor.linux;

import java.io.File;
import java.io.IOException;
import java.util.Iterator;
import java.util.List;

import org.eclipse.kura.KuraErrorCode;
import org.eclipse.kura.KuraException;
import org.eclipse.kura.core.net.NetworkConfiguration;
import org.eclipse.kura.core.net.NetworkConfigurationVisitor;
import org.eclipse.kura.core.net.modem.ModemInterfaceConfigImpl;
import org.eclipse.kura.linux.net.modem.SupportedSerialModemInfo;
import org.eclipse.kura.linux.net.modem.SupportedSerialModemsInfo;
import org.eclipse.kura.linux.net.modem.SupportedUsbModemInfo;
import org.eclipse.kura.linux.net.modem.SupportedUsbModemsInfo;
import org.eclipse.kura.net.IP4Address;
import org.eclipse.kura.net.NetConfig;
import org.eclipse.kura.net.NetConfigIP4;
import org.eclipse.kura.net.NetInterfaceAddressConfig;
import org.eclipse.kura.net.NetInterfaceConfig;
import org.eclipse.kura.net.admin.modem.ModemPppConfigGenerator;
import org.eclipse.kura.net.admin.modem.PppPeer;
import org.eclipse.kura.net.admin.modem.SupportedSerialModemsFactoryInfo;
import org.eclipse.kura.net.admin.modem.SupportedUsbModemsFactoryInfo;
import org.eclipse.kura.net.admin.modem.SupportedSerialModemsFactoryInfo.SerialModemFactoryInfo;
import org.eclipse.kura.net.admin.modem.SupportedUsbModemsFactoryInfo.UsbModemFactoryInfo;
import org.eclipse.kura.net.admin.util.LinuxFileUtil;
import org.eclipse.kura.net.admin.visitor.linux.util.KuranetConfig;
import org.eclipse.kura.net.admin.visitor.linux.util.ModemXchangeScript;
import org.eclipse.kura.net.modem.ModemConfig;
import org.eclipse.kura.net.modem.ModemInterfaceAddressConfig;
import org.eclipse.kura.usb.UsbDevice;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PppConfigWriter implements NetworkConfigurationVisitor {

    private static final Logger s_logger = LoggerFactory.getLogger(PppConfigWriter.class);

    public static final String OS_PEERS_DIRECTORY = "/etc/ppp/peers/";
    public static final String OS_PPP_LOG_DIRECTORY = "/var/log/";
    public static final String OS_SCRIPTS_DIRECTORY = "/etc/ppp/scripts/";
    public static final String DNS_DELIM = ",";
    
    private static PppConfigWriter s_instance;
    
    public static PppConfigWriter getInstance() {
        if(s_instance == null) {
            s_instance = new PppConfigWriter();            
        }
        
        return s_instance;
    }
    
    private PppConfigWriter() {
        File peersDir = new File(OS_PEERS_DIRECTORY);
        if(!peersDir.exists()) {
            if(peersDir.mkdirs()) {
                s_logger.debug("Created directory: " + OS_PEERS_DIRECTORY);
            } else {
                s_logger.warn("Could not create peers directory: " + OS_PEERS_DIRECTORY);
            }
        }
        
        File scriptsDir = new File(OS_SCRIPTS_DIRECTORY);
        if(!scriptsDir.exists()) {
            if(scriptsDir.mkdirs()) {
                s_logger.debug("Created directory: " + OS_SCRIPTS_DIRECTORY);
            } else {
                s_logger.warn("Could not create scripts directory: " + OS_SCRIPTS_DIRECTORY);
            }
        }
    }
    
    @Override
    public void visit(NetworkConfiguration config) throws KuraException {
    	
        List<NetInterfaceConfig<? extends NetInterfaceAddressConfig>> netInterfaceConfigs = config.getModifiedNetInterfaceConfigs();
        for(NetInterfaceConfig<? extends NetInterfaceAddressConfig> netInterfaceConfig : netInterfaceConfigs) {
            if(netInterfaceConfig instanceof ModemInterfaceConfigImpl) {
                writeConfig((ModemInterfaceConfigImpl)netInterfaceConfig);
            }
        }
    }
    
    private void writeConfig(ModemInterfaceConfigImpl modemInterfaceConfig) throws KuraException {
    	
        String oldInterfaceName = modemInterfaceConfig.getName();
        String newInterfaceName = modemInterfaceConfig.getName();
        
        // Get the configs
        ModemConfig modemConfig = null;
        NetConfigIP4 netConfigIP4 = null;

        for(ModemInterfaceAddressConfig modemInterfaceAddressConfig : modemInterfaceConfig.getNetInterfaceAddresses()) {
            for(NetConfig netConfig : modemInterfaceAddressConfig.getConfigs()) {
                if(netConfig instanceof ModemConfig) {
                    modemConfig = (ModemConfig) netConfig;
                } else if(netConfig instanceof NetConfigIP4) {
                	netConfigIP4 = (NetConfigIP4) netConfig;
                }
            }
        }
         
        // Use the ppp number for the interface name, if configured
        int pppNum = -1;
        if(modemConfig != null) {
            pppNum = modemConfig.getPppNumber();
            if(pppNum >= 0) {
                newInterfaceName = "ppp" + pppNum;
                modemInterfaceConfig.setName(newInterfaceName);
            }
        }

        // Save the status and priority
        IfcfgConfigWriter.writeKuraExtendedConfig(modemInterfaceConfig);
        
        Class<? extends ModemPppConfigGenerator> configClass = null;
        UsbDevice usbDevice = modemInterfaceConfig.getUsbDevice();
        int baudRate = -1;
		if (usbDevice != null) {
			SupportedUsbModemInfo modemInfo = SupportedUsbModemsInfo.getModem(usbDevice);
			UsbModemFactoryInfo usbFactoryInfo = SupportedUsbModemsFactoryInfo.getModem(modemInfo);
			if (usbFactoryInfo != null) {
				configClass = usbFactoryInfo.getConfigGeneratorClass();
			}
			baudRate = 921600;
		} else {
			SupportedSerialModemInfo serialModemInfo = SupportedSerialModemsInfo.getModem();
			SerialModemFactoryInfo serialFactoryInfo = SupportedSerialModemsFactoryInfo.getModem(serialModemInfo);
			configClass = serialFactoryInfo.getConfigGeneratorClass();
			baudRate = serialModemInfo.getDriver().getComm().getBaudRate();
		}
		
		String pppPeerFilename = formPeerFilename(usbDevice);
		String pppLogfile = formPppLogFilename(usbDevice);
		String chatFilename = formChatFilename(usbDevice);
		String disconnectFilename = formDisconnectFilename(usbDevice);

		/*
		String tmpPppPeerFilename = new StringBuffer().append(pppPeerFilename).append(".tmp").toString();
		String tmpPppLogfile = new StringBuffer().append(pppLogfile).append(".tmp").toString();
		String tmpChatFilename = new StringBuffer().append(chatFilename).append(".tmp").toString();
		String tmpDisconnectFilename = new StringBuffer().append(chatFilename).append(".tmp").toString();
		*/
		
		// Cleanup values associated with the old name if the interface name has changed
		if (!oldInterfaceName.equals(newInterfaceName)) {
			try {
				// Remove the old ppp peers symlink
				s_logger.debug("Removing old symlinks to " + pppPeerFilename);
				removeSymbolicLinks(pppPeerFilename, OS_PEERS_DIRECTORY);
				
				// Remove the old modem identifier
				StringBuilder key = new StringBuilder("net.interface.").append(oldInterfaceName).append(".modem.identifier");
				s_logger.debug("Removing modem identifier for " + oldInterfaceName);
				KuranetConfig.deleteProperty(key.toString());
				
				// Remove custom dns servers
				key = new StringBuilder("net.interface.").append(oldInterfaceName).append(".config.dnsServers");
				s_logger.debug("Removing dns servers for " + oldInterfaceName);
				KuranetConfig.deleteProperty(key.toString());
				
				// Remove gpsEnabled 
				key = new StringBuilder().append("net.interface.").append(oldInterfaceName).append(".config.gpsEnabled");
				s_logger.debug("Removing gpsEnabled for " + oldInterfaceName);
				KuranetConfig.deleteProperty(key.toString());
				
				// Remove status
				IfcfgConfigWriter.removeKuraExtendedConfig(oldInterfaceName);
			} catch (IOException e) {
				throw new KuraException(KuraErrorCode.INTERNAL_ERROR, e);
			}
		}
            
		if (configClass != null) {
			try {
				ModemPppConfigGenerator scriptGenerator = configClass
						.newInstance();

				if (modemConfig != null) {
					s_logger.debug("Writing connect scripts for " + modemInterfaceConfig.getName() + " using " + configClass.toString());

					s_logger.debug("Writing " + pppPeerFilename);
					PppPeer pppPeer = scriptGenerator.getPppPeer(getDeviceId(usbDevice), modemConfig,pppLogfile, chatFilename, disconnectFilename);
					pppPeer.setBaudRate(baudRate);
					pppPeer.write(pppPeerFilename);

					s_logger.debug("Writing " + chatFilename);
					ModemXchangeScript connectScript = scriptGenerator.getConnectScript(modemConfig);
					connectScript.writeScript(chatFilename);

					s_logger.debug("Writing " + disconnectFilename);
					ModemXchangeScript disconnectScript = scriptGenerator.getDisconnectScript(modemConfig);
					disconnectScript.writeScript(disconnectFilename);

					if (pppNum >= 0) {
						s_logger.debug("Linking peer file using ppp number: " + pppNum);
						String symlinkFilename = formPeerLinkAbsoluteName(pppNum);
						LinuxFileUtil.createSymbolicLink(pppPeerFilename, symlinkFilename);
					} else {
						s_logger.error("Can't create symbolic link to " + pppPeerFilename + ", invalid ppp number: " + pppNum);
					}

					String modemIdentifier = modemInterfaceConfig.getModemIdentifier();
					if (modemIdentifier != null) {
						StringBuilder key = new StringBuilder("net.interface.").append(modemInterfaceConfig.getName()).append(".modem.identifier");
						s_logger.debug("Storing modem identifier " + modemIdentifier + " using key: " + key);
						KuranetConfig.setProperty(key.toString(), modemIdentifier);
					}
					
					// Custom dns servers
					if (netConfigIP4 != null) {
						StringBuilder key = new StringBuilder("net.interface.").append(modemInterfaceConfig.getName()).append(".config.dnsServers");

						List<IP4Address> dnsServers = netConfigIP4.getDnsServers();
						if(dnsServers != null && !dnsServers.isEmpty()) {
							StringBuilder serversSB = new StringBuilder();
							
							Iterator<IP4Address> it = dnsServers.iterator();
							serversSB.append(it.next().getHostAddress());
							while(it.hasNext()) {
								serversSB.append(DNS_DELIM).append(it.next().getHostAddress());
							}

							s_logger.debug("Storing DNS servers " + serversSB + " using key: " + key);
							KuranetConfig.setProperty(key.toString(), serversSB.toString());
						} else {
							KuranetConfig.deleteProperty(key.toString());
						}
					}
					
					StringBuilder key = new StringBuilder().append("net.interface.").append(newInterfaceName).append(".config.gpsEnabled");
					s_logger.debug("Setting gpsEnabled for " + newInterfaceName);
					KuranetConfig.setProperty(key.toString(), Boolean.toString(modemConfig.isGpsEnabled()));
					
					key = new StringBuilder().append("net.interface.").append(newInterfaceName).append(".config.resetTimeout");
					s_logger.debug("Setting modem resetTimeout for " + newInterfaceName);
					KuranetConfig.setProperty(key.toString(), Integer.toString(modemConfig.getResetTimeout()));
					
					key = new StringBuilder().append("net.interface.").append(newInterfaceName).append(".config.activeSimSlot");
					s_logger.debug("Setting active SIM card slot for " + newInterfaceName);
					KuranetConfig.setProperty(key.toString(), Integer.toString(modemConfig.getActiveSimCardSlot().getValue()));
				} else {
					s_logger.error("Error writing connect scripts - modemConfig is null");
				}
			} catch (Exception e) {
				s_logger.error("Could not write modem config", e);
			}
		}
    }
    
    public static String formPeerFilename(UsbDevice usbDevice) {
        StringBuffer buf = new StringBuffer();
        buf.append(OS_PEERS_DIRECTORY);
        buf.append(formBaseFilename(usbDevice));
        return buf.toString();
    }
    
    public static String formPppLogFilename(UsbDevice usbDevice) {
        StringBuffer buf = new StringBuffer();
        buf.append(OS_PPP_LOG_DIRECTORY);
        buf.append(formBaseFilename(usbDevice));
        return buf.toString();
    }

    public static String formChatFilename(UsbDevice usbDevice) {
        StringBuffer buf = new StringBuffer();
        buf.append(OS_SCRIPTS_DIRECTORY);
        buf.append("chat");
        buf.append('_');
        buf.append(formBaseFilename(usbDevice));
        return buf.toString();
    }
    
    public static String formPeerLinkName(int pppUnitNo) {
        StringBuffer peerLinkName = new StringBuffer();
        peerLinkName.append("ppp");
        peerLinkName.append(pppUnitNo);

        return peerLinkName.toString();
    }

    public static String formPeerLinkAbsoluteName(int pppUnitNo) {
        StringBuffer peerLink = new StringBuffer();
        peerLink.append(OS_PEERS_DIRECTORY);
        peerLink.append(formPeerLinkName(pppUnitNo));
        return peerLink.toString();
    }

    public static String formDisconnectFilename(UsbDevice usbDevice) {
        StringBuffer buf = new StringBuffer();
        buf.append(OS_SCRIPTS_DIRECTORY);
        buf.append("disconnect");
        buf.append('_');
        buf.append(formBaseFilename(usbDevice));
        return buf.toString();
    }
    
    private static String formBaseFilename(UsbDevice usbDevice) {
        StringBuffer sb = new StringBuffer();
        
        if(usbDevice != null) {
            SupportedUsbModemInfo modemInfo = SupportedUsbModemsInfo.getModem(usbDevice);
            if(modemInfo != null) {
                sb.append(modemInfo.getDeviceName());
                sb.append('_');
                sb.append(usbDevice.getUsbPort());
            }
        } else {
        	SupportedSerialModemInfo modemInfo = SupportedSerialModemsInfo.getModem();
        	if(modemInfo != null) {
        		sb.append(modemInfo.getModemName());
        	}
        }
        return sb.toString();
    }
    
	private static String getDeviceId(UsbDevice usbDevice) {

		StringBuffer sb = new StringBuffer();
		if (usbDevice != null) {
			SupportedUsbModemInfo modemInfo = SupportedUsbModemsInfo.getModem(usbDevice);
			if (modemInfo != null) {
				sb.append(modemInfo.getDeviceName());
			}
		} else {
			SupportedSerialModemInfo modemInfo = SupportedSerialModemsInfo.getModem();
			if (modemInfo != null) {
				sb.append(modemInfo.getModemName());
			}
		}

		return sb.toString();
	}

    // Delete all symbolic links to the specified target file in the specified directory
    private void removeSymbolicLinks(String target, String directory) throws IOException {
        File targetFile = new File(target);
        File dir = new File(directory);
        if(dir.isDirectory()) {
            for(File file : dir.listFiles()) {
                if(file.getAbsolutePath().equals(targetFile.getAbsolutePath())) {
                    // this is the target file
                    continue;
                }
                
                if(file.getCanonicalPath().equals(targetFile.getAbsolutePath())) {
                    s_logger.debug("Deleting " + file.getAbsolutePath());
                    file.delete();
                }
            }
        }
    }
}
