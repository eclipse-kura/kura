/*******************************************************************************
 * Copyright (c) 2017 Eurotech and/or its affiliates
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Eurotech
 *******************************************************************************/
package org.eclipse.kura.web.client.ui.drivers.assets;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.eclipse.kura.web.client.messages.Messages;
import org.eclipse.kura.web.client.ui.EntryClassUi;
import org.eclipse.kura.web.client.ui.wires.ValidationInputCell;
import org.eclipse.kura.web.client.util.FailureHandler;
import org.eclipse.kura.web.shared.AssetConstants;
import org.eclipse.kura.web.shared.model.GwtChannelData;
import org.eclipse.kura.web.shared.model.GwtChannelInfo;
import org.eclipse.kura.web.shared.model.GwtConfigComponent;
import org.eclipse.kura.web.shared.model.GwtXSRFToken;
import org.eclipse.kura.web.shared.service.GwtAssetService;
import org.eclipse.kura.web.shared.service.GwtAssetServiceAsync;
import org.eclipse.kura.web.shared.service.GwtComponentService;
import org.eclipse.kura.web.shared.service.GwtComponentServiceAsync;
import org.eclipse.kura.web.shared.service.GwtSecurityTokenService;
import org.eclipse.kura.web.shared.service.GwtSecurityTokenServiceAsync;
import org.eclipse.kura.web.shared.service.GwtWireService;
import org.eclipse.kura.web.shared.service.GwtWireServiceAsync;
import org.gwtbootstrap3.client.ui.Button;
import org.gwtbootstrap3.client.ui.ButtonGroup;
import org.gwtbootstrap3.client.ui.Modal;
import org.gwtbootstrap3.client.ui.ModalBody;
import org.gwtbootstrap3.client.ui.ModalFooter;
import org.gwtbootstrap3.client.ui.ModalHeader;
import org.gwtbootstrap3.client.ui.PanelBody;
import org.gwtbootstrap3.client.ui.gwt.CellTable;
import org.gwtbootstrap3.client.ui.html.Span;
import org.gwtbootstrap3.client.ui.html.Text;

import com.google.gwt.cell.client.FieldUpdater;
import com.google.gwt.cell.client.TextCell;
import com.google.gwt.core.client.GWT;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.user.cellview.client.Column;
import com.google.gwt.user.cellview.client.DefaultHeaderOrFooterBuilder;
import com.google.gwt.user.cellview.client.SimplePager;
import com.google.gwt.user.cellview.client.TextHeader;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.Widget;
import com.google.gwt.view.client.ListDataProvider;
import com.google.gwt.view.client.SingleSelectionModel;

public class AssetDataUi extends Composite {

    private static AssetDataUiBinder uiBinder = GWT.create(AssetDataUiBinder.class);

    interface AssetDataUiBinder extends UiBinder<Widget, AssetDataUi> {
    }

    protected static final Messages MSGS = GWT.create(Messages.class);
    private static final Logger errorLogger = Logger.getLogger("ErrorLogger");

    private static final int MAXIMUM_PAGE_SIZE = 5;

    private final GwtWireServiceAsync gwtWireService = GWT.create(GwtWireService.class);
    private final GwtAssetServiceAsync gwtAssetService = GWT.create(GwtAssetService.class);
    private static final GwtComponentServiceAsync gwtComponentService = GWT.create(GwtComponentService.class);
    private final GwtSecurityTokenServiceAsync gwtXSRFService = GWT.create(GwtSecurityTokenService.class);

    private final ListDataProvider<GwtChannelData> channelsDataProvider = new ListDataProvider<>();
    private final SingleSelectionModel<GwtChannelData> selectionModel = new SingleSelectionModel<>();

    private GwtConfigComponent configurableComponent;
    private GwtConfigComponent driverDescriptor;

    private boolean dirty;

    private Modal modal;

