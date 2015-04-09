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
package org.eclipse.kura.web.client.network;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.kura.web.client.messages.Messages;
import org.eclipse.kura.web.shared.model.GwtModemInterfaceConfig;
import org.eclipse.kura.web.shared.model.GwtNetIfStatus;
import org.eclipse.kura.web.shared.model.GwtNetIfType;
import org.eclipse.kura.web.shared.model.GwtNetInterfaceConfig;
import org.eclipse.kura.web.shared.model.GwtSession;
import org.eclipse.kura.web.shared.model.GwtWifiNetInterfaceConfig;
import org.eclipse.kura.web.shared.model.GwtWifiWirelessMode;

import com.allen_sauer.gwt.log.client.Log;
import com.extjs.gxt.ui.client.event.BaseEvent;
import com.extjs.gxt.ui.client.event.ComponentEvent;
import com.extjs.gxt.ui.client.event.Events;
import com.extjs.gxt.ui.client.event.Listener;
import com.extjs.gxt.ui.client.widget.LayoutContainer;
import com.extjs.gxt.ui.client.widget.TabItem;
import com.extjs.gxt.ui.client.widget.TabPanel;
import com.extjs.gxt.ui.client.widget.layout.FitLayout;
import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.Element;

public class NetInterfaceConfigTabs extends LayoutContainer 
{
	private static final Messages MSGS = GWT.create(Messages.class);

	private GwtSession             m_currentSession;
	
	private TabPanel               m_tabsPanel;
	private TabItem                m_tabIPv4Config;
	private TabItem                m_tabWirelessConfig;
	private TabItem                m_tabDhcpNatConfig;
	private TabItem                m_tabModemConfig;
	private TabItem                m_tabHardwareConfig;
	
	private GwtNetInterfaceConfig  m_netIfConfig;
	private TcpIpConfigTab         m_tcpIpConfigTab;
	private WirelessConfigTab      m_wirelessConfigTab;
	private DhcpNatConfigTab       m_dhcpNatConfigTab;
	private ModemConfigTab         m_modemConfigTab;
	private HardwareConfigTab      m_hwConfigTab;
	
	private Object				   m_tabsPanelLock = new Object();
	
