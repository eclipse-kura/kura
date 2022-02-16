/*******************************************************************************
 * Copyright (c) 2022 Eurotech and/or its affiliates and others
 * 
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 * 
 * SPDX-License-Identifier: EPL-2.0
 * 
 * Contributors:
 * Eurotech
 ******************************************************************************/
package org.eclipse.kura.rest.wire.provider.test;

public class Snippets {

    public static final String BASE_CHANNEL_DESCRIPTOR = "[\n" //
            + "        {\n" //
            + "            \"name\": \"enabled\",\n" //
            + "            \"description\": \"Determines if the channel is enabled or not\",\n" //
            + "            \"id\": \"+enabled\",\n" //
            + "            \"type\": \"BOOLEAN\",\n" //
            + "            \"cardinality\": 0,\n" //
            + "            \"defaultValue\": \"true\",\n" //
            + "            \"isRequired\": true\n" //
            + "        },\n" //
            + "        {\n" //
            + "            \"name\": \"name\",\n" //
            + "            \"description\": \"Name of the Channel\",\n" //
            + "            \"id\": \"+name\",\n" //
            + "            \"type\": \"STRING\",\n" //
            + "            \"cardinality\": 0,\n" //
            + "            \"defaultValue\": \"Channel-1\",\n" //
            + "            \"isRequired\": true\n" //
            + "        },\n" //
            + "        {\n" //
            + "            \"option\": [\n" //
            + "                {\n" //
            + "                    \"label\": \"READ\",\n" //
            + "                    \"value\": \"READ\"\n" //
            + "                },\n" //
            + "                {\n" //
            + "                    \"label\": \"READ_WRITE\",\n" //
            + "                    \"value\": \"READ_WRITE\"\n" //
            + "                },\n" //
            + "                {\n" //
            + "                    \"label\": \"WRITE\",\n" //
            + "                    \"value\": \"WRITE\"\n" //
            + "                }\n" //
            + "            ],\n" //
            + "            \"name\": \"type\",\n" //
            + "            \"description\": \"Type of the channel\",\n" //
            + "            \"id\": \"+type\",\n" //
            + "            \"type\": \"STRING\",\n" //
            + "            \"cardinality\": 0,\n" //
            + "            \"defaultValue\": \"READ\",\n" //
            + "            \"isRequired\": true\n" //
            + "        },\n" //
            + "        {\n" //
            + "            \"option\": [\n" //
            + "                {\n" //
            + "                    \"label\": \"BOOLEAN\",\n" //
            + "                    \"value\": \"BOOLEAN\"\n" //
            + "                },\n" //
            + "                {\n" //
            + "                    \"label\": \"BYTE_ARRAY\",\n" //
            + "                    \"value\": \"BYTE_ARRAY\"\n" //
            + "                },\n" //
            + "                {\n" //
            + "                    \"label\": \"DOUBLE\",\n" //
            + "                    \"value\": \"DOUBLE\"\n" //
            + "                },\n" //
            + "                {\n" //
            + "                    \"label\": \"INTEGER\",\n" //
            + "                    \"value\": \"INTEGER\"\n" //
            + "                },\n" //
            + "                {\n" //
            + "                    \"label\": \"LONG\",\n" //
            + "                    \"value\": \"LONG\"\n" //
            + "                },\n" //
            + "                {\n" //
            + "                    \"label\": \"FLOAT\",\n" //
            + "                    \"value\": \"FLOAT\"\n" //
            + "                },\n" //
            + "                {\n" //
            + "                    \"label\": \"STRING\",\n" //
            + "                    \"value\": \"STRING\"\n" //
            + "                }\n" //
            + "            ],\n" //
            + "            \"name\": \"value.type\",\n" //
            + "            \"description\": \"Value type of the channel\",\n" //
            + "            \"id\": \"+value.type\",\n" //
            + "            \"type\": \"STRING\",\n" //
            + "            \"cardinality\": 0,\n" //
            + "            \"defaultValue\": \"INTEGER\",\n" //
            + "            \"isRequired\": true\n" //
            + "        },\n" //
            + "        {\n" //
            + "            \"name\": \"scale\",\n" //
            + "            \"description\": \"Scale to be applied to the numeric value of the channel\",\n" //
            + "            \"id\": \"+scale\",\n" //
            + "            \"type\": \"DOUBLE\",\n" //
            + "            \"cardinality\": 0,\n" //
            + "            \"isRequired\": false\n" //
            + "        },\n" //
            + "        {\n" //
            + "            \"name\": \"offset\",\n" //
            + "            \"description\": \"Offset to be applied to the numeric value of the channel\",\n" //
            + "            \"id\": \"+offset\",\n" //
            + "            \"type\": \"DOUBLE\",\n" //
            + "            \"cardinality\": 0,\n" //
            + "            \"isRequired\": false\n" //
            + "        },\n" //
            + "        {\n" //
            + "            \"name\": \"unit\",\n" //
            + "            \"description\": \"Unit associated to the value of the channel\",\n" //
            + "            \"id\": \"+unit\",\n" //
            + "            \"type\": \"STRING\",\n" //
            + "            \"cardinality\": 0,\n" //
            + "            \"defaultValue\": \"\",\n" //
            + "            \"isRequired\": false\n" //
            + "        },\n" //
            + "        {\n" //
            + "            \"name\": \"listen\",\n" //
            + "            \"description\": \"Specifies if WireAsset should emit envelopes on Channel events\",\n" //
            + "            \"id\": \"+listen\",\n" //
            + "            \"type\": \"BOOLEAN\",\n" //
            + "            \"cardinality\": 0,\n" //
            + "            \"defaultValue\": \"false\",\n" //
            + "            \"isRequired\": true\n" //
            + "        }\n" //
            + "    ]";

