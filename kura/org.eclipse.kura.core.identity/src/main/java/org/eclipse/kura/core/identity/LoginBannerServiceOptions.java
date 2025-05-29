/*******************************************************************************
 * Copyright (c) 2025 Eurotech and/or its affiliates and others
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
package org.eclipse.kura.core.identity;

import org.osgi.service.component.annotations.ComponentPropertyType;
import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;

@ObjectClassDefinition(id = LoginBannerServiceOptions.PID, name = "Login Banner", //
        description = "This component allows to enable and configure pre and post login banners that can be shown by Kura user interfaces.")
@ComponentPropertyType
public @interface LoginBannerServiceOptions {

    public static final String PID = "org.eclipse.kura.identity.LoginBannerService";

    @AttributeDefinition(name = "Pre Login Banner Enabled", //
            description = "If enabled, a customizable banner will be shown before user login.")
    public boolean pre_login_banner_enabled() default true;

    @AttributeDefinition(name = "Pre Login Banner Content", //
            description = "The message to be shown in the pre login banner, if the feature is enabled.|TextArea")
    public String pre_login_banner_content() default "WARNING: This is a secure system. The details of this login attempt have been recorded for future inspection by the system administrator. Log out now if you are not authorized to use this device.";

    @AttributeDefinition(name = "Post Login Banner Enabled", //
            description = "If enabled, a customizable banner will be shown after successful user login.")
    public boolean post_login_banner_enabled() default false;

    @AttributeDefinition(name = "Post Login Banner Content", //
            description = "The message to be shown in the post login banner, if the feature is enabled.|TextArea")
    public String post_login_banner_content() default "Sample Banner Content";

}
