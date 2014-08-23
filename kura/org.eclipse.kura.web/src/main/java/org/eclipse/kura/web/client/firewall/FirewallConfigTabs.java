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
package org.eclipse.kura.web.client.firewall;

import java.util.List;

import org.eclipse.kura.web.client.messages.Messages;
import org.eclipse.kura.web.shared.model.GwtFirewallNatEntry;
import org.eclipse.kura.web.shared.model.GwtFirewallOpenPortEntry;
import org.eclipse.kura.web.shared.model.GwtFirewallPortForwardEntry;
import org.eclipse.kura.web.shared.model.GwtSession;

import com.allen_sauer.gwt.log.client.Log;
import com.extjs.gxt.ui.client.event.Events;
import com.extjs.gxt.ui.client.event.Listener;
import com.extjs.gxt.ui.client.event.MessageBoxEvent;
import com.extjs.gxt.ui.client.event.TabPanelEvent;
import com.extjs.gxt.ui.client.widget.Dialog;
import com.extjs.gxt.ui.client.widget.LayoutContainer;
import com.extjs.gxt.ui.client.widget.MessageBox;
import com.extjs.gxt.ui.client.widget.TabItem;
import com.extjs.gxt.ui.client.widget.TabPanel;
import com.extjs.gxt.ui.client.widget.layout.FitLayout;
import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.Element;

public class FirewallConfigTabs extends LayoutContainer 
{
	private static final Messages MSGS = GWT.create(Messages.class);
	
	private GwtSession             m_currentSession;
	
	private TabPanel               m_tabsPanel;
	private TabItem                m_tabOpenPortsConfig;
	private TabItem                m_tabPortForwardingConfig;
	private TabItem				   m_tabNatConfig;
	
	private OpenPortsConfigTab	    m_openPortsConfigTab;
	private PortForwardingConfigTab m_portForwardingConfigTab;
	private NatConfigTab			m_natConfigTab;
	
	private Boolean					m_ignoreEvent;
	
	public FirewallConfigTabs(GwtSession currentSession) {
		m_currentSession   = currentSession;
		m_ignoreEvent = false;
		
		Log.debug("about to get the firewall configuration");
		initTabs();
	    
	}
	
    private void initTabs()
    {
	    m_openPortsConfigTab = new OpenPortsConfigTab(m_currentSession);
		if (m_tabOpenPortsConfig != null) {
			m_tabOpenPortsConfig.add(m_openPortsConfigTab);
			m_tabOpenPortsConfig.layout();
		}

		m_portForwardingConfigTab = new PortForwardingConfigTab(m_currentSession);
		if (m_tabPortForwardingConfig != null) {
			m_tabPortForwardingConfig.add(m_portForwardingConfigTab);
			m_tabPortForwardingConfig.layout();
		}
		
		m_natConfigTab = new NatConfigTab(m_currentSession);
		if (m_tabNatConfig != null) {
			m_tabNatConfig.add(m_natConfigTab);
			m_tabNatConfig.layout();
		}
    }
    
    public boolean isDirty() {
    	
    	if (m_openPortsConfigTab.isDirty()) {
    		return true;
    	}
    	if (m_portForwardingConfigTab.isDirty()) {
    		return true;
    	}
    	if (m_natConfigTab.isDirty()) {
    		return true;
    	}
    	
    	return false;
    }
    
    public List<GwtFirewallOpenPortEntry> getUpdatedOpenPortConfiguration() {
    	return m_openPortsConfigTab.getCurrentConfigurations();
    }
    
    public List<GwtFirewallPortForwardEntry> getUpdatedPortForwardConfiguration() {
    	return m_portForwardingConfigTab.getCurrentConfigurations();
    }
    
    public List<GwtFirewallNatEntry> getUpdatedNatConfiguration() {
    	return m_natConfigTab.getCurrentConfigurations();
    }
    
    private void handleTabChangeEvent(boolean isDirty, final TabPanelEvent be) {
    	if (m_ignoreEvent) {
			m_ignoreEvent = false;
			return;
		}
		if (isDirty) {
			be.setCancelled(true);
			MessageBox.confirm(MSGS.confirm(), 
	            	MSGS.deviceConfigDirty(),
	                new Listener<MessageBoxEvent>() {  
	                    public void handleEvent(MessageBoxEvent ce) {
	                        // if confirmed, delete
	                        Dialog  dialog = ce.getDialog(); 
	                        if (dialog.yesText.equals(ce.getButtonClicked().getText())) {
	                        	m_ignoreEvent = true;
	                        	m_tabsPanel.setSelection(be.getItem());
	                        }
	                    }
	        });
		}
    }
    
    protected void onRender(Element parent, int index) {        
    	super.onRender(parent, index);
        
    	setId("firewall-tabs-wrapper");
        setLayout(new FitLayout());
      
        m_tabsPanel = new TabPanel();
        m_tabsPanel.setPlain(true);
        m_tabsPanel.setBorders(false);
        m_tabsPanel.setStyleAttribute("padding-top", "5px");
        
        m_tabOpenPortsConfig = new TabItem(MSGS.firewallOpenPorts());
        m_tabOpenPortsConfig.setBorders(true);
        m_tabOpenPortsConfig.setLayout(new FitLayout());
        m_tabOpenPortsConfig.add(m_openPortsConfigTab);
        m_tabOpenPortsConfig.addListener(Events.BeforeSelect, new Listener<TabPanelEvent>() {
			public void handleEvent(final TabPanelEvent be) {
				handleTabChangeEvent(m_openPortsConfigTab.isDirty(), be);
			}
        });
        m_tabsPanel.add(m_tabOpenPortsConfig);
        
        m_tabPortForwardingConfig = new TabItem(MSGS.firewallPortForwarding());
        m_tabPortForwardingConfig.setBorders(true);
        m_tabPortForwardingConfig.setLayout(new FitLayout());
        m_tabPortForwardingConfig.add(m_portForwardingConfigTab);
        m_tabPortForwardingConfig.addListener(Events.BeforeSelect, new Listener<TabPanelEvent>() {
			public void handleEvent(final TabPanelEvent be) {
				handleTabChangeEvent(m_portForwardingConfigTab.isDirty(), be);
			}
        });
        m_tabsPanel.add(m_tabPortForwardingConfig);
        
        m_tabNatConfig = new TabItem(MSGS.firewallNat());
        m_tabNatConfig.setBorders(true);
        m_tabNatConfig.setLayout(new FitLayout());
        m_tabNatConfig.add(m_natConfigTab);
        m_tabNatConfig.addListener(Events.BeforeSelect, new Listener<TabPanelEvent>() {
			public void handleEvent(final TabPanelEvent be) {
				handleTabChangeEvent(m_natConfigTab.isDirty(), be);
			}
        });
        m_tabsPanel.add(m_tabNatConfig);
        
        add(m_tabsPanel);
    }
}
