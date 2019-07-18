package org.eclipse.kura.internal.driver.ble.xdk;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Matchers.anyObject;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Consumer;

import org.eclipse.kura.KuraBluetoothConnectionException;
import org.eclipse.kura.KuraBluetoothIOException;
import org.eclipse.kura.KuraBluetoothResourceNotFoundException;
import org.eclipse.kura.KuraErrorCode;
import org.eclipse.kura.KuraException;
import org.eclipse.kura.bluetooth.le.BluetoothLeDevice;
import org.eclipse.kura.bluetooth.le.BluetoothLeGattCharacteristic;
import org.eclipse.kura.bluetooth.le.BluetoothLeGattService;
import org.eclipse.kura.core.testutil.TestUtil;
import org.eclipse.kura.driver.Driver.ConnectionException;
import org.junit.Test;

public class XdkTest {

    private static final double EPS = 0.01;

    @Test
    public void testConnectionFailure()
            throws KuraBluetoothConnectionException, KuraBluetoothResourceNotFoundException, ConnectionException {

        ImplementationXdk impXdk = new ImplementationXdk();

        BluetoothLeDevice deviceMock = impXdk.getBuilder().getDevice();

        doThrow(new KuraBluetoothConnectionException(KuraErrorCode.BLE_IO_ERROR)).when(deviceMock).connect();

        verify(deviceMock, times(1)).connect();
        verify(deviceMock, times(2)).isConnected();

        when(deviceMock.isConnected()).thenReturn(false);
        impXdk.getXdk().disconnect();

        verify(deviceMock, times(1)).disconnect();
    }

    @Test
    public void testConnectDisconnect() throws KuraBluetoothConnectionException, KuraBluetoothResourceNotFoundException,
            NoSuchFieldException, ConnectionException {

        ImplementationXdk impXdk = new ImplementationXdk();

        BluetoothLeDevice deviceMock = impXdk.getXdk().getBluetoothLeDevice();
        verify(deviceMock, times(1)).connect();

        Map<String, XdkGattResources> gattResources = (Map<String, XdkGattResources>) TestUtil
                .getFieldValue(impXdk.getXdk(), "gattResources");

        assertNotNull(gattResources);
        assertEquals(5, gattResources.size());
        assertEquals(impXdk.getControlSvcMock(), gattResources.get("sensor").getGattService());
        assertEquals(impXdk.getDataSvcMock(), gattResources.get("high priority array").getGattService());

        when(deviceMock.isConnected()).thenReturn(false);
        impXdk.getXdk().disconnect();

        verify(deviceMock, times(1)).disconnect();
    }

    @Test
    public void testDiscoverServicesEmpty() throws KuraBluetoothResourceNotFoundException, KuraBluetoothIOException {
        XdkBuilder builder = new XdkBuilder(true)
                .addService(XdkGatt.UUID_XDK_CONTROL_SERVICE, mock(BluetoothLeGattService.class))
                .addService(XdkGatt.UUID_XDK_HIGH_DATA_RATE, mock(BluetoothLeGattService.class));

        Xdk xdk = builder.build(true);

        List<BluetoothLeGattCharacteristic> characteristics = xdk.getCharacteristics();
        assertEquals(0, characteristics.size());
    }

    @Test
    public void testDiscoverServices() throws KuraBluetoothResourceNotFoundException, KuraBluetoothIOException {
        BluetoothLeGattService infoSvcMock = mock(BluetoothLeGattService.class);
        List<BluetoothLeGattCharacteristic> isChs = new ArrayList<>();
        isChs.add(mock(BluetoothLeGattCharacteristic.class));
        isChs.add(mock(BluetoothLeGattCharacteristic.class));
        when(infoSvcMock.findCharacteristics()).thenReturn(isChs);

        BluetoothLeGattService optoSvcMock = mock(BluetoothLeGattService.class);
        List<BluetoothLeGattCharacteristic> osChs = new ArrayList<>();
        osChs.add(mock(BluetoothLeGattCharacteristic.class));
        when(optoSvcMock.findCharacteristics()).thenReturn(osChs);

        XdkBuilder builder = new XdkBuilder(true)
                .addService(XdkGatt.UUID_XDK_CONTROL_SERVICE, mock(BluetoothLeGattService.class))
                .addService(XdkGatt.UUID_XDK_HIGH_DATA_RATE, mock(BluetoothLeGattService.class));

        Xdk xdk = builder.build(true);

        List<BluetoothLeGattCharacteristic> characteristics = xdk.getCharacteristics();
        assertEquals(0, characteristics.size());
    }

