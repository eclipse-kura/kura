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
package org.eclipse.kura.configuration.change.manager;

import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;
import org.osgi.service.metatype.annotations.Icon;

@ObjectClassDefinition(id = "org.eclipse.kura.configuration.change.manager.ConfigurationChangeManager", name = "Configuration Change Manager", description = "Detect changes to the configuration service and publish them to the cloud as a KuraMessage.", icon = @Icon(resource = "OSGI-INF/configurationChangeManagerLogo.png", size = 32))
public @interface ConfigurationChangeManagerMetatype {

    @AttributeDefinition(name = "Enable", description = "Set to true to enable this component.")
    boolean enabled() default false;

    @AttributeDefinition(name = "CloudPublisher Target Filter", description = "Specifies, as an OSGi target filter, the pid of the Cloud Publisher used to publish messages             to the cloud platform.")
    String CloudPublisher_target() default "(kura.service.pid=changeme)";

    @AttributeDefinition(name = "Notification send delay (sec)", min = "0", description = "Delay before notifications are sent. A large delay accumulates notifications into a single message.    A delay of 0 will send the notifications as soon as they arrive, without accumulation.")
    long send_delay() default 10L;

}


