/*******************************************************************************
 * Copyright (c) 2011, 2016 Eurotech and/or its affiliates
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Eurotech
 *******************************************************************************/
package org.eclipse.kura.web.client.settings;

import org.eclipse.kura.web.client.messages.Messages;
import org.eclipse.kura.web.client.util.FailureHandler;
import org.eclipse.kura.web.shared.GwtKuraErrorCode;
import org.eclipse.kura.web.shared.model.GwtSession;
import org.eclipse.kura.web.shared.model.GwtXSRFToken;
import org.eclipse.kura.web.shared.service.GwtCertificatesService;
import org.eclipse.kura.web.shared.service.GwtCertificatesServiceAsync;
import org.eclipse.kura.web.shared.service.GwtSecurityTokenService;
import org.eclipse.kura.web.shared.service.GwtSecurityTokenServiceAsync;

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


public class ApplicationCertsTab extends LayoutContainer {

	private static final Messages MSGS = GWT.create(Messages.class);

	private final static String SERVLET_URL = "/" + GWT.getModuleName() + "/file/certificate";

	private final GwtSecurityTokenServiceAsync gwtXSRFService = GWT.create(GwtSecurityTokenService.class);
	private final GwtCertificatesServiceAsync gwtCertificatesService= GWT.create(GwtCertificatesService.class);

	@SuppressWarnings("unused")
	private GwtSession			m_currentSession;
	private LayoutContainer 	m_commandInput;
	private FormPanel			m_formPanel;
	private TextArea			m_publicCertificate;
	private TextField<String>   m_storageAlias;

	private Button				m_applyButton;
	private Button				m_resetButton;
	private ButtonBar			m_buttonBar;


	public ApplicationCertsTab(GwtSession currentSession) 
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

				gwtXSRFService.generateSecurityToken(new AsyncCallback<GwtXSRFToken> () {
					@Override
					public void onFailure(Throwable ex) {
						FailureHandler.handle(ex);
					}

					@Override
					public void onSuccess(GwtXSRFToken token) {	
						gwtCertificatesService.storeApplicationPublicChain(token, m_publicCertificate.getValue(), m_storageAlias.getValue(), new AsyncCallback<Integer>() {
							public void onFailure(Throwable caught) {
								if(caught.getLocalizedMessage().equals(GwtKuraErrorCode.ILLEGAL_ARGUMENT.toString())){
									Info.display(MSGS.error(), "Error while storing the public keys in the key store");
								}else{
									Info.display(MSGS.error(), caught.getLocalizedMessage());
								}
								m_commandInput.unmask();
							}

							public void onSuccess(Integer certsStored) {
								m_publicCertificate.clear();
								m_storageAlias.clear();
								Info.display(MSGS.info(), "Storage success. Stored " + certsStored + " public keys.");
								m_commandInput.unmask();
							}
						});
					}});
			}
		});

		//
		// Initial description
		// 
		LayoutContainer description = new LayoutContainer();
		description.setBorders(false);
		description.setLayout(new ColumnLayout());

		Label descriptionLabel = new Label(MSGS.settingsAddBundleCertsDescription());

		description.add(descriptionLabel);
		description.setStyleAttribute("padding-bottom", "10px");
		m_formPanel.add(description);

		//
		// Storage alias
		//
		m_storageAlias = new TextField<String>();
		m_storageAlias.setName(MSGS.settingsStorageAliasLabel());
		m_storageAlias.setPassword(false);
		m_storageAlias.setAllowBlank(false);
		m_storageAlias.setEmptyText("* " + MSGS.settingsStorageAliasLabel());
		m_storageAlias.setFieldLabel(MSGS.settingsStorageAliasLabel());
		m_formPanel.add(m_storageAlias, new FormData("95%"));

		//
		// Public Certificate
		//
		m_publicCertificate = new TextArea();
		m_publicCertificate.setBorders(false);
		m_publicCertificate.setReadOnly(false);
		m_publicCertificate.setEmptyText("* " + MSGS.settingsAddCertLabel());
		m_publicCertificate.setName(MSGS.settingsAddCertLabel());
		m_publicCertificate.setAllowBlank(false);
		m_publicCertificate.setFieldLabel(MSGS.settingsAddCertLabel());
		m_formPanel.add(m_publicCertificate, formData);


		m_commandInput = m_formPanel;

		// Main Panel
		ContentPanel deviceCommandPanel = new ContentPanel();
		deviceCommandPanel.setBorders(false);
		deviceCommandPanel.setBodyBorder(false);
		deviceCommandPanel.setHeaderVisible(false);
		deviceCommandPanel.setScrollMode(Scroll.AUTO);
		deviceCommandPanel.setLayout(new FitLayout());
		deviceCommandPanel.add(m_commandInput);

		add(deviceCommandPanel);
	}

	private void initButtonBar() {
		m_applyButton = new Button(MSGS.apply());
		m_applyButton.addSelectionListener(new SelectionListener<ButtonEvent>() {  
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
		m_buttonBar.add(m_applyButton);
	}

	public void refresh() {
		m_commandInput.unmask();
	}

}