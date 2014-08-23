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
import org.eclipse.kura.web.client.util.FormUtils;
import org.eclipse.kura.web.client.util.TextFieldValidator;
import org.eclipse.kura.web.client.util.TextFieldValidator.FieldType;
import org.eclipse.kura.web.shared.model.GwtFirewallNatEntry;
import org.eclipse.kura.web.shared.model.GwtFirewallNatMasquerade;
import org.eclipse.kura.web.shared.model.GwtFirewallNatProtocol;
import org.eclipse.kura.web.shared.model.GwtSession;

import com.extjs.gxt.ui.client.Style.HorizontalAlignment;
import com.extjs.gxt.ui.client.Style.Scroll;
import com.extjs.gxt.ui.client.event.ButtonEvent;
import com.extjs.gxt.ui.client.event.ComponentEvent;
import com.extjs.gxt.ui.client.event.Events;
import com.extjs.gxt.ui.client.event.Listener;
import com.extjs.gxt.ui.client.event.SelectionListener;
import com.extjs.gxt.ui.client.widget.Component;
import com.extjs.gxt.ui.client.widget.ComponentPlugin;
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

public class NatForm extends Window {

	private static final Messages MSGS = GWT.create(Messages.class);

    private static final int  LABEL_WIDTH_FORM = 190; 
    
	private GwtFirewallNatEntry m_newEntry;
	private GwtFirewallNatEntry m_existingEntry;
	private FormPanel m_formPanel;
	private Status m_status;
	private boolean m_isCanceled;
	private ComponentPlugin m_dirtyPlugin;
    
    public NatForm(GwtSession session) {
    	
    	m_existingEntry = null;
    	
    	 setModal(true);
         setSize(600, 500);
         setLayout(new FitLayout());
         setResizable(false);
         String heading = MSGS.firewallNatFormNew();
         setHeading(heading);
         
		final NatForm theTab = this;
		m_dirtyPlugin = new ComponentPlugin() {
			public void init(Component component) {
				component.addListener(Events.Change,
						new Listener<ComponentEvent>() {
							public void handleEvent(ComponentEvent be) {
								FormUtils.addDirtyFieldIcon(be.getComponent());
								theTab.fireEvent(Events.Change);
							}
						});
			}
		};   
    }
    
	public NatForm(GwtSession session, GwtFirewallNatEntry existingEntry) {
		this(session);
		m_existingEntry = existingEntry;
		if (m_existingEntry != null) {
			setHeading(MSGS.firewallNatFormUpdate(m_existingEntry
					.getOutInterface()));
		}
	}

	public GwtFirewallNatEntry getNewFirewallNatEntry() {
		return m_newEntry;
	}

	public GwtFirewallNatEntry getExistingFirewallNatEntry() {
		return m_existingEntry;
	}

