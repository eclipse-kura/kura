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
package org.eclipse.kura.web.client.device;

import org.eclipse.kura.web.client.messages.Messages;
import org.eclipse.kura.web.shared.GwtKuraErrorCode;
import org.eclipse.kura.web.shared.model.GwtSession;
import org.eclipse.kura.web.shared.service.GwtDeviceService;
import org.eclipse.kura.web.shared.service.GwtDeviceServiceAsync;

import com.extjs.gxt.ui.client.Style.HorizontalAlignment;
import com.extjs.gxt.ui.client.Style.Scroll;
import com.extjs.gxt.ui.client.event.ButtonEvent;
import com.extjs.gxt.ui.client.event.Events;
import com.extjs.gxt.ui.client.event.FormEvent;
import com.extjs.gxt.ui.client.event.Listener;
import com.extjs.gxt.ui.client.event.SelectionListener;
import com.extjs.gxt.ui.client.widget.ContentPanel;
import com.extjs.gxt.ui.client.widget.Info;
import com.extjs.gxt.ui.client.widget.LayoutContainer;
import com.extjs.gxt.ui.client.widget.MessageBox;
import com.extjs.gxt.ui.client.widget.button.Button;
import com.extjs.gxt.ui.client.widget.button.ButtonBar;
import com.extjs.gxt.ui.client.widget.form.FileUploadField;
import com.extjs.gxt.ui.client.widget.form.FormPanel;
import com.extjs.gxt.ui.client.widget.form.FormPanel.Encoding;
import com.extjs.gxt.ui.client.widget.form.FormPanel.Method;
import com.extjs.gxt.ui.client.widget.form.TextArea;
import com.extjs.gxt.ui.client.widget.form.TextField;
import com.extjs.gxt.ui.client.widget.layout.FitLayout;
import com.extjs.gxt.ui.client.widget.layout.FormData;
import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.Element;
import com.google.gwt.user.client.rpc.AsyncCallback;

public class CommandTab extends LayoutContainer {
	
	private static final Messages MSGS = GWT.create(Messages.class);
	private static final String SERVLET_URL = "/" + GWT.getModuleName() + "/file/command";

	private final GwtDeviceServiceAsync gwtDeviceService = GWT.create(GwtDeviceService.class);
	
	@SuppressWarnings("unused")
	private GwtSession			m_currentSession;
	private LayoutContainer 	m_commandInput;
	private FormPanel			m_formPanel;
	private ButtonBar			m_buttonBar;
	private Button				m_executeButton;
	private Button				m_resetButton;
	private LayoutContainer		m_commandOutput;
	private TextArea			m_result;
	private TextField<String>	m_commandField;
	private TextField<String>	m_passwordField;
	private FileUploadField		m_fileUploadField;
	
	public CommandTab(GwtSession currentSession) {
		m_currentSession = currentSession;
	}
	
