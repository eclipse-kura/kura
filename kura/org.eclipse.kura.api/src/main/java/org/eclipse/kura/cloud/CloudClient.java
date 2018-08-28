/*******************************************************************************
 * Copyright (c) 2011, 2016 Eurotech and/or its affiliates
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Eurotech
 *******************************************************************************/
package org.eclipse.kura.cloud;

import java.util.List;

import org.eclipse.kura.KuraException;
import org.eclipse.kura.cloudconnection.publisher.CloudPublisher;
import org.eclipse.kura.cloudconnection.subscriber.CloudSubscriber;
import org.eclipse.kura.data.DataService;
import org.eclipse.kura.message.KuraPayload;
import org.osgi.annotation.versioning.ProviderType;

/**
 * The CloudClient is designed to be used by single application bundles.
 * CloudClient instances are acquired from the CloudService and they
 * are released when the work is completed. Generally, a CloudClient
 * is acquired during the activation phase of a bundle and it is released
 * during the deactivation phase.
 * <br>
 * CloudClient leverages the {@link org.eclipse.kura.data.DataService}
 * for all interactions with the transport layer and the communication
 * with the remote server. CloudClient establishes a set of default
 * subscriptions that allow remote servers or other devices to direct messages
 * to the application instance.
 * <br>
 * If the bundle using the CloudClient relies on custom subscriptions
 * beyond the default ones, it is responsibility of the application to implement
 * the {@link CloudClientListener#onConnectionEstablished()} callback method in the
 * CloudCallbackHandler to restore the subscriptions it needs.
 * <br>
 * The <b>CloudClient.release method will unsubscribe</b> all subscriptions
 * incurred by this client and it will unregister this CloudClient
 * instance from the list of CloudCallbackHandlers registered.
 * <br>
 * There can be more than one instance of CloudClient in the system,
 * ideally one per ApplicationId but this is not enforced.
 * The class accepts more than one callback handler; all registered handlers are invoked
 * when a message is received. It is up to the received to analyze the topic
 * of the message received and handle it appropriately.
 * <br>
 * The CloudClient publishes and receives messages using a topic namespace
 * following a structure as: [CRTL_PREFIX/]accountName/deviceId/appId/appTopic.<br>
 * <ul>
 * <li>CRTL_PREFIX: is an optional prefix to denote topic used for control messages
 * as opposed to data messages. The framework makes use of control topics to
 * separate management messages like replies from those used for application data.
 * <li>accountName: an unique identifier that represents a group of devices and users
 * <li>deviceId: an unique identifier within an account that represents a single gateway device.
 * By default, the MAC address of its primary network interface is generally used as the deviceId of the gateway.
 * In the case of an MQTT transport, for example, deviceId maps to the Client Identifier (Client ID).
 * <li>appId: an identifier to denote an application running on the gateway device.
 * We suggest to version the application identifier in order to allow multiple versions of the application
 * to coexist, e.g. CONF-V1, CONF-V2, etc.
 * <li>appTopic topic defined and managed by the application.
 * </ul>
 * accountName, deviceId, and applicationId are derived based on the configuration parameters
 * of the system where this instance is deployed while the applicationTopic is controlled
 * by the application. The following is an example of topic used for publishing where the prefix
 * used for the control Topics is $EDC.
 * <ul>
 * <li>publish: accountName/deviceId/applicationId/appTopic
 * <li>controlPublish: $EDC/accountName/assetId/applicationId/appTopic
 * <li>subscribe: accountName/deviceId/applicationId/appTopic
 * <li>controlSubscribe: $EDC/accountName/deviceId/applicationId/appTopic
 * <li>default subscriptions: $EDC/accountName/deviceId/applicationId/#
 * </ul>
 * Note that the default subscription of a CloudClient allows remote servers
 * or applications running on other devices to publish messages addressed
 * to specific applications running on specific devices.
 * 
 * @noimplement This interface is not intended to be implemented by clients.
 * @deprecated Please consider using {@link CloudPublisher} and {@link CloudSubscriber}
 */
@ProviderType
@Deprecated
public interface CloudClient {

    /**
     * Returns the applicationId of this CloudClient
     *
     * @return applicationId
     */
    public String getApplicationId();

    /**
     * Releases this CloudClient handle. This instance should no longer be used.
     * Note: CloudClient does not unsubscribes all subscriptions incurred by this client,
     * this responsibility is left to the application developer
     */
    public void release();

