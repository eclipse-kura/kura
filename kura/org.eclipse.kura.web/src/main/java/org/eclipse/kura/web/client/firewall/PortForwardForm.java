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
import org.eclipse.kura.web.shared.model.GwtFirewallNatMasquerade;
import org.eclipse.kura.web.shared.model.GwtFirewallPortForwardEntry;
import org.eclipse.kura.web.shared.model.GwtNetProtocol;
import org.eclipse.kura.web.shared.model.GwtSession;

import com.extjs.gxt.ui.client.Style.HorizontalAlignment;
import com.extjs.gxt.ui.client.Style.Scroll;
import com.extjs.gxt.ui.client.event.ButtonEvent;
import com.extjs.gxt.ui.client.event.SelectionListener;
import com.extjs.gxt.ui.client.widget.MessageBox;
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
    
    private GwtFirewallPortForwardEntry	m_newEntry;
    private GwtFirewallPortForwardEntry	m_existingEntry;
    private FormPanel    				m_formPanel;
    private Status 					 	m_status;
    private boolean						m_isCanceled;
    
    public PortForwardForm(GwtSession session) {
        
    	m_existingEntry = null;
    	
        setModal(true);
        setSize(600, 500);
        setLayout(new FitLayout());
        setResizable(false);
        String heading = MSGS.firewallPortForwardFormNew();
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
    	// in-bound interface
        //
        final LabelField inboundInterfaceLabel = new LabelField();
        inboundInterfaceLabel.setName("inboundInterfaceLabel");
        inboundInterfaceLabel.setFieldLabel(MSGS.firewallPortForwardFormInboundInterface());
        inboundInterfaceLabel.setLabelSeparator(":");
        fieldSet.add(inboundInterfaceLabel, formData);

        final TextField<String> inboundInterfaceField = new TextField<String>();
        inboundInterfaceField.setAllowBlank(false);
        inboundInterfaceField.setName("interfaceName");
        inboundInterfaceField.setFieldLabel(MSGS.firewallPortForwardFormInboundInterface());
        inboundInterfaceField.setToolTip(MSGS.firewallPortForwardFormInboundInterfaceToolTip());
        inboundInterfaceField.setValidator(new TextFieldValidator(inboundInterfaceField, FieldType.ALPHANUMERIC));
        fieldSet.add(inboundInterfaceField, formData);
        
        //
    	// out-bound interface
        //
        final LabelField outboundInterfaceLabel = new LabelField();
        outboundInterfaceLabel.setName("inboundInterfaceLabel");
        outboundInterfaceLabel.setFieldLabel(MSGS.firewallPortForwardFormOutboundInterface());
        outboundInterfaceLabel.setLabelSeparator(":");
        fieldSet.add(outboundInterfaceLabel, formData);

        final TextField<String> outboundInterfaceField = new TextField<String>();
        outboundInterfaceField.setAllowBlank(false);
        outboundInterfaceField.setName("interfaceName");
        outboundInterfaceField.setFieldLabel(MSGS.firewallPortForwardFormOutboundInterface());
        outboundInterfaceField.setToolTip(MSGS.firewallPortForwardFormOutboundInterfaceToolTip());
        outboundInterfaceField.setValidator(new TextFieldValidator(outboundInterfaceField, FieldType.ALPHANUMERIC));
        fieldSet.add(outboundInterfaceField, formData);
        
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
        addressField.setToolTip(MSGS.firewallPortForwardFormLanAddressToolTip());
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
        protocolCombo.setToolTip(MSGS.firewallPortForwardFormProtocolToolTip());
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
        inPortField.setToolTip(MSGS.firewallPortForwardFormExternalPortToolTip());
        inPortField.setValidator(new TextFieldValidator(inPortField, FieldType.NUMERIC));
        fieldSet.add(inPortField, formData);
        
        //
    	// out port number
        //
        final LabelField outPortLabel = new LabelField();
        outPortLabel.setName("outPortLabel");
        outPortLabel.setFieldLabel(MSGS.firewallPortForwardFormOutPort());
        outPortLabel.setLabelSeparator(":");
        fieldSet.add(outPortLabel, formData);
        
        final TextField<String> outPortField = new TextField<String>();
        outPortField.setAllowBlank(false);
        outPortField.setName("outPort");
        outPortField.setFieldLabel(MSGS.firewallPortForwardFormOutPort());
        outPortField.setToolTip(MSGS.firewallPortForwardFormInternalPortToolTip());
        outPortField.setValidator(new TextFieldValidator(outPortField, FieldType.NUMERIC));
        fieldSet.add(outPortField, formData);
        
        //
    	// masquerade
        //
        final LabelField masqueradeLabel = new LabelField();
        masqueradeLabel.setName("masqueradeLabel");
        masqueradeLabel.setFieldLabel(MSGS.firewallNatFormMasquerade());
        masqueradeLabel.setLabelSeparator(":");
        fieldSet.add(masqueradeLabel, formData);
        
        final SimpleComboBox<String> masqueradeCombo = new SimpleComboBox<String>();
        masqueradeCombo.setName("masqueradeCombo");
        masqueradeCombo.setFieldLabel(MSGS.firewallNatFormMasquerade());
        masqueradeCombo.setEditable(false);
        masqueradeCombo.setTypeAhead(true);  
        masqueradeCombo.setTriggerAction(TriggerAction.ALL);
        masqueradeCombo.setToolTip(MSGS.firewallPortForwardFormMasqueradingToolTip());
        for (GwtFirewallNatMasquerade masquerade : GwtFirewallNatMasquerade.values()) {
        	masqueradeCombo.add(masquerade.name());
        }
        masqueradeCombo.setSimpleValue(GwtFirewallNatMasquerade.no.name());
        fieldSet.add(masqueradeCombo, formData);
        
        //
    	// permitted network
        //
        final LabelField permittedNetworkLabel = new LabelField();
        permittedNetworkLabel.setName("permittedNetworkLabel");
        permittedNetworkLabel.setFieldLabel(MSGS.firewallPortForwardFormPermittedNetwork());
        permittedNetworkLabel.setLabelSeparator(":");
        fieldSet.add(permittedNetworkLabel, formData);
        
        final TextField<String> permittedNetworkField = new TextField<String>();
        permittedNetworkField.setAllowBlank(true);
        permittedNetworkField.setName("permittedNetwork");
        permittedNetworkField.setFieldLabel(MSGS.firewallPortForwardFormPermittedNetwork());
        permittedNetworkField.setToolTip(MSGS.firewallPortForwardFormPermittedNetworkToolTip());
        permittedNetworkField.setValidator(new TextFieldValidator(permittedNetworkField, FieldType.NETWORK));
        fieldSet.add(permittedNetworkField, formData);
        
        //
    	// permitted MAC
        //
        final LabelField permittedMacLabel = new LabelField();
        permittedMacLabel.setName("permittedMacLabel");
        permittedMacLabel.setFieldLabel(MSGS.firewallPortForwardFormPermittedMac());
        permittedMacLabel.setLabelSeparator(":");
        fieldSet.add(permittedMacLabel, formData);
        
        final TextField<String> permittedMacField = new TextField<String>();
        permittedMacField.setAllowBlank(true);
        permittedMacField.setName("permittedMac");
        permittedMacField.setFieldLabel(MSGS.firewallPortForwardFormPermittedMac());
        permittedMacField.setToolTip(MSGS.firewallPortForwardFormPermittedMacAddressToolTip());
        permittedMacField.setValidator(new TextFieldValidator(permittedMacField, FieldType.MAC_ADDRESS));
        fieldSet.add(permittedMacField, formData);
        
        //
    	// source port range
        //
        final LabelField sourcePortRangeLabel = new LabelField();
        sourcePortRangeLabel.setName("sourcePortRangeLabel");
        sourcePortRangeLabel.setFieldLabel(MSGS.firewallPortForwardFormSourcePortRange());
        sourcePortRangeLabel.setLabelSeparator(":");
        fieldSet.add(sourcePortRangeLabel, formData);
        
        final TextField<String> sourcePortRangeField = new TextField<String>();
        sourcePortRangeField.setAllowBlank(true);
        sourcePortRangeField.setName("sourcePortRange");
        sourcePortRangeField.setFieldLabel(MSGS.firewallPortForwardFormSourcePortRange());
        sourcePortRangeField.setToolTip(MSGS.firewallPortForwardFormSourcePortRangeToolTip());
        sourcePortRangeField.setValidator(new TextFieldValidator(sourcePortRangeField, FieldType.PORT_RANGE));
        fieldSet.add(sourcePortRangeField, formData);
        
        //add the fieldSet to the panel
        m_formPanel.add(fieldSet);
    	
        //disable the labels
        inboundInterfaceLabel.setVisible(false);
    	outboundInterfaceLabel.setVisible(false);
    	addressLabel.setVisible(false);
    	protocolLabel.setVisible(false);
    	inPortLabel.setVisible(false);
    	outPortLabel.setVisible(false);
    	masqueradeLabel.setVisible(false);
    	permittedNetworkLabel.setVisible(false);
    	permittedMacLabel.setVisible(false);
    	sourcePortRangeLabel.setVisible(false);
    	
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
            		m_newEntry.setInboundInterface(inboundInterfaceField.getValue());
            		m_newEntry.setOutboundInterface(outboundInterfaceField.getValue());
            		m_newEntry.setAddress(addressField.getValue());
            		m_newEntry.setProtocol(protocolCombo.getValue().getValue());
            		m_newEntry.setInPort(Integer.parseInt(inPortField.getValue()));
            		m_newEntry.setOutPort(Integer.parseInt(outPortField.getValue()));
            		m_newEntry.setMasquerade(masqueradeCombo.getValue().getValue());
            		m_newEntry.setPermittedNetwork(permittedNetworkField.getValue());
            		m_newEntry.setPermittedMAC(permittedMacField.getValue());
            		m_newEntry.setSourcePortRange(sourcePortRangeField.getValue());
            		
            		if (m_newEntry.getPermittedMAC() != null) {
                		MessageBox.alert(MSGS.firewallPortForwardFormNotification(), MSGS.firewallPortForwardFormNotificationMacFiltering(), null);
                	}
            	} else {
            		//update the current entry
            		m_existingEntry = new GwtFirewallPortForwardEntry();
            		m_existingEntry.setInboundInterface(inboundInterfaceField.getValue());
            		m_existingEntry.setOutboundInterface(outboundInterfaceField.getValue());
            		m_existingEntry.setAddress(addressField.getValue());
            		m_existingEntry.setProtocol(protocolCombo.getValue().getValue());
            		m_existingEntry.setInPort(Integer.parseInt(inPortField.getValue()));
            		m_existingEntry.setOutPort(Integer.parseInt(outPortField.getValue()));
            		m_existingEntry.setMasquerade(masqueradeCombo.getValue().getValue());
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
        	
        	inboundInterfaceLabel.setValue(m_existingEntry.getInboundInterface());
        	inboundInterfaceField.setValue(m_existingEntry.getInboundInterface());
        	inboundInterfaceField.setOriginalValue(m_existingEntry.getInboundInterface());
        	
        	outboundInterfaceLabel.setValue(m_existingEntry.getOutboundInterface());
        	outboundInterfaceField.setValue(m_existingEntry.getOutboundInterface());
        	outboundInterfaceField.setOriginalValue(m_existingEntry.getOutboundInterface());
        	
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
        
        	masqueradeCombo.setSimpleValue(m_existingEntry.getMasquerade());
        	
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