    public static final String TEST_DRIVER_DESCRIPTOR = "{\n" //
            + "            \"pid\": \"testDriver\",\n" //
            + "            \"factoryPid\": \"org.eclipse.kura.util.test.driver.ChannelDescriptorTestDriver\",\n" //
            + "            \"channelDescriptor\": [\n" //
            + "                {\n" //
            + "                    \"name\": \"STRING property\",\n" //
            + "                    \"description\": \"A STRING property\",\n" //
            + "                    \"id\": \"STRING.prop\",\n" //
            + "                    \"type\": \"STRING\",\n" //
            + "                    \"cardinality\": 0,\n" //
            + "                    \"defaultValue\": \"foo\",\n" //
            + "                    \"isRequired\": true\n" //
            + "                },\n" //
            + "                {\n" //
            + "                    \"name\": \"STRING property not required\",\n" //
            + "                    \"description\": \"A STRING property not required\",\n" //
            + "                    \"id\": \"STRING.propnot.required\",\n" //
            + "                    \"type\": \"STRING\",\n" //
            + "                    \"cardinality\": 0,\n" //
            + "                    \"isRequired\": true\n" //
            + "                },\n" //
            + "                {\n" //
            + "                    \"name\": \"STRING property with min and max\",\n" //
            + "                    \"description\": \"A STRING property with min : foo and max : bar\",\n" //
            + "                    \"id\": \"STRING.prop.min.max\",\n" //
            + "                    \"type\": \"STRING\",\n" //
            + "                    \"cardinality\": 0,\n" //
            + "                    \"max\": \"bar\",\n" //
            + "                    \"defaultValue\": \"foo\",\n" //
            + "                    \"isRequired\": true\n" //
            + "                },\n" //
            + "                {\n" //
            + "                    \"name\": \"BOOLEAN property\",\n" //
            + "                    \"description\": \"A BOOLEAN property\",\n" //
            + "                    \"id\": \"BOOLEAN.prop\",\n" //
            + "                    \"type\": \"BOOLEAN\",\n" //
            + "                    \"cardinality\": 0,\n" //
            + "                    \"defaultValue\": \"true\",\n" //
            + "                    \"isRequired\": true\n" //
            + "                },\n" //
            + "                {\n" //
            + "                    \"name\": \"BOOLEAN property not required\",\n" //
            + "                    \"description\": \"A BOOLEAN property not required\",\n" //
            + "                    \"id\": \"BOOLEAN.propnot.required\",\n" //
            + "                    \"type\": \"BOOLEAN\",\n" //
            + "                    \"cardinality\": 0,\n" //
            + "                    \"isRequired\": true\n" //
            + "                },\n" //
            + "                {\n" //
            + "                    \"name\": \"BYTE property\",\n" //
            + "                    \"description\": \"A BYTE property\",\n" //
            + "                    \"id\": \"BYTE.prop\",\n" //
            + "                    \"type\": \"BYTE\",\n" //
            + "                    \"cardinality\": 0,\n" //
            + "                    \"defaultValue\": \"15\",\n" //
            + "                    \"isRequired\": true\n" //
            + "                },\n" //
            + "                {\n" //
            + "                    \"name\": \"BYTE property not required\",\n" //
            + "                    \"description\": \"A BYTE property not required\",\n" //
            + "                    \"id\": \"BYTE.propnot.required\",\n" //
            + "                    \"type\": \"BYTE\",\n" //
            + "                    \"cardinality\": 0,\n" //
            + "                    \"isRequired\": true\n" //
            + "                },\n" //
            + "                {\n" //
            + "                    \"name\": \"BYTE property with min and max\",\n" //
            + "                    \"description\": \"A BYTE property with min : 10 and max : 20\",\n" //
            + "                    \"id\": \"BYTE.prop.min.max\",\n" //
            + "                    \"type\": \"BYTE\",\n" //
            + "                    \"cardinality\": 0,\n" //
            + "                    \"max\": \"20\",\n" //
            + "                    \"defaultValue\": \"15\",\n" //
            + "                    \"isRequired\": true\n" //
            + "                },\n" //
            + "                {\n" //
            + "                    \"name\": \"CHAR property\",\n" //
            + "                    \"description\": \"A CHAR property\",\n" //
            + "                    \"id\": \"CHAR.prop\",\n" //
            + "                    \"type\": \"CHAR\",\n" //
            + "                    \"cardinality\": 0,\n" //
            + "                    \"defaultValue\": \"c\",\n" //
            + "                    \"isRequired\": true\n" //
            + "                },\n" //
            + "                {\n" //
            + "                    \"name\": \"CHAR property not required\",\n" //
            + "                    \"description\": \"A CHAR property not required\",\n" //
            + "                    \"id\": \"CHAR.propnot.required\",\n" //
            + "                    \"type\": \"CHAR\",\n" //
            + "                    \"cardinality\": 0,\n" //
            + "                    \"isRequired\": true\n" //
            + "                },\n" //
            + "                {\n" //
            + "                    \"name\": \"CHAR property with min and max\",\n" //
            + "                    \"description\": \"A CHAR property with min : b and max : l\",\n" //
            + "                    \"id\": \"CHAR.prop.min.max\",\n" //
            + "                    \"type\": \"CHAR\",\n" //
            + "                    \"cardinality\": 0,\n" //
            + "                    \"max\": \"l\",\n" //
            + "                    \"defaultValue\": \"c\",\n" //
            + "                    \"isRequired\": true\n" //
            + "                },\n" //
            + "                {\n" //
            + "                    \"name\": \"DOUBLE property\",\n" //
            + "                    \"description\": \"A DOUBLE property\",\n" //
            + "                    \"id\": \"DOUBLE.prop\",\n" //
            + "                    \"type\": \"DOUBLE\",\n" //
            + "                    \"cardinality\": 0,\n" //
            + "                    \"defaultValue\": \"16\",\n" //
            + "                    \"isRequired\": true\n" //
            + "                },\n" //
            + "                {\n" //
            + "                    \"name\": \"DOUBLE property not required\",\n" //
            + "                    \"description\": \"A DOUBLE property not required\",\n" //
            + "                    \"id\": \"DOUBLE.propnot.required\",\n" //
            + "                    \"type\": \"DOUBLE\",\n" //
            + "                    \"cardinality\": 0,\n" //
            + "                    \"isRequired\": true\n" //
            + "                },\n" //
            + "                {\n" //
            + "                    \"name\": \"DOUBLE property with min and max\",\n" //
            + "                    \"description\": \"A DOUBLE property with min : 13.5 and max : 20.5\",\n" //
            + "                    \"id\": \"DOUBLE.prop.min.max\",\n" //
            + "                    \"type\": \"DOUBLE\",\n" //
            + "                    \"cardinality\": 0,\n" //
            + "                    \"max\": \"20.5\",\n" //
            + "                    \"defaultValue\": \"16\",\n" //
            + "                    \"isRequired\": true\n" //
            + "                },\n" //
            + "                {\n" //
            + "                    \"name\": \"FLOAT property\",\n" //
            + "                    \"description\": \"A FLOAT property\",\n" //
            + "                    \"id\": \"FLOAT.prop\",\n" //
            + "                    \"type\": \"FLOAT\",\n" //
            + "                    \"cardinality\": 0,\n" //
            + "                    \"defaultValue\": \"16\",\n" //
            + "                    \"isRequired\": true\n" //
            + "                },\n" //
            + "                {\n" //
            + "                    \"name\": \"FLOAT property not required\",\n" //
            + "                    \"description\": \"A FLOAT property not required\",\n" //
            + "                    \"id\": \"FLOAT.propnot.required\",\n" //
            + "                    \"type\": \"FLOAT\",\n" //
            + "                    \"cardinality\": 0,\n" //
            + "                    \"isRequired\": true\n" //
            + "                },\n" //
            + "                {\n" //
            + "                    \"name\": \"FLOAT property with min and max\",\n" //
            + "                    \"description\": \"A FLOAT property with min : 13.5 and max : 20.5\",\n" //
            + "                    \"id\": \"FLOAT.prop.min.max\",\n" //
            + "                    \"type\": \"FLOAT\",\n" //
            + "                    \"cardinality\": 0,\n" //
            + "                    \"max\": \"20.5\",\n" //
            + "                    \"defaultValue\": \"16\",\n" //
            + "                    \"isRequired\": true\n" //
            + "                },\n" //
            + "                {\n" //
            + "                    \"name\": \"INTEGER property\",\n" //
            + "                    \"description\": \"A INTEGER property\",\n" //
            + "                    \"id\": \"INTEGER.prop\",\n" //
            + "                    \"type\": \"INTEGER\",\n" //
            + "                    \"cardinality\": 0,\n" //
            + "                    \"defaultValue\": \"10\",\n" //
            + "                    \"isRequired\": true\n" //
            + "                },\n" //
            + "                {\n" //
            + "                    \"name\": \"INTEGER property not required\",\n" //
            + "                    \"description\": \"A INTEGER property not required\",\n" //
            + "                    \"id\": \"INTEGER.propnot.required\",\n" //
            + "                    \"type\": \"INTEGER\",\n" //
            + "                    \"cardinality\": 0,\n" //
            + "                    \"isRequired\": true\n" //
            + "                },\n" //
            + "                {\n" //
            + "                    \"name\": \"INTEGER property with min and max\",\n" //
            + "                    \"description\": \"A INTEGER property with min : -200000 and max : 300000\",\n" //
            + "                    \"id\": \"INTEGER.prop.min.max\",\n" //
            + "                    \"type\": \"INTEGER\",\n" //
            + "                    \"cardinality\": 0,\n" //
            + "                    \"max\": \"300000\",\n" //
            + "                    \"defaultValue\": \"10\",\n" //
            + "                    \"isRequired\": true\n" //
            + "                },\n" //
            + "                {\n" //
            + "                    \"name\": \"LONG property\",\n" //
            + "                    \"description\": \"A LONG property\",\n" //
            + "                    \"id\": \"LONG.prop\",\n" //
            + "                    \"type\": \"LONG\",\n" //
            + "                    \"cardinality\": 0,\n" //
            + "                    \"defaultValue\": \"2\",\n" //
            + "                    \"isRequired\": true\n" //
            + "                },\n" //
            + "                {\n" //
            + "                    \"name\": \"LONG property not required\",\n" //
            + "                    \"description\": \"A LONG property not required\",\n" //
            + "                    \"id\": \"LONG.propnot.required\",\n" //
            + "                    \"type\": \"LONG\",\n" //
            + "                    \"cardinality\": 0,\n" //
            + "                    \"isRequired\": true\n" //
            + "                },\n" //
            + "                {\n" //
            + "                    \"name\": \"LONG property with min and max\",\n" //
            + "                    \"description\": \"A LONG property with min : -2147493648 and max : 2147493647\",\n" //
            + "                    \"id\": \"LONG.prop.min.max\",\n" //
            + "                    \"type\": \"LONG\",\n" //
            + "                    \"cardinality\": 0,\n" //
            + "                    \"max\": \"2147493647\",\n" //
            + "                    \"defaultValue\": \"2\",\n" //
            + "                    \"isRequired\": true\n" //
            + "                },\n" //
            + "                {\n" //
            + "                    \"name\": \"SHORT property\",\n" //
            + "                    \"description\": \"A SHORT property\",\n" //
            + "                    \"id\": \"SHORT.prop\",\n" //
            + "                    \"type\": \"SHORT\",\n" //
            + "                    \"cardinality\": 0,\n" //
            + "                    \"defaultValue\": \"1\",\n" //
            + "                    \"isRequired\": true\n" //
            + "                },\n" //
            + "                {\n" //
            + "                    \"name\": \"SHORT property not required\",\n" //
            + "                    \"description\": \"A SHORT property not required\",\n" //
            + "                    \"id\": \"SHORT.propnot.required\",\n" //
            + "                    \"type\": \"SHORT\",\n" //
            + "                    \"cardinality\": 0,\n" //
            + "                    \"isRequired\": true\n" //
            + "                },\n" //
            + "                {\n" //
            + "                    \"name\": \"SHORT property with min and max\",\n" //
            + "                    \"description\": \"A SHORT property with min : -20000 and max : 20000\",\n" //
            + "                    \"id\": \"SHORT.prop.min.max\",\n" //
            + "                    \"type\": \"SHORT\",\n" //
            + "                    \"cardinality\": 0,\n" //
            + "                    \"max\": \"20000\",\n" //
            + "                    \"defaultValue\": \"1\",\n" //
            + "                    \"isRequired\": true\n" //
            + "                },\n" //
            + "                {\n" //
            + "                    \"option\": [\n" //
            + "                        {\n" //
            + "                            \"label\": \"Choice 0, value : foo\",\n" //
            + "                            \"value\": \"foo\"\n" //
            + "                        },\n" //
            + "                        {\n" //
            + "                            \"label\": \"Choice 1, value : bar\",\n" //
            + "                            \"value\": \"bar\"\n" //
            + "                        },\n" //
            + "                        {\n" //
            + "                            \"label\": \"Choice 2, value : baz\",\n" //
            + "                            \"value\": \"baz\"\n" //
            + "                        }\n" //
            + "                    ],\n" //
            + "                    \"name\": \"STRING property with options\",\n" //
            + "                    \"description\": \"A STRING property wit options\",\n" //
            + "                    \"id\": \"STRING.options\",\n" //
            + "                    \"type\": \"STRING\",\n" //
            + "                    \"cardinality\": 0,\n" //
            + "                    \"defaultValue\": \"foo\",\n" //
            + "                    \"isRequired\": true\n" //
            + "                },\n" //
            + "                {\n" //
            + "                    \"option\": [\n" //
            + "                        {\n" //
            + "                            \"label\": \"Choice 0, value : 10\",\n" //
            + "                            \"value\": \"10\"\n" //
            + "                        },\n" //
            + "                        {\n" //
            + "                            \"label\": \"Choice 1, value : 20\",\n" //
            + "                            \"value\": \"20\"\n" //
            + "                        },\n" //
            + "                        {\n" //
            + "                            \"label\": \"Choice 2, value : 30\",\n" //
            + "                            \"value\": \"30\"\n" //
            + "                        }\n" //
            + "                    ],\n" //
            + "                    \"name\": \"BYTE property with options\",\n" //
            + "                    \"description\": \"A BYTE property wit options\",\n" //
            + "                    \"id\": \"BYTE.options\",\n" //
            + "                    \"type\": \"BYTE\",\n" //
            + "                    \"cardinality\": 0,\n" //
            + "                    \"defaultValue\": \"10\",\n" //
            + "                    \"isRequired\": true\n" //
            + "                },\n" //     
            + "                {\n" //
            + "                    \"option\": [\n" //
            + "                        {\n" //
            + "                            \"label\": \"Choice 0, value : a\",\n" //
            + "                            \"value\": \"a\"\n" //
            + "                        },\n" //
            + "                        {\n" //
            + "                            \"label\": \"Choice 1, value : b\",\n" //
            + "                            \"value\": \"b\"\n" //
            + "                        },\n" //
            + "                        {\n" //
            + "                            \"label\": \"Choice 2, value : c\",\n" //
            + "                            \"value\": \"c\"\n" //
            + "                        }\n" //
            + "                    ],\n" //
            + "                    \"name\": \"CHAR property with options\",\n" //
            + "                    \"description\": \"A CHAR property wit options\",\n" //
            + "                    \"id\": \"CHAR.options\",\n" //
            + "                    \"type\": \"CHAR\",\n" //
            + "                    \"cardinality\": 0,\n" //
            + "                    \"defaultValue\": \"a\",\n" //
            + "                    \"isRequired\": true\n" //
            + "                },\n" //
            + "                {\n" //
            + "                    \"option\": [\n" //
            + "                        {\n" //
            + "                            \"label\": \"Choice 0, value : -10.21\",\n" //
            + "                            \"value\": \"-10.21\"\n" //
            + "                        },\n" //
            + "                        {\n" //
            + "                            \"label\": \"Choice 1, value : 20.123123\",\n" //
            + "                            \"value\": \"20.123123\"\n" //
            + "                        },\n" //
            + "                        {\n" //
            + "                            \"label\": \"Choice 2, value : 30.23123\",\n" //
            + "                            \"value\": \"30.23123\"\n" //
            + "                        }\n" //
            + "                    ],\n" //
            + "                    \"name\": \"DOUBLE property with options\",\n" //
            + "                    \"description\": \"A DOUBLE property wit options\",\n" //
            + "                    \"id\": \"DOUBLE.options\",\n" //
            + "                    \"type\": \"DOUBLE\",\n" //
            + "                    \"cardinality\": 0,\n" //
            + "                    \"defaultValue\": \"-10.21\",\n" //
            + "                    \"isRequired\": true\n" //
            + "                },\n" //
            + "                {\n" //
            + "                    \"option\": [\n" //
            + "                        {\n" //
            + "                            \"label\": \"Choice 0, value : 10.4\",\n" //
            + "                            \"value\": \"10.4\"\n" //
            + "                        },\n" //
            + "                        {\n" //
            + "                            \"label\": \"Choice 1, value : -12.4\",\n" //
            + "                            \"value\": \"-12.4\"\n" //
            + "                        },\n" //
            + "                        {\n" //
            + "                            \"label\": \"Choice 2, value : 0.5\",\n" //
            + "                            \"value\": \"0.5\"\n" //
            + "                        }\n" //
            + "                    ],\n" //
            + "                    \"name\": \"FLOAT property with options\",\n" //
            + "                    \"description\": \"A FLOAT property wit options\",\n" //
            + "                    \"id\": \"FLOAT.options\",\n" //
            + "                    \"type\": \"FLOAT\",\n" //
            + "                    \"cardinality\": 0,\n" //
            + "                    \"defaultValue\": \"10.4\",\n" //
            + "                    \"isRequired\": true\n" //
            + "                },\n" //
            + "                {\n" //
            + "                    \"option\": [\n" //
            + "                        {\n" //
            + "                            \"label\": \"Choice 0, value : -200000\",\n" //
            + "                            \"value\": \"-200000\"\n" //
            + "                        },\n" //
            + "                        {\n" //
            + "                            \"label\": \"Choice 1, value : 300000\",\n" //
            + "                            \"value\": \"300000\"\n" //
            + "                        },\n" //
            + "                        {\n" //
            + "                            \"label\": \"Choice 2, value : 10\",\n" //
            + "                            \"value\": \"10\"\n" //
            + "                        }\n" //
            + "                    ],\n" //
            + "                    \"name\": \"INTEGER property with options\",\n" //
            + "                    \"description\": \"A INTEGER property wit options\",\n" //
            + "                    \"id\": \"INTEGER.options\",\n" //
            + "                    \"type\": \"INTEGER\",\n" //
            + "                    \"cardinality\": 0,\n" //
            + "                    \"defaultValue\": \"-200000\",\n" //
            + "                    \"isRequired\": true\n" //
            + "                },\n" //
            + "                {\n" //
            + "                    \"option\": [\n" //
            + "                        {\n" //
            + "                            \"label\": \"Choice 0, value : -2147493648\",\n" //
            + "                            \"value\": \"-2147493648\"\n" //
            + "                        },\n" //
            + "                        {\n" //
            + "                            \"label\": \"Choice 1, value : 2147493647\",\n" //
            + "                            \"value\": \"2147493647\"\n" //
            + "                        },\n" //
            + "                        {\n" //
            + "                            \"label\": \"Choice 2, value : -34\",\n" //
            + "                            \"value\": \"-34\"\n" //
            + "                        }\n" //
            + "                    ],\n" //
            + "                    \"name\": \"LONG property with options\",\n" //
            + "                    \"description\": \"A LONG property wit options\",\n" //
            + "                    \"id\": \"LONG.options\",\n" //
            + "                    \"type\": \"LONG\",\n" //
            + "                    \"cardinality\": 0,\n" //
            + "                    \"defaultValue\": \"-2147493648\",\n" //
            + "                    \"isRequired\": true\n" //
            + "                },\n" //
            + "                {\n" //
            + "                    \"option\": [\n" //
            + "                        {\n" //
            + "                            \"label\": \"Choice 0, value : -20000\",\n" //
            + "                            \"value\": \"-20000\"\n" //
            + "                        },\n" //
            + "                        {\n" //
            + "                            \"label\": \"Choice 1, value : 20000\",\n" //
            + "                            \"value\": \"20000\"\n" //
            + "                        },\n" //
            + "                        {\n" //
            + "                            \"label\": \"Choice 2, value : 1\",\n" //
            + "                            \"value\": \"1\"\n" //
            + "                        }\n" //
            + "                    ],\n" //
            + "                    \"name\": \"SHORT property with options\",\n" //
            + "                    \"description\": \"A SHORT property wit options\",\n" //
            + "                    \"id\": \"SHORT.options\",\n" //
            + "                    \"type\": \"SHORT\",\n" //
            + "                    \"cardinality\": 0,\n" //
            + "                    \"defaultValue\": \"-20000\",\n" //
            + "                    \"isRequired\": true\n" //
            + "                }\n" //
            + "            ]\n" //
            + "        }";

