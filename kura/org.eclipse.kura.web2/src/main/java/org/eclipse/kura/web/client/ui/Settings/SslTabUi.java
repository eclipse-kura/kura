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
import org.eclipse.kura.web.shared.model.GwtSslConfig;
import org.eclipse.kura.web.shared.model.GwtXSRFToken;
import org.eclipse.kura.web.shared.service.GwtSecurityTokenService;
import org.eclipse.kura.web.shared.service.GwtSecurityTokenServiceAsync;
import org.eclipse.kura.web.shared.service.GwtSslService;
import org.eclipse.kura.web.shared.service.GwtSslServiceAsync;
import org.gwtbootstrap3.client.ui.Button;
import org.gwtbootstrap3.client.ui.ButtonGroup;
import org.gwtbootstrap3.client.ui.Form;
import org.gwtbootstrap3.client.ui.FormGroup;
import org.gwtbootstrap3.client.ui.FormLabel;
import org.gwtbootstrap3.client.ui.Input;
import org.gwtbootstrap3.client.ui.Modal;
import org.gwtbootstrap3.client.ui.ModalBody;
import org.gwtbootstrap3.client.ui.ModalFooter;
import org.gwtbootstrap3.client.ui.ModalHeader;
import org.gwtbootstrap3.client.ui.RadioButton;
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

public class SslTabUi extends Composite implements Tab {

	private static SslTabUiUiBinder uiBinder = GWT.create(SslTabUiUiBinder.class);

	interface SslTabUiUiBinder extends UiBinder<Widget, SslTabUi> {
	}

	private static final Messages MSGS = GWT.create(Messages.class);

	private final GwtSecurityTokenServiceAsync gwtXSRFService = GWT.create(GwtSecurityTokenService.class);
	private final GwtSslServiceAsync gwtSslService= GWT.create(GwtSslService.class);

	private boolean dirty;
	private Modal modal;

	@UiField
	HTMLPanel description;

	@UiField
	Button apply;
	@UiField
	Button reset;

	@UiField
	Form sslForm;

	@UiField
	FormGroup defaultProtocolFormGroup;
	@UiField
	FormGroup hostnameVerificationFormGroup;
	@UiField
	FormGroup keystorePathFormGroup;
	@UiField
	FormGroup keystorePasswordFormGroup;
	@UiField
	FormGroup cipherSuitesFormGroup;

	@UiField
	FormLabel defaultProtocolFormLabel;
	@UiField
	FormLabel hostnameVerificationFormLabel;
	@UiField
	FormLabel keystorePathFormLabel;
	@UiField
	FormLabel keystorePasswordFormLabel;
	@UiField
	FormLabel cipherSuitesFormLabel;

	@UiField
	Input defaultProtocolInput;
	@UiField
	Input keystorePathInput;
	@UiField
	Input keystorePasswordInput;
	@UiField
	Input cipherSuitesInput;

	@UiField
	RadioButton radio1;
	@UiField
	RadioButton radio2;


	public SslTabUi() {
		initWidget(uiBinder.createAndBindUi(this));

		initButtonBar();
		initForm();

		//loadData();

		setDirty(false);
		apply.setEnabled(false);
		reset.setEnabled(false);
	}

	@Override
	public void setDirty(boolean flag) {
		dirty= flag;
	}

	@Override
	public boolean isDirty() {
		return dirty;
	}

	@Override
	public boolean isValid() {
		return true;
	}

	@Override
	public void refresh() {
//		if (isDirty()) {  //necessary due to the fact that if enabled the loadData in the constructor, we get an XSRF error. If setDirty is set to true, it causes a problem to switch tab.
			setDirty(false);
			loadData();
//		}
	}

	private void initButtonBar() {
		apply.setText(MSGS.apply());
		apply.addClickHandler(new ClickHandler() {
			@Override
			public void onClick(ClickEvent event) {
				apply();
			}
		});

		reset.setText(MSGS.reset());
		reset.addClickHandler(new ClickHandler() {
			@Override
			public void onClick(ClickEvent event) {
				reset();
			}
		});
	}

