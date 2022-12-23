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
 *  Eurotech
 ******************************************************************************/
package org.eclipse.kura.wire.script.tools;

public class TestScripts {

    public static String invertBooleanOnProperty(String prop) {
        return "var x = input.getRecords()[0].getProperties().get('" + prop + "').getValue();\n"
                + "x = !x;\n"
                + "var output = new Array();\n"
                + "var outputMap = new Object();\n"
                + "outputMap['" + prop + "'] = newBooleanValue(x);\n"
                + "output[0] = newWireRecord(outputMap);";
    }

    public static String incrementIntegerOnProperty(String prop) {
        return "var x = input.getRecords()[0].getProperties().get('" + prop + "').getValue();\n"
                + "x++;\n"
                + "var output = new Array();\n"
                + "var outputMap = new Object();\n"
                + "outputMap['" + prop + "'] = newIntegerValue(x);\n"
                + "output[0] = newWireRecord(outputMap);";
    }

    public static String incrementDoubleOnProperty(String prop) {
        return "var x = input.getRecords()[0].getProperties().get('" + prop + "').getValue();\n"
                + "x++;\n"
                + "var output = new Array();\n"
                + "var outputMap = new Object();\n"
                + "outputMap['" + prop + "'] = newDoubleValue(x);\n"
                + "output[0] = newWireRecord(outputMap);";
    }

    public static String incrementLongOnProperty(String prop) {
        return "var x = input.getRecords()[0].getProperties().get('" + prop + "').getValue();\n"
                + "x++;\n"
                + "var output = new Array();\n"
                + "var outputMap = new Object();\n"
                + "outputMap['" + prop + "'] = newLongValue(x);\n"
                + "output[0] = newWireRecord(outputMap);";
    }

    public static String incrementFloatOnProperty(String prop) {
        return "var x = input.getRecords()[0].getProperties().get('" + prop + "').getValue();\n"
                + "x++;\n"
                + "var output = new Array();\n"
                + "var outputMap = new Object();\n"
                + "outputMap['" + prop + "'] = newFloatValue(x);\n"
                + "output[0] = newWireRecord(outputMap);";
    }

    public static String identityByteArrayOnProperty(String prop) {
        return "var x = input.getRecords()[0].getProperties().get('" + prop + "').getValue();\n"
                + "var output = new Array();\n"
                + "var outputMap = new Object();\n"
                + "outputMap['" + prop + "'] = newByteArrayValue(x);\n"
                + "output[0] = newWireRecord(outputMap);";
    }

    public static String appendStringOnProperty(String prop, String toAppend) {
        return "var x = input.getRecords()[0].getProperties().get('" + prop + "').getValue();\n"
                + "x = x.concat('" + toAppend + "');\n"
                + "var output = new Array();\n"
                + "var outputMap = new Object();\n"
                + "outputMap['" + prop + "'] = newStringValue(x);\n"
                + "output[0] = newWireRecord(outputMap);";
    }

    public static String comparePropsOnProperty(String prop, String prop2) {
        return "input.getRecords()[0].getProperties().get('" + prop + "').getValue()"
                + " === "
                + "input.getRecords()[0].getProperties().get('" + prop2 + "').getValue();";
    }

    public static String counter() {
        return "counter = typeof(counter) === 'undefined' ? 0 : counter;\n"
                + "counter++;\n"
                + "outputMap = new Object();\n"
                + "outputMap['counter'] = newIntegerValue(counter);\n"
                + "var output = new Array();\n"
                + "output[0] = newWireRecord(outputMap)";
    }

}
