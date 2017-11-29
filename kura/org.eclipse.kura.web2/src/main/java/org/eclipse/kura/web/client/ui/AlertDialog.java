/*******************************************************************************
 * Copyright (c) 2017 Eurotech and/or its affiliates
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 *******************************************************************************/

package org.eclipse.kura.web.client.ui;

import org.eclipse.kura.web.client.messages.Messages;
import org.gwtbootstrap3.client.ui.Alert;
import org.gwtbootstrap3.client.ui.Button;
import org.gwtbootstrap3.client.ui.Modal;
import org.gwtbootstrap3.client.ui.ModalFooter;
import org.gwtbootstrap3.client.ui.html.Span;
import org.gwtbootstrap3.client.ui.html.Strong;

import com.google.gwt.core.client.GWT;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.Widget;

public class AlertDialog extends Composite {

    private static AlertDialogUiBinder uiBinder = GWT.create(AlertDialogUiBinder.class);

    interface AlertDialogUiBinder extends UiBinder<Widget, AlertDialog> {
    }

    private static final Messages MSGS = GWT.create(Messages.class);

    private Listener listener;

    @UiField
    Button yes;
    @UiField
    Button no;
    @UiField
    ModalFooter alertFooter;
    @UiField
    Span messageText;
    @UiField
    Alert alertBody;
    @UiField
    Strong alertText;

    private final Modal modal;

    public AlertDialog() {
        this.modal = (Modal) uiBinder.createAndBindUi(this);
        initWidget(this.modal);

        this.modal.setHideOtherModals(false);

        this.modal.setTitle(MSGS.confirm());
        this.yes.setText(MSGS.yesButton());
        this.no.setText(MSGS.noButton());

        this.yes.addClickHandler(new ClickHandler() {

            @Override
            public void onClick(ClickEvent event) {
                if (AlertDialog.this.listener != null) {
                    AlertDialog.this.listener.onConfirm();
                }
                AlertDialog.this.modal.hide();
            }
        });

        this.no.addClickHandler(new ClickHandler() {

            @Override
            public void onClick(ClickEvent event) {
                AlertDialog.this.modal.hide();
            }
        });
    }

    public void setListener(Listener listener) {
        this.listener = listener;
        this.alertFooter.setVisible(listener != null);
    }

    public void show(String title, String message, Severity severity, Listener listener) {
        setAlertText(message, severity);
        setTitle(title);
        setListener(listener);
        this.modal.show();
    }

    public void show(String message, Listener listener) {
        show(MSGS.confirm(), message, Severity.INFO, listener);
    }

    public void show(String message, Severity severity, Listener listener) {
        show(severity == Severity.INFO ? MSGS.confirm() : MSGS.warning(), message, severity, listener);
    }

    public void show(String title, String message, Listener listener) {
        show(title, message, Severity.INFO, listener);
    }

    public void setAlertText(String message, Severity severity) {
        if (severity == Severity.INFO) {
            this.messageText.setText(message);
            this.messageText.setVisible(true);
            this.alertBody.setVisible(false);
        } else {
            this.alertText.setText(message);
            this.alertBody.setVisible(true);
            this.messageText.setVisible(false);
        }
    }

    @Override
    public void setTitle(String title) {
        this.modal.setTitle(title);
    }

    public interface Listener {

        public void onConfirm();

    }

    public enum Severity {
        INFO,
        ALERT
    }
}
