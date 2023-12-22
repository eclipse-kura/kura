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
        return String.format("%s/%s/NBIRTH/%s", NAMESPACE, groupId, nodeId);
    }

    public static String getNodeDeathTopic(String groupId, String nodeId) {
        return String.format("%s/%s/NDEATH/%s", NAMESPACE, groupId, nodeId);
    }

    public static String getNodeCommandTopic(String groupId, String nodeId) {
        return String.format("%s/%s/NCMD/%s", NAMESPACE, groupId, nodeId);
    }

    // Device topics

    public static String getDeviceBirthTopic(String groupId, String nodeId, String deviceId) {
        return String.format("%s/%s/DBIRTH/%s/%s", NAMESPACE, groupId, nodeId, deviceId);
    }

    public static String getDeviceDeathTopic(String groupId, String nodeId, String deviceId) {
        return String.format("%s/%s/DDEATH/%s/%s", NAMESPACE, groupId, nodeId, deviceId);
    }

    public static String getDeviceDataTopic(String groupId, String nodeId, String deviceId) {
        return String.format("%s/%s/DDATA/%s/%s", NAMESPACE, groupId, nodeId, deviceId);
    }

    public static String getDeviceCommandTopic(String groupId, String nodeId, String deviceId) {
        return String.format("%s/%s/DCMD/%s/%s", NAMESPACE, groupId, nodeId, deviceId);
    }

    // Host Application topics

    public static String getStateTopic(String hostId) {
        return String.format("%s/STATE/%s", NAMESPACE, hostId);
    }

}
