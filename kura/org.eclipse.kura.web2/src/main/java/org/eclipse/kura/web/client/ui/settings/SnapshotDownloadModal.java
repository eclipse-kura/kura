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

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import org.eclipse.kura.web.Console;
import org.eclipse.kura.web.client.messages.Messages;
import org.eclipse.kura.web.client.util.request.RequestQueue;
import org.eclipse.kura.web.shared.service.GwtSecurityTokenService;
import org.eclipse.kura.web.shared.service.GwtSecurityTokenServiceAsync;
import org.gwtbootstrap3.client.ui.Anchor;
import org.gwtbootstrap3.client.ui.Button;
import org.gwtbootstrap3.client.ui.CheckBox;
import org.gwtbootstrap3.client.ui.Form;
import org.gwtbootstrap3.client.ui.FormLabel;
import org.gwtbootstrap3.client.ui.Modal;
import org.gwtbootstrap3.client.ui.TextBox;
import org.gwtbootstrap3.client.ui.base.form.AbstractForm.SubmitCompleteEvent;
import org.gwtbootstrap3.client.ui.html.Paragraph;

import com.google.gwt.core.client.GWT;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.KeyUpEvent;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.Hidden;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.ScrollPanel;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.client.ui.Widget;

public class SnapshotDownloadModal extends Composite {

    private static SnapshotDownloadModalUiBinder uiBinder = GWT.create(SnapshotDownloadModalUiBinder.class);
    private final GwtSecurityTokenServiceAsync gwtXSRFService = GWT.create(GwtSecurityTokenService.class);
    private static final Messages MSGS = GWT.create(Messages.class);

    interface SnapshotDownloadModalUiBinder extends UiBinder<Widget, SnapshotDownloadModal> {
    }

    @UiField
    Modal downloadModal;
    @UiField
    Form snapshotDownloadForm;
    @UiField
    Paragraph downloadModalDescription;
    @UiField
    Paragraph formatModalHint;
    @UiField
    Label searchBoxSeparatorSmall;
    @UiField
    TextBox pidSearch;
    @UiField
    Label searchBoxSeparatorBig;
    @UiField
    ScrollPanel pidSelectionScrollPanel;
    @UiField
    Anchor selectOrRemoveAllAnchor;
    @UiField
    Button downloadXml;
    @UiField
    Button downloadJson;
    @UiField
    Button cancelButton;
    @UiField
    FormLabel noPidSelectedError;
    @UiField
    Label selectedPidCounter;

    @UiField
    Hidden xsrfTokenField;
    @UiField
    Hidden pidsListField;
    @UiField
    Hidden snapshotDownloadFormatField;
    @UiField
    Hidden snapshotIdField;

    VerticalPanel pidPanel = new VerticalPanel();

    HandlerRegistration anchorClickHandler;
    HandlerRegistration downloadHandler;
    HandlerRegistration xmlDownloadHandler;
    HandlerRegistration jsonDownloadHandler;

    Consumer<String> wiregraphDownloadConsumer;

    AsyncCallback<SubmitCompleteEvent> downloadCallback;

    public SnapshotDownloadModal() {

        initWidget(uiBinder.createAndBindUi(this));

        this.pidSearch.setVisible(false);
        this.pidSelectionScrollPanel.setVisible(false);
        this.selectOrRemoveAllAnchor.setVisible(false);
        this.noPidSelectedError.setVisible(false);
        this.selectedPidCounter.setVisible(false);
        this.searchBoxSeparatorSmall.setVisible(false);
        this.searchBoxSeparatorBig.setVisible(false);

        this.cancelButton.addClickHandler(this::onCancelClick);

    }

    /*
     * Wiregraph Snapshot Download
     */

    public void show(Consumer<String> consumer) {
        this.wiregraphDownloadConsumer = consumer;
        this.downloadModal.setTitle(MSGS.deviceWiregraphDownloadModalTitle());
        this.downloadModalDescription.setText(MSGS.deviceWiregraphDownloadModalHint());
        initWiregraphDownloadButtons();
        this.downloadModal.show();
    }

