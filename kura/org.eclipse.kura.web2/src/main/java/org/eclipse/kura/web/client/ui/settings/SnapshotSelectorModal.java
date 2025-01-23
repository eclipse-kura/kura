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
package org.eclipse.kura.web.client.ui.settings;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.stream.Collectors;

import org.eclipse.kura.web.client.messages.Messages;
import org.gwtbootstrap3.client.ui.Anchor;
import org.gwtbootstrap3.client.ui.CheckBox;
import org.gwtbootstrap3.client.ui.Form;
import org.gwtbootstrap3.client.ui.FormLabel;
import org.gwtbootstrap3.client.ui.Modal;
import org.gwtbootstrap3.client.ui.ModalFooter;
import org.gwtbootstrap3.client.ui.TextBox;
import org.gwtbootstrap3.client.ui.html.Paragraph;

import com.google.gwt.core.client.GWT;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.KeyUpEvent;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.Hidden;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.ScrollPanel;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.client.ui.Widget;

public abstract class SnapshotSelectorModal extends Composite {

    protected static final Messages MSGS = GWT.create(Messages.class);
    private static SnapshotSelectorModalUiBinder uiBinder = GWT.create(SnapshotSelectorModalUiBinder.class);

    interface SnapshotSelectorModalUiBinder extends UiBinder<Widget, SnapshotSelectorModal> {
    }

    @UiField
    Modal snapshotModal;
    @UiField
    Form snapshotForm;
    @UiField
    Paragraph snapshotModalDescription;
    @UiField
    Paragraph snapshotModalHint;
    @UiField
    TextBox pidSearch;
    @UiField
    ScrollPanel pidSelectionScrollPanel;
    @UiField
    Anchor selectOrRemoveAllAnchor;
    @UiField
    FormLabel noPidSelectedError;
    @UiField
    Label selectedPidCounter;
    @UiField
    ModalFooter snapshotFooter;

    HandlerRegistration anchorClickHandler;

    VerticalPanel pidPanel = new VerticalPanel();

    protected SnapshotSelectorModal() {
        initWidget(uiBinder.createAndBindUi(this));
        this.noPidSelectedError.setVisible(false);
    }

    /*
     * Use it to customise the modal using the target snapshot
     */
    protected abstract void customiseModal(Long snapshotId);

    /*
     * Use it to show the modal
     */

    public void showModal(Long snapshotId, List<String> pidList) {
        customiseModal(snapshotId);

        initPidSearch();
        initSnapshotScrollPanel();
        initSnapshotPidList(pidList);
        initSelectedPidCounter();
        initSnapshotSelectAllAnchor();

        this.snapshotModal.show();
    }

    /*
     * Use it to hide and reset the modal
     */

    public void hideAndReset() {
        this.snapshotModal.hide();

        this.pidSelectionScrollPanel.setVerticalScrollPosition(0);
        this.pidSelectionScrollPanel.setHorizontalScrollPosition(0);
        this.noPidSelectedError.setVisible(false);

        this.snapshotFooter.clear();
    }

    /*
     * Customising Helpers
     */

    public void setFormType(String encodingType, String method, String action) {
        this.snapshotForm.setEncoding(encodingType);
        this.snapshotForm.setMethod(method);
        this.snapshotForm.setAction(action);
    }

    public void addRequestParameter(Hidden parameter) {
        this.snapshotForm.add(parameter);
    }

    public void removeRequestParameter(Hidden parameter) {
        this.snapshotForm.remove(parameter);
    }

    public void addFooterButton(SnapshotSelectorActionButton actionButton) {
        this.snapshotFooter.add(actionButton.getButton());
    }

    public void setTitleDescriptionAndHints(String title, String description, String hint) {
        this.snapshotModal.setTitle(title);
        this.snapshotModalDescription.setText(description);
        this.snapshotModalHint.setText(hint);
    }

    /*
     * Generic public initializers
     */

    private void initPidSearch() {
        this.pidSearch.clear();
        this.pidSearch.setVisible(true);
        this.pidSearch.addKeyUpHandler(this::onSearchBoxEvent);
    }

    private void initSnapshotScrollPanel() {
        this.pidSelectionScrollPanel.setAlwaysShowScrollBars(false);
        this.pidSelectionScrollPanel.setHeight("350px");
        this.pidSelectionScrollPanel.clear();
        this.pidSelectionScrollPanel.add(pidPanel);
        this.pidSelectionScrollPanel.setVisible(true);
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

    private void initSelectedPidCounter() {
        updateSelectedPidsCounter();
        this.selectedPidCounter.setVisible(true);
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

    /*
     * Generic OnEvents Methods
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
     * Generic Public Utils Methods
     */

    public List<CheckBox> getSelectedPidsCheckboxes() {
        List<CheckBox> selectedPidCheckboxes = new ArrayList<>();
        this.pidPanel.forEach(widget -> {
            CheckBox box = (CheckBox) widget;
            if (box.getValue().booleanValue()) {
                selectedPidCheckboxes.add(box);
            }
        });

        return selectedPidCheckboxes;
    }

    public String getSelectedPidsField(List<CheckBox> selectedCheckboxes) {
        StringBuilder selectedPidsBuilder = new StringBuilder();

        selectedCheckboxes.forEach(checkBox -> selectedPidsBuilder.append(checkBox.getText() + ","));

        selectedPidsBuilder.replace(selectedPidsBuilder.length() - 1, selectedPidsBuilder.length(), "");
        return selectedPidsBuilder.toString();
    }

    /*
     * Generic Private Utils Methods
     */

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

    private boolean isMatchingSearch(Widget widget, String searchedPid) {
        return ((CheckBox) widget).getText().toLowerCase().contains(searchedPid.toLowerCase());
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
