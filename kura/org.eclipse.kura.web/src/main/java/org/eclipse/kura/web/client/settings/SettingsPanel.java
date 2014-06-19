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
import org.eclipse.kura.web.shared.model.GwtSession;

import com.extjs.gxt.ui.client.Style.LayoutRegion;
import com.extjs.gxt.ui.client.util.Margins;
import com.extjs.gxt.ui.client.widget.Label;
import com.extjs.gxt.ui.client.widget.LayoutContainer;
import com.extjs.gxt.ui.client.widget.layout.BorderLayout;
import com.extjs.gxt.ui.client.widget.layout.BorderLayoutData;
import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.Element;

public class SettingsPanel extends LayoutContainer 
{
	private static final Messages MSGS = GWT.create(Messages.class);

	private GwtSession        m_currentSession;
    private SettingsTabs      m_settingsTabs;
    private ServiceTree       m_servicesTree;
    
    public SettingsPanel(GwtSession currentSession,
    					 ServiceTree serviceTree) {
    	m_currentSession = currentSession;
    	m_servicesTree   = serviceTree;
    }
    
    
    protected void onRender(Element parent, int index) 
    {    
        super.onRender(parent, index);

        m_settingsTabs = new SettingsTabs(m_currentSession, m_servicesTree);
       
        BorderLayout borderLayout = new BorderLayout();
        setLayout(borderLayout);
        setBorders(true);
        setId("settings-panel-wrapper");
        
        //
        // north
        BorderLayoutData northData = new BorderLayoutData(LayoutRegion.NORTH, 10);  
        northData.setMargins(new Margins(10, 25, 5, 25));
        Label intro = new Label(MSGS.settingsIntro());
        intro.setId("settings-label");
        add(intro, northData);

        //
        // center
        BorderLayoutData centerData = new BorderLayoutData(LayoutRegion.CENTER);  
        centerData.setMargins(new Margins(10, 15, 25, 15));          
        add(m_settingsTabs, centerData);
    }    
}
