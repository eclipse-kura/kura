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
package org.eclipse.kura.internal.misc.cloudcat;

import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;

@ObjectClassDefinition(id = "org.eclipse.kura.misc.cloudcat.CloudCat", name = "CloudCat", description = "Creates a pair of CloudClient instances and copies messages between them.          In a typical scenario, the first client connects to the cloud platform broker via the default CloudService.          The second client connects to the embedded broker via a second CloudService instance.          An external process, connected to the embedded broker, can optionally subscribe to the topic namespace of the second client i.e.          second-account-name/second-device-id/second.cloud.client.app.id/# to receive data messages and $EDC/second-account-name/second-device-id/second.cloud.client.app.id/#          to receive control messages where the account name and devide ID are configured in the DataTransportService layer of the CloudClient's CloudService.          Messages will be relayed between the first and second client preserving the application topic and the QoS of the incoming meessage:          [$EDC/]first-account-name/first-device-id/first.cloud.client.app.id/app-topic <-> [$EDC/]second-account-name/second-device-id/second.cloud.client.app.id/app-topic.")
public @interface CloudCatMetatype {

    @AttributeDefinition(name = "Relay Enable", description = "Enable relaying messages between CloudClient instances.")
    boolean relay_enable() default false;

    @AttributeDefinition(name = "First CloudService PID", description = "The PID of the CloudService used by the first CloudClient, e.g. org.eclipse.kura.cloud.CloudService.")
    String first_cloud_service_pid() default "org.eclipse.kura.cloud.CloudService";

    @AttributeDefinition(name = "Second CloudService PID", description = "The PID of the CloudService used by the first CloudClient, e.g. org.eclipse.kura.cloud.CloudService-2.")
    String second_cloud_service_pid() default "org.eclipse.kura.cloud.CloudService-2";

    @AttributeDefinition(name = "First CloudClient App ID", description = "The application identifier of the first CloudClient.")
    String first_cloud_client_app_id() default "CLOUDCAT1";

    @AttributeDefinition(name = "Second CloudClient App ID", description = "The application identifier of the second CloudClient.")
    String second_cloud_client_app_id() default "CLOUDCAT2";

    @AttributeDefinition(name = "First CloudClient Control Subscriptions", required = false, description = "Comma-separated list of control subscriptions, each in the form control-app-topic;Qos. Usually not needed in virtue of the CloudService default subscription.")
    String first_cloud_client_control_subscriptions() default "";

    @AttributeDefinition(name = "Second CloudClient Control Subscriptions", required = false, description = "Comma-separated list of control subscriptions, each in the form control-app-topic;Qos. Usually not needed in virtue of the CloudService default subscription.")
    String second_cloud_client_control_subscriptions() default "";

    @AttributeDefinition(name = "First CloudClient Data Subscriptions", required = false, description = "Comma-separated list of data subscriptions, each in the form data-app-topic;Qos. Usually not needed.")
    String first_cloud_client_data_subscriptions() default "";

    @AttributeDefinition(name = "Second CloudClient Data Subscriptions", required = false, description = "Comma-separated list of data subscriptions, each in the form data-app-topic;Qos.")
    String second_cloud_client_data_subscriptions() default "#;0";

}


