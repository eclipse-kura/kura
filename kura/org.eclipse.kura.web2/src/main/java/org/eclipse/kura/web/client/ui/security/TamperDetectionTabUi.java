/*******************************************************************************
 * Copyright (c) 2021 Eurotech and/or its affiliates and others
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
package org.eclipse.kura.web.client.ui.security;

import java.util.Date;
import java.util.Optional;

import org.eclipse.kura.web.client.messages.Messages;
import org.eclipse.kura.web.client.ui.AlertDialog;
import org.eclipse.kura.web.client.ui.Tab;
import org.eclipse.kura.web.client.util.EventService;
import org.eclipse.kura.web.client.util.request.RequestContext;
import org.eclipse.kura.web.client.util.request.RequestQueue;
import org.eclipse.kura.web.shared.ForwardedEventTopic;
import org.eclipse.kura.web.shared.service.GwtSecurityService;
import org.eclipse.kura.web.shared.service.GwtSecurityServiceAsync;
import org.eclipse.kura.web.shared.service.GwtSecurityTokenService;
import org.eclipse.kura.web.shared.service.GwtSecurityTokenServiceAsync;
import org.gwtbootstrap3.client.ui.Button;
import org.gwtbootstrap3.client.ui.FormControlStatic;
import org.gwtbootstrap3.client.ui.FormGroup;

import com.google.gwt.core.client.GWT;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.Widget;

public class TamperDetectionTabUi extends Composite implements Tab {

    private static TamperDetectionTabUiUiBinder uiBinder = GWT.create(TamperDetectionTabUiUiBinder.class);

    interface TamperDetectionTabUiUiBinder extends UiBinder<Widget, TamperDetectionTabUi> {
    }

    private static final Messages MSGS = GWT.create(Messages.class);

    private final GwtSecurityTokenServiceAsync gwtXSRFService = GWT.create(GwtSecurityTokenService.class);
    private final GwtSecurityServiceAsync gwtSecurityService = GWT.create(GwtSecurityService.class);

    @UiField
    FormControlStatic state;
    @UiField
    FormGroup timestampGroup;
    @UiField
    FormControlStatic timestamp;
    @UiField
    Button resetButton;
    @UiField
    AlertDialog alertDialog;

    public TamperDetectionTabUi() {
        initWidget(uiBinder.createAndBindUi(this));
        clear();
        resetButton.addClickHandler(e -> alertDialog.show(MSGS.securityTamperStateResetConfirm(),
                () -> RequestQueue.submit(c -> gwtXSRFService.generateSecurityToken(
                        c.callback(token -> gwtSecurityService.resetTamperStatus(token, c.callback()))))));
        EventService.subscribe(ForwardedEventTopic.TAMPER_EVENT, e -> this.refresh());
    }

    @Override
    public void setDirty(boolean flag) {
        // no need
    }

    @Override
    public boolean isDirty() {
        return false;
    }

    @Override
    public boolean isValid() {
        return true;
    }

    @Override
    public void refresh() {
        RequestQueue.submit(this::refreshInternal);
    }

    private void refreshInternal(final RequestContext context) {
        clear();
        gwtXSRFService.generateSecurityToken(
                context.callback(token -> gwtSecurityService.getTamperStatus(token, context.callback(status -> {
                    this.state.setText(status.isTampered() ? MSGS.securityTamperStateTampered()
                            : MSGS.securityTamperStateNotTampered());
                    this.resetButton.setEnabled(status.isTampered());
                    final Optional<Long> eventTimestamp = status.getTimestamp();
                    if (eventTimestamp.isPresent()) {
                        this.timestamp.setText(new Date(eventTimestamp.get()).toString());
                        this.timestampGroup.setVisible(true);
                    }
                }))));
    }

    @Override
    public void clear() {
        this.timestampGroup.setVisible(false);
        this.state.setText(MSGS.securityTamperStateUnknown());
        this.resetButton.setEnabled(false);
    }
}
