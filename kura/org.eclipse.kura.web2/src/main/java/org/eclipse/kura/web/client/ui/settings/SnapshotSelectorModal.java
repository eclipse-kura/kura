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
import org.eclipse.kura.web.client.ui.AlertDialog;
import org.eclipse.kura.web.client.ui.AlertDialog.DismissListener;
import org.eclipse.kura.web.client.ui.AlertDialog.Severity;
import org.eclipse.kura.web.client.util.request.RequestQueue;
import org.eclipse.kura.web.shared.model.GwtSnapshot;
import org.eclipse.kura.web.shared.service.GwtSecurityTokenService;
import org.eclipse.kura.web.shared.service.GwtSecurityTokenServiceAsync;
import org.gwtbootstrap3.client.ui.Anchor;
import org.gwtbootstrap3.client.ui.CheckBox;
import org.gwtbootstrap3.client.ui.Form;
import org.gwtbootstrap3.client.ui.FormLabel;
import org.gwtbootstrap3.client.ui.Modal;
import org.gwtbootstrap3.client.ui.ModalFooter;
import org.gwtbootstrap3.client.ui.TextBox;
import org.gwtbootstrap3.client.ui.base.form.AbstractForm.SubmitCompleteHandler;
import org.gwtbootstrap3.client.ui.html.Paragraph;

import com.google.gwt.core.client.GWT;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.dom.client.KeyUpEvent;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.Hidden;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.ScrollPanel;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.client.ui.Widget;

public abstract class SnapshotSelectorModal extends Composite {

    private static final String XSRF_TOKEN_REQUEST_FIELD = "xsrfToken";

    protected static final Messages MSGS = GWT.create(Messages.class);
    private static SnapshotSelectorModalUiBinder uiBinder = GWT.create(SnapshotSelectorModalUiBinder.class);

    private final GwtSecurityTokenServiceAsync gwtXSRFService = GWT.create(GwtSecurityTokenService.class);

    interface SnapshotSelectorModalUiBinder extends UiBinder<Widget, SnapshotSelectorModal> {
    }

    @UiField
    Modal snapshotModal;
    @UiField
    Form snapshotForm;
    @UiField
    HorizontalPanel advancedModePanel;
    @UiField
    CheckBox advancedModeCheckbox;
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
    Paragraph advancedModeDescription;
    @UiField
    Label advancedModeDescriptionSeparator;
    @UiField
    ModalFooter snapshotFooter;
    @UiField
    AlertDialog advancedConfirmationAlert;

    HandlerRegistration anchorClickHandler;
    HandlerRegistration downloadHandler;
    HandlerRegistration advancedModeClickHandler;

    GwtSnapshot selectedSnapshot;

    VerticalPanel pidPanel = new VerticalPanel();

    Hidden requestXsrfToken = createRequestParameter(XSRF_TOKEN_REQUEST_FIELD, XSRF_TOKEN_REQUEST_FIELD, "");

    protected SnapshotSelectorModal() {
        initWidget(uiBinder.createAndBindUi(this));
        this.noPidSelectedError.setVisible(false);
        this.advancedModePanel.setVisible(false);
        this.advancedModeDescription.setVisible(false);
        this.advancedModeDescriptionSeparator.setVisible(false);
        this.advancedConfirmationAlert.setVisible(false);

        this.snapshotForm.add(this.requestXsrfToken);
    }

    /*
     * Use it to customise the modal using the target snapshot parameters.
     */
    protected abstract void customiseModal();

    /*
     * Use it to show the modal with the proper customisation.
     */

    public void showModal(GwtSnapshot snapshot, List<String> pidList) {
        this.selectedSnapshot = snapshot;

        customiseModal();

        initPidSearch();
        initSnapshotScrollPanel();
        initSnapshotPidList(pidList);
        initSelectedPidCounter();
        initSnapshotSelectAllAnchor();

        this.snapshotModal.show();
    }

    /*
     * Use it to hide and reset the modal.
     */

