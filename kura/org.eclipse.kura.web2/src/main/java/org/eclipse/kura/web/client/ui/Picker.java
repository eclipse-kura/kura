package org.eclipse.kura.web.client.ui;

import static java.util.Objects.requireNonNull;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.function.BiFunction;
import java.util.function.Consumer;

import org.gwtbootstrap3.client.ui.Button;
import org.gwtbootstrap3.client.ui.FormLabel;
import org.gwtbootstrap3.client.ui.Input;
import org.gwtbootstrap3.client.ui.Modal;
import org.gwtbootstrap3.client.ui.Panel;
import org.gwtbootstrap3.client.ui.base.HasId;
import org.gwtbootstrap3.client.ui.form.error.BasicEditorError;
import org.gwtbootstrap3.client.ui.form.validator.Validator;

import com.google.gwt.core.client.GWT;
import com.google.gwt.editor.client.Editor;
import com.google.gwt.editor.client.EditorError;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.FormPanel;
import com.google.gwt.user.client.ui.Widget;

public class Picker extends Composite implements HasId {

    private static PickerUiBinder uiBinder = GWT.create(PickerUiBinder.class);

    interface PickerUiBinder extends UiBinder<Widget, Picker> {
    }

    @UiField
    FormLabel label;
    @UiField
    FormPanel form;
    @UiField
    Button yes;
    @UiField
    Button no;
    @UiField
    Panel inputPanel;

    private final Modal modal;

    private Optional<Consumer<State<?>>> dismissAction = Optional.empty();
    private Optional<State<?>> state = Optional.empty();

    public Picker() {
        this.modal = (Modal) uiBinder.createAndBindUi(this);
        initWidget(this.modal);

        this.modal.setHideOtherModals(false);

        this.yes.addClickHandler(e -> {
            dismissAction = Optional.of(State::onAccept);
            this.modal.hide();
        });
        this.no.addClickHandler(e -> {
            this.modal.hide();
        });
        this.modal.addHiddenHandler(e -> {
            if (!state.isPresent()) {
                return;
            }

            final State<?> currentState = this.state.get();

            if (dismissAction.isPresent()) {
                final Consumer<State<?>> currentAction = dismissAction.get();
                this.dismissAction = Optional.empty();
                currentAction.accept(currentState);
            }
            if (this.state.isPresent() && this.state.get() == currentState) {
                this.modal.hide();
                this.state = Optional.empty();
            }
        });
    }

    @Override
    public String getId() {
        return this.modal.getId();
    }

    @Override
    public void setId(final String id) {
        this.modal.setId(id);
    }

    public <U> Builder<U> builder(final Class<U> classz) {
        return new Builder<>();
    }

    public class Builder<U> {

        private BiFunction<Editor<String>, String, U> validator;
        private Consumer<U> consumer;
        private String title;

        private Optional<String> message = Optional.empty();
        private Optional<Runnable> onCancel = Optional.empty();
        private Optional<Consumer<Input>> customizer = Optional.empty();

        public Builder<U> setTitle(final String title) {
            this.title = title;
            return this;
        }

        public Builder<U> setValidator(final BiFunction<Editor<String>, String, U> validator) {
            this.validator = validator;
            return this;
        }

        public Builder<U> setOnPick(final Consumer<U> consumer) {
            this.consumer = consumer;
            return this;
        }

        public Builder<U> setMessage(final String message) {
            this.message = Optional.of(message);
            return this;
        }

        public Builder<U> setOnCancel(final Runnable onCancel) {
            this.onCancel = Optional.of(onCancel);
            return this;
        }

        public Builder<U> setInputCustomizer(final Consumer<Input> customizer) {
            this.customizer = Optional.of(customizer);
            return this;
        }

        private Input initInput() {
            final Input result = new Input();
            result.addKeyUpHandler(e -> result.validate());
            if (customizer.isPresent()) {
                customizer.get().accept(result);
            }
            return result;
        }

        public void pick() {
            requireNonNull(validator, "validator cannot be null");
            requireNonNull(consumer, "onPick cannot be null");
            requireNonNull(title, "title cannot be null");

            Picker.this.modal.setTitle(title);

            if (message.isPresent()) {
                Picker.this.label.setText(message.get());
            } else {
                Picker.this.label.setText("");
            }

            final Input input = initInput();

            final State<U> state = new State<>(input, validator, consumer, onCancel.orElse(() -> {
            }));

            Picker.this.state = Optional.of(state);
            Picker.this.dismissAction = Optional.of(State::onCancel);

            inputPanel.add(input);

            input.validate();
            Picker.this.modal.show();
        }
    }

    private class State<U> implements Validator<String> {

        private final Input value;
        private final BiFunction<Editor<String>, String, U> builder;
        private final Consumer<U> consumer;
        private final Runnable onDismiss;
        private final HandlerRegistration submitHandler;
        private final com.google.web.bindery.event.shared.HandlerRegistration shownHandler;

        private Optional<U> currentValue = Optional.empty();

        @SuppressWarnings("unchecked")
        public State(Input value, BiFunction<Editor<String>, String, U> builder, Consumer<U> consumer,
                final Runnable onCancel) {
            this.value = value;
            this.builder = builder;
            this.consumer = consumer;
            this.onDismiss = onCancel;
            this.value.setValidators(this);
            this.submitHandler = form.addSubmitHandler(e -> {
                e.cancel();
                if (value.validate()) {
                    dismissAction = Optional.of(State::onAccept);
                    modal.hide();
                }
            });
            this.shownHandler = modal.addShownHandler(e -> {
                value.setFocus(true);
            });
        }

        public void update(final Editor<String> editor, final String valueString) {
            try {
                currentValue = Optional.of(builder.apply(editor, valueString));
                yes.setEnabled(true);
            } catch (final Exception e) {
                currentValue = Optional.empty();
                yes.setEnabled(false);
                throw e;
            }
        }

        public void onAccept() {
            cleanup();
            if (currentValue.isPresent()) {
                consumer.accept(currentValue.get());
            }
        }

        public void onCancel() {
            cleanup();
            onDismiss.run();
        }

        private void cleanup() {
            inputPanel.clear();
            submitHandler.removeHandler();
            shownHandler.removeHandler();
        }

        @Override
        public int getPriority() {
            return 0;
        }

        @Override
        public List<EditorError> validate(Editor<String> editor, String valueString) {
            try {
                update(editor, valueString);
                return Collections.emptyList();
            } catch (final Exception e) {
                return Collections.singletonList(new BasicEditorError(editor, valueString, e.getMessage()));
            }
        }
    }
}
