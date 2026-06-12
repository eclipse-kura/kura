/*******************************************************************************
 * Copyright (c) 2024 Eurotech and/or its affiliates and others
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
package org.eclipse.kura.cloudconnection.sparkplug.mqtt.endpoint.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;

import org.eclipse.kura.cloudconnection.sparkplug.mqtt.endpoint.SubscriptionRecord;
import org.junit.Test;

public class SubscriptionRecordTest {

    private SubscriptionRecord record1;
    private SubscriptionRecord record2;
    private SubscriptionRecord record3;
    private Object otherType;

    /*
     * Scenarios
     */

    @Test
    public void equalsShouldBeReflective() {
        whenInitSubscriptionRecord1("t1", 0);

        thenObjectsAreEqual(this.record1, this.record1);
        thenHashCodesAreSame(this.record1.hashCode(), this.record1.hashCode());
    }

    @Test
    public void equalsShouldBeSymmetric() {
        whenInitSubscriptionRecord1("t1", 0);
        whenInitSubscriptionRecord2("t1", 0);

        thenObjectsAreEqual(this.record1, this.record2);
        thenHashCodesAreSame(this.record1.hashCode(), this.record2.hashCode());
    }

    @Test
    public void equalsShouldBeTransitive() {
        whenInitSubscriptionRecord1("t1", 0);
        whenInitSubscriptionRecord2("t1", 0);
        whenInitSubscriptionRecord3("t1", 0);

        thenObjectsAreEqual(this.record1, this.record2);
        thenObjectsAreEqual(this.record2, this.record3);
        thenObjectsAreEqual(this.record1, this.record3);
        thenHashCodesAreSame(this.record1.hashCode(), this.record2.hashCode());
        thenHashCodesAreSame(this.record2.hashCode(), this.record3.hashCode());
        thenHashCodesAreSame(this.record1.hashCode(), this.record3.hashCode());
    }

    @Test
    public void shouldNotEqualWithDifferentTopic() {
        whenInitSubscriptionRecord1("t1", 0);
        whenInitSubscriptionRecord2("t2", 0);
        
        thenObjectsAreNotEqual(this.record1, this.record2);
        thenHashCodesAreDifferent(this.record1.hashCode(), this.record2.hashCode());
    }

    @Test
    public void shouldNotEqualWithDifferentQos() {
        whenInitSubscriptionRecord1("t1", 0);
        whenInitSubscriptionRecord2("t1", 1);

        thenObjectsAreNotEqual(this.record1, this.record2);
        thenHashCodesAreDifferent(this.record1.hashCode(), this.record2.hashCode());
    }

    @Test
    public void shouldNotEqualWithDifferentType() {
        whenInitSubscriptionRecord1("t1", 0);
        whenInitOtherTypeObject();

        thenObjectsAreNotEqual(this.record1, this.otherType);
        thenHashCodesAreDifferent(this.record1.hashCode(), this.otherType.hashCode());
    }

    /*
     * Steps
     */

    private void whenInitSubscriptionRecord1(String topicFilter, int qos) {
        this.record1 = new SubscriptionRecord(topicFilter, qos);
    }

    private void whenInitSubscriptionRecord2(String topicFilter, int qos) {
        this.record2 = new SubscriptionRecord(topicFilter, qos);
    }

    private void whenInitSubscriptionRecord3(String topicFilter, int qos) {
        this.record3 = new SubscriptionRecord(topicFilter, qos);
    }

    private void whenInitOtherTypeObject() {
        this.otherType = new Object();
    }

    private void thenObjectsAreEqual(Object o1, Object o2) {
        assertEquals(o1, o2);
    }

    private void thenObjectsAreNotEqual(Object o1, Object o2) {
        assertNotEquals(o1, o2);
    }

    private void thenHashCodesAreSame(int hash1, int hash2) {
        assertEquals(hash1, hash2);
    }

    private void thenHashCodesAreDifferent(int hash1, int hash2) {
        assertNotEquals(hash1, hash2);
    }

}
