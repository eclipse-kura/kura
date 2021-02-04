/*******************************************************************************
 * Copyright (c) 2011, 2020 Eurotech and/or its affiliates and others
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
/*
 * Render the Content in the Main Panel corresponding to Service (GwtBSConfigComponent) selected in the Services Panel
 *
 * Fields are rendered based on their type (Password(Input), Choice(Dropboxes) etc. with Text fields rendered
 * for both numeric and other textual field with validate() checking if value in numeric fields is numeric
 */
package org.eclipse.kura.web.client.ui;

import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.stream.Collectors;

import org.eclipse.kura.web.client.ui.AlertDialog.ConfirmListener;
import org.eclipse.kura.web.client.util.FailureHandler;
import org.eclipse.kura.web.client.util.request.RequestQueue;
import org.eclipse.kura.web.shared.model.GwtConfigComponent;
import org.eclipse.kura.web.shared.model.GwtConfigParameter;
import org.eclipse.kura.web.shared.model.GwtXSRFToken;
import org.eclipse.kura.web.shared.service.GwtComponentService;
import org.eclipse.kura.web.shared.service.GwtComponentServiceAsync;
import org.eclipse.kura.web.shared.service.GwtSecurityTokenService;
import org.eclipse.kura.web.shared.service.GwtSecurityTokenServiceAsync;
import org.gwtbootstrap3.client.ui.Alert;
import org.gwtbootstrap3.client.ui.AnchorListItem;
import org.gwtbootstrap3.client.ui.Button;
import org.gwtbootstrap3.client.ui.ButtonGroup;
import org.gwtbootstrap3.client.ui.FieldSet;
import org.gwtbootstrap3.client.ui.Form;
import org.gwtbootstrap3.client.ui.FormGroup;
import org.gwtbootstrap3.client.ui.Modal;
import org.gwtbootstrap3.client.ui.ModalBody;
import org.gwtbootstrap3.client.ui.ModalFooter;
import org.gwtbootstrap3.client.ui.ModalHeader;
import org.gwtbootstrap3.client.ui.NavPills;
import org.gwtbootstrap3.client.ui.PanelBody;
import org.gwtbootstrap3.client.ui.TextBox;
import org.gwtbootstrap3.client.ui.constants.ModalBackdrop;
import org.gwtbootstrap3.client.ui.html.Span;
import org.gwtbootstrap3.client.ui.html.Text;

import com.google.gwt.core.client.GWT;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.user.client.Timer;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.Widget;

public class ServicesUi extends AbstractServicesUi {

    private static final ServicesUiUiBinder uiBinder = GWT.create(ServicesUiUiBinder.class);

    interface ServicesUiUiBinder extends UiBinder<Widget, ServicesUi> {
    }

    private final GwtComponentServiceAsync gwtComponentService = GWT.create(GwtComponentService.class);
    private final GwtSecurityTokenServiceAsync gwtXSRFService = GWT.create(GwtSecurityTokenService.class);

    private boolean dirty;
    private boolean initialized;
    private GwtConfigComponent originalConfig;

    private final Optional<Validator> validator;

    NavPills menu;
    PanelBody content;
    AnchorListItem service;
    TextBox validated;
    FormGroup validatedGroup;
    Modal modal;

    @UiField
    Button apply;
    @UiField
    Button reset;
    @UiField
    Button delete;
    @UiField
    FieldSet fields;
    @UiField
    Form form;
    @UiField
    Button deleteButton;
    @UiField
    Button cancelButton;

    @UiField
    Modal incompleteFieldsModal;
    @UiField
    Alert incompleteFields;
    @UiField
    Text incompleteFieldsText;

    @UiField
    Modal deleteModal;
    @UiField
    ModalHeader deleteModalHeader;
    @UiField
    ModalBody deleteModalBody;
    @UiField
    AlertDialog alertDialog;

    private final Optional<Listener> listener;
    private Optional<Consumer<Optional<Throwable>>> onApply = Optional.empty();

    //
    // Public methods
    //
    public ServicesUi(final GwtConfigComponent addedItem, final Optional<Listener> listener,
            final Optional<Validator> validator) {
        initWidget(uiBinder.createAndBindUi(this));
        this.validator = validator;
        this.initialized = false;
        this.listener = listener;
        this.originalConfig = addedItem;
        restoreConfiguration(this.originalConfig);
        this.fields.clear();

        this.apply.setText(MSGS.apply());
        this.apply.addClickHandler(event -> apply());

        this.reset.setText(MSGS.reset());
        this.reset.addClickHandler(event -> reset());

        this.delete.setText(MSGS.delete());
        this.delete.addClickHandler(event -> ServicesUi.this.deleteModal.show());

        this.deleteButton.addClickHandler(event -> delete());
        this.deleteButton.setText(MSGS.yesButton());
        this.cancelButton.setText(MSGS.noButton());
        this.deleteModalBody.add(new Span(MSGS.deleteWarning()));
        this.deleteModalHeader.setTitle(MSGS.confirm());

        renderForm();
        initInvalidDataModal();

        setDirty(false);
        this.apply.setEnabled(false);
        this.reset.setEnabled(false);
        this.delete.setEnabled(this.configurableComponent.isFactoryComponent());
    }

