package org.eclipse.kura.core.system.test;

import java.util.Arrays;
import java.util.List;

import org.eclipse.kura.system.ExtendedPropertiesHolder;

/**
 * Mock implementation of the {@link ExtendedPropertiesHolder} interface, returning static data which can be accessed by
 * public class constants.
 */
public class MockExtendedPropertiesHolder implements ExtendedPropertiesHolder {

    public static final String GROUP1 = "test-group1";
    public static final String GROUP2 = "test-group2";
    public static final String PROPERTY1 = "property1";
    public static final String PROPERTY2 = "property2";
    public static final String VALUE1 = "value1";
    public static final String VALUE2 = "value2";

    private static final Property G1_P1 = new Property(GROUP1, PROPERTY1, VALUE1);
    private static final Property G1_P2 = new Property(GROUP1, PROPERTY2, VALUE2);
    private static final Property G2_P1 = new Property(GROUP2, PROPERTY1, VALUE1);

    public MockExtendedPropertiesHolder() {
    }

    @Override
    public List<Property> getProperties() {
        return Arrays.asList(G1_P1, G1_P2, G2_P1);
    }

}
