/*******************************************************************************
 * Copyright (c) 2024 Eurotech and/or its affiliates and others
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *  Eurotech
 *******************************************************************************/
package org.eclipse.kura.internal.rest.identity.provider.test;

import static org.junit.Assert.fail;
import static org.mockito.Mockito.mock;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.eclipse.kura.KuraErrorCode;
import org.eclipse.kura.KuraException;
import org.eclipse.kura.configuration.ComponentConfiguration;
import org.eclipse.kura.configuration.metatype.OCD;
import org.eclipse.kura.core.testutil.requesthandler.AbstractRequestHandlerTest;
import org.eclipse.kura.core.testutil.requesthandler.MqttTransport;
import org.eclipse.kura.core.testutil.requesthandler.RestTransport;
import org.eclipse.kura.core.testutil.requesthandler.Transport;
import org.eclipse.kura.core.testutil.requesthandler.Transport.MethodSpec;
import org.eclipse.kura.core.testutil.service.ServiceUtil;
import org.eclipse.kura.identity.AssignedPermissions;
import org.eclipse.kura.identity.IdentityConfiguration;
import org.eclipse.kura.identity.IdentityService;
import org.eclipse.kura.identity.PasswordStrengthRequirements;
import org.eclipse.kura.identity.PasswordStrengthVerificationService;
import org.eclipse.kura.identity.Permission;
import org.eclipse.kura.identity.configuration.extension.IdentityConfigurationExtension;
import org.eclipse.kura.internal.rest.identity.provider.util.IdentityDTOUtils;
import org.eclipse.kura.internal.rest.identity.provider.v2.dto.AdditionalConfigurationsDTO;
import org.eclipse.kura.internal.rest.identity.provider.v2.dto.IdentityConfigurationDTO;
import org.eclipse.kura.internal.rest.identity.provider.v2.dto.IdentityConfigurationRequestDTO;
import org.eclipse.kura.internal.rest.identity.provider.v2.dto.IdentityDTO;
import org.eclipse.kura.internal.rest.identity.provider.v2.dto.PasswordConfigurationDTO;
import org.eclipse.kura.internal.rest.identity.provider.v2.dto.PermissionConfigurationDTO;
import org.eclipse.kura.internal.rest.identity.provider.v2.dto.PermissionDTO;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.mockito.ArgumentMatchers;
import org.mockito.Mockito;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.component.runtime.ServiceComponentRuntime;
import org.osgi.service.component.runtime.dto.ComponentDescriptionDTO;
import org.osgi.util.promise.Promise;

import com.google.gson.Gson;

@RunWith(Parameterized.class)
public class IdentityV2EndpointsTest extends AbstractRequestHandlerTest {

    private static final String MQTT_APP_ID = "IDN-V2";
    private static final String REST_APP_ID = "identity/v2";

    private static final String METHOD_SPEC_GET = "GET";
    private static final String METHOD_SPEC_POST = "POST";
    private static final String METHOD_SPEC_DELETE = "DELETE";
    private static final String MQTT_METHOD_SPEC_DEL = "DEL";
    private static final String METHOD_SPEC_PUT = "PUT";
    private static IdentityService identityService;

    private static final int HTTP_STATUS_CONFLICT = 409;

    private Gson gson = new Gson();

    private IdentityDTO identity;
    private IdentityConfigurationRequestDTO identityConfigurationRequestDTO;
    private String testUsername;
    private String testPermissionName;

    private IdentityConfigurationDTO identityConfiguration;
    private PasswordConfigurationDTO passwordConfiguration;
    private AdditionalConfigurationsDTO additionalConfigurations;
    private PermissionConfigurationDTO permissionConfiguration;

    private static PasswordStrengthVerificationService passwordStrengthVerificationServiceMock = mock(
            PasswordStrengthVerificationService.class);

    private final Map<String, MockExtensionHolder> extensions = new HashMap<>();

    @Parameterized.Parameters(name = "{0} - {1}")
    public static Collection<?> transports() {
        return Arrays.asList(new Object[][] { //
                { new MqttTransport(MQTT_APP_ID), "test_user_for_mqtt", "test.permission.for.mqtt" }, //
                { new RestTransport(REST_APP_ID), "test_user_for_rest", "test.permission.for.rest" } });
    }

