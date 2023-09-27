package org.eclipse.kura.internal.rest.identity.provider.validator;

import java.util.function.Consumer;

public interface Validator<T> {

    void validate(T value, Consumer<String> errorMessageConsumer);
}
