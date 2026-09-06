/*******************************************************************************
 * Copyright (c) 2026 Eurotech and/or its affiliates and others
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
package org.eclipse.kura;

import java.text.MessageFormat;
import java.util.Locale;
import java.util.ResourceBundle;

/**
 * Builds the localized messages of {@link KuraException} and {@link KuraRuntimeException} from the
 * {@link KuraErrorCode} messages bundle.
 */
final class KuraExceptionMessages {

    private static final String MESSAGES_BUNDLE = "org.eclipse.kura.core.messages.KuraExceptionMessagesBundle";

    private static final String GENERIC_MESSAGE_PATTERN = "Generic Error - {0}: {1}";

    private static final String UNKNOWN_CODE = "Unknown";

    private KuraExceptionMessages() {
    }

    static String localizedMessage(final Locale locale, final KuraErrorCode code, final Object[] arguments) {
        final String pattern = messagePattern(locale, code);
        Object[] messageArguments = arguments;
        if ((code == null || KuraErrorCode.INTERNAL_ERROR.equals(code)) && arguments != null && arguments.length > 1) {
            final StringBuilder allArguments = new StringBuilder();
            for (final Object argument : arguments) {
                allArguments.append(" - ").append(argument);
            }
            messageArguments = new Object[] { allArguments.toString() };
        }
        return MessageFormat.format(pattern, messageArguments);
    }

    static String messagePattern(final Locale locale, final KuraErrorCode code) {
        if (code == null) {
            return MessageFormat.format(GENERIC_MESSAGE_PATTERN, UNKNOWN_CODE);
        }
        final ResourceBundle messages = ResourceBundle.getBundle(MESSAGES_BUNDLE, locale);
        if (messages.containsKey(code.name())) {
            return messages.getString(code.name());
        }
        return MessageFormat.format(GENERIC_MESSAGE_PATTERN, code.name());
    }
}
