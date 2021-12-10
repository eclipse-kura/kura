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
package org.eclipse.kura.web.client.ui.settings;

import org.gwtbootstrap3.client.ui.Button;
import org.gwtbootstrap3.client.ui.ListBox;
import org.gwtbootstrap3.client.ui.Modal;

import com.google.gwt.core.client.GWT;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.Widget;

public class SnapshotDownloadModal extends Composite {

    private static SnapshotDownloadModalUiBinder uiBinder = GWT.create(SnapshotDownloadModalUiBinder.class);

    interface SnapshotDownloadModalUiBinder extends UiBinder<Widget, SnapshotDownloadModal> {
    }

    @UiField
    Modal modal;
    @UiField
    ListBox formatList;
    @UiField
    Button download;

    private Listener listener = format -> {
    };

    public SnapshotDownloadModal() {
        initWidget(uiBinder.createAndBindUi(this));

        this.download.addClickHandler(e -> {
            this.modal.hide();
            this.listener.onDonwload(this.formatList.getSelectedValue());
        });
    }

    public void show(final Listener listener) {
        this.listener = listener;
        this.modal.show();
    }

    public interface Listener {

        public void onDonwload(String format);
    }

}
