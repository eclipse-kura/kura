/*******************************************************************************
 * Copyright (c) 2011, 2016 Eurotech and/or its affiliates
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Eurotech
 *******************************************************************************/
package org.eclipse.kura.net.admin.monitor;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import org.eclipse.kura.KuraException;
import org.eclipse.kura.comm.CommURI;
import org.eclipse.kura.core.net.NetworkConfiguration;
import org.eclipse.kura.linux.net.ConnectionInfoImpl;
import org.eclipse.kura.linux.net.modem.SupportedSerialModemInfo;
import org.eclipse.kura.linux.net.modem.SupportedSerialModemsInfo;
import org.eclipse.kura.linux.net.modem.SupportedUsbModemInfo;
import org.eclipse.kura.linux.net.modem.SupportedUsbModemsInfo;
import org.eclipse.kura.linux.net.util.LinuxNetworkUtil;
import org.eclipse.kura.net.ConnectionInfo;
import org.eclipse.kura.net.NetConfig;
import org.eclipse.kura.net.NetConfigIP4;
import org.eclipse.kura.net.NetInterface;
import org.eclipse.kura.net.NetInterfaceAddress;
import org.eclipse.kura.net.NetInterfaceAddressConfig;
import org.eclipse.kura.net.NetInterfaceConfig;
import org.eclipse.kura.net.NetInterfaceStatus;
import org.eclipse.kura.net.NetworkService;
import org.eclipse.kura.net.admin.NetworkConfigurationService;
import org.eclipse.kura.net.admin.event.NetworkConfigurationChangeEvent;
import org.eclipse.kura.net.admin.event.NetworkStatusChangeEvent;
import org.eclipse.kura.net.admin.modem.CellularModemFactory;
import org.eclipse.kura.net.admin.modem.EvdoCellularModem;
import org.eclipse.kura.net.admin.modem.HspaCellularModem;
import org.eclipse.kura.net.admin.modem.IModemLinkService;
import org.eclipse.kura.net.admin.modem.PppFactory;
import org.eclipse.kura.net.admin.modem.PppState;
import org.eclipse.kura.net.admin.modem.SupportedSerialModemsFactoryInfo;
import org.eclipse.kura.net.admin.modem.SupportedSerialModemsFactoryInfo.SerialModemFactoryInfo;
import org.eclipse.kura.net.admin.modem.SupportedUsbModemsFactoryInfo;
import org.eclipse.kura.net.admin.modem.SupportedUsbModemsFactoryInfo.UsbModemFactoryInfo;
import org.eclipse.kura.net.admin.visitor.linux.util.KuranetConfig;
import org.eclipse.kura.net.modem.CellularModem;
import org.eclipse.kura.net.modem.ModemAddedEvent;
import org.eclipse.kura.net.modem.ModemConfig;
import org.eclipse.kura.net.modem.ModemDevice;
import org.eclipse.kura.net.modem.ModemGpsDisabledEvent;
import org.eclipse.kura.net.modem.ModemGpsEnabledEvent;
import org.eclipse.kura.net.modem.ModemInterface;
import org.eclipse.kura.net.modem.ModemManagerService;
import org.eclipse.kura.net.modem.ModemMonitorListener;
import org.eclipse.kura.net.modem.ModemMonitorService;
import org.eclipse.kura.net.modem.ModemReadyEvent;
import org.eclipse.kura.net.modem.ModemRemovedEvent;
import org.eclipse.kura.net.modem.ModemTechnologyType;
import org.eclipse.kura.net.modem.SerialModemDevice;
import org.eclipse.kura.system.SystemService;
import org.eclipse.kura.usb.UsbDeviceEvent;
import org.eclipse.kura.usb.UsbModemDevice;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventAdmin;
import org.osgi.service.event.EventConstants;
import org.osgi.service.event.EventHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ModemMonitorServiceImpl implements ModemMonitorService, ModemManagerService, EventHandler {

	private static final Logger s_logger = LoggerFactory.getLogger(ModemMonitorServiceImpl.class);

	private ComponentContext      m_ctx;
	private static final String[] EVENT_TOPICS = new String[] {
		NetworkConfigurationChangeEvent.NETWORK_EVENT_CONFIG_CHANGE_TOPIC,
		ModemAddedEvent.MODEM_EVENT_ADDED_TOPIC,
		ModemRemovedEvent.MODEM_EVENT_REMOVED_TOPIC, };

	private static final long THREAD_INTERVAL = 30000;
	private static final long THREAD_TERMINATION_TOUT = 1; // in seconds

	private static Object s_lock = new Object();

	private static Future<?>  task;
	private static AtomicBoolean stopThread;

	private SystemService m_systemService;
	private NetworkService m_networkService;
	private NetworkConfigurationService m_netConfigService;
	private EventAdmin m_eventAdmin;

	private List<ModemMonitorListener>m_listeners;

	private ExecutorService m_executor;

	private Map<String, CellularModem> m_modems;
	private Map<String, InterfaceState> m_interfaceStatuses;

	private NetworkConfiguration m_networkConfig;

	private boolean m_serviceActivated; 

	private PppState m_pppState;
	private long m_resetTimerStart;

	public void setNetworkService(NetworkService networkService) {
		m_networkService = networkService;
	}

	public void unsetNetworkService(NetworkService networkService) {
		m_networkService = null;
	}

	public void setEventAdmin(EventAdmin eventAdmin) {
		m_eventAdmin = eventAdmin;
	}

	public void unsetEventAdmin(EventAdmin eventAdmin) {
		m_eventAdmin = null;
	}

	public void setNetworkConfigurationService(NetworkConfigurationService netConfigService) {
		m_netConfigService = netConfigService;
	}

	public void unsetNetworkConfigurationService(NetworkConfigurationService netConfigService) {
		m_netConfigService = null;
	}

	public void setSystemService(SystemService systemService) {
		m_systemService = systemService;
	}

	public void unsetSystemService(SystemService systemService) {
		m_systemService = null;
	}

	protected void activate(ComponentContext componentContext)  {

		// save the bundle context
		m_ctx = componentContext;

		m_pppState = PppState.NOT_CONNECTED;
		m_resetTimerStart = 0L;

		Dictionary<String, String[]> d = new Hashtable<String, String[]>();
		d.put(EventConstants.EVENT_TOPIC, EVENT_TOPICS);
		m_ctx.getBundleContext().registerService(EventHandler.class.getName(), this, d);

		m_modems = new HashMap<String, CellularModem>();
		m_interfaceStatuses = new HashMap<String, InterfaceState>();
		m_listeners = new ArrayList<ModemMonitorListener>();

		stopThread = new AtomicBoolean();

		// track currently installed modems
		try {
			m_networkConfig = m_netConfigService.getNetworkConfiguration();
			for(NetInterface<? extends NetInterfaceAddress> netInterface : m_networkService.getNetworkInterfaces()) {
				if(netInterface instanceof ModemInterface) {
					ModemDevice modemDevice = ((ModemInterface<?>) netInterface).getModemDevice();
					trackModem(modemDevice);
				}
			}
		} catch (Exception e) {
			s_logger.error("Error getting installed modems", e);
		}

		stopThread.set(false);
		m_executor = Executors.newSingleThreadExecutor();
		task = m_executor.submit(new Runnable() {
			@Override
			public void run() {
				while (!stopThread.get()) {
					Thread.currentThread().setName("ModemMonitor");
					try {
						monitor();
						monitorWait();
					} catch (InterruptedException interruptedException) {
						Thread.interrupted();
						s_logger.debug("modem monitor interrupted - {}", interruptedException);
					} catch (Throwable t) {
						s_logger.error("activate() :: Exception while monitoring cellular connection {}", t);
					}
				}
			}});

		m_serviceActivated = true;
		s_logger.debug("ModemMonitor activated and ready to receive events");
	}

	protected void deactivate(ComponentContext componentContext) {
		m_listeners = null;
		PppFactory.releaseAllPppServices();
		if ((task != null) && (!task.isDone())) {
			stopThread.set(true);
			monitorNotity();
			s_logger.debug("Cancelling ModemMonitor task ...");
			task.cancel(true);
			s_logger.info("ModemMonitor task cancelled? = {}", task.isDone());
			task = null;
		}

		if (m_executor != null) {
			s_logger.debug("Terminating ModemMonitor Thread ...");
			m_executor.shutdownNow();
			try {
				m_executor.awaitTermination(THREAD_TERMINATION_TOUT, TimeUnit.SECONDS);
			} catch (InterruptedException e) {
				s_logger.warn("Interrupted", e);
			}
			s_logger.info("ModemMonitor Thread terminated? - {}", m_executor.isTerminated());
			m_executor = null;
		}
		m_serviceActivated = false;

		m_networkConfig = null;
	}

	@Override
	public void handleEvent(Event event) {
		s_logger.debug("handleEvent - topic: {}", event.getTopic());
		String topic = event.getTopic();
		if (topic.equals(NetworkConfigurationChangeEvent.NETWORK_EVENT_CONFIG_CHANGE_TOPIC)) {

			NetworkConfigurationChangeEvent netConfigChangedEvent = (NetworkConfigurationChangeEvent)event;
			String [] propNames = netConfigChangedEvent.getPropertyNames();
			if ((propNames != null) && (propNames.length > 0)) {
				Map<String, Object> props = new HashMap<String, Object>();
				for (String propName : propNames) {
					Object prop = netConfigChangedEvent.getProperty(propName);
					if (prop != null) {
						props.put(propName, prop);
					}
				}
				try {
					final NetworkConfiguration newNetworkConfig = new NetworkConfiguration(props);
					ExecutorService ex = Executors.newSingleThreadExecutor();
					ex.submit(new Runnable() {
						@Override
						public void run() {
							processNetworkConfigurationChangeEvent(newNetworkConfig);
						}
					});
				} catch (Exception e) {
					s_logger.error("Failed to handle the NetworkConfigurationChangeEvent - {}", e);
				}
			}
		} else if (topic.equals(ModemAddedEvent.MODEM_EVENT_ADDED_TOPIC)) {
			ModemAddedEvent modemAddedEvent = (ModemAddedEvent)event;
			final ModemDevice modemDevice = modemAddedEvent.getModemDevice();
			if (m_serviceActivated) {
				ExecutorService ex = Executors.newSingleThreadExecutor();
				ex.submit(new Runnable() {
					@Override
					public void run() {
						trackModem(modemDevice);
					}});
			}
		} else if (topic.equals(ModemRemovedEvent.MODEM_EVENT_REMOVED_TOPIC)) {
			ModemRemovedEvent modemRemovedEvent = (ModemRemovedEvent)event;
			String usbPort = (String)modemRemovedEvent.getProperty(UsbDeviceEvent.USB_EVENT_USB_PORT_PROPERTY);
			m_modems.remove(usbPort);
		}	
	}


	@Override
	public CellularModem getModemService (String usbPort) {
		return m_modems.get(usbPort);
	}

	@Override
	public Collection<CellularModem> getAllModemServices() {
		return m_modems.values();
	}

	private NetInterfaceStatus getNetInterfaceStatus (List<NetConfig> netConfigs) {

		NetInterfaceStatus interfaceStatus = NetInterfaceStatus.netIPv4StatusUnknown;
		if ((netConfigs != null) && (netConfigs.size() > 0)) {
			for (NetConfig netConfig : netConfigs) {
				if (netConfig instanceof NetConfigIP4) {
					interfaceStatus = ((NetConfigIP4) netConfig).getStatus();
					break;
				}
			}
		}
		return interfaceStatus;
	}
	
	private void setNetInterfaceStatus(NetInterfaceStatus netInterfaceStatus, List<NetConfig> netConfigs) {
		if ((netConfigs != null) && !netConfigs.isEmpty()) {
			for (NetConfig netConfig : netConfigs) {
				if (netConfig instanceof NetConfigIP4) {
					((NetConfigIP4) netConfig).setStatus(netInterfaceStatus);
					break;
				}
			}
		}
	}

	@Override
	public void registerListener(ModemMonitorListener newListener) {
		boolean found = false;
		if (m_listeners == null) {
			m_listeners = new ArrayList<ModemMonitorListener>();
		}
		if (!m_listeners.isEmpty()) {
			for (ModemMonitorListener listener : m_listeners) {
				if (listener.equals(newListener)) {
					found = true;
					break;
				}
			}
		}
		if (!found) {
			m_listeners.add(newListener);
		}
	}

	@Override
	public void unregisterListener(ModemMonitorListener listenerToUnregister) {
		if ((m_listeners != null) && (!m_listeners.isEmpty())) {

			for (int i = 0; i < m_listeners.size(); i++) {
				if ((m_listeners.get(i)).equals(listenerToUnregister)) {
					m_listeners.remove(i);
				}
			}
		}
	}

	private void processNetworkConfigurationChangeEvent(NetworkConfiguration newNetworkConfig) {
		synchronized (s_lock) {
			if (m_modems == null || m_modems.isEmpty()){
				return;
			}
			for (Map.Entry<String, CellularModem> modemEntry : m_modems.entrySet()) {
				String usbPort = modemEntry.getKey();
				CellularModem modem = modemEntry.getValue();
				try {
					String ifaceName = null;
					if (m_networkService != null) {
						ifaceName = m_networkService.getModemPppPort(modem.getModemDevice());
					}
					if (ifaceName != null) {
						List<NetConfig> oldNetConfigs = modem.getConfiguration();
						NetInterfaceConfig<? extends NetInterfaceAddressConfig> netInterfaceConfig = newNetworkConfig.getNetInterfaceConfig(ifaceName);
						if (netInterfaceConfig == null) {
							netInterfaceConfig = newNetworkConfig.getNetInterfaceConfig(usbPort);
						}
						List<NetConfig>newNetConfigs = null;
						IModemLinkService pppService = null;
						int ifaceNo = getInterfaceNumber(oldNetConfigs);
						if (ifaceNo >= 0) {
							pppService = PppFactory.obtainPppService(ifaceNo, modem.getDataPort());
						}

						if (netInterfaceConfig != null) {
							newNetConfigs = getNetConfigs(netInterfaceConfig);
						} else {
							if ((oldNetConfigs != null) && (pppService != null)) {
								if (!ifaceName.equals(pppService.getIfaceName())) {
									StringBuilder key = new StringBuilder().append("net.interface.").append(ifaceName).append(".config.ip4.status");
							        String statusString = KuranetConfig.getProperty(key.toString());
							        NetInterfaceStatus netInterfaceStatus = NetInterfaceStatus.netIPv4StatusDisabled;
							        if(statusString != null && !statusString.isEmpty()) {
							            netInterfaceStatus = NetInterfaceStatus.valueOf(statusString);
							        }
									
									newNetConfigs = oldNetConfigs;
									oldNetConfigs = null;
									try {
										setInterfaceNumber(ifaceName, newNetConfigs);
										setNetInterfaceStatus(netInterfaceStatus, newNetConfigs);
									} catch (NumberFormatException e) {
										s_logger.error("failed to set new interface number - {}", e);
									}
								}
							}
						}

						if((oldNetConfigs == null) || !isConfigsEqual(oldNetConfigs, newNetConfigs)) {	
							s_logger.info("new configuration for cellular modem on usb port {} netinterface {}", usbPort, ifaceName); 
							m_networkConfig = newNetworkConfig;

							if (pppService != null) {
								PppState pppState = pppService.getPppState();
								if ((pppState == PppState.CONNECTED) || (pppState == PppState.IN_PROGRESS)) {
									s_logger.info("disconnecting " + pppService.getIfaceName());
									pppService.disconnect();
								}
								PppFactory.releasePppService(pppService.getIfaceName());
							}

							if (modem.isGpsEnabled()) {
								if (!disableModemGps(modem)) {
									s_logger.error("processNetworkConfigurationChangeEvent() :: Failed to disable modem GPS");
									modem.reset();
								}
							}

							modem.setConfiguration(newNetConfigs);

							if (modem instanceof EvdoCellularModem) {
								NetInterfaceStatus netIfaceStatus = getNetInterfaceStatus(newNetConfigs);
								if (netIfaceStatus == NetInterfaceStatus.netIPv4StatusEnabledWAN) {

									if (!((EvdoCellularModem) modem).isProvisioned()) {
										s_logger.info("NetworkConfigurationChangeEvent :: The {} is not provisioned, will try to provision it ...", modem.getModel());

										if ((task != null) && !task.isCancelled()) {
											s_logger.info("NetworkConfigurationChangeEvent :: Cancelling monitor task");
											stopThread.set(true);
											monitorNotity();
											task.cancel(true);
											task = null;
										}

										((EvdoCellularModem) modem).provision();
										if (task == null) {
											s_logger.info("NetworkConfigurationChangeEvent :: Restarting monitor task");
											stopThread.set(false);
											task = m_executor.submit(new Runnable() {
												@Override
												public void run() {
													while (!stopThread.get()) {
														Thread.currentThread().setName("ModemMonitor");
														try {
															monitor();
															monitorWait();
														} catch (InterruptedException interruptedException) {
															Thread.interrupted();
															s_logger.debug("modem monitor interrupted - {}", interruptedException);
														} catch (Throwable t) {
															s_logger.error("handleEvent() :: Exception while monitoring cellular connection {}", t);
														}
													}
												}});
										} else {
											monitorNotity();
										}
									} else {
										s_logger.info("NetworkConfigurationChangeEvent :: The " + modem.getModel() + " is provisioned");
									}	
								}

								if (modem.isGpsSupported()) {
									if (isGpsEnabledInConfig(newNetConfigs) && !modem.isGpsEnabled()) {
										modem.enableGps();
										postModemGpsEvent(modem, true);
									}
								}
							}
						}
					}
				} catch (KuraException e) {
					s_logger.error("NetworkConfigurationChangeEvent :: Failed to process - {}", e);
				}
			}
		}
	}

	private boolean isConfigsEqual(List<NetConfig>oldConfig, List<NetConfig> newConfig) {
		if (((oldConfig == null) && (newConfig == null))
				|| ((oldConfig == null) && (newConfig != null))
				|| ((oldConfig != null) && (newConfig == null))) {
			return false;
		}
		boolean ret = false;
		ModemConfig oldModemConfig = getModemConfig(oldConfig);
		ModemConfig newModemConfig = getModemConfig(newConfig);
		NetConfigIP4 oldNetConfigIP4 = getNetConfigIp4(oldConfig);
		NetConfigIP4 newNetConfigIP4 = getNetConfigIp4(newConfig);

		if (oldNetConfigIP4.equals(newNetConfigIP4) && oldModemConfig.equals(newModemConfig)) {
			ret = true;
		}
		return ret;
	}

	private List<NetConfig> getNetConfigs(NetInterfaceConfig<? extends NetInterfaceAddressConfig> netInterfaceConfig) {

		List<NetConfig> netConfigs = null;
		if (netInterfaceConfig != null) {
			List<? extends NetInterfaceAddressConfig> netInterfaceAddressConfigs = netInterfaceConfig.getNetInterfaceAddresses();
			if (netInterfaceAddressConfigs != null && netInterfaceAddressConfigs.size() > 0) {
				for (NetInterfaceAddressConfig netInterfaceAddressConfig : netInterfaceAddressConfigs) {
					netConfigs = netInterfaceAddressConfig.getConfigs();
				}
			}
		}
		return netConfigs;
	}

	private ModemConfig getModemConfig(List<NetConfig> netConfigs) {
		ModemConfig modemConfig = null;
		for (NetConfig netConfig : netConfigs) {
			if (netConfig instanceof ModemConfig) {
				modemConfig = (ModemConfig)netConfig;
			}
		}
		return modemConfig;
	}

	private NetConfigIP4 getNetConfigIp4(List<NetConfig> netConfigs) {
		NetConfigIP4 netConfigIP4 = null;
		for (NetConfig netConfig : netConfigs) {
			if (netConfig instanceof NetConfigIP4) {
				netConfigIP4 = (NetConfigIP4)netConfig;
			}
		}
		return netConfigIP4;
	}

	private int getInterfaceNumber (List<NetConfig> netConfigs) {
		int ifaceNo = -1;
		if ((netConfigs != null) && (netConfigs.size() > 0)) {
			for (NetConfig netConfig : netConfigs) {
				if (netConfig instanceof ModemConfig) {
					ifaceNo = ((ModemConfig) netConfig).getPppNumber();
					break;
				}
			}
		}
		return ifaceNo;
	}

	private int getInterfaceNumber(String ifaceName) {
		return Integer.parseInt(ifaceName.replaceAll("[^0-9]", ""));
	}

	private void setInterfaceNumber (String ifaceName, List<NetConfig> netConfigs) {
		if ((netConfigs != null) && (netConfigs.size() > 0)) {
			for (NetConfig netConfig : netConfigs) {
				if (netConfig instanceof ModemConfig) {
					((ModemConfig) netConfig).setPppNumber(getInterfaceNumber(ifaceName));
					break;
				}
			}
		}
	}

	private long getModemResetTimeoutMsec(String ifaceName, List<NetConfig> netConfigs) {
		long resetToutMsec = 0L;

		if ((ifaceName != null) && (netConfigs != null) && (netConfigs.size() > 0)) {
			for (NetConfig netConfig : netConfigs) {
				if (netConfig instanceof ModemConfig) {
					resetToutMsec = ((ModemConfig) netConfig).getResetTimeout() * 60000;
					break;
				}
			}
		}
		return resetToutMsec;
	}

	private boolean isGpsEnabledInConfig(List<NetConfig> netConfigs) {
		boolean isGpsEnabled = false;
		if ((netConfigs != null) && (netConfigs.size() > 0)) {
			for (NetConfig netConfig : netConfigs) {
				if (netConfig instanceof ModemConfig) {
					isGpsEnabled = ((ModemConfig) netConfig).isGpsEnabled();
					break;
				}
			}
		}
		return isGpsEnabled;
	}

	private void monitor() {
		synchronized (s_lock) {
			HashMap<String, InterfaceState> newInterfaceStatuses = new HashMap<String, InterfaceState>();
			if(m_modems == null || m_modems.isEmpty()){
				return;
			}
			for (Map.Entry<String, CellularModem> modemEntry : m_modems.entrySet()) {
				CellularModem modem = modemEntry.getValue();
				// get signal strength only if somebody needs it
				if ((m_listeners != null) && (!m_listeners.isEmpty())) {
					for (ModemMonitorListener listener : m_listeners) {
						try {
							int rssi = modem.getSignalStrength();
							listener.setCellularSignalLevel(rssi);
						} catch (KuraException e) {
							listener.setCellularSignalLevel(0);
							s_logger.error("monitor() :: Failed to obtain signal strength - {}", e);
						}
					}
				}

				IModemLinkService pppService = null;
				PppState pppState = null;
				NetInterfaceStatus netInterfaceStatus = getNetInterfaceStatus(modem.getConfiguration());
				try {
					String ifaceName = m_networkService.getModemPppPort(modem.getModemDevice());
					if (netInterfaceStatus == NetInterfaceStatus.netIPv4StatusEnabledWAN && ifaceName != null) {
						pppService = PppFactory.obtainPppService(ifaceName, modem.getDataPort());
						pppState = pppService.getPppState();
						if (m_pppState != pppState) {
							s_logger.info("monitor() :: previous PppState={}", m_pppState);
							s_logger.info("monitor() :: current PppState={}", pppState);
						}

						if (pppState == PppState.NOT_CONNECTED) {
							boolean checkIfSimCardReady = false;
							List<ModemTechnologyType> modemTechnologyTypes = modem.getTechnologyTypes();
							for (ModemTechnologyType modemTechnologyType : modemTechnologyTypes) {
								if ((modemTechnologyType == ModemTechnologyType.GSM_GPRS)
										|| (modemTechnologyType == ModemTechnologyType.UMTS)
										|| (modemTechnologyType == ModemTechnologyType.HSDPA)
										|| (modemTechnologyType == ModemTechnologyType.HSPA)) {
									checkIfSimCardReady = true;
									break;
								}
							}
							if (checkIfSimCardReady) {
								if(((HspaCellularModem)modem).isSimCardReady()) {
									s_logger.info("monitor() :: !!! SIM CARD IS READY !!! connecting ...");
									pppService.connect();
									if (m_pppState == PppState.NOT_CONNECTED) {
										m_resetTimerStart = System.currentTimeMillis();
									}
								} else {
									s_logger.warn("monitor() :: ! SIM CARD IS NOT READY !");
								}
							} else {
								s_logger.info("monitor() :: connecting ...");
								pppService.connect();
								if (m_pppState == PppState.NOT_CONNECTED) {
									m_resetTimerStart = System.currentTimeMillis();
								}
							}
						} else if (pppState == PppState.IN_PROGRESS) {
							long modemResetTout = getModemResetTimeoutMsec(ifaceName, modem.getConfiguration());
							if (modemResetTout > 0) {
								long timeElapsed = System.currentTimeMillis() - m_resetTimerStart;
								if (timeElapsed > modemResetTout) {
									// reset modem
									s_logger.info("monitor() :: Modem Reset TIMEOUT !!!");
									pppService.disconnect();
									if (modem.isGpsEnabled() && !disableModemGps(modem)) {
										s_logger.error("monitor() :: Failed to disable modem GPS");
									}
									modem.reset();
									pppState = PppState.NOT_CONNECTED;
								} else {
									int timeTillReset = (int)(modemResetTout - timeElapsed) / 1000;
									s_logger.info("monitor() :: PPP connection in progress. Modem will be reset in {} sec if not connected", timeTillReset);
								}
							}
						} else if (pppState == PppState.CONNECTED) {
							m_resetTimerStart = System.currentTimeMillis();
						}

						m_pppState = pppState;
						ConnectionInfo connInfo = new ConnectionInfoImpl(ifaceName);
						InterfaceState interfaceState = new InterfaceState(ifaceName, 
								LinuxNetworkUtil.isUp(ifaceName), 
								pppState == PppState.CONNECTED, 
								connInfo.getIpAddress());
						newInterfaceStatuses.put(ifaceName, interfaceState);
					}  

					if(modem.isGpsSupported()) {
						if (isGpsEnabledInConfig(modem.getConfiguration())) {
							if (modem instanceof HspaCellularModem) {
								if (!modem.isGpsEnabled()) {
									modem.enableGps();
								}
							}
							postModemGpsEvent(modem, true);
						}
					}
				} catch (Exception e) {
					s_logger.error("monitor() :: Exception", e);
					if ((pppService != null) && (pppState != null)) {
						try {
							s_logger.info("monitor() :: Exception :: PPPD disconnect");
							pppService.disconnect();
						} catch (KuraException e1) {
							s_logger.error("monitor() :: Exception while disconnect", e1);
						}
						m_pppState = pppState;
					}

					if (modem.isGpsEnabled()) {
						try {
							if (!disableModemGps(modem)) {
								s_logger.error("monitor() :: Failed to disable modem GPS");
							}
						} catch (KuraException e1) {
							s_logger.error("monitor() :: Exception disableModemGps", e1);
						}
					}

					try {
						s_logger.info("monitor() :: Exception :: modem reset");
						modem.reset();
					} catch (KuraException e1) {
						s_logger.error("monitor() :: Exception modem.reset", e1);
					}
				}
			}

			// post event for any status changes
			checkStatusChange(m_interfaceStatuses, newInterfaceStatuses);
			m_interfaceStatuses = newInterfaceStatuses;
		}
	}

	private void checkStatusChange(Map<String, InterfaceState> oldStatuses, Map<String, InterfaceState> newStatuses) {

		if (newStatuses != null) {
			// post NetworkStatusChangeEvent on current and new interfaces
			for(String interfaceName : newStatuses.keySet()) {
				if ((oldStatuses != null) && oldStatuses.containsKey(interfaceName)) {
					if (!newStatuses.get(interfaceName).equals(oldStatuses.get(interfaceName))) {
						s_logger.debug("Posting NetworkStatusChangeEvent on interface: {}", interfaceName);
						m_eventAdmin.postEvent(new NetworkStatusChangeEvent(interfaceName, newStatuses.get(interfaceName), null));
					}
				} else {
					s_logger.debug("Posting NetworkStatusChangeEvent on enabled interface: {}", interfaceName);
					m_eventAdmin.postEvent(new NetworkStatusChangeEvent(interfaceName, newStatuses.get(interfaceName), null));
				}
			}

			// post NetworkStatusChangeEvent on interfaces that are no longer there
			if (oldStatuses != null) {
				for(String interfaceName : oldStatuses.keySet()) {
					if(!newStatuses.containsKey(interfaceName)) {
						s_logger.debug("Posting NetworkStatusChangeEvent on disabled interface: {}", interfaceName);
						m_eventAdmin.postEvent(new NetworkStatusChangeEvent(interfaceName, oldStatuses.get(interfaceName), null));
					}
				}
			}
		}
	}

	private void trackModem(ModemDevice modemDevice) {
		Class<? extends CellularModemFactory> modemFactoryClass = null;

		if (modemDevice instanceof UsbModemDevice) {
			SupportedUsbModemInfo supportedUsbModemInfo = SupportedUsbModemsInfo.getModem((UsbModemDevice)modemDevice);
			UsbModemFactoryInfo usbModemFactoryInfo = SupportedUsbModemsFactoryInfo.getModem(supportedUsbModemInfo);
			modemFactoryClass = usbModemFactoryInfo.getModemFactoryClass();
		} else if (modemDevice instanceof SerialModemDevice) {
			SupportedSerialModemInfo supportedSerialModemInfo = SupportedSerialModemsInfo.getModem();
			SerialModemFactoryInfo serialModemFactoryInfo = SupportedSerialModemsFactoryInfo.getModem(supportedSerialModemInfo);
			modemFactoryClass = serialModemFactoryInfo.getModemFactoryClass();
		}

		if (modemFactoryClass != null) {
			CellularModemFactory modemFactoryService = null;
			try {
				try {
					Method getInstanceMethod = modemFactoryClass.getDeclaredMethod("getInstance", (Class<?>[]) null);
					getInstanceMethod.setAccessible(true);
					modemFactoryService = (CellularModemFactory) getInstanceMethod.invoke(null, (Object[]) null);
				} catch (Exception e) {
					s_logger.error("Error calling getInstance() method on " + modemFactoryClass.getName() + e);
				}

				// if unsuccessful in calling getInstance()
				if (modemFactoryService == null) {
					modemFactoryService = (CellularModemFactory) modemFactoryClass.newInstance();
				}

				String platform = null;
				if(m_systemService != null) {
					platform = m_systemService.getPlatform();
				}
				CellularModem modem = modemFactoryService.obtainCellularModemService(modemDevice, platform);
				try {
					HashMap<String, String> modemInfoMap = new HashMap<String, String>();
					modemInfoMap.put(ModemReadyEvent.IMEI, modem.getSerialNumber());
					modemInfoMap.put(ModemReadyEvent.IMSI, modem.getMobileSubscriberIdentity());
					modemInfoMap.put(ModemReadyEvent.ICCID, modem.getIntegratedCirquitCardId());
					modemInfoMap.put(ModemReadyEvent.RSSI, Integer.toString(modem.getSignalStrength()));
					s_logger.info("posting ModemReadyEvent on topic {}", ModemReadyEvent.MODEM_EVENT_READY_TOPIC);
					m_eventAdmin.postEvent(new ModemReadyEvent(modemInfoMap));
				} catch (Exception e) {
					s_logger.error("Failed to post the ModemReadyEvent - {}", e);
				}

				String ifaceName = m_networkService.getModemPppPort(modemDevice);
				List<NetConfig> netConfigs = null;
				if (ifaceName != null) {
					NetInterfaceConfig<? extends NetInterfaceAddressConfig> netInterfaceConfig = m_networkConfig
							.getNetInterfaceConfig(ifaceName);

					if(netInterfaceConfig == null) {
						m_networkConfig = m_netConfigService.getNetworkConfiguration();
						netInterfaceConfig = m_networkConfig.getNetInterfaceConfig(ifaceName);
					}

					if (netInterfaceConfig != null) {
						netConfigs = getNetConfigs(netInterfaceConfig);
						if ((netConfigs != null) && (netConfigs.size() > 0)) {
							modem.setConfiguration(netConfigs);
						}
					}
				}

				if (modemDevice instanceof UsbModemDevice) {
					m_modems.put(((UsbModemDevice)modemDevice).getUsbPort(), modem);
				} else if (modemDevice instanceof SerialModemDevice) {
					m_modems.put(modemDevice.getProductName(), modem);
				}

				if (modem instanceof EvdoCellularModem) {
					NetInterfaceStatus netIfaceStatus = getNetInterfaceStatus(netConfigs);
					if (netIfaceStatus == NetInterfaceStatus.netIPv4StatusEnabledWAN) {
						if (modem.isGpsEnabled()) {
							if (!disableModemGps(modem)) {
								s_logger.error("trackModem() :: Failed to disable modem GPS, resetting modem ...");
								modem.reset();
							}
						}

						if (!((EvdoCellularModem) modem).isProvisioned()) {
							s_logger.info("trackModem() :: The {} is not provisioned, will try to provision it ...", modem.getModel());
							if ((task != null) && !task.isCancelled()) {
								s_logger.info("trackModem() :: Cancelling monitor task");
								stopThread.set(true);
								monitorNotity();
								task.cancel(true);
								task = null;
							}
							((EvdoCellularModem) modem).provision();
							if (task == null) {
								s_logger.info("trackModem() :: Restarting monitor task");
								stopThread.set(false);
								task = m_executor.submit(new Runnable() {
									@Override
									public void run() {
										while (!stopThread.get()) {
											Thread.currentThread().setName("ModemMonitor");
											try {
												monitor();
												monitorWait();
											} catch (InterruptedException interruptedException) {
												Thread.interrupted();
												s_logger.debug("modem monitor interrupted - {}", interruptedException);
											} catch (Throwable t) {
												s_logger.error("trackModem() :: Exception while monitoring cellular connection {}", t);
											}
										}
									}});
							} else {
								monitorNotity();
							}
						} else {
							s_logger.info("trackModem() :: The {} is provisioned", modem.getModel());
						}
					}

					if (modem.isGpsSupported()) {
						if (isGpsEnabledInConfig(netConfigs) && !modem.isGpsEnabled()) {
							modem.enableGps();
							postModemGpsEvent(modem, true);
						}
					}
				}
			} catch (Exception e) {
				s_logger.error("trackModem() :: {}", e);
			}
		}
	}


	private boolean disableModemGps(CellularModem modem) throws KuraException {

		postModemGpsEvent(modem, false);

		boolean portIsReachable = false;
		long startTimer = System.currentTimeMillis();	
		do {
			try {
				Thread.sleep(3000);
				if (modem.isPortReachable(modem.getAtPort())) {
					s_logger.debug("disableModemGps() modem is now reachable ...");
					portIsReachable = true;
					break;
				} else {
					s_logger.debug("disableModemGps() waiting for PositionService to release serial port ...");
				}
			} catch (Exception e) {
				s_logger.debug("disableModemGps() waiting for PositionService to release serial port: ex={}", e);
			}
		} while ((System.currentTimeMillis()-startTimer) < 20000L);

		modem.disableGps();
		try {
			Thread.sleep(1000);
		} catch(InterruptedException e) {}

		boolean ret = false;
		if (portIsReachable && !modem.isGpsEnabled()) {
			s_logger.error("disableModemGps() :: Modem GPS is disabled :: portIsReachable={}, modem.isGpsEnabled()={}",
					portIsReachable, modem.isGpsEnabled());
			ret = true;
		}
		return ret;
	}

	private void postModemGpsEvent(CellularModem modem, boolean enabled) throws KuraException {

		if (enabled) {
			CommURI commUri = modem.getSerialConnectionProperties(CellularModem.SerialPortType.GPSPORT);
			if (commUri != null) {
				s_logger.trace("postModemGpsEvent() :: Modem SeralConnectionProperties: {}", commUri.toString());			

				HashMap<String, Object> modemInfoMap = new HashMap<String, Object>();
				modemInfoMap.put(ModemGpsEnabledEvent.Port, modem.getGpsPort());
				modemInfoMap.put(ModemGpsEnabledEvent.BaudRate, Integer.valueOf(commUri.getBaudRate()));
				modemInfoMap.put(ModemGpsEnabledEvent.DataBits, Integer.valueOf(commUri.getDataBits()));
				modemInfoMap.put(ModemGpsEnabledEvent.StopBits, Integer.valueOf(commUri.getStopBits()));
				modemInfoMap.put(ModemGpsEnabledEvent.Parity, Integer.valueOf(commUri.getParity()));

				s_logger.debug("postModemGpsEvent() :: posting ModemGpsEnabledEvent on topic {}", ModemGpsEnabledEvent.MODEM_EVENT_GPS_ENABLED_TOPIC);
				m_eventAdmin.postEvent(new ModemGpsEnabledEvent(modemInfoMap));
			}
		} else {
			s_logger.debug("postModemGpsEvent() :: posting ModemGpsDisableEvent on topic {}", ModemGpsDisabledEvent.MODEM_EVENT_GPS_DISABLED_TOPIC);
			HashMap<String, Object> modemInfoMap = new HashMap<String, Object>();
			m_eventAdmin.postEvent(new ModemGpsDisabledEvent(modemInfoMap));
		}
	}

	private void monitorNotity() {
		if (stopThread != null) {
			synchronized (stopThread) {
				stopThread.notifyAll();
			}
		}
	}

	private void monitorWait() throws InterruptedException {
		if (stopThread != null) {
			synchronized (stopThread) {
				stopThread.wait(THREAD_INTERVAL);
			}
		}
	}
}
