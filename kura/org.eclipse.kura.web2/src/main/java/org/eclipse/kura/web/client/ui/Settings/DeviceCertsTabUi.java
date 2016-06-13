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

import org.eclipse.kura.web.client.messages.Messages;
import org.eclipse.kura.web.client.ui.EntryClassUi;
import org.eclipse.kura.web.client.ui.Tab;
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

public class DeviceCertsTabUi extends Composite implements Tab {

	private static DeviceCertsTabUiUiBinder uiBinder = GWT.create(DeviceCertsTabUiUiBinder.class);

	interface DeviceCertsTabUiUiBinder extends UiBinder<Widget, DeviceCertsTabUi> {
	}
	
	private static final Messages MSGS = GWT.create(Messages.class);

	private final static String SERVLET_URL = "/" + GWT.getModuleName() + "/file/certificate";

	private final GwtSecurityTokenServiceAsync gwtXSRFService = GWT.create(GwtSecurityTokenService.class);
	private final GwtCertificatesServiceAsync gwtCertificatesService = GWT.create(GwtCertificatesService.class);

	private boolean dirty;
	
	@UiField
	HTMLPanel description;
	@UiField
	Form deviceSslCertsForm;
	@UiField
	FormGroup groupStorageAliasForm;
	@UiField
	FormGroup groupPrivateKeyForm;
	@UiField
	FormGroup groupCertForm;
	@UiField
	FormLabel storageAliasLabel;
	@UiField
	FormLabel privateKeyLabel;
	@UiField
	FormLabel certificateLabel;
	@UiField
	Input storageAliasInput;
	@UiField
	TextArea privateKeyInput;
	@UiField
	TextArea certificateInput;
	@UiField
	Button reset;
	@UiField
	Button apply;

	public DeviceCertsTabUi() {
		initWidget(uiBinder.createAndBindUi(this));
		initForm();
		
		setDirty(false);
		apply.setEnabled(false);
		reset.setEnabled(false);
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
	public boolean isValid() {
		boolean validAlias= isAliasValid();
		boolean validPrivateKey= isPrivateKeyValid();
		boolean validDeviceCert= isDeviceCertValid();
		if (	validAlias      && 
				validPrivateKey && 
				validDeviceCert ) {
			return true;
		}
		return false;
	}

	@Override
	public void refresh() {
		if (isDirty()) {
			setDirty(false);
			reset();
		}
	}
	
	private void initForm() {
		deviceSslCertsForm.setAction(SERVLET_URL);
		deviceSslCertsForm.setEncoding(FormPanel.ENCODING_MULTIPART);
		deviceSslCertsForm.setMethod(FormPanel.METHOD_POST);
		StringBuilder title= new StringBuilder();
		title.append("<p>");
		title.append(MSGS.settingsMAuthDescription1());
		title.append(" ");
		title.append(MSGS.settingsMAuthDescription2());
		title.append("</p>");
		description.add(new Span(title.toString()));
		deviceSslCertsForm.addSubmitCompleteHandler(new SubmitCompleteHandler(){
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
						gwtCertificatesService.storePublicPrivateKeys(token, privateKeyInput.getValue(), certificateInput.getValue(), null, storageAliasInput.getValue(), new AsyncCallback<Integer>() {
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
		storageAliasInput.addChangeHandler(new ChangeHandler() {
			@Override
			public void onChange(ChangeEvent event) {
				isAliasValid();
				setDirty(true);
				apply.setEnabled(true);
				reset.setEnabled(true);
			}
		});
		
		privateKeyLabel.setText(MSGS.settingsPrivateCertLabel());
		privateKeyInput.setVisibleLines(20);
		privateKeyInput.addChangeHandler(new ChangeHandler() {
			@Override
			public void onChange(ChangeEvent event) {
				isPrivateKeyValid();
				setDirty(true);
				apply.setEnabled(true);
				reset.setEnabled(true);
			}
		});

		certificateLabel.setText(MSGS.settingsPublicCertLabel());
		certificateInput.setVisibleLines(20);
		certificateInput.addChangeHandler(new ChangeHandler() {
			@Override
			public void onChange(ChangeEvent event) {
				isDeviceCertValid();
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
					deviceSslCertsForm.submit();
				}				
			}
		});
	}
	
	private void reset() {
		storageAliasInput.setText("");
		privateKeyInput.setText("");
		certificateInput.setText("");
	}
	
	private boolean isAliasValid() {
		if(storageAliasInput.getText() == null || "".equals(storageAliasInput.getText().trim())){
			groupStorageAliasForm.setValidationState(ValidationState.ERROR);
			return false;
		}else {
			groupStorageAliasForm.setValidationState(ValidationState.NONE);
			return true;
		}
	}
	
	private boolean isPrivateKeyValid() {
		if(certificateInput.getText() == null ||  "".equals(certificateInput.getText().trim())){
			groupCertForm.setValidationState(ValidationState.ERROR);
			return false;
		}else {
			groupCertForm.setValidationState(ValidationState.NONE);
			return true;
		}
	}

	private boolean isDeviceCertValid() {
		if(certificateInput.getText() == null ||  "".equals(certificateInput.getText().trim())){
			groupCertForm.setValidationState(ValidationState.ERROR);
			return false;
		}else {
			groupCertForm.setValidationState(ValidationState.NONE);
			return true;
		}
	}
}