    @UiField
    PanelBody configurationPanelBody;
    @UiField
    Button applyDataChanges;
    @UiField
    Button refreshData;
    @UiField
    CellTable<GwtChannelData> assetDataTable;
    @UiField
    SimplePager channelPager;

    @UiField
    Modal incompleteFieldsModal;
    @UiField
    Text incompleteFieldsText;

    public AssetDataUi(GwtConfigComponent addedItem) {
        initWidget(uiBinder.createAndBindUi(this));
        this.configurableComponent = addedItem;

        this.channelPager.setPageSize(MAXIMUM_PAGE_SIZE);
        this.channelPager.setDisplay(this.assetDataTable);
        this.assetDataTable.setSelectionModel(this.selectionModel);
        this.channelsDataProvider.addDataDisplay(this.assetDataTable);

        initButtons();
        initTable();
        initInvalidDataModal();
    }

    private void initButtons() {
        this.applyDataChanges.setText(MSGS.apply());
        this.applyDataChanges.addClickHandler(new ClickHandler() {

            @Override
            public void onClick(ClickEvent event) {
                apply();
            }
        });

        this.refreshData.setText(MSGS.refresh());
        this.refreshData.addClickHandler(new ClickHandler() {

            @Override
            public void onClick(ClickEvent event) {
                renderForm();
            }
        });
        this.applyDataChanges.setEnabled(false);
    }

    private void initTable() {
        this.assetDataTable
                .setHeaderBuilder(new DefaultHeaderOrFooterBuilder<GwtChannelData>(this.assetDataTable, false));

        final Column<GwtChannelData, String> c = new Column<GwtChannelData, String>(new TextCell()) {

            @Override
            public String getValue(final GwtChannelData object) {
                return object.getName();
            }

        };

        this.assetDataTable.addColumn(c, new TextHeader(MSGS.wiresChannelName()));

        final Column<GwtChannelData, String> c2 = new Column<GwtChannelData, String>(new TextCell()) {

            @Override
            public String getValue(final GwtChannelData object) {
                return object.getType();
            }
        };

        this.assetDataTable.addColumn(c2, new TextHeader(MSGS.wiresChannelOperation()));

        final Column<GwtChannelData, String> c3 = new Column<GwtChannelData, String>(new TextCell()) {

            @Override
            public String getValue(final GwtChannelData object) {
                return object.getValueType();
            }
        };

        this.assetDataTable.addColumn(c3, new TextHeader(MSGS.wiresChannelValueType()));

        final ValidationInputCell cell = new ValidationInputCell();
        final Column<GwtChannelData, String> c4 = new Column<GwtChannelData, String>(cell) {

            @Override
            public String getValue(final GwtChannelData object) {
                Object result = object.get("value");
                if (result != null) {
                    return result.toString();
                }
                return "";
            }
        };

        c4.setFieldUpdater(new FieldUpdater<GwtChannelData, String>() {

            @Override
            public void update(final int index, final GwtChannelData object, final String value) {
                setDirty(true);
                object.set("value", value);
                AssetDataUi.this.assetDataTable.redraw();
            }
        });

        this.assetDataTable.addColumn(c4, new TextHeader(MSGS.devicePropValue()));
    }

    private void initInvalidDataModal() {
        this.incompleteFieldsModal.setTitle(MSGS.warning());
        this.incompleteFieldsText.setText(MSGS.formWithErrorsOrIncomplete());
    }

