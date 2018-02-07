/*******************************************************************************
 * Copyright (c) 2016, 2018 Eurotech and/or its affiliates and others
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *  Eurotech
 *  Amit Kumar Mondal
 *
 *******************************************************************************/
/**
 * Render the Content in the Wire Component Properties Panel based on Service (GwtBSConfigComponent) selected in Wire graph
 *
 * Fields are rendered based on their type (Password(Input), Choice(Dropboxes) etc. with Text fields rendered
 * for both numeric and other textual field with validate() checking if value in numeric fields is numeric
 */
package org.eclipse.kura.web.client.ui.drivers.assets;

import static org.eclipse.kura.web.shared.AssetConstants.CHANNEL_PROPERTY_SEPARATOR;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.eclipse.kura.web.client.configuration.HasConfiguration;
import org.eclipse.kura.web.client.ui.AbstractServicesUi;
import org.eclipse.kura.web.client.ui.drivers.assets.AssetModel.ChannelModel;
import org.eclipse.kura.web.client.ui.wires.ValidationData;
import org.eclipse.kura.web.client.ui.wires.ValidationInputCell;
import org.eclipse.kura.web.shared.AssetConstants;
import org.eclipse.kura.web.shared.model.GwtConfigComponent;
import org.eclipse.kura.web.shared.model.GwtConfigParameter;
import org.eclipse.kura.web.shared.model.GwtConfigParameter.GwtConfigParameterType;
import org.gwtbootstrap3.client.ui.Button;
import org.gwtbootstrap3.client.ui.FieldSet;
import org.gwtbootstrap3.client.ui.FormGroup;
import org.gwtbootstrap3.client.ui.FormLabel;
import org.gwtbootstrap3.client.ui.Modal;
import org.gwtbootstrap3.client.ui.Panel;
import org.gwtbootstrap3.client.ui.TextBox;
import org.gwtbootstrap3.client.ui.gwt.CellTable;
import org.gwtbootstrap3.client.ui.html.Strong;

import com.google.gwt.cell.client.AbstractCell;
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
import com.google.gwt.user.cellview.client.SimplePager;
import com.google.gwt.user.cellview.client.TextHeader;
import com.google.gwt.user.client.ui.Widget;
import com.google.gwt.view.client.ListDataProvider;
import com.google.gwt.view.client.SelectionChangeEvent;
import com.google.gwt.view.client.SingleSelectionModel;

public class AssetConfigurationUi extends AbstractServicesUi implements HasConfiguration {

    interface ServicesUiUiBinder extends UiBinder<Widget, AssetConfigurationUi> {
    }

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
    CellTable<ChannelModel> channelTable;

    @UiField
    Strong channelTitle;
    @UiField
    FieldSet fields;

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

    private static final String INVALID_CLASS_NAME = "error-text-box";

    private static final int MAXIMUM_PAGE_SIZE = 5;

    private static ServicesUiUiBinder uiBinder = GWT.create(ServicesUiUiBinder.class);

    private final ListDataProvider<ChannelModel> channelsDataProvider = new ListDataProvider<>();

    private final SingleSelectionModel<ChannelModel> selectionModel = new SingleSelectionModel<>();

    private final Set<String> nonValidatedCells;

    private boolean dirty;

    private AssetModel model;
    private Widget associatedView;

    private HasConfiguration.Listener listener;

    public AssetConfigurationUi(final AssetModel assetModel, final Widget associatedView) {
        initWidget(uiBinder.createAndBindUi(this));
        this.model = assetModel;
        this.fields.clear();

        this.channelPager.setPageSize(MAXIMUM_PAGE_SIZE);
        this.channelPager.setDisplay(this.channelTable);
        this.channelTable.setSelectionModel(this.selectionModel);
        this.channelsDataProvider.addDataDisplay(this.channelTable);
        this.channelPanel.setVisible(false);
        this.btnRemove.setEnabled(false);
        this.associatedView = associatedView;

        this.nonValidatedCells = new HashSet<>();

        this.btnDownload.addClickHandler(new ClickHandler() {

            @Override
            public void onClick(final ClickEvent event) {
                // TODO implement configuration download
            }
        });

        this.btnAdd.addClickHandler(new ClickHandler() {

            @Override
            public void onClick(final ClickEvent event) {
                AssetConfigurationUi.this.newChannelNameInput.setText(getNewChannelName());
                AssetConfigurationUi.this.newChannelModal.show();
            }
        });

        this.selectionModel.addSelectionChangeHandler(new SelectionChangeEvent.Handler() {

            @Override
            public void onSelectionChange(final SelectionChangeEvent event) {
                AssetConfigurationUi.this.btnRemove
                        .setEnabled(AssetConfigurationUi.this.selectionModel.getSelectedObject() != null);
            }
        });

        setModel(assetModel);
        initNewChannelModal();
    }

