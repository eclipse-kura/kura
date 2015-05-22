/**
 * Copyright (c) 2011, 2015 Eurotech and/or its affiliates
 *
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *   Eurotech
 */
package org.eclipse.kura.web.client.settings;

import org.eclipse.kura.web.client.messages.Messages;
import org.eclipse.kura.web.shared.GwtKuraErrorCode;
import org.eclipse.kura.web.shared.model.GwtSession;
import org.eclipse.kura.web.shared.service.GwtCertificatesService;
import org.eclipse.kura.web.shared.service.GwtCertificatesServiceAsync;

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
import com.extjs.gxt.ui.client.widget.button.Button;
import com.extjs.gxt.ui.client.widget.button.ButtonBar;
import com.extjs.gxt.ui.client.widget.form.FormPanel;
import com.extjs.gxt.ui.client.widget.form.TextArea;
import com.extjs.gxt.ui.client.widget.form.TextField;
import com.extjs.gxt.ui.client.widget.form.FormPanel.Encoding;
import com.extjs.gxt.ui.client.widget.form.FormPanel.Method;
import com.extjs.gxt.ui.client.widget.layout.ColumnLayout;
import com.extjs.gxt.ui.client.widget.layout.FitLayout;
import com.extjs.gxt.ui.client.widget.layout.FormData;
import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.Element;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.Label;

public class CertificatesTab extends LayoutContainer {

	private static final Messages MSGS = GWT.create(Messages.class);

	private final static String SERVLET_URL = "/" + GWT.getModuleName() + "/file/certificate";
	private final GwtCertificatesServiceAsync gwtCertificatesService = GWT.create(GwtCertificatesService.class);

	@SuppressWarnings("unused")
	private GwtSession			m_currentSession;
	private LayoutContainer 	m_commandInput;
	private FormPanel			m_formPanel;
	private TextArea			m_publicCertificate;
	private TextArea			m_privateCertificate;
	private TextField<String>	m_storagePassword;
	private TextField<String>   m_storageAlias;

	private Button				m_executeButton;
	private Button				m_resetButton;
	private ButtonBar			m_buttonBar;


	public CertificatesTab(GwtSession currentSession) 
	{
		m_currentSession = currentSession;
	}


