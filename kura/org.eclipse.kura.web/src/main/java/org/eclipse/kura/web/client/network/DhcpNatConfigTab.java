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
import org.eclipse.kura.web.client.util.FormUtils;
import org.eclipse.kura.web.client.util.MessageUtils;
import org.eclipse.kura.web.shared.model.GwtNetIfType;
import org.eclipse.kura.web.shared.model.GwtNetInterfaceConfig;
import org.eclipse.kura.web.shared.model.GwtNetRouterMode;
import org.eclipse.kura.web.shared.model.GwtSession;
import org.eclipse.kura.web.shared.model.GwtWifiWirelessMode;

import com.allen_sauer.gwt.log.client.Log;
import com.extjs.gxt.ui.client.Style.Scroll;
import com.extjs.gxt.ui.client.event.BaseEvent;
import com.extjs.gxt.ui.client.event.ComponentEvent;
import com.extjs.gxt.ui.client.event.Events;
import com.extjs.gxt.ui.client.event.Listener;
import com.extjs.gxt.ui.client.event.SelectionChangedEvent;
import com.extjs.gxt.ui.client.event.SelectionChangedListener;
import com.extjs.gxt.ui.client.widget.Component;
import com.extjs.gxt.ui.client.widget.ComponentPlugin;
import com.extjs.gxt.ui.client.widget.LayoutContainer;
import com.extjs.gxt.ui.client.widget.form.ComboBox.TriggerAction;
import com.extjs.gxt.ui.client.widget.form.Field;
import com.extjs.gxt.ui.client.widget.form.FieldSet;
import com.extjs.gxt.ui.client.widget.form.FormPanel;
import com.extjs.gxt.ui.client.widget.form.NumberField;
import com.extjs.gxt.ui.client.widget.form.Radio;
import com.extjs.gxt.ui.client.widget.form.RadioGroup;
import com.extjs.gxt.ui.client.widget.form.SimpleComboBox;
import com.extjs.gxt.ui.client.widget.form.SimpleComboValue;
import com.extjs.gxt.ui.client.widget.form.TextField;
import com.extjs.gxt.ui.client.widget.form.Validator;
import com.extjs.gxt.ui.client.widget.layout.FitLayout;
import com.extjs.gxt.ui.client.widget.layout.FlowLayout;
import com.extjs.gxt.ui.client.widget.layout.FormData;
import com.extjs.gxt.ui.client.widget.layout.FormLayout;
import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.Element;

public class DhcpNatConfigTab extends LayoutContainer 
{
	private static final Messages MSGS = GWT.create(Messages.class);
	
	private final ToolTipBox toolTipField = new ToolTipBox("205px");
	private final String defaultToolTip = "Mouse over enabled items on the left to see help text.";

	private static final String IPV4_REGEX  = "\\b(?:(?:25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\\.){3}(?:25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\\b";
	
	@SuppressWarnings("unused")
    private GwtSession            m_currentSession;

    private boolean                m_dirty;
    private boolean                m_initialized;
	private GwtNetInterfaceConfig  m_selectNetIfConfig;
	private TcpIpConfigTab         m_tcpIpConfigTab;
	private WirelessConfigTab	   m_wirelessConfigTab;
	
	private FormPanel              m_formPanel;
	private SimpleComboBox<String> m_modeCombo;
	private TextField<String>      m_dhcpBeginAddressField;
	private TextField<String>      m_dhcpEndAddressField;
	private TextField<String>      m_dhcpSubnetMaskField;
	private NumberField            m_dhcpLeaseDefaultField;
	private NumberField            m_dhcpLeaseMaxField;
	private Radio 				   m_passDnsRadioTrue;
	private Radio 				   m_passDnsRadioFalse;
	private RadioGroup 			   m_passDnsRadioGroup;

    private ComponentPlugin        m_dirtyPlugin;
	
    private class MouseOverListener implements Listener<BaseEvent> {

    	private String  html;
    	
    	public MouseOverListener(String html) {
    		this.html = html;
    	}
		public void handleEvent(BaseEvent be) {
			toolTipField.setText(html);
		}
    	
    }
	
