package org.eclipse.kura.rest.network.configuration.provider.test.responses;

public class RestNetworkConfigurationJson {

    private RestNetworkConfigurationJson() {
    }

    /*
     * Requests
     */
    public static final String FIREWALL_IP6_BYPID_REQUEST = "{\"pids\":[\"org.eclipse.kura.net.admin.ipv6.FirewallConfigurationServiceIPv6\"]}";
    public static final String FIREWALL_IP6_UPDATE_REQUEST = "{\"configs\":[{\"pid\":\"org.eclipse.kura.net.admin.ipv6.FirewallConfigurationServiceIPv6\",properties: {\"firewall.ipv6.open.ports\":{\"type\":\"STRING\",\"value\":\"1234,tcp,0:0:0:0:0:0:0:0/0,,,,,#\"}}}]}";

    /*
     * Responses
     */
    public static final String ALL_CONFIGURATIONS_RESPONSE = "{\"configs\":[{\"pid\":\"CONF_COMP_PID_0\",\"definition\":{\"name\":\"OCD_MOCK_NAME_0\",\"description\":\"OCD_MOCK_DESC_0\",\"id\":\"OCD_MOCK_ID_0\"},\"properties\":{}},{\"pid\":\"CONF_COMP_PID_1\",\"definition\":{\"name\":\"OCD_MOCK_NAME_1\",\"description\":\"OCD_MOCK_DESC_1\",\"id\":\"OCD_MOCK_ID_1\"},\"properties\":{}},{\"pid\":\"CONF_COMP_PID_2\",\"definition\":{\"name\":\"OCD_MOCK_NAME_2\",\"description\":\"OCD_MOCK_DESC_2\",\"id\":\"OCD_MOCK_ID_2\"},\"properties\":{}}]}";
    public static final String SINGLE_CONFIG_RESPONSE = "{\"configs\":[{\"pid\":\"CONF_COMP_PID_0\",\"definition\":{\"name\":\"OCD_MOCK_NAME_0\",\"description\":\"OCD_MOCK_DESC_0\",\"id\":\"OCD_MOCK_ID_0\"},\"properties\":{}}]}";
}
