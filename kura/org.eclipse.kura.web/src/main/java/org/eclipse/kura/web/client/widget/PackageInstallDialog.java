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
package org.eclipse.kura.web.client.widget;

import java.util.List;

import org.eclipse.kura.web.client.messages.Messages;
import org.eclipse.kura.web.client.util.Constants;

import com.extjs.gxt.ui.client.Style.HorizontalAlignment;
import com.extjs.gxt.ui.client.Style.Scroll;
import com.extjs.gxt.ui.client.event.ButtonEvent;
import com.extjs.gxt.ui.client.event.Events;
import com.extjs.gxt.ui.client.event.FormEvent;
import com.extjs.gxt.ui.client.event.Listener;
import com.extjs.gxt.ui.client.event.SelectionListener;
import com.extjs.gxt.ui.client.util.Margins;
import com.extjs.gxt.ui.client.widget.Dialog;
import com.extjs.gxt.ui.client.widget.MessageBox;
import com.extjs.gxt.ui.client.widget.Status;
import com.extjs.gxt.ui.client.widget.TabItem;
import com.extjs.gxt.ui.client.widget.TabPanel;
import com.extjs.gxt.ui.client.widget.button.Button;
import com.extjs.gxt.ui.client.widget.form.FieldSet;
import com.extjs.gxt.ui.client.widget.form.FileUploadField;
import com.extjs.gxt.ui.client.widget.form.FormPanel;
import com.extjs.gxt.ui.client.widget.form.FormPanel.Encoding;
import com.extjs.gxt.ui.client.widget.form.FormPanel.Method;
import com.extjs.gxt.ui.client.widget.form.HiddenField;
import com.extjs.gxt.ui.client.widget.form.TextField;
import com.extjs.gxt.ui.client.widget.layout.FitLayout;
import com.extjs.gxt.ui.client.widget.layout.FormData;
import com.extjs.gxt.ui.client.widget.layout.FormLayout;
import com.extjs.gxt.ui.client.widget.toolbar.FillToolItem;
import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.Element;

public class PackageInstallDialog extends Dialog {
	
	private static final Messages MSGS = GWT.create(Messages.class);
	
	private TabPanel				m_tabsPanel;
	private TabItem					m_tabFile;
	private TabItem					m_tabUrl;

	private FormPanel 				m_formPanelFile;
	private FileUploadField 		m_fileUploadField;
	
	private FormPanel 				m_formPanelUrl;
	private TextField<String> 		m_textFieldUrl;
	
	private List<HiddenField<?>> 	m_hiddenFields;
	
	private Button 					m_submitButton;
	private Button 					m_cancelButton;
	private Status 					m_status;
	
	private String 					m_actionUrl;
	
	
	public PackageInstallDialog(String actionUrl, List<HiddenField<?>> hiddenFields) {
		super();
		m_actionUrl = actionUrl;
		m_hiddenFields = hiddenFields;
		setButtonAlign(HorizontalAlignment.RIGHT);
	}
	