    public ServicesUi(final GwtConfigComponent addedItem, final Optional<Listener> listener) {
        this(addedItem, listener, Optional.empty());
    }

    public ServicesUi(final GwtConfigComponent addedItem) {
        this(addedItem, Optional.empty(), Optional.empty());
    }

    @Override
    public void setDirty(boolean flag) {
        this.dirty = flag;
        if (this.dirty && this.initialized) {
            this.apply.setEnabled(true);
            this.reset.setEnabled(true);
        }
    }

    @Override
    public boolean isDirty() {
        return this.dirty;
    }

    @Override
    public void reset() {
        if (isDirty()) {
            // Modal
            this.modal = new Modal();
            modal.setClosable(false);
            modal.setFade(true);
            modal.setDataKeyboard(true);
            modal.setDataBackdrop(ModalBackdrop.STATIC);

            ModalHeader header = new ModalHeader();
            header.setTitle(MSGS.confirm());
            this.modal.add(header);

            ModalBody body = new ModalBody();
            body.add(new Span(MSGS.deviceConfigDirty()));
            this.modal.add(body);

            ModalFooter footer = new ModalFooter();
            ButtonGroup group = new ButtonGroup();
            Button no = new Button();
            no.setText(MSGS.noButton());
            no.addStyleName("fa fa-times");
            no.addClickHandler(event -> ServicesUi.this.modal.hide());
            group.add(no);
            Button yes = new Button();
            yes.setText(MSGS.yesButton());
            yes.addStyleName("fa fa-check");
            yes.addClickHandler(event -> {
                ServicesUi.this.modal.hide();
                restoreConfiguration(ServicesUi.this.originalConfig);
                renderForm();
                ServicesUi.this.apply.setEnabled(false);
                ServicesUi.this.reset.setEnabled(false);
                setDirty(false);
                logger.info(MSGS.info() + ": " + "Refetching services");
                Timer timer = new Timer() {

                    @Override
                    public void run() {
                        listener.ifPresent(Listener::onConfigurationChanged);
                    }
                };

                timer.schedule(2000);
            });
            group.add(yes);
            footer.add(group);
            this.modal.add(footer);
            this.modal.show();
            no.setFocus(true);
        }
    }

    public void delete() {
        if (this.configurableComponent.isFactoryComponent()) {
            EntryClassUi.showWaitModal();
            this.gwtXSRFService.generateSecurityToken(new AsyncCallback<GwtXSRFToken>() {

                @Override
                public void onFailure(Throwable ex) {
                    EntryClassUi.hideWaitModal();
                    FailureHandler.handle(ex);
                }

                @Override
                public void onSuccess(GwtXSRFToken token) {
                    ServicesUi.this.gwtComponentService.deleteFactoryConfiguration(token,
                            ServicesUi.this.configurableComponent.getComponentId(), true, new AsyncCallback<Void>() {

                                @Override
                                public void onFailure(Throwable caught) {
                                    EntryClassUi.hideWaitModal();
                                    errorLogger.log(Level.SEVERE, caught.getLocalizedMessage());
                                }

                                @Override
                                public void onSuccess(Void result) {
                                    Window.Location.reload();
                                }
                            });
                }
            });
        }
    }

    // TODO: Separate render methods for each type (ex: Boolean, String,
    // Password, etc.). See latest org.eclipse.kura.web code.
    // Iterates through all GwtConfigParameter in the selected
    // GwtConfigComponent
    @Override
    public void renderForm() {
        this.fields.clear();
        for (GwtConfigParameter param : this.configurableComponent.getParameters()) {
            if (param.getCardinality() == 0 || param.getCardinality() == 1 || param.getCardinality() == -1) {
                FormGroup formGroup = new FormGroup();
                renderConfigParameter(param, true, formGroup);
            } else {
                renderMultiFieldConfigParameter(param);
            }
        }
        this.initialized = true;
    }

