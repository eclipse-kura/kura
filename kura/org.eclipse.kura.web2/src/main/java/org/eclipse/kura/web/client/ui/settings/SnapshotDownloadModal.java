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

    @Override
    protected void customiseModal(Long snapshotId) {
        clearClickHandlers();
        removeRequestParameters();

        this.cancelButton = new SnapshotSelectorActionButton(MSGS.cancelButton(), FONT_AWESOME_STYLE_NAME,
                ButtonType.PRIMARY, this::onCancelClick);

        this.jsonDownloadButton = new SnapshotSelectorActionButton(MSGS.downloadSnapshotJsonButton(),
                FONT_AWESOME_STYLE_NAME, ButtonType.PRIMARY,
                e -> onSnapshotDownloadButtonClick(snapshotId, JSON_DOWNLOAD_FORMAT));

        this.xmlDownloadButton = new SnapshotSelectorActionButton(MSGS.downloadSnapshotXmlButton(),
                FONT_AWESOME_STYLE_NAME, ButtonType.PRIMARY,
                e -> onSnapshotDownloadButtonClick(snapshotId, XML_DOWNLOAD_FORMAT));

        addFooterButton(this.cancelButton);
        addFooterButton(this.jsonDownloadButton);
        addFooterButton(this.xmlDownloadButton);

        this.xsrfTokenField = createRequestParameter(XSRF_TOKEN_REQUEST_FIELD, XSRF_TOKEN_REQUEST_FIELD, "");
        this.pidsListField = createRequestParameter(PIDS_LIST_REQUEST_FIELD, PIDS_LIST_REQUEST_FIELD, "");
        this.snapshotDownloadFormatField = createRequestParameter(DOWNLOAD_FORMAT_REQUEST_FIELD,
                DOWNLOAD_FORMAT_REQUEST_FIELD, "");
        this.snapshotIdField = createRequestParameter(SNAPSHOT_ID_REQUEST_FIELD, SNAPSHOT_ID_REQUEST_FIELD, "");

        addRequestParameter(this.xsrfTokenField);
        addRequestParameter(this.pidsListField);
        addRequestParameter(this.snapshotDownloadFormatField);
        addRequestParameter(this.snapshotIdField);

        setTitleDescriptionAndHints(MSGS.deviceSnapshotDownloadModalTitle(), MSGS.deviceSnapshotDownloadModalHint(),
                MSGS.formatDownloadHint());

        setFormType(com.google.gwt.user.client.ui.FormPanel.ENCODING_URLENCODED,
                com.google.gwt.user.client.ui.FormPanel.METHOD_POST,
                Console.ADMIN_ROOT + '/' + GWT.getModuleName() + "/device_snapshots");

        this.downloadHandler = this.snapshotForm
                .addSubmitCompleteHandler(event -> this.downloadCallback.onSuccess(event));
    }

    /*
     * OnEvent methods
     */

    private void onCancelClick(ClickEvent handler) {
        hideAndReset();
    }

    private void onSnapshotDownloadButtonClick(Long snapshotId, String format) {

        List<CheckBox> selectedPids = getSelectedPidsCheckboxes();

        if (selectedPids.isEmpty()) {

            this.noPidSelectedError.setVisible(true);

        } else {

            if (selectedPids.size() == this.pidPanel.getWidgetCount()) {
                onDownloadEntireSnapshot(snapshotId, format);
            } else {
                onDownloadPartialSnapshot(snapshotId, format, getSelectedPidsField(selectedPids));
            }

            hideAndReset();
        }
    }

    /*
     * Utils methods
     */

    private void onDownloadEntireSnapshot(Long snapshotId, String format) {
        RequestQueue.submit(context -> this.gwtXSRFService.generateSecurityToken(context.callback(token -> {
            xsrfTokenField.setValue(token.getToken());
            pidsListField.setValue("");
            snapshotDownloadFormatField.setValue(format);
            snapshotIdField.setValue(snapshotId.toString());
            snapshotForm.submit();
        })));
    }

    private void onDownloadPartialSnapshot(Long snapshotId, String format, String selectedPids) {
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
            this.cancelButton.removeClickHandler();
        }

        if (this.jsonDownloadButton != null) {
            this.jsonDownloadButton.removeClickHandler();
        }

        if (this.xmlDownloadButton != null) {
            this.xmlDownloadButton.removeClickHandler();
        }
    }

    private void removeRequestParameters() {
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

    private Hidden createRequestParameter(String id, String name, String defaultValue) {
        Hidden parameter = new Hidden();
        parameter.setID(id);
        parameter.setName(name);
        parameter.setValue("");
        return parameter;
    }

}
