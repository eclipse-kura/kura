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

import static org.eclipse.kura.web.shared.AssetConstants.CHANNEL_PROPERTY_SEPARATOR;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.logging.Level;

import org.eclipse.kura.web.client.ui.AbstractServicesUi;
import org.eclipse.kura.web.client.ui.EntryClassUi;
import org.eclipse.kura.web.client.ui.wires.ValidationData;
import org.eclipse.kura.web.client.ui.wires.ValidationInputCell;
import org.eclipse.kura.web.client.util.FailureHandler;
import org.eclipse.kura.web.shared.AssetConstants;
import org.eclipse.kura.web.shared.model.GwtChannelInfo;
import org.eclipse.kura.web.shared.model.GwtConfigComponent;
import org.eclipse.kura.web.shared.model.GwtConfigParameter;
import org.eclipse.kura.web.shared.model.GwtWiresChannelType;
import org.eclipse.kura.web.shared.model.GwtWiresDataType;
import org.eclipse.kura.web.shared.model.GwtXSRFToken;
import org.eclipse.kura.web.shared.service.GwtComponentService;
import org.eclipse.kura.web.shared.service.GwtComponentServiceAsync;
import org.eclipse.kura.web.shared.service.GwtSecurityTokenService;
import org.eclipse.kura.web.shared.service.GwtSecurityTokenServiceAsync;
import org.eclipse.kura.web.shared.service.GwtWireService;
import org.eclipse.kura.web.shared.service.GwtWireServiceAsync;
import org.gwtbootstrap3.client.ui.Alert;
import org.gwtbootstrap3.client.ui.Button;
import org.gwtbootstrap3.client.ui.ButtonGroup;
import org.gwtbootstrap3.client.ui.FieldSet;
import org.gwtbootstrap3.client.ui.FormGroup;
import org.gwtbootstrap3.client.ui.FormLabel;
import org.gwtbootstrap3.client.ui.Modal;
import org.gwtbootstrap3.client.ui.ModalBody;
import org.gwtbootstrap3.client.ui.ModalFooter;
import org.gwtbootstrap3.client.ui.ModalHeader;
import org.gwtbootstrap3.client.ui.Panel;
import org.gwtbootstrap3.client.ui.TextBox;
import org.gwtbootstrap3.client.ui.gwt.CellTable;
import org.gwtbootstrap3.client.ui.html.Span;
import org.gwtbootstrap3.client.ui.html.Strong;
import org.gwtbootstrap3.client.ui.html.Text;

import com.google.gwt.cell.client.FieldUpdater;
import com.google.gwt.cell.client.SelectionCell;
import com.google.gwt.cell.client.TextCell;
import com.google.gwt.core.client.GWT;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.dom.client.KeyUpEvent;
import com.google.gwt.event.dom.client.KeyUpHandler;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.user.cellview.client.Column;
import com.google.gwt.user.cellview.client.DefaultHeaderOrFooterBuilder;
import com.google.gwt.user.cellview.client.SimplePager;
import com.google.gwt.user.cellview.client.TextHeader;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.Widget;
import com.google.gwt.view.client.ListDataProvider;
import com.google.gwt.view.client.SelectionChangeEvent;
import com.google.gwt.view.client.SingleSelectionModel;

public class AssetConfigUi extends AbstractServicesUi {

    private static AssetConfigUiUiBinder uiBinder = GWT.create(AssetConfigUiUiBinder.class);

    interface AssetConfigUiUiBinder extends UiBinder<Widget, AssetConfigUi> {
    }

    private final GwtWireServiceAsync gwtWireService = GWT.create(GwtWireService.class);
    private final GwtComponentServiceAsync gwtComponentService = GWT.create(GwtComponentService.class);
    private final GwtSecurityTokenServiceAsync gwtXSRFService = GWT.create(GwtSecurityTokenService.class);

    private final ListDataProvider<GwtChannelInfo> channelsDataProvider = new ListDataProvider<>();

    private final SingleSelectionModel<GwtChannelInfo> selectionModel = new SingleSelectionModel<>();