    public static final String TEST_EMITTER_RECEIVER_DEFINITION = "{\n" //
            + "            \"factoryPid\": \"org.eclipse.kura.util.wire.test.TestEmitterReceiver\",\n"  //
            + "            \"minInputPorts\": 1,\n" //
            + "            \"maxInputPorts\": 1,\n" //
            + "            \"defaultInputPorts\": 1,\n" //
            + "            \"minOutputPorts\": 1,\n" //
            + "            \"maxOutputPorts\": 1,\n" //
            + "            \"defaultOutputPorts\": 1,\n" //
            + "            \"componentOCD\": []\n" //
            + "        }";

    public static final String TEST_DRIVER_DEFINITION = "{\n"
            + "            \"pid\": \"org.eclipse.kura.util.test.driver.ChannelDescriptorTestDriver\",\n" //
            + "            \"definition\": {\n" //
            + "                \"ad\": [\n" //
            + "                    {\n" //
            + "                        \"name\": \"Test Property\",\n" //
            + "                        \"description\": \"A test property\",\n" //
            + "                        \"id\": \"test.property\",\n" //
            + "                        \"type\": \"STRING\",\n" //
            + "                        \"cardinality\": 0,\n" //
            + "                        \"defaultValue\": \"test value\",\n" //
            + "                        \"isRequired\": true\n" //
            + "                    }\n" //
            + "                ],\n" //
            + "                \"name\": \"ChannelDescriptorTestDriver\",\n" //
            + "                \"description\": \"A driver for testing channel descriptor properties\",\n" //
            + "                \"id\": \"org.eclipse.kura.util.test.driver.ChannelDescriptorTestDriver\"\n" //
            + "            }\n" //
            + "        }";

    private Snippets() {
    }
}