    @Test
    public void testEnableData() throws Throwable {

        ImplementationXdk impXdk = new ImplementationXdk();

        testEnable(impXdk.getStartChrMock(), impXdk.getXdk(), "startSensor");
    }

    @Test
    public void testEnableRate() throws Throwable {

        ImplementationXdk impXdk = new ImplementationXdk();

        testEnableRate(impXdk.getRateChrMock(), impXdk.getXdk(), "startSensor");
    }

    @Test
    public void testEnableQuaternion() throws Throwable {

        ImplementationXdk impXdk = new ImplementationXdk();

        testEnableFusion(impXdk.getFusionChrMock(), impXdk.getXdk(), "startSensor");
    }

    @Test
    public void testDisableQuaternion() throws Throwable {

        ImplementationXdk impXdk = new ImplementationXdk();

        testDisableFusion(impXdk.getFusionChrMock(), impXdk.getXdk(), "startSensor");
    }

    @Test(expected = KuraBluetoothResourceNotFoundException.class)
    public void testReadLowDataFail() throws KuraException {

        BluetoothLeGattService lowDataSvcMock = mock(BluetoothLeGattService.class);
        BluetoothLeGattCharacteristic lowDataChrMock = mock(BluetoothLeGattCharacteristic.class);

        doThrow(new KuraBluetoothResourceNotFoundException("test")).when(lowDataSvcMock)
                .findCharacteristic(XdkGatt.UUID_XDK_HIGH_DATA_RATE_LOW_PRIORITY_ARREY);

        Xdk xdk = new XdkBuilder(true).addService(XdkGatt.UUID_XDK_HIGH_DATA_RATE, lowDataSvcMock)
                .addCharacteristic(XdkGatt.UUID_XDK_HIGH_DATA_RATE, XdkGatt.UUID_XDK_HIGH_DATA_RATE_LOW_PRIORITY_ARREY,
                        lowDataChrMock)
                .build(true);

        byte message = 0x01;

        xdk.readLowData(message);

    }

    @Test
    public void testReadHighData() throws Throwable {

        ImplementationXdk impXdk = new ImplementationXdk();

        float[] expected = new float[] { 11, 22, 1033, 44, 55, 66, 0, 0, 0, 0 };

        testRead(ImplementationXdk.HIGHDATABYTE, expected, impXdk.getXdk(), impXdk.getHighChrMock(),
                impXdk.getDataSvcMock(), "readHighData");
    }

    @Test
    public void testReadHighDataQuaternion() throws Throwable {

        boolean enableQuaternion = true;

        ImplementationXdk impXdk = new ImplementationXdk((byte) 0x01, enableQuaternion);

        float[] expected = new float[] { 0, 0, 0, 0, 0, 0, (float) 0.93681324, (float) 0.013107585, (float) 0.01862044,
                (float) -0.34908798 };

        testRead(ImplementationXdk.HIGHDATABYTE, expected, impXdk.getXdk(), impXdk.getHighChrMock(),
                impXdk.getDataSvcMock(), "readHighData");
    }

    @Test
    public void testReadLowDataMessageOne() throws Throwable {

        byte message = 0x01;

        ImplementationXdk impXdk = new ImplementationXdk(message, false);

        Integer[] expected = new Integer[] { 336, 0, 98216, 23, 46, 0, 0 };

        testRead(ImplementationXdk.LOWDATABYTE_MESSAGE_ONE, expected, impXdk.getXdk(), impXdk.getLowChrMock(),
                impXdk.getDataSvcMock(), "readLowData", message);
    }

    @Test
    public void testReadLowDataMessageTwo() throws Throwable {

        byte message = 0x02;

        ImplementationXdk impXdk = new ImplementationXdk(message, false);

        Integer[] expected = new Integer[] { -22, -13, -62, 6305, 2, 0, 0 };

        testRead(ImplementationXdk.LOWDATABYTE_MESSAGE_TWO, expected, impXdk.getXdk(), impXdk.getLowChrMock(),
                impXdk.getDataSvcMock(), "readLowData", message);
    }

