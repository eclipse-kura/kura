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
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.ScrollPanel;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.client.ui.Widget;

public abstract class SnapshotGenericModal extends Composite {

    protected static final Messages MSGS = GWT.create(Messages.class);
    private static SnapshotGenericModalUiBinder uiBinder = GWT.create(SnapshotGenericModalUiBinder.class);

    interface SnapshotGenericModalUiBinder extends UiBinder<Widget, SnapshotGenericModal> {
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
    FormLabel noPidSelectedError;
    @UiField
    Label selectedPidCounter;
    @UiField
    ModalFooter snapshotFooter;

    HandlerRegistration anchorClickHandler;

    VerticalPanel pidPanel = new VerticalPanel();

    protected SnapshotGenericModal() {
        initWidget(uiBinder.createAndBindUi(this));
        this.noPidSelectedError.setVisible(false);
    }

    /*
     * Abstract initializers
     */

    /**
     * Initialize the modal title, the description and the hint paragraphs of the modal.
     */
    abstract void initTitleAndDescriptionAndHint();

    /**
     * Initialize eventual buttons to perform the desired operations on the PIDs list.
     * 
     * @param snapshotId
     *            - snapshotId of the snapshot from which the PIDs list was taken as {@link Long}
     */
    abstract void initEventButtons(Long snapshotId);

    /**
     * Initialize the footer of the modal. It can be used to add the buttons to perform the operations.
     */
    abstract void initFooter();

    /**
     * Initialize the eventual hidden field required to perform HTTP requests.
     */
    abstract void initHiddenFields();

    /**
     * Method used to initialize the required elements and eventually customize the modal.
     * 
     * To finally show the modal, the user must call 'this.snapshotModal.show()'.
     * 
     * @param snapshotId
     *            - snapshotId to be downloaded as {@link Long}
     * @param snapshotConfigs
     *            - list of configuration contained in the snapshot to be downloaded as {@link List<String>}
     */
    abstract void show(Long snapshotId, List<String> snapshotConfigs);

    /*
     * Generic public initializers
     */

    public void initSnapshotPidList(List<String> snapshotConfigs) {

        this.pidPanel.clear();

        List<String> orderedPids = snapshotConfigs.stream().sorted().collect(Collectors.toList());
        orderedPids.forEach(pid -> {
            CheckBox box = new CheckBox(pid);
            box.setValue(true);
            box.addClickHandler(this::onCheckboxClick);
            this.pidPanel.add(box);
        });
    }

    public void initSelectedPidCounter() {
        updateSelectedPidsCounter();
        this.selectedPidCounter.setVisible(true);
    }

    public void initSnapshotSelectAllAnchor() {
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

    public void initPidSearch() {
        this.pidSearch.clear();
        this.pidSearch.setVisible(true);
        this.pidSearch.addKeyUpHandler(this::onSearchBoxEvent);
    }

    public void initSnapshotScrollPanel() {
        this.pidSelectionScrollPanel.setAlwaysShowScrollBars(false);
        this.pidSelectionScrollPanel.setHeight("350px");
        this.pidSelectionScrollPanel.clear();
        this.pidSelectionScrollPanel.add(pidPanel);
        this.pidSelectionScrollPanel.setVisible(true);
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

    public void resetScrollPanel() {
        this.pidSelectionScrollPanel.setVerticalScrollPosition(0);
        this.pidSelectionScrollPanel.setHorizontalScrollPosition(0);
        this.noPidSelectedError.setVisible(false);
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
