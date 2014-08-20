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

import org.eclipse.kura.web.client.messages.Messages;
import org.eclipse.kura.web.client.util.TextFieldValidator;
import org.eclipse.kura.web.client.util.TextFieldValidator.FieldType;
import org.eclipse.kura.web.shared.model.GwtFirewallOpenPortEntry;
import org.eclipse.kura.web.shared.model.GwtNetProtocol;
import org.eclipse.kura.web.shared.model.GwtSession;

import com.allen_sauer.gwt.log.client.Log;
import com.extjs.gxt.ui.client.Style.HorizontalAlignment;
import com.extjs.gxt.ui.client.Style.Scroll;
import com.extjs.gxt.ui.client.event.ButtonEvent;
import com.extjs.gxt.ui.client.event.Events;
import com.extjs.gxt.ui.client.event.FieldEvent;
import com.extjs.gxt.ui.client.event.Listener;
import com.extjs.gxt.ui.client.event.SelectionListener;
import com.extjs.gxt.ui.client.widget.Status;
import com.extjs.gxt.ui.client.widget.Window;
import com.extjs.gxt.ui.client.widget.button.Button;
import com.extjs.gxt.ui.client.widget.form.ComboBox.TriggerAction;
import com.extjs.gxt.ui.client.widget.form.Field;
import com.extjs.gxt.ui.client.widget.form.FieldSet;
import com.extjs.gxt.ui.client.widget.form.FormPanel;
import com.extjs.gxt.ui.client.widget.form.LabelField;
import com.extjs.gxt.ui.client.widget.form.SimpleComboBox;
import com.extjs.gxt.ui.client.widget.form.TextField;
import com.extjs.gxt.ui.client.widget.layout.FitLayout;
import com.extjs.gxt.ui.client.widget.layout.FlowLayout;
import com.extjs.gxt.ui.client.widget.layout.FormData;
import com.extjs.gxt.ui.client.widget.layout.FormLayout;
import com.extjs.gxt.ui.client.widget.toolbar.FillToolItem;
import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.Element;

public class OpenPortForm extends Window {

	private static final Messages MSGS = GWT.create(Messages.class);

    private static final int  LABEL_WIDTH_FORM = 190; 
    
    private GwtSession   				m_currentSession;
    private GwtFirewallOpenPortEntry	m_newOpenPortEntry;
    private GwtFirewallOpenPortEntry	m_existingOpenPortEntry;
    private FormPanel    				m_formPanel;
    private Status 					 	m_status;
    private TextField<String> 			m_permittedInterfaceName;
    private TextField<String> 			m_unpermittedInterfaceName;
    private boolean						m_isCanceled;
    
    public OpenPortForm(GwtSession session) {
        
    	m_currentSession = session;
    	m_existingOpenPortEntry = null;
    	
        setModal(true);
        setSize(600, 500);
        setLayout(new FitLayout());
        setResizable(false);
        String heading = MSGS.firewallOpenPortFormNew();
        setHeading(heading);
    }
    
    public OpenPortForm(GwtSession session, GwtFirewallOpenPortEntry existingEntry) {
        this(session);
        m_existingOpenPortEntry = existingEntry;
        if (m_existingOpenPortEntry != null) {
            setHeading(MSGS.firewallOpenPortFormUpdate(m_existingOpenPortEntry.getPort().toString()));
        }
    }
    
    public GwtFirewallOpenPortEntry getNewOpenPortEntry() {
    	return m_newOpenPortEntry;
    }

    public GwtFirewallOpenPortEntry getExistingOpenPortEntry() {
    	return m_existingOpenPortEntry;
    }
    
    public boolean isCanceled() {
    	return m_isCanceled;
    }