    /**
     * Returns an indication of whether the connection to the remote server is established.
     * If your application needs to manage the connection directly, it can use the
     * {@link DataService#connect} and {@link DataService#disconnect} methods.
     *
     * @return boolean, whether connection to broker is established.
     */
    public boolean isConnected();

    /**
     * Publishes a message to the remote server using the default priority 5.
     * Before passing the message the to {@link org.eclipse.kura.data.DataService},
     * the CloudClient will manipulate the provided topic by appending the necessary parts
     * to achieve topic partitioning and device identification. It is also responsible to
     * encode the {@link KuraPayload} payload into binary format.
     * <br>
     * The KuraStoreCapacityReachedException is thrown if the database buffer
     * has reached its capacity for messages that are not yet published or
     * they are still in transit.
     *
     * @param appTopic
     *            A String specifying the application portion of the topic the message is published on.
     * @param payload
     *            An KuraPayload representing the message to be published
     * @param qos
     *            An integer specifying the quality of service the message was published on.
     * @param retain
     *            Whether or not the broker should retain the message
     * @return The published message's ID.
     * @throws KuraException
     *             if one of the message composition or message publishing operation fails.
     */
    public int publish(String appTopic, KuraPayload payload, int qos, boolean retain) throws KuraException;

    /**
     * Publishes a message to the remote server using the default priority 5.
     * Before passing the message the to {@link org.eclipse.kura.data.DataService},
     * the CloudClient will manipulate the provided topic by appending the necessary parts
     * to achieve topic partitioning and device identification. It is also responsible to
     * encode the {@link KuraPayload} payload into binary format.
     * <br>
     * The KuraStoreCapacityReachedException is thrown if the database buffer
     * has reached its capacity for messages that are not yet published or
     * they are still in transit.
     *
     * @param deviceId
     *            A String specifying the device ID.
     * @param appTopic
     *            A String specifying the application portion of the topic the message is published on.
     * @param payload
     *            An KuraPayload representing the message to be published
     * @param qos
     *            An integer specifying the quality of service the message was published on.
     * @param retain
     *            Whether or not the broker should retain the message
     * @return The published message's ID.
     * @throws KuraException
     *             if one of the message composition or message publishing operation fails.
     * @since 1.2
     */
    public int publish(String deviceId, String appTopic, KuraPayload payload, int qos, boolean retain)
            throws KuraException;

    /**
     * Publishes a message to the remote server.
     * Before passing the message the to {@link org.eclipse.kura.data.DataService},
     * the CloudClient will manipulate the provided topic by appending the necessary parts
     * to achieve topic partitioning and device identification. It is also responsible to
     * encode the {@link KuraPayload} payload into binary format.
     * <br>
     * The priority argument can be used to control the relative ordering of this
     * message with other messages that may be currently queued for publishing.
     * Priority level 0 (highest) should be used sparingly and reserved for
     * messages that should be sent with the minimum latency. Life-cycle messages
     * (e.g. device start and stop) are an example of messages that are
     * published by the framework with priority 0.
     * Priority 1 messages are used by the framework to publish response messages
     * in request/response conversations to prevent a timeout at the requester.
     * Application should consider using priority 5 or higher.
     * <br>
     * The KuraStoreCapacityReachedException is thrown if the database buffer
     * has reached its capacity for messages that are not yet published or
     * they are still in transit. The limit does not apply to internal messages with the priority less than 2.
     * These priority levels are reserved to the framework which uses it for life-cycle messages
     * - birth and death certificates - and replies to request/response flows.
     *
     * @param appTopic
     *            A String specifying the application portion of the topic the message is published on.
     * @param payload
     *            An KuraPayload representing the message to be published
     * @param qos
     *            An integer specifying the quality of service the message was published on.
     * @param retain
     *            Whether or not the broker should retain the message
     * @param priority
     *            Relative ordering of this message with other messages that may be currently queued for publishing.
     * @return The published message's ID.
     * @throws KuraException
     *             if one of the message composition or message publishing operation fails.
     */
    public int publish(String appTopic, KuraPayload payload, int qos, boolean retain, int priority)
            throws KuraException;

