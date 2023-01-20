/*******************************************************************************
 * Copyright (c) 2017, 2023 Eurotech and/or its affiliates and others
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
package org.eclipse.kura.internal.wire.publisher;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.eclipse.kura.KuraException;
import org.eclipse.kura.cloudconnection.listener.CloudConnectionListener;
import org.eclipse.kura.cloudconnection.listener.CloudDeliveryListener;
import org.eclipse.kura.cloudconnection.message.KuraMessage;
import org.eclipse.kura.core.testutil.TestUtil;
import org.eclipse.kura.message.KuraPayload;
import org.eclipse.kura.message.KuraPosition;
import org.eclipse.kura.position.NmeaPosition;
import org.eclipse.kura.position.PositionService;
import org.eclipse.kura.type.StringValue;
import org.eclipse.kura.type.TypedValue;
import org.eclipse.kura.wire.WireEnvelope;
import org.eclipse.kura.wire.WireHelperService;
import org.eclipse.kura.wire.WireRecord;
import org.junit.Test;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Filter;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CloudPublisherTest {

    private static final Logger logger = LoggerFactory.getLogger(CloudPublisherTest.class);

    private CloudPublisher cp;
    private org.eclipse.kura.cloudconnection.publisher.CloudPublisher cloudPublisherMock;
    private Map<String, Object> properties;
    private PositionService positionServiceMock;
    private FakeCloudPublisher fakeCloudPublisher;
    private Map<String, TypedValue<?>> recordProps;
    private KuraPayload payload;
    private KuraPosition position;
    private KuraMessage kuraMessage;
    private Map<String, Object> kuraMessageProps;

    @Test
    public void testOnWireReceive() throws InvalidSyntaxException, NoSuchFieldException, KuraException {
        // test publishing a normal message with topic replacement
        givenCloudPublisher();
        givenDefaultProperties();
        givenUpdatedProperties("publish.position", "none");
        givenActivatedComponentProperties();
        givenDefaultRecordProp();

        whenOnWireReceive();
        whenKuraMessageReceived();

        thenPayloadHasNullBody();
        thenPayloadHasNullPosition();
        thenTotalMetricReceived(2);
        thenCheckDefaultMetricReceived();
        thenTotalKuraMessagePropsReceived(2);
        thenCheckDefaultKuraMessageProps();
    }

    @Test
    public void testOnWireReceiveSetBody() throws InvalidSyntaxException, NoSuchFieldException, KuraException {
        // test publishing a normal message with topic replacement
        givenCloudPublisher();
        givenDefaultProperties();
        givenUpdatedProperties("publish.position", "none");
        givenBodyProperties("key", Boolean.FALSE);
        givenActivatedComponentProperties();
        givenDefaultRecordProp();

        whenOnWireReceive();
        whenKuraMessageReceived();

        thenPayloadHasNotNullBody();
        thenPayloadHasNullPosition();
        thenTotalMetricReceived(2);
        thenCheckDefaultMetricReceived();
        thenTotalKuraMessagePropsReceived(2);
        thenCheckDefaultKuraMessageProps();
    }

    @Test
    public void testOnWireReceiveSetBodyRemoveFromMetrics()
            throws InvalidSyntaxException, NoSuchFieldException, KuraException {
        // test publishing a normal message with topic replacement
        givenCloudPublisher();
        givenDefaultProperties();
        givenUpdatedProperties("publish.position", "none");
        givenBodyProperties("key", Boolean.TRUE);
        givenActivatedComponentProperties();
        givenDefaultRecordProp();

        whenOnWireReceive();
        whenKuraMessageReceived();

        thenPayloadHasNotNullBody();
        thenPayloadHasNullPosition();
        thenTotalMetricReceived(1);
        thenCheckOneMetricReceived();
        thenTotalKuraMessagePropsReceived(1);
        thenCheckOneKuraMessagePropsReceived();
    }

    @Test
    public void testOnWireReceiveWithBasicPosition()
            throws InvalidSyntaxException, NoSuchFieldException, KuraException {
        // test publishing a normal message with topic replacement and position
        givenCloudPublisher();
        givenDefaultProperties();
        givenUpdatedProperties("publish.position", "basic");
        givenActivatedComponentProperties();
        givenPositionServiceMock();
        givenDefaultRecordProp();

        whenSetPositionServiceMock();
        whenOnWireReceive();
        whenKuraMessageReceived();

        thenPayloadHasNullBody();
        thenCheckBasicPosition();
        thenTotalMetricReceived(2);
        thenCheckDefaultMetricReceived();
        thenTotalKuraMessagePropsReceived(2);
        thenCheckDefaultKuraMessageProps();
    }

    @Test
    public void testOnWireReceiveWithFullPosition() throws InvalidSyntaxException, NoSuchFieldException, KuraException {
        // test publishing a normal message with topic replacement and position
        givenCloudPublisher();
        givenDefaultProperties();
        givenUpdatedProperties("publish.position", "full");
        givenActivatedComponentProperties();
        givenPositionServiceMock();
        givenDefaultRecordProp();

        whenSetPositionServiceMock();
        whenOnWireReceive();
        whenKuraMessageReceived();

        thenPayloadHasNullBody();
        thenCheckFullPosition();
        thenTotalMetricReceived(2);
        thenCheckDefaultMetricReceived();
        thenTotalKuraMessagePropsReceived(2);
        thenCheckDefaultKuraMessageProps();
    }

    @Test
    public void testTopicReplacementOnWireReceive() throws InvalidSyntaxException, NoSuchFieldException, KuraException {
        // test publishing a normal message with topic replacement
        givenCloudPublisher();
        givenDefaultProperties();
        givenUpdatedProperties("publish.position", "none");
        givenUpdatedProperties("app.topic", "$assetName/$test");
        givenActivatedComponentProperties();
        givenDefaultRecordProp();
        givenAddedRecordProp("assetName", "testAsset");
        givenAddedRecordProp("test", "replaceTest");

        whenOnWireReceive();
        whenKuraMessageReceived();

        thenPayloadHasNullBody();
        thenPayloadHasNullPosition();
        thenTotalMetricReceived(4);
        thenCheckDefaultMetricReceived();
        thenCheckSemanticTopic();
        thenTotalKuraMessagePropsReceived(4);
        thenCheckDefaultKuraMessageProps();
    }

    /*
     * Steps
     */

    /*
     * Given steps
     */

    private void givenCloudPublisher() {
        this.cp = new CloudPublisher();
        WireHelperService wireHelperServiceMock = mock(WireHelperService.class);
        this.cp.bindWireHelperService(wireHelperServiceMock);
        this.fakeCloudPublisher = new FakeCloudPublisher() {

            private KuraMessage kmessage = new KuraMessage(null);

            @Override
            public String publish(KuraMessage message) throws KuraException {
                kmessage = message;
                return null;
            }

            @Override
            public void registerCloudConnectionListener(CloudConnectionListener cloudConnectionListener) {
                // TODO Auto-generated method stub

            }

            @Override
            public void unregisterCloudConnectionListener(CloudConnectionListener cloudConnectionListener) {
                // TODO Auto-generated method stub

            }

            @Override
            public void registerCloudDeliveryListener(CloudDeliveryListener cloudDeliveryListener) {
                // TODO Auto-generated method stub

            }

            @Override
            public void unregisterCloudDeliveryListener(CloudDeliveryListener cloudDeliveryListener) {
                // TODO Auto-generated method stub

            }

            @Override
            public KuraMessage getMessage() {
                return kmessage;
            }
        };
    }

    private void givenDefaultProperties() throws InvalidSyntaxException {
        this.properties = new HashMap<>();
        this.properties.put("CloudPublisher.target", "cspid");
    }

    private void givenUpdatedProperties(String key, String value) {
        this.properties.put(key, value);
    }

    private void givenBodyProperties(String setBodyFromProperty, Boolean removeBodyFromMetrics) {
        this.properties.put("set.body.from.property", setBodyFromProperty);
        this.properties.put("remove.body.from.metrics", removeBodyFromMetrics);
    }

    private void givenActivatedComponentProperties() throws InvalidSyntaxException {
        BundleContext bundleCtxMock = mock(BundleContext.class);
        Filter filter = mock(Filter.class);
        when(bundleCtxMock.createFilter(anyString())).thenReturn(filter);
        ComponentContext ctxMock = mock(ComponentContext.class);
        when(ctxMock.getBundleContext()).thenReturn(bundleCtxMock);
        this.cp.activate(ctxMock, this.properties);
        this.cp.setCloudPublisher(this.fakeCloudPublisher);
    }

    private void givenPositionServiceMock() {
        this.positionServiceMock = mock(PositionService.class);
    }

    private void givenFakeCloudPublisher() {

    }

    private void givenSetFakeCloudPublisher() {

    }

    private void givenDefaultRecordProp() {
        this.recordProps = new HashMap<>();
        this.recordProps.put("key", new StringValue("val"));
        this.recordProps.put("topic", new StringValue("my test topic"));
    }

    private void givenAddedRecordProp(String key, String stringValue) {
        this.recordProps.put(key, new StringValue(stringValue));
    }

    /*
     * When
     */

    private void whenOnWireReceive() {
        List<WireRecord> wireRecords = new ArrayList<>();

        WireRecord record = new WireRecord(this.recordProps);
        wireRecords.add(record);

        WireEnvelope wireEnvelope = new WireEnvelope("emitter", wireRecords);
        this.cp.onWireReceive(wireEnvelope);
    }

    private void whenSetPositionServiceMock() throws NoSuchFieldException {
        when(this.positionServiceMock.getNmeaPosition())
                .thenReturn(new NmeaPosition(0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0));
        TestUtil.setFieldValue(this.cp, "positionService", this.positionServiceMock);
    }

    private void whenKuraMessageReceived() {
        this.kuraMessage = this.fakeCloudPublisher.getMessage();
        this.kuraMessageProps = this.kuraMessage.getProperties();
        this.payload = this.kuraMessage.getPayload();
        this.position = this.payload.getPosition();
    }

    /*
     * Then
     */

    private void thenPayloadHasNullBody() {
        assertNull(this.payload.getBody());
    }

    private void thenPayloadHasNotNullBody() {
        assertNotNull(this.payload.getBody());
        byte[] body = this.payload.getBody();
        // byte[] decodedBody = Base64.getDecoder().decode(body);
        String stringBody = new String(body);
        assertEquals("val", stringBody);
    }

    private void thenPayloadHasNullPosition() {
        assertNull(this.payload.getPosition());
    }

    private void thenTotalMetricReceived(int numMetrics) {
        assertNotNull(this.payload.getTimestamp());
        assertNotNull(this.payload.metrics());
        assertEquals(numMetrics, this.payload.metrics().size());
    }

    private void thenCheckDefaultMetricReceived() {
        assertEquals("val", this.payload.getMetric("key"));
        assertEquals("my test topic", this.payload.getMetric("topic"));
    }

    private void thenCheckOneMetricReceived() {
        assertEquals("my test topic", this.payload.getMetric("topic"));
    }

    private void thenTotalKuraMessagePropsReceived(int numMetrics) {
        assertNotNull(this.kuraMessageProps);
        assertEquals(numMetrics, this.kuraMessageProps.size());
    }

    private void thenCheckDefaultKuraMessageProps() {
        assertEquals("val", this.kuraMessageProps.get("key"));
        assertEquals("my test topic", this.kuraMessageProps.get("topic"));
    }

    private void thenCheckOneKuraMessagePropsReceived() {
        assertEquals("my test topic", this.kuraMessageProps.get("topic"));
    }

    private void thenCheckSemanticTopic() {
        String appTopic = fillAppTopicPlaceholders("$assetName/$test", this.kuraMessage);
        assertEquals("testAsset/replaceTest", appTopic);
    }

    private void thenCheckBasicPosition() {
        assertNotNull(this.position);
        assertNotNull(this.position.getAltitude());
        assertNotNull(this.position.getLatitude());
        assertNotNull(this.position.getLongitude());
        assertNull(this.position.getSatellites());
    }

    private void thenCheckFullPosition() {
        assertNotNull(this.position.getAltitude());
        assertNotNull(this.position.getLatitude());
        assertNotNull(this.position.getLongitude());
        assertNotNull(this.position.getHeading());
        assertNotNull(this.position.getPrecision());
        assertNotNull(this.position.getSpeed());
        assertNotNull(this.position.getSatellites());
    }
    /*
     * Utilities
     */

    public interface FakeCloudPublisher extends org.eclipse.kura.cloudconnection.publisher.CloudPublisher {

        public KuraMessage getMessage();
    }

    private String fillAppTopicPlaceholders(String appTopic, KuraMessage message) {
        String TOPIC_PATTERN_STRING = "\\$([^\\s/]+)";
        Pattern TOPIC_PATTERN = Pattern.compile(TOPIC_PATTERN_STRING);
        Matcher matcher = TOPIC_PATTERN.matcher(appTopic);
        StringBuffer buffer = new StringBuffer();

        while (matcher.find()) {
            Map<String, Object> properties = message.getProperties();
            if (properties.containsKey(matcher.group(1))) {
                String replacement = matcher.group(0);

                Object value = properties.get(matcher.group(1));
                if (replacement != null) {
                    matcher.appendReplacement(buffer, value.toString());
                }
            }
        }
        matcher.appendTail(buffer);
        return buffer.toString();
    }

}
