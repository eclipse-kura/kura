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
import org.eclipse.kura.web.client.resources.Resources;
import org.eclipse.kura.web.client.util.FailureHandler;
import org.eclipse.kura.web.shared.model.GwtFirewallOpenPortEntry;
import org.eclipse.kura.web.shared.model.GwtFirewallPortForwardEntry;
import org.eclipse.kura.web.shared.model.GwtSession;
import org.eclipse.kura.web.shared.service.GwtNetworkService;
import org.eclipse.kura.web.shared.service.GwtNetworkServiceAsync;

import com.allen_sauer.gwt.log.client.Log;
import com.extjs.gxt.ui.client.Style.HorizontalAlignment;
import com.extjs.gxt.ui.client.event.BaseEvent;
import com.extjs.gxt.ui.client.event.ButtonEvent;
import com.extjs.gxt.ui.client.event.Events;
import com.extjs.gxt.ui.client.event.Listener;
import com.extjs.gxt.ui.client.event.SelectionListener;
import com.extjs.gxt.ui.client.widget.LayoutContainer;
import com.extjs.gxt.ui.client.widget.button.Button;
import com.extjs.gxt.ui.client.widget.button.ButtonBar;
import com.extjs.gxt.ui.client.widget.layout.FitLayout;
import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.Element;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.AbstractImagePrototype;

public class FirewallButtonBar extends LayoutContainer
{
	private static final Messages MSGS = GWT.create(Messages.class);

	private final GwtNetworkServiceAsync gwtNetworkService = GWT.create(GwtNetworkService.class);

	@SuppressWarnings("unused")
    private GwtSession          m_currentSession;
	private FirewallConfigTabs	m_firewallTabs;
	
	private ButtonBar m_buttonBar;
	private Button    m_applyButton; 
	
	public FirewallButtonBar(GwtSession currentSession,
						    FirewallConfigTabs firewallTabs)
	{
        m_currentSession = currentSession;
        m_firewallTabs   = firewallTabs;
		

		m_firewallTabs.addListener(Events.Change, new Listener<BaseEvent>() {
			public void handleEvent(BaseEvent be) {
				if(m_firewallTabs.isDirty()) {
					m_buttonBar.enable();
				} else {
					m_buttonBar.disable();
				}
			}
		});
	}
	
    protected void onRender(Element parent, int index) 
    {        
    	super.onRender(parent, index);         
        setLayout(new FitLayout());
        
        m_buttonBar = new ButtonBar();
        m_buttonBar.setHeight(20);
        m_buttonBar.setAlignment(HorizontalAlignment.CENTER);
        
        m_applyButton = new Button(MSGS.firewallApply(),
        		AbstractImagePrototype.create(Resources.INSTANCE.accept()),
                new SelectionListener<ButtonEvent>() {
            		@Override
            		public void componentSelected(ButtonEvent ce) {
            			Log.debug("about to updateDeviceFirewallOpenPorts() and updateDeviceFirewallPortForwards()");
            			List<GwtFirewallOpenPortEntry> updatedOpenPortConf = m_firewallTabs.getUpdatedOpenPortConfiguration();
            			List<GwtFirewallPortForwardEntry> updatedPortForwardConf = m_firewallTabs.getUpdatedPortForwardConfiguration();

            			if(updatedOpenPortConf != null) {
            				Log.debug("got updatedOpenPortConf: " + updatedOpenPortConf.size());
	            			gwtNetworkService.updateDeviceFirewallOpenPorts(updatedOpenPortConf, new AsyncCallback<Void>() {
	            				public void onSuccess(Void result) {
									Log.debug("updated!");
								}
	            				
								public void onFailure(Throwable caught) {
									Log.debug("caught: " + caught.toString());
									FailureHandler.handle(caught);
								}
	            			});
            			}
            			
            			if(updatedPortForwardConf != null) {
            				Log.debug("got updatedPortForwardConf: " + updatedPortForwardConf.size());
	            			gwtNetworkService.updateDeviceFirewallPortForwards(updatedPortForwardConf, new AsyncCallback<Void>() {
	            				public void onSuccess(Void result) {
									Log.debug("updated!");
								}
	            				
								public void onFailure(Throwable caught) {
									Log.debug("caught: " + caught.toString());
									FailureHandler.handle(caught);
								}
	            			});
            			}
            		}
    	});
        //m_applyButton.setWidth(100);
        m_buttonBar.add(m_applyButton);
        
		m_buttonBar.enable();
        add(m_applyButton);
    }	
}
