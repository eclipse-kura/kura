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

import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BooleanSupplier;
import java.util.function.Supplier;

import org.eclipse.kura.configuration.ConfigurableComponent;
import org.eclipse.kura.identity.LoginBannerService;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Modified;
import org.osgi.service.metatype.annotations.Designate;

@Component(immediate = true, name = LoginBannerServiceOptions.PID, //
        configurationPolicy = ConfigurationPolicy.REQUIRE, property = "kura.ui.service.hide:Boolean=true")
@Designate(ocd = LoginBannerServiceOptions.class)
public class LoginBannerServiceImpl implements LoginBannerService, ConfigurableComponent {

    private final AtomicReference<LoginBannerServiceOptions> options;

    @Activate
    public LoginBannerServiceImpl(final LoginBannerServiceOptions options) {
        this.options = new AtomicReference<>(options);
    }

    @Modified
    public void updated(final LoginBannerServiceOptions options) {
        this.options.set(options);
    }

    @Override
    public Optional<String> getPreLoginBanner() {
        final LoginBannerServiceOptions currentOptions = this.options.get();

        return getMessage(currentOptions::pre_login_banner_enabled, currentOptions::pre_login_banner_content);
    }

    @Override
    public Optional<String> getPostLoginBanner() {
        final LoginBannerServiceOptions currentOptions = this.options.get();

        return getMessage(currentOptions::post_login_banner_enabled, currentOptions::post_login_banner_content);
    }

    private static final Optional<String> getMessage(final BooleanSupplier enabled, final Supplier<String> message) {
        if (enabled.getAsBoolean()) {
            return Optional.ofNullable(message.get()).map(String::trim).filter(s -> !s.isEmpty());
        } else {
            return Optional.empty();
        }
    }

}