    public void setModel(AssetModel model) {
        this.model = model;
        AssetConfigurationUi.this.channelTitle.setText(MSGS.channelTableTitle(
                model.getConfiguration().getParameterValue(AssetConstants.ASSET_DRIVER_PROP.value())));
        renderForm();
        channelTable.redraw();
        setDirty(false);
    }

    @Override
    public void renderForm() {
        this.fields.clear();

        final GwtConfigComponent nonChannelFields = new GwtConfigComponent();

        for (final GwtConfigParameter param : this.model.getConfiguration().getParameters()) {
            final String[] tokens = param.getId().split(CHANNEL_PROPERTY_SEPARATOR.value());
            boolean isChannelData = tokens.length == 2;
            final boolean isDriverField = param.getId().equals(AssetConstants.ASSET_DRIVER_PROP.value());

            if (!isChannelData && !isDriverField) {
                nonChannelFields.getParameters().add(param);
                if (param.getCardinality() == 0 || param.getCardinality() == 1 || param.getCardinality() == -1) {
                    final FormGroup formGroup = new FormGroup();
                    renderConfigParameter(param, true, formGroup);
                } else {
                    renderMultiFieldConfigParameter(param);
                }
            }
        }

        this.configurableComponent = nonChannelFields;
        initTable();

    }

    private void initTable() {

        int columnCount = AssetConfigurationUi.this.channelTable.getColumnCount();
        for (int i = 0; i < columnCount; i++) {
            AssetConfigurationUi.this.channelTable.removeColumn(0);
        }

        for (final GwtConfigParameter param : this.model.getChannelDescriptor().getParameters()) {
            AssetConfigurationUi.this.channelTable.addColumn(
                    getColumnFromParam(param, param.getId().equals(AssetConstants.NAME.value())),
                    new TextHeader(param.getName()));
        }

        this.channelsDataProvider.setList(model.getChannels());
        this.channelsDataProvider.refresh();
        this.channelPanel.setVisible(true);
    }

