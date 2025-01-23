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

import org.eclipse.kura.web.Console;
import org.eclipse.kura.web.client.util.request.RequestQueue;
import org.eclipse.kura.web.shared.service.GwtSecurityTokenService;
import org.eclipse.kura.web.shared.service.GwtSecurityTokenServiceAsync;
import org.gwtbootstrap3.client.ui.CheckBox;
import org.gwtbootstrap3.client.ui.base.form.AbstractForm.SubmitCompleteEvent;
import org.gwtbootstrap3.client.ui.constants.ButtonType;

import com.google.gwt.core.client.GWT;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.Hidden;

public class SnapshotDownloadModal extends SnapshotSelectorModal {

    private static final String XSRF_TOKEN_REQUEST_FIELD = "xsrfToken";
    private static final String PIDS_LIST_REQUEST_FIELD = "pidsList";
    private static final String DOWNLOAD_FORMAT_REQUEST_FIELD = "downloadFormat";
    private static final String SNAPSHOT_ID_REQUEST_FIELD = "snapshotId";
    private static final String FONT_AWESOME_STYLE_NAME = "fa";
    private static final String XML_DOWNLOAD_FORMAT = "XML";
    private static final String JSON_DOWNLOAD_FORMAT = "JSON";
    private final GwtSecurityTokenServiceAsync gwtXSRFService = GWT.create(GwtSecurityTokenService.class);

    SnapshotSelectorActionButton cancelButton;
    SnapshotSelectorActionButton jsonDownloadButton;
    SnapshotSelectorActionButton xmlDownloadButton;

    Hidden xsrfTokenField;
    Hidden pidsListField;
    Hidden snapshotDownloadFormatField;
    Hidden snapshotIdField;

    AsyncCallback<SubmitCompleteEvent> downloadCallback;
    HandlerRegistration downloadHandler;

    public void customizeModal(Long snapshotId) {

        clearClickHandlers();
        clearHiddenFields();

        this.cancelButton = new SnapshotSelectorActionButton(MSGS.cancelButton(), FONT_AWESOME_STYLE_NAME,
                ButtonType.PRIMARY, this::onCancelClick);

        this.jsonDownloadButton = new SnapshotSelectorActionButton(MSGS.downloadSnapshotJsonButton(),
                FONT_AWESOME_STYLE_NAME, ButtonType.PRIMARY,
                e -> onSnapshotDownloadButtonClick(snapshotId, JSON_DOWNLOAD_FORMAT));

        this.xmlDownloadButton = new SnapshotSelectorActionButton(MSGS.downloadSnapshotXmlButton(),
                FONT_AWESOME_STYLE_NAME, ButtonType.PRIMARY,
                e -> onSnapshotDownloadButtonClick(snapshotId, XML_DOWNLOAD_FORMAT));

        setFormType(com.google.gwt.user.client.ui.FormPanel.ENCODING_URLENCODED,
                com.google.gwt.user.client.ui.FormPanel.METHOD_POST,
                Console.ADMIN_ROOT + '/' + GWT.getModuleName() + "/device_snapshots");

        this.xsrfTokenField = new Hidden();
        this.xsrfTokenField.setID(XSRF_TOKEN_REQUEST_FIELD);
        this.xsrfTokenField.setName(XSRF_TOKEN_REQUEST_FIELD);
        this.xsrfTokenField.setValue("");
        addRequestParameter(this.xsrfTokenField);

        this.pidsListField = new Hidden();
        this.pidsListField.setID(PIDS_LIST_REQUEST_FIELD);
        this.pidsListField.setName(PIDS_LIST_REQUEST_FIELD);
        this.pidsListField.setValue("");
        addRequestParameter(this.pidsListField);

        this.snapshotDownloadFormatField = new Hidden();
        this.snapshotDownloadFormatField.setID(DOWNLOAD_FORMAT_REQUEST_FIELD);
        this.snapshotDownloadFormatField.setName(DOWNLOAD_FORMAT_REQUEST_FIELD);
        this.snapshotDownloadFormatField.setValue("");
        addRequestParameter(this.snapshotDownloadFormatField);

        this.snapshotIdField = new Hidden();
        this.snapshotIdField.setID(SNAPSHOT_ID_REQUEST_FIELD);
        this.snapshotIdField.setName(SNAPSHOT_ID_REQUEST_FIELD);
        this.snapshotIdField.setValue("");
        addRequestParameter(this.snapshotIdField);

        setTitleDescriptionAndHints(MSGS.deviceSnapshotDownloadModalTitle(), MSGS.deviceSnapshotDownloadModalHint(),
                MSGS.formatDownloadHint());

        addFooterButton(this.cancelButton.getButton());
        addFooterButton(this.jsonDownloadButton.getButton());
        addFooterButton(this.xmlDownloadButton.getButton());

        this.downloadHandler = this.snapshotForm
                .addSubmitCompleteHandler(event -> this.downloadCallback.onSuccess(event));
    }

    void show(Long snapshotId, List<String> snapshotConfigs) {
        customizeModal(snapshotId);
        showModal(snapshotConfigs);
    }

    /*
     * OnEvent methods
     */

    private void onCancelClick(ClickEvent handler) {
        clearAndHide();
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

            clearAndHide();
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

    private void clearClickHandlers() {
        if (this.downloadHandler != null) {
            this.downloadHandler.removeHandler();
        }

        if (this.cancelButton != null) {
            this.cancelButton.cleanClickHandler();
        }

        if (this.jsonDownloadButton != null) {
            this.jsonDownloadButton.cleanClickHandler();
        }

        if (this.xmlDownloadButton != null) {
            this.xmlDownloadButton.cleanClickHandler();
        }
    }

    private void clearHiddenFields() {
        if (this.xsrfTokenField != null) {
            removeRequestParameter(this.xsrfTokenField);
        }

        if (this.pidsListField != null) {
            removeRequestParameter(this.pidsListField);
        }

        if (this.snapshotDownloadFormatField != null) {
            removeRequestParameter(this.snapshotDownloadFormatField);
        }

        if (this.snapshotIdField != null) {
            removeRequestParameter(this.snapshotIdField);
        }
    }

}
