/*******************************************************************************
 * Copyright (c) 2023 Eurotech and/or its affiliates and others
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
package org.eclipse.kura.cloudconnection.sparkplug.mqtt.message;


public class SparkplugTopics {

    private SparkplugTopics() {
    }

    private static final String NAMESPACE = "spBv1.0";

    // Edge Node topics

    public static String getNodeBirthTopic(String groupId, String nodeId) {
        return getTopic(groupId, SparkplugMessageType.NBIRTH.toString(), nodeId);
    }

    public static String getNodeDeathTopic(String groupId, String nodeId) {
        return getTopic(groupId, SparkplugMessageType.NDEATH.toString(), nodeId);
    }

    public static String getNodeCommandTopic(String groupId, String nodeId) {
        return getTopic(groupId, SparkplugMessageType.NCMD.toString(), nodeId);
    }

    // Device topics

    public static String getDeviceBirthTopic(String groupId, String nodeId, String deviceId) {
        return getTopic(groupId, SparkplugMessageType.DBIRTH.toString(), nodeId, deviceId);
    }

    public static String getDeviceDeathTopic(String groupId, String nodeId, String deviceId) {
        return getTopic(groupId, SparkplugMessageType.DDEATH.toString(), nodeId, deviceId);
    }

    public static String getDeviceDataTopic(String groupId, String nodeId, String deviceId) {
        return getTopic(groupId, SparkplugMessageType.DDATA.toString(), nodeId, deviceId);
    }

    public static String getDeviceCommandTopic(String groupId, String nodeId, String deviceId) {
        return getTopic(groupId, SparkplugMessageType.DCMD.toString(), nodeId, deviceId);
    }

    // Host Application topics

    public static String getStateTopic(String hostId) {
        return getTopic(SparkplugMessageType.STATE.toString(), hostId);
    }

    private static String getTopic(String... args) {
        StringBuilder topicBuilder = new StringBuilder(NAMESPACE);

        for (String arg : args) {
            topicBuilder.append("/");
            topicBuilder.append(arg);
        }

        return topicBuilder.toString();
    }

}