    @Override
    protected void renderTextField(final GwtConfigParameter param, boolean isFirstInstance, final FormGroup formGroup) {
        super.renderTextField(param, isFirstInstance, formGroup);
        this.fields.add(formGroup);
    }

    @Override
    protected void renderPasswordField(final GwtConfigParameter param, boolean isFirstInstance, FormGroup formGroup) {
        super.renderPasswordField(param, isFirstInstance, formGroup);
        this.fields.add(formGroup);
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

    //
    // Private methods
    //
    private void apply() {
        if (isValid()) {
            if (isDirty()) {
                this.configurableComponent = getUpdatedConfiguration();

                final List<String> messages;

                if (validator.isPresent()) {
                    final List<ValidationResult> result = validator.get().validate(this.configurableComponent);
                    messages = result.stream().map(ValidationResult::getMessage).collect(Collectors.toList());

                    if (result.stream().anyMatch(r -> r instanceof ServicesUi.Error)) {
                        alertDialog.show(MSGS.error(), MSGS.formWithErrorsOrIncomplete(), AlertDialog.Severity.ERROR,
                                null, messages.toArray(new String[messages.size()]));
                        return;
                    }
                } else {
                    messages = Collections.emptyList();
                }

                alertDialog.show(MSGS.confirm(),
                        MSGS.deviceConfigConfirmation(this.configurableComponent.getComponentName()),
                        AlertDialog.Severity.INFO, ok -> {
                            if (ok) {
                                RequestQueue.submit(context -> ServicesUi.this.gwtXSRFService.generateSecurityToken(
                                        context.callback(token -> ServicesUi.this.gwtComponentService
                                                .updateComponentConfiguration(token,
                                                        ServicesUi.this.configurableComponent,
                                                        context.callback(new AsyncCallback<Void>() {

                                                            @Override
                                                            public void onFailure(Throwable caught) {
                                                                FailureHandler.handle(caught);
                                                                errorLogger.log(Level.SEVERE,
                                                                        caught.getLocalizedMessage() != null
                                                                                ? caught.getLocalizedMessage()
                                                                                : caught.getClass().getName(),
                                                                        caught);
                                                                onApply.ifPresent(
                                                                        action -> action.accept(Optional.of(caught)));
                                                            }

                                                            @Override
                                                            public void onSuccess(Void result) {
                                                                logger.info(MSGS.info() + ": "
                                                                        + MSGS.deviceConfigApplied());
                                                                ServicesUi.this.apply.setEnabled(false);
                                                                ServicesUi.this.reset.setEnabled(false);
                                                                setDirty(false);
                                                                ServicesUi.this.originalConfig = ServicesUi.this.configurableComponent;
                                                                context.defer(2000, () -> ServicesUi.this.listener
                                                                        .ifPresent(Listener::onConfigurationChanged));
                                                                onApply.ifPresent(
                                                                        action -> action.accept(Optional.empty()));
                                                            }
                                                        })))));
                            }
                        }, messages.toArray(new String[messages.size()]));
            }

        } else {
            errorLogger.log(Level.SEVERE, "Device configuration error!");
            alertDialog.show(MSGS.warning(), MSGS.formWithErrorsOrIncomplete(), AlertDialog.Severity.ERROR,
                    (ConfirmListener) null);
        }
    }

    private GwtConfigComponent getUpdatedConfiguration() {
        Iterator<Widget> it = this.fields.iterator();
        while (it.hasNext()) {
            Widget w = it.next();
            if (w instanceof FormGroup) {
                FormGroup fg = (FormGroup) w;
                fillUpdatedConfiguration(fg);
            }
        }
        return this.configurableComponent;
    }

    private void initInvalidDataModal() {
        this.incompleteFieldsModal.setTitle(MSGS.warning());
        this.incompleteFieldsText.setText(MSGS.formWithErrorsOrIncomplete());
    }

    public void onApply(final Consumer<Optional<Throwable>> action) {
        this.onApply = Optional.of(action);
    }

    public interface Listener {

        public void onConfigurationChanged();
    }

    public interface ValidationResult {

        public String getMessage();

        public static ValidationResult warning(final String message) {
            return new Warning(message);
        }

        public static ValidationResult error(final String message) {
            return new Error(message);
        }
    }

    public static class Warning implements ValidationResult {

        private final String message;

        private Warning(final String message) {
            this.message = message;
        }

        public String getMessage() {
            return message;
        }
    }

    public static class Error implements ValidationResult {

        private final String message;

        private Error(final String message) {
            this.message = message;
        }

        public String getMessage() {
            return message;
        }
    }

    public interface Validator {

        public List<ValidationResult> validate(final GwtConfigComponent config);
    }
}