    public DhcpNatConfigTab(GwtSession currentSession,
    					    TcpIpConfigTab tcpIpConfigTab,
    					    WirelessConfigTab wirelessConfigTab)
    {
        m_currentSession    = currentSession;
        m_tcpIpConfigTab    = tcpIpConfigTab;
        m_wirelessConfigTab = wirelessConfigTab;
    	m_dirty             = true;
    	m_initialized       = false;
    
	    final DhcpNatConfigTab theTab = this;
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
  	    
  	    m_tcpIpConfigTab.addListener(Events.Change, new Listener<BaseEvent>() {
  	    	public void handleEvent(BaseEvent be) {
  	    		refreshForm();
  	    	}
  	    });
  	    
  	    m_wirelessConfigTab.addListener(Events.Change, new Listener<BaseEvent>() {
  	    	public void handleEvent(BaseEvent be) {
  	    		refreshForm();
  	    	}
  	    });
    }

    
    public void setNetInterface(GwtNetInterfaceConfig netIfConfig)
    {
    	m_dirty = true;
    	m_selectNetIfConfig = netIfConfig;
    	
    	Log.debug("got new netIfConfig for DHCP server for " + netIfConfig.getName() + ": " +
       			"\n\t\trouter mode: " + netIfConfig.getRouterMode() +
       			"\n\t\trouter DHCP start address: " + netIfConfig.getRouterDhcpBeginAddress() +
       			"\n\t\trouter DHCP end address: " + netIfConfig.getRouterDhcpEndAddress() +
       			"\n\t\trouter default lease: " + Integer.toString(netIfConfig.getRouterDhcpDefaultLease()) +
       			"\n\t\trouter max lease: " + Integer.toString(netIfConfig.getRouterDhcpMaxLease()) +
       			"\n\t\trouter Pass DNS: " + Boolean.toString(netIfConfig.getRouterDnsPass()));
    }
    
    public void disableDhcpNat() {
    	m_selectNetIfConfig.setRouterMode(GwtNetRouterMode.netRouterOff.name());
    }
    
    public void getUpdatedNetInterface(GwtNetInterfaceConfig updatedNetIf)
    {
    	if (m_formPanel != null) {
    		String modeValue = m_modeCombo.getValue().getValue();
    		for (GwtNetRouterMode mode : GwtNetRouterMode.values()) {
    			Log.info("Possible Mode: " + mode.name() + " with util: " + MessageUtils.get(mode.name()));
    			if (MessageUtils.get(mode.name()).equals(modeValue)) {
    				updatedNetIf.setRouterMode(mode.name());
    			}
    		}
    		Log.info("Mode Value: " + modeValue);
    		
	    	updatedNetIf.setRouterDhcpBeginAddress(m_dhcpBeginAddressField.getValue());
	    	updatedNetIf.setRouterDhcpEndAddress(m_dhcpEndAddressField.getValue());
	    	updatedNetIf.setRouterDhcpSubnetMask(m_dhcpSubnetMaskField.getValue());
	    	if (m_dhcpLeaseDefaultField.getValue() != null) {
	    		updatedNetIf.setRouterDhcpDefaultLease(m_dhcpLeaseDefaultField.getValue().intValue());
	    	}
	    	if (m_dhcpLeaseMaxField.getValue() != null) {
	    		updatedNetIf.setRouterDhcpMaxLease(m_dhcpLeaseMaxField.getValue().intValue());
	    	}
	    	updatedNetIf.setRouterDnsPass(m_passDnsRadioTrue.getValue());
    	}
    }

    public boolean isValid() {
    	if (m_formPanel != null) {
    		for (Field<?> field : m_formPanel.getFields()) {
    			if (!field.isValid()) {
    				return false;
    			}
    		}
    	}
    	return true;
    }
    
