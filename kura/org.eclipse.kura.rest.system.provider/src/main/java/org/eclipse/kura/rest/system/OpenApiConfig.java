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
package org.eclipse.kura.rest.system;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.info.Contact;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.info.License;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import io.swagger.v3.oas.annotations.servers.Server;
import io.swagger.v3.oas.annotations.tags.Tag;

/**
 * OpenAPI configuration for Eclipse Kura System REST API.
 * This class defines the overall API specification metadata, security schemes,
 * and server information for the System API.
 */
@SuppressWarnings("all")
@OpenAPIDefinition(info = @Info(title = "Eclipse Kura System REST API", version = "1.0.0", description = "REST API for Eclipse Kura system properties and information management. "
        + "This API provides access to hardware, software, and framework properties "
        + "of the Eclipse Kura IoT gateway platform.", contact = @Contact(name = "Eclipse Kura Project", url = "https://www.eclipse.org/kura/", email = "kura-dev@eclipse.org"), license = @License(name = "Eclipse Public License 2.0", url = "https://www.eclipse.org/legal/epl-2.0/"), termsOfService = "https://www.eclipse.org/legal/epl-2.0/"), servers = {
                @Server(url = "https://localhost:8080/services", description = "Local Kura Gateway (HTTPS)"),
                @Server(url = "http://localhost:8080/services", description = "Local Kura Gateway (HTTP)"),
                @Server(url = "https://{gateway-ip}:8080/services", description = "Remote Kura Gateway") }, tags = {
                        @Tag(name = "System", description = "System information and properties operations"),
                        @Tag(name = "Framework", description = "Framework-level system properties including hardware, Java, and OS information"),
                        @Tag(name = "Extended", description = "Extended system properties and metadata"),
                        @Tag(name = "Kura", description = "Kura-specific configuration and runtime properties") })
@SecurityScheme(name = "basicAuth", type = SecuritySchemeType.HTTP, scheme = "basic", description = "Basic HTTP authentication. Use your Kura username and password. "
        + "Default credentials are admin:admin for development environments.")
public class OpenApiConfig {

    /**
     * Private constructor to prevent instantiation of this configuration class.
     */
    private OpenApiConfig() {
        // Configuration class - no instances needed
    }

    // API Version Constants
    public static final String API_VERSION = "1.0.0";
    public static final String API_TITLE = "Eclipse Kura System REST API";

    // Common HTTP Status Codes for Documentation
    public static final String HTTP_200 = "200";
    public static final String HTTP_400 = "400";
    public static final String HTTP_401 = "401";
    public static final String HTTP_403 = "403";
    public static final String HTTP_404 = "404";
    public static final String HTTP_500 = "500";

    // Common Response Descriptions
    public static final String SUCCESS_RESPONSE = "Operation completed successfully";
    public static final String BAD_REQUEST_RESPONSE = "Bad request - Invalid parameters";
    public static final String UNAUTHORIZED_RESPONSE = "Unauthorized - Authentication required";
    public static final String FORBIDDEN_RESPONSE = "Forbidden - Insufficient permissions";
    public static final String NOT_FOUND_RESPONSE = "Resource not found";
    public static final String SERVER_ERROR_RESPONSE = "Internal server error";
}
