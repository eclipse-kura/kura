/*******************************************************************************
 * Copyright (c) 2011, 2016 Eurotech and/or its affiliates
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Eurotech
 *******************************************************************************/
package org.eclipse.kura.web.client.ui.Settings;

import org.eclipse.kura.web.client.Tab;
import org.eclipse.kura.web.client.messages.Messages;
import org.eclipse.kura.web.client.ui.EntryClassUi;
import org.eclipse.kura.web.client.util.FailureHandler;
import org.eclipse.kura.web.shared.model.GwtXSRFToken;
import org.eclipse.kura.web.shared.service.GwtCertificatesService;
import org.eclipse.kura.web.shared.service.GwtCertificatesServiceAsync;
import org.eclipse.kura.web.shared.service.GwtSecurityTokenService;
import org.eclipse.kura.web.shared.service.GwtSecurityTokenServiceAsync;
import org.gwtbootstrap3.client.ui.Button;
import org.gwtbootstrap3.client.ui.Form;
import org.gwtbootstrap3.client.ui.FormGroup;
import org.gwtbootstrap3.client.ui.FormLabel;
import org.gwtbootstrap3.client.ui.Input;
import org.gwtbootstrap3.client.ui.TextArea;
import org.gwtbootstrap3.client.ui.base.form.AbstractForm.SubmitCompleteEvent;
import org.gwtbootstrap3.client.ui.base.form.AbstractForm.SubmitCompleteHandler;
import org.gwtbootstrap3.client.ui.constants.ValidationState;
import org.gwtbootstrap3.client.ui.gwt.FormPanel;
import org.gwtbootstrap3.client.ui.html.Span;

import com.google.gwt.core.client.GWT;
import com.google.gwt.event.dom.client.ChangeEvent;
import com.google.gwt.event.dom.client.ChangeHandler;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.HTMLPanel;
import com.google.gwt.user.client.ui.Widget;

public class ApplicationCertsTabUi extends Composite implements Tab {

	private static ApplicationCertsTabUiUiBinder uiBinder = GWT.create(ApplicationCertsTabUiUiBinder.class);

	interface ApplicationCertsTabUiUiBinder extends UiBinder<Widget, ApplicationCertsTabUi> {
	}

	private static final Messages MSGS = GWT.create(Messages.class);

	private final static String SERVLET_URL = "/" + GWT.getModuleName() + "/file/certificate";

	private final GwtSecurityTokenServiceAsync gwtXSRFService = GWT.create(GwtSecurityTokenService.class);
	private final GwtCertificatesServiceAsync gwtCertificatesService = GWT.create(GwtCertificatesService.class);

	private boolean dirty;
	
	@UiField
	Form appCertsForm;
	@UiField
	FormGroup groupStorageAlias;
	@UiField
	FormGroup groupFormCert;
	@UiField
	HTMLPanel description;
	@UiField
	FormLabel storageAliasLabel;
	@UiField
	FormLabel certificateLabel;
	@UiField
	TextArea formCert;
	@UiField
	Input formStorageAlias;
	@UiField
	Button reset;
	@UiField
	Button apply;


	public ApplicationCertsTabUi() {
		initWidget(uiBinder.createAndBindUi(this));
		initForm();
		
		setDirty(false);
		apply.setEnabled(false);
		reset.setEnabled(false);
	}

	@Override
	public boolean isValid() {
		boolean validAlias= isAliasValid();
		boolean validAppCert= isAppCertValid();
		if (validAlias && validAppCert) {
			return true;
		}
		return false;
	}

	@Override
	public void setDirty(boolean flag) {
		dirty = flag;
	}

	@Override
	public boolean isDirty() {
		return dirty;
	}

	@Override
	public void refresh() {
		if (isDirty()) {
			setDirty(false);
			reset();
		}
	}
	
	private void initForm() {
		appCertsForm.setAction(SERVLET_URL);
		appCertsForm.setEncoding(FormPanel.ENCODING_MULTIPART);
		appCertsForm.setMethod(FormPanel.METHOD_POST);
		description.add(new Span("<p>"+MSGS.settingsAddBundleCertsDescription()+"</p>"));
		appCertsForm.addSubmitCompleteHandler(new SubmitCompleteHandler(){
			@Override
			public void onSubmitComplete(SubmitCompleteEvent event) {
				gwtXSRFService.generateSecurityToken(new AsyncCallback<GwtXSRFToken> () {
					@Override
					public void onFailure(Throwable ex) {
						FailureHandler.handle(ex);
						EntryClassUi.hideWaitModal();
					}

					@Override
					public void onSuccess(GwtXSRFToken token) {	
						gwtCertificatesService.storeApplicationPublicChain(token, formCert.getValue(), formStorageAlias.getValue(), new AsyncCallback<Integer>() {
							public void onFailure(Throwable caught) {
								FailureHandler.handle(caught);
								EntryClassUi.hideWaitModal();
							}

							public void onSuccess(Integer certsStored) {
								reset();
								setDirty(false);
								apply.setEnabled(false);
								reset.setEnabled(false);
								EntryClassUi.hideWaitModal();
							}
						});
					}});
			}
		}
		);
		
		storageAliasLabel.setText(MSGS.settingsStorageAliasLabel());
		formStorageAlias.addChangeHandler(new ChangeHandler() {
			@Override
			public void onChange(ChangeEvent event) {
				isAliasValid();
				setDirty(true);
				apply.setEnabled(true);
				reset.setEnabled(true);
			}
		});

		certificateLabel.setText(MSGS.settingsAddCertLabel()); //TODO: this is a wrong label. Change it!
		formCert.setVisibleLines(20);
		formCert.addChangeHandler(new ChangeHandler() {
			@Override
			public void onChange(ChangeEvent event) {
				isAppCertValid();
				setDirty(true);
				apply.setEnabled(true);
				reset.setEnabled(true);
			}
		});

		reset.setText(MSGS.reset());
		reset.addClickHandler(new ClickHandler(){
			@Override
			public void onClick(ClickEvent event) {
				reset();
				setDirty(false);
				apply.setEnabled(false);
				reset.setEnabled(false);
			}
		});

		apply.setText(MSGS.apply());
		apply.addClickHandler(new ClickHandler(){
			@Override
			public void onClick(ClickEvent event) {
				if(isValid()){
					EntryClassUi.showWaitModal();
					appCertsForm.submit();
				}				
			}
		});
	}
	
	private void reset() {
		formStorageAlias.setText("");
		formCert.setText("");
	}
	
	private boolean isAliasValid() {
		if(formStorageAlias.getText() == null || "".equals(formStorageAlias.getText().trim())){
			groupStorageAlias.setValidationState(ValidationState.ERROR);
			return false;
		}else {
			groupStorageAlias.setValidationState(ValidationState.NONE);
			return true;
		}
	}

	private boolean isAppCertValid() {
		if(formCert.getText() == null ||  "".equals(formCert.getText().trim())){
			groupFormCert.setValidationState(ValidationState.ERROR);
			return false;
		}else {
			groupFormCert.setValidationState(ValidationState.NONE);
			return true;
		}
	}
}