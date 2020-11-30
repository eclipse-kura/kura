package org.eclipse.kura.web.shared;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

public final class KuraPermission {

    public static final String ADMIN = "kura.admin";
    public static final String NETWORK_ADMIN = "kura.network.admin";
    public static final String PACKAGES_ADMIN = "kura.packages.admin";
    public static final String DEVICE = "kura.device";
    public static final String CLOUD_CONNECTION_ADMIN = "kura.cloud.connection.admin";
    public static final String WIRES_ADMIN = "kura.wires.admin";

    public static final Set<String> DEFAULT_PERMISSIONS = Collections.unmodifiableSet(new HashSet<>(
            Arrays.asList(ADMIN, NETWORK_ADMIN, PACKAGES_ADMIN, DEVICE, CLOUD_CONNECTION_ADMIN, WIRES_ADMIN)));

    private KuraPermission() {
    }

}
