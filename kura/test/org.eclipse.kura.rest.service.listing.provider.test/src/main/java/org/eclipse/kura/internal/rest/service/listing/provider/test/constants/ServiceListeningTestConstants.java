package org.eclipse.kura.internal.rest.service.listing.provider.test.constants;

public class ServiceListeningTestConstants {

    /*
     * POST BODIES
     */

    public static final String COMPLETE_POST_BODY = "{\"interfacesIds\": [ \"org.eclipse.kura.configuration.ConfigurableComponent\", \"org.eclipse.kura.security.keystore.KeystoreService\"]}";
    public static final String NULL_POST_BODY = "{}";
    public static final String EMPTY_POST_BODY = "{\"interfacesIds\": []}";
    public static final String NULL_FIELD_POST_BODY = "{\"interfacesIds\": [\"org.eclipse.kura.configuration.ConfigurableComponent\",]}";
    public static final String EMPTY_FIELD_POST_BODY = "{\"interfacesIds\": [\"org.eclipse.kura.configuration.ConfigurableComponent\",\"\"]}";

    /*
     * END POINTS
     */

    public static final String GET_ENDPOINT = "/sortedList";
    public static final String POST_ENDPOINT = "/sortedList/byAllInterfaces";

    /*
     * CORRECT RESPONSES
     */

    public static final String FILTERED_SERVICES_RESPONSE = "{\"sortedServicesList\":[\"SSLKeystore\"]}";

    /*
     * FAILING MESSAGES ON FAILING
     */

    public static final String NULL_BODY_RESPONSE = "{\"message\":\"Bad request. interfacesIds must not be null\"}";
    public static final String EMPTY_BODY_RESPONSE = "{\"message\":\"Bad request. interfacesIds must not be empty\"}";
    public static final String NULL_FIELD_BODY_RESPONSE = "{\"message\":\"Bad request. none of the interfacesIds can be null\"}";
    public static final String EMPTY_FIELD_BODY_RESPONSE = "{\"message\":\"Bad request. none of the interfacesIds can be empty\"}";

    private ServiceListeningTestConstants() {
    }
}
