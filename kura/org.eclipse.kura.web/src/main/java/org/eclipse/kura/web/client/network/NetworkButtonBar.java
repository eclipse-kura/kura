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

import org.eclipse.kura.web.Console;
import org.eclipse.kura.web.client.messages.Messages;
import org.eclipse.kura.web.client.resources.Resources;
import org.eclipse.kura.web.client.util.FailureHandler;
import org.eclipse.kura.web.shared.model.GwtNetIfConfigMode;
import org.eclipse.kura.web.shared.model.GwtNetInterfaceConfig;
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
import com.extjs.gxt.ui.client.widget.MessageBox;
import com.extjs.gxt.ui.client.widget.button.Button;
import com.extjs.gxt.ui.client.widget.layout.FitLayout;
import com.extjs.gxt.ui.client.widget.toolbar.SeparatorToolItem;
import com.extjs.gxt.ui.client.widget.toolbar.ToolBar;
import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.Element;
import com.google.gwt.user.client.Timer;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.AbstractImagePrototype;


public class NetworkButtonBar extends LayoutContainer
{
	private static final Messages MSGS = GWT.create(Messages.class);

	private final GwtNetworkServiceAsync gwtNetworkService = GWT.create(GwtNetworkService.class);

	@SuppressWarnings("unused")
    private GwtSession             m_currentSession;
	private NetworkPanel           m_netPanel;
	private NetworkInterfacesTable m_netTable;
	private NetInterfaceConfigTabs m_netTabs;
	
	private ToolBar m_buttonBar;
	private Button    m_applyButton; 
	
	public NetworkButtonBar(GwtSession currentSession,
							NetworkPanel netPanel,
							NetworkInterfacesTable netTable,
						    NetInterfaceConfigTabs netTabs)
	{
        m_currentSession = currentSession;
        m_netPanel       = netPanel; 
        m_netTable       = netTable;
		m_netTabs        = netTabs;
		
		m_netTable.addListener(Events.Change, new Listener<BaseEvent>() {
			public void handleEvent(BaseEvent be) {
				Log.debug("Enabling Button Bar because of " + be.getSource().toString() + " :: " + be.toString());
				if(m_buttonBar == null) {
					Log.debug("ButtonBar is null");
				}
				m_applyButton.enable();
			}
		});
		m_netTabs.addListener(Events.Change, new Listener<BaseEvent>() {
			public void handleEvent(BaseEvent be) {
				Log.debug("Enabling Button Bar because of " + be.getSource().toString() + " :: " + be.toString());
				if(m_buttonBar == null) {
					Log.debug("ButtonBar is null");
				}
				m_applyButton.enable();
			}
		});
	}