    /**
     * Publishes a message to the remote server.
     * Before passing the message the to {@link org.eclipse.kura.data.DataService},
     * the CloudClient will manipulate the provided topic by appending the necessary parts
     * to achieve topic partitioning and device identification. It is also responsible to
     * encode the {@link KuraPayload} payload into binary format.
     * <br>
     * The priority argument can be used to control the relative ordering of this
     * message with other messages that may be currently queued for publishing.
     * Priority level 0 (highest) should be used sparingly and reserved for
     * messages that should be sent with the minimum latency. Life-cycle messages
     * (e.g. device start and stop) are an example of messages that are
     * published by the framework with priority 0.
     * Priority 1 messages are used by the framework to publish response messages
     * in request/response conversations to prevent a timeout at the requester.
     * Application should consider using priority 5 or higher.
     * <br>
     * The KuraStoreCapacityReachedException is thrown if the database buffer
     * has reached its capacity for messages that are not yet published or
     * they are still in transit. The limit does not apply to internal messages with the priority less than 2.
     * These priority levels are reserved to the framework which uses it for life-cycle messages
     * - birth and death certificates - and replies to request/response flows.
     *
     * @param deviceId
     *            A String specifying the device ID.
     * @param appTopic
     *            A String specifying the application portion of the topic the message is published on.
     * @param payload
     *            An KuraPayload representing the message to be published
     * @param qos
     *            An integer specifying the quality of service the message was published on.
     * @param retain
     *            Whether or not the broker should retain the message
     * @param priority
     *            Relative ordering of this message with other messages that may be currently queued for publishing.
     * @return The published message's ID.
     * @throws KuraException
     *             if one of the message composition or message publishing operation fails.
     * @since 1.2
     */
    public int publish(String deviceId, String appTopic, KuraPayload payload, int qos, boolean retain, int priority)
            throws KuraException;

    /**
     * Publishes a message to the remote server with a raw byte array payload.
     * This is the lowest level publish API exposed by the CloudClient.
     * Before passing the message the to {@link org.eclipse.kura.data.DataService},
     * the CloudClient will manipulate the provided topic by appending the necessary parts
     * to achieve topic partitioning and device identification.
     * <br>
     * The priority argument can be used to control the relative ordering of this
     * message with other messages that may be currently queued for publishing.
     * Priority level 0 (highest) should be used sparingly and reserved for
     * messages that should be sent with the minimum latency. Life-cycle messages
     * (e.g. device start and stop) are an example of messages that are
     * published by the framework with priority 0.
     * Priority 1 messages are used by the framework to publish response messages
     * in request/response conversations to prevent a timeout at the requester.
     * Application should consider using priority 5 or higher.
     * <br>
     * The KuraStoreCapacityReachedException is thrown if the database buffer
     * has reached its capacity for messages that are not yet published or
     * they are still in transit. The limit does not apply to internal messages with the priority less than 2.
     * These priority levels are reserved to the framework which uses it for life-cycle messages
     * - birth and death certificates - and replies to request/response flows.
     *
     * @param appTopic
     *            A String specifying the application portion of the topic the message is published on.
     * @param payload
     *            Binary payload representing the message to be published
     * @param qos
     *            An integer specifying the quality of service the message was published on.
     * @param retain
     *            Whether or not the broker should retain the message
     * @param priority
     *            Relative ordering of this message with other messages that may be currently queued for publishing.
     * @return The published message's ID.
     * @throws KuraException
     *             if one of the message composition or message publishing operation fails.
     */
    public int publish(String appTopic, byte[] payload, int qos, boolean retain, int priority) throws KuraException;

    /**
     * Publishes a message to the remote server with a raw byte array payload.
     * This is the lowest level publish API exposed by the CloudClient.
     * Before passing the message the to {@link org.eclipse.kura.data.DataService},
     * the CloudClient will manipulate the provided topic by appending the necessary parts
     * to achieve topic partitioning and device identification.
     * <br>
     * The priority argument can be used to control the relative ordering of this
     * message with other messages that may be currently queued for publishing.
     * Priority level 0 (highest) should be used sparingly and reserved for
     * messages that should be sent with the minimum latency. Life-cycle messages
     * (e.g. device start and stop) are an example of messages that are
     * published by the framework with priority 0.
     * Priority 1 messages are used by the framework to publish response messages
     * in request/response conversations to prevent a timeout at the requester.
     * Application should consider using priority 5 or higher.
     * <br>
     * The KuraStoreCapacityReachedException is thrown if the database buffer
     * has reached its capacity for messages that are not yet published or
     * they are still in transit. The limit does not apply to internal messages with the priority less than 2.
     * These priority levels are reserved to the framework which uses it for life-cycle messages
     * - birth and death certificates - and replies to request/response flows.
     *
     * @param deviceId
     *            A String specifying the device ID.
     * @param appTopic
     *            A String specifying the application portion of the topic the message is published on.
     * @param payload
     *            Binary payload representing the message to be published
     * @param qos
     *            An integer specifying the quality of service the message was published on.
     * @param retain
     *            Whether or not the broker should retain the message
     * @param priority
     *            Relative ordering of this message with other messages that may be currently queued for publishing.
     * @return The published message's ID.
     * @throws KuraException
     *             if one of the message composition or message publishing operation fails.
     * @since 1.2
     */
    public int publish(String deviceId, String appTopic, byte[] payload, int qos, boolean retain, int priority)
            throws KuraException;

