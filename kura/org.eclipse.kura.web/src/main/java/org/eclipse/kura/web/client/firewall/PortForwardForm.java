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
import org.eclipse.kura.web.shared.model.GwtFirewallPortForwardEntry;
import org.eclipse.kura.web.shared.model.GwtNetProtocol;
import org.eclipse.kura.web.shared.model.GwtSession;

import com.extjs.gxt.ui.client.Style.HorizontalAlignment;
import com.extjs.gxt.ui.client.Style.Scroll;
import com.extjs.gxt.ui.client.event.ButtonEvent;
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

public class PortForwardForm extends Window {

	private static final Messages MSGS = GWT.create(Messages.class);

    private static final int  LABEL_WIDTH_FORM = 190; 
    
    private GwtSession   				m_currentSession;
    private GwtFirewallPortForwardEntry	m_newEntry;
    private GwtFirewallPortForwardEntry	m_existingEntry;
    private FormPanel    				m_formPanel;
    private Status 					 	m_status;
    private boolean						m_isCanceled;
    
    public PortForwardForm(GwtSession session) {
        
    	m_currentSession = session;
    	m_existingEntry = null;
    	
        setModal(true);
        setSize(600, 500);
        setLayout(new FitLayout());
        setResizable(false);
        String heading = MSGS.firewallOpenPortFormNew();
        setHeading(heading);
    }
    
    public PortForwardForm(GwtSession session, GwtFirewallPortForwardEntry existingEntry) {
        this(session);
        m_existingEntry = existingEntry;
        if (m_existingEntry != null) {
            setHeading(MSGS.firewallPortForwardFormUpdate(m_existingEntry.getInPort().toString()));
        }
    }
    
    public GwtFirewallPortForwardEntry getNewPortForwardEntry() {
    	return m_newEntry;
    }

    public GwtFirewallPortForwardEntry getExistingPortForwardEntry() {
    	return m_existingEntry;
    }
    
    public boolean isCanceled() {
    	return m_isCanceled;
    }