    @Test
    public void testEnableHighNotifications() throws Throwable {
        ImplementationXdk impXdk = new ImplementationXdk();

        float[] expected = new float[] { 11, 22, 1033, 44, 55, 66, 0, 0, 0, 0 };

        testEnableNotifications(ImplementationXdk.HIGHDATABYTE, expected, impXdk.getXdk(), impXdk.getHighChrMock(),
                impXdk.getDataSvcMock(), "enableHighNotifications");
    }

    @Test
    public void testEnableLowNotificationsMessageOne() throws Throwable {
        ImplementationXdk impXdk = new ImplementationXdk();

        Integer[] expected = new Integer[] { 336, 0, 98216, 23, 46, 0, 0 };

        byte message = 0x01;

        testEnableNotifications(ImplementationXdk.LOWDATABYTE_MESSAGE_ONE, expected, impXdk.getXdk(),
                impXdk.getLowChrMock(), impXdk.getDataSvcMock(), "enableLowNotifications", message);

    }

    @Test
    public void testEnableLowNotificationsMessageTwo() throws Throwable {
        ImplementationXdk impXdk = new ImplementationXdk();

        Integer[] expected = new Integer[] { -22, -13, -62, 6305, 2, 0, 0 };

        byte message = 0x02;

        testEnableNotifications(ImplementationXdk.LOWDATABYTE_MESSAGE_TWO, expected, impXdk.getXdk(),
                impXdk.getLowChrMock(), impXdk.getDataSvcMock(), "enableLowNotifications", message);

    }

    @Test
    public void testDisableHighNotifications() throws Throwable {
        ImplementationXdk impXdk = new ImplementationXdk();
        testDisableNotifications(impXdk.getXdk(), impXdk.getHighChrMock(), impXdk.getDataSvcMock(),
                "disableHighNotifications");
    }

    @Test
    public void testDisableLowNotifications() throws Throwable {
        ImplementationXdk impXdk = new ImplementationXdk();
        testDisableNotifications(impXdk.getXdk(), impXdk.getLowChrMock(), impXdk.getDataSvcMock(),
                "disableLowNotifications");
    }

    private void testEnable(BluetoothLeGattCharacteristic bch, Xdk xdk, String method) throws Throwable {

        TestUtil.invokePrivate(xdk, method, new Class<?>[] { boolean.class, int.class }, false, 10);

        byte[] config = new byte[] { 0x01 };

        verify(bch, times(1)).writeValue(config);
    }

    private void testEnableRate(BluetoothLeGattCharacteristic bch, Xdk xdk, String method) throws Throwable {

        TestUtil.invokePrivate(xdk, method, new Class<?>[] { boolean.class, int.class }, false, 10);

        byte[] config = new byte[] { (byte) 0x0A, 0x00, 0x00, 0x00 };

        verify(bch, times(1)).writeValue(config);
    }

    private void testEnableFusion(BluetoothLeGattCharacteristic bch, Xdk xdk, String method) throws Throwable {

        TestUtil.invokePrivate(xdk, method, new Class<?>[] { boolean.class, int.class }, true, 10);

        byte[] config = new byte[] { 0x01 };

        verify(bch, times(1)).writeValue(config);
    }

    private void testDisableFusion(BluetoothLeGattCharacteristic bch, Xdk xdk, String method) throws Throwable {

        TestUtil.invokePrivate(xdk, method, new Class<?>[] { boolean.class, int.class }, false, 10);

        byte[] config = new byte[] { 0x00 };

        verify(bch, times(1)).writeValue(config);
    }

    private void testRead(byte[] val, float[] expected, Xdk xdk, BluetoothLeGattCharacteristic bch,
            BluetoothLeGattService service, String method) throws Throwable {

        testRead(val, expected, xdk, method, (e, a) -> assertArrayEquals(e, a, (float) EPS));
    }

    private void testRead(byte[] val, Integer[] expected, Xdk xdk, BluetoothLeGattCharacteristic bch,
            BluetoothLeGattService service, String method, byte b) throws Throwable {

        testRead(val, expected, xdk, method, b, (e, a) -> assertArrayEquals(e, a));
    }

