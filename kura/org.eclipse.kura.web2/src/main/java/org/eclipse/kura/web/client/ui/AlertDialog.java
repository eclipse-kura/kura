/*******************************************************************************
 * Copyright (c) 2017, 2020 Eurotech and/or its affiliates and others
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

package org.eclipse.kura.web.client.ui;

import java.util.Optional;

import org.eclipse.kura.web.client.messages.Messages;
import org.gwtbootstrap3.client.ui.Alert;
import org.gwtbootstrap3.client.ui.Button;
import org.gwtbootstrap3.client.ui.ListItem;
import org.gwtbootstrap3.client.ui.Modal;
import org.gwtbootstrap3.client.ui.ModalFooter;
import org.gwtbootstrap3.client.ui.base.HasId;
import org.gwtbootstrap3.client.ui.html.Paragraph;
import org.gwtbootstrap3.client.ui.html.Strong;
import org.gwtbootstrap3.client.ui.html.UnorderedList;

import com.google.gwt.core.client.GWT;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.Widget;

public class AlertDialog extends Composite implements HasId {

    private static AlertDialogUiBinder uiBinder = GWT.create(AlertDialogUiBinder.class);

    interface AlertDialogUiBinder extends UiBinder<Widget, AlertDialog> {
    }

    private static final Messages MSGS = GWT.create(Messages.class);

    private Optional<DismissListener> listener = Optional.empty();

    @UiField
    Button yes;
    @UiField
    Button no;
    @UiField
    ModalFooter alertFooter;
    @UiField
    Paragraph messageText;
    @UiField
    Alert alertBody;
    @UiField
    Strong alertText;
    @UiField
    UnorderedList extraItems;

    private final Modal modal;

    public AlertDialog() {
        this.modal = (Modal) uiBinder.createAndBindUi(this);
        initWidget(this.modal);

        this.modal.setHideOtherModals(false);

        this.modal.setTitle(MSGS.confirm());
        this.yes.setText(MSGS.yesButton());
        this.no.setText(MSGS.noButton());

        this.yes.addClickHandler(event -> {
            if (AlertDialog.this.listener.isPresent()) {
                AlertDialog.this.listener.get().onDismissed(true);
                AlertDialog.this.listener = Optional.empty();
            }
            AlertDialog.this.modal.hide();
        });

        this.modal.addHideHandler(event -> {
            if (AlertDialog.this.listener.isPresent()) {
                AlertDialog.this.listener.get().onDismissed(false);
                AlertDialog.this.listener = Optional.empty();
            }
        });
    }

    public void setListener(DismissListener listener) {
        this.listener = Optional.ofNullable(listener);
        this.alertFooter.setVisible(listener != null);
        this.modal.setClosable(listener == null);
    }

    public void show(String title, String message, Severity severity, DismissListener listener, String... extraItems) {
        setAlertText(message, severity);
        setTitle(title);
        setListener(listener);
        this.extraItems.clear();
        if (extraItems != null) {
            for (final String extraItem : extraItems) {
                this.extraItems.add(new ListItem(extraItem));
            }
        }
        this.modal.show();
    }

    public void show(String title, String message, Severity severity, ConfirmListener listener) {
        show(title, message, severity, toDismissListener(listener));
    }

    public void show(String message, DismissListener listener) {
        show(MSGS.confirm(), message, Severity.INFO, listener);
    }

    public void show(String message, ConfirmListener listener) {
        show(message, toDismissListener(listener));
    }

    public void show(String message, Severity severity, DismissListener listener) {
        String title = "";
        if (severity == Severity.INFO) {
            title = MSGS.confirm();
        } else if (severity == Severity.ERROR) {
            title = MSGS.error();
        } else {
            title = MSGS.warning();
        }
        show(title, message, severity, listener);
    }

    public void show(String message, Severity severity, ConfirmListener listener) {
        show(message, severity, toDismissListener(listener));
    }

    public void show(String title, String message, DismissListener listener) {
        show(title, message, Severity.INFO, listener);
    }

    public void show(String title, String message, ConfirmListener listener) {
        show(title, message, toDismissListener(listener));
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
    public String getId() {
        return this.modal.getId();
    }

    @Override
    public void setId(final String id) {
        this.modal.setId(id);
    }

    @Override
    public void setTitle(String title) {
        this.modal.setTitle(title);
    }

    public interface ConfirmListener {

        public void onConfirm();

    }

    public interface DismissListener {

        public void onDismissed(boolean confirmed);

    }

    private static DismissListener toDismissListener(final ConfirmListener listener) {
        if (listener == null) {
            return null;
        }

        return ok -> {
            if (ok) {
                listener.onConfirm();
            }
        };
    }

    public enum Severity {
        INFO,
        ALERT,
        ERROR
    }
}
