package org.eclipse.kura.locale;

import org.eclipse.kura.utils.Assert;

/**
 * {@link ThreadLocal} subclass that exposes a specified name
 * as {@link #toString()} result (allowing for introspection).
 *
 * @author Juergen Hoeller
 * @since 2.5.2
 * @param <T>
 *                the value type
 * @see NamedInheritableThreadLocal
 */
public class NamedThreadLocal<T> extends ThreadLocal<T> {

    private final String name;

    /**
     * Create a new NamedThreadLocal with the given name.
     * 
     * @param name
     *                 a descriptive name for this ThreadLocal
     */
    public NamedThreadLocal(String name) {
        Assert.hasText(name, "Name must not be empty");
        this.name = name;
    }

    @Override
    public String toString() {
        return this.name;
    }

}