	private void initForm() {
		description.add(new Span("<p>" + MSGS.settingsSSLConfigurationDescription() + "</p>"));
		defaultProtocolFormLabel.setText(MSGS.settingsSSLConfigurationProtocol());
		hostnameVerificationFormLabel.setText("Hostname Verification"); //TODO: externalize string
		radio1.setText(MSGS.trueLabel());
		radio2.setText(MSGS.falseLabel());
		keystorePathFormLabel.setText(MSGS.settingsSSLConfigurationKeystorePath());
		keystorePasswordFormLabel.setText(MSGS.settingsSSLConfigurationKeystorePassword());
		cipherSuitesFormLabel.setText(MSGS.settingsSSLConfigurationCipherSuites());


		defaultProtocolInput.addChangeHandler(new ChangeHandler() {
			@Override
			public void onChange(ChangeEvent event) {
				setDirty(true);
				apply.setEnabled(true);
				reset.setEnabled(true);
			}
		});

		radio1.addChangeHandler(new ChangeHandler() {
			@Override
			public void onChange(ChangeEvent event) {
				setDirty(true);
				apply.setEnabled(true);
				reset.setEnabled(true);
			}
		});

		radio2.addChangeHandler(new ChangeHandler() {
			@Override
			public void onChange(ChangeEvent event) {
				setDirty(true);
				apply.setEnabled(true);
				reset.setEnabled(true);
			}
		});

		keystorePathInput.addChangeHandler(new ChangeHandler() {
			@Override
			public void onChange(ChangeEvent event) {
				setDirty(true);
				apply.setEnabled(true);
				reset.setEnabled(true);
			}
		});

		keystorePasswordInput.addChangeHandler(new ChangeHandler() {
			@Override
			public void onChange(ChangeEvent event) {
				setDirty(true);
				apply.setEnabled(true);
				reset.setEnabled(true);
			}
		});

		cipherSuitesInput.addChangeHandler(new ChangeHandler() {
			@Override
			public void onChange(ChangeEvent event) {
				setDirty(true);
				apply.setEnabled(true);
				reset.setEnabled(true);
			}
		});
	}

	private void apply() {		
		if (isValid() && isDirty()){
			EntryClassUi.showWaitModal();
			gwtXSRFService.generateSecurityToken(new AsyncCallback<GwtXSRFToken> () {
				@Override
				public void onFailure(Throwable ex) {
					FailureHandler.handle(ex);
					EntryClassUi.hideWaitModal();
				}

				@Override
				public void onSuccess(GwtXSRFToken token) {	
					GwtSslConfig sslConfig= new GwtSslConfig();
					sslConfig.setProtocol(defaultProtocolInput.getValue());
					if(radio1.getValue()){
						sslConfig.setHostnameVerification(true);
					} else {
						sslConfig.setHostnameVerification(false);
					}
					sslConfig.setKeyStore(keystorePathInput.getValue());
					sslConfig.setKeystorePassword(keystorePasswordInput.getValue());
					sslConfig.setCiphers(cipherSuitesInput.getValue());

					gwtSslService.updateSslConfiguration(token, sslConfig, new AsyncCallback<Void>() {
						public void onFailure(Throwable caught) {
							FailureHandler.handle(caught);
							EntryClassUi.hideWaitModal();
						}

						public void onSuccess(Void result) {
							//refresh();
							setDirty(false);
							apply.setEnabled(false);
							reset.setEnabled(false);
							EntryClassUi.hideWaitModal();
						}
					});
				}});
		}
	}

	private void reset() {
		if (isDirty()) {
			//Modal
			modal = new Modal();

			ModalHeader header = new ModalHeader();
			header.setTitle(MSGS.confirm());
			modal.add(header);

			ModalBody body = new ModalBody();			
			body.add(new Span(MSGS.deviceConfigDirty()));
			modal.add(body);

			ModalFooter footer = new ModalFooter();
			ButtonGroup group= new ButtonGroup();
			Button yes = new Button();
			yes.setText(MSGS.yesButton());
			yes.addClickHandler(new ClickHandler(){
				@Override
				public void onClick(ClickEvent event) {
					modal.hide();	
					refresh();
					apply.setEnabled(false);
					reset.setEnabled(false);
					setDirty(false);
				}
			});
			group.add(yes);
			Button no = new Button();
			no.setText(MSGS.noButton());
			no.addClickHandler(new ClickHandler(){
				@Override
				public void onClick(ClickEvent event) {
					modal.hide();	
				}
			});						
			group.add(no);
			footer.add(group);
			modal.add(footer);
			modal.show();							
		}
	}

	private void loadData(){
		gwtXSRFService.generateSecurityToken(new AsyncCallback<GwtXSRFToken> () {
			@Override
			public void onFailure(Throwable ex) {
				FailureHandler.handle(ex);
			}

			@Override
			public void onSuccess(GwtXSRFToken token) {	
				gwtSslService.getSslConfiguration(token, new AsyncCallback<GwtSslConfig>() {
					public void onFailure(Throwable caught) {
						FailureHandler.handle(caught);
					}

					public void onSuccess(GwtSslConfig sslConfig) {
						defaultProtocolInput.setValue(sslConfig.getProtocol());
						if(sslConfig.isHostnameVerification()){
							radio1.setActive(true);
							radio2.setActive(false);
						} else {
							radio1.setActive(false);
							radio2.setActive(true);
						}

						keystorePathInput.setValue(sslConfig.getKeyStore());
						keystorePasswordInput.setValue(sslConfig.getKeystorePassword());
						cipherSuitesInput.setValue(sslConfig.getCiphers());
					}
				});
			}
		});
	}
}
