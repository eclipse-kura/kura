package org.eclipse.kura.locale;

import org.eclipse.kura.utils.Assert;

public class NamedInheritableThreadLocal<T> extends InheritableThreadLocal<T> {

    private final String name;

    /**
     * Create a new NamedInheritableThreadLocal with the given name.
     * 
     * @param name
     *                 a descriptive name for this ThreadLocal
     */
    public NamedInheritableThreadLocal(String name) {
        Assert.hasText(name, "Name must not be empty");
        this.name = name;
    }

    @Override
    public String toString() {
        return this.name;
    }

}