    @Test
    public void shouldInvokeCreateIdentitySuccessfully() throws KuraException {
        givenNewIdentity(new IdentityDTO(this.testUsername));

        whenRequestIsPerformed(new MethodSpec(METHOD_SPEC_POST), "/identities", gson.toJson(this.identity));

        thenRequestSucceeds();
        thenResponseBodyIsEmpty();
    }

    @Test
    public void shouldReturnErrorIfIdentityAlreadyExists() throws KuraException {
        givenExistingIdentity(new IdentityDTO(this.testUsername));

        givenNewIdentity(new IdentityDTO(this.testUsername));

        whenRequestIsPerformed(new MethodSpec(METHOD_SPEC_POST), "/identities", gson.toJson(this.identity));

        thenResponseCodeIs(HTTP_STATUS_CONFLICT);
        thenResponseBodyIsNotEmpty();
    }

    @Test
    public void shouldUpdateIdentityWithConfiguration() throws KuraException {
        givenExistingIdentity(new IdentityDTO(this.testUsername));
        givenExistingPermission(new PermissionDTO(this.testPermissionName));
        givenMockIdentityConfigurationExtension("test.extension");
        givenMockIdentityConfigurationExtension("test2.extension");

        givenPermissionConfiguration(this.testPermissionName);
        givenPasswordConfiguration("abcdef1234567", true, false);
        givenAdditionalConfigurations(TestComponentConfiguration.forPid("test.extension"),
                TestComponentConfiguration.forPid("test2.extension"));

        givenIdentityConfigurationFor(new IdentityDTO(this.testUsername));

        whenRequestIsPerformed(new MethodSpec(METHOD_SPEC_PUT), "/identities", gson.toJson(this.identityConfiguration));

        thenRequestSucceeds();
        thenResponseBodyIsEmpty();
    }

    @Test
    public void shouldGetIdentityByName() {
        givenExistingIdentity(new IdentityDTO(this.testUsername));
        givenExistingPermission(new PermissionDTO(this.testPermissionName));
        givenAssignedPermissionToIdentity();

        givenIdentityConfigurationRequestDTO();

        whenRequestIsPerformed(new MethodSpec(METHOD_SPEC_POST), "/identities/byName",
                gson.toJson(this.identityConfigurationRequestDTO));

        thenRequestSucceeds();
        thenResponseBodyEqualsJson("{\"identity\":{\"name\":\"" + this.testUsername
                + "\"},\"permissionConfiguration\":{\"permissions\":[{\"name\":\"" + this.testPermissionName
                + "\"}]},\"passwordConfiguration\":{\"passwordChangeNeeded\":false,\"passwordAuthEnabled\":false},\"additionalConfigurations\":{\"configurations\":[]}}");
    }

    @Test
    public void shouldGetIdentityDefaultByName() {
        givenExistingIdentity(new IdentityDTO(this.testUsername));
        givenExistingPermission(new PermissionDTO(this.testPermissionName));
        givenIdentityConfigurationRequestDTO();

        whenRequestIsPerformed(new MethodSpec(METHOD_SPEC_POST), "/identities/default/byName",
                gson.toJson(this.identityConfigurationRequestDTO));

        thenRequestSucceeds();
        thenResponseBodyEqualsJson("{\"identity\":{\"name\":\"" + this.testUsername
                + "\"},\"permissionConfiguration\":{\"permissions\":[]},\"passwordConfiguration\":{\"passwordChangeNeeded\":false,\"passwordAuthEnabled\":false},\"additionalConfigurations\":{\"configurations\":[]}}");
    }

    @Test
    public void shouldDeleteExistingIdentity() {
        givenExistingIdentity(new IdentityDTO(this.testUsername));

        whenRequestIsPerformed(new MethodSpec(METHOD_SPEC_DELETE, MQTT_METHOD_SPEC_DEL), "/identities",
                gson.toJson(new IdentityDTO(this.testUsername)));

        thenRequestSucceeds();
        thenResponseBodyIsEmpty();

    }

    @Test
    public void shouldReturnErrorDeletingNonExistingIdentity() {

        whenRequestIsPerformed(new MethodSpec(METHOD_SPEC_DELETE, MQTT_METHOD_SPEC_DEL), "/identities",
                gson.toJson(new IdentityDTO(this.testUsername)));

        thenResponseCodeIs(404);
        thenResponseBodyEqualsJson("{\"message\":\"Identity not found\"}");

    }