	public NetInterfaceConfigTabs(GwtSession currentSession) {
	    
		m_currentSession   = currentSession;
	    
	    initTabs();
	}

	
    private void initTabs()
    {    	
	    final NetInterfaceConfigTabs theTabs = this;

	    // TCP/IP
	    if (m_tcpIpConfigTab != null) {
    		m_tcpIpConfigTab.removeFromParent();
    	}
	    m_tcpIpConfigTab = new TcpIpConfigTab(m_currentSession, this);
	    m_tcpIpConfigTab.addListener(Events.Change, new Listener<BaseEvent>() {
			public void handleEvent(BaseEvent be) {
				theTabs.fireEvent(be.getType());
			}
		});
		if (m_tabIPv4Config != null) {
			m_tabIPv4Config.add(m_tcpIpConfigTab);
			m_tabIPv4Config.layout();
		}
	    
		// Wireless
	    if (m_wirelessConfigTab != null) {
	    	m_wirelessConfigTab.removeFromParent();
    	}
	    m_wirelessConfigTab = new WirelessConfigTab(m_currentSession, m_tcpIpConfigTab, this);
	    m_wirelessConfigTab.addListener(Events.Change, new Listener<BaseEvent>() {
			public void handleEvent(BaseEvent be) {
				theTabs.fireEvent(be.getType());
			}
		});
		if (m_tabWirelessConfig != null) {
			m_tabWirelessConfig.add(m_wirelessConfigTab);
			m_tabWirelessConfig.layout();
		}

		// Modem
	    if (m_modemConfigTab != null) {
	    	m_modemConfigTab.removeFromParent();
    	}
	    m_modemConfigTab = new ModemConfigTab(m_currentSession);
	    m_modemConfigTab.addListener(Events.Change, new Listener<BaseEvent>() {
			public void handleEvent(BaseEvent be) {
				theTabs.fireEvent(be.getType());
			}
		});
		if (m_tabModemConfig != null) {
			m_tabModemConfig.add(m_modemConfigTab);
			m_tabModemConfig.layout();
		}
		
		// DHCP/NAT
	    if (m_dhcpNatConfigTab != null) {
	    	m_dhcpNatConfigTab.removeFromParent();
    	}
	    m_dhcpNatConfigTab = new DhcpNatConfigTab(m_currentSession, m_tcpIpConfigTab, m_wirelessConfigTab);
	    m_dhcpNatConfigTab.addListener(Events.Change, new Listener<BaseEvent>() {
			public void handleEvent(BaseEvent be) {
				theTabs.fireEvent(be.getType());
			}
		});
		if (m_tabDhcpNatConfig != null) {
			m_tabDhcpNatConfig.add(m_dhcpNatConfigTab);
			m_tabDhcpNatConfig.layout();			
		}
		
		// Hardware
	    if (m_hwConfigTab != null) {
	    	m_hwConfigTab.removeFromParent();
    	}
	    m_hwConfigTab = new HardwareConfigTab(m_currentSession);
	    m_hwConfigTab.addListener(Events.Change, new Listener<BaseEvent>() {
			public void handleEvent(BaseEvent be) {
				theTabs.fireEvent(be.getType());
			}
		});    	
		if (m_tabHardwareConfig != null) {
			m_tabHardwareConfig.add(m_hwConfigTab);
			m_tabHardwareConfig.layout();			
		}
    }

	
    public void setNetInterface(GwtNetInterfaceConfig netIfConfig) {
   
    	Log.debug("Setting the netInterface " + netIfConfig.getName());
    	    	
    	m_netIfConfig = netIfConfig;
    	
    	// refresh the tabs so the dirty state is cleaned up
    	initTabs();
    			
     	m_tcpIpConfigTab.setNetInterface(netIfConfig);     	
    	m_dhcpNatConfigTab.setNetInterface(netIfConfig);
    	m_hwConfigTab.setNetInterface(netIfConfig);
    	
		m_wirelessConfigTab.setNetInterface(netIfConfig);
		m_modemConfigTab.setNetInterface(netIfConfig);
    	
    	// set the tabs for this interface
    	removeInterfaceTabs();    	
    	if(!GwtNetIfStatus.netIPv4StatusDisabled.equals(netIfConfig.getStatusEnum())) {
    	    adjustInterfaceTabs();
    	}
    	
    	// Refresh all visible tabs
    	List<TabItem> visibleTabs = m_tabsPanel.getItems();
        if (visibleTabs.contains(m_tabIPv4Config)) {
            m_tcpIpConfigTab.refresh();
        }
        if (visibleTabs.contains(m_tabWirelessConfig)) {
            m_wirelessConfigTab.refresh();
        }
        if (visibleTabs.contains(m_tabModemConfig)) {
            m_modemConfigTab.refresh();
        }
        if (visibleTabs.contains(m_tabDhcpNatConfig)) {
            m_dhcpNatConfigTab.refresh();
        }
        if (visibleTabs.contains(m_tabHardwareConfig)) {
            m_hwConfigTab.refresh();
        }
    }
    
    
    public GwtNetInterfaceConfig getUpdatedNetInterface() 
    {
    	Log.debug("getting updatedNetInterface");
    	
    	GwtNetInterfaceConfig updatedNetIf = null;
    	if (m_netIfConfig instanceof GwtWifiNetInterfaceConfig) {
    		Log.debug("Creating GwtWifiNetInterfaceConfig");
    		updatedNetIf = new GwtWifiNetInterfaceConfig();
    	} else if (m_netIfConfig instanceof GwtModemInterfaceConfig) {
    		Log.debug("Creating GwtModemInterfaceConfig");
    		updatedNetIf = new GwtModemInterfaceConfig();
    	} else {
    		Log.debug("Creating GwtNetInterfaceConfig");
    		updatedNetIf = new GwtNetInterfaceConfig();
    	}
    	
    	// Copy the previous values
    	updatedNetIf.setProperties(m_netIfConfig.getProperties());

        // Get updated values from visible tabs
    	Log.debug("Setting updated values");
        List<TabItem> tabItems = new ArrayList<TabItem>(m_tabsPanel.getItems());
        if (tabItems.contains(m_tabIPv4Config)) {
            m_tcpIpConfigTab.getUpdatedNetInterface(updatedNetIf);
        }
        if (tabItems.contains(m_tabWirelessConfig)) {
            m_wirelessConfigTab.getUpdatedNetInterface(updatedNetIf);
        }
        if (tabItems.contains(m_tabModemConfig)) {
            m_modemConfigTab.getUpdatedNetInterface(updatedNetIf);
        }
        if (tabItems.contains(m_tabDhcpNatConfig)) {
            m_dhcpNatConfigTab.getUpdatedNetInterface(updatedNetIf);
        }
        if (tabItems.contains(m_tabHardwareConfig)) {
            m_hwConfigTab.getUpdatedNetInterface(updatedNetIf);
        }
    	
    	return updatedNetIf;
    }
    
