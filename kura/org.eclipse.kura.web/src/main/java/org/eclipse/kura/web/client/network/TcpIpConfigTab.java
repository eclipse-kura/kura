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

import java.util.List;

import org.eclipse.kura.web.client.messages.Messages;
import org.eclipse.kura.web.client.util.Constants;
import org.eclipse.kura.web.client.util.FailureHandler;
import org.eclipse.kura.web.client.util.FormUtils;
import org.eclipse.kura.web.client.util.MessageUtils;
import org.eclipse.kura.web.shared.model.GwtNetIfConfigMode;
import org.eclipse.kura.web.shared.model.GwtNetIfStatus;
import org.eclipse.kura.web.shared.model.GwtNetIfStatusModel;
import org.eclipse.kura.web.shared.model.GwtNetIfType;
import org.eclipse.kura.web.shared.model.GwtNetInterfaceConfig;
import org.eclipse.kura.web.shared.model.GwtSession;
import org.eclipse.kura.web.shared.service.GwtNetworkService;
import org.eclipse.kura.web.shared.service.GwtNetworkServiceAsync;

import com.allen_sauer.gwt.log.client.Log;
import com.extjs.gxt.ui.client.Style.Scroll;
import com.extjs.gxt.ui.client.data.ListLoadResult;
import com.extjs.gxt.ui.client.event.BaseEvent;
import com.extjs.gxt.ui.client.event.ButtonEvent;
import com.extjs.gxt.ui.client.event.ComponentEvent;
import com.extjs.gxt.ui.client.event.Events;
import com.extjs.gxt.ui.client.event.FieldEvent;
import com.extjs.gxt.ui.client.event.Listener;
import com.extjs.gxt.ui.client.event.SelectionChangedEvent;
import com.extjs.gxt.ui.client.event.SelectionChangedListener;
import com.extjs.gxt.ui.client.event.SelectionListener;
import com.extjs.gxt.ui.client.store.ListStore;
import com.extjs.gxt.ui.client.widget.Component;
import com.extjs.gxt.ui.client.widget.ComponentPlugin;
import com.extjs.gxt.ui.client.widget.LayoutContainer;
import com.extjs.gxt.ui.client.widget.button.Button;
import com.extjs.gxt.ui.client.widget.form.AdapterField;
import com.extjs.gxt.ui.client.widget.form.ComboBox;
import com.extjs.gxt.ui.client.widget.form.ComboBox.TriggerAction;
import com.extjs.gxt.ui.client.widget.form.Field;
import com.extjs.gxt.ui.client.widget.form.FieldSet;
import com.extjs.gxt.ui.client.widget.form.FormPanel;
import com.extjs.gxt.ui.client.widget.form.SimpleComboBox;
import com.extjs.gxt.ui.client.widget.form.SimpleComboValue;
import com.extjs.gxt.ui.client.widget.form.TextArea;
import com.extjs.gxt.ui.client.widget.form.TextField;
import com.extjs.gxt.ui.client.widget.layout.FitLayout;
import com.extjs.gxt.ui.client.widget.layout.FlowLayout;
import com.extjs.gxt.ui.client.widget.layout.FormData;
import com.extjs.gxt.ui.client.widget.layout.FormLayout;
import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.Element;
import com.google.gwt.user.client.rpc.AsyncCallback;

public class TcpIpConfigTab extends LayoutContainer 
{
	private static final Messages MSGS = GWT.create(Messages.class);
	
	private final GwtNetworkServiceAsync gwtNetworkService = GWT.create(GwtNetworkService.class);
	private final ToolTipBox toolTipField = new ToolTipBox("345px");
	private final String defaultToolTip = "Mouse over enabled items on the left to see help text.";

	private static final String IPV4_REGEX  = "\\b(?:(?:25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\\.){3}(?:25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\\b";
	
    private GwtSession             m_currentSession;

    private boolean                m_dirty;
    private boolean                m_initialized;
	private GwtNetInterfaceConfig  m_selectNetIfConfig;
	private NetInterfaceConfigTabs m_netInterfaceConfigTabs;
	
	private FormPanel              m_formPanel;
	private FieldSet 			   m_fieldSet;
	private ListStore<GwtNetIfStatusModel> m_statusListStore;
	private ComboBox<GwtNetIfStatusModel> m_statusCombo;
	private SimpleComboBox<String> m_configureCombo;
	private TextField<String>      m_ipAddressField;
	private TextField<String>      m_subnetMaskField;
	private TextField<String>      m_gatewayField;
	private AdapterField           m_renewDHCPButton;
	private TextArea               m_dnsReadOnlyField;
	private TextArea               m_dnsField;
	private TextArea               m_domainsField;