    private static final String INVALID_CLASS_NAME = "error-text-box";

    private static final int MAXIMUM_PAGE_SIZE = 5;

    private final Set<String> nonValidatedCells;

    private boolean dirty;

    private GwtConfigComponent baseDriverDescriptor;
    private GwtConfigComponent driverDescriptor;

    private Modal modal;

    private GwtConfigComponent originalConfig;

    @UiField
    Button applyConfigurationEdit;
    @UiField
    Button resetConfigurationEdit;

    @UiField
    Button btnAdd;
    @UiField
    Button btnRemove;
    @UiField
    Button btnDownload;
    @UiField
    Button btnUpload;

    @UiField
    SimplePager channelPager;
    @UiField
    Panel channelPanel;

    @UiField
    CellTable<GwtChannelInfo> channelTable;

    @UiField
    Strong channelTitle;
    @UiField
    FieldSet fields;

    @UiField
    Alert incompleteFields;

    @UiField
    Modal incompleteFieldsModal;

    @UiField
    Text incompleteFieldsText;

    @UiField
    Modal newChannelModal;
    @UiField
    FormLabel newChannelNameLabel;
    @UiField
    FormLabel newChannelNameError;
    @UiField
    TextBox newChannelNameInput;
    @UiField
    Button btnCreateNewChannel;
    @UiField
    Button btnCancelCreatingNewChannel;

    public AssetConfigUi(final GwtConfigComponent addedItem) {
        initWidget(uiBinder.createAndBindUi(this));
        this.originalConfig = addedItem;
        restoreConfiguration(this.originalConfig);
        this.fields.clear();

        this.channelPager.setPageSize(MAXIMUM_PAGE_SIZE);
        this.channelPager.setDisplay(this.channelTable);
        this.channelTable.setSelectionModel(this.selectionModel);
        this.channelsDataProvider.addDataDisplay(this.channelTable);
        this.channelPanel.setVisible(false);
        this.btnRemove.setEnabled(false);

        this.nonValidatedCells = new HashSet<>();
        this.btnAdd.addClickHandler(new ClickHandler() {

            @Override
            public void onClick(final ClickEvent event) {
                AssetConfigUi.this.newChannelNameInput.setText(getNewChannelName());
                AssetConfigUi.this.newChannelModal.show();
            }
        });

        this.selectionModel.addSelectionChangeHandler(new SelectionChangeEvent.Handler() {

            @Override
            public void onSelectionChange(final SelectionChangeEvent event) {
                AssetConfigUi.this.btnRemove.setEnabled(AssetConfigUi.this.selectionModel.getSelectedObject() != null);
            }
        });

        initInvalidDataModal();
        initNewChannelModal();
        initButtons();

        setDirty(false);
    }

    private void initButtons() {
        this.applyConfigurationEdit.setText(MSGS.apply());
        this.applyConfigurationEdit.addClickHandler(new ClickHandler() {

            @Override
            public void onClick(ClickEvent event) {
                apply();
            }
        });

        this.resetConfigurationEdit.setText(MSGS.reset());
        this.resetConfigurationEdit.addClickHandler(new ClickHandler() {

            @Override
            public void onClick(ClickEvent event) {
                reset();
            }
        });

        this.applyConfigurationEdit.setEnabled(false);
        this.resetConfigurationEdit.setEnabled(false);
    }

