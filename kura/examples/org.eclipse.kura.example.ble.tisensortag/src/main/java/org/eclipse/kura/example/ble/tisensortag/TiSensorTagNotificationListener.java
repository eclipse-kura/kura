package org.eclipse.kura.example.ble.tisensortag;

import java.util.Map;

public interface TiSensorTagNotificationListener {

    public void notify(String address, Map<String, Object> values);

}