	protected void onRender(Element parent, int index) {
		super.onRender(parent, index);         
        setLayout(new FitLayout());
        setId("device-command");
        
        FormData formData = new FormData("100%");

        //
        // Command Form
        //
		m_formPanel = new FormPanel();
		m_formPanel.setFrame(true);
		m_formPanel.setHeaderVisible(false);
		m_formPanel.setBorders(false);
		m_formPanel.setBodyBorder(false);
		m_formPanel.setAction(SERVLET_URL);
		m_formPanel.setEncoding(Encoding.MULTIPART);
		m_formPanel.setMethod(Method.POST);

		m_formPanel.setButtonAlign(HorizontalAlignment.RIGHT);
		m_buttonBar = m_formPanel.getButtonBar();
		initButtonBar();
		
		
		
		m_formPanel.addListener(Events.Submit, new Listener<FormEvent>() {
			public void handleEvent(FormEvent be) {
				String htmlResult = be.getResultHtml();
				if (htmlResult.startsWith("HTTP ERROR")) {
					MessageBox.info(MSGS.information(), MSGS.fileUploadFailure() + "\n" + htmlResult, null);
					m_commandInput.unmask();
				}
				else {
					gwtDeviceService.executeCommand(m_commandField.getValue(), m_passwordField.getValue(), new AsyncCallback<String>() {
						public void onFailure(Throwable caught) {
							if(caught.getLocalizedMessage().equals(GwtKuraErrorCode.SERVICE_NOT_ENABLED.toString())){
								Info.display(MSGS.error(), MSGS.commandServiceNotEnabled());
							}else if(caught.getLocalizedMessage().equals(GwtKuraErrorCode.ILLEGAL_ARGUMENT.toString())){
								Info.display(MSGS.error(), MSGS.commandPasswordNotCorrect());
							}else{
								Info.display(MSGS.error(), caught.getLocalizedMessage());
							}
							//FailureHandler.handle(caught);
							m_commandInput.unmask();
						}

						public void onSuccess(String result) {
							m_result.clear();
							m_result.setValue(result);
							m_commandInput.unmask();
						}
					});
				}
			}
		});
		
		//
		// Command Output
		//
		m_commandOutput = new LayoutContainer();
		m_commandOutput.setBorders(false);
		m_commandOutput.setWidth("99.5%");
		m_commandOutput.setLayout(new FitLayout());
		        
		m_result = new TextArea();
		m_result.setBorders(false);
		m_result.setReadOnly(true);
		m_result.setEmptyText(MSGS.deviceCommandNoOutput());
        m_commandOutput.add(m_result);
        
        //
        // Input and Upload
        //
        m_commandField = new TextField<String>();
		m_commandField.setName("command");
		m_commandField.setAllowBlank(false);
		m_commandField.setFieldLabel(MSGS.deviceCommandExecute());
		m_formPanel.add(m_commandField, formData);
		
		
		m_passwordField = new TextField<String>();
		m_passwordField.setName("password");
		m_passwordField.setPassword(true);
		m_passwordField.setAllowBlank(true);
		m_passwordField.setFieldLabel(MSGS.deviceCommandPassword());
		m_formPanel.add(m_passwordField, formData);
	

		m_fileUploadField = new FileUploadField();
		m_fileUploadField.setAllowBlank(true);
		m_fileUploadField.setName("file");
		m_fileUploadField.setFieldLabel("File");
		m_formPanel.add(m_fileUploadField, formData);

		m_commandInput = m_formPanel;
		
		// Main Panel
		ContentPanel deviceCommandPanel = new ContentPanel();
		deviceCommandPanel.setBorders(false);
		deviceCommandPanel.setBodyBorder(false);
		deviceCommandPanel.setHeaderVisible(false);
		deviceCommandPanel.setScrollMode(Scroll.AUTO);
		deviceCommandPanel.setLayout(new FitLayout());

		deviceCommandPanel.setTopComponent(m_commandInput);
		deviceCommandPanel.add(m_commandOutput);

		add(deviceCommandPanel);
	}
	
	private void initButtonBar() {
		m_executeButton = new Button(MSGS.deviceCommandExecute());
		m_executeButton.addSelectionListener(new SelectionListener<ButtonEvent>() {  
			@Override  
			public void componentSelected(ButtonEvent ce) {
				if (m_formPanel.isValid()) {
					m_result.clear();
					m_commandInput.mask(MSGS.waiting());
		
					m_formPanel.submit();
				}
			}
		});

		m_resetButton = new Button(MSGS.reset());
		m_resetButton.addSelectionListener(new SelectionListener<ButtonEvent>() {  
            @Override
            public void componentSelected(ButtonEvent ce) {
				m_formPanel.reset();
            }
        });

		m_buttonBar.add(m_resetButton);
		m_buttonBar.add(m_executeButton);
    }
	
	public void refresh() {
		m_commandInput.unmask();
	}

}