    @Test
    public void shouldReturnErrorDeletingWithMalformedIdentityRequest() {

        whenRequestIsPerformed(new MethodSpec(METHOD_SPEC_DELETE, MQTT_METHOD_SPEC_DEL), "/identities",
                "{\"nm\":\"identity\"}");

        thenResponseCodeIs(400);
        thenResponseBodyEqualsJson("{\"message\":\"Missing 'name' property\"}");

    }

    @Test
    public void shouldGetDefinedPermissions() {
        whenRequestIsPerformed(new MethodSpec(METHOD_SPEC_GET), "/definedPermissions");

        thenRequestSucceeds();
        thenResponseBodyIsNotEmpty();

    }

    @Test
    public void shouldGetIdentities() {
        whenRequestIsPerformed(new MethodSpec(METHOD_SPEC_GET), "/identities");

        thenRequestSucceeds();
        thenResponseBodyIsNotEmpty();

    }

    @Test
    public void shouldGetPasswordStrenghtRequirements() {

        givenPasswordStrenghtServiceMockConfiguration(8, false, false, false);

        whenRequestIsPerformed(new MethodSpec(METHOD_SPEC_GET), "/passwordStrenghtRequirements");

        thenRequestSucceeds();
        thenResponseBodyIsNotEmpty();

    }

    @Test
    public void shouldCreatePermission() {
        whenRequestIsPerformed(new MethodSpec(METHOD_SPEC_POST), "/permissions",
                gson.toJson(new PermissionDTO(testPermissionName)));

        thenRequestSucceeds();
        thenResponseBodyIsEmpty();

    }

    @Test
    public void shouldDeleteExistingPermission() {
        givenExistingPermission(new PermissionDTO(this.testPermissionName));

        whenRequestIsPerformed(new MethodSpec(METHOD_SPEC_DELETE, MQTT_METHOD_SPEC_DEL), "/permissions",
                gson.toJson(new PermissionDTO(this.testPermissionName)));

        thenRequestSucceeds();
        thenResponseBodyIsEmpty();

    }

    @Test
    public void shouldReturnErrorDeletingNonExistingPermission() {

        whenRequestIsPerformed(new MethodSpec(METHOD_SPEC_DELETE, MQTT_METHOD_SPEC_DEL), "/permissions",
                gson.toJson(new PermissionDTO(this.testPermissionName)));

        thenResponseCodeIs(404);
        thenResponseBodyEqualsJson("{\"message\":\"Permission not found\"}");

    }

    @Test
    public void shouldReturnErrorDeletingWithMalformedPermissionRequest() {

        whenRequestIsPerformed(new MethodSpec(METHOD_SPEC_DELETE, MQTT_METHOD_SPEC_DEL), "/permissions",
                "{\"nm\":\"permission\"}");

        thenResponseCodeIs(400);
        thenResponseBodyEqualsJson("{\"message\":\"Missing 'name' property\"}");

    }

    @Test
    public void shouldValidateIdentityConfiguration() {
        givenExistingIdentity(new IdentityDTO(this.testUsername));

        whenRequestIsPerformed(new MethodSpec(METHOD_SPEC_POST), "/identities/validate",
                gson.toJson(new IdentityConfigurationDTO(new IdentityDTO(this.testUsername))));

        thenRequestSucceeds();
    }

    @BeforeClass
    public static void setup() {
        try {
            final ServiceComponentRuntime scr = ServiceUtil
                    .trackService(ServiceComponentRuntime.class, Optional.empty()).get(30, TimeUnit.SECONDS);

            disableComponent(scr, "org.eclipse.kura.core.identity.PasswordStrengthVerificationServiceImpl");

            FrameworkUtil.getBundle(IdentityV2EndpointsTest.class).getBundleContext().registerService(
                    PasswordStrengthVerificationService.class, passwordStrengthVerificationServiceMock,
                    new Hashtable<>());

            identityService = ServiceUtil.trackService(IdentityService.class, Optional.empty()).get(30,
                    TimeUnit.SECONDS);

        } catch (InterruptedException | ExecutionException | TimeoutException e) {
            e.printStackTrace();
            fail("Unable to track IdentityService");
        }
    }