    public void hideAndReset() {
        this.snapshotModal.hide();

        this.selectedSnapshot = null;

        this.advancedModePanel.setVisible(false);
        this.advancedModeDescription.setVisible(false);

        this.pidSelectionScrollPanel.setVerticalScrollPosition(0);
        this.pidSelectionScrollPanel.setHorizontalScrollPosition(0);
        this.noPidSelectedError.setVisible(false);

        this.advancedModeCheckbox.setValue(false);
        this.advancedModeDescription.setText("");
        this.advancedModeDescription.setVisible(false);
        this.advancedModeDescriptionSeparator.setVisible(false);

        this.selectOrRemoveAllAnchor.setEnabled(true);

        this.requestXsrfToken.setValue("");

        this.advancedConfirmationAlert.setYesButtonAlternativeText(MSGS.yesButton());
        this.advancedConfirmationAlert.setNoButtonAlternativeText(MSGS.noButton());

        this.snapshotFooter.clear();
    }

    /*
     * Use it to show again the modal without resetting it. It can be used when the user does not confirm the advanced
     * mode confirmation alert to recover the modal previously hidden.
     */

    public void recoverAndShowMainModal() {
        this.snapshotModal.show();
    }

    /*
     * Use it to temporary hide the main modal and show the advanced mode confirmation alert. Remember to reset the
     * modal when done.
     */

    public void hideMainAndShowAdvancedModal(String title, String message, String applyButton, String cancelButton,
            DismissListener listener) {
        this.snapshotModal.hide();
        this.advancedConfirmationAlert.show(title, message, applyButton, cancelButton, Severity.ALERT, listener);
    }

    /*
     * Customising Helpers
     */

    public GwtSnapshot getSelectedSnapshot() {
        return this.selectedSnapshot;
    }

    public void setFormRequestType(String submittingEncodingType, String httpMethod, String submissionUrl) {
        this.snapshotForm.setEncoding(submittingEncodingType);
        this.snapshotForm.setMethod(httpMethod);
        this.snapshotForm.setAction(submissionUrl);
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

    public void setModalTitleDescriptionAndHints(String title, String description, String hint) {
        this.snapshotModal.setTitle(title);
        this.snapshotModalDescription.setText(description);
        this.snapshotModalHint.setText(hint);
    }

    public void setAdvancedModePanelVisible(boolean isVisible) {
        this.advancedModePanel.setVisible(isVisible);
    }

    public void setAdvancedModeDescriptionSpawnText(String text) {
        this.advancedModeDescription.setText(text);
    }

    public void setAdvancedModeDescriptionVisibility(boolean isVisible) {
        this.advancedModeDescription.setVisible(isVisible);
        this.advancedModeDescriptionSeparator.setVisible(isVisible);
        updateSelectedPidsCounter();
    }

    public void setAdvancedModeClickHandler(ClickHandler clickHandler) {
        this.advancedModeClickHandler = this.advancedModeCheckbox.addClickHandler(clickHandler);
    }

    public boolean getAdvancedModeValue() {
        return this.advancedModeCheckbox.getValue().booleanValue();
    }

    public void setAnchorEnable(boolean isEnabled) {
        this.selectOrRemoveAllAnchor.setEnabled(isEnabled);
    }

    public Hidden createRequestParameter(String id, String name, String defaultValue) {
        Hidden parameter = new Hidden();
        parameter.setID(id);
        parameter.setName(name);
        parameter.setValue(defaultValue);
        return parameter;
    }

    public void setErrorVisible(boolean isVisible) {
        this.noPidSelectedError.setVisible(isVisible);
    }

    public void submitRequest(List<Hidden> requestParameters) {
        RequestQueue.submit(context -> this.gwtXSRFService.generateSecurityToken(context.callback(token -> {
            this.requestXsrfToken.setValue(token.getToken());
            requestParameters.forEach(parameter -> this.snapshotForm.add(parameter));
            this.snapshotForm.submit();
        })));
    }

    public void addSubmitCompleteHandler(SubmitCompleteHandler completeHandler) {
        this.downloadHandler = this.snapshotForm.addSubmitCompleteHandler(completeHandler);
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

    public List<String> getSelectedPidsList() {
        List<String> selectedPidsList = new ArrayList<>();
        this.pidPanel.forEach(widget -> {
            CheckBox box = (CheckBox) widget;
            if (box.getValue().booleanValue()) {
                selectedPidsList.add(box.getText());
            }
        });

        return selectedPidsList;
    }

    public String selectedPidsToRequestParameter(List<CheckBox> selectedCheckboxes) {
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