    /**
     * Publishes a control message to the remote server. Control messages are qualified with an
     * additional prefix appended at the beginning of the target topic. The
     * prefix is configured as a property of the {@link DataService} and it appended
     * automatically by this controlPublish method. Just as {@link #publish}, the
     * controlPublish method will manipulate the provided topic by appending the necessary parts
     * to achieve topic partitioning including device identification and encode
     * the {@link KuraPayload} payload into binary format.
     * <br>
     * The priority argument can be used to control the relative ordering of this
     * message with other messages that may be currently queued for publishing.
     * Priority level 0 (highest) should be used sparingly and reserved for
     * messages that should be sent with the minimum latency. Life-cycle messages
     * (e.g. device start and stop) are an example of messages that are
     * published by the framework with priority 0.
     * Priority 1 messages are used by the framework to publish response messages
     * in request/response conversations to prevent a timeout at the requester.
     * Application should consider using priority 5 or higher.
     * <br>
     * The KuraStoreCapacityReachedException is thrown if the database buffer
     * has reached its capacity for messages that are not yet published or
     * they are still in transit. The limit does not apply to internal messages with the priority less than 2.
     * These priority levels are reserved to the framework which uses it for life-cycle messages
     * - birth and death certificates - and replies to request/response flows.
     *
     * @param appTopic
     *            A String specifying the application topic the message is published on.
     * @param payload
     *            An KuraPayload representing the message to be published
     * @param qos
     *            An integer specifying the quality of service the message was published on.
     * @param retain
     *            Whether or not the broker should retain the message
     * @param priority
     *            Relative ordering of this message with other messages that may be currently queued for publishing.
     * @return The published message's ID.
     * @throws KuraException
     *             if one of the message composition or message publishing operation fails.
     */
    public int controlPublish(String appTopic, KuraPayload payload, int qos, boolean retain, int priority)
            throws KuraException;

    /**
     * Publishes a control message to the remote server addressing it to another device.
     * Control messages are qualified with an additional prefix appended at the beginning of the target topic.
     * The prefix is configured as a property of the {@link DataService} and it appended
     * automatically by this controlPublish method. Just as {@link #publish}, the
     * controlPublish method will manipulate the provided topic by appending the necessary parts
     * to achieve topic partitioning including device identification and encode
     * the {@link KuraPayload} payload into binary format.
     * <br>
     * The priority argument can be used to control the relative ordering of this
     * message with other messages that may be currently queued for publishing.
     * Priority level 0 (highest) should be used sparingly and reserved for
     * messages that should be sent with the minimum latency. Life-cycle messages
     * (e.g. device start and stop) are an example of messages that are
     * published by the framework with priority 0.
     * Priority 1 messages are used by the framework to publish response messages
     * in request/response conversations to prevent a timeout at the requester.
     * Application should consider using priority 5 or higher.
     * <br>
     * The KuraStoreCapacityReachedException is thrown if the database buffer
     * has reached its capacity for messages that are not yet published or
     * they are still in transit. The limit does not apply to internal messages with the priority less than 2.
     * These priority levels are reserved to the framework which uses it for life-cycle messages
     * - birth and death certificates - and replies to request/response flows.
     *
     * @param deviceId
     *            A String specifying the device ID.
     * @param appTopic
     *            A String specifying the application topic the message is published on.
     * @param payload
     *            An KuraPayload representing the message to be published
     * @param qos
     *            An integer specifying the quality of service the message was published on.
     * @param retain
     *            Whether or not the broker should retain the message
     * @param priority
     *            Relative ordering of this message with other messages that may be currently queued for publishing.
     * @return The published message's ID.
     * @throws KuraException
     *             if one of the message composition or message publishing operation fails.
     */
    public int controlPublish(String deviceId, String appTopic, KuraPayload payload, int qos, boolean retain,
            int priority) throws KuraException;

