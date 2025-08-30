# Eclipse Kura System REST API - OpenAPI Integration

## Overview

This document describes the OpenAPI 3.0 integration for the Eclipse Kura System REST API. The integration provides comprehensive API documentation, interactive testing capabilities, and client SDK generation support.

## Features

- **Comprehensive Documentation**: Detailed API documentation with parameter descriptions, examples, and response schemas
- **Interactive Testing**: Swagger UI integration for testing endpoints directly from the browser
- **Client SDK Generation**: Support for generating client libraries in multiple programming languages
- **Security Documentation**: Clear documentation of authentication requirements and permissions
- **Request/Response Examples**: Real-world examples for all API endpoints

## API Endpoints

### Framework Properties

#### GET /system/v1/properties/framework

Retrieves comprehensive system framework properties including hardware, Java runtime, OS, and Kura-specific information.

**Example Request:**
```bash
curl -X GET \
  https://localhost:8080/services/system/v1/properties/framework \
  -H 'Authorization: Basic YWRtaW46YWRtaW4=' \
  -H 'Accept: application/json'
```

**Example Response:**
```json
{
  "biosVersion": "1.2.3",
  "cpuVersion": "Intel Core i7",
  "deviceName": "Kura-Gateway",
  "javaVersion": "17.0.1",
  "osName": "Linux",
  "osVersion": "5.4.0",
  "kuraVersion": "6.0.0-SNAPSHOT",
  "totalMemory": 8589934592,
  "freeMemory": 4294967296
}
```

#### POST /system/v1/properties/framework/filter

Retrieves filtered system framework properties based on specified criteria.

**Example Request:**
```bash
curl -X POST \
  https://localhost:8080/services/system/v1/properties/framework/filter \
  -H 'Authorization: Basic YWRtaW46YWRtaW4=' \
  -H 'Content-Type: application/json' \
  -H 'Accept: application/json' \
  -d '{
    "names": ["javaVersion", "osName", "kuraVersion", "totalMemory"]
  }'
```

**Example Response:**
```json
{
  "javaVersion": "17.0.1",
  "osName": "Linux",
  "kuraVersion": "6.0.0-SNAPSHOT",
  "totalMemory": 8589934592
}
```

### Extended Properties

#### GET /system/v1/properties/extended

Retrieves extended system properties and metadata.

#### POST /system/v1/properties/extended/filter

Retrieves filtered extended properties using group-based filtering.

### Kura Properties  

#### GET /system/v1/properties/kura

Retrieves Kura-specific configuration and runtime properties.

#### POST /system/v1/properties/kura/filter

Retrieves filtered Kura properties based on specified names.

## Authentication

All endpoints require Basic HTTP Authentication. The API uses role-based access control with the `system` role requirement.

**Default Credentials (Development):**
- Username: `admin`
- Password: `admin`

**Authorization Header Format:**
```
Authorization: Basic <base64-encoded-credentials>
```

**Example (admin:admin):**
```
Authorization: Basic YWRtaW46YWRtaW4=
```

## Building and Generating Documentation

### Maven Build

To build the project and generate OpenAPI documentation:

```bash
# Build the project
mvn clean compile

# Generated OpenAPI specs will be available in:
# target/generated-openapi/openapi.json
# target/generated-openapi/openapi.yaml
```

### Generated Artifacts

After building, you'll find:

1. **OpenAPI JSON Specification**: `target/generated-openapi/openapi.json`
2. **OpenAPI YAML Specification**: `target/generated-openapi/openapi.yaml`
3. **Compiled Classes**: All OpenAPI-annotated classes with documentation metadata

## Integration with Swagger UI

To view the interactive API documentation:

1. **Deploy Swagger UI**: Set up Swagger UI in your web server
2. **Load the OpenAPI Spec**: Point Swagger UI to the generated `openapi.json` file
3. **Access Interactive Docs**: Navigate to the Swagger UI URL

**Example Swagger UI Configuration:**
```javascript
const ui = SwaggerUIBundle({
  url: 'path/to/openapi.json',
  dom_id: '#swagger-ui',
  deepLinking: true,
  presets: [
    SwaggerUIBundle.presets.apis,
    SwaggerUIStandalonePreset
  ]
});
```

## Client SDK Generation

### Using OpenAPI Generator

Generate client SDKs using the OpenAPI Generator tool:

