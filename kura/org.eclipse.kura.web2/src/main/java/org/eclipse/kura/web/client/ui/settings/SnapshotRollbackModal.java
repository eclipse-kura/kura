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

import java.util.List;

import org.eclipse.kura.web.client.ui.EntryClassUi;
import org.eclipse.kura.web.client.util.FailureHandler;
import org.eclipse.kura.web.shared.model.GwtSnapshot;
import org.eclipse.kura.web.shared.model.GwtXSRFToken;
import org.eclipse.kura.web.shared.service.GwtSecurityTokenService;
import org.eclipse.kura.web.shared.service.GwtSecurityTokenServiceAsync;
import org.eclipse.kura.web.shared.service.GwtSnapshotService;
import org.eclipse.kura.web.shared.service.GwtSnapshotServiceAsync;
import org.gwtbootstrap3.client.ui.CheckBox;
import org.gwtbootstrap3.client.ui.constants.ButtonType;

import com.google.gwt.core.client.GWT;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.rpc.AsyncCallback;

public class SnapshotRollbackModal extends SnapshotSelectorModal {

    private static final String FONT_AWESOME_STYLE_NAME = "fa";

    private final GwtSecurityTokenServiceAsync gwtXSRFService = GWT.create(GwtSecurityTokenService.class);
    private final GwtSnapshotServiceAsync gwtSnapshotService = GWT.create(GwtSnapshotService.class);

    SnapshotSelectorActionButton cancelButton;
    SnapshotSelectorActionButton rollbackOrNextButton;

    @Override
    protected void customiseModal() {

        clearClickHandlers();

        this.cancelButton = new SnapshotSelectorActionButton(MSGS.cancelButton(), FONT_AWESOME_STYLE_NAME,
                ButtonType.PRIMARY, e -> hideAndReset());

        this.rollbackOrNextButton = new SnapshotSelectorActionButton(MSGS.rollback(), FONT_AWESOME_STYLE_NAME,
                ButtonType.PRIMARY,
                e -> onRollbackOrNextClick(getSelectedSnapshot(), getSelectedPidsList(), getAdvancedModeValue()));

        addFooterButton(this.cancelButton);
        addFooterButton(this.rollbackOrNextButton);

        setModalTitleDescriptionAndHints(MSGS.deviceSnapshotRollbackTitle(), MSGS.deviceSnapshotRollbackConfirm(),
                MSGS.deviceSnapshotRollbackHint());

        setAdvancedModePanelVisible(true);

        setAdvancedModeClickHandler(this::onAdvancedModeClick);
    }

    /*
     * OnEvent Methods
     */

    private void onAdvancedModeClick(ClickEvent clickHandler) {

        boolean currentAdvancedModeState = getAdvancedModeValue();

        this.pidPanel.forEach(widget -> {
            CheckBox box = (CheckBox) widget;
            box.setValue(true);
            box.setEnabled(!currentAdvancedModeState);
        });

        setAnchorEnable(!currentAdvancedModeState);

        if (currentAdvancedModeState) {
            this.rollbackOrNextButton.setButtonText(MSGS.next());
        } else {
            this.rollbackOrNextButton.setButtonText(MSGS.rollback());
        }
    }

    private void onRollbackOrNextClick(GwtSnapshot snapshot, List<String> selectedPids, boolean isAdvancedRollback) {
        if (isAdvancedRollback) {
            onSnapshotRollback(snapshot);
        } else {
            onConfigurationRollback(snapshot, selectedPids);
        }
    }

    private void onConfigurationRollback(GwtSnapshot snapshot, List<String> selectedPids) {
        EntryClassUi.showWaitModal();
        this.gwtXSRFService.generateSecurityToken(new AsyncCallback<GwtXSRFToken>() {

            @Override
            public void onFailure(Throwable ex) {
                EntryClassUi.hideWaitModal();
                FailureHandler.handle(ex);

            }

            @Override
            public void onSuccess(GwtXSRFToken token) {

                gwtSnapshotService.configurationRollbackDeviceSnapshot(token, snapshot, selectedPids,
                        new AsyncCallback<Void>() {

                            @Override
                            public void onFailure(Throwable ex) {
                                EntryClassUi.hideWaitModal();
                                FailureHandler.handle(ex);
                            }

                            @Override
                            public void onSuccess(Void result) {
                                Window.Location.reload();
                            }
                        });
            }
        });

        hideAndReset();

    }

    private void onSnapshotRollback(GwtSnapshot snapshot) {
        hideMainAndShowAdvancedModal(MSGS.deviceSnapshotRollbackAdvancedModalTitle(),
                MSGS.deviceSnapshotRollbackAdvancedModalMessage(), MSGS.apply(), MSGS.cancelButton(),
                userConfirmed -> onSnapshotRollbackConfirmationSelection(snapshot, userConfirmed));
    }

    public void onSnapshotRollbackConfirmationSelection(GwtSnapshot snapshot, boolean userConfirmed) {
        if (userConfirmed) {
            EntryClassUi.showWaitModal();
            this.gwtXSRFService.generateSecurityToken(new AsyncCallback<GwtXSRFToken>() {

                @Override
                public void onFailure(Throwable ex) {
                    EntryClassUi.hideWaitModal();
                    FailureHandler.handle(ex);

                }

                @Override
                public void onSuccess(GwtXSRFToken token) {

                    gwtSnapshotService.rollbackDeviceSnapshot(token, snapshot, new AsyncCallback<Void>() {

                        @Override
                        public void onFailure(Throwable ex) {
                            EntryClassUi.hideWaitModal();
                            FailureHandler.handle(ex);
                        }

                        @Override
                        public void onSuccess(Void result) {
                            Window.Location.reload();
                        }
                    });
                }
            });

            hideAndReset();

        } else {
            recoverAndShowMainModal();
        }
    }

    /*
     * Utils methods
     */

    private void clearClickHandlers() {

        if (this.cancelButton != null) {
            this.cancelButton.removeClickHandler();
        }

        if (this.rollbackOrNextButton != null) {
            this.rollbackOrNextButton.removeClickHandler();
        }

        if (this.advancedModeClickHandler != null) {
            this.advancedModeClickHandler.removeHandler();
        }

    }
}