    private void apply() {
        if (isValid()) {
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
                        AssetConfigUi.this.modal.hide();
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
                        try {
                            getUpdatedConfiguration();
                        } catch (Exception ex) {
                            EntryClassUi.hideWaitModal();
                            FailureHandler.handle(ex);
                            return;
                        }
                        AssetConfigUi.this.gwtXSRFService.generateSecurityToken(new AsyncCallback<GwtXSRFToken>() {

                            @Override
                            public void onFailure(Throwable ex) {
                                EntryClassUi.hideWaitModal();
                                FailureHandler.handle(ex);
                            }

                            @Override
                            public void onSuccess(GwtXSRFToken token) {
                                AssetConfigUi.this.gwtComponentService.deleteFactoryConfiguration(token,
                                        AssetConfigUi.this.configurableComponent.getComponentId(), false,
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
                                        AssetConfigUi.this.gwtXSRFService
                                                .generateSecurityToken(new AsyncCallback<GwtXSRFToken>() {

                                            @Override
                                            public void onFailure(Throwable ex) {
                                                EntryClassUi.hideWaitModal();
                                                FailureHandler.handle(ex);
                                            }

                                            @Override
                                            public void onSuccess(GwtXSRFToken token) {
                                                AssetConfigUi.this.gwtComponentService.createFactoryComponent(token,
                                                        AssetConfigUi.this.configurableComponent.getFactoryId(),
                                                        AssetConfigUi.this.configurableComponent.getComponentId(),
                                                        AssetConfigUi.this.configurableComponent,
                                                        new AsyncCallback<Void>() {

                                                    @Override
                                                    public void onFailure(Throwable caught) {
                                                        EntryClassUi.hideWaitModal();
                                                        FailureHandler.handle(caught);
                                                        errorLogger.log(Level.SEVERE,
                                                                caught.getLocalizedMessage() != null
                                                                        ? caught.getLocalizedMessage()
                                                                        : caught.getClass().getName(),
                                                                caught);
                                                    }

                                                    @Override
                                                    public void onSuccess(Void result) {
                                                        AssetConfigUi.this.modal.hide();
                                                        logger.info(MSGS.info() + ": " + MSGS.deviceConfigApplied());
                                                        AssetConfigUi.this.applyConfigurationEdit.setEnabled(false);
                                                        AssetConfigUi.this.resetConfigurationEdit.setEnabled(false);
                                                        setDirty(false);
                                                        AssetConfigUi.this.originalConfig = AssetConfigUi.this.configurableComponent;
                                                        EntryClassUi.hideWaitModal();
                                                    }
                                                });

                                            }
                                        });
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
        } else {
            errorLogger.log(Level.SEVERE, "Device configuration error!");
            this.incompleteFieldsModal.show();
        }
    }

    @Override
    protected void setDirty(boolean flag) {
        this.dirty = flag;
        if (this.dirty) {
            this.applyConfigurationEdit.setEnabled(true);
            this.resetConfigurationEdit.setEnabled(true);
        }
    }

    @Override
    public boolean isDirty() {
        return this.dirty;
    }

    @Override
    protected void reset() {
        return;
    }

    @Override
    protected void renderForm() {
        this.fields.clear();
        for (final GwtConfigParameter param : this.configurableComponent.getParameters()) {
            final String[] tokens = param.getId().split(CHANNEL_PROPERTY_SEPARATOR.value());
            boolean isChannelData = tokens.length == 2;
            final boolean isDriverField = param.getId().equals(AssetConstants.ASSET_DRIVER_PROP.value());

            if (!isChannelData && !isDriverField) {
                if (param.getCardinality() == 0 || param.getCardinality() == 1 || param.getCardinality() == -1) {
                    final FormGroup formGroup = new FormGroup();
                    renderConfigParameter(param, true, formGroup);
                } else {
                    renderMultiFieldConfigParameter(param);
                }
            }
        }

        EntryClassUi.showWaitModal();
        this.gwtXSRFService.generateSecurityToken(new GetAssetDataCallback());

    }

    private final class GetAssetDataCallback extends BaseAsyncCallback<GwtXSRFToken> {

        @Override
        public void onSuccess(GwtXSRFToken result) {
            AssetConfigUi.this.gwtWireService.getGwtBaseChannelDescriptor(result,
                    new BaseAsyncCallback<GwtConfigComponent>() {

                        @Override
                        public void onSuccess(GwtConfigComponent result) {
                            AssetConfigUi.this.baseDriverDescriptor = result;
                            AssetConfigUi.this.gwtXSRFService
                                    .generateSecurityToken(new BaseAsyncCallback<GwtXSRFToken>() {

                                @Override
                                public void onSuccess(final GwtXSRFToken result) {
                                    AssetConfigUi.this.gwtWireService.getGwtChannelDescriptor(result,
                                            AssetConfigUi.this.configurableComponent
                                                    .get(AssetConstants.ASSET_DRIVER_PROP.value()).toString(),
                                            new BaseAsyncCallback<GwtConfigComponent>() {

                                        @Override
                                        public void onSuccess(final GwtConfigComponent result) {
                                            AssetConfigUi.this.driverDescriptor = result;

                                            int columnCount = AssetConfigUi.this.channelTable.getColumnCount();
                                            for (int i = 0; i < columnCount; i++) {
                                                AssetConfigUi.this.channelTable.removeColumn(0);
                                            }

                                            addDefaultColumns();
                                            for (final GwtConfigParameter param : result.getParameters()) {
                                                AssetConfigUi.this.channelTable.addColumn(getColumnFromParam(param),
                                                        new TextHeader(param.getName()));
                                            }

                                            AssetConfigUi.this.gwtXSRFService
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
        public void onSuccess(final GwtXSRFToken result) {
            AssetConfigUi.this.gwtWireService.getGwtChannels(result, AssetConfigUi.this.driverDescriptor,
                    AssetConfigUi.this.configurableComponent, new BaseAsyncCallback<List<GwtChannelInfo>>() {

                        @Override
                        public void onSuccess(List<GwtChannelInfo> result) {
                            for (GwtChannelInfo channelInfo : result) {
                                channelInfo.setUnescaped(true);
                            }
                            AssetConfigUi.this.channelsDataProvider.getList().clear();
                            AssetConfigUi.this.channelsDataProvider.getList().addAll(result);
                            AssetConfigUi.this.channelsDataProvider.refresh();
                            AssetConfigUi.this.channelPanel.setVisible(true);
                            EntryClassUi.hideWaitModal();
                        }
                    });
        }
    }

    private void addDefaultColumns() {

        this.channelTable.setHeaderBuilder(new DefaultHeaderOrFooterBuilder<GwtChannelInfo>(this.channelTable, false));

        final Column<GwtChannelInfo, String> c = new Column<GwtChannelInfo, String>(new TextCell()) {

            @Override
            public String getValue(final GwtChannelInfo object) {
                return object.getName();
            }

        };

        this.channelTable.addColumn(c, new TextHeader(MSGS.wiresChannelName()));

        final List<String> valueOptions = Arrays.asList(GwtWiresChannelType.READ.name(),
                GwtWiresChannelType.WRITE.name(), GwtWiresChannelType.READ_WRITE.name());

        final Column<GwtChannelInfo, String> c2 = new Column<GwtChannelInfo, String>(new SelectionCell(valueOptions)) {

            @Override
            public String getValue(final GwtChannelInfo object) {
                return object.getType();
            }
        };

        c2.setFieldUpdater(new FieldUpdater<GwtChannelInfo, String>() {

            @Override
            public void update(final int index, final GwtChannelInfo object, final String value) {
                object.setType(value);
                setDirty(true);
                AssetConfigUi.this.channelTable.redraw();
            }
        });
        this.channelTable.addColumn(c2, new TextHeader(MSGS.wiresChannelOperation()));

        final List<String> valueTypeOptions = Arrays.asList(GwtWiresDataType.BOOLEAN.name(),
                GwtWiresDataType.BYTE_ARRAY.name(), GwtWiresDataType.FLOAT.name(), GwtWiresDataType.DOUBLE.name(),
                GwtWiresDataType.INTEGER.name(), GwtWiresDataType.LONG.name(), GwtWiresDataType.STRING.name());

        final Column<GwtChannelInfo, String> c3 = new Column<GwtChannelInfo, String>(
                new SelectionCell(valueTypeOptions)) {

            @Override
            public String getValue(final GwtChannelInfo object) {
                return object.getValueType();
            }
        };

        c3.setFieldUpdater(new FieldUpdater<GwtChannelInfo, String>() {

            @Override
            public void update(final int index, final GwtChannelInfo object, final String value) {
                object.setValueType(value);
                setDirty(true);
                AssetConfigUi.this.channelTable.redraw();
            }
        });
        this.channelTable.addColumn(c3, new TextHeader(MSGS.wiresChannelValueType()));

    }

    private Column<GwtChannelInfo, String> getColumnFromParam(final GwtConfigParameter param) {
        final Map<String, String> options = param.getOptions();
        if (options != null && options.size() > 0) {
            return getSelectionInputColumn(param);
        } else {
            return getInputCellColumn(param);
        }
    }

    private Column<GwtChannelInfo, String> getSelectionInputColumn(final GwtConfigParameter param) {
        final String id = param.getId();
        final Map<String, String> labelsToValues = param.getOptions();
        final Map<String, String> valuesToLabels = new HashMap<>();
        for (Entry<String, String> entry : labelsToValues.entrySet()) {
            valuesToLabels.put(entry.getValue(), entry.getKey());
        }
        final ArrayList<String> labels = new ArrayList<>(labelsToValues.keySet());
        final SelectionCell cell = new SelectionCell(new ArrayList<>(labels));
        final Column<GwtChannelInfo, String> result = new Column<GwtChannelInfo, String>(cell) {

            @Override
            public String getValue(final GwtChannelInfo object) {
                Object result = object.get(id);
                if (result == null) {
                    final String defaultValue = param.getDefault();
                    result = defaultValue != null ? defaultValue : labelsToValues.get(labels.get(0));
                    object.set(id, result);
                }
                return valuesToLabels.get(result.toString());
            }
        };

        result.setFieldUpdater(new FieldUpdater<GwtChannelInfo, String>() {

            @Override
            public void update(final int index, final GwtChannelInfo object, final String label) {
                setDirty(true);
                object.set(param.getId(), labelsToValues.get(label));
                AssetConfigUi.this.channelTable.redraw();
            }
        });

        return result;
    }

    private Column<GwtChannelInfo, String> getInputCellColumn(final GwtConfigParameter param) {
        final String id = param.getId();
        final ValidationInputCell cell = new ValidationInputCell();
        final Column<GwtChannelInfo, String> result = new Column<GwtChannelInfo, String>(cell) {

            @Override
            public String getValue(final GwtChannelInfo object) {
                Object result = object.get(id);
                if (result != null) {
                    return result.toString();
                }
                return param.isRequired() ? param.getDefault() : null;
            }
        };

        result.setFieldUpdater(new FieldUpdater<GwtChannelInfo, String>() {

            @Override
            public void update(final int index, final GwtChannelInfo object, final String value) {
                ValidationData viewData;
                if (!isValid(param, value)) {
                    viewData = cell.getViewData(object);
                    viewData.setInvalid(true);
                    AssetConfigUi.this.nonValidatedCells.add(object.getName());
                    // We only modified the cell, so do a local redraw.
                    AssetConfigUi.this.channelTable.redraw();
                    return;
                }
                AssetConfigUi.this.nonValidatedCells.remove(object.getName());
                setDirty(true);
                AssetConfigUi.this.channelTable.redraw();
                object.set(param.getId(), value);
            }
        });

        return result;
    }

    private void initInvalidDataModal() {
        this.incompleteFieldsModal.setTitle(MSGS.warning());
        this.incompleteFieldsText.setText(MSGS.formWithErrorsOrIncomplete());
    }

    private void initNewChannelModal() {
        this.newChannelModal.setTitle(MSGS.wiresCreateNewChannel());
        this.newChannelNameLabel.setText(MSGS.wiresCreateNewChannelName());
        this.btnCreateNewChannel.setText(MSGS.addButton());
        this.btnCancelCreatingNewChannel.setText(MSGS.cancelButton());

        this.newChannelNameInput.addKeyUpHandler(new KeyUpHandler() {

            @Override
            public void onKeyUp(KeyUpEvent event) {
                ValidationData isChannelNameValid = validateChannelName(
                        AssetConfigUi.this.newChannelNameInput.getValue().trim());
                if (isChannelNameValid.isInvalid()) {
                    AssetConfigUi.this.newChannelNameInput.addStyleName(INVALID_CLASS_NAME);
                    AssetConfigUi.this.newChannelNameError.setText(isChannelNameValid.getValue());
                    return;
                }
                AssetConfigUi.this.newChannelNameError.setText("");
                AssetConfigUi.this.newChannelNameInput.removeStyleName(INVALID_CLASS_NAME);
            }
        });

        this.btnCreateNewChannel.addClickHandler(new ClickHandler() {

            @Override
            public void onClick(final ClickEvent event) {
                final String newChannelName = AssetConfigUi.this.newChannelNameInput.getValue().trim();

                ValidationData isChannelNameValid = validateChannelName(newChannelName);
                if (isChannelNameValid.isInvalid()) {
                    return;
                }

                final GwtChannelInfo ci = new GwtChannelInfo();
                ci.setUnescaped(true);
                ci.setName(newChannelName);
                ci.setType(GwtWiresChannelType.READ.name());
                ci.setValueType(GwtWiresDataType.INTEGER.name());
                for (final GwtConfigParameter param : AssetConfigUi.this.driverDescriptor.getParameters()) {
                    ci.set(param.getName(), param.getDefault());
                }

                AssetConfigUi.this.channelsDataProvider.getList().add(ci);
                AssetConfigUi.this.channelsDataProvider.refresh();
                AssetConfigUi.this.channelPager.lastPage();
                setDirty(true);
                AssetConfigUi.this.newChannelModal.hide();
            }
        });

        this.btnRemove.addClickHandler(new ClickHandler() {

            @Override
            public void onClick(final ClickEvent event) {
                final GwtChannelInfo ci = AssetConfigUi.this.selectionModel.getSelectedObject();
                AssetConfigUi.this.channelsDataProvider.getList().remove(ci);
                AssetConfigUi.this.channelsDataProvider.refresh();
                AssetConfigUi.this.btnRemove.setEnabled(false);
                setDirty(true);
            }
        });
    }

    private ValidationData validateChannelName(final String channelName) {
        ValidationData result = new ValidationData();

        if (channelName.isEmpty()) {
            result.setInvalid(true);
            result.setValue(MSGS.wiresChannelNameEmpty());
            return result;
        }

        final String prohibitedChars = AssetConstants.CHANNEL_NAME_PROHIBITED_CHARS.value();

        for (int i = 0; i < prohibitedChars.length(); i++) {
            final char prohibitedChar = prohibitedChars.charAt(i);
            if (channelName.indexOf(prohibitedChar) != -1) {
                result.setInvalid(true);
                result.setValue(MSGS.wiresChannelNameInvalidCharacters() + " \'" + prohibitedChar + '\'');
                return result;
            }
        }

        if (channelName.indexOf(' ') != -1) {
            result.setInvalid(true);
            result.setValue(MSGS.wiresChannelNameNoSpaces());
            return result;
        }

        if (channelExists(channelName)) {
            result.setInvalid(true);
            result.setValue(MSGS.wiresChannelNameAlreadyPresent());
            return result;
        }

        result.setInvalid(false);
        return result;
    }

    private String getNewChannelName() {
        int suffix = 1;
        String result = null;
        while (channelExists(result = MSGS.wiresChannel() + suffix)) {
            suffix++;
        }
        return result;
    }

    private boolean channelExists(String channelName) {
        for (GwtChannelInfo channelInfo : this.channelsDataProvider.getList()) {
            if (channelName.equals(channelInfo.getName())) {
                return true;
            }
        }
        return false;
    }

    private abstract class BaseAsyncCallback<T> implements AsyncCallback<T> {

        @Override
        public void onFailure(Throwable caught) {
            EntryClassUi.hideWaitModal();
            FailureHandler.handle(caught);
        }
    }

    // Get updated parameters
    protected GwtConfigComponent getUpdatedConfiguration() {
        Iterator<Widget> it = this.fields.iterator();
        while (it.hasNext()) {
            Widget w = it.next();
            if (w instanceof FormGroup) {
                FormGroup fg = (FormGroup) w;
                fillUpdatedConfiguration(fg);
            }
        }

        this.channelsDataProvider.refresh();
        clearChannelsFromConfig();

        for (final GwtChannelInfo ci : this.channelsDataProvider.getList()) {
            StringBuilder prefixBuilder = new StringBuilder(ci.getName());
            prefixBuilder.append(CHANNEL_PROPERTY_SEPARATOR.value());
            String prefix = prefixBuilder.toString();

            final GwtConfigParameter newType = copyOf(
                    this.baseDriverDescriptor.getParameter(AssetConstants.TYPE.value()));
            newType.setName(prefix + AssetConstants.TYPE.value());
            newType.setId(prefix + AssetConstants.TYPE.value());
            newType.setValue(ci.getType());
            this.configurableComponent.getParameters().add(newType);

            final GwtConfigParameter newValueType = copyOf(
                    this.baseDriverDescriptor.getParameter(AssetConstants.VALUE_TYPE.value()));
            newValueType.setName(prefix + AssetConstants.VALUE_TYPE.value());
            newValueType.setId(prefix + AssetConstants.VALUE_TYPE.value());
            newValueType.setValue(ci.getValueType());
            this.configurableComponent.getParameters().add(newValueType);

            for (final GwtConfigParameter param : this.driverDescriptor.getParameters()) {
                final GwtConfigParameter newParam = copyOf(param);
                newParam.setName(prefix + param.getName());
                newParam.setId(prefix + param.getId());
                final Object value = ci.get(param.getName());
                newParam.setValue(value != null ? value.toString() : null);
                this.configurableComponent.getParameters().add(newParam);
            }
        }

        return this.configurableComponent;
    }

    private void clearChannelsFromConfig() {
        final List<GwtConfigParameter> params = this.configurableComponent.getParameters();
        final Iterator<GwtConfigParameter> it = params.iterator();
        while (it.hasNext()) {
            final GwtConfigParameter p = it.next();
            if (p.getName() != null
                    && p.getName().indexOf(AssetConstants.CHANNEL_PROPERTY_SEPARATOR.value().charAt(0)) != -1) {
                it.remove();
            }
        }
    }

    private GwtConfigParameter copyOf(final GwtConfigParameter source) {
        final GwtConfigParameter newParam = new GwtConfigParameter();
        newParam.setCardinality(source.getCardinality());
        newParam.setDefault(source.getDefault());
        newParam.setDescription(source.getDescription());
        newParam.setId(source.getId());
        newParam.setMax(source.getMax());
        newParam.setMin(source.getMin());
        newParam.setName(source.getName());
        newParam.setRequired(source.isRequired());
        newParam.setType(source.getType());
        newParam.setValue(source.getValue());
        if (source.getValues() != null) {
            newParam.setValues(Arrays.copyOf(source.getValues(), source.getValues().length));
        }
        if (source.getOptions() != null) {
            final Map<String, String> newOpts = new HashMap<>(source.getOptions());
            newParam.setOptions(newOpts);
        }

        return newParam;
    }

    @Override
    protected void renderBooleanField(final GwtConfigParameter param, boolean isFirstInstance, FormGroup formGroup) {
        super.renderBooleanField(param, isFirstInstance, formGroup);
        this.fields.add(formGroup);
    }

    @Override
    protected void renderChoiceField(final GwtConfigParameter param, boolean isFirstInstance, FormGroup formGroup) {
        super.renderChoiceField(param, isFirstInstance, formGroup);
        this.fields.add(formGroup);
    }

    @Override
    protected void renderPasswordField(final GwtConfigParameter param, boolean isFirstInstance, FormGroup formGroup) {
        super.renderPasswordField(param, isFirstInstance, formGroup);
        this.fields.add(formGroup);
    }

    @Override
    protected void renderTextField(final GwtConfigParameter param, boolean isFirstInstance, final FormGroup formGroup) {
        super.renderTextField(param, isFirstInstance, formGroup);
        this.fields.add(formGroup);
    }
}
