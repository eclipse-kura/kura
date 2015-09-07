package org.eclipse.kura.web.client.bootstrap.ui.Device;

import org.eclipse.kura.web.client.messages.Messages;
import org.eclipse.kura.web.shared.GwtKuraErrorCode;
import org.eclipse.kura.web.shared.service.GwtBSDeviceService;
import org.eclipse.kura.web.shared.service.GwtBSDeviceServiceAsync;
import org.gwtbootstrap3.client.ui.Button;
import org.gwtbootstrap3.client.ui.Input;
import org.gwtbootstrap3.client.ui.PanelBody;
import org.gwtbootstrap3.client.ui.TextBox;

import com.google.gwt.core.client.GWT;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.dom.client.KeyCodes;
import com.google.gwt.event.dom.client.KeyDownEvent;
import com.google.gwt.event.dom.client.KeyDownHandler;
import com.google.gwt.safehtml.shared.SafeHtmlBuilder;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.FileUpload;
import com.google.gwt.user.client.ui.FormPanel;
import com.google.gwt.user.client.ui.FormPanel.SubmitCompleteEvent;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.Widget;

public class CommandTabUi extends Composite {

	private static CommandTabUiUiBinder uiBinder = GWT
			.create(CommandTabUiUiBinder.class);

	private static final Messages MSGS = GWT.create(Messages.class);
	private static final String SERVLET_URL = "/" + GWT.getModuleName()
			+ "/file/command";

	interface CommandTabUiUiBinder extends UiBinder<Widget, CommandTabUi> {
	}

	private final GwtBSDeviceServiceAsync gwtDeviceService = GWT
			.create(GwtBSDeviceService.class);

	@UiField
	TextBox formExecute;
	@UiField
	Input formPassword;
	@UiField
	Button reset, execute;
	@UiField
	FormPanel commandForm;
	@UiField
	FileUpload docPath;
	@UiField
	PanelBody resultPanel;

	String command, password;
	SafeHtmlBuilder safeHtml= new SafeHtmlBuilder();
	
	public CommandTabUi() {
		initWidget(uiBinder.createAndBindUi(this));

		formExecute.clear();
		formExecute.setFocus(true);
		formExecute.addKeyDownHandler(new KeyDownHandler() {
			@Override
			public void onKeyDown(KeyDownEvent event) {
				if (event.getNativeKeyCode() == KeyCodes.KEY_ENTER) {
					formPassword.setFocus(true);
				}
			}
		});

		formPassword.setText("");
		formPassword.addKeyDownHandler(new KeyDownHandler() {
			@Override
			public void onKeyDown(KeyDownEvent event) {
				if (event.getNativeKeyCode() == KeyCodes.KEY_ENTER) {
					execute.setFocus(true);
				}
			}
		});

		
		display(MSGS.deviceCommandNoOutput());
		
		reset.setText(MSGS.reset());
		reset.addClickHandler(new ClickHandler() {
			@Override
			public void onClick(ClickEvent event) {
				commandForm.reset();
				formExecute.setFocus(true);				
				display(MSGS.deviceCommandNoOutput());
			}
		});

		execute.setText(MSGS.deviceCommandExecute());
		execute.addClickHandler(new ClickHandler() {
			@Override
			public void onClick(ClickEvent event) {
				
				commandForm.submit();
				formExecute.setFocus(true);
				// http://www.gwtproject.org/javadoc/latest/com/google/gwt/user/client/ui/FileUpload.html
			}
		});

		commandForm.setEncoding(FormPanel.ENCODING_MULTIPART);
		commandForm.setMethod(FormPanel.METHOD_POST);
		commandForm.setAction(SERVLET_URL);
		commandForm
				.addSubmitCompleteHandler(new FormPanel.SubmitCompleteHandler() {
					@Override
					public void onSubmitComplete(SubmitCompleteEvent event) {

						String result = event.getResults();
						String command = formExecute.getText();
						String password = formPassword.getText();
						if (password.isEmpty() || password.equals(null)) {
							password = null;
						}

						if (result.startsWith("HTTP ERROR")) {							
							display(MSGS.fileUploadFailure());
						} else {
							gwtDeviceService.executeCommand(command, password,
									new AsyncCallback<String>() {

										@Override
										public void onFailure(Throwable caught) {
											
											
											if (caught
													.getLocalizedMessage()
													.equals(GwtKuraErrorCode.SERVICE_NOT_ENABLED
															.toString())) {
												display(MSGS
														.error()
														+ "\n"
														+ MSGS.commandServiceNotEnabled());

											} else if (caught
													.getLocalizedMessage()
													.equals(GwtKuraErrorCode.ILLEGAL_ARGUMENT
															.toString())) {
												display(MSGS
														.error()
														+ "\n"
														+ MSGS.commandPasswordNotCorrect());
											} else {
												display(MSGS.error()
														+ "\n"
														+ caught.getLocalizedMessage());
												
											}

										}

										@Override
										public void onSuccess(String result) {											
											display(result);
										}

									});

						}

					}

				});

	}
	
	public void display(String string){
		resultPanel.clear();		
		resultPanel.add(new HTML((new SafeHtmlBuilder().appendEscapedLines(string).toSafeHtml())));

	}

}