    private <T> void testRead(byte[] val, T expected, Xdk xdk, String method, Assertion<T> assertion) throws Throwable {

        assertion.assertEq(expected, (T) TestUtil.invokePrivate(xdk, method));
    }

    private <T> void testRead(byte[] val, T expected, Xdk xdk, String method, byte b, Assertion<T> assertion)
            throws Throwable {

        assertion.assertEq(expected, (T) TestUtil.invokePrivate(xdk, method, b));
    }

    private void testEnableNotifications(byte[] val, float[] expected, Xdk xdk,
            BluetoothLeGattCharacteristic characteristic, BluetoothLeGattService service, String method)
            throws Throwable {

        Consumer<float[]> callback = t -> assertArrayEquals(expected, t, (float) EPS);

        testEnableNotifications(val, expected, xdk, characteristic, service, method, callback);
    }

    private void testEnableNotifications(byte[] val, Integer[] expected, Xdk xdk,
            BluetoothLeGattCharacteristic characteristic, BluetoothLeGattService service, String method, byte b)
            throws Throwable {

        Consumer<Integer[]> callback = t -> assertArrayEquals(expected, t);

        testEnableNotifications(val, expected, xdk, characteristic, service, method, callback, b);
    }

    private <T> void testEnableNotifications(byte[] val, T expected, Xdk xdk, BluetoothLeGattCharacteristic bch,
            BluetoothLeGattService service, String method, Consumer<T> callback) throws Throwable {

        doAnswer(invocation -> {
            Consumer<byte[]> consumer = invocation.getArgumentAt(0, Consumer.class);
            consumer.accept(val);
            return null;
        }).when(bch).enableValueNotifications(anyObject());

        TestUtil.invokePrivate(xdk, method, callback);
    }

    private <T> void testEnableNotifications(byte[] val, T expected, Xdk xdk, BluetoothLeGattCharacteristic bch,
            BluetoothLeGattService service, String method, Consumer<T> callback, byte b) throws Throwable {

        doAnswer(invocation -> {
            Consumer<byte[]> consumer = invocation.getArgumentAt(0, Consumer.class);
            consumer.accept(val);
            return null;
        }).when(bch).enableValueNotifications(anyObject());

        TestUtil.invokePrivate(xdk, method, callback, b);
    }

    private void testDisableNotifications(Xdk xdk, BluetoothLeGattCharacteristic bch, BluetoothLeGattService service,
            String method) throws Throwable {

        TestUtil.invokePrivate(xdk, method);

        verify(bch, times(1)).disableValueNotifications();
    }

}

class ImplementationXdk {

    public static final byte MESSAGE_ONE = 0x01;
    public static final byte MESSAGE_TWO = 0x02;

    public static final UUID UUID_XDK_CONTROL_SERVICE_CONTROL_NODE_USE_SENSOR_FUSION = UUID
            .fromString("55b741d5-7ada-11e4-82f8-0800200c9a66");

