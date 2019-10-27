package org.eclipse.kura.locale;

import java.util.Locale;
import java.util.TimeZone;

import org.eclipse.kura.annotation.Nullable;

/**
 * Simple implementation of the {@link TimeZoneAwareLocaleContext} interface,
 * always returning a specified {@code Locale} and {@code TimeZone}.
 *
 * <p>
 * Note: Prefer the use of {@link SimpleLocaleContext} when only setting
 * a Locale but no TimeZone.
 *
 * @author Juergen Hoeller
 * @author Nicholas Williams
 * @since 4.0
 * @see LocaleContextHolder#setLocaleContext
 * @see LocaleContextHolder#getTimeZone()
 */
public class SimpleTimeZoneAwareLocaleContext extends SimpleLocaleContext implements TimeZoneAwareLocaleContext {

    @Nullable
    private final TimeZone timeZone;

    /**
     * Create a new SimpleTimeZoneAwareLocaleContext that exposes the specified
     * Locale and TimeZone. Every {@link #getLocale()} call will return the given
     * Locale, and every {@link #getTimeZone()} call will return the given TimeZone.
     * 
     * @param locale
     *                     the Locale to expose
     * @param timeZone
     *                     the TimeZone to expose
     */
    public SimpleTimeZoneAwareLocaleContext(@Nullable Locale locale, @Nullable TimeZone timeZone) {
        super(locale);
        this.timeZone = timeZone;
    }

    @Override
    @Nullable
    public TimeZone getTimeZone() {
        return this.timeZone;
    }

    @Override
    public String toString() {
        return super.toString() + " " + (this.timeZone != null ? this.timeZone.toString() : "-");
    }

}