    @Before
    public void cleanUp() {
        try {
            identityService.deleteIdentity(this.testUsername);
            identityService.deletePermission(new Permission(this.testPermissionName));
        } catch (KuraException e) {
            e.printStackTrace();
            fail("unable to clean the Identities repository");
        }
    }

    @After
    public void cleanup() {
        for (final MockExtensionHolder reg : this.extensions.values()) {
            reg.registration.unregister();
        }
    }

    public IdentityV2EndpointsTest(Transport transport, String testUsername, String testPermissionName) {
        super(transport);
        this.testUsername = testUsername;
        this.testPermissionName = testPermissionName;
    }

    private void givenAdditionalConfigurations(ComponentConfiguration... componentConfigurations) {

        this.additionalConfigurations = new AdditionalConfigurationsDTO();
        this.additionalConfigurations.setConfigurations(Stream.of(componentConfigurations)
                .map(IdentityDTOUtils::fromComponentConfiguration).collect(Collectors.toSet()));

    }

    private void givenPermissionConfiguration(String... permissionNames) {
        this.permissionConfiguration = new PermissionConfigurationDTO();
        this.permissionConfiguration.setPermissions(new HashSet<PermissionDTO>(
                Stream.of(permissionNames).map(PermissionDTO::new).collect(Collectors.toList())));

    }

    private void givenExistingPermission(PermissionDTO permissionDTO) {
        try {
            identityService.createPermission(IdentityDTOUtils.toPermission(permissionDTO));
        } catch (KuraException e) {
            e.printStackTrace();
            fail("unable to create the permission");
        }
    }

    private void givenPasswordConfiguration(String password, boolean passwordAuthEnabled,
            boolean passwordChangeEnabled) {
        this.passwordConfiguration = new PasswordConfigurationDTO();

        this.passwordConfiguration.setPassword(password);
        this.passwordConfiguration.setPasswordAuthEnabled(passwordAuthEnabled);
        this.passwordConfiguration.setPasswordChangeNeeded(passwordChangeEnabled);
    }

    private void givenIdentityConfigurationFor(IdentityDTO identityDTO) {
        this.identityConfiguration = new IdentityConfigurationDTO(identityDTO);

        this.identityConfiguration.setAdditionalConfigurations(this.additionalConfigurations);
        this.identityConfiguration.setPasswordConfiguration(this.passwordConfiguration);
        this.identityConfiguration.setPermissionConfiguration(this.permissionConfiguration);
    }

    private void givenNewIdentity(IdentityDTO identity) {
        this.identity = identity;
    }

    private void givenExistingIdentity(IdentityDTO identity) {
        try {
            identityService.createIdentity(identity.getName());
        } catch (KuraException e) {
            e.printStackTrace();
            fail("unable to create the identity");
        }
    }

    private void givenMockIdentityConfigurationExtension(final String pid) {
        this.extensions.put(pid, new MockExtensionHolder(pid));
    }

    private void givenIdentityConfigurationRequestDTO() {
        this.identityConfigurationRequestDTO = new IdentityConfigurationRequestDTO();
        this.identityConfigurationRequestDTO.setIdentity(new IdentityDTO(testUsername));
        this.identityConfigurationRequestDTO.setConfigurationComponents(new HashSet<String>(
                Arrays.asList("AdditionalConfigurations", "AssignedPermissions", "PasswordConfiguration")));
    }

    private void givenPasswordStrenghtServiceMockConfiguration(int passwordMinimumLength, boolean digitsRequired,
            boolean specialCharactersRequired, boolean bothCasesRequired) {

        try {
            Mockito.when(passwordStrengthVerificationServiceMock.getPasswordStrengthRequirements())
                    .thenReturn(new PasswordStrengthRequirements(passwordMinimumLength, digitsRequired,
                            specialCharactersRequired, bothCasesRequired));
        } catch (Exception ex) {
            ex.printStackTrace();
            fail("Unable to configure password strenght requirements");
        }
    }

    private void givenAssignedPermissionToIdentity() {
        try {
            Set<Permission> permissions = new HashSet<>(Arrays.asList(new Permission(this.testPermissionName)));
            identityService.updateIdentityConfiguration(
                    new IdentityConfiguration(this.testUsername, Arrays.asList(new AssignedPermissions(permissions))));
        } catch (Exception ex) {
            ex.printStackTrace();
            fail("Unable to configure identity permissions");
        }
    }