    private void initWiregraphDownloadButtons() {

        this.downloadJson.addClickHandler(e -> {
            this.downloadModal.hide();
            this.wiregraphDownloadConsumer.accept("JSON");
        });

        this.downloadXml.addClickHandler(e -> {
            this.downloadModal.hide();
            this.wiregraphDownloadConsumer.accept("XML");
        });
    }

    /*
     * Snapshot Download
     */

    public void show(List<String> availablePids, Long snapshotId) {
        this.noPidSelectedError.setVisible(false);
        this.searchBoxSeparatorSmall.setVisible(true);
        this.searchBoxSeparatorBig.setVisible(true);
        this.downloadModal.setTitle(MSGS.deviceSnapshotDownloadModalTitle());
        this.downloadModalDescription.setText(MSGS.deviceSnapshotDownloadModalHint());
        initPidSearch();
        initSnapshotPidList(availablePids);
        initSnapshotSelectAllAnchor();
        initSnapshotScrollPanel();
        initSelectedPidCounter();
        initSnapshotDownloadButtons(snapshotId);
        initHiddenFields();
        this.downloadModal.show();
    }

    private void initPidSearch() {
        this.pidSearch.clear();
        this.pidSearch.setVisible(true);
        this.pidSearch.addKeyUpHandler(this::onSearchBoxEvent);
    }

    private void initSnapshotPidList(List<String> snapshotConfigs) {

        this.pidPanel.clear();

        List<String> orderedPids = snapshotConfigs.stream().sorted().collect(Collectors.toList());
        orderedPids.forEach(pid -> {
            CheckBox box = new CheckBox(pid);
            box.setValue(true);
            box.addClickHandler(this::onCheckboxClick);
            this.pidPanel.add(box);
        });
    }

    private void initSnapshotSelectAllAnchor() {
        if (this.anchorClickHandler != null) {
            this.anchorClickHandler.removeHandler();
        }

        if (this.noPidSelectedError.isVisible()) {
            this.noPidSelectedError.setVisible(false);
        }

        this.selectOrRemoveAllAnchor.setText(MSGS.removeAllAnchorText());
        this.anchorClickHandler = this.selectOrRemoveAllAnchor.addClickHandler(this::onSelectOrRemoveAllSelection);
        this.selectOrRemoveAllAnchor.setVisible(true);
    }

    private void initSnapshotScrollPanel() {
        this.pidSelectionScrollPanel.setAlwaysShowScrollBars(false);
        this.pidSelectionScrollPanel.setHeight("350px");
        this.pidSelectionScrollPanel.clear();
        this.pidSelectionScrollPanel.add(pidPanel);
        this.pidSelectionScrollPanel.setVisible(true);
    }

    private void initSnapshotDownloadButtons(Long snapshotId) {

        cleanDownloadHandlers();

        this.downloadHandler = this.snapshotDownloadForm
                .addSubmitCompleteHandler(event -> this.downloadCallback.onSuccess(event));

        this.jsonDownloadHandler = this.downloadJson
                .addClickHandler(e -> onSnapshotDownloadButtonClick(snapshotId, "JSON"));

        this.xmlDownloadHandler = this.downloadXml
                .addClickHandler(e -> onSnapshotDownloadButtonClick(snapshotId, "XML"));
    }

