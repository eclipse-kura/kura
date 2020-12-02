/*******************************************************************************
 * Copyright (c) 2020 Eurotech and/or its affiliates and others
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

import java.util.logging.Level;
import java.util.logging.Logger;

import org.eclipse.kura.web.client.messages.Messages;
import org.eclipse.kura.web.client.ui.Tab;
import org.eclipse.kura.web.client.util.request.RequestQueue;
import org.eclipse.kura.web.shared.model.GwtCertificate;
import org.eclipse.kura.web.shared.service.GwtCertificatesService;
import org.eclipse.kura.web.shared.service.GwtCertificatesServiceAsync;
import org.eclipse.kura.web.shared.service.GwtSecurityTokenService;
import org.eclipse.kura.web.shared.service.GwtSecurityTokenServiceAsync;
import org.gwtbootstrap3.client.ui.Button;
import org.gwtbootstrap3.client.ui.ListBox;
import org.gwtbootstrap3.client.ui.Modal;
import org.gwtbootstrap3.client.ui.ModalBody;
import org.gwtbootstrap3.client.ui.ModalFooter;
import org.gwtbootstrap3.client.ui.html.Span;

import com.google.gwt.core.client.GWT;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.user.cellview.client.CellTable;
import com.google.gwt.user.cellview.client.ColumnSortEvent.ListHandler;
import com.google.gwt.user.cellview.client.TextColumn;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.Widget;
import com.google.gwt.view.client.ListDataProvider;
import com.google.gwt.view.client.SingleSelectionModel;

public class CertificateListTabUi extends Composite implements Tab {

    private static CertificateListTabUiUiBinder uiBinder = GWT.create(CertificateListTabUiUiBinder.class);
    private static final Logger logger = Logger.getLogger(CertificateListTabUi.class.getSimpleName());

    interface CertificateListTabUiUiBinder extends UiBinder<Widget, CertificateListTabUi> {
    }

    private static final Messages MSGS = GWT.create(Messages.class);

    private final GwtSecurityTokenServiceAsync gwtXSRFService = GWT.create(GwtSecurityTokenService.class);
    private final GwtCertificatesServiceAsync gwtCertificatesService = GWT.create(GwtCertificatesService.class);

    @UiField
    Button refresh;
    @UiField
    Button add;
    @UiField
    Button uninstall;

    @UiField
    Modal certAddModal;
    @UiField
    ModalBody certAddModalBody;
    @UiField
    Button nextStepButton;
    @UiField
    Button closeModalButton;

    @UiField
    CellTable<GwtCertificate> certificatesGrid;

    final SingleSelectionModel<GwtCertificate> selectionModel = new SingleSelectionModel<>();

    private final ListDataProvider<GwtCertificate> certificatesDataProvider = new ListDataProvider<>();

    public CertificateListTabUi() {
        logger.log(Level.FINER, "Initiating CertificatesTabUI...");
        initWidget(uiBinder.createAndBindUi(this));
        initTable();

        initInterfaceButtons();
    }

    @Override
    public void setDirty(boolean flag) {
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
        RequestQueue.submit(c -> this.gwtXSRFService.generateSecurityToken(
                c.callback(token -> this.gwtCertificatesService.listCertificates(c.callback(result -> {
                    CertificateListTabUi.this.certificatesDataProvider.getList().clear();
                    for (GwtCertificate pair : result) {
                        if (pair != null) {
                            this.certificatesDataProvider.getList().add(pair);
                        }
                    }
                    this.certificatesGrid.setVisible(!this.certificatesDataProvider.getList().isEmpty());
                    // ColumnSortEvent.fire(this.certificatesGrid, this.certificatesGrid.getColumnSortList());
                })))));
    }

    private void initTable() {

        TextColumn<GwtCertificate> col1 = new TextColumn<GwtCertificate>() {

            @Override
            public String getValue(GwtCertificate object) {
                return String.valueOf(object.getAlias());
            }
        };
        this.certificatesGrid.addColumn(col1, MSGS.certificateAlias());

        TextColumn<GwtCertificate> col2 = new TextColumn<GwtCertificate>() {

            @Override
            public String getValue(GwtCertificate object) {
                return String.valueOf(object.getType().toString());
            }
        };
        col2.setSortable(true);
        this.certificatesGrid.addColumn(col2, MSGS.certificateType());

        ListHandler<GwtCertificate> columnSortHandler = new ListHandler<>(this.certificatesDataProvider.getList());
        columnSortHandler.setComparator(col2, (o1, o2) -> {
            if (o1 == o2) {
                return 0;
            }

            // Compare the name columns.
            if (o1 != null) {
                return o2 != null ? o1.getType().name().compareTo(o2.getType().name()) : 1;
            }
            return -1;
        });

        this.certificatesGrid.getColumnSortList().push(col2);
        this.certificatesGrid.addColumnSortHandler(columnSortHandler);

        this.certificatesDataProvider.addDataDisplay(this.certificatesGrid);
        this.certificatesGrid.setSelectionModel(this.selectionModel);
    }

    private void initInterfaceButtons() {
        this.refresh.setText(MSGS.refresh());
        this.refresh.addClickHandler(event -> refresh());

        this.add.setText(MSGS.addButton());
        this.add.addClickHandler(event -> {
            initCertificateTypeSelection();
            this.certAddModal.show();
        });

        this.uninstall.setText(MSGS.packageDeleteButton());
        this.uninstall.addClickHandler(event -> {
            final GwtCertificate selected = this.selectionModel.getSelectedObject();
            if (selected != null) {
                final Modal modal = new Modal();
                ModalBody modalBody = new ModalBody();
                ModalFooter modalFooter = new ModalFooter();
                modal.setClosable(true);
                modal.setTitle(MSGS.confirm());
                modalBody.add(new Span(MSGS.securityUninstallCertificate(selected.getAlias())));
                modalFooter.add(new Button(MSGS.noButton(), event11 -> modal.hide()));
                modalFooter.add(new Button(MSGS.yesButton(), event12 -> {
                    modal.hide();
                    uninstall(selected);
                }));

                modal.add(modalBody);
                modal.add(modalFooter);
                modal.show();
            }
        });
    }

    private void initCertificateTypeSelection() {
        this.certAddModal.setTitle(MSGS.securityCertificateTypeLabel());
        ListBox certType = new ListBox();
        for (CertType c : CertType.values()) {
            certType.addItem(c.value());
        }

        this.certAddModalBody.clear();
        this.certAddModalBody.add(certType);

        this.closeModalButton.setText(MSGS.closeButton());
        this.nextStepButton.setVisible(true);
        this.nextStepButton.setText(MSGS.submitButton());
        this.nextStepButton.addClickHandler(event -> {
            CertType selectedCertType = CertType.fromValue(certType.getSelectedValue());

            initCertificateAddModal(selectedCertType);
        });

    }

    private void initCertificateAddModal(CertType selectedCertType) {
        this.certAddModal.setTitle("Add Certificate");
        this.certAddModalBody.clear();

        if (selectedCertType == CertType.APPLICATION_CERT) {
            ApplicationCertsTabUi appCertPanel = new ApplicationCertsTabUi();
            this.certAddModalBody.add(appCertPanel);
        } else if (selectedCertType == CertType.DEVICE_SSL_CERT) {
            DeviceCertsTabUi deviceCertsTabUi = new DeviceCertsTabUi();
            this.certAddModalBody.add(deviceCertsTabUi);
        } else if (selectedCertType == CertType.SERVER_SSL_CERT) {
            ServerCertsTabUi serverCertsTabUi = new ServerCertsTabUi();
            this.certAddModalBody.add(serverCertsTabUi);
        } else if (selectedCertType == CertType.HTTPS_CLIENT_CERT) {
            HttpsUserCertsTabUi httpsUserCertsTabUi = new HttpsUserCertsTabUi();
            this.certAddModalBody.add(httpsUserCertsTabUi);
        } else if (selectedCertType == CertType.HTTPS_SERVER_CERT) {
            HttpsServerCertsTabUi httpsServerCertsTabUi = new HttpsServerCertsTabUi();
            this.certAddModalBody.add(httpsServerCertsTabUi);
        }

        this.nextStepButton.setVisible(false);

    }

    private void uninstall(final GwtCertificate selected) {

        RequestQueue.submit(c -> this.gwtXSRFService.generateSecurityToken(c.callback(
                token -> this.gwtCertificatesService.removeCertificate(token, selected, c.callback(ok -> refresh())))));

    }

    private enum CertType {

        SERVER_SSL_CERT(MSGS.settingsAddCertificates()),
        DEVICE_SSL_CERT(MSGS.settingsAddMAuthCertificates()),
        APPLICATION_CERT(MSGS.settingsAddBundleCerts()),
        HTTPS_SERVER_CERT(MSGS.securityHttpsServerLabel()),
        HTTPS_CLIENT_CERT(MSGS.securityHttpsClientLabel());

        private String value;

        private CertType(String v) {
            this.value = v;
        }

        public String value() {
            return this.value;
        }

        public static CertType fromValue(String v) {
            for (CertType c : CertType.values()) {
                if (c.value().equals(v)) {
                    return c;
                }
            }
            throw new IllegalArgumentException(v);
        }
    }

    @Override
    public void clear() {
        // TODO Auto-generated method stub

    }
}