    private static Future<?> disableComponent(final ServiceComponentRuntime scr, final String componentName) {
        return withComponent(scr, componentName, scr::disableComponent);
    }

    private static Future<?> withComponent(final ServiceComponentRuntime scr, final String componentName,
            final Function<ComponentDescriptionDTO, Promise<?>> func) {
        final Optional<ComponentDescriptionDTO> component = scr.getComponentDescriptionDTOs().stream()
                .filter(d -> Objects.equals(componentName, d.name)).findAny();

        final CompletableFuture<Void> result = new CompletableFuture<>();

        if (component.isPresent()) {
            func.apply(component.get()).onResolve(() -> result.complete(null));
        } else {
            result.complete(null);
        }

        return result;
    }

    private static class MockExtensionHolder {

        private final ServiceRegistration<IdentityConfigurationExtension> registration;
        private final IdentityConfigurationExtension extension;

        private final Map<String, ComponentConfiguration> defaultConfigurations = new HashMap<>();
        private final Map<String, ComponentConfiguration> configurations = new HashMap<>();

        public MockExtensionHolder(final String pid) {
            this.extension = Mockito.mock(IdentityConfigurationExtension.class);

            try {
                Mockito.when(this.extension.getConfiguration(ArgumentMatchers.anyString())).thenAnswer(a -> {
                    final String name = a.getArgument(0, String.class);

                    return Optional.ofNullable(this.configurations.get(name));
                });

                Mockito.when(this.extension.getDefaultConfiguration(ArgumentMatchers.anyString())).thenAnswer(a -> {
                    final String name = a.getArgument(0, String.class);

                    return Optional.ofNullable(this.defaultConfigurations.get(name));
                });

                Mockito.doAnswer(i -> {
                    final String identityName = i.getArgument(0, String.class);
                    final ComponentConfiguration connfig = i.getArgument(1, ComponentConfiguration.class);

                    this.configurations.put(identityName, connfig);

                    return (Void) null;
                }).when(this.extension).updateConfiguration(ArgumentMatchers.anyString(), ArgumentMatchers.any());

            } catch (KuraException e) {
                // no need
            }

            final Dictionary<String, Object> properties = new Hashtable<>();
            properties.put("kura.service.pid", pid);

            this.registration = FrameworkUtil.getBundle(IdentityV2EndpointsTest.class).getBundleContext()
                    .registerService(IdentityConfigurationExtension.class, this.extension, properties);
        }

        void throwOnIdentityValidation(final String identityName) {
            try {
                Mockito.doThrow(new KuraException(KuraErrorCode.INVALID_PARAMETER)).when(this.extension)
                        .validateConfiguration(ArgumentMatchers.eq(identityName), ArgumentMatchers.any());
            } catch (KuraException e) {
                // no need
            }
        }
    }

    private static class TestComponentConfiguration implements ComponentConfiguration {

        private final String pid;
        private final OCD ocd;
        private final Map<String, Object> properties;

        public TestComponentConfiguration(String pid, OCD ocd, Map<String, Object> properties) {
            this.pid = pid;
            this.ocd = ocd;
            this.properties = properties;
        }

        @Override
        public String getPid() {
            return this.pid;
        }

        @Override
        public OCD getDefinition() {
            return this.ocd;
        }

        @Override
        public Map<String, Object> getConfigurationProperties() {
            return this.properties;
        }

        @Override
        public int hashCode() {
            return Objects.hash(this.ocd, this.pid, this.properties);
        }

        static TestComponentConfiguration forPid(final String pid) {
            return new TestComponentConfiguration(pid, null, Collections.singletonMap("foo", "bar"));
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (obj == null || getClass() != obj.getClass()) {
                return false;
            }
            TestComponentConfiguration other = (TestComponentConfiguration) obj;
            return Objects.equals(this.ocd, other.ocd) && Objects.equals(this.pid, other.pid)
                    && Objects.equals(this.properties, other.properties);
        }

        @Override
        public String toString() {
            return "TestComponentConfiguration [pid=" + this.pid + ", ocd=" + this.ocd + ", properties="
                    + this.properties + "]";
        }

    }

}