    public void removeAllInterfaceTabs() {
		synchronized (m_tabsPanelLock) {
			for (TabItem tabItem : new ArrayList<TabItem>(m_tabsPanel.getItems())) {
					removeTab(tabItem);
			}
		}
    }
    
	public void removeInterfaceTabs() {
		synchronized (m_tabsPanelLock) {
			for (TabItem tabItem : new ArrayList<TabItem>(
					m_tabsPanel.getItems())) {
				if (tabItem == m_tabWirelessConfig
						|| tabItem == m_tabModemConfig
						|| tabItem == m_tabDhcpNatConfig) {
					removeTab(tabItem);
				}
			}
		}
	}
	
	public void disableInterfaceTabs() {
		synchronized (m_tabsPanelLock) {
			for (TabItem tabItem : new ArrayList<TabItem>(
					m_tabsPanel.getItems())) {
				if (tabItem == m_tabWirelessConfig
						|| tabItem == m_tabModemConfig
						|| tabItem == m_tabDhcpNatConfig) {
					tabItem.disable();
				}
			}
		}
	}
    
    public void removeDhcpNatTab() {
    	synchronized(m_tabsPanelLock) {
	        for (TabItem tabItem : m_tabsPanel.getItems()) {
	            if (tabItem == m_tabDhcpNatConfig) {
	            	m_dhcpNatConfigTab.disableDhcpNat();
	                m_tabsPanel.remove(tabItem);
	                break;
	            }
	        }
    	}
    }
    
    /*
    public void addInterfaceTabs() {
        removeInterfaceTabs();
        
        if(m_netIfConfig != null) {
            if ((m_netIfConfig instanceof GwtNetInterfaceConfig)
                    && m_netIfConfig.getHwTypeEnum() != GwtNetIfType.LOOPBACK 
                    && !m_netIfConfig.getName().startsWith("mon.wlan")) {
                m_tabsPanel.insert(m_tabDhcpNatConfig, 1);
            }
            if (m_netIfConfig instanceof GwtWifiNetInterfaceConfig) {
                if (!m_netIfConfig.getName().startsWith("mon.wlan")) {
                    m_wirelessConfigTab.setNetInterface(m_netIfConfig);
                    m_tabsPanel.insert(m_tabWirelessConfig, 1);
                    m_tabsPanel.insert(m_tabDhcpNatConfig, 2);
                }
            }
            else if (m_netIfConfig instanceof GwtModemInterfaceConfig) {
                m_modemConfigTab.setNetInterface(m_netIfConfig);
                m_tabsPanel.insert(m_tabModemConfig, 1);
                m_tabsPanel.remove(m_tabDhcpNatConfig);
            }
        } 
    }
    */
    