    protected void onRender(Element parent, int index) {
        
    	super.onRender(parent, index);
    	setId("firewall-port-forwarding-form");
    	
        FormData formData = new FormData("-30");

        m_formPanel = new FormPanel();
        m_formPanel.setFrame(false);
        m_formPanel.setBodyBorder(true);
        m_formPanel.setHeaderVisible(false);
        m_formPanel.setScrollMode(Scroll.AUTOY);
        m_formPanel.setLayout(new FlowLayout());

        FieldSet fieldSet = new FieldSet();
        fieldSet.setHeading(MSGS.firewallPortForwardFormInformation());
        FormLayout layoutAccount = new FormLayout();
        layoutAccount.setLabelWidth(LABEL_WIDTH_FORM);
        fieldSet.setLayout(layoutAccount);
        
        //
    	// interface name
        //
        final LabelField interfaceNameLabel = new LabelField();
        interfaceNameLabel.setName("interfaceNameLabel");
        interfaceNameLabel.setFieldLabel(MSGS.firewallPortForwardFormInterfaceName());
        interfaceNameLabel.setLabelSeparator(":");
        fieldSet.add(interfaceNameLabel, formData);

        final TextField<String> interfaceNameField = new TextField<String>();
        interfaceNameField.setAllowBlank(false);
        interfaceNameField.setName("interfaceName");
        interfaceNameField.setFieldLabel(MSGS.firewallPortForwardFormInterfaceName());
        interfaceNameField.setValidator(new TextFieldValidator(interfaceNameField, FieldType.ALPHANUMERIC));
        fieldSet.add(interfaceNameField, formData);
        
        //
    	// address
        //
        final LabelField addressLabel = new LabelField();
        addressLabel.setName("addressLabel");
        addressLabel.setFieldLabel(MSGS.firewallPortForwardFormAddress());
        addressLabel.setLabelSeparator(":");
        fieldSet.add(addressLabel, formData);

        final TextField<String> addressField = new TextField<String>();
        addressField.setAllowBlank(false);
        addressField.setName("address");
        addressField.setFieldLabel(MSGS.firewallPortForwardFormAddress());
        addressField.setValidator(new TextFieldValidator(addressField, FieldType.IPv4_ADDRESS));
        fieldSet.add(addressField, formData);

        //
    	// protocol
        //
        final LabelField protocolLabel = new LabelField();
        protocolLabel.setName("protocolLabel");
        protocolLabel.setFieldLabel(MSGS.firewallPortForwardFormProtocol());
        protocolLabel.setLabelSeparator(":");
        fieldSet.add(protocolLabel, formData);

        final SimpleComboBox<String> protocolCombo = new SimpleComboBox<String>();
        protocolCombo.setName("protocolCombo");
        protocolCombo.setFieldLabel(MSGS.firewallPortForwardFormProtocol());
        protocolCombo.setEditable(false);
        protocolCombo.setTypeAhead(true);  
        protocolCombo.setTriggerAction(TriggerAction.ALL);
        for (GwtNetProtocol protocol : GwtNetProtocol.values()) {
        	protocolCombo.add(protocol.name());
        }
        protocolCombo.setSimpleValue(GwtNetProtocol.tcp.name());
        fieldSet.add(protocolCombo, formData);
        
        //
    	// in port number
        //
        final LabelField inPortLabel = new LabelField();
        inPortLabel.setName("inPortLabel");
        inPortLabel.setFieldLabel(MSGS.firewallPortForwardFormInPort());
        inPortLabel.setLabelSeparator(":");
        fieldSet.add(inPortLabel, formData);

        final TextField<String> inPortField = new TextField<String>();
        inPortField.setAllowBlank(false);
        inPortField.setName("inPort");
        inPortField.setFieldLabel(MSGS.firewallPortForwardFormInPort());
        inPortField.setValidator(new TextFieldValidator(inPortField, FieldType.NUMERIC));
        fieldSet.add(inPortField, formData);
        
        //
    	// out port number
        //
        final TextField<String> outPortField = new TextField<String>();
        outPortField.setAllowBlank(false);
        outPortField.setName("outPort");
        outPortField.setFieldLabel(MSGS.firewallPortForwardFormOutPort());
        outPortField.setValidator(new TextFieldValidator(outPortField, FieldType.NUMERIC));
        fieldSet.add(outPortField, formData);
        
        //
    	// permitted network
        //        
        final TextField<String> permittedNetworkField = new TextField<String>();
        permittedNetworkField.setAllowBlank(true);
        permittedNetworkField.setName("permittedNetwork");
        permittedNetworkField.setFieldLabel(MSGS.firewallPortForwardFormPermittedNetwork());
        permittedNetworkField.setValidator(new TextFieldValidator(permittedNetworkField, FieldType.NETWORK));
        fieldSet.add(permittedNetworkField, formData);
        
        //
    	// permitted MAC
        //
        final TextField<String> permittedMacField = new TextField<String>();
        permittedMacField.setAllowBlank(true);
        permittedMacField.setName("permittedMac");
        permittedMacField.setFieldLabel(MSGS.firewallPortForwardFormPermittedMac());
        permittedMacField.setValidator(new TextFieldValidator(permittedMacField, FieldType.MAC_ADDRESS));
        fieldSet.add(permittedMacField, formData);
        
        //
    	// source port range
        //
        final TextField<String> sourcePortRangeField = new TextField<String>();
        sourcePortRangeField.setAllowBlank(true);
        sourcePortRangeField.setName("sourcePortRange");
        sourcePortRangeField.setFieldLabel(MSGS.firewallPortForwardFormSourcePortRange());
        sourcePortRangeField.setValidator(new TextFieldValidator(sourcePortRangeField, FieldType.PORT_RANGE));
        fieldSet.add(sourcePortRangeField, formData);
        
        //add the fieldSet to the panel
        m_formPanel.add(fieldSet);
    	
        //disable the labels
    	interfaceNameLabel.setVisible(false);
    	addressLabel.setVisible(false);
    	protocolLabel.setVisible(false);
    	inPortLabel.setVisible(false);
    	
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

            	//we need to add a new row to the open ports table
            	if(m_existingEntry == null) {
            		//create a new entry
            		m_newEntry = new GwtFirewallPortForwardEntry();
            		m_newEntry.setInterfaceName(interfaceNameField.getValue());
            		m_newEntry.setAddress(addressField.getValue());
            		m_newEntry.setProtocol(protocolCombo.getValue().getValue());
            		m_newEntry.setInPort(Integer.parseInt(inPortField.getValue()));
            		m_newEntry.setOutPort(Integer.parseInt(outPortField.getValue()));
            		m_newEntry.setPermittedNetwork(permittedNetworkField.getValue());
            		m_newEntry.setPermittedMAC(permittedMacField.getValue());
            		m_newEntry.setSourcePortRange(sourcePortRangeField.getValue());
            		
            		if (m_newEntry.getPermittedMAC() != null) {
                		MessageBox.alert(MSGS.firewallPortForwardFormNotification(), MSGS.firewallPortForwardFormNotificationMacFiltering(), null);
                	}
            	} else {
            		//update the current entry
            		m_existingEntry = new GwtFirewallPortForwardEntry();
            		m_existingEntry.setInterfaceName(interfaceNameField.getValue());
            		m_existingEntry.setAddress(addressField.getValue());
            		m_existingEntry.setProtocol(protocolCombo.getValue().getValue());
            		m_existingEntry.setInPort(Integer.parseInt(inPortField.getValue()));
            		m_existingEntry.setOutPort(Integer.parseInt(outPortField.getValue()));
            		m_existingEntry.setPermittedNetwork(permittedNetworkField.getValue());
            		m_existingEntry.setPermittedMAC(permittedMacField.getValue());
            		m_existingEntry.setSourcePortRange(sourcePortRangeField.getValue());
            		
            		if (m_existingEntry.getPermittedMAC() != null) {
                		MessageBox.alert(MSGS.firewallPortForwardFormNotification(), MSGS.firewallPortForwardFormNotificationMacFiltering(), null);
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
        if (m_existingEntry != null) {
        	
        	interfaceNameLabel.setValue(m_existingEntry.getInterfaceName());
        	interfaceNameField.setValue(m_existingEntry.getInterfaceName());
        	interfaceNameField.setOriginalValue(m_existingEntry.getInterfaceName());
        	
        	addressLabel.setValue(m_existingEntry.getAddress());
        	addressField.setValue(m_existingEntry.getAddress());
        	addressField.setOriginalValue(m_existingEntry.getAddress());
        	
        	protocolLabel.setValue(m_existingEntry.getProtocol());
        	protocolCombo.setSimpleValue(m_existingEntry.getProtocol());
        	
        	inPortLabel.setValue(m_existingEntry.getInPort());
        	inPortField.setValue(m_existingEntry.getInPort().toString());
        	inPortField.setOriginalValue(m_existingEntry.getInPort().toString());
        	
        	outPortField.setValue(m_existingEntry.getOutPort().toString());
        	outPortField.setOriginalValue(m_existingEntry.getOutPort().toString());
        
        	permittedNetworkField.setValue(m_existingEntry.getPermittedNetwork());
        	permittedNetworkField.setOriginalValue(m_existingEntry.getPermittedNetwork());
        	
           	permittedMacField.setValue(m_existingEntry.getPermittedMAC());
           	permittedMacField.setOriginalValue(m_existingEntry.getPermittedMAC());
           	
        	sourcePortRangeField.setValue(m_existingEntry.getSourcePortRange());
        	sourcePortRangeField.setOriginalValue(m_existingEntry.getSourcePortRange());
        }
        
        add(m_formPanel);
    }
}