    public static final byte[] HIGHDATABYTE = { (byte) 0x0B, (byte) 0x00, (byte) 0x16, (byte) 0x00, (byte) 0x09,
            (byte) 0x04, (byte) 0x2C, (byte) 0x00, (byte) 0x37, (byte) 0x00, (byte) 0x42, (byte) 0x00, (byte) 0x00,
            (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00 };

    public static final byte[] HIGHDATABYTE_QUATERNION = { (byte) 0xfe, (byte) 0xd2, (byte) 0x6f, (byte) 0x3f,
            (byte) 0x32, (byte) 0xc1, (byte) 0x56, (byte) 0x3c, (byte) 0xe5, (byte) 0x89, (byte) 0x98, (byte) 0x3c,
            (byte) 0xa9, (byte) 0xbb, (byte) 0xb2, (byte) 0xbe, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00 };

    public static final byte[] LOWDATABYTE_MESSAGE_ONE = { (byte) 0x01, (byte) 0x40, (byte) 0x24, (byte) 0x05,
            (byte) 0x00, (byte) 0x00, (byte) 0xa8, (byte) 0x7f, (byte) 0x01, (byte) 0x00, (byte) 0xbd, (byte) 0x5c,
            (byte) 0x00, (byte) 0x00, (byte) 0x2e, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00 };

    public static final byte[] LOWDATABYTE_MESSAGE_TWO = { (byte) 0x02, (byte) 0xea, (byte) 0xff, (byte) 0xf3,
            (byte) 0xff, (byte) 0xc2, (byte) 0xff, (byte) 0xa1, (byte) 0x18, (byte) 0x02, (byte) 0x00, (byte) 0x00,
            (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x01, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00 };

    private BluetoothLeGattService controlSvcMock;
    private BluetoothLeGattService dataSvcMock;

    private BluetoothLeGattCharacteristic startChrMock;
    private BluetoothLeGattCharacteristic rateChrMock;
    private BluetoothLeGattCharacteristic fusionChrMock;
    private BluetoothLeGattCharacteristic highChrMock;
    private BluetoothLeGattCharacteristic lowChrMock;

    private XdkBuilder builder;
    private Xdk xdk;

    public ImplementationXdk() throws KuraBluetoothResourceNotFoundException {
        controlSvcMock = mock(BluetoothLeGattService.class);

        dataSvcMock = mock(BluetoothLeGattService.class);

        startChrMock = mock(BluetoothLeGattCharacteristic.class);
        rateChrMock = mock(BluetoothLeGattCharacteristic.class);
        fusionChrMock = mock(BluetoothLeGattCharacteristic.class);

        highChrMock = mock(BluetoothLeGattCharacteristic.class);
        lowChrMock = mock(BluetoothLeGattCharacteristic.class);

        builder = new XdkBuilder(true).addService(XdkGatt.UUID_XDK_HIGH_DATA_RATE, dataSvcMock)
                .addCharacteristic(XdkGatt.UUID_XDK_HIGH_DATA_RATE, XdkGatt.UUID_XDK_HIGH_DATA_RATE_HIGH_PRIORITY_ARREY,
                        highChrMock)
                .addCharacteristic(XdkGatt.UUID_XDK_HIGH_DATA_RATE, XdkGatt.UUID_XDK_HIGH_DATA_RATE_LOW_PRIORITY_ARREY,
                        lowChrMock)
                .addService(XdkGatt.UUID_XDK_CONTROL_SERVICE, controlSvcMock)
                .addCharacteristic(XdkGatt.UUID_XDK_CONTROL_SERVICE,
                        XdkGatt.UUID_XDK_CONTROL_SERVICE_START_SENSOR_SAMPLING_AND_NOTIFICATION, startChrMock)
                .addCharacteristic(XdkGatt.UUID_XDK_CONTROL_SERVICE,
                        XdkGatt.UUID_XDK_CONTROL_SERVICE_CHANGE_SENSOR_SAMPLING_RATA, rateChrMock)
                .addCharacteristic(XdkGatt.UUID_XDK_CONTROL_SERVICE,
                        UUID_XDK_CONTROL_SERVICE_CONTROL_NODE_USE_SENSOR_FUSION, fusionChrMock);

        try {
            when(highChrMock.readValue()).thenReturn(HIGHDATABYTE);
        } catch (KuraBluetoothIOException e) {
            e.printStackTrace();
        }

        xdk = builder.build(true);
    }

    public ImplementationXdk(byte message, boolean enableQuaternion) throws Throwable {
        controlSvcMock = mock(BluetoothLeGattService.class);

        dataSvcMock = mock(BluetoothLeGattService.class);

        startChrMock = mock(BluetoothLeGattCharacteristic.class);
        rateChrMock = mock(BluetoothLeGattCharacteristic.class);
        fusionChrMock = mock(BluetoothLeGattCharacteristic.class);

        highChrMock = mock(BluetoothLeGattCharacteristic.class);
        lowChrMock = mock(BluetoothLeGattCharacteristic.class);

        builder = new XdkBuilder(true).addService(XdkGatt.UUID_XDK_HIGH_DATA_RATE, dataSvcMock)
                .addCharacteristic(XdkGatt.UUID_XDK_HIGH_DATA_RATE, XdkGatt.UUID_XDK_HIGH_DATA_RATE_HIGH_PRIORITY_ARREY,
                        highChrMock)
                .addCharacteristic(XdkGatt.UUID_XDK_HIGH_DATA_RATE, XdkGatt.UUID_XDK_HIGH_DATA_RATE_LOW_PRIORITY_ARREY,
                        lowChrMock)
                .addService(XdkGatt.UUID_XDK_CONTROL_SERVICE, controlSvcMock)
                .addCharacteristic(XdkGatt.UUID_XDK_CONTROL_SERVICE,
                        XdkGatt.UUID_XDK_CONTROL_SERVICE_START_SENSOR_SAMPLING_AND_NOTIFICATION, startChrMock)
                .addCharacteristic(XdkGatt.UUID_XDK_CONTROL_SERVICE,
                        XdkGatt.UUID_XDK_CONTROL_SERVICE_CHANGE_SENSOR_SAMPLING_RATA, rateChrMock)
                .addCharacteristic(XdkGatt.UUID_XDK_CONTROL_SERVICE,
                        UUID_XDK_CONTROL_SERVICE_CONTROL_NODE_USE_SENSOR_FUSION, fusionChrMock);

        try {
            if (enableQuaternion) {
                when(highChrMock.readValue()).thenReturn(HIGHDATABYTE_QUATERNION);
            } else {
                when(highChrMock.readValue()).thenReturn(HIGHDATABYTE);
            }

            if (message == MESSAGE_ONE) {
                when(lowChrMock.readValue()).thenReturn(LOWDATABYTE_MESSAGE_ONE);
            }
            if (message == MESSAGE_TWO) {
                when(lowChrMock.readValue()).thenReturn(LOWDATABYTE_MESSAGE_TWO);
            }
        } catch (KuraBluetoothIOException e) {
            e.printStackTrace();
        }

        xdk = builder.build(true, enableQuaternion);
    }

    public BluetoothLeGattService getControlSvcMock() {
        return controlSvcMock;
    }

    public BluetoothLeGattService getDataSvcMock() {
        return dataSvcMock;
    }

    public BluetoothLeGattCharacteristic getStartChrMock() {
        return startChrMock;
    }

    public BluetoothLeGattCharacteristic getRateChrMock() {
        return rateChrMock;
    }

    public BluetoothLeGattCharacteristic getFusionChrMock() {
        return fusionChrMock;
    }

    public BluetoothLeGattCharacteristic getHighChrMock() {
        return highChrMock;
    }

    public BluetoothLeGattCharacteristic getLowChrMock() {
        return lowChrMock;
    }

    public XdkBuilder getBuilder() {
        return builder;
    }

    public Xdk getXdk() {
        return xdk;
    }

}

class XdkBuilder {

    private BluetoothLeDevice deviceMock;

    public XdkBuilder() {
        this(false);
    }

    public XdkBuilder(boolean connected) {
        deviceMock = mock(BluetoothLeDevice.class);

        if (connected) {
            when(deviceMock.isConnected()).thenReturn(true);
            when(deviceMock.isServicesResolved()).thenReturn(true);
        }
    }

    public XdkBuilder addService(UUID id, BluetoothLeGattService service)
            throws KuraBluetoothResourceNotFoundException {

        when(deviceMock.findService(id)).thenReturn(service);

        return this;
    }

    public XdkBuilder addCharacteristic(UUID serviceId, UUID CharacteristicId,
            BluetoothLeGattCharacteristic characteristic) throws KuraBluetoothResourceNotFoundException {

        when(deviceMock.findService(serviceId).findCharacteristic(CharacteristicId)).thenReturn(characteristic);

        return this;
    }

    public BluetoothLeDevice getDevice() {
        return this.deviceMock;
    }

    public Xdk build(boolean connect) {
        Xdk xdk = new Xdk(deviceMock);

        try {
            if (connect) {
                xdk.connect();
            }
            xdk.init();
        } catch (ConnectionException e) {
            // Do nothing
        }

        return xdk;
    }

    public Xdk build(boolean connect, boolean quaternion) throws Throwable {
        Xdk xdk = new Xdk(deviceMock);

        try {
            if (connect) {
                xdk.connect();
            }
            xdk.init();
        } catch (ConnectionException e) {
            // Do nothing
        }
        TestUtil.invokePrivate(xdk, "startSensor", new Class<?>[] { boolean.class, int.class }, quaternion, 10);

        return xdk;
    }
}

interface Assertion<T> {

    void assertEq(T expected, T actual);
}
