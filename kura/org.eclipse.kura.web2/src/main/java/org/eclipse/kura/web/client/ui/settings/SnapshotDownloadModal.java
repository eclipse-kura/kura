/*******************************************************************************
 * Copyright (c) 2024 Eurotech and/or its affiliates and others
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
import org.gwtbootstrap3.client.ui.html.Paragraph;

import com.google.gwt.core.client.GWT;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.ScrollPanel;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.client.ui.Widget;

public class SnapshotDownloadModal extends Composite {

    private static SnapshotDownloadModalUiBinder uiBinder = GWT.create(SnapshotDownloadModalUiBinder.class);
    private static final Messages MSGS = GWT.create(Messages.class);

    private static final String SELECT_ALL_PIDS_SELECTION = "Select All Pids";
    private static final String REMOVE_ALL_PIDS_SELECTION = "Remove All Pids";

    interface SnapshotDownloadModalUiBinder extends UiBinder<Widget, SnapshotDownloadModal> {
    }

    @UiField
    Paragraph downloadModalDescription;
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

    boolean areAllPidsSelected = true;
    VerticalPanel pidPanel = new VerticalPanel();

    HandlerRegistration anchorClickHandler;

    Consumer<SnapshotDownloadOptions> snapshotDownloadConsumer;

    public SnapshotDownloadModal() {

        initWidget(uiBinder.createAndBindUi(this));

        this.pidSelectionScrollPanel.setVisible(false);
        this.selectOrRemoveAllAnchor.setVisible(false);
        this.noPidSelectedError.setVisible(false);
        this.noPidSelectedError.setText("Please select at least one pid from the list");

        this.cancelButton.addClickHandler(this::onCancelClick);

    }

    public void show(Consumer<SnapshotDownloadOptions> consumer) {
        this.snapshotDownloadConsumer = consumer;
        this.modal.setTitle(MSGS.deviceWiregraphDownloadModalTitle());
        initWiregraphDownloadButtons();
        this.modal.show();
    }

    public void show(Consumer<SnapshotDownloadOptions> consumer, List<String> availablePids) {
        this.snapshotDownloadConsumer = consumer;
        this.noPidSelectedError.setVisible(false);
        this.modal.setTitle(MSGS.deviceSnapshotDownloadModalTitle());
        this.downloadModalDescription.setText(MSGS.deviceSnapshotDownloadModalHint());
        initSnapshotPidList(availablePids);
        initSnapshotSelectAllAnchor();
        initSnapshotScrollPanel();
        initSnapshotDownloadButtons();
        this.modal.show();
    }

    /*
     * Snapshot Download Inits
     */

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
        this.areAllPidsSelected = true;
        this.selectOrRemoveAllAnchor.setText(REMOVE_ALL_PIDS_SELECTION);
        this.anchorClickHandler = this.selectOrRemoveAllAnchor.addClickHandler(this::selectOrRemoveAllSelection);
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
     * Utils
     */

    private void onCheckboxClick(ClickEvent handler) {
        if (noPidSelectedError.isVisible()) {
            noPidSelectedError.setVisible(false);
        }

        checkAllPidsSelected();
        updateSelectOrRemoveAllText();

    }

    private void onCancelClick(ClickEvent handler) {
        this.modal.hide();
        resetScrollPanel();
        this.noPidSelectedError.setVisible(false);
    }

    private Optional<List<String>> getSelectedPids() {
        List<String> selectedPids = new ArrayList<>();
        this.pidPanel.iterator().forEachRemaining(pid -> {
            CheckBox checkBox = (CheckBox) pid;
            if (checkBox.getValue().booleanValue() && !checkBox.getText().equals(SELECT_ALL_PIDS_SELECTION)
                    && !checkBox.getText().equals(REMOVE_ALL_PIDS_SELECTION)) {
                selectedPids.add(checkBox.getText());
            }
        });

        return selectedPids.isEmpty() ? Optional.empty() : Optional.of(selectedPids);
    }

    private void selectOrRemoveAllSelection(ClickEvent handler) {
        pidPanel.iterator().forEachRemaining(widget -> ((CheckBox) widget).setValue(!this.areAllPidsSelected));
        this.areAllPidsSelected = !this.areAllPidsSelected;
        updateSelectOrRemoveAllText();
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

    private void checkAllPidsSelected() {
        boolean areAllSelected = true;
        Iterator<Widget> pidPanelIterator = this.pidPanel.iterator();
        while (pidPanelIterator.hasNext()) {
            if (!((CheckBox) pidPanelIterator.next()).getValue().booleanValue()) {
                areAllSelected = false;
                break;
            }
        }
        this.areAllPidsSelected = areAllSelected;
    }

    private void updateSelectOrRemoveAllText() {
        if (this.areAllPidsSelected) {
            this.selectOrRemoveAllAnchor.setText(REMOVE_ALL_PIDS_SELECTION);
        } else {
            this.selectOrRemoveAllAnchor.setText(SELECT_ALL_PIDS_SELECTION);
        }
    }

    private void resetScrollPanel() {
        this.pidSelectionScrollPanel.setVerticalScrollPosition(0);
        this.pidSelectionScrollPanel.setHorizontalScrollPosition(0);
        this.noPidSelectedError.setVisible(false);
    }

}