    private void apply() {
        if (isDirty()) {
            // TODO: maybe this can be declared in the xml?
            this.modal = new Modal();

            ModalHeader header = new ModalHeader();
            header.setTitle(MSGS.confirm());
            this.modal.add(header);

            ModalBody body = new ModalBody();
            body.add(new Span(MSGS.deviceConfigConfirmation(this.configurableComponent.getComponentName())));
            this.modal.add(body);

            ModalFooter footer = new ModalFooter();
            ButtonGroup group = new ButtonGroup();
            Button no = new Button();
            no.setText(MSGS.noButton());
            no.addStyleName("fa fa-times");
            no.addClickHandler(new ClickHandler() {

                @Override
                public void onClick(ClickEvent event) {
                    AssetDataUi.this.modal.hide();
                }
            });
            group.add(no);

            Button yes = new Button();
            yes.setText(MSGS.yesButton());
            yes.addStyleName("fa fa-check");
            yes.addClickHandler(new ClickHandler() {

                @Override
                public void onClick(ClickEvent event) {
                    EntryClassUi.showWaitModal();
                    AssetDataUi.this.gwtXSRFService.generateSecurityToken(new AsyncCallback<GwtXSRFToken>() {

                        @Override
                        public void onFailure(Throwable ex) {
                            EntryClassUi.hideWaitModal();
                            FailureHandler.handle(ex);
                        }

                        @Override
                        public void onSuccess(GwtXSRFToken token) {
                            final List<GwtChannelData> channelsData = new ArrayList<>(
                                    AssetDataUi.this.channelsDataProvider.getList());
                            AssetDataUi.this.gwtAssetService.write(token,
                                    AssetDataUi.this.configurableComponent.getComponentId(), channelsData,
                                    new AsyncCallback<Void>() {

                                @Override
                                public void onFailure(Throwable caught) {
                                    EntryClassUi.hideWaitModal();
                                    FailureHandler.handle(caught);
                                    errorLogger.log(
                                            Level.SEVERE, caught.getLocalizedMessage() != null
                                                    ? caught.getLocalizedMessage() : caught.getClass().getName(),
                                            caught);
                                }

                                @Override
                                public void onSuccess(Void result) {
                                    EntryClassUi.hideWaitModal();
                                    AssetDataUi.this.applyDataChanges.setEnabled(false);
                                    AssetDataUi.this.modal.hide();
                                }
                            });
                        }
                    });
                }
            });
            group.add(yes);
            footer.add(group);
            this.modal.add(footer);
            this.modal.show();
            no.setFocus(true);
        }
    }

    public void setDirty(boolean flag) {
        this.dirty = flag;
        if (this.dirty) {
            this.applyDataChanges.setEnabled(true);
        }
    }

    public boolean isDirty() {
        return this.dirty;
    }

    public void renderForm() {
        EntryClassUi.showWaitModal();
        this.gwtXSRFService.generateSecurityToken(new AsyncCallback<GwtXSRFToken>() {

            @Override
            public void onFailure(Throwable ex) {
                FailureHandler.handle(ex, EntryClassUi.class.getName());
            }

            @Override
            public void onSuccess(GwtXSRFToken token) {
                gwtComponentService.findFilteredComponentConfiguration(token,
                        AssetDataUi.this.configurableComponent.getComponentId(),
                        new AsyncCallback<List<GwtConfigComponent>>() {

                    @Override
                    public void onFailure(Throwable ex) {
                        FailureHandler.handle(ex, EntryClassUi.class.getName());
                    }

                    @Override
                    public void onSuccess(List<GwtConfigComponent> result) {
                        for (GwtConfigComponent configuration : result) {
                            AssetDataUi.this.configurableComponent = configuration;
                            AssetDataUi.this.gwtXSRFService.generateSecurityToken(new GetAssetDataCallback());
                        }
                    }
                });
            }
        });
    }

    private final class GetAssetDataCallback extends BaseAsyncCallback<GwtXSRFToken> {

