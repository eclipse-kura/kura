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
import java.util.Optional;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import org.eclipse.kura.web.client.messages.Messages;
import org.eclipse.kura.web.client.ui.wires.SnapshotDownloadOptions;
import org.gwtbootstrap3.client.ui.Anchor;
import org.gwtbootstrap3.client.ui.Button;
import org.gwtbootstrap3.client.ui.CheckBox;
import org.gwtbootstrap3.client.ui.FormLabel;
import org.gwtbootstrap3.client.ui.Modal;
import org.gwtbootstrap3.client.ui.TextBox;
import org.gwtbootstrap3.client.ui.html.Paragraph;

import com.google.gwt.core.client.GWT;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.KeyUpEvent;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.ScrollPanel;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.client.ui.Widget;

public class SnapshotDownloadModal extends Composite {

    private static SnapshotDownloadModalUiBinder uiBinder = GWT.create(SnapshotDownloadModalUiBinder.class);
    private static final Messages MSGS = GWT.create(Messages.class);

    interface SnapshotDownloadModalUiBinder extends UiBinder<Widget, SnapshotDownloadModal> {
    }

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
    Modal modal;
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

    VerticalPanel pidPanel = new VerticalPanel();

    HandlerRegistration anchorClickHandler;

    Consumer<SnapshotDownloadOptions> snapshotDownloadConsumer;

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

    public void show(Consumer<SnapshotDownloadOptions> consumer) {
        this.snapshotDownloadConsumer = consumer;
        this.modal.setTitle(MSGS.deviceWiregraphDownloadModalTitle());
        this.downloadModalDescription.setText(MSGS.deviceWiregraphDownloadModalHint());
        initWiregraphDownloadButtons();
        this.modal.show();
    }

    public void show(Consumer<SnapshotDownloadOptions> consumer, List<String> availablePids) {
        this.snapshotDownloadConsumer = consumer;
        this.noPidSelectedError.setVisible(false);
        this.searchBoxSeparatorSmall.setVisible(true);
        this.searchBoxSeparatorBig.setVisible(true);
        this.modal.setTitle(MSGS.deviceSnapshotDownloadModalTitle());
        this.downloadModalDescription.setText(MSGS.deviceSnapshotDownloadModalHint());
        initPidSearch();
        initSnapshotPidList(availablePids);
        initSnapshotSelectAllAnchor();
        initSnapshotScrollPanel();
        initSnapshotDownloadButtons();
        initSelectedPidCounter();
        this.modal.show();
    }

    /*
     * Snapshot Download Inits
     */

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

    private void initSnapshotDownloadButtons() {
        this.downloadJson.addClickHandler(e -> {
            if (isOnePidSelected()) {
                this.modal.hide();
                resetScrollPanel();
                this.snapshotDownloadConsumer.accept(new SnapshotDownloadOptions("JSON", getSelectedPids()));
            } else {
                this.noPidSelectedError.setVisible(true);
            }

        });

        this.downloadXml.addClickHandler(e -> {
            if (isOnePidSelected()) {
                this.modal.hide();
                resetScrollPanel();
                this.snapshotDownloadConsumer.accept(new SnapshotDownloadOptions("XML", getSelectedPids()));
            } else {
                this.noPidSelectedError.setVisible(true);
            }
        });
    }

    private void initSelectedPidCounter() {
        updateSelectedPidsCounter();
        this.selectedPidCounter.setVisible(true);
    }

    /*
     * Wiregraph Snapshot Download Inits
     */

    private void initWiregraphDownloadButtons() {

        this.downloadJson.addClickHandler(e -> {
            this.modal.hide();
            this.snapshotDownloadConsumer.accept(new SnapshotDownloadOptions("JSON"));
        });

        this.downloadXml.addClickHandler(e -> {
            this.modal.hide();
            this.snapshotDownloadConsumer.accept(new SnapshotDownloadOptions("XML"));
        });
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
            this.pidPanel.iterator().forEachRemaining(widget -> {
                CheckBox box = (CheckBox) widget;
                box.setVisible(box.getText().toLowerCase().contains(searchedPid.toLowerCase()));
            });
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
        this.modal.hide();
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

    private Optional<List<String>> getSelectedPids() {
        List<String> selectedPids = new ArrayList<>();
        this.pidPanel.iterator().forEachRemaining(pid -> {
            CheckBox checkBox = (CheckBox) pid;
            if (checkBox.getValue().booleanValue() && !checkBox.getText().equals(MSGS.selectAllAnchorText())
                    && !checkBox.getText().equals(MSGS.removeAllAnchorText())) {
                selectedPids.add(checkBox.getText());
            }
        });

        return selectedPids.isEmpty() ? Optional.empty() : Optional.of(selectedPids);
    }

    private boolean isOnePidSelected() {
        boolean result = false;
        Iterator<Widget> pidPanelIterator = this.pidPanel.iterator();
        while (pidPanelIterator.hasNext()) {
            CheckBox box = (CheckBox) pidPanelIterator.next();
            if (box.getValue().booleanValue()) {
                result = true;
                break;
            }
        }

        return result;
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
}