    private ComponentPlugin        m_dirtyPlugin;
    private ComponentPlugin		   m_warningPlugin;
    
    private class MouseOverListener implements Listener<BaseEvent> {

    	private String  html;
    	
    	public MouseOverListener(String html) {
    		this.html = html;
    	}
		public void handleEvent(BaseEvent be) {
			toolTipField.setText(html);
		}
    }

	
    public TcpIpConfigTab(GwtSession currentSession, NetInterfaceConfigTabs netInterfaceConfigTabs) {
        m_currentSession = currentSession;
    	m_dirty          = true;
    	m_initialized    = false;
    	
    	m_netInterfaceConfigTabs = netInterfaceConfigTabs;
    	
	    final TcpIpConfigTab theTab = this;
    	m_dirtyPlugin = new ComponentPlugin() {  
    		public void init(Component component) {  
    			component.addListener(Events.Change, new Listener<ComponentEvent>() {  
    				public void handleEvent(ComponentEvent be) {
    					FormUtils.addDirtyFieldIcon(be.getComponent());    					
    					theTab.fireEvent(Events.Change);
    				}  
    			});  
  	      	}  
  	    };
  	    m_warningPlugin = new ComponentPlugin() {
			public void init(Component component) {
				component.addListener(Events.Invalid, new Listener<ComponentEvent>() {
					public void handleEvent(ComponentEvent be) {
						FormUtils.addWarningFieldIcon(be.getComponent(), MSGS.netStatusWarning());
					}
				});
			}
		};
    }
  	    

    
    public void setNetInterface(GwtNetInterfaceConfig netIfConfig)
    {
    	m_dirty = true;
    	if(netIfConfig != null && netIfConfig.getSubnetMask() != null && netIfConfig.getSubnetMask().equals("255.255.255.255")) {
    		netIfConfig.setSubnetMask("");
    	}
    	m_selectNetIfConfig = netIfConfig;
    	
    	Log.debug("got new netIfConfig for TCP/IP config for " + netIfConfig.getName() + ": " +
       			"\n\t\tStatus: " + netIfConfig.getStatus() +
       			"\n\t\tConfig Mode: " + netIfConfig.getConfigMode() +
       			"\n\t\tIP Address: " + netIfConfig.getIpAddress() +
       			"\n\t\tSubnet Mask: " + netIfConfig.getSubnetMask() +
       			"\n\t\tGateway: " + netIfConfig.getGateway() +
       			"\n\t\tDNS Servers: " + netIfConfig.getDnsServers());
    	
    	// Remove LAN option for modems
    	if(m_selectNetIfConfig != null && m_selectNetIfConfig.getHwTypeEnum() == GwtNetIfType.MODEM) {
    	    if(m_statusCombo != null) {
                m_statusListStore.remove(m_statusListStore.findModel(GwtNetIfStatusModel.STATUS, GwtNetIfStatus.netIPv4StatusEnabledLAN.name()));
                m_statusCombo.addListener(Events.OnMouseOver, new MouseOverListener(MSGS.netIPv4ModemToolTipStatus()));
    	    }
    	} else {
    	    if(m_statusCombo != null) {
    	        initializeStatusListStore();
    	        m_statusCombo.addListener(Events.OnMouseOver, new MouseOverListener(MSGS.netIPv4ToolTipStatus()));
             }
    	}
    }
    
        
    public void getUpdatedNetInterface(GwtNetInterfaceConfig updatedNetIf)
    {
    	if (m_formPanel != null) {
    	    Log.debug("in getUpdatedNetInterface(): m_statusCombo.getValue().getStatus().name(): " + m_statusCombo.getValue().getStatus().name());
    	    updatedNetIf.setStatus(m_statusCombo.getValue().getStatus().name());
    		
    		if(MessageUtils.get(GwtNetIfConfigMode.netIPv4ConfigModeDHCP.name()).equals(m_configureCombo.getValue().getValue())) {
    			updatedNetIf.setConfigMode(GwtNetIfConfigMode.netIPv4ConfigModeDHCP.name());
    		} else {
    			updatedNetIf.setConfigMode(GwtNetIfConfigMode.netIPv4ConfigModeManual.name());
    		}
    		
    		if(m_ipAddressField.getValue() != null) {
    			updatedNetIf.setIpAddress(m_ipAddressField.getValue());
    		} else {
    			updatedNetIf.setIpAddress("");
    		}
    		if(m_subnetMaskField.getValue() != null) {
    			updatedNetIf.setSubnetMask(m_subnetMaskField.getValue());
    		} else {
    			updatedNetIf.setSubnetMask("");
    		}
    		if(m_gatewayField.getValue() != null) {
    			updatedNetIf.setGateway(m_gatewayField.getValue());
    		} else {
    			updatedNetIf.setGateway("");
    		}
    		if(m_dnsField.getValue() != null) {
    			updatedNetIf.setDnsServers(m_dnsField.getValue());
    		} else {
    			updatedNetIf.setDnsServers("");
    		}    
    		if(m_domainsField.getValue() != null) {
    			updatedNetIf.setSearchDomains(m_domainsField.getValue());
    		} else {
    			updatedNetIf.setSearchDomains("");
    		}
    	}
    }
    
    
    public boolean isValid() 
    {
        List<Field<?>> fields = m_formPanel.getFields();
        for (int i=0; i<fields.size(); i++) {
        	if (fields.get(i) instanceof Field) {
        		Field<?> field = (Field<?>) fields.get(i);
        		if (!field.isValid()) {
        			return false;
        		}
        	}
        }
        
        //check and make sure if 'Enabled for WAN' then either DHCP is selected or STATIC and a gateway is set
        if(GwtNetIfStatus.netIPv4StatusEnabledWAN.equals(m_statusCombo.getValue().getStatus())) {
        	if(m_configureCombo.getSimpleValue().equals(GwtNetIfConfigMode.netIPv4ConfigModeManual)) {
        		if(m_gatewayField.getValue() == null || m_gatewayField.getValue().trim().equals("")) {
        			return false;
        		}
        	}
        }
        
        return true;
    }


