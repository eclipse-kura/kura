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

public class NetworkPanel extends LayoutContainer 
{
	private static final Messages MSGS = GWT.create(Messages.class);

    @SuppressWarnings("unused")
	private boolean                m_initialized;
	private GwtSession             m_currentSession;

	private NetworkInterfacesTable m_netIfTable; 
	private NetInterfaceConfigTabs m_netConfigTabs;
	private NetworkButtonBar       m_netButtons; 
			
    public NetworkPanel(GwtSession currentSession) {
    	m_currentSession  = currentSession;
    	m_initialized     = false;    	

    }
    
    public boolean isDirty() {
    	if (m_netIfTable != null) {
    		return m_netIfTable.isDirty();
    	}
    	else {
    		return false;
    	}
    }
    
    protected void onRender(Element parent, int index) {
        
        super.onRender(parent, index);
        setId("network-panel-wrapper");
        
        m_netConfigTabs = new NetInterfaceConfigTabs(m_currentSession);  
        m_netIfTable    = new NetworkInterfacesTable(m_currentSession, m_netConfigTabs);
        m_netButtons    = new NetworkButtonBar(m_currentSession, this, m_netIfTable, m_netConfigTabs);
        BorderLayout borderLayout = new BorderLayout();
        setLayout(borderLayout);
        setBorders(true);
        //
        // west
        BorderLayoutData westData = new BorderLayoutData(LayoutRegion.WEST);  
        westData.setSplit(false);  
        westData.setCollapsible(false);  
        westData.setMargins(new Margins(25, 5, 5, 25));        
        add(m_netIfTable, westData);

        //
        // north
        BorderLayoutData northData = new BorderLayoutData(LayoutRegion.NORTH, 25);  
        northData.setMargins(new Margins(10, 25, 5, 25));
        Label intro = new Label(MSGS.netIntro());
        intro.setId("network-label");
        add(intro, northData);

        //
        // center
        
        BorderLayout centerBorderLayout = new BorderLayout();
        LayoutContainer centerLayoutContainer = new LayoutContainer(centerBorderLayout);
        centerLayoutContainer.setBorders(true);
        centerLayoutContainer.setId("network-content-wrapper");
        BorderLayoutData blDataNorth = new BorderLayoutData(LayoutRegion.NORTH, 25);
        blDataNorth.setMargins(new Margins(1, 1, 1, 1));
        centerLayoutContainer.add(m_netButtons, blDataNorth);
        BorderLayoutData blDataCenter = new BorderLayoutData(LayoutRegion.CENTER);
        blDataCenter.setMargins(new Margins(1, 1, 1, 1));
        centerLayoutContainer.add(m_netConfigTabs, blDataCenter);
        
        BorderLayoutData centerData = new BorderLayoutData(LayoutRegion.CENTER);  
        centerData.setMargins(new Margins(25, 25, 5, 0));
        //add(m_netButtons, centerData);
        
        add(centerLayoutContainer, centerData);
                
        m_initialized = true;
    }    
}