    @Override
    public void setDirty(final boolean flag) {
        boolean isDirtyStateChanged = flag != this.dirty;
        this.dirty = flag;
        if (listener != null) {
            if (isDirtyStateChanged) {
                listener.onDirtyStateChanged(this);
            }
            if (isValid()) {
                listener.onConfigurationChanged(this);
            }
        }
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

    @Override
    protected void reset() {
        return;
    }

    private Column<ChannelModel, String> getColumnFromParam(final GwtConfigParameter param, boolean isReadOnly) {
        final Map<String, String> options = param.getOptions();
        if (options != null && options.size() > 0) {
            return getSelectionInputColumn(param, isReadOnly);
        } else {
            return getInputCellColumn(param, isReadOnly);
        }
    }

    private Column<ChannelModel, String> getInputCellColumn(final GwtConfigParameter param, boolean isReadOnly) {
        final String id = param.getId();
        final AbstractCell<String> cell;
        if (isReadOnly) {
            cell = new TextCell();
        } else if (param.getType() == GwtConfigParameterType.BOOLEAN) {
            cell = new BooleanInputCell();
        } else {
            cell = new ValidationInputCell();
        }

        final Column<ChannelModel, String> result = new Column<ChannelModel, String>(cell) {

            @Override
            public String getValue(final ChannelModel object) {
                String result = object.getValue(id);
                if (result != null) {
                    return result;
                }
                return param.isRequired() ? param.getDefault() : null;
            }
        };

        if (!isReadOnly) {
            result.setFieldUpdater(new FieldUpdater<ChannelModel, String>() {

                @Override
                public void update(final int index, final ChannelModel object, final String value) {
                    ValidationData viewData;
                    if (!isValid(param, value)) {
                        viewData = ((ValidationInputCell) cell).getViewData(object);
                        viewData.setInvalid(true);
                        AssetConfigurationUi.this.nonValidatedCells.add(object.getChannelName());
                        // We only modified the cell, so do a local redraw.
                        AssetConfigurationUi.this.channelTable.redraw();
                        return;
                    }
                    AssetConfigurationUi.this.nonValidatedCells.remove(object.getChannelName());
                    AssetConfigurationUi.this.setDirty(true);
                    AssetConfigurationUi.this.channelTable.redraw();
                    object.setValue(param.getId(), value);
                }
            });
        }

        return result;
    }

    private Column<ChannelModel, String> getSelectionInputColumn(final GwtConfigParameter param, boolean isReadOnly) {
        final String id = param.getId();
        final Map<String, String> labelsToValues = param.getOptions();
        ArrayList<Entry<String, String>> sortedLabelsToValues = new ArrayList<>(labelsToValues.entrySet());
        Collections.sort(sortedLabelsToValues, DROPDOWN_LABEL_COMPARATOR);
        final ArrayList<String> labels = new ArrayList<>();
        final Map<String, String> valuesToLabels = new HashMap<>();
        for (Entry<String, String> entry : sortedLabelsToValues) {
            labels.add(entry.getKey());
            valuesToLabels.put(entry.getValue(), entry.getKey());
        }
        final SelectionCell cell = new SelectionCell(new ArrayList<>(labels));
        final Column<ChannelModel, String> result = new Column<ChannelModel, String>(cell) {

            @Override
            public String getValue(final ChannelModel object) {
                String result = object.getValue(id);
                if (result == null) {
                    final String defaultValue = param.getDefault();
                    result = defaultValue != null ? defaultValue : labelsToValues.get(labels.get(0));
                    object.setValue(id, result);
                }
                return valuesToLabels.get(result);
            }
        };

        if (!isReadOnly) {
            result.setFieldUpdater(new FieldUpdater<ChannelModel, String>() {

                @Override
                public void update(final int index, final ChannelModel object, final String label) {
                    AssetConfigurationUi.this.setDirty(true);
                    object.setValue(param.getId(), labelsToValues.get(label));
                    AssetConfigurationUi.this.channelTable.redraw();
                }
            });
        }

        return result;
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
                        AssetConfigurationUi.this.newChannelNameInput.getValue().trim());
                if (isChannelNameValid.isInvalid()) {
                    AssetConfigurationUi.this.newChannelNameInput.addStyleName(INVALID_CLASS_NAME);
                    AssetConfigurationUi.this.newChannelNameError.setText(isChannelNameValid.getValue());
                    return;
                }
                AssetConfigurationUi.this.newChannelNameError.setText("");
                AssetConfigurationUi.this.newChannelNameInput.removeStyleName(INVALID_CLASS_NAME);
            }
        });

        this.btnCreateNewChannel.addClickHandler(new ClickHandler() {

            @Override
            public void onClick(final ClickEvent event) {
                final String newChannelName = AssetConfigurationUi.this.newChannelNameInput.getValue().trim();

                ValidationData isChannelNameValid = validateChannelName(newChannelName);
                if (isChannelNameValid.isInvalid()) {
                    return;
                }

                model.createNewChannel(newChannelName);

                channelsDataProvider.setList(model.getChannels());
                AssetConfigurationUi.this.channelsDataProvider.refresh();
                AssetConfigurationUi.this.channelPager.lastPage();
                AssetConfigurationUi.this.setDirty(true);
                AssetConfigurationUi.this.newChannelModal.hide();
            }
        });

        this.btnRemove.addClickHandler(new ClickHandler() {

            @Override
            public void onClick(final ClickEvent event) {
                final ChannelModel ci = AssetConfigurationUi.this.selectionModel.getSelectedObject();
                model.deleteChannel(ci.getChannelName());

                channelsDataProvider.setList(model.getChannels());
                AssetConfigurationUi.this.channelsDataProvider.refresh();
                AssetConfigurationUi.this.btnRemove.setEnabled(false);
                AssetConfigurationUi.this.setDirty(true);
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

        if (model.getChannelNames().contains(channelName)) {
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
        while (model.getChannelNames().contains(result = MSGS.wiresChannel() + suffix)) {
            suffix++;
        }
        return result;
    }

    @Override
    public void setListener(HasConfiguration.Listener listener) {
        this.listener = listener;
        listener.onConfigurationChanged(this);
    }

    public Widget getAssociatedView() {
        return this.associatedView;
    }

    protected void updateNonChannelFields() {
        Iterator<Widget> it = this.fields.iterator();
        while (it.hasNext()) {
            Widget w = it.next();
            if (w instanceof FormGroup) {
                FormGroup fg = (FormGroup) w;
                fillUpdatedConfiguration(fg);
            }
        }
    }

    @Override
    public GwtConfigComponent getConfiguration() {
        updateNonChannelFields();
        return model.getConfiguration();
    }

    @Override
    public boolean isDirty() {
        return this.dirty;
    }

    @Override
    public boolean isValid() {
        return nonValidatedCells.isEmpty() && super.isValid();
    }

    @Override
    public void clearDirtyState() {
        this.dirty = false;
    }

    @Override
    public void markAsDirty() {
        setDirty(true);
    }
}