    public boolean isDirty() 
    {
    	if (m_formPanel == null) {
    		return false;
    	}
        List<Field<?>> fields = m_formPanel.getFields();
        for (int i=0; i<fields.size(); i++) {
        	if (fields.get(i) instanceof Field) {
	            Field<?> field = (Field<?>) fields.get(i);
	            if (field.isDirty()) {
	            	//handle special case of null value vs empty string
	            	if((field.getOriginalValue() != null && field.getOriginalValue().equals("") && field.getValue() == null) || 
	            			(field.getValue() != null && field.getValue().equals("") && field.getOriginalValue() == null)) {
	            		continue;
	            	} else if(field.getOriginalValue() != null && field.getValue() != null && field.getOriginalValue().equals(field.getValue())) {
	            		continue;
	            	} else {
	            		if(field.getOriginalValue() instanceof SimpleComboValue) {
	            			Log.debug("field " + field.getName() + " is dirty - original value: " + ((SimpleComboValue)field.getOriginalValue()).getValue() + " with type: " + field.getOriginalValue().getClass().toString());
	            			if(field.getValue() != null) {
	            				Log.debug("\tnew value: " + ((SimpleComboValue)field.getValue()).getValue() +  " with type: " + field.getValue().getClass().toString());
	            			}
	            		} else {
	            			Log.debug("field " + field.getName() + " is dirty - original value: " + field.getOriginalValue() + " with type: " + field.getOriginalValue().getClass().toString());
	            			if(field.getValue() != null) {
	            				Log.debug("\tnew value: " + field.getValue() +  " with type: " + field.getValue().getClass().toString());
	            			}
	            		}
	            		return true;
	            	}
	            } else {
	            	Log.debug("NOT DIRTY: " + field.getName() + " value: " + field.getValue());
	            }
        	}
        }
        return false;
    }
    
    
    public boolean isLanEnabled() 
    {
    	if (m_statusCombo == null) {
    		Log.debug("TcpIpConfigTab.isLanEnabled() - m_statusCombo is null");
    		return false;
    	}
    	Log.debug("TcpIpConfigTab.isLanEnabled() - m_statusCombo.getValue().getStatus(): " + m_statusCombo.getValue().getStatus());
    	return GwtNetIfStatus.netIPv4StatusEnabledLAN.equals(m_statusCombo.getValue().getStatus());
    }
    
    public boolean isWanEnabled() 
    {
    	if (m_statusCombo == null) {
    		Log.debug("TcpIpConfigTab.isWanEnabled() - m_statusCombo is null");
    		return false;
    	}
        Log.debug("TcpIpConfigTab.isWanEnabled() - m_statusCombo.getValue().getStatus(): " + m_statusCombo.getValue().getStatus());
        return GwtNetIfStatus.netIPv4StatusEnabledWAN.equals(m_statusCombo.getValue().getStatus());
    }
    
