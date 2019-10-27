package org.eclipse.kura.locale;

import java.util.Locale;

import org.eclipse.kura.annotation.Nullable;

public interface LocaleContext {

    /**
     * Return the current Locale, which can be fixed or determined dynamically,
     * depending on the implementation strategy.
     * 
     * @return the current Locale, or {@code null} if no specific Locale associated
     */
    @Nullable
    Locale getLocale();

}