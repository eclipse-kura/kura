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

import java.util.Arrays;
import java.util.List;

import org.eclipse.kura.web.Console;
import org.gwtbootstrap3.client.ui.CheckBox;
import org.gwtbootstrap3.client.ui.base.form.AbstractForm.SubmitCompleteEvent;
import org.gwtbootstrap3.client.ui.constants.ButtonType;

import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.Hidden;

public class SnapshotDownloadModal extends SnapshotSelectorModal {

    private static final String PIDS_LIST_REQUEST_FIELD = "pidsList";
    private static final String DOWNLOAD_FORMAT_REQUEST_FIELD = "downloadFormat";
    private static final String SNAPSHOT_ID_REQUEST_FIELD = "snapshotId";
    private static final String FONT_AWESOME_STYLE_NAME = "fa";
    private static final String XML_DOWNLOAD_FORMAT = "XML";
    private static final String JSON_DOWNLOAD_FORMAT = "JSON";

    SnapshotSelectorActionButton cancelButton;
    SnapshotSelectorActionButton jsonDownloadButton;
    SnapshotSelectorActionButton xmlDownloadButton;

    Hidden pidsListField;
    Hidden snapshotDownloadFormatField;
    Hidden snapshotIdField;

    AsyncCallback<SubmitCompleteEvent> downloadCallback;

    @Override
    protected void customiseModal() {
        clearClickHandlers();
        removeRequestParameters();

        this.cancelButton = new SnapshotSelectorActionButton(MSGS.cancelButton(), FONT_AWESOME_STYLE_NAME,
                ButtonType.PRIMARY, e -> hideAndReset());

        this.jsonDownloadButton = new SnapshotSelectorActionButton(MSGS.downloadSnapshotJsonButton(),
                FONT_AWESOME_STYLE_NAME, ButtonType.PRIMARY, e -> onSnapshotDownloadButtonClick(JSON_DOWNLOAD_FORMAT));

        this.xmlDownloadButton = new SnapshotSelectorActionButton(MSGS.downloadSnapshotXmlButton(),
                FONT_AWESOME_STYLE_NAME, ButtonType.PRIMARY, e -> onSnapshotDownloadButtonClick(XML_DOWNLOAD_FORMAT));

        addFooterButton(this.cancelButton);
        addFooterButton(this.jsonDownloadButton);
        addFooterButton(this.xmlDownloadButton);

        this.pidsListField = createRequestParameter(PIDS_LIST_REQUEST_FIELD, PIDS_LIST_REQUEST_FIELD, "");
        this.snapshotDownloadFormatField = createRequestParameter(DOWNLOAD_FORMAT_REQUEST_FIELD,
                DOWNLOAD_FORMAT_REQUEST_FIELD, "");
        this.snapshotIdField = createRequestParameter(SNAPSHOT_ID_REQUEST_FIELD, SNAPSHOT_ID_REQUEST_FIELD, "");

        addRequestParameter(this.pidsListField);
        addRequestParameter(this.snapshotDownloadFormatField);
        addRequestParameter(this.snapshotIdField);

        setModalTitleDescriptionAndHints(MSGS.deviceSnapshotDownloadModalTitle(),
                MSGS.deviceSnapshotDownloadModalDescription(), MSGS.deviceSnapshotDownloadFormatHint());

        setFormRequestType(com.google.gwt.user.client.ui.FormPanel.ENCODING_URLENCODED,
                com.google.gwt.user.client.ui.FormPanel.METHOD_POST,
                Console.ADMIN_ROOT + '/' + GWT.getModuleName() + "/device_snapshots");

        addSubmitCompleteHandler(event -> this.downloadCallback.onSuccess(event));
    }

    /*
     * OnEvent methods
     */

    private void onSnapshotDownloadButtonClick(String format) {

        List<CheckBox> selectedPids = getSelectedPidsCheckboxes();

        if (selectedPids.isEmpty()) {

            setErrorVisible(true);

        } else {

            if (selectedPids.size() == this.pidPanel.getWidgetCount()) {
                onDownloadEntireSnapshot(format);
            } else {
                onDownloadPartialSnapshot(format, selectedPidsToRequestParameter(selectedPids));
            }

            hideAndReset();
        }
    }

    private void onDownloadEntireSnapshot(String format) {
        pidsListField.setValue("");
        snapshotDownloadFormatField.setValue(format);
        snapshotIdField.setValue(String.valueOf(getSelectedSnapshot().getSnapshotId()));
        submitRequest(Arrays.asList(pidsListField, snapshotDownloadFormatField, snapshotIdField));
    }

    private void onDownloadPartialSnapshot(String format, String selectedPids) {
        pidsListField.setValue(selectedPids);
        snapshotDownloadFormatField.setValue(format);
        snapshotIdField.setValue(String.valueOf(getSelectedSnapshot().getSnapshotId()));
        submitRequest(Arrays.asList(pidsListField, snapshotDownloadFormatField, snapshotIdField));
    }

    /*
     * Utils methods
     */

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