    protected void onRender(Element parent, int index) 
    {        
    	super.onRender(parent, index);         
        setLayout(new FitLayout());
        setId("network-buttons");
        
        m_buttonBar = new ToolBar();
        m_buttonBar.setHeight(20);
        m_buttonBar.setAlignment(HorizontalAlignment.LEFT);
        
        m_applyButton = new Button(MSGS.netApply(),
        		AbstractImagePrototype.create(Resources.INSTANCE.accept()),
                new SelectionListener<ButtonEvent>() {
            		@Override
            		public void componentSelected(ButtonEvent ce) {
            			
            			if (m_netTabs.isValid()) {
	            		    GwtNetInterfaceConfig prevNetIf = m_netTable.getSelectedNetInterfaceConfig();
	            		    
	            			Log.debug("about to getUpdatedNetInterface()");
	            			final GwtNetInterfaceConfig updatedNetIf = m_netTabs.getUpdatedNetInterface();
	            			
	            			// Submit updated NetInterfaceConfig and priorities
	            			if(prevNetIf != null && prevNetIf.equals(updatedNetIf)) {
	            				Log.debug("net interface config matches the previous one");
	            				m_netTable.refresh();
	            				m_applyButton.disable();
	            			} else {
	                			String newNetwork = calculateNetwork(updatedNetIf.getIpAddress(), updatedNetIf.getSubnetMask());
	                			String prevNetwork = Window.Location.getHost();
	                			try {
	                				prevNetwork = calculateNetwork(Window.Location.getHost(), updatedNetIf.getSubnetMask());
	                			} catch (Exception e) {
	                				Log.debug("calculateNetwork() failed for ipAddress: " + Window.Location.getHost() + ", and subnet: " + updatedNetIf.getSubnetMask());
	                			}
	                			
	                			if (newNetwork != null) {
	                    			//if a static IP assigned, re-direct to the new location
	                    			if (updatedNetIf.getConfigMode().equals(GwtNetIfConfigMode.netIPv4ConfigModeManual.name()) &&
	                    					newNetwork.equals(prevNetwork) &&
	                    					Window.Location.getHost().equals(prevNetIf.getIpAddress())) {
	                    				Timer t = new Timer () {
	                        				public void run () {
	                        					Log.debug("redirecting to new address: " + updatedNetIf.getIpAddress());
	                							Window.Location.replace("http://" + updatedNetIf.getIpAddress());
	                        				}
	                        			};
	                        			t.schedule(500);
	        						}
	                			}
	                			
	                			m_netPanel.mask(MSGS.applying());
	                			Log.debug("updateNetInterfaceConfigurations()");
	                			gwtNetworkService.updateNetInterfaceConfigurations(updatedNetIf, new AsyncCallback<Void>() {
	    							public void onSuccess(Void result) {
	    							    Log.debug("successfully update net interface config");
	    							    m_netTable.refresh();
	    							    m_applyButton.disable();
	    							    m_netPanel.unmask();
	    							}
	    							public void onFailure(Throwable caught) {
	    								Log.debug("caught: " + caught.toString());
	    								FailureHandler.handle(caught);
	    								m_netPanel.unmask();
	    							}
	    						});
	            			}
            			}
            			else {
            				MessageBox.info(MSGS.information(),
            						MSGS.deviceConfigError(),
            						null);
            			}
            		}
    	});
        Button m_refresh = new Button("Refresh",
        		AbstractImagePrototype.create(Resources.INSTANCE.refresh()),
        		new SelectionListener<ButtonEvent>() {

					@Override
					public void componentSelected(ButtonEvent ce) {
						// TODO Auto-generated method stub
						m_netTable.refresh();
					}
        	
        });
        m_applyButton.setWidth(100);
        m_buttonBar.setBorders(true);
        m_refresh.setWidth(100);
        m_buttonBar.add(m_applyButton);
        m_buttonBar.add(new SeparatorToolItem());
        m_buttonBar.add(m_refresh);
        m_buttonBar.add(new SeparatorToolItem());
        
        m_applyButton.disable();
		//m_buttonBar.disable();
        add(m_buttonBar);
    }
    
    public String calculateNetwork(String ipAddress, String netmask) {
        if(ipAddress == null || ipAddress.isEmpty() || netmask == null || netmask.isEmpty()) {
            return null;
        }
        
        String network = null;
        
        try {
    		int ipAddressValue = 0;
    		int netmaskValue = 0;
    
    		String[] sa = this.splitIp(ipAddress);
    		
    		for (int i = 24, t = 0; i >= 0; i -= 8, t++) {
    			ipAddressValue = ipAddressValue | (Integer.parseInt(sa[t]) << i);
    		}
    
    		sa = this.splitIp(netmask);
    		for (int i = 24, t = 0; i >= 0; i -= 8, t++) {
    			netmaskValue = netmaskValue | (Integer.parseInt(sa[t]) << i);
    		}
    
    		network = dottedQuad(ipAddressValue & netmaskValue);
        } catch (Exception e) {
            Log.warn("Error calculating network for ip address: " + ipAddress + " and netmask: " + netmask, e);
        }
        
        return network;
	}

	private String dottedQuad(int ip) {
		StringBuffer sb = new StringBuffer(15);
		for (int shift = 24; shift > 0; shift -= 8) {
			// process 3 bytes, from high order byte down.
			sb.append(Integer.toString((ip >>> shift) & 0xff));
			sb.append('.');
		}
		sb.append(Integer.toString(ip & 0xff));
		return sb.toString();
	}

	private String[] splitIp(String ip) {

		String sIp = new String(ip);
		String[] ret = new String[4];

		int ind = 0;
		for (int i = 0; i < 3; i++) {
			if ((ind = sIp.indexOf(".")) >= 0) {
				ret[i] = sIp.substring(0, ind);
				sIp = sIp.substring(ind + 1);
				if (i == 2) {
					ret[3] = sIp;
				}
			}
		}
		return ret;
	}
}
