package org.eclipse.kura.web.client.bootstrap.ui.Settings;

import org.eclipse.kura.web.client.messages.Messages;
import org.eclipse.kura.web.shared.GwtKuraErrorCode;
import org.eclipse.kura.web.shared.service.GwtBSCertificatesService;
import org.eclipse.kura.web.shared.service.GwtBSCertificatesServiceAsync;
import org.gwtbootstrap3.client.ui.Button;
import org.gwtbootstrap3.client.ui.Form;
import org.gwtbootstrap3.client.ui.FormGroup;
import org.gwtbootstrap3.client.ui.FormLabel;
import org.gwtbootstrap3.client.ui.Input;
import org.gwtbootstrap3.client.ui.TextBox;
import org.gwtbootstrap3.client.ui.base.form.AbstractForm.SubmitCompleteEvent;
import org.gwtbootstrap3.client.ui.base.form.AbstractForm.SubmitCompleteHandler;
import org.gwtbootstrap3.client.ui.constants.ValidationState;
import org.gwtbootstrap3.client.ui.html.Span;
import org.gwtbootstrap3.extras.growl.client.ui.Growl;

import com.google.gwt.core.client.GWT;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.FormPanel;
import com.google.gwt.user.client.ui.HTMLPanel;
import com.google.gwt.user.client.ui.Widget;

public class SslCertificatesTabUi extends Composite {

	private static SslCertificatesTabUiUiBinder uiBinder = GWT
			.create(SslCertificatesTabUiUiBinder.class);

	interface SslCertificatesTabUiUiBinder extends
			UiBinder<Widget, SslCertificatesTabUi> {
	}
	
	private static final Messages MSGS = GWT.create(Messages.class);

	private final static String SERVLET_URL = "/" + GWT.getModuleName() + "/file/certificate";
	private final GwtBSCertificatesServiceAsync gwtCertificatesService = GWT.create(GwtBSCertificatesService.class);
	
	@UiField
	Form sslForm;
	@UiField
	FormGroup groupStoragePassword, groupPublicCertificate, groupStorageAlias;
	@UiField
	HTMLPanel description;
	@UiField
	FormLabel privateCertificate, publicCertificate ,storagePassword, storageAlias;
	@UiField
	TextBox formPrivCert, formPubCert;
	@UiField
	Input formPassword, formStorageAlias;
	@UiField
	Button reset, execute;
	
	public SslCertificatesTabUi() {
		initWidget(uiBinder.createAndBindUi(this));
		
		sslForm.setAction(SERVLET_URL);
		sslForm.setEncoding( FormPanel.ENCODING_MULTIPART);
		sslForm.setMethod(FormPanel.METHOD_POST);
		description.add(new Span("<p>"+MSGS.settingsSSLDescription1()+MSGS.settingsSSLDescription2()+"</p>"));		
		
		privateCertificate.setText(MSGS.settingsPrivateCertLabel());
		formPrivCert.setPlaceholder(MSGS.settingsPrivateCertLabel());
		
		storagePassword.setText(MSGS.settingsStoragePasswordLabel());		
		
		publicCertificate.setText(MSGS.settingsPublicCertLabel());
		formPubCert.setPlaceholder("* "+MSGS.settingsPublicCertLabel());
		formPubCert.setAllowBlank(false);
		
		storageAlias.setText(MSGS.settingsStorageAliasLabel());
		
		execute.setText(MSGS.deviceCommandExecute());
		reset.setText(MSGS.reset());
		
		reset.addClickHandler(new ClickHandler(){
			@Override
			public void onClick(ClickEvent event) {
				sslForm.reset();
			}
		});
		
		execute.addClickHandler(new ClickHandler(){
			@Override
			public void onClick(ClickEvent event) {
				if(blankValidation()){
					sslForm.submit();
				}				
			}
		});
		
		sslForm.addSubmitCompleteHandler(new SubmitCompleteHandler(){
			@Override
			public void onSubmitComplete(SubmitCompleteEvent event) {
				if(formPrivCert.getText()!=null && formPrivCert.getText()!=""){
					gwtCertificatesService.storePrivateSSLCertificate(formPrivCert.getValue(), formPubCert.getValue(), formPassword.getValue(), formStorageAlias.getText(), new AsyncCallback<Integer>() {
						@Override
						public void onFailure(Throwable caught) {
							if(caught.getLocalizedMessage().equals(GwtKuraErrorCode.ILLEGAL_ARGUMENT.toString())){
								Growl.growl(MSGS.error()+": ", "Error while storing the private certificate in the key store");
							}else{
								Growl.growl(MSGS.error()+": ", caught.getLocalizedMessage());
							}
							
						}

						@Override
						public void onSuccess(Integer result) {
							sslForm.reset();
							Growl.growl(MSGS.info()+": ", "Storage success. Stored private and public certificates.");
						}
						
					});
				} else {
					gwtCertificatesService.storePublicSSLCertificate(formPubCert.getValue(), formStorageAlias.getText(), new AsyncCallback<Integer>() {
						@Override
						public void onFailure(Throwable caught) {
							if(caught.getLocalizedMessage().equals(GwtKuraErrorCode.ILLEGAL_ARGUMENT.toString())){
								Growl.growl(MSGS.error()+": ", "Error while storing the public certificate(s) in the key store");
							}else{
								Growl.growl(MSGS.error()+": ", caught.getLocalizedMessage());
							}
							
						}

						@Override
						public void onSuccess(Integer result) {
							sslForm.reset();
							Growl.growl(MSGS.info()+": ", "Storage success. Stored " + result + " public certificate(s).");
						}
					});
						
				}
			}});
	}

	private boolean blankValidation(){
		boolean flag=true;
		if(formPassword.getText()==null || formPassword.getText()==""){
			groupStoragePassword.setValidationState(ValidationState.ERROR);
			flag=false;
		}else {
			groupStoragePassword.setValidationState(ValidationState.NONE);
		}
		
		if(formPubCert.getText()==null || formPubCert.getText()==""){
			groupPublicCertificate.setValidationState(ValidationState.ERROR);
			flag=false;
		}else {
			groupPublicCertificate.setValidationState(ValidationState.NONE);
		}
		
		if(formStorageAlias.getText()==null || formStorageAlias.getText()==""){
			groupStorageAlias.setValidationState(ValidationState.ERROR);
			flag=false;
		}else {
			groupStorageAlias.setValidationState(ValidationState.NONE);
		}
		
		return flag;
	}
	
}