    /**
     * Publishes a control message to the remote server addressing it to another device
     * with a raw byte array payload.
     * Control messages are qualified with an additional prefix appended at the beginning of the target topic.
     * The prefix is configured as a property of the {@link DataService} and it appended
     * automatically by this controlPublish method. Just as {@link #publish}, the
     * controlPublish method will manipulate the provided topic by appending the necessary parts
     * to achieve topic partitioning including device identification.
     * <br>
     * The priority argument can be used to control the relative ordering of this
     * message with other messages that may be currently queued for publishing.
     * Priority level 0 (highest) should be used sparingly and reserved for
     * messages that should be sent with the minimum latency. Life-cycle messages
     * (e.g. device start and stop) are an example of messages that are
     * published by the framework with priority 0.
     * Priority 1 messages are used by the framework to publish response messages
     * in request/response conversations to prevent a timeout at the requester.
     * Application should consider using priority 5 or higher.
     * <br>
     * The KuraStoreCapacityReachedException is thrown if the database buffer
     * has reached its capacity for messages that are not yet published or
     * they are still in transit. The limit does not apply to internal messages with the priority less than 2.
     * These priority levels are reserved to the framework which uses it for life-cycle messages
     * - birth and death certificates - and replies to request/response flows.
     *
     * @param deviceId
     *            A String specifying the device ID.
     * @param appTopic
     *            A String specifying the application topic the message is published on.
     * @param payload
     *            Binary payload representing the message to be published.
     * @param qos
     *            An integer specifying the quality of service the message was published on.
     * @param retain
     *            Whether or not the broker should retain the message.
     * @param priority
     *            Relative ordering of this message with other messages that may be currently queued for publishing.
     * @return The published message's ID.
     * @throws KuraException
     *             if one of the message composition or message publishing operation fails.
     */
    public int controlPublish(String deviceId, String appTopic, byte[] payload, int qos, boolean retain, int priority)
            throws KuraException;

    /**
     * Subscribes to a topic with the remote server. The topic is specified as a String
     * object and the QoS is specified as an integer. The CloudClient will manipulate the
     * provided topic by appending the necessary parts to achieve topic partitioning and
     * device identification.<br>
     * This is a synchronous call. If the subscribe fails, an exception will be thrown
     * that will contain information about the cause of the failure.
     *
     * @param appTopic
     *            A String object containing the application topic.
     * @param qos
     *            An int containing the Quality of Service.
     * @throws KuraException
     *             if the subscription fails.
     */
    public void subscribe(String appTopic, int qos) throws KuraException;

    /**
     * Subscribes to a topic with the remote server. The topic is specified by two StringS: one characterizing the
     * deviceId and the other representing the appTopic. The QoS is specified as an integer. The CloudClient will
     * manipulate the provided topic by appending the necessary parts to achieve topic partitioning and
     * device identification.<br>
     * This is a synchronous call. If the subscribe fails, an exception will be thrown
     * that will contain information about the cause of the failure.
     *
     * @param deviceId
     *            A String specifying the device ID.
     * @param appTopic
     *            A String object containing the application topic.
     * @param qos
     *            An int containing the Quality of Service.
     * @throws KuraException
     *             if the subscription fails.
     * @since 1.2
     */
    public void subscribe(String deviceId, String appTopic, int qos) throws KuraException;

    /**
     * Subscribes to a control topic with the remote server. The topic is specified as a String
     * object and the QoS is specified as an integer. The CloudClient will manipulate the
     * provided topic by appending the necessary parts to achieve topic partitioning and
     * including control prefix and device identification.<br>
     * This is a synchronous call. If the subscribe fails, an exception will be thrown
     * that will contain information about the cause of the failure.
     *
     * @param appTopic
     *            A String object containing the application topic.
     * @param qos
     *            An int containing the Quality of Service.
     * @throws KuraException
     *             if the subscription fails.
     */
    public void controlSubscribe(String appTopic, int qos) throws KuraException;

    /**
     * Subscribes to a control topic with the remote server. The topic is specified by two StringS: one characterizing
     * the deviceId and the other representing the appTopic. The QoS is specified as an integer. The CloudClient will
     * manipulate the provided topic by appending the necessary parts to achieve topic partitioning and
     * including control prefix and device identification.<br>
     * This is a synchronous call. If the subscribe fails, an exception will be thrown
     * that will contain information about the cause of the failure.
     *
     * @param deviceId
     *            A String specifying the device ID.
     * @param appTopic
     *            A String object containing the application topic.
     * @param qos
     *            An int containing the Quality of Service.
     * @throws KuraException
     *             if the subscription fails.
     * @since 1.2
     */
    public void controlSubscribe(String deviceId, String appTopic, int qos) throws KuraException;