	protected void onRender(Element parent, int index) {
		super.onRender(parent, index);         
		setLayout(new FitLayout());
		setId("device-command");

		FormData formData = new FormData("95% 40%");

		//initToolBar();

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
		//m_formPanel.setHeight("100.0%");

		m_formPanel.setButtonAlign(HorizontalAlignment.RIGHT);
		m_buttonBar = m_formPanel.getButtonBar();
		initButtonBar();



		m_formPanel.addListener(Events.Submit, new Listener<FormEvent>() {
			public void handleEvent(FormEvent be) {
				if(m_privateCertificate.getValue() != null && m_privateCertificate.getValue() != ""){
					gwtCertificatesService.storePrivateSSLCertificate(m_privateCertificate.getValue(), m_publicCertificate.getValue(), m_storagePassword.getValue(), m_storageAlias.getValue(), new AsyncCallback<Integer>() {
						public void onFailure(Throwable caught) {
							if(caught.getLocalizedMessage().equals(GwtKuraErrorCode.ILLEGAL_ARGUMENT.toString())){
								Info.display(MSGS.error(), "Error while storing the private certificate in the key store");
							}else{
								Info.display(MSGS.error(), caught.getLocalizedMessage());
							}
							m_commandInput.unmask();
						}

						public void onSuccess(Integer certsStored) {
							m_publicCertificate.clear();
							m_privateCertificate.clear();
							m_storagePassword.clear();
							m_storageAlias.clear();
							Info.display(MSGS.info(), "Storage success. Stored private and public certificates.");
							m_commandInput.unmask();
						}
					});
				}else{
					gwtCertificatesService.storePublicSSLCertificate(m_publicCertificate.getValue(), m_storageAlias.getValue(), new AsyncCallback<Integer>() {
						public void onFailure(Throwable caught) {
							if(caught.getLocalizedMessage().equals(GwtKuraErrorCode.ILLEGAL_ARGUMENT.toString())){
								Info.display(MSGS.error(), "Error while storing the public certificate(s) in the key store");
							}else{
								Info.display(MSGS.error(), caught.getLocalizedMessage());
							}
							m_commandInput.unmask();
						}

						public void onSuccess(Integer certsStored) {
							m_publicCertificate.clear();
							m_storagePassword.clear();
							m_storageAlias.clear();
							Info.display(MSGS.info(), "Storage success. Stored " + certsStored + " public certificate(s).");
							m_commandInput.unmask();
						}
					});
				}
			}
		});
		
		//
		// Initial description
		// 
		LayoutContainer description = new LayoutContainer();
		description.setBorders(false);
		description.setLayout(new ColumnLayout());

		Label descriptionLabel = new Label(MSGS.settingsSSLDescription1());
		Label descriptionLabel2 = new Label(MSGS.settingsSSLDescription2());
		
		description.add(descriptionLabel);
		description.add(descriptionLabel2);
		description.setStyleAttribute("padding-bottom", "10px");
		m_formPanel.add(description);

		//
		// Private Certificate
		//       
		m_privateCertificate = new TextArea();
		m_privateCertificate.setBorders(false);
		m_privateCertificate.setReadOnly(false);
		m_privateCertificate.setEmptyText(MSGS.settingsPrivateCertLabel());
		m_privateCertificate.setName(MSGS.settingsPrivateCertLabel());
		m_privateCertificate.setAllowBlank(true);
		m_privateCertificate.setFieldLabel(MSGS.settingsPrivateCertLabel());
		m_formPanel.add(m_privateCertificate, formData);
		
		//
		//
		//
		m_storagePassword = new TextField<String>();
		m_storagePassword.setName(MSGS.settingsStoragePasswordLabel());
		m_storagePassword.setPassword(true);
		m_storagePassword.setAllowBlank(false);
		m_storagePassword.setEmptyText("* " + MSGS.settingsStoragePasswordLabel());
		m_storagePassword.setFieldLabel(MSGS.settingsStoragePasswordLabel());
		m_formPanel.add(m_storagePassword, new FormData("95%"));

		//
		// Public Certificate
		//
		m_publicCertificate = new TextArea();
		m_publicCertificate.setBorders(false);
		m_publicCertificate.setReadOnly(false);
		m_publicCertificate.setEmptyText("* " + MSGS.settingsPublicCertLabel());
		m_publicCertificate.setName(MSGS.settingsPublicCertLabel());
		m_publicCertificate.setAllowBlank(false);
		m_publicCertificate.setFieldLabel(MSGS.settingsPublicCertLabel());
		m_formPanel.add(m_publicCertificate, formData);
		
		//
		//
		//
		m_storageAlias = new TextField<String>();
		m_storageAlias.setName(MSGS.settingsStorageAliasLabel());
		m_storageAlias.setPassword(true);
		m_storageAlias.setAllowBlank(false);
		m_storageAlias.setEmptyText("* " + MSGS.settingsStorageAliasLabel());
		m_storageAlias.setFieldLabel(MSGS.settingsStorageAliasLabel());
		m_formPanel.add(m_storageAlias, new FormData("95%"));



		m_commandInput = m_formPanel;

		// Main Panel
		ContentPanel deviceCommandPanel = new ContentPanel();
		deviceCommandPanel.setBorders(false);
		deviceCommandPanel.setBodyBorder(false);
		deviceCommandPanel.setHeaderVisible(false);
		deviceCommandPanel.setScrollMode(Scroll.AUTO);
		deviceCommandPanel.setLayout(new FitLayout());
		//deviceCommandPanel.setHeight("100%");

		//deviceCommandPanel.setTopComponent(m_commandInput);
		deviceCommandPanel.add(m_commandInput);
		//deviceCommandPanel.add(m_publicCertificateArea);

		add(deviceCommandPanel);
	}

	private void initButtonBar() {
		m_executeButton = new Button(MSGS.deviceCommandExecute());
		m_executeButton.addSelectionListener(new SelectionListener<ButtonEvent>() {  
			@Override  
			public void componentSelected(ButtonEvent ce) {
				if (m_formPanel.isValid()) {
					//m_result.clear();
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