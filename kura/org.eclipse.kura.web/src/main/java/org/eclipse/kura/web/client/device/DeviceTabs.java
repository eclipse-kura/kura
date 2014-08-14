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
package org.eclipse.kura.web.client.device;

import org.eclipse.kura.web.client.messages.Messages;
import org.eclipse.kura.web.shared.model.GwtSession;

import com.extjs.gxt.ui.client.event.ComponentEvent;
import com.extjs.gxt.ui.client.event.Events;
import com.extjs.gxt.ui.client.event.Listener;
import com.extjs.gxt.ui.client.widget.LayoutContainer;
import com.extjs.gxt.ui.client.widget.TabItem;
import com.extjs.gxt.ui.client.widget.TabPanel;
import com.extjs.gxt.ui.client.widget.layout.FitLayout;
import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.Element;

public class DeviceTabs extends LayoutContainer 
{
	private static final Messages MSGS = GWT.create(Messages.class);

	private GwtSession				m_currentSession;
	
	private TabPanel				m_tabsPanel;
	private TabItem					m_tabProfile;
	private TabItem					m_tabBundles;
	private TabItem					m_tabThreads;
	private TabItem					m_tabSysProps;
	private TabItem					m_tabCommand;
	
	private ProfileTab				m_profileTab;
	private BundlesTab		        m_bundlesTab;
	private ThreadsTab		        m_threadsTab;
	private SystemPropertiesTab		m_sysPropsTab;
	private CommandTab				m_commandTab;
	
	public DeviceTabs(GwtSession currentSession) {	    
		m_currentSession = currentSession;
		m_profileTab  = new ProfileTab(m_currentSession);
		m_bundlesTab  = new BundlesTab(m_currentSession);
		m_threadsTab  = new ThreadsTab(m_currentSession);
		m_sysPropsTab = new SystemPropertiesTab(m_currentSession);
		m_commandTab  = new CommandTab(m_currentSession);
	}


	protected void onRender(Element parent, int index) 
    {        
    	super.onRender(parent, index);
        
        setLayout(new FitLayout());
        setId("device-tabs-wrapper");
        
        m_tabsPanel = new TabPanel();
        m_tabsPanel.setPlain(true);
        m_tabsPanel.setBorders(false);
        m_tabsPanel.setStyleAttribute("padding-top", "5px");
        
        m_tabProfile = new TabItem(MSGS.deviceTabProfile());
        m_tabProfile.setBorders(true);
        m_tabProfile.setLayout(new FitLayout());
        m_tabProfile.add(m_profileTab);
        m_tabProfile.addListener(Events.Select, new Listener<ComponentEvent>() {  
            public void handleEvent(ComponentEvent be) {  
            	m_profileTab.refresh();
            }  
        });
        m_tabsPanel.add(m_tabProfile);

        m_tabBundles = new TabItem(MSGS.deviceTabBundles());
        m_tabBundles.setBorders(true);
        m_tabBundles.setLayout(new FitLayout());
        m_tabBundles.add(m_bundlesTab);
        m_tabBundles.addListener(Events.Select, new Listener<ComponentEvent>() {  
            public void handleEvent(ComponentEvent be) {  
            	m_bundlesTab.refresh();
            }  
        });
        m_tabsPanel.add(m_tabBundles);

        m_tabThreads = new TabItem(MSGS.deviceTabThreads());
        m_tabThreads.setBorders(true);
        m_tabThreads.setLayout(new FitLayout());
        m_tabThreads.add(m_threadsTab);
        m_tabThreads.addListener(Events.Select, new Listener<ComponentEvent>() {  
            public void handleEvent(ComponentEvent be) {  
            	m_threadsTab.refresh();
            }  
        });
        m_tabsPanel.add(m_tabThreads);

        m_tabSysProps = new TabItem(MSGS.deviceTabSystemProperties());
        m_tabSysProps.setBorders(true);
        m_tabSysProps.setLayout(new FitLayout());
        m_tabSysProps.add(m_sysPropsTab);
        m_tabSysProps.addListener(Events.Select, new Listener<ComponentEvent>() {  
            public void handleEvent(ComponentEvent be) {  
            	m_sysPropsTab.refresh();
            }  
        });
        m_tabsPanel.add(m_tabSysProps);
        
        m_tabCommand = new TabItem(MSGS.deviceTabCommand());
        m_tabCommand.setBorders(true);
        m_tabCommand.setLayout(new FitLayout());
        m_tabCommand.add(m_commandTab);
        m_tabCommand.addListener(Events.Select, new Listener<ComponentEvent>() {
        	public void handleEvent(ComponentEvent be) {
        		m_commandTab.refresh();
        	}
        });
        m_tabsPanel.add(m_tabCommand);

        add(m_tabsPanel);
    }
}