    private void initHiddenFields() {
        this.snapshotDownloadForm.setEncoding(com.google.gwt.user.client.ui.FormPanel.ENCODING_URLENCODED);
        this.snapshotDownloadForm.setMethod(com.google.gwt.user.client.ui.FormPanel.METHOD_POST);
        this.snapshotDownloadForm.setAction(Console.ADMIN_ROOT + '/' + GWT.getModuleName() + "/device_snapshots");

        this.xsrfTokenField.setID("xsrfToken");
        this.xsrfTokenField.setName("xsrfToken");
        this.xsrfTokenField.setValue("");

        this.pidsListField.setID("pidsList");
        this.pidsListField.setName("pidsList");
        this.pidsListField.setValue("");

        this.snapshotDownloadFormatField.setID("downloadFormat");
        this.snapshotDownloadFormatField.setName("downloadFormat");
        this.snapshotDownloadFormatField.setValue("");

        this.snapshotIdField.setID("snapshotId");
        this.snapshotIdField.setName("snapshotId");
        this.snapshotIdField.setValue("");
    }

    private void initSelectedPidCounter() {
        updateSelectedPidsCounter();
        this.selectedPidCounter.setVisible(true);
    }

    /*
     * onEvent method
     */

    private void onSnapshotDownloadButtonClick(Long snapshotId, String format) {

        List<CheckBox> selectedPids = getSelectedPidsCheckboxes();

        if (selectedPids.isEmpty()) {

            this.noPidSelectedError.setVisible(true);

        } else {

            if (selectedPids.size() == this.pidPanel.getWidgetCount()) {
                downloadEntireSnapshot(snapshotId, format);
            } else {
                downloadPartialSnapshot(snapshotId, format);
            }

            this.downloadModal.hide();
            resetScrollPanel();
        }
    }

    /*
     * OnEvents Methods
     */

    private void onSearchBoxEvent(KeyUpEvent event) {
        TextBox searchBox = (TextBox) event.getSource();
        String searchedPid = searchBox.getValue();

        if (searchedPid == null || searchedPid.isEmpty() || searchedPid.equals("")) {
            this.pidPanel.iterator().forEachRemaining(widget -> widget.setVisible(true));
        } else {
            this.pidPanel.iterator()
                    .forEachRemaining(widget -> widget.setVisible(isMatchingSearch(widget, searchedPid)));
        }

        if (this.noPidSelectedError.isVisible()) {
            this.noPidSelectedError.setVisible(false);
        }

        updateSelectOrRemoveAllText(checkPidsCheckboxStates());
    }

    private void onCheckboxClick(ClickEvent handler) {
        if (noPidSelectedError.isVisible()) {
            noPidSelectedError.setVisible(false);
        }

        updateSelectOrRemoveAllText(checkPidsCheckboxStates());
        updateSelectedPidsCounter();

    }

    private void onCancelClick(ClickEvent handler) {
        this.downloadModal.hide();
        resetScrollPanel();
        this.noPidSelectedError.setVisible(false);
    }

    private void onSelectOrRemoveAllSelection(ClickEvent handler) {
        PartialSnapshotCheckboxStatus state = checkPidsCheckboxStates();
        switch (state) {
        case ALL_VISIBLE_ALL_SELECTED:
        case PARTIAL_VISIBLE_ALL_SELECTED: {
            pidPanel.iterator().forEachRemaining(widget -> {
                if (widget.isVisible()) {
                    ((CheckBox) widget).setValue(false);
                }
            });
            break;
        }

        case ALL_VISIBLE_PARTIAL_SELECTED:
        case PARTIAL_VISIBLE_PARTIAL_SELECTED:
            pidPanel.iterator().forEachRemaining(widget -> {
                if (widget.isVisible()) {
                    ((CheckBox) widget).setValue(true);
                }
            });
            break;
        }

        updateSelectOrRemoveAllText(checkPidsCheckboxStates());
        updateSelectedPidsCounter();

        if (this.noPidSelectedError.isVisible()) {
            this.noPidSelectedError.setVisible(false);
        }
    }
    /*
     * Utils
     */

    private String getSelectedPids() {
        StringBuilder selectedPidsBuilder = new StringBuilder("SelectedPids: ");
        this.pidPanel.iterator().forEachRemaining(pid -> {
            CheckBox checkBox = (CheckBox) pid;
            if (checkBox.getValue().booleanValue()) {
                selectedPidsBuilder.append(checkBox.getText() + ",");
            }
        });

        return selectedPidsBuilder.toString();
    }

