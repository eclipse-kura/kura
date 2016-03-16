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
import org.gwtbootstrap3.client.ui.AnchorButton;
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

public class ServerCertsTabUi extends Composite implements Tab {

	private static ServerCertsTabUiUiBinder uiBinder = GWT.create(ServerCertsTabUiUiBinder.class);

	interface ServerCertsTabUiUiBinder extends UiBinder<Widget, ServerCertsTabUi> {
	}
	
	private static final Messages MSGS = GWT.create(Messages.class);

	private final static String SERVLET_URL = "/" + GWT.getModuleName() + "/file/certificate";

	private final GwtSecurityTokenServiceAsync gwtXSRFService = GWT.create(GwtSecurityTokenService.class);
	private final GwtCertificatesServiceAsync gwtCertificatesService = GWT.create(GwtCertificatesService.class);

	private boolean dirty;
	
	@UiField
	HTMLPanel description;
	@UiField
	Form serverSslCertsForm;
	@UiField
	FormGroup groupStorageAliasForm;
	@UiField
	FormGroup groupCertForm;
	@UiField
	FormLabel storageAliasLabel;
	@UiField
	FormLabel certificateLabel;
	@UiField
	Input storageAliasInput;
	@UiField
	TextArea certificateInput;
	@UiField
	AnchorButton reset, execute;

	public ServerCertsTabUi() {
		initWidget(uiBinder.createAndBindUi(this));
		initForm();
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
		boolean validAppCert= isServerCertValid();
		if (validAlias && validAppCert) {
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
		serverSslCertsForm.setAction(SERVLET_URL);
		serverSslCertsForm.setEncoding(FormPanel.ENCODING_MULTIPART);
		serverSslCertsForm.setMethod(FormPanel.METHOD_POST);
		StringBuilder title= new StringBuilder();
		title.append("<p>");
		title.append(MSGS.settingsAddCertDescription1());
		title.append(" ");
		title.append(MSGS.settingsAddCertDescription2());
		title.append("</p>");
		description.add(new Span(title.toString()));
		serverSslCertsForm.addSubmitCompleteHandler(new SubmitCompleteHandler(){
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
						gwtCertificatesService.storeSSLPublicChain(token, storageAliasInput.getValue(), certificateInput.getValue(), new AsyncCallback<Integer>() {
							public void onFailure(Throwable caught) {
								FailureHandler.handle(caught);
								EntryClassUi.hideWaitModal();
							}

							public void onSuccess(Integer certsStored) {
								reset();
								setDirty(false);
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
			}
		});

		certificateLabel.setText(MSGS.settingsPublicCertLabel());
		certificateInput.setVisibleLines(20);
		certificateInput.addChangeHandler(new ChangeHandler() {
			@Override
			public void onChange(ChangeEvent event) {
				isServerCertValid();
				setDirty(true);
			}
		});

		execute.setText(MSGS.deviceCommandExecute());
		reset.setText(MSGS.reset());

		reset.addClickHandler(new ClickHandler(){
			@Override
			public void onClick(ClickEvent event) {
				reset();
				setDirty(false);
			}
		});

		execute.addClickHandler(new ClickHandler(){
			@Override
			public void onClick(ClickEvent event) {
				if(isValid()){
					EntryClassUi.showWaitModal();
					serverSslCertsForm.submit();
				}				
			}
		});
	}
	
	private void reset() {
		storageAliasInput.setText("");
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

	private boolean isServerCertValid() {
		if(certificateInput.getText() == null ||  "".equals(certificateInput.getText().trim())){
			groupCertForm.setValidationState(ValidationState.ERROR);
			return false;
		}else {
			groupCertForm.setValidationState(ValidationState.NONE);
			return true;
		}
	}
}