    protected void onRender(Element parent, int index) {
        
    	super.onRender(parent, index);
    	setId("firewall-open-port-form-wrapper");
    	
        FormData formData = new FormData("-30");

        m_formPanel = new FormPanel();
        m_formPanel.setFrame(false);
        m_formPanel.setBodyBorder(true);
        m_formPanel.setHeaderVisible(false);
        m_formPanel.setScrollMode(Scroll.AUTOY);
        m_formPanel.setLayout(new FlowLayout());

        FieldSet fieldSet = new FieldSet();
        fieldSet.setHeading(MSGS.firewallOpenPortFormInformation());
        FormLayout layoutAccount = new FormLayout();
        layoutAccount.setLabelWidth(LABEL_WIDTH_FORM);
        fieldSet.setLayout(layoutAccount);
        
        //
    	// port number
        //
        final LabelField portLabel = new LabelField();
        portLabel.setName("portLabel");
        portLabel.setFieldLabel(MSGS.firewallOpenPortFormPort());
        portLabel.setLabelSeparator(":");
        fieldSet.add(portLabel, formData);

        final TextField<String> portField = new TextField<String>();
        portField.setAllowBlank(false);
        portField.setName("port");
        portField.setFieldLabel(MSGS.firewallOpenPortFormPort());
        portField.setValidator(new TextFieldValidator(portField, FieldType.NUMERIC));
        fieldSet.add(portField, formData);
        
        //
    	// protocol
        //
        final LabelField protocolLabel = new LabelField();
        protocolLabel.setName("protocolLabel");
        protocolLabel.setFieldLabel(MSGS.firewallOpenPortFormProtocol());
        protocolLabel.setLabelSeparator(":");
        fieldSet.add(protocolLabel, formData);

        final SimpleComboBox<String> protocolCombo = new SimpleComboBox<String>();
        protocolCombo.setName("protocolCombo");
        protocolCombo.setFieldLabel(MSGS.firewallOpenPortFormProtocol());
        protocolCombo.setEditable(false);
        protocolCombo.setTypeAhead(true);  
        protocolCombo.setTriggerAction(TriggerAction.ALL);
        for (GwtNetProtocol protocol : GwtNetProtocol.values()) {
        	protocolCombo.add(protocol.name());
        }
        protocolCombo.setSimpleValue(GwtNetProtocol.tcp.name());
        fieldSet.add(protocolCombo, formData);
        
        //
    	// permitted network
        //        
        final TextField<String> permittedNetworkField = new TextField<String>();
        permittedNetworkField.setAllowBlank(true);
        permittedNetworkField.setName("permittedNetwork");
        permittedNetworkField.setFieldLabel(MSGS.firewallOpenPortFormPermittedNetwork());
        permittedNetworkField.setValidator(new TextFieldValidator(permittedNetworkField, FieldType.NETWORK));
        fieldSet.add(permittedNetworkField, formData);
        
        //
    	// permitted network interface
        //        
        m_permittedInterfaceName = new TextField<String>();
        m_permittedInterfaceName.setAllowBlank(true);
        m_permittedInterfaceName.setName("permittedInterfaceName");
        m_permittedInterfaceName.setFieldLabel(MSGS.firewallOpenPortFormPermittedInterfaceName());
        m_permittedInterfaceName.setValidator(new TextFieldValidator(m_permittedInterfaceName, FieldType.ALPHANUMERIC));
        m_permittedInterfaceName.setToolTip(MSGS.firewallOpenPortFormPermittedInterfaceToolTip());
        m_permittedInterfaceName.addListener(Events.Change, new Listener<FieldEvent>() {
			public void handleEvent(FieldEvent be) {
				if (be.getValue() != null) {
					m_unpermittedInterfaceName.disable();
				}
				else {
					m_unpermittedInterfaceName.enable();
				}
			}
        });
        fieldSet.add(m_permittedInterfaceName, formData);
        
        //
    	// unpermitted network interface
        //        
        m_unpermittedInterfaceName = new TextField<String>();
        m_unpermittedInterfaceName.setAllowBlank(true);
        m_unpermittedInterfaceName.setName("unpermittedInterfaceName");
        m_unpermittedInterfaceName.setFieldLabel(MSGS.firewallOpenPortFormUnpermittedInterfaceName());
        m_unpermittedInterfaceName.setValidator(new TextFieldValidator(m_unpermittedInterfaceName, FieldType.ALPHANUMERIC));
        m_unpermittedInterfaceName.setToolTip(MSGS.firewallOpenPortFormUnpermittedInterfaceToolTip());
        m_unpermittedInterfaceName.addListener(Events.Change, new Listener<FieldEvent>() {
			public void handleEvent(FieldEvent be) {
				if (be.getValue() != null) {
					m_permittedInterfaceName.disable();
				}
				else {
					m_permittedInterfaceName.enable();
				}
			}
        });
        fieldSet.add(m_unpermittedInterfaceName, formData);
        
        
        //
    	// permitted MAC
        //
        final TextField<String> permittedMacField = new TextField<String>();
        permittedMacField.setAllowBlank(true);
        permittedMacField.setName("permittedMac");
        permittedMacField.setFieldLabel(MSGS.firewallOpenPortFormPermittedMac());
        permittedMacField.setValidator(new TextFieldValidator(permittedMacField, FieldType.MAC_ADDRESS));
        fieldSet.add(permittedMacField, formData);
        
        //
    	// source port range
        //
        final TextField<String> sourcePortRangeField = new TextField<String>();
        sourcePortRangeField.setAllowBlank(true);
        sourcePortRangeField.setName("sourcePortRange");
        sourcePortRangeField.setFieldLabel(MSGS.firewallOpenPortFormSourcePortRange());
        sourcePortRangeField.setValidator(new TextFieldValidator(sourcePortRangeField, FieldType.PORT_RANGE));
        fieldSet.add(sourcePortRangeField, formData);
        
        //add the fieldSet to the panel
        m_formPanel.add(fieldSet);
    	
        //disable the labels
    	portLabel.setVisible(false);
    	protocolLabel.setVisible(false);
    	
		m_status = new Status();
		m_status.setBusy(MSGS.waitMsg());
		m_status.hide();
		m_status.setAutoWidth(true);
		
		m_formPanel.setButtonAlign(HorizontalAlignment.LEFT);
		m_formPanel.getButtonBar().add(m_status);
		m_formPanel.getButtonBar().add(new FillToolItem());

        m_formPanel.addButton(new Button(MSGS.submitButton(), new SelectionListener<ButtonEvent>() {
            @Override
            public void componentSelected(ButtonEvent ce) {

            	// make sure all visible fields are valid before performing the action
            	for (Field<?> field : m_formPanel.getFields()) {
            		if (field.isVisible() && !field.isValid()) {
                		return;
            		}
            	}
            	
            	Log.debug("Open port fields are visible and valid...");

            	//we need to add a new row to the open ports table
            	if(m_existingOpenPortEntry == null) {
            		//create a new entry
            		m_newOpenPortEntry = new GwtFirewallOpenPortEntry();
            		m_newOpenPortEntry.setPort(Integer.parseInt(portField.getValue()));
            		m_newOpenPortEntry.setProtocol(protocolCombo.getValue().getValue());
            		if(permittedNetworkField.getValue() != null) {
            			m_newOpenPortEntry.setPermittedNetwork(permittedNetworkField.getValue());
            		}
            		else {
            			m_newOpenPortEntry.setPermittedNetwork("0.0.0.0/0");
            		}
            		if(m_permittedInterfaceName.getValue() != null) {
            			m_newOpenPortEntry.setPermittedInterfaceName(m_permittedInterfaceName.getValue());
            		}
            		if(m_unpermittedInterfaceName.getValue() != null) {
            			m_newOpenPortEntry.setUnpermittedInterfaceName(m_unpermittedInterfaceName.getValue());
            		}
            		if(permittedMacField.getValue() != null) {
            			m_newOpenPortEntry.setPermittedMAC(permittedMacField.getValue());
            		}
            		if(sourcePortRangeField.getValue() != null) {
            			m_newOpenPortEntry.setSourcePortRange(sourcePortRangeField.getValue());
            		}
            	} else {
            		//update the current entry
            		m_existingOpenPortEntry = new GwtFirewallOpenPortEntry();
            		m_existingOpenPortEntry.setPort(Integer.parseInt(portField.getValue()));
            		m_existingOpenPortEntry.setProtocol(protocolCombo.getValue().getValue());
            		if(permittedNetworkField.getValue() != null) {
            			m_existingOpenPortEntry.setPermittedNetwork(permittedNetworkField.getValue());
            		}
            		else {
            			m_existingOpenPortEntry.setPermittedNetwork("0.0.0.0/0");
            		}
            		if(m_permittedInterfaceName.getValue() != null) {
            			m_existingOpenPortEntry.setPermittedInterfaceName(m_permittedInterfaceName.getValue());
            		}
            		if(m_unpermittedInterfaceName.getValue() != null) {
            			m_existingOpenPortEntry.setUnpermittedInterfaceName(m_unpermittedInterfaceName.getValue());
            		}
            		if(permittedMacField.getValue() != null) {
            			m_existingOpenPortEntry.setPermittedMAC(permittedMacField.getValue());
            		}
            		if(sourcePortRangeField.getValue() != null) {
            			m_existingOpenPortEntry.setSourcePortRange(sourcePortRangeField.getValue());
            		}
            	}
            	m_isCanceled = false;
            	hide();
            }
        }));
        
        m_formPanel.addButton(new Button(MSGS.cancelButton(), new SelectionListener<ButtonEvent>() {
            @Override
            public void componentSelected(ButtonEvent ce) {
            	m_isCanceled = true;
                hide();
            }

        }));
        m_formPanel.setButtonAlign(HorizontalAlignment.CENTER);

        // populate if necessary
        if (m_existingOpenPortEntry != null) {
        	
        	portLabel.setValue(m_existingOpenPortEntry.getPort());
        	portField.setValue(m_existingOpenPortEntry.getPort().toString());
        	portField.setOriginalValue(m_existingOpenPortEntry.getPort().toString());
        	
        	protocolLabel.setValue(m_existingOpenPortEntry.getProtocol());
        	protocolCombo.setSimpleValue(m_existingOpenPortEntry.getProtocol());
        
        	permittedNetworkField.setValue(m_existingOpenPortEntry.getPermittedNetwork());
        	permittedNetworkField.setOriginalValue(m_existingOpenPortEntry.getPermittedNetwork());
        	
        	m_permittedInterfaceName.setValue(m_existingOpenPortEntry.getPermittedInterfaceName());
        	m_permittedInterfaceName.setOriginalValue(m_existingOpenPortEntry.getPermittedInterfaceName());
        	
        	m_unpermittedInterfaceName.setValue(m_existingOpenPortEntry.getUnpermittedInterfaceName());
        	m_unpermittedInterfaceName.setOriginalValue(m_existingOpenPortEntry.getUnpermittedInterfaceName());
        	
           	permittedMacField.setValue(m_existingOpenPortEntry.getPermittedMAC());
           	permittedMacField.setOriginalValue(m_existingOpenPortEntry.getPermittedMAC());
           	
        	sourcePortRangeField.setValue(m_existingOpenPortEntry.getSourcePortRange());
        	sourcePortRangeField.setOriginalValue(m_existingOpenPortEntry.getSourcePortRange());
        }
        
        add(m_formPanel);
    }
}