```bash
# Install OpenAPI Generator
npm install @openapitools/openapi-generator-cli -g

# Generate Java client
openapi-generator-cli generate \
  -i target/generated-openapi/openapi.json \
  -g java \
  -o generated-clients/java \
  --additional-properties=packageName=org.eclipse.kura.client

# Generate Python client
openapi-generator-cli generate \
  -i target/generated-openapi/openapi.json \
  -g python \
  -o generated-clients/python \
  --additional-properties=packageName=kura_client

# Generate JavaScript client
openapi-generator-cli generate \
  -i target/generated-openapi/openapi.json \
  -g javascript \
  -o generated-clients/javascript
```

### Available Generators

The OpenAPI specification supports client generation for:

- Java
- Python  
- JavaScript/TypeScript
- C#
- Go
- Ruby
- PHP
- Kotlin
- Swift
- And many more...

## Error Handling

### Standard HTTP Status Codes

| Code | Description | Example Scenario |
|------|-------------|------------------|
| 200  | Success | Request completed successfully |
| 400  | Bad Request | Invalid filter parameters |
| 401  | Unauthorized | Missing or invalid authentication |
| 403  | Forbidden | Insufficient permissions (not in 'system' role) |
| 404  | Not Found | Endpoint doesn't exist |
| 500  | Internal Server Error | System service unavailable |

### Error Response Format

```json
{
  "error": {
    "code": 400,
    "message": "Bad request - Invalid filter parameters",
    "details": "The 'names' array cannot be empty when specified"
  }
}
```

## Best Practices

### 1. API Versioning
- Always specify API version in URLs (`/system/v1/...`)
- Update version when making breaking changes
- Maintain backward compatibility when possible

### 2. Security
- Use HTTPS in production environments
- Implement proper authentication and authorization
- Follow principle of least privilege for role assignments

### 3. Performance
- Use filtered endpoints when you only need specific properties
- Implement proper caching strategies for frequently accessed data
- Monitor API performance and usage patterns

### 4. Documentation Maintenance
- Keep OpenAPI annotations synchronized with code changes
- Update examples when API behavior changes
- Review and update documentation during code reviews

## Development Workflow

### Adding New Endpoints

1. **Create the endpoint method** with JAX-RS annotations
2. **Add OpenAPI annotations**:
   - `@Operation` for method documentation
   - `@ApiResponses` for response documentation
   - `@RequestBody` for request body documentation
   - `@Parameter` for path/query parameters

3. **Update DTOs** with `@Schema` annotations
4. **Rebuild** to generate updated OpenAPI spec
5. **Test** using Swagger UI or generated clients

### Example New Endpoint

```java
@GET
@Path("/status")
@Produces(MediaType.APPLICATION_JSON)
@Operation(
    summary = "Get System Status",
    description = "Retrieves current system status and health information"
)
@ApiResponses(value = {
    @ApiResponse(
        responseCode = "200",
        description = "System status retrieved successfully",
        content = @Content(schema = @Schema(implementation = SystemStatusDTO.class))
    )
})
public SystemStatusDTO getSystemStatus() {
    // Implementation
}
```

## Troubleshooting

### Common Issues

1. **OpenAPI Generation Fails**
   - Check that all referenced classes have proper `@Schema` annotations
   - Verify Maven dependencies are correctly configured
   - Review compilation errors in the build log

2. **Swagger UI Not Loading**
   - Verify the OpenAPI spec file is accessible
   - Check for CORS issues when loading from different domains
   - Validate the generated OpenAPI JSON/YAML syntax

3. **Authentication Issues**
   - Verify Basic Auth credentials are correctly encoded
   - Check that the user has the required `system` role
   - Ensure the Kura user management service is running

### Debugging Tips

- Enable debug logging for the SystemRestService class
- Use browser developer tools to inspect API requests/responses
- Validate OpenAPI specs using online validators
- Test endpoints individually before integration testing

## Future Enhancements

### Planned Features

1. **Additional API Endpoints**: Network status, component health, metrics
2. **Enhanced Security**: OAuth2/JWT support, API key authentication  
3. **Real-time Updates**: WebSocket support for live system monitoring
4. **Advanced Filtering**: Query language support, pagination
5. **API Rate Limiting**: Request throttling and usage quotas

### Contributing

To contribute to the OpenAPI integration:

1. Follow existing annotation patterns and conventions
2. Add comprehensive documentation to all new endpoints
3. Include realistic examples in all `@Schema` annotations
4. Test generated documentation with Swagger UI
5. Update this README with any new features or changes

## Resources

- [OpenAPI 3.0 Specification](https://swagger.io/specification/)
- [Swagger Annotations Documentation](https://github.com/swagger-api/swagger-core/wiki/Swagger-2.X---Annotations)
- [Eclipse Kura Documentation](https://eclipse.github.io/kura/)
- [JAX-RS Reference](https://jakarta.ee/specifications/restful-ws/)