    // Add/remove tabs based on the selected settings in the various tabs
    public void adjustInterfaceTabs() {
    	GwtNetIfStatus netIfStatus = m_tcpIpConfigTab.getStatus();
    	
		boolean includeDhcpNatTab = !m_tcpIpConfigTab.isDhcp() && netIfStatus.equals(GwtNetIfStatus.netIPv4StatusEnabledLAN);
		
		Log.debug("includeDhcpNatTab? " + includeDhcpNatTab);
		
		if(m_netIfConfig instanceof GwtWifiNetInterfaceConfig) {
			Log.debug("insert wifi tab");
			removeTab(m_tabModemConfig);
			insertTab(m_tabWirelessConfig, 1);
			if (!m_tabWirelessConfig.isEnabled()) {
				m_tabWirelessConfig.enable();
			}
			insertTab(m_tabDhcpNatConfig, 2);
			
			// remove dhcp/nat tab if not an access point
			if(!GwtWifiWirelessMode.netWifiWirelessModeAccessPoint.equals(m_wirelessConfigTab.getWirelessMode())) {
				includeDhcpNatTab = false;
			}			
		} else if(m_netIfConfig instanceof GwtModemInterfaceConfig) {
			includeDhcpNatTab = false;
			removeTab(m_tabWirelessConfig);
			removeTab(m_tabDhcpNatConfig);

			Log.debug("insert modem tab");
			insertTab(m_tabModemConfig, 1);
			if (!m_tabModemConfig.isEnabled()) {
				m_tabModemConfig.enable();
			}
		} else {
			removeTab(m_tabWirelessConfig);
			removeTab(m_tabModemConfig);
			
			if(m_netIfConfig.getHwTypeEnum() == GwtNetIfType.LOOPBACK || m_netIfConfig.getName().startsWith("mon.wlan")) {
				removeTab(m_tabDhcpNatConfig);
			}
			else {
				insertTab(m_tabDhcpNatConfig, 1);
			}
		}
		    		    		
		if(includeDhcpNatTab) {
			Log.debug("enable dhcp/nat tab");
			m_tabDhcpNatConfig.enable();
		} else {
			Log.debug("disable dhcp/nat tab");
			m_tabDhcpNatConfig.disable();
		}
		
		if(netIfStatus.equals(GwtNetIfStatus.netIPv4StatusDisabled)) {
    		Log.debug("Disabled - remove tabs");
    		disableInterfaceTabs();
    	} 

    }
    
    public boolean isValid() {
    	List<TabItem> tabItems = m_tabsPanel.getItems();
    	
    	if (tabItems.contains(m_tabIPv4Config) && !m_tcpIpConfigTab.isValid()) {
    		return false;
    	}
    	if (tabItems.contains(m_tabWirelessConfig) && !m_wirelessConfigTab.isValid()) {
    		return false;
    	}
    	if (tabItems.contains(m_tabModemConfig) && !m_modemConfigTab.isValid()) {
    		return false;
    	}
    	if (tabItems.contains(m_tabDhcpNatConfig) && m_tabDhcpNatConfig.isEnabled() && !m_dhcpNatConfigTab.isValid()) {
    		return false;
    	}
    	if (tabItems.contains(m_tabHardwareConfig) && !m_hwConfigTab.isValid()) {
    		return false;
    	}
    	return true;
    }
    