    private void downloadEntireSnapshot(Long snapshotId, String format) {
        RequestQueue.submit(context -> this.gwtXSRFService.generateSecurityToken(context.callback(token -> {
            xsrfTokenField.setValue(token.getToken());
            pidsListField.setValue("EntireSnapshot");
            snapshotDownloadFormatField.setValue(format);
            snapshotIdField.setValue(snapshotId.toString());
            snapshotDownloadForm.submit();
        })));
    }

    private void downloadPartialSnapshot(Long snapshotId, String format) {
        RequestQueue.submit(context -> this.gwtXSRFService.generateSecurityToken(context.callback(token -> {
            xsrfTokenField.setValue(token.getToken());
            pidsListField.setValue(getSelectedPids());
            snapshotDownloadFormatField.setValue(format);
            snapshotIdField.setValue(snapshotId.toString());
            snapshotDownloadForm.submit();
        })));
    }

    private List<CheckBox> getSelectedPidsCheckboxes() {
        List<CheckBox> selectedPidCheckboxes = new ArrayList<>();
        this.pidPanel.forEach(widget -> {
            CheckBox box = (CheckBox) widget;
            if (box.getValue().booleanValue()) {
                selectedPidCheckboxes.add(box);
            }
        });

        return selectedPidCheckboxes;
    }

    private PartialSnapshotCheckboxStatus checkPidsCheckboxStates() {
        boolean areAllVisible = true;
        boolean areAllSelected = true;

        for (Widget widget : pidPanel) {
            if (!widget.isVisible()) {
                areAllVisible = false;
                break;
            }
        }

        for (Widget widget : pidPanel) {
            if (widget.isVisible() && !((CheckBox) widget).getValue().booleanValue()) {
                areAllSelected = false;
                break;
            }
        }

        return PartialSnapshotCheckboxStatus.fromVisibleAndSelectedStatus(areAllVisible, areAllSelected);
    }

    private void updateSelectOrRemoveAllText(PartialSnapshotCheckboxStatus state) {

        switch (state) {
        case ALL_VISIBLE_ALL_SELECTED:
            this.selectOrRemoveAllAnchor.setText(MSGS.removeAllAnchorText());
            break;

        case ALL_VISIBLE_PARTIAL_SELECTED:
            this.selectOrRemoveAllAnchor.setText(MSGS.selectAllAnchorText());
            break;

        case PARTIAL_VISIBLE_ALL_SELECTED:
            this.selectOrRemoveAllAnchor.setText(MSGS.removeAllVisibleAnchorText());
            break;

        case PARTIAL_VISIBLE_PARTIAL_SELECTED:
            this.selectOrRemoveAllAnchor.setText(MSGS.selectAllVisibleAnchorText());
            break;
        }
    }

    private void resetScrollPanel() {
        this.pidSelectionScrollPanel.setVerticalScrollPosition(0);
        this.pidSelectionScrollPanel.setHorizontalScrollPosition(0);
        this.noPidSelectedError.setVisible(false);
    }

    private void updateSelectedPidsCounter() {

        int selectedPids = 0;

        Iterator<Widget> pidPanelIterator = this.pidPanel.iterator();
        while (pidPanelIterator.hasNext()) {
            if (((CheckBox) pidPanelIterator.next()).getValue().booleanValue()) {
                selectedPids++;
            }
        }

        StringBuilder counterTextBuilder = new StringBuilder("PIDs Selected ").append(selectedPids).append("/")
                .append(this.pidPanel.getWidgetCount());

        this.selectedPidCounter.setText(counterTextBuilder.toString());
    }

    private boolean isMatchingSearch(Widget widget, String searchedPid) {
        return ((CheckBox) widget).getText().toLowerCase().contains(searchedPid.toLowerCase());
    }

    private void cleanDownloadHandlers() {
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
}
