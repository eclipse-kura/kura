package org.eclipse.kura.web.shared.validator;

import java.util.function.Consumer;

public interface Validator<T> {

    void validate(T value, Consumer<String> errorMessageConsumer);
}