    public GwtNetIfStatus getStatus()
    {
        return m_statusCombo.getValue().getStatus();
    }
    
    public boolean isDhcp() 
    {
        if (m_configureCombo == null) {
            Log.debug("TcpIpConfigTab.isDhcp() - m_configureCombo is null");
            return true;
        }
        Log.debug("TcpIpConfigTab.isDhcp() - m_configureCombo.getSimpleValue(): " + m_configureCombo.getSimpleValue());
        return (MessageUtils.get(GwtNetIfConfigMode.netIPv4ConfigModeDHCP.name()).equals(m_configureCombo.getSimpleValue()));
    }
    
    protected void onRender(Element parent, int index) 
    {
    	super.onRender(parent, index);         
        setLayout(new FitLayout());
        setId("network-tcpip");
        
        FormData formData = new FormData();
        formData.setWidth(250);
        
        m_formPanel = new FormPanel();
        m_formPanel.setFrame(false);
        m_formPanel.setBodyBorder(false);
        m_formPanel.setHeaderVisible(false);
        m_formPanel.setLayout(new FlowLayout());
        m_formPanel.setStyleAttribute("min-width", "775px");
        m_formPanel.setStyleAttribute("padding-left", "30px");

        m_fieldSet = new FieldSet();
        FormLayout layoutAccount = new FormLayout();
        layoutAccount.setLabelWidth(Constants.LABEL_WIDTH_FORM);
        m_fieldSet.setLayout(layoutAccount);
        m_fieldSet.setBorders(false);
        
        //
    	// Enabled
        //
        initializeStatusListStore();
        
        //
        // Tool Tip Box
        //
        toolTipField.setText(defaultToolTip);
        m_fieldSet.add(toolTipField);

        
        //
        // Status Combo
        //
        m_statusCombo = new ComboBox<GwtNetIfStatusModel>();
        m_statusCombo.setName("comboStatus");
        m_statusCombo.setDisplayField(GwtNetIfStatusModel.NAME);
        m_statusCombo.setFieldLabel(MSGS.netIPv4Status());
        m_statusCombo.setEditable(false);
        m_statusCombo.setStore(m_statusListStore);
        m_statusCombo.setTemplate(getTemplate());
        m_statusCombo.setTypeAhead(true);  
        m_statusCombo.setTriggerAction(TriggerAction.ALL);
        m_statusCombo.setValue(m_statusListStore.findModel(GwtNetIfStatusModel.STATUS, GwtNetIfStatus.netIPv4StatusDisabled.name()));
        m_statusCombo.addSelectionChangedListener( new SelectionChangedListener<GwtNetIfStatusModel>() {			
			@Override
			public void selectionChanged(SelectionChangedEvent<GwtNetIfStatusModel> se) {
				m_netInterfaceConfigTabs.adjustInterfaceTabs();
				refreshForm();
				// Check for other WAN interfaces if current interface is changed to WAN
				if(isWanEnabled()) {
					m_formPanel.mask(MSGS.waiting());
					gwtNetworkService.findNetInterfaceConfigurations(new AsyncCallback<ListLoadResult<GwtNetInterfaceConfig>>() {
						public void onFailure(Throwable caught) {
							m_formPanel.unmask();;
						}
						public void onSuccess(ListLoadResult<GwtNetInterfaceConfig> result) {
							for(GwtNetInterfaceConfig config : result.getData()) {
								if(config.getStatusEnum().equals(GwtNetIfStatus.netIPv4StatusEnabledWAN) &&
										!config.getName().equals(m_selectNetIfConfig.getName())) {
									m_statusCombo.fireEvent(Events.Invalid);
								}
							}
							m_formPanel.unmask();;
						}
						
					});
				}
				else { 
					FormUtils.removeWarningFieldIcon(m_statusCombo);
				}
			}
		});
        m_statusCombo.addStyleName("kura-combobox");
        m_statusCombo.addPlugin(m_dirtyPlugin);
        m_statusCombo.addPlugin(m_warningPlugin);
        m_fieldSet.add(m_statusCombo, formData);

        //
    	// Configure IP
        //
        m_configureCombo = new SimpleComboBox<String>();
        m_configureCombo.setName("comboConfigure");
        m_configureCombo.setFieldLabel(MSGS.netIPv4Configure());
        m_configureCombo.setEditable(false);
        m_configureCombo.setTypeAhead(true);  
        m_configureCombo.setTriggerAction(TriggerAction.ALL);
        m_configureCombo.setStyleAttribute("margin-top", Constants.LABEL_MARGIN_TOP_SEPARATOR);
    	// show account status combo box
        for (GwtNetIfConfigMode mode : GwtNetIfConfigMode.values()) {
        	m_configureCombo.add(MessageUtils.get(mode.name()));
        }
        if(m_selectNetIfConfig != null && m_selectNetIfConfig.getHwTypeEnum() == GwtNetIfType.MODEM) {
            m_statusListStore.remove(m_statusListStore.findModel(GwtNetIfStatusModel.STATUS, GwtNetIfStatus.netIPv4StatusEnabledLAN.name()));
        }
        m_configureCombo.setSimpleValue(MessageUtils.get(GwtNetIfConfigMode.netIPv4ConfigModeDHCP.name()));
        m_configureCombo.addSelectionChangedListener( new SelectionChangedListener<SimpleComboValue<String>>() {			
			@Override
			public void selectionChanged(SelectionChangedEvent<SimpleComboValue<String>> se) {
				m_netInterfaceConfigTabs.adjustInterfaceTabs();
				refreshForm();
			}
		});
        m_configureCombo.addListener(Events.OnMouseOver, new MouseOverListener(MSGS.netIPv4ToolTipConfigure()));
        m_configureCombo.addStyleName("kura-combobox");
        m_configureCombo.addPlugin(m_dirtyPlugin);
        m_fieldSet.add(m_configureCombo, formData);
        
        //
        // IP Address
        // 
        m_ipAddressField = new TextField<String>();
        m_ipAddressField.setAllowBlank(true);
        m_ipAddressField.setName("ipAddress");
    	m_ipAddressField.setFieldLabel(MSGS.netIPv4Address());
    	m_ipAddressField.setRegex(IPV4_REGEX);
    	m_ipAddressField.getMessages().setRegexText(MSGS.netIPv4InvalidAddress());
    	m_ipAddressField.addPlugin(m_dirtyPlugin);
    	m_ipAddressField.addListener(Events.OnMouseOver, new MouseOverListener(MSGS.netIPv4ToolTipAddress()));
    	m_ipAddressField.addStyleName("kura-textfield");
        m_fieldSet.add(m_ipAddressField, formData);

        //
        // Subnet mask
        // 
        m_subnetMaskField = new TextField<String>();
        m_subnetMaskField.setAllowBlank(true);
        m_subnetMaskField.setName("subnetMask");
    	m_subnetMaskField.setFieldLabel(MSGS.netIPv4SubnetMask());
    	m_subnetMaskField.setRegex(IPV4_REGEX);
    	m_subnetMaskField.getMessages().setRegexText(MSGS.netIPv4InvalidAddress());
    	m_subnetMaskField.addListener(Events.OnMouseOver, new MouseOverListener(MSGS.netIPv4ToolTipSubnetMask()));
    	m_subnetMaskField.addStyleName("kura-textfield");
    	m_subnetMaskField.addPlugin(m_dirtyPlugin);
        m_fieldSet.add(m_subnetMaskField, formData);

        //
        // Gateway
        // 
        m_gatewayField = new TextField<String>();
        m_gatewayField.setAllowBlank(true);
        m_gatewayField.setName("gateway");
    	m_gatewayField.setFieldLabel(MSGS.netIPv4Gateway());
    	m_gatewayField.setRegex(IPV4_REGEX);
    	m_gatewayField.getMessages().setRegexText(MSGS.netIPv4InvalidAddress());
    	m_gatewayField.addListener(Events.OnMouseOver, new MouseOverListener(MSGS.netIPv4ToolTipGateway()));
    	m_gatewayField.addStyleName("kura-textfield");
    	m_gatewayField.addPlugin(m_dirtyPlugin);
        m_fieldSet.add(m_gatewayField, formData);

        //
        // Renew DHCP Lease
        // 
        Button renewDHCP = new Button(MSGS.netIPv4RenewDHCPLease(),
        		null, // no image
        		new SelectionListener<ButtonEvent>() {
					@Override
					public void componentSelected(ButtonEvent ce) {
						gwtNetworkService.renewDhcpLease(m_selectNetIfConfig.getName(), new AsyncCallback<Void>() {
							public void onSuccess(Void result) {
								update();
							    Log.debug("successfully renewed DHCP lease");
							}
							public void onFailure(Throwable caught) {
								Log.debug("caught: " + caught.toString());
								FailureHandler.handle(caught);
							}
    					});
						
					}});
        m_renewDHCPButton = new AdapterField(renewDHCP);
        m_renewDHCPButton.setId("net-ipv4-renew-dhcp");
        m_renewDHCPButton.setLabelSeparator("");
        m_renewDHCPButton.addListener(Events.OnMouseOver, new MouseOverListener(MSGS.netIPv4ToolTipRenew()));
        
        FormData formDataButton = new FormData();
        formDataButton.setWidth(150);
        m_renewDHCPButton.addPlugin(m_dirtyPlugin);
        m_fieldSet.add(m_renewDHCPButton, formDataButton);        

        //
        // DNS read-only (for DHCP)
        // 
        m_dnsReadOnlyField = new TextArea();
        m_dnsReadOnlyField.setAllowBlank(true);
        m_dnsReadOnlyField.setName("dnsReadOnlyServers");
        m_dnsReadOnlyField.setFieldLabel(MSGS.netIPv4DNSServers());
        m_dnsReadOnlyField.setEnabled(false);
        m_dnsReadOnlyField.setHeight("1.5em");
        m_dnsReadOnlyField.setStyleAttribute("margin-top", Constants.LABEL_MARGIN_TOP_SEPARATOR);
        m_dnsReadOnlyField.setStyleAttribute("margin-bottom", "0px");
        m_dnsReadOnlyField.addListener(Events.OnMouseOver, new MouseOverListener(MSGS.netIPv4ToolTipDns()));
        m_fieldSet.add(m_dnsReadOnlyField, formData);

        //
        // DNS
        // 
        m_dnsField = new TextArea();
        m_dnsField.setAllowBlank(true);
        m_dnsField.setName("dnsServers");
    	m_dnsField.setFieldLabel(MSGS.netIPv4DNSServers());
    	m_dnsField.setRegex("((" + IPV4_REGEX + ")[\\s,;\\n\\r\\t]*)+");
    	m_dnsField.getMessages().setRegexText(MSGS.netIPv4InvalidAddress());
    	m_dnsField.addListener(Events.OnMouseOver, new MouseOverListener(MSGS.netIPv4ToolTipDns()));
		m_dnsField.addListener(Events.OnChange, new Listener<FieldEvent>() {
			// Hide the DNS entry from the DHCP server if a custom DNS entry is added
    		public void handleEvent(FieldEvent be) {
    			TextArea t = (TextArea) be.getField();
    			if (t.getValue() == null || t.getValue().isEmpty()) {
        			showReadOnlyDns();
    			} else {
    				hideReadOnlyDns();
    			}
    		}
    	});
    	m_dnsField.addPlugin(m_dirtyPlugin);
        m_fieldSet.add(m_dnsField, formData);
        
        //
        // Search Domains
        // 
        m_domainsField = new TextArea();
        m_domainsField.setAllowBlank(true);
        m_domainsField.setName("searchDomains");
    	m_domainsField.setFieldLabel(MSGS.netIPv4SearchDomains());
    	m_domainsField.setStyleAttribute("margin-top", Constants.LABEL_MARGIN_TOP_SEPARATOR);
    	m_domainsField.addPlugin(m_dirtyPlugin);
        m_fieldSet.add(m_domainsField, formData);

        m_formPanel.add(m_fieldSet);
        add(m_formPanel);
        setScrollMode(Scroll.AUTO);
        m_initialized = true;
    }
        