    public boolean isDirty() {
    	if (m_formPanel == null) {
    		return false;
    	}
        List<Field<?>> fields = m_formPanel.getFields();
        for (int i=0; i<fields.size(); i++) {
        	if (fields.get(i) instanceof Field) {
	            Field<?> field = (Field<?>) fields.get(i);
	            if (field.isDirty()) {
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
    
    protected void onRender(Element parent, int index) 
    {        
    	super.onRender(parent, index);         
        setLayout(new FitLayout());
        setId("network-dhcp-nat");
        FormData formData = new FormData();
        formData.setWidth(250);
        
        m_formPanel = new FormPanel();
        m_formPanel.setFrame(false);
        m_formPanel.setBodyBorder(false);
        m_formPanel.setHeaderVisible(false);
        m_formPanel.setLayout(new FlowLayout());
        m_formPanel.setStyleAttribute("min-width", "775px");
        m_formPanel.setStyleAttribute("padding-left", "30px");


        FieldSet fieldSet = new FieldSet();
        FormLayout layoutAccount = new FormLayout();
        layoutAccount.setLabelWidth(Constants.LABEL_WIDTH_FORM);
        fieldSet.setLayout(layoutAccount);
        fieldSet.setBorders(false);
        
        //
        // Tool Tip Box
        //
        toolTipField.setText(defaultToolTip);
        fieldSet.add(toolTipField);
        
        //
    	// Router Mode
        //
        m_modeCombo = new SimpleComboBox<String>();
        m_modeCombo.setName("comboMode");
        m_modeCombo.setFieldLabel(MSGS.netRouterMode());
        m_modeCombo.setEditable(false);
        m_modeCombo.setTypeAhead(true);  
        m_modeCombo.setTriggerAction(TriggerAction.ALL);
        for (GwtNetRouterMode mode : GwtNetRouterMode.values()) {
        	m_modeCombo.add(MessageUtils.get(mode.name()));
        }
        m_modeCombo.setSimpleValue(MessageUtils.get(GwtNetRouterMode.netRouterDchpNat.name()));
        m_modeCombo.setValidator(new Validator() {
            public String validate(Field<?> field, String value) {
                if (m_tcpIpConfigTab.isDhcp()
                        && !value.equals(MessageUtils.get(GwtNetRouterMode.netRouterOff.toString()))){
                    return MSGS.netRouterConfiguredForDhcpError();
                }
                
                return null;
            }
        });
        m_modeCombo.addSelectionChangedListener( new SelectionChangedListener<SimpleComboValue<String>>() {			
			@Override
			public void selectionChanged(SelectionChangedEvent<SimpleComboValue<String>> se) {
				refreshForm();
			}
		});
        m_modeCombo.addListener(Events.OnMouseOver, new MouseOverListener(MSGS.netRouterToolTipMode()));
        m_modeCombo.addStyleName("kura-combobox");
        m_modeCombo.addPlugin(m_dirtyPlugin);
        fieldSet.add(m_modeCombo, formData);
        
        //
        // DHCP Beginning Address
        // 
        m_dhcpBeginAddressField = new TextField<String>();
        m_dhcpBeginAddressField.setAllowBlank(true);
        m_dhcpBeginAddressField.setName("dhcpBeginAddress");
    	m_dhcpBeginAddressField.setFieldLabel(MSGS.netRouterDhcpBeginningAddress());
    	m_dhcpBeginAddressField.setRegex(IPV4_REGEX);
    	m_dhcpBeginAddressField.getMessages().setRegexText(MSGS.netIPv4InvalidAddress());
    	m_dhcpBeginAddressField.addPlugin(m_dirtyPlugin);
    	m_dhcpBeginAddressField.setStyleAttribute("margin-top", Constants.LABEL_MARGIN_TOP_SEPARATOR);
    	m_dhcpBeginAddressField.addStyleName("kura-textfield");
    	m_dhcpBeginAddressField.addListener(Events.OnMouseOver, new MouseOverListener(MSGS.netRouterToolTipDhcpBeginAddr()));
        fieldSet.add(m_dhcpBeginAddressField, formData);

        //
        // DHCP Ending Address
        // 
        m_dhcpEndAddressField = new TextField<String>();
        m_dhcpEndAddressField.setAllowBlank(true);
        m_dhcpEndAddressField.setName("dhcpEndAddress");
        m_dhcpEndAddressField.setFieldLabel(MSGS.netRouterDhcpEndingAddress());
        m_dhcpEndAddressField.setRegex(IPV4_REGEX);
        m_dhcpEndAddressField.getMessages().setRegexText(MSGS.netIPv4InvalidAddress());
        m_dhcpEndAddressField.addListener(Events.OnMouseOver, new MouseOverListener(MSGS.netRouterToolTipDhcpEndAddr()));
        m_dhcpEndAddressField.addStyleName("kura-textfield");;
        m_dhcpEndAddressField.addPlugin(m_dirtyPlugin);
        fieldSet.add(m_dhcpEndAddressField, formData);

        //
        // DHCP Subnet Mask
        // 
        m_dhcpSubnetMaskField = new TextField<String>();
        m_dhcpSubnetMaskField.setAllowBlank(true);
        m_dhcpSubnetMaskField.setName("dhcpSubnetMask");
        m_dhcpSubnetMaskField.setFieldLabel(MSGS.netRouterDhcpSubnetMask());
        m_dhcpSubnetMaskField.setRegex(IPV4_REGEX);
        m_dhcpSubnetMaskField.addListener(Events.OnMouseOver, new MouseOverListener(MSGS.netRouterToolTipDhcpSubnet()));
    	m_dhcpSubnetMaskField.getMessages().setRegexText(MSGS.netIPv4InvalidAddress());
    	m_dhcpSubnetMaskField.addStyleName("kura-textfield");
    	m_dhcpSubnetMaskField.addPlugin(m_dirtyPlugin);
        fieldSet.add(m_dhcpSubnetMaskField, formData);

        //
        // DHCP Default Lease
        // 
        m_dhcpLeaseDefaultField = new NumberField();
    	m_dhcpLeaseDefaultField.setPropertyEditorType(Integer.class);
        m_dhcpLeaseDefaultField.setAllowDecimals(false);
        m_dhcpLeaseDefaultField.setAllowNegative(false);
        m_dhcpLeaseDefaultField.setMaxValue(Integer.MAX_VALUE);
        m_dhcpLeaseDefaultField.setAllowBlank(true);
        m_dhcpLeaseDefaultField.setName("dhcpDefaultLease");
    	m_dhcpLeaseDefaultField.setFieldLabel(MSGS.netRouterDhcpDefaultLease());
    	m_dhcpLeaseDefaultField.addListener(Events.OnMouseOver, new MouseOverListener(MSGS.netRouterToolTipDhcpDefaultLeaseTime()));
    	m_dhcpLeaseDefaultField.addPlugin(m_dirtyPlugin);
        fieldSet.add(m_dhcpLeaseDefaultField, formData);
        
        //
        // DHCP Max Lease
        // 
        m_dhcpLeaseMaxField = new NumberField();
        m_dhcpLeaseMaxField.setPropertyEditorType(Integer.class);
        m_dhcpLeaseMaxField.setAllowDecimals(false);
        m_dhcpLeaseMaxField.setAllowNegative(false);
        m_dhcpLeaseMaxField.setMaxValue(Integer.MAX_VALUE);
        m_dhcpLeaseMaxField.setAllowBlank(true);
        m_dhcpLeaseMaxField.setName("dhcpMaxLease");
        m_dhcpLeaseMaxField.setFieldLabel(MSGS.netRouterDhcpMaxLease());
        m_dhcpLeaseMaxField.addListener(Events.OnMouseOver, new MouseOverListener(MSGS.netRouterToolTipDhcpMaxLeaseTime()));
    	m_dhcpLeaseMaxField.addPlugin(m_dirtyPlugin);
        fieldSet.add(m_dhcpLeaseMaxField, formData);

        //
        // Pass DNS
        // 
    	m_passDnsRadioTrue = new Radio();  
        m_passDnsRadioTrue.setBoxLabel(MSGS.trueLabel());
        m_passDnsRadioTrue.setItemId("true");
      
        m_passDnsRadioFalse = new Radio();  
        m_passDnsRadioFalse.setBoxLabel(MSGS.falseLabel());  
        m_passDnsRadioFalse.setItemId("false");
      
        m_passDnsRadioGroup = new RadioGroup();
        m_passDnsRadioGroup.setName("dhcpPassDns");
        m_passDnsRadioGroup.setFieldLabel(MSGS.netRouterPassDns()); 
        m_passDnsRadioGroup.add(m_passDnsRadioTrue);  
        m_passDnsRadioGroup.add(m_passDnsRadioFalse);
        m_passDnsRadioGroup.addPlugin(m_dirtyPlugin);  
        m_passDnsRadioGroup.addListener(Events.OnMouseOver, new MouseOverListener(MSGS.netRouterToolTipPassDns()));
        fieldSet.add(m_passDnsRadioGroup, formData);

	    m_formPanel.add(fieldSet);
	    add(m_formPanel);
	    setScrollMode(Scroll.AUTO);
	    m_initialized = true;
	}
	    
	public void refresh() 
	{
		Log.debug("DhcpNatConfigTab.refresh()");
		if (m_dirty && m_initialized) {			
	        m_dirty = false;
			if (m_selectNetIfConfig == null) {
				Log.debug("DhcpNatConfigTab.refresh() - resetting"); 
				reset();
	        } else {
	        	Log.debug("DhcpNatConfigTab.refresh() - updating");
				update();				
			}		
		}
	}	
	
	private void update()
	{
		if (m_selectNetIfConfig != null) {
	    	Log.debug("in update(): got new netIfConfig for DHCP server for " + m_selectNetIfConfig.getName() + ": " +
	       			"\n\t\trouter mode: " + m_selectNetIfConfig.getRouterMode() +
	       			"\n\t\trouter DHCP start address: " + m_selectNetIfConfig.getRouterDhcpBeginAddress() +
	       			"\n\t\trouter DHCP end address: " + m_selectNetIfConfig.getRouterDhcpEndAddress() +
	       			"\n\t\trouter default lease: " + Integer.toString(m_selectNetIfConfig.getRouterDhcpDefaultLease()) +
	       			"\n\t\trouter max lease: " + Integer.toString(m_selectNetIfConfig.getRouterDhcpMaxLease()) +
	       			"\n\t\trouter Pass DNS: " + Boolean.toString(m_selectNetIfConfig.getRouterDnsPass()));
			
			m_modeCombo.setSimpleValue(MessageUtils.get(m_selectNetIfConfig.getRouterMode()));
			m_modeCombo.setOriginalValue(m_modeCombo.getValue());
			
			m_dhcpBeginAddressField.setValue(m_selectNetIfConfig.getRouterDhcpBeginAddress());
			m_dhcpBeginAddressField.setOriginalValue(m_dhcpBeginAddressField.getValue());
            
            m_dhcpEndAddressField.setValue(m_selectNetIfConfig.getRouterDhcpEndAddress());
            m_dhcpEndAddressField.setOriginalValue(m_dhcpEndAddressField.getValue());
			
			m_dhcpSubnetMaskField.setValue(m_selectNetIfConfig.getRouterDhcpSubnetMask());
			m_dhcpSubnetMaskField.setOriginalValue(m_dhcpSubnetMaskField.getValue());
			
			m_dhcpLeaseDefaultField.setValue(m_selectNetIfConfig.getRouterDhcpDefaultLease());
			m_dhcpLeaseDefaultField.setOriginalValue(m_dhcpLeaseDefaultField.getValue());
			
			m_dhcpLeaseMaxField.setValue(m_selectNetIfConfig.getRouterDhcpMaxLease());
			m_dhcpLeaseMaxField.setOriginalValue(m_dhcpLeaseMaxField.getValue());
			
			if (m_selectNetIfConfig.getRouterDnsPass()) {
				m_passDnsRadioTrue.setValue(true);
				m_passDnsRadioTrue.setOriginalValue(m_passDnsRadioTrue.getValue());
				
				m_passDnsRadioFalse.setValue(false);
				m_passDnsRadioFalse.setOriginalValue(m_passDnsRadioFalse.getValue());
				
				m_passDnsRadioGroup.setOriginalValue(m_passDnsRadioTrue);
				m_passDnsRadioGroup.setValue(m_passDnsRadioGroup.getValue());
			}
			else {
				m_passDnsRadioTrue.setValue(false);
				m_passDnsRadioTrue.setOriginalValue(m_passDnsRadioTrue.getValue());

				m_passDnsRadioFalse.setValue(true);
				m_passDnsRadioFalse.setOriginalValue(m_passDnsRadioFalse.getValue());

				m_passDnsRadioGroup.setOriginalValue(m_passDnsRadioFalse);
				m_passDnsRadioGroup.setValue(m_passDnsRadioGroup.getValue());
			}
		} else {
			Log.debug("selected Network Interface Config is null");
		}

        for (Field<?> field : m_formPanel.getFields()) {
            FormUtils.removeDirtyFieldIcon(field);
        }

		refreshForm();
	}
	
	
	private void refreshForm() {

	    if (m_formPanel != null) {		
	  		if (!m_tcpIpConfigTab.isLanEnabled()) {
				for (Field<?> field : m_formPanel.getFields()) {			
					field.setEnabled(false);
				}
	  		}
	  		else {
	  			GwtWifiWirelessMode wirelessMode = m_wirelessConfigTab.getWirelessMode();
	  			if (m_selectNetIfConfig.getHwTypeEnum() == GwtNetIfType.WIFI && (wirelessMode == GwtWifiWirelessMode.netWifiWirelessModeStation ||
	  					wirelessMode == GwtWifiWirelessMode.netWifiWirelessModeDisabled)) 
	  			{
					for (Field<?> field : m_formPanel.getFields()) {			
						field.setEnabled(false);
					}	  				
	  			} else {
		
					for (Field<?> field : m_formPanel.getFields()) {			
						field.setEnabled(true);
						field.validate();
					}
					String modeValue = m_modeCombo.getValue().getValue();
					if (modeValue == MessageUtils.get(GwtNetRouterMode.netRouterNat.name()) ||
					        modeValue == MessageUtils.get(GwtNetRouterMode.netRouterOff.name()))
					{
						for (Field<?> field : m_formPanel.getFields()) {			
							if (field != m_modeCombo) {
								field.setEnabled(false);
							}
						}
					}
					else {
						for (Field<?> field : m_formPanel.getFields()) {			
							if (field != m_modeCombo) {
								field.setEnabled(true);
							}
						}
					}
	  			}
	  		}
		}
	}
	
	
	private void reset()
	{
		Log.debug("DhcpNatConfigTab: reset()");
		m_modeCombo.setSimpleValue(MessageUtils.get(GwtNetRouterMode.netRouterOff.name()));
		m_modeCombo.setOriginalValue(m_modeCombo.getValue());
		
		m_dhcpBeginAddressField.setValue("");
		m_dhcpBeginAddressField.setOriginalValue(m_dhcpBeginAddressField.getValue());
        
        m_dhcpEndAddressField.setValue("");
        m_dhcpEndAddressField.setOriginalValue(m_dhcpEndAddressField.getValue());
		
		m_dhcpSubnetMaskField.setValue("");
		m_dhcpSubnetMaskField.setOriginalValue(m_dhcpSubnetMaskField.getValue());
		
		m_dhcpLeaseDefaultField.setValue(0);
		m_dhcpLeaseDefaultField.setOriginalValue(m_dhcpLeaseDefaultField.getValue());
		
		m_dhcpLeaseMaxField.setValue(0);
		m_dhcpLeaseMaxField.setOriginalValue(m_dhcpLeaseMaxField.getValue());
				
		m_passDnsRadioTrue.setValue(false);
		m_passDnsRadioTrue.setOriginalValue(m_passDnsRadioTrue.getValue());
		
		m_passDnsRadioFalse.setValue(true);
		m_passDnsRadioFalse.setOriginalValue(m_passDnsRadioFalse.getValue());
		
		m_passDnsRadioGroup.setValue(m_passDnsRadioFalse);
		m_passDnsRadioGroup.setOriginalValue(m_passDnsRadioGroup.getValue());

		update();
	}
}