    /**
     * Unubscribes to a topic with the remote server. The topic is specified as a String
     * object and the QoS is specified as an integer. The CloudClient will manipulate the
     * provided topic by appending the necessary parts to achieve topic partitioning and
     * device identification.<br>
     * This is a synchronous call. If the unsubscribe fails, an exception will be thrown
     * that will contain information about the cause of the failure.
     *
     * @param appTopic
     *            A String object containing the application topic.
     * @throws KuraException
     *             if the unsubscription fails.
     */
    public void unsubscribe(String appTopic) throws KuraException;

    /**
     * Unubscribes to a topic with the remote server. The topic is specified by two StringS: one characterizing
     * the deviceId and the other representing the appTopic. The QoS is specified as an integer. The CloudClient will
     * manipulate the provided topic by appending the necessary parts to achieve topic partitioning and
     * device identification.<br>
     * This is a synchronous call. If the unsubscribe fails, an exception will be thrown
     * that will contain information about the cause of the failure.
     *
     * @param deviceId
     *            A String specifying the device ID.
     * @param appTopic
     *            A String object containing the application topic.
     * @throws KuraException
     *             if the unsubscription fails.
     * @since 1.2
     */
    public void unsubscribe(String deviceId, String appTopic) throws KuraException;

    /**
     * Unsubscribes to a control topic with the remote server. The topic is specified as a String
     * object and the QoS is specified as an integer. The CloudClient will manipulate the
     * provided topic by appending the necessary parts to achieve topic partitioning and
     * including control prefix and device identification.<br>
     * This is a synchronous call. If the unsubscribe fails, an exception will be thrown
     * that will contain information about the cause of the failure.
     *
     * @param appTopic
     *            A String object containing the application topic.
     * @throws KuraException
     *             if the unsubscription fails.
     */
    public void controlUnsubscribe(String appTopic) throws KuraException;

    /**
     * Unsubscribes to a control topic with the remote server. The topic is specified by two StringS: one characterizing
     * the deviceId and the other representing the appTopic. The QoS is specified as an integer. The CloudClient will
     * manipulate the provided topic by appending the necessary parts to achieve topic partitioning and
     * including control prefix and device identification.<br>
     * This is a synchronous call. If the unsubscribe fails, an exception will be thrown
     * that will contain information about the cause of the failure.
     *
     * @param deviceId
     *            A String specifying the device ID.
     * @param appTopic
     *            A String object containing the application topic.
     * @throws KuraException
     *             if the unsubscription fails.
     * @since 1.2
     */
    public void controlUnsubscribe(String deviceId, String appTopic) throws KuraException;

    /**
     * Adds a CloudCallbackHandler with this CloudClient. This handler
     * will receive events when a client publication has arrived, and
     * when a publish has been fully acknowledged by the remote server.
     *
     * @param cloudClientListener
     *            An implementation of the CloudCallbackHandler interface.
     */
    public void addCloudClientListener(CloudClientListener cloudClientListener);

    /**
     * Removes a CloudCallbackHandler from this CloudClient.
     * The provided CloudCallbackHandler will no longer receive the events
     * when a published message is received.
     */
    public void removeCloudClientListener(CloudClientListener cloudClientListener);

    /**
     * Gets the list of identifiers of messages that have not been published yet.
     *
     * @return a list of integers.
     * @throws KuraException
     *             if the operation fails.
     */
    List<Integer> getUnpublishedMessageIds() throws KuraException;

    /**
     * Finds the list of identifiers of messages that are still in-flight
     * (messages published but not confirmed yet).
     * This only applies to messages published with QoS &gt; 0.
     *
     * @return a list of integers.
     * @throws KuraException
     *             if the operation fails.
     */
    List<Integer> getInFlightMessageIds() throws KuraException;

    /**
     * Finds the list of identifiers of in-flight messages that have been dropped.
     * This only applies to messages published with QoS &gt; 0.
     * On the establishment of a new connection, the service can be configured
     * either to republish or drop in-flight messages.
     * The former option can be used if service users tolerate publishing message
     * duplicates.
     * The latter option can be used it service users tolerate losing messages.
     *
     * @return a list of integers.
     * @throws KuraException
     *             if the operation fails.
     */
    List<Integer> getDroppedInFlightMessageIds() throws KuraException;
}