	public void refresh() 
	{
		if (m_dirty && m_initialized) {			
	        m_dirty = false;
			if (m_selectNetIfConfig == null) {
				reset();
	        }
			else {
				update();				
			}		
		}
	}	
	
	private void update()
	{
		for (Field<?> field : m_formPanel.getFields()) {
			FormUtils.removeDirtyFieldIcon(field);
		}		
		FormUtils.removeWarningFieldIcon(m_statusCombo);
		if (m_selectNetIfConfig != null) {

			Log.debug("in update(): m_selectNetIfConfig.getStatus().name(): " + m_selectNetIfConfig.getStatus());
			
			m_statusCombo.setValue(m_statusListStore.findModel(GwtNetIfStatusModel.STATUS, GwtNetIfStatus.valueOf(m_selectNetIfConfig.getStatus()).name()));
			m_statusCombo.setOriginalValue(m_statusCombo.getValue());

			m_configureCombo.setSimpleValue(MessageUtils.get(m_selectNetIfConfig.getConfigMode()));
			m_configureCombo.setOriginalValue(m_configureCombo.getValue());
			
			// adjust tabs based on status and configure combo boxes
			m_netInterfaceConfigTabs.adjustInterfaceTabs();
			
			m_ipAddressField.setValue(m_selectNetIfConfig.getIpAddress());
			m_ipAddressField.setOriginalValue(m_selectNetIfConfig.getIpAddress());
			
			m_subnetMaskField.setValue(m_selectNetIfConfig.getSubnetMask());
			m_subnetMaskField.setOriginalValue(m_selectNetIfConfig.getSubnetMask());
			
			m_gatewayField.setValue(m_selectNetIfConfig.getGateway());
			m_gatewayField.setOriginalValue(m_selectNetIfConfig.getGateway());
            
            if(m_selectNetIfConfig.getReadOnlyDnsServers() != null) {
                m_dnsReadOnlyField.setValue(m_selectNetIfConfig.getReadOnlyDnsServers());
                m_dnsReadOnlyField.setOriginalValue(m_selectNetIfConfig.getReadOnlyDnsServers());
            } else {
                m_dnsReadOnlyField.setValue("");
                m_dnsReadOnlyField.setOriginalValue("");
            }
			
			if(m_selectNetIfConfig.getDnsServers() != null) {
				m_dnsField.setValue(m_selectNetIfConfig.getDnsServers());
				m_dnsField.setOriginalValue(m_selectNetIfConfig.getDnsServers());
			} else {
				m_dnsField.setValue("");
				m_dnsField.setOriginalValue("");
			}
			
			if(m_selectNetIfConfig.getSearchDomains() != null) {
				m_domainsField.setValue(m_selectNetIfConfig.getSearchDomains());
				m_domainsField.setOriginalValue(m_selectNetIfConfig.getSearchDomains());
			} else {
				m_domainsField.setValue("");
				m_domainsField.setOriginalValue("");
			}
		}		
		refreshForm();
	}


