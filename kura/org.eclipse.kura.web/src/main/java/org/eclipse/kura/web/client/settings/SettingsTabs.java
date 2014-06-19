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
package org.eclipse.kura.web.client.settings;

import org.eclipse.kura.web.client.configuration.ServiceTree;
import org.eclipse.kura.web.client.messages.Messages;
import org.eclipse.kura.web.client.resources.Resources;
import org.eclipse.kura.web.shared.model.GwtSession;

import com.allen_sauer.gwt.log.client.Log;
import com.extjs.gxt.ui.client.widget.LayoutContainer;
import com.extjs.gxt.ui.client.widget.TabItem;
import com.extjs.gxt.ui.client.widget.TabPanel;
import com.extjs.gxt.ui.client.widget.layout.FitLayout;
import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.Element;
import com.google.gwt.user.client.ui.AbstractImagePrototype;

public class SettingsTabs extends LayoutContainer 
{
	private static final Messages MSGS = GWT.create(Messages.class);
	
	private GwtSession          m_currentSession;
    private ServiceTree         m_servicesTree;
	
	private TabPanel            m_tabsPanel;
	private TabItem             m_adminPasswordConfig;
	private TabItem             m_snapshotsConfig;
	
	private AdminPasswordTab	m_adminPasswordTab;
	private SnapshotsTab	    m_snapshotsTab;
	
	public SettingsTabs(GwtSession currentSession,
					    ServiceTree serviceTree) 
	{    
		m_currentSession = currentSession;
    	m_servicesTree   = serviceTree;
		
		Log.debug("about to get the firewall configuration");
	    initTabs();
	}

	
    private void initTabs()
    {
    	m_adminPasswordTab = new AdminPasswordTab(m_currentSession);
		if (m_adminPasswordConfig != null) {
			m_adminPasswordConfig.add(m_adminPasswordTab);
			m_adminPasswordConfig.layout();
		}

    	m_snapshotsTab = new SnapshotsTab(m_currentSession, m_servicesTree);
		if (m_snapshotsConfig != null) {
			m_snapshotsConfig.add(m_snapshotsTab);
			m_snapshotsConfig.layout();
		}
    }
    
    public boolean isDirty() {
    	
    	if (m_adminPasswordTab.isDirty()) {
    		return true;
    	}
    	
    	return false;
    }
    
    protected void onRender(Element parent, int index) 
    {        
    	super.onRender(parent, index);
        
    	setId("settings-tabs-wrapper");
        setLayout(new FitLayout());
      
        m_tabsPanel = new TabPanel();
        m_tabsPanel.setPlain(true);
        m_tabsPanel.setBorders(false);
        m_tabsPanel.setBodyBorder(false);
        m_tabsPanel.setStyleAttribute("padding-top", "5px");
                
        m_snapshotsConfig = new TabItem(MSGS.settingsSnapshots());
        m_snapshotsConfig.setIcon(AbstractImagePrototype.create(Resources.INSTANCE.snapshots()));
        m_snapshotsConfig.setBorders(true);
        m_snapshotsConfig.setLayout(new FitLayout());
        m_snapshotsConfig.add(m_snapshotsTab);
        m_tabsPanel.add(m_snapshotsConfig);

        m_adminPasswordConfig = new TabItem(MSGS.settingsAdminPassword());
        m_adminPasswordConfig.setBorders(true);
        m_adminPasswordConfig.setLayout(new FitLayout());
        m_adminPasswordConfig.add(m_adminPasswordTab);
        m_tabsPanel.add(m_adminPasswordConfig);

        add(m_tabsPanel);
    }
}
