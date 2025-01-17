/*******************************************************************************
 * Copyright (c) 2024, 2025 Eurotech and/or its affiliates and others
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
package org.eclipse.kura.web.client.ui.settings;

import java.util.List;
import java.util.function.Consumer;

import org.eclipse.kura.web.Console;
import org.eclipse.kura.web.client.util.request.RequestQueue;
import org.eclipse.kura.web.shared.service.GwtSecurityTokenService;
import org.eclipse.kura.web.shared.service.GwtSecurityTokenServiceAsync;
import org.gwtbootstrap3.client.ui.Button;
import org.gwtbootstrap3.client.ui.CheckBox;
import org.gwtbootstrap3.client.ui.base.form.AbstractForm.SubmitCompleteEvent;
import org.gwtbootstrap3.client.ui.constants.ButtonType;

import com.google.gwt.core.client.GWT;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.Hidden;

public class SnapshotDownloadModal extends SnapshotGenericModal {

    private static final String XML_DOWNLOAD_FORMAT = "XML";
    private static final String JSON_DOWNLOAD_FORMAT = "JSON";
    private final GwtSecurityTokenServiceAsync gwtXSRFService = GWT.create(GwtSecurityTokenService.class);

    Button downloadXml;
    Button downloadJson;
    Button cancelButton;

    Hidden xsrfTokenField;
    Hidden pidsListField;
    Hidden snapshotDownloadFormatField;
    Hidden snapshotIdField;

    HandlerRegistration cancelHandler;
    HandlerRegistration downloadHandler;
    HandlerRegistration xmlDownloadHandler;
    HandlerRegistration jsonDownloadHandler;

    Consumer<String> wiregraphDownloadConsumer;

    AsyncCallback<SubmitCompleteEvent> downloadCallback;

    @Override
    void show(Long snapshotId, List<String> snapshotConfigs) {

        initSnapshotPidList(snapshotConfigs);
        initSelectedPidCounter();
        initSnapshotSelectAllAnchor();

        initTitleAndDescription();
        initFooter();
        initHiddenFields();
        initEventButtons(snapshotId);

        this.snapshotModal.show();
    }

    /*
     * Init methods
     */

    @Override
    void initTitleAndDescription() {
        this.snapshotModal.setTitle(MSGS.deviceSnapshotDownloadModalTitle());
        this.snapshotModalDescription.setText(MSGS.deviceSnapshotDownloadModalHint());
        this.snapshotModalHint.setText(MSGS.formatDownloadHint());
    }

    @Override
    void initEventButtons(Long snapshotId) {

        cleanClickHandlers();

        this.cancelHandler = this.cancelButton.addClickHandler(this::onCancelClick);

        this.downloadHandler = this.snapshotForm
                .addSubmitCompleteHandler(event -> this.downloadCallback.onSuccess(event));

        this.jsonDownloadHandler = this.downloadJson
                .addClickHandler(e -> onSnapshotDownloadButtonClick(snapshotId, JSON_DOWNLOAD_FORMAT));

        this.xmlDownloadHandler = this.downloadXml
                .addClickHandler(e -> onSnapshotDownloadButtonClick(snapshotId, XML_DOWNLOAD_FORMAT));
    }

    @Override
    void initFooter() {

        this.snapshotFooter.clear();

        this.cancelButton = new Button(MSGS.cancelButton());
        this.cancelButton.addStyleName("fa");
        this.cancelButton.setType(ButtonType.PRIMARY);
        this.snapshotFooter.add(cancelButton);

        this.downloadJson = new Button(MSGS.downloadSnapshotJsonButton());
        this.downloadJson.addStyleName("fa");
        this.downloadJson.setType(ButtonType.PRIMARY);
        this.snapshotFooter.add(downloadJson);

        this.downloadXml = new Button(MSGS.downloadSnapshotXmlButton());
        this.downloadXml.addStyleName("fa");
        this.downloadXml.setType(ButtonType.PRIMARY);
        this.snapshotFooter.add(downloadXml);

    }

    @Override
    void initHiddenFields() {

        clearHiddenFields();

        this.snapshotForm.setEncoding(com.google.gwt.user.client.ui.FormPanel.ENCODING_URLENCODED);
        this.snapshotForm.setMethod(com.google.gwt.user.client.ui.FormPanel.METHOD_POST);
        this.snapshotForm.setAction(Console.ADMIN_ROOT + '/' + GWT.getModuleName() + "/device_snapshots");

        this.xsrfTokenField = new Hidden();
        this.xsrfTokenField.setID("xsrfToken");
        this.xsrfTokenField.setName("xsrfToken");
        this.xsrfTokenField.setValue("");
        this.snapshotForm.add(this.xsrfTokenField);

        this.pidsListField = new Hidden();
        this.pidsListField.setID("pidsList");
        this.pidsListField.setName("pidsList");
        this.pidsListField.setValue("");
        this.snapshotForm.add(this.pidsListField);

        this.snapshotDownloadFormatField = new Hidden();
        this.snapshotDownloadFormatField.setID("downloadFormat");
        this.snapshotDownloadFormatField.setName("downloadFormat");
        this.snapshotDownloadFormatField.setValue("");
        this.snapshotForm.add(this.snapshotDownloadFormatField);

        this.snapshotIdField = new Hidden();
        this.snapshotIdField.setID("snapshotId");
        this.snapshotIdField.setName("snapshotId");
        this.snapshotIdField.setValue("");
        this.snapshotForm.add(this.snapshotIdField);

    }

    /*
     * OnEvent methods
     */

    private void onCancelClick(ClickEvent handler) {
        this.snapshotModal.hide();
        resetScrollPanel();
    }

    private void onSnapshotDownloadButtonClick(Long snapshotId, String format) {

        List<CheckBox> selectedPids = getSelectedPidsCheckboxes();

        if (selectedPids.isEmpty()) {

            this.noPidSelectedError.setVisible(true);

        } else {

            if (selectedPids.size() == this.pidPanel.getWidgetCount()) {
                downloadEntireSnapshot(snapshotId, format);
            } else {
                downloadPartialSnapshot(snapshotId, format, getSelectedPidsField(selectedPids));
            }

            this.snapshotModal.hide();
            resetScrollPanel();
        }
    }

    /*
     * Utils methods
     */

    private void downloadEntireSnapshot(Long snapshotId, String format) {
        RequestQueue.submit(context -> this.gwtXSRFService.generateSecurityToken(context.callback(token -> {
            xsrfTokenField.setValue(token.getToken());
            pidsListField.setValue("");
            snapshotDownloadFormatField.setValue(format);
            snapshotIdField.setValue(snapshotId.toString());
            snapshotForm.submit();
        })));
    }

    private void downloadPartialSnapshot(Long snapshotId, String format, String selectedPids) {
        RequestQueue.submit(context -> this.gwtXSRFService.generateSecurityToken(context.callback(token -> {
            xsrfTokenField.setValue(token.getToken());
            pidsListField.setValue(selectedPids);
            snapshotDownloadFormatField.setValue(format);
            snapshotIdField.setValue(snapshotId.toString());
            snapshotForm.submit();
        })));
    }

    private void cleanClickHandlers() {
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
            this.snapshotForm.remove(this.xsrfTokenField);
        }

        if (this.pidsListField != null) {
            this.snapshotForm.remove(this.pidsListField);
        }

        if (this.snapshotDownloadFormatField != null) {
            this.snapshotForm.remove(this.snapshotDownloadFormatField);
        }

        if (this.snapshotIdField != null) {
            this.snapshotForm.remove(this.snapshotIdField);
        }
    }
}
