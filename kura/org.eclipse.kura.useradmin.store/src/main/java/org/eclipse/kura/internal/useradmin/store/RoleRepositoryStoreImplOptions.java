/*******************************************************************************
 * Copyright (c) 2011, 2026 Eurotech and/or its affiliates and others
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *  Eurotech
 ******************************************************************************/
package org.eclipse.kura.internal.useradmin.store;

import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;
import org.osgi.service.metatype.annotations.Icon;

@ObjectClassDefinition(id = "org.eclipse.kura.internal.useradmin.store.RoleRepositoryStoreImpl", name = "UserAdmin Store", description = "This component provides snapshot-based persistence to the OSGi UserAdmin service.", icon = @Icon(resource = "DenaliService", size = 32))
public @interface RoleRepositoryStoreImplOptions {

    @AttributeDefinition(name = "Role configuration", description = "The currently defined UserAdmin Roles.")
    String roles_config() default "[]";

    @AttributeDefinition(name = "User configuration", description = "The currently defined UserAdmin Users.")
    String users_config() default "[]";

    @AttributeDefinition(name = "Group configuration", description = "The currently defined UserAdmin Groups.")
    String groups_config() default "[]";

    @AttributeDefinition(name = "Write Delay (milliseconds)", description = "This service defers the snapshot updates required for persisting changes made through UserAdmin APIs. The snapshot update is performed Write Delay milliseconds after the last change performed through UserAdmin APIs.")
    long write_delay_ms() default 5000L;

}


