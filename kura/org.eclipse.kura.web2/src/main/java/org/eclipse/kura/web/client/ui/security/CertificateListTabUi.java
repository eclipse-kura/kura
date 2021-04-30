/*******************************************************************************
 * Copyright (c) 2020, 2021 Eurotech and/or its affiliates and others
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

import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.eclipse.kura.web.client.messages.Messages;
import org.eclipse.kura.web.client.ui.AlertDialog;
import org.eclipse.kura.web.client.ui.AlertDialog.ConfirmListener;
import org.eclipse.kura.web.client.ui.Tab;
import org.eclipse.kura.web.client.util.request.RequestQueue;
import org.eclipse.kura.web.shared.DateUtils;
import org.eclipse.kura.web.shared.model.GwtKeystoreEntry;
import org.eclipse.kura.web.shared.model.GwtKeystoreEntry.Kind;
import org.eclipse.kura.web.shared.service.GwtCertificatesService;
import org.eclipse.kura.web.shared.service.GwtCertificatesServiceAsync;
import org.eclipse.kura.web.shared.service.GwtSecurityTokenService;
import org.eclipse.kura.web.shared.service.GwtSecurityTokenServiceAsync;
import org.gwtbootstrap3.client.ui.Button;
import org.gwtbootstrap3.client.ui.ListBox;
import org.gwtbootstrap3.client.ui.Modal;
import org.gwtbootstrap3.client.ui.ModalBody;
import org.gwtbootstrap3.client.ui.ModalFooter;
import org.gwtbootstrap3.client.ui.PanelFooter;
import org.gwtbootstrap3.client.ui.html.Span;

import com.google.gwt.core.client.GWT;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.user.cellview.client.CellTable;
import com.google.gwt.user.cellview.client.ColumnSortEvent;
import com.google.gwt.user.cellview.client.ColumnSortEvent.ListHandler;
import com.google.gwt.user.cellview.client.SimplePager;
import com.google.gwt.user.cellview.client.SimplePager.TextLocation;
import com.google.gwt.user.cellview.client.TextColumn;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.ScrollPanel;
import com.google.gwt.user.client.ui.Widget;
import com.google.gwt.view.client.HasRows;
import com.google.gwt.view.client.ListDataProvider;
import com.google.gwt.view.client.SingleSelectionModel;

public class CertificateListTabUi extends Composite implements Tab, CertificateModalListener {

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
    ScrollPanel certAddModalBody;
    @UiField
    Button nextStepButton;
    @UiField
    Button closeModalButton;
    @UiField
    AlertDialog alertDialog;
    @UiField
    PanelFooter tablePanelFooter;

    @UiField
    CellTable<GwtKeystoreEntry> certificatesGrid;

    final SingleSelectionModel<GwtKeystoreEntry> selectionModel = new SingleSelectionModel<>();

    private final ListDataProvider<GwtKeystoreEntry> certificatesDataProvider = new ListDataProvider<>();

    private final SimplePager pager;

    private List<String> pids;

    public CertificateListTabUi() {
        logger.log(Level.FINER, "Initiating CertificatesTabUI...");
        initWidget(uiBinder.createAndBindUi(this));

        this.pager = new SimplePager(TextLocation.CENTER, false, 0, true) {

            @Override
            public void nextPage() {
                setPage(getPage() + 1);
            }

            @Override
            public void setPageStart(int index) {
                final HasRows display = getDisplay();
                if (display != null) {
                    display.setVisibleRange(index, getPageSize());
                }
            }
        };
        this.pager.setPageSize(15);
        this.pager.setDisplay(this.certificatesGrid);
        this.tablePanelFooter.add(this.pager);
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
        RequestQueue.submit(c -> this.gwtCertificatesService.listKeystoreServicePids(c.callback(resultPids -> {
            this.pids = resultPids;
            this.gwtCertificatesService.listEntries(c.callback(result -> {

                CertificateListTabUi.this.certificatesDataProvider.getList().clear();
                for (GwtKeystoreEntry pair : result) {
                    if (pair != null) {
                        this.certificatesDataProvider.getList().add(pair);
                    }
                }
                this.certificatesGrid.setVisible(!this.certificatesDataProvider.getList().isEmpty());
                this.selectionModel.clear();
                ColumnSortEvent.fire(this.certificatesGrid, this.certificatesGrid.getColumnSortList());
            }));
        })));
    }

    private void initTable() {

        TextColumn<GwtKeystoreEntry> col1 = new TextColumn<GwtKeystoreEntry>() {

            @Override
            public String getValue(GwtKeystoreEntry object) {
                return String.valueOf(object.getAlias());
            }
        };
        this.certificatesGrid.addColumn(col1, MSGS.certificateAlias());
        col1.setSortable(true);

        TextColumn<GwtKeystoreEntry> col2 = new TextColumn<GwtKeystoreEntry>() {

            @Override
            public String getValue(GwtKeystoreEntry object) {
                final Kind kind = object.getKind();

                if (kind == Kind.KEY_PAIR) {
                    return "Key Pair";
                } else if (kind == Kind.SECRET_KEY) {
                    return "Secret Key";
                } else if (kind == Kind.TRUSTED_CERT) {
                    return "Trusted Certificate";
                } else {
                    return "Unknown";
                }
            }
        };
        col2.setSortable(true);
        this.certificatesGrid.addColumn(col2, MSGS.certificateKind());

        TextColumn<GwtKeystoreEntry> col3 = new TextColumn<GwtKeystoreEntry>() {

            @Override
            public String getValue(GwtKeystoreEntry object) {
                return String.valueOf(object.getKeystoreName());
            }
        };
        col3.setSortable(true);
        this.certificatesGrid.addColumn(col3, MSGS.certificateKeystoreName());
        
        TextColumn<GwtKeystoreEntry> col4 = new TextColumn<GwtKeystoreEntry>() {
        	
        	@Override
        	public String getValue(GwtKeystoreEntry object) {
        		Date date = object.getValidityStartDate();
        		return date != null ? DateUtils.formatDateTime(date) : "";
        	}
        };
        this.certificatesGrid.addColumn(col4, MSGS.certificateValidityStart());
        col4.setSortable(true);
        
        TextColumn<GwtKeystoreEntry> col5 = new TextColumn<GwtKeystoreEntry>() {
        	
        	@Override
        	public String getValue(GwtKeystoreEntry object) {
        		Date date = object.getValidityEndDate();
        		return date != null ? DateUtils.formatDateTime(date) : "";
        	}
        };
        this.certificatesGrid.addColumn(col5, MSGS.certificateValidityEnd());
        col5.setSortable(true);

        this.selectionModel.addSelectionChangeHandler(
                e -> this.uninstall.setEnabled(this.selectionModel.getSelectedObject() != null));

        this.certificatesGrid.getColumnSortList().push(col2);
        this.certificatesGrid.addColumnSortHandler(getAliasSortHandler(col1));
        this.certificatesGrid.addColumnSortHandler(getTypeSortHandler(col2));
        this.certificatesGrid.addColumnSortHandler(getNameSortHandler(col3));
        this.certificatesGrid.addColumnSortHandler(getStartDateSortHandler(col4));
        this.certificatesGrid.addColumnSortHandler(getEndDateSortHandler(col5));

        this.certificatesDataProvider.addDataDisplay(this.certificatesGrid);
        this.certificatesGrid.setSelectionModel(this.selectionModel);
    }

    private <U extends Comparable<U>> Comparator<GwtKeystoreEntry> getComparator(Function<GwtKeystoreEntry, U> comparableElementSupplier) {
    	return new Comparator<GwtKeystoreEntry>() {

			@Override
			public int compare(GwtKeystoreEntry o1, GwtKeystoreEntry o2) {
				if(o1 == o2)
					return 0;
				if(o1 == null)
					return -1;
				if(o2 == null)
					return 1;
				
				U item1 = comparableElementSupplier.apply(o1);
				U item2 = comparableElementSupplier.apply(o2);
				
				if(item1 == item2)
					return 0;
				if(item1 == null)
					return -1;
				if(item2 == null)
					return 1;
				
				return item1.compareTo(item2);
			}
    	};
    }
    
    private ListHandler<GwtKeystoreEntry> getNameSortHandler(TextColumn<GwtKeystoreEntry> col3) {
        ListHandler<GwtKeystoreEntry> nameSortHandler = new ListHandler<>(certificatesDataProvider.getList());

        nameSortHandler.setComparator(col3, getComparator(GwtKeystoreEntry::getKeystoreName));
        
        return nameSortHandler;
    }

    private ListHandler<GwtKeystoreEntry> getTypeSortHandler(TextColumn<GwtKeystoreEntry> col2) {
        ListHandler<GwtKeystoreEntry> typeSortHandler = new ListHandler<>(certificatesDataProvider.getList());

        typeSortHandler.setComparator(col2, getComparator(entry -> entry.getKind().name()));
        
        return typeSortHandler;
    }

    private ListHandler<GwtKeystoreEntry> getAliasSortHandler(TextColumn<GwtKeystoreEntry> col1) {
        ListHandler<GwtKeystoreEntry> aliasSortHandler = new ListHandler<>(certificatesDataProvider.getList());

        aliasSortHandler.setComparator(col1, getComparator(GwtKeystoreEntry::getAlias));
        
        return aliasSortHandler;
    }
    
    private ListHandler<GwtKeystoreEntry> getStartDateSortHandler(TextColumn<GwtKeystoreEntry> col4) {
        ListHandler<GwtKeystoreEntry> startDateSortHandler = new ListHandler<>(certificatesDataProvider.getList());
        
        startDateSortHandler.setComparator(col4, getComparator(GwtKeystoreEntry::getValidityStartDate));
 
        return startDateSortHandler;
    }
    
    private ListHandler<GwtKeystoreEntry> getEndDateSortHandler(TextColumn<GwtKeystoreEntry> col5) {
        ListHandler<GwtKeystoreEntry> endDateSortHandler = new ListHandler<>(certificatesDataProvider.getList());

        endDateSortHandler.setComparator(col5, getComparator(GwtKeystoreEntry::getValidityEndDate));
        
        return endDateSortHandler;
    }

    private void initInterfaceButtons() {
        this.refresh.setText(MSGS.refresh());
        this.refresh.addClickHandler(event -> refresh());

        this.add.setText(MSGS.addButton());
        this.add.addClickHandler(event -> {
            initCertificateTypeSelection();
            this.certAddModal.show();
        });

        this.uninstall.setText(MSGS.delete());
        this.uninstall.addClickHandler(event -> {
            final GwtKeystoreEntry selected = this.selectionModel.getSelectedObject();
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
        this.certAddModal.setTitle(MSGS.securityAddKeystoreEntry());
        ListBox certType = new ListBox();
        for (CertType c : CertType.values()) {
            certType.addItem(c.value());
        }

        this.certAddModalBody.clear();
        this.certAddModalBody.add(certType);

        this.closeModalButton.setText(MSGS.closeButton());
        this.nextStepButton.setVisible(true);
        this.nextStepButton.setText(MSGS.next());
        this.nextStepButton.addClickHandler(event -> {
            CertType selectedCertType = CertType.fromValue(certType.getSelectedValue());

            initCertificateAddModal(selectedCertType);
        });

    }

    private void initCertificateAddModal(CertType selectedCertType) {
        this.certAddModal.setTitle("Add Certificate");
        this.certAddModalBody.clear();

        final KeyPairTabUi widget = new KeyPairTabUi(selectedCertType.getType(), this.pids, this);
        this.certAddModalBody.add(widget);

        this.nextStepButton.setVisible(false);
    }

    private void uninstall(final GwtKeystoreEntry selected) {

        RequestQueue.submit(c -> this.gwtXSRFService.generateSecurityToken(c.callback(
                token -> this.gwtCertificatesService.removeEntry(token, selected, c.callback(ok -> refresh())))));

    }

    private enum CertType {

        KEY_PAIR(MSGS.securityAddKeyPair(), KeyPairTabUi.Type.KEY_PAIR),
        CERTIFICATE(MSGS.securityAddCertificate(), KeyPairTabUi.Type.CERTIFICATE);

        private String value;
        private KeyPairTabUi.Type type;

        private CertType(String v, KeyPairTabUi.Type type) {
            this.value = v;
            this.type = type;
        }

        public String value() {
            return this.value;
        }

        public KeyPairTabUi.Type getType() {
            return this.type;
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

    @Override
    public void onApply(final boolean isValid) {
        if (isValid) {
            this.certAddModal.hide();
        } else {
            alertDialog.show(MSGS.formWithErrorsOrIncomplete(), AlertDialog.Severity.ERROR, (ConfirmListener) null);
        }
    }

    @Override
    public void onKeystoreChanged() {
        this.refresh();
    }
}
