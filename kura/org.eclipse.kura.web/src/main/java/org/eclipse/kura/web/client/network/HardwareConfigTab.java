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
import org.eclipse.kura.web.client.util.Constants;
import org.eclipse.kura.web.shared.model.GwtNetInterfaceConfig;
import org.eclipse.kura.web.shared.model.GwtSession;

import com.extjs.gxt.ui.client.Style.Scroll;
import com.extjs.gxt.ui.client.widget.LayoutContainer;
import com.extjs.gxt.ui.client.widget.form.Field;
import com.extjs.gxt.ui.client.widget.form.FieldSet;
import com.extjs.gxt.ui.client.widget.form.FormPanel;
import com.extjs.gxt.ui.client.widget.form.LabelField;
import com.extjs.gxt.ui.client.widget.layout.FitLayout;
import com.extjs.gxt.ui.client.widget.layout.FlowLayout;
import com.extjs.gxt.ui.client.widget.layout.FormData;
import com.extjs.gxt.ui.client.widget.layout.FormLayout;
import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.Element;

public class HardwareConfigTab extends LayoutContainer 
{
	private static final Messages MSGS = GWT.create(Messages.class);

	@SuppressWarnings("unused")
    private GwtSession            m_currentSession;

    private boolean               m_dirty;
    private boolean               m_initialized;
	private GwtNetInterfaceConfig m_selectNetIfConfig;

	private FormPanel  m_formPanel;
	private LabelField m_stateField;
	private LabelField m_nameField;
	private LabelField m_typeField;
	private LabelField m_addressField;
	private LabelField m_serialField;
	private LabelField m_driverField;
	private LabelField m_versionField;
	private LabelField m_firmwareField;
	private LabelField m_mtuField;
	private LabelField m_usbField;
	private LabelField m_rssiField;
	
    public HardwareConfigTab(GwtSession currentSession) {
        m_currentSession = currentSession;
    	m_dirty          = true;
    	m_initialized    = false;
    }
    
    public void setNetInterface(GwtNetInterfaceConfig netIfConfig)
    {
    	m_dirty = true;
    	m_selectNetIfConfig = netIfConfig;
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
    	return false;
    }
    
    
    public void getUpdatedNetInterface(GwtNetInterfaceConfig updatedNetIf)
    {
    	if (m_formPanel != null) {
	    	updatedNetIf.setHwState((String) m_stateField.getValue());
	    	updatedNetIf.setHwName((String) m_nameField.getValue());
	    	updatedNetIf.setHwType((String) m_typeField.getValue());
	    	updatedNetIf.setHwAddress((String) m_addressField.getValue());
	    	updatedNetIf.setHwSerial((String) m_serialField.getValue());
	    	updatedNetIf.setHwDriver((String) m_driverField.getValue());
	    	updatedNetIf.setHwDriverVersion((String) m_versionField.getValue());
	    	updatedNetIf.setHwFirmware((String) m_firmwareField.getValue());
	    	if (m_mtuField.getValue() != null) {
	    		int mtu = Integer.parseInt((String) m_mtuField.getValue());
	    		updatedNetIf.setHwMTU(mtu);
	    	}	    	
	    	updatedNetIf.setHwUsbDevice((String) m_usbField.getValue());
	    	updatedNetIf.setHwRssi((String) m_usbField.getValue());
    	}
    }

    
    protected void onRender(Element parent, int index) 
    {        
    	super.onRender(parent, index);         
        setLayout(new FitLayout());
        setId("network-hardware");
        
        FormData formData = new FormData();
        formData.setWidth(250);

        m_formPanel = new FormPanel();
        m_formPanel.setFrame(false);
        m_formPanel.setBodyBorder(false);
        m_formPanel.setHeaderVisible(false);
        m_formPanel.setScrollMode(Scroll.AUTOY);
        m_formPanel.setLayout(new FlowLayout());
        m_formPanel.setStyleAttribute("padding-left", "30px");


        FieldSet fieldSet = new FieldSet();
        FormLayout layoutAccount = new FormLayout();
        layoutAccount.setLabelWidth(Constants.LABEL_WIDTH_FORM+20);
        fieldSet.setLayout(layoutAccount);
        fieldSet.setBorders(false);

        // state
        m_stateField = new LabelField();
        m_stateField.setFieldLabel(MSGS.netHwState());
        fieldSet.add(m_stateField, formData);
        
        // name
        m_nameField = new LabelField();
        m_nameField.setFieldLabel(MSGS.netHwName());
        fieldSet.add(m_nameField, formData);

        // type
        m_typeField = new LabelField();
        m_typeField.setFieldLabel(MSGS.netHwType());
        fieldSet.add(m_typeField, formData);

        // hardware address
        m_addressField = new LabelField();
        m_addressField.setFieldLabel(MSGS.netHwAddress());
        fieldSet.add(m_addressField, formData);

        // serial num
        m_serialField = new LabelField();
        m_serialField.setFieldLabel(MSGS.netHwSerial());
        fieldSet.add(m_serialField, formData);

        // driver
        m_driverField = new LabelField();
        m_driverField.setFieldLabel(MSGS.netHwDriver());
        fieldSet.add(m_driverField, formData);

        // driver version
        m_versionField = new LabelField();
        m_versionField.setFieldLabel(MSGS.netHwVersion());
        fieldSet.add(m_versionField, formData);

        // firmware version
        m_firmwareField = new LabelField();
        m_firmwareField.setFieldLabel(MSGS.netHwFirmware());
        fieldSet.add(m_firmwareField, formData);

        // mtu
        m_mtuField = new LabelField();
        m_mtuField.setFieldLabel(MSGS.netHwMTU());
        fieldSet.add(m_mtuField, formData);

        // usb device
        m_usbField = new LabelField();
        m_usbField.setFieldLabel(MSGS.netHwUSBDevice());
        fieldSet.add(m_usbField, formData);
        
        // RSSI
        m_rssiField = new LabelField();
        m_rssiField.setFieldLabel(MSGS.netHwSignalStrength());
        fieldSet.add(m_rssiField, formData);
    
        m_formPanel.add(fieldSet);
        add(m_formPanel);
        
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
		if (m_selectNetIfConfig != null) {
			m_stateField.setValue(m_selectNetIfConfig.getHwState());
			m_nameField.setValue(m_selectNetIfConfig.getHwName());
			m_typeField.setValue(m_selectNetIfConfig.getHwType());
			m_addressField.setValue(m_selectNetIfConfig.getHwAddress());
			m_serialField.setValue(m_selectNetIfConfig.getHwSerial());
			m_driverField.setValue(m_selectNetIfConfig.getHwDriver());
			m_versionField.setValue(m_selectNetIfConfig.getHwDriverVersion());
			m_firmwareField.setValue(m_selectNetIfConfig.getHwFirmware());
			m_mtuField.setValue(m_selectNetIfConfig.getHwMTU());
			m_usbField.setValue(m_selectNetIfConfig.getHwUsbDevice());
			m_rssiField.setValue(m_selectNetIfConfig.getHwRssi());
		}
		else {
			reset();
		}
	}

	private void reset()
	{
		m_stateField.setValue("");
		m_nameField.setValue("");
		m_typeField.setValue("");
		m_addressField.setValue("");
		m_serialField.setValue("");
		m_driverField.setValue("");
		m_versionField.setValue("");
		m_firmwareField.setValue("");
		m_mtuField.setValue("");
		m_usbField.setValue("");
		m_rssiField.setValue("");
	}
}