    public boolean isDirty() {
    	
        List<TabItem> tabItems = m_tabsPanel.getItems();
        
    	if (tabItems.contains(m_tabIPv4Config) && m_tcpIpConfigTab.isDirty()) {
    		Log.debug("m_tcpIpConfigTab is dirty");
    		return true;
    	}
    	if (tabItems.contains(m_tabWirelessConfig) && m_wirelessConfigTab.isDirty()) {
    		Log.debug("m_wirelessConfigTab is dirty");
    		return true;
    	}
    	if (tabItems.contains(m_tabModemConfig) && m_modemConfigTab.isDirty()) {
    		Log.debug("m_modemConfigTab is dirty");
    		return true;
    	}
    	if (tabItems.contains(m_tabDhcpNatConfig) && m_tabDhcpNatConfig.isEnabled() && m_dhcpNatConfigTab.isDirty()) {
    		Log.debug("m_dhcpNatConfigTab is dirty");
    		return true;
    	}
    	if (tabItems.contains(m_tabHardwareConfig) && m_hwConfigTab.isDirty()) {
    		Log.debug("m_hwConfigTab is dirty");
    		return true;
    	}
    	
    	return false;
    }
    
    
    protected void onRender(Element parent, int index) 
    {        
    	super.onRender(parent, index);
        
    	setId("network-tabs-wrapper");
        setLayout(new FitLayout());
        
        synchronized(m_tabsPanelLock) {
	        m_tabsPanel = new TabPanel();
	        m_tabsPanel.setPlain(true);
	        m_tabsPanel.setBorders(false);
	        m_tabsPanel.setStyleAttribute("padding-top", "5px");
	        
	        m_tabIPv4Config = new TabItem(MSGS.netIPv4());
	        m_tabIPv4Config.setBorders(true);
	        m_tabIPv4Config.setLayout(new FitLayout());
	        m_tabIPv4Config.add(m_tcpIpConfigTab);
	        m_tabIPv4Config.addListener(Events.Select, new Listener<ComponentEvent>() {  
	            public void handleEvent(ComponentEvent be) {  
	            	m_tcpIpConfigTab.refresh();
	            }  
	        });
	        m_tabsPanel.add(m_tabIPv4Config);
	        
	        m_tabWirelessConfig = new TabItem(MSGS.netWifiWireless());
	        m_tabWirelessConfig.setBorders(true);
	        m_tabWirelessConfig.setLayout(new FitLayout());
	        m_tabWirelessConfig.add(m_wirelessConfigTab);
	        m_tabWirelessConfig.addListener(Events.Select, new Listener<ComponentEvent>() {  
	            public void handleEvent(ComponentEvent be) { 
	            	m_wirelessConfigTab.refresh();
	            }  
	        });
	        m_tabsPanel.add(m_tabWirelessConfig);
	        
	        m_tabModemConfig = new TabItem(MSGS.netModemCellular());
	        m_tabModemConfig.setBorders(true);
	        m_tabModemConfig.setLayout(new FitLayout());
	        m_tabModemConfig.add(m_modemConfigTab);
	        m_tabModemConfig.addListener(Events.Select, new Listener<ComponentEvent>() {  
	            public void handleEvent(ComponentEvent be) { 
	            	m_modemConfigTab.refresh();
	            }  
	        });
	        m_tabsPanel.add(m_tabModemConfig);
	
	        m_tabDhcpNatConfig = new TabItem(MSGS.netRouter());
	        m_tabDhcpNatConfig.setBorders(true);
	        m_tabDhcpNatConfig.setLayout(new FitLayout());
	        m_tabDhcpNatConfig.add(m_dhcpNatConfigTab);
	        m_tabDhcpNatConfig.addListener(Events.Select, new Listener<ComponentEvent>() {  
	            public void handleEvent(ComponentEvent be) {  
	            	m_dhcpNatConfigTab.refresh();
	            }  
	        });
	        m_tabsPanel.add(m_tabDhcpNatConfig);
	        	        
	        m_tabHardwareConfig = new TabItem(MSGS.netHwHardware());
	        m_tabHardwareConfig.setBorders(true);
	        m_tabHardwareConfig.setLayout(new FitLayout());
	        m_tabHardwareConfig.add(m_hwConfigTab);
	        m_tabHardwareConfig.addListener(Events.Select, new Listener<ComponentEvent>() {  
	            public void handleEvent(ComponentEvent be) {  
	            	m_hwConfigTab.refresh();
	            }  
	        });
	        m_tabsPanel.add(m_tabHardwareConfig);
	        
	        add(m_tabsPanel);
        }
    }
    
    private void insertTab(TabItem tabItem, int index) {
    	synchronized(m_tabsPanelLock) {
	    	if(!containsTab(tabItem)) {
	    		m_tabsPanel.insert(tabItem, index);
	    	}
    	}
    }
    
    private void removeTab(TabItem tabItem) {
    	synchronized(m_tabsPanelLock) {
	    	if(containsTab(tabItem)) {
	    		m_tabsPanel.remove(tabItem);
	    	}
    	}
    }
    
    private boolean containsTab(TabItem tabItem) {
        for (TabItem item : m_tabsPanel.getItems()) {
            if (item == tabItem) {
            	return true;
            }
        }
        return false;
    }
}