        @Override
        public void onSuccess(GwtXSRFToken result) {
            AssetDataUi.this.gwtWireService.getGwtBaseChannelDescriptor(result,
                    new BaseAsyncCallback<GwtConfigComponent>() {

                        @Override
                        public void onSuccess(GwtConfigComponent result) {
                            AssetDataUi.this.gwtXSRFService
                                    .generateSecurityToken(new BaseAsyncCallback<GwtXSRFToken>() {

                                @Override
                                public void onSuccess(final GwtXSRFToken result) {
                                    AssetDataUi.this.gwtWireService.getGwtChannelDescriptor(result,
                                            AssetDataUi.this.configurableComponent
                                                    .get(AssetConstants.ASSET_DRIVER_PROP.value()).toString(),
                                            new BaseAsyncCallback<GwtConfigComponent>() {

                                        @Override
                                        public void onSuccess(final GwtConfigComponent result) {
                                            AssetDataUi.this.driverDescriptor = result;

                                            AssetDataUi.this.gwtXSRFService
                                                    .generateSecurityToken(new GetChannelDataCallback());
                                        }
                                    });
                                }
                            });
                        }

                    });
        }
    }

    private final class GetChannelDataCallback extends BaseAsyncCallback<GwtXSRFToken> {

        @Override
        public void onSuccess(final GwtXSRFToken token) {
            AssetDataUi.this.gwtWireService.getGwtChannels(token, AssetDataUi.this.driverDescriptor,
                    AssetDataUi.this.configurableComponent, new BaseAsyncCallback<List<GwtChannelInfo>>() {

                        @Override
                        public void onSuccess(List<GwtChannelInfo> result) {
                            final List<GwtChannelData> channelsData = new ArrayList<>();
                            for (GwtChannelInfo channelInfo : result) {
                                GwtChannelData channelData = new GwtChannelData();
                                channelData.setName(channelInfo.getName());
                                channelData.setType(channelInfo.getType());
                                channelData.setValueType(channelInfo.getValueType());
                                channelData.setProperties(channelInfo.getProperties());
                                channelData.setValue("Read Error!");
                                channelData.setUnescaped(true);
                                channelsData.add(channelData);
                            }
                            AssetDataUi.this.gwtXSRFService.generateSecurityToken(new AsyncCallback<GwtXSRFToken>() {

                                @Override
                                public void onFailure(Throwable ex) {
                                    EntryClassUi.hideWaitModal();
                                    FailureHandler.handle(ex, EntryClassUi.class.getName());
                                }

                                @Override
                                public void onSuccess(GwtXSRFToken token) {
                                    AssetDataUi.this.gwtAssetService.readAllChannels(token,
                                            AssetDataUi.this.configurableComponent.getComponentId(),
                                            new AsyncCallback<List<GwtChannelData>>() {

                                        @Override
                                        public void onFailure(Throwable ex) {
                                            EntryClassUi.hideWaitModal();
                                            FailureHandler.handle(ex, EntryClassUi.class.getName());
                                        }

                                        @Override
                                        public void onSuccess(final List<GwtChannelData> result) {
                                            for (GwtChannelData channelValue : result) {
                                                for (GwtChannelData channelData : channelsData) {
                                                    if (channelData.getName().equals(channelValue.getName())) {
                                                        channelData.setValue(channelValue.getValue());
                                                        break;
                                                    }
                                                }
                                            }
                                            AssetDataUi.this.channelsDataProvider.getList().clear();
                                            AssetDataUi.this.channelsDataProvider.getList().addAll(channelsData);
                                            AssetDataUi.this.channelsDataProvider.refresh();

                                            int size = AssetDataUi.this.channelsDataProvider.getList().size();
                                            AssetDataUi.this.assetDataTable.setVisibleRange(0, size);
                                            AssetDataUi.this.assetDataTable.redraw();

                                            AssetDataUi.this.applyDataChanges.setEnabled(false);

                                            EntryClassUi.hideWaitModal();
                                        }
                                    });
                                }
                            });
                        }
                    });
        }
    }

    private abstract class BaseAsyncCallback<T> implements AsyncCallback<T> {

        @Override
        public void onFailure(Throwable caught) {
            EntryClassUi.hideWaitModal();
            FailureHandler.handle(caught);
        }
    }
}
