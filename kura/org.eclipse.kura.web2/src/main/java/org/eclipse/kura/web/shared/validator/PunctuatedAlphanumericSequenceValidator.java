package org.eclipse.kura.web.shared.validator;

import java.util.function.Consumer;

public class PunctuatedAlphanumericSequenceValidator implements Validator<String> {

    private final char[] delimiters;
    private final String message;

    private static final String ALPHANUMERIC_PATTERN = "[a-zA-Z0-9]+";

    public PunctuatedAlphanumericSequenceValidator(final char[] delimiters, final String message) {
        this.delimiters = delimiters;
        this.message = message;
    }

    private boolean isDelimiter(final char value) {
        for (final char delimiter : delimiters) {
            if (value == delimiter) {
                return true;
            }
        }

        return false;
    }

    private void requireNonEmptyAlphanumericString(final String value) {
        if (!value.matches(ALPHANUMERIC_PATTERN)) {
            throw new IllegalArgumentException(message);
        }
    }

    @Override
    public void validate(String value, Consumer<String> errorMessageConsumer) {
        if (value == null) {
            return;
        }

        try {
            final StringBuilder component = new StringBuilder();

            for (int i = 0; i < value.length(); i++) {
                final char c = value.charAt(i);

                if (isDelimiter(c)) {
                    requireNonEmptyAlphanumericString(component.toString());
                    component.setLength(0);
                } else {
                    component.append(c);
                }
            }

            requireNonEmptyAlphanumericString(component.toString());
        } catch (final Exception e) {
            errorMessageConsumer.accept(this.message);
        }
    }
}