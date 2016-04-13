/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.eclipse.kura.camel.cloud;

public final class KuraCloudConstants {

    public static final String TOPIC = "topic";
    public static final String APPLICATION_ID = "applicationId";

    public static final String CAMEL_KURA_CLOUD = "CamelKuraCloud";
    public static final String CAMEL_KURA_CLOUD_TOPIC = CAMEL_KURA_CLOUD + ".topic";
    public static final String CAMEL_KURA_CLOUD_QOS = CAMEL_KURA_CLOUD + ".qos";
    public static final String CAMEL_KURA_CLOUD_RETAIN = CAMEL_KURA_CLOUD + ".retain";
    public static final String CAMEL_KURA_CLOUD_PRIORITY = CAMEL_KURA_CLOUD + ".priority";
    public static final String CAMEL_KURA_CLOUD_CONTROL = CAMEL_KURA_CLOUD + ".control";
    public static final String CAMEL_KURA_CLOUD_DEVICEID = CAMEL_KURA_CLOUD + ".deviceId";

    private KuraCloudConstants() {
    }

}
