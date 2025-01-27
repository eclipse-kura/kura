/*******************************************************************************
 * Copyright (c) 2025 Eurotech and/or its affiliates and others
 * 
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 * 
 * SPDX-License-Identifier: EPL-2.0
 * 
 * Contributors:
 *  Eurotech
 *******************************************************************************/
package org.eclipse.kura.web.client.ui.wires;

import org.eclipse.kura.web.Console;
import org.eclipse.kura.web.client.messages.Messages;
import org.eclipse.kura.web.client.util.request.RequestQueue;
import org.eclipse.kura.web.shared.service.GwtSecurityTokenService;
import org.eclipse.kura.web.shared.service.GwtSecurityTokenServiceAsync;
import org.gwtbootstrap3.client.ui.Button;
import org.gwtbootstrap3.client.ui.Form;
import org.gwtbootstrap3.client.ui.Modal;
import org.gwtbootstrap3.client.ui.base.form.AbstractForm.SubmitCompleteEvent;
import org.gwtbootstrap3.client.ui.html.Paragraph;

import com.google.gwt.core.client.GWT;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.Hidden;
import com.google.gwt.user.client.ui.Widget;

public class WiregraphDownloadModal extends Composite {

    private static final String XML_DOWNLOAD_FORMAT = "XML";
    private static final String JSON_DOWNLOAD_FORMAT = "JSON";

    private static final Messages MSGS = GWT.create(Messages.class);
    private static WiregraphDownloadModalUiBinder uiBinder = GWT.create(WiregraphDownloadModalUiBinder.class);
    private final GwtSecurityTokenServiceAsync gwtXSRFService = GWT.create(GwtSecurityTokenService.class);

    interface WiregraphDownloadModalUiBinder extends UiBinder<Widget, WiregraphDownloadModal> {
    }

    @UiField
    Modal wiregraphModal;
    @UiField
    Form wiregraphForm;
    @UiField
    Paragraph wiregraphModalDescription;
    @UiField
    Paragraph wiregraphModalHint;
    @UiField
    Button downloadXml;
    @UiField
    Button downloadJson;
    @UiField
    Button cancelButton;

    Hidden xsrfTokenField;
    Hidden wiregraphDownloadFormatField;

    HandlerRegistration cancelHandler;
    HandlerRegistration downloadHandler;
    HandlerRegistration xmlDownloadHandler;
    HandlerRegistration jsonDownloadHandler;

    AsyncCallback<SubmitCompleteEvent> downloadCallback;

    public WiregraphDownloadModal() {
        initWidget(uiBinder.createAndBindUi(this));
        this.wiregraphModal.setTitle(MSGS.deviceWiregraphDownloadModalTitle());
    }

    public void show() {
        initHiddenFields();
        initWiregraphButtons();
        this.wiregraphModal.show();

    }

    /*
     * Initializers
     */

    private void initWiregraphButtons() {

        clearClickHandlers();

        this.cancelHandler = this.cancelButton.addClickHandler(this::onCancelClick);

        this.downloadHandler = this.wiregraphForm
                .addSubmitCompleteHandler(event -> this.downloadCallback.onSuccess(event));

        this.jsonDownloadHandler = this.downloadJson.addClickHandler(e -> onWiregraphDownload(JSON_DOWNLOAD_FORMAT));

        this.xmlDownloadHandler = this.downloadXml.addClickHandler(e -> onWiregraphDownload(XML_DOWNLOAD_FORMAT));
    }

    private void initHiddenFields() {

        clearHiddenFields();

        this.wiregraphForm.setEncoding(com.google.gwt.user.client.ui.FormPanel.ENCODING_URLENCODED);
        this.wiregraphForm.setMethod(com.google.gwt.user.client.ui.FormPanel.METHOD_POST);
        this.wiregraphForm.setAction(Console.ADMIN_ROOT + '/' + GWT.getModuleName() + "/wiresSnapshot");

        this.xsrfTokenField = new Hidden();
        this.xsrfTokenField.setID("xsrfToken");
        this.xsrfTokenField.setName("xsrfToken");
        this.xsrfTokenField.setValue("");
        this.wiregraphForm.add(this.xsrfTokenField);

        this.wiregraphDownloadFormatField = new Hidden();
        this.wiregraphDownloadFormatField.setID("downloadFormat");
        this.wiregraphDownloadFormatField.setName("downloadFormat");
        this.wiregraphDownloadFormatField.setValue("");
        this.wiregraphForm.add(this.wiregraphDownloadFormatField);
    }

    /*
     * OnEvent Methods
     */

    private void onCancelClick(ClickEvent handler) {
        this.wiregraphModal.hide();
        clearClickHandlers();
        clearHiddenFields();
    }

    private void onWiregraphDownload(String downloadFormat) {
        RequestQueue.submit(context -> this.gwtXSRFService.generateSecurityToken(context.callback(token -> {
            xsrfTokenField.setValue(token.getToken());
            wiregraphDownloadFormatField.setValue(downloadFormat);
            wiregraphForm.submit();
        })));

        this.wiregraphModal.hide();
    }

    /*
     * Utils
     */

    private void clearClickHandlers() {
        if (this.cancelHandler != null) {
            this.cancelHandler.removeHandler();
        }

        if (this.downloadHandler != null) {
            this.downloadHandler.removeHandler();
        }

        if (this.jsonDownloadHandler != null) {
            this.jsonDownloadHandler.removeHandler();
        }

        if (this.xmlDownloadHandler != null) {
            this.xmlDownloadHandler.removeHandler();
        }
    }

    private void clearHiddenFields() {
        if (this.xsrfTokenField != null) {
            this.wiregraphForm.remove(this.xsrfTokenField);
        }

        if (this.wiregraphDownloadFormatField != null) {
            this.wiregraphForm.remove(this.wiregraphDownloadFormatField);
        }
    }
}