	@Override
	protected void onRender(Element parent, int pos) {
		super.onRender(parent, pos);
		
		setLayout(new FitLayout());
		setBodyBorder(false);
		setModal(true);
		setButtons("");
		setScrollMode(Scroll.AUTO);
		setHideOnButtonClick(false);
		setSize(400, 175);
		
		m_tabsPanel = new TabPanel();
        m_tabsPanel.setPlain(true);
        m_tabsPanel.setBorders(false);
		
        //
        // File upload tab
	    m_formPanelFile = new FormPanel();
	    m_formPanelFile.setFrame(false);
	    m_formPanelFile.setHeaderVisible(false);
	    m_formPanelFile.setBodyBorder(false);
	    m_formPanelFile.setAction(m_actionUrl+"/upload");
	    m_formPanelFile.setEncoding(Encoding.MULTIPART);
	    m_formPanelFile.setMethod(Method.POST);
	    m_formPanelFile.setButtonAlign(HorizontalAlignment.CENTER);
	    m_formPanelFile.setStyleAttribute("padding-right", "0px");
	    
	    m_formPanelFile.addListener(Events.Submit, new Listener<FormEvent>() {
			public void handleEvent(FormEvent be) {
				String htmlResponse = be.getResultHtml();				
				if (htmlResponse == null || htmlResponse.isEmpty()) {
					MessageBox.info(MSGS.information(), MSGS.fileUploadSuccess(), null);
				} else {
					MessageBox.info(MSGS.information(), MSGS.fileUploadFailure(), null);
				}
				hide();
			}
		});
		
	    FieldSet fileFieldSet = new FieldSet();
	    fileFieldSet.setBorders(false);
	    fileFieldSet.setStyleAttribute("padding", "0px");
        FormLayout fileFormLayout = new FormLayout();
        fileFormLayout.setLabelWidth(Constants.LABEL_WIDTH_FORM_SMALL);
        fileFieldSet.setLayout(fileFormLayout);

	    m_fileUploadField = new FileUploadField();
	    m_fileUploadField.setAllowBlank(false);
	    m_fileUploadField.setName("uploadedFile");
	    m_fileUploadField.setFieldLabel(MSGS.fileLabel());

	    if (m_hiddenFields != null) {
	    	for (HiddenField<?> hf : m_hiddenFields) {
	    		fileFieldSet.add(hf);
	    	}
	    }

        FormData fileFormData = new FormData("-20");
        fileFormData.setMargins(new Margins(0, 0, 0, 0));
	    fileFieldSet.add(m_fileUploadField, fileFormData);
	    
	    m_formPanelFile.add(fileFieldSet, fileFormData);
	    
        m_tabFile = new TabItem(MSGS.fileLabel());
        m_tabFile.setBorders(true);
        m_tabFile.setLayout(new FormLayout());
        m_tabFile.add(m_formPanelFile);

        m_tabsPanel.add(m_tabFile);        

        //
        // Download URL tab
        m_formPanelUrl = new FormPanel();
	    m_formPanelUrl.setFrame(false);
	    m_formPanelUrl.setHeaderVisible(false);
	    m_formPanelUrl.setBodyBorder(false);
	    m_formPanelUrl.setAction(m_actionUrl+"/url");
	    m_formPanelUrl.setMethod(Method.POST);
	    m_formPanelUrl.setStyleAttribute("padding-right", "0px");
	    
	    m_formPanelUrl.addListener(Events.Submit, new Listener<FormEvent>() {

			public void handleEvent(FormEvent be) {
				String htmlResponse = be.getResultHtml();				
				if (htmlResponse == null || htmlResponse.isEmpty()) {
					MessageBox.info(MSGS.information(), MSGS.fileDownloadSuccess(), null);
				} 
				else {
					String errMsg = htmlResponse;
					int startIdx = htmlResponse.indexOf("<pre>");
					int endIndex = htmlResponse.indexOf("</pre>");
					if (startIdx != -1 && endIndex != -1) {
						errMsg = htmlResponse.substring(startIdx+5, endIndex);
					}
					MessageBox.alert(MSGS.error(), MSGS.fileDownloadFailure()+": "+errMsg, null);
				}
				hide();
			}
		});
	    
	    FieldSet urlFieldSet = new FieldSet();
	    urlFieldSet.setBorders(false);
	    urlFieldSet.setStyleAttribute("padding", "0px");
        FormLayout urlFormLayout = new FormLayout();
        urlFormLayout.setLabelWidth(Constants.LABEL_WIDTH_FORM_SMALL);
        urlFieldSet.setLayout(urlFormLayout);

	    m_textFieldUrl = new TextField<String>();
	    m_textFieldUrl.setAllowBlank(false);
	    m_textFieldUrl.setName("packageUrl");
	    m_textFieldUrl.setFieldLabel(MSGS.urlLabel());
	    
        FormData urlFormData = new FormData("-20");
        urlFormData.setMargins(new Margins(0, 0, 0, 0));
	    urlFieldSet.add(m_textFieldUrl, urlFormData);
	    if (m_hiddenFields != null) {
	    	for (HiddenField<?> hf : m_hiddenFields) {
	    		urlFieldSet.add(hf);
	    	}
	    }
	    	    
	    m_formPanelUrl.add(urlFieldSet, urlFormData);
	    
        m_tabUrl = new TabItem(MSGS.urlLabel());
        m_tabUrl.setBorders(true);
        m_tabUrl.setLayout(new FormLayout());
        m_tabUrl.add(m_formPanelUrl);        
        m_tabsPanel.add(m_tabUrl);
        
	    add(m_tabsPanel);
	}

	@Override
	protected void createButtons() {
		super.createButtons();
		
		m_status = new Status();
		m_status.setBusy(MSGS.waitMsg());
		m_status.hide();
		m_status.setAutoWidth(true);
		getButtonBar().add(m_status);

		getButtonBar().add(new FillToolItem());

	    m_submitButton = new Button(MSGS.submitButton());
	    m_submitButton.addSelectionListener(new SelectionListener<ButtonEvent>() {  
	    	@Override  
	    	public void componentSelected(ButtonEvent ce) {  
	    		submit();
	    	}  
	    });


	    m_cancelButton = new Button(MSGS.cancelButton());
	    m_cancelButton.addSelectionListener(new SelectionListener<ButtonEvent>() {  
	    	@Override  
	    	public void componentSelected(ButtonEvent ce) {  
	    		hide();
	    	}
	    });
	    
	    addButton(m_cancelButton);
	    addButton(m_submitButton);
	}
	
	private void submit() {
		TabItem selectedItem = m_tabsPanel.getSelectedItem();
		FormPanel formPanel = null;
		if (selectedItem == m_tabFile) {
			formPanel = m_formPanelFile;
		} else {
			formPanel = m_formPanelUrl;
		}
		
		if (!formPanel.isValid()) {
			return;
		}
		
		m_tabsPanel.mask();
		m_submitButton.disable();
		m_cancelButton.disable();
		m_status.show();
		formPanel.submit();
	}
}