/*******************************************************************************
 * Copyright (c) 2017 Eurotech and/or its affiliates
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 *******************************************************************************/

package org.eclipse.kura.web.client.ui.Firewall;

import org.eclipse.kura.web.client.messages.Messages;
import org.gwtbootstrap3.client.ui.Button;
import org.gwtbootstrap3.client.ui.Modal;
import org.gwtbootstrap3.client.ui.ModalFooter;
import org.gwtbootstrap3.client.ui.html.Span;

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
    Button yes, no;
    @UiField
    ModalFooter alertFooter;
    @UiField
    Span alertBody;

    private Modal modal;

    public AlertDialog() {
        modal = (Modal) uiBinder.createAndBindUi(this);
        initWidget(modal);

        this.modal.setHideOtherModals(false);

        modal.setTitle(MSGS.confirm());
        yes.setText(MSGS.yesButton());
        no.setText(MSGS.noButton());

        this.yes.addClickHandler(new ClickHandler() {

            @Override
            public void onClick(ClickEvent event) {
                if (listener != null) {
                    listener.onConfirm();
                }
                modal.hide();
            }
        });

        this.no.addClickHandler(new ClickHandler() {

            @Override
            public void onClick(ClickEvent event) {
                modal.hide();
            }
        });
    }

    public void setListener(Listener listener) {
        this.listener = listener;
        this.alertFooter.setVisible(listener != null);
    }

    public void show(String message, Listener listener) {
        this.setAlertText(message);
        this.setListener(listener);
        this.modal.show();
    }

    public void setAlertText(String message) {
        this.alertBody.setText(message);
    }

    @Override
    public void setTitle(String title) {
        this.modal.setTitle(title);
    }

    public interface Listener {

        public void onConfirm();

    }

}