	private void refreshForm() {
	    if(m_formPanel != null) {
    	    if(m_selectNetIfConfig != null && m_selectNetIfConfig.getHwTypeEnum() == GwtNetIfType.MODEM) {
                for (Field<?> field : m_formPanel.getFields()) {            
                    field.setEnabled(false);
                }
    
                m_statusCombo.setEnabled(true);
                m_dnsField.setEnabled(true);

                // TODO: set value according to modem setting    	        
    	        m_configureCombo.setSimpleValue(MessageUtils.get(GwtNetIfConfigMode.netIPv4ConfigModeDHCP.name()));    	        
    	    } else {
    	        GwtNetIfStatus statusValue = m_statusCombo.getValue().getStatus();
    	        
    	        if(GwtNetIfStatus.netIPv4StatusDisabled.equals(statusValue)) {
    				//clear/reset the fields
    				m_configureCombo.setSimpleValue(MessageUtils.get(GwtNetIfConfigMode.netIPv4ConfigModeDHCP.name()));
    				m_ipAddressField.setValue("");
    				m_subnetMaskField.setValue("");
    				m_gatewayField.setValue("");
    				m_dnsField.setValue("");
    				m_domainsField.setValue("");
    				
        			for (Field<?> field : m_formPanel.getFields()) {			
        				if (field != m_statusCombo) {
        					field.setEnabled(false);
        				}
        			}
        		}
        		else {
                    m_configureCombo.setEnabled(true);
        
                    String configureValue = m_configureCombo.getSimpleValue();
                    
                    Log.debug("configureValue: " + configureValue);
                    Log.debug("MessageUtils.get(GwtNetIfConfigMode.netIPv4ConfigModeDHCP.name())): " + MessageUtils.get(GwtNetIfConfigMode.netIPv4ConfigModeDHCP.name()));
                    Log.debug("GwtNetIfConfigMode.netIPv4ConfigModeDHCP.name(): " + GwtNetIfConfigMode.netIPv4ConfigModeDHCP.name());
                    
        			if (configureValue.equals(MessageUtils.get(GwtNetIfConfigMode.netIPv4ConfigModeDHCP.name()))) {
        				Log.debug("Enabling gateway field");
        				m_ipAddressField.setEnabled(false);
        				m_subnetMaskField.setEnabled(false);
        				m_gatewayField.setEnabled(false);
        				m_renewDHCPButton.setEnabled(true);
        			}
        			else {
        				m_ipAddressField.setEnabled(true);
        				m_subnetMaskField.setEnabled(true);
        				m_gatewayField.setEnabled(true);
        				
        				if (GwtNetIfStatus.netIPv4StatusEnabledWAN.equals(statusValue)) {
        				    Log.debug("Enabling gateway field");
        				    m_gatewayField.setEnabled(true);
        				} else {
        					Log.debug("Disabling gateway field");
        					m_gatewayField.setValue("");
        					m_gatewayField.setEnabled(false);
        				}
        				
        				m_renewDHCPButton.setEnabled(false);
        			}
        			m_dnsField.setEnabled(true);
        			m_domainsField.setEnabled(true);
        		}
    	    }
    	    
    	    // Show read-only dns field when DHCP is selected and there are no custom DNS entries
    	    String configureValue = m_configureCombo.getSimpleValue();
    	    if (configureValue.equals(MessageUtils.get(GwtNetIfConfigMode.netIPv4ConfigModeDHCP.name())) 
    	    		&& (m_dnsField.getValue() == null || m_dnsField.getValue().isEmpty())) {    	    	
    	    	showReadOnlyDns();
    	    } else {
    	    	hideReadOnlyDns();
    	    }
	    }
	}
	
	
	private void reset()
	{
		Log.debug("TcpIpConfigTab: reset()");
		m_statusCombo.setValue(m_statusListStore.findModel(GwtNetIfStatusModel.STATUS, GwtNetIfStatus.netIPv4StatusDisabled.name()));
		m_statusCombo.setOriginalValue(m_statusCombo.getValue());

		m_configureCombo.setSimpleValue(MessageUtils.get(GwtNetIfConfigMode.netIPv4ConfigModeDHCP.name()));
		m_configureCombo.setOriginalValue(m_configureCombo.getValue());

		m_ipAddressField.setValue("");
		m_ipAddressField.setOriginalValue("");
		
		m_subnetMaskField.setValue("");
		m_subnetMaskField.setOriginalValue("");
		
		m_gatewayField.setValue("");
		m_gatewayField.setOriginalValue("");
		
		m_dnsField.setValue("");
		m_dnsField.setOriginalValue("");
		
		m_domainsField.setValue("");
		m_domainsField.setOriginalValue("");
		
		update();
	}
	