	public boolean isCanceled() {
		return m_isCanceled;
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

	protected void onRender(Element parent, int index) {
		
		super.onRender(parent, index);
    	setId("nat-form");
    	
    	FormData formData = new FormData("-30");

        m_formPanel = new FormPanel();
        m_formPanel.setFrame(false);
        m_formPanel.setBodyBorder(true);
        m_formPanel.setHeaderVisible(false);
        m_formPanel.setScrollMode(Scroll.AUTOY);
        m_formPanel.setLayout(new FlowLayout());

        FieldSet fieldSet = new FieldSet();
        fieldSet.setHeading(MSGS.firewallNatFormInformation());
        FormLayout layoutAccount = new FormLayout();
        layoutAccount.setLabelWidth(LABEL_WIDTH_FORM);
        fieldSet.setLayout(layoutAccount);
        
        //
    	// input interface name
        //
        final LabelField inInterfaceNameLabel = new LabelField();
        inInterfaceNameLabel.setName("inInterfaceNameLabel");
        inInterfaceNameLabel.setFieldLabel(MSGS.firewallNatFormInInterfaceName());
        inInterfaceNameLabel.setLabelSeparator(":");
        fieldSet.add(inInterfaceNameLabel, formData);
        
        final TextField<String> inInterfaceNameField = new TextField<String>();
        inInterfaceNameField.setAllowBlank(false);
        inInterfaceNameField.setName("inInterfaceName");
        inInterfaceNameField.setFieldLabel(MSGS.firewallNatFormInInterfaceName());
        inInterfaceNameField.setToolTip(MSGS.firewallNatFormInputInterfaceToolTip());
        inInterfaceNameField.setValidator(new TextFieldValidator(inInterfaceNameField, FieldType.ALPHANUMERIC));
        inInterfaceNameField.addPlugin(m_dirtyPlugin);
        fieldSet.add(inInterfaceNameField, formData);
        
        //
    	// output interface name
        //
        final LabelField outInterfaceNameLabel = new LabelField();
        outInterfaceNameLabel.setName("outInterfaceNameLabel");
        outInterfaceNameLabel.setFieldLabel(MSGS.firewallNatFormOutInterfaceName());
        outInterfaceNameLabel.setLabelSeparator(":");
        fieldSet.add(outInterfaceNameLabel, formData);
        
        final TextField<String> outInterfaceNameField = new TextField<String>();
        outInterfaceNameField.setAllowBlank(false);
        outInterfaceNameField.setName("outInterfaceName");
        outInterfaceNameField.setFieldLabel(MSGS.firewallNatFormOutInterfaceName());
        outInterfaceNameField.setToolTip(MSGS.firewallNatFormOutputInterfaceToolTip());
        outInterfaceNameField.setValidator(new TextFieldValidator(outInterfaceNameField, FieldType.ALPHANUMERIC));
        outInterfaceNameField.addPlugin(m_dirtyPlugin);
        fieldSet.add(outInterfaceNameField, formData);
        
        //
    	// protocol
        //
        final LabelField protocolLabel = new LabelField();
        protocolLabel.setName("protocolLabel");
        protocolLabel.setFieldLabel(MSGS.firewallNatFormProtocol());
        protocolLabel.setLabelSeparator(":");
        fieldSet.add(protocolLabel, formData);
        
        final SimpleComboBox<String> protocolCombo = new SimpleComboBox<String>();
        protocolCombo.setName("protocolCombo");
        protocolCombo.setFieldLabel(MSGS.firewallNatFormProtocol());
        protocolCombo.setEditable(false);
        protocolCombo.setTypeAhead(true);  
        protocolCombo.setTriggerAction(TriggerAction.ALL);
        protocolCombo.setToolTip(MSGS.firewallNatFormProtocolToolTip());
        for (GwtFirewallNatProtocol protocol : GwtFirewallNatProtocol.values()) {
        	protocolCombo.add(protocol.name());
        }
        protocolCombo.setSimpleValue(GwtFirewallNatProtocol.all.name());
        fieldSet.add(protocolCombo, formData);
        
        //
    	// Source Network
        //
        final LabelField sourceNetworkLabel = new LabelField();
        sourceNetworkLabel.setName("sourceNetworkLabel");
        sourceNetworkLabel.setFieldLabel(MSGS.firewallNatFormSourceNetwork());
        sourceNetworkLabel.setLabelSeparator(":");
        fieldSet.add(sourceNetworkLabel, formData);
        
        final TextField<String> sourceNetworkField = new TextField<String>();
        sourceNetworkField.setAllowBlank(true);
        sourceNetworkField.setName("address");
        sourceNetworkField.setFieldLabel(MSGS.firewallNatFormSourceNetwork());
        sourceNetworkField.setToolTip(MSGS.firewallNatFormSourceNetworkToolTip());
        sourceNetworkField.setValidator(new TextFieldValidator(sourceNetworkField, FieldType.NETWORK));
        sourceNetworkField.addPlugin(m_dirtyPlugin);
        fieldSet.add(sourceNetworkField, formData);
        
        //
    	// Destination Network
        //
        final LabelField destinationNetworkLabel = new LabelField();
        destinationNetworkLabel.setName("destinationNetworkLabel");
        destinationNetworkLabel.setFieldLabel(MSGS.firewallNatFormDestinationNetwork());
        destinationNetworkLabel.setLabelSeparator(":");
        fieldSet.add(destinationNetworkLabel, formData);
        
        final TextField<String> destinationNetworkField = new TextField<String>();
        destinationNetworkField.setAllowBlank(true);
        destinationNetworkField.setName("address");
        destinationNetworkField.setFieldLabel(MSGS.firewallNatFormDestinationNetwork());
        destinationNetworkField.setToolTip(MSGS.firewallNatFormDestinationNetworkToolTip());
        destinationNetworkField.setValidator(new TextFieldValidator(destinationNetworkField, FieldType.NETWORK));
        destinationNetworkField.addPlugin(m_dirtyPlugin);
        fieldSet.add(destinationNetworkField, formData);
        
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
        masqueradeCombo.setToolTip(MSGS.firewallNatFormMasqueradingToolTip());
        for (GwtFirewallNatMasquerade masquerade : GwtFirewallNatMasquerade.values()) {
        	masqueradeCombo.add(masquerade.name());
        }
        masqueradeCombo.setSimpleValue(GwtFirewallNatMasquerade.yes.name());
        fieldSet.add(masqueradeCombo, formData);
        
        //add the fieldSet to the panel
        m_formPanel.add(fieldSet);
    	
        //disable the labels
        inInterfaceNameLabel.setVisible(false);
        outInterfaceNameLabel.setVisible(false);
        protocolLabel.setVisible(false);
        sourceNetworkLabel.setVisible(false);
        destinationNetworkLabel.setVisible(false);
        masqueradeLabel.setVisible(false);
    	
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
            		m_newEntry = new GwtFirewallNatEntry();
            		m_newEntry.setInInterface(inInterfaceNameField.getValue());
            		m_newEntry.setOutInterface(outInterfaceNameField.getValue());
            		m_newEntry.setProtocol(protocolCombo.getValue().getValue());
            		m_newEntry.setSourceNetwork(sourceNetworkField.getValue());
            		m_newEntry.setDestinationNetwork(destinationNetworkField.getValue());
            		m_newEntry.setMasquerade(masqueradeCombo.getValue().getValue());
            	} else {
            		m_existingEntry = new GwtFirewallNatEntry();
            		m_existingEntry.setInInterface(inInterfaceNameField.getValue());
            		m_existingEntry.setOutInterface(outInterfaceNameField.getValue());
            		m_existingEntry.setProtocol(protocolCombo.getValue().getValue());
            		m_existingEntry.setSourceNetwork(sourceNetworkField.getValue());
            		m_existingEntry.setDestinationNetwork(destinationNetworkField.getValue());
            		m_existingEntry.setMasquerade(masqueradeCombo.getValue().getValue());
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
        	
        	//inInterfaceNameLabel.setValue(m_existingEntry.getInInterface());
        	inInterfaceNameField.setValue(m_existingEntry.getInInterface());
        	inInterfaceNameField.setOriginalValue(m_existingEntry.getInInterface());
        	
        	outInterfaceNameLabel.setValue(m_existingEntry.getOutInterface());
        	outInterfaceNameField.setValue(m_existingEntry.getOutInterface());
        	outInterfaceNameField.setOriginalValue(m_existingEntry.getOutInterface());
        	
        	protocolCombo.setSimpleValue(m_existingEntry.getProtocol());
        	
        	sourceNetworkLabel.setValue(m_existingEntry.getSourceNetwork());
        	sourceNetworkField.setValue(m_existingEntry.getSourceNetwork());
        	sourceNetworkField.setOriginalValue(m_existingEntry.getSourceNetwork());
        	
        	destinationNetworkLabel.setValue(m_existingEntry.getDestinationNetwork());
        	destinationNetworkField.setValue(m_existingEntry.getDestinationNetwork());
        	destinationNetworkField.setOriginalValue(m_existingEntry.getDestinationNetwork());
        	
        	masqueradeCombo.setSimpleValue(m_existingEntry.getMasquerade());
        }
        
        add(m_formPanel);
	}
}
