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

import com.google.gwt.core.client.GWT;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.Widget;

public class ButtonBar extends Composite {

    private static ButtonBarUiBinder uiBinder = GWT.create(ButtonBarUiBinder.class);

    interface ButtonBarUiBinder extends UiBinder<Widget, ButtonBar> {
    }

    private static final Messages MSGS = GWT.create(Messages.class);

    private Listener listener;

    @UiField
    Button apply;
    @UiField
    Button reset;
    @UiField
    Button create;
    @UiField
    Button edit;
    @UiField
    Button delete;

    public ButtonBar() {
        initWidget(uiBinder.createAndBindUi(this));

        this.apply.setText(MSGS.firewallApply());
        this.reset.setText(MSGS.reset());
        this.create.setText(MSGS.newButton());
        this.edit.setText(MSGS.editButton());
        this.delete.setText(MSGS.deleteButton());

        this.apply.addClickHandler(new ClickHandler() {

            @Override
            public void onClick(ClickEvent event) {
                if (ButtonBar.this.listener != null) {
                    ButtonBar.this.listener.onApply();
                }
            }
        });

        this.reset.addClickHandler(new ClickHandler() {

            @Override
            public void onClick(ClickEvent event) {
                if (ButtonBar.this.listener != null) {
                    ButtonBar.this.listener.onCancel();
                }
            }
        });

        this.create.addClickHandler(new ClickHandler() {

            @Override
            public void onClick(ClickEvent event) {
                if (ButtonBar.this.listener != null) {
                    ButtonBar.this.listener.onCreate();
                }
            }
        });

        this.edit.addClickHandler(new ClickHandler() {

            @Override
            public void onClick(ClickEvent event) {
                if (ButtonBar.this.listener != null) {
                    ButtonBar.this.listener.onEdit();
                }
            }
        });

        this.delete.addClickHandler(new ClickHandler() {

            @Override
            public void onClick(ClickEvent event) {
                if (ButtonBar.this.listener != null) {
                    ButtonBar.this.listener.onDelete();
                }
            }
        });
    }

    public void setListener(Listener listener) {
        this.listener = listener;
    }

    public void setDirty(boolean dirty) {
        this.apply.setEnabled(dirty);
        this.reset.setEnabled(dirty);
    }

    public interface Listener {

        public void onApply();

        public void onCancel();

        public void onCreate();

        public void onEdit();

        public void onDelete();
    }

}