	private void initializeStatusListStore() {
        m_statusListStore = new ListStore<GwtNetIfStatusModel>();
        m_statusListStore.add(new GwtNetIfStatusModel(GwtNetIfStatus.netIPv4StatusDisabled,
                MessageUtils.get("netIPv4StatusDisabled"),
                MSGS.netIPv4ToolTipStatusDisabled()));
        m_statusListStore.add(new GwtNetIfStatusModel(GwtNetIfStatus.netIPv4StatusEnabledLAN,
                MessageUtils.get("netIPv4StatusEnabledLAN"),
                MSGS.netIPv4ToolTipStatusEnabledLAN()));
        m_statusListStore.add(new GwtNetIfStatusModel(GwtNetIfStatus.netIPv4StatusEnabledWAN,
                MessageUtils.get("netIPv4StatusEnabledWAN"),
                MSGS.netIPv4ToolTipStatusEnabledWAN()));
	}
	
    // Combo box item template
    private native String getTemplate() /*-{
        return  [
        '<tpl for=".">',
        '<div class="x-combo-list-item" qtitle="{name}">{name}</div>',
        '</tpl>'
        ].join("");
    }-*/;
    
    private void showReadOnlyDns() {
    	m_dnsReadOnlyField.show();
        
        // TODO: Better way to set the height?
        int rows = 1;
        if(m_selectNetIfConfig != null) {
	        String dnsString = m_selectNetIfConfig.getReadOnlyDnsServers();
	        if(dnsString != null) {
    	        for(int i=0; i<dnsString.length(); i++) {
    	            if(dnsString.charAt(i) == '\n') {
    	                rows++;
    	            }
    	        }
	        }
        }
        m_dnsReadOnlyField.setHeight(Double.toString(1.5 * rows) + "em");

        m_dnsField.setStyleAttribute("margin-top", "0px");
        m_dnsField.setLabelSeparator("");
        m_dnsField.setFieldLabel("");
    }
    
    private void hideReadOnlyDns() {
    	m_dnsReadOnlyField.hide();
        
        m_dnsField.setStyleAttribute("margin-top", Constants.LABEL_MARGIN_TOP_SEPARATOR);
        m_dnsField.setLabelSeparator(m_formPanel.getLabelSeparator());
        m_dnsField.setFieldLabel(MSGS.netIPv4DNSServers());
    }
}
