package org.eclipse.kura.web.server.ublox;

/** class name : BluetoothLeGattServiceImpl
 * implement the BluetoothLeGattService Interface
 *  @author NarcisseKAPDJOU
 *  @version 1.0.1
 */
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class BluetoothLeGattServiceImpl implements BluetoothLeGattService {

    private Map<UUID, BluetoothLeServiceImpl> m_services = null;
    private List<BluetoothLeServiceImpl> l_serviesByUuid = null;

    public BluetoothLeGattServiceImpl() {
        this.m_services = new HashMap<UUID, BluetoothLeServiceImpl>();

    }

    public BluetoothLeGattServiceImpl(UUID uuid) {
        this.l_serviesByUuid = new ArrayList<BluetoothLeServiceImpl>();
    }

    /*
     * @Override
     * public BluetoothLeGattCharacteristic findCharacteristic(UUID uuid) throws Exception {
     * // TODO Auto-generated method stub
     * return null;
     * }
     * 
     * @Override
     * public List<BluetoothLeGattCharacteristic> findCharacteristics() throws Exception {
     * // TODO Auto-generated method stub
     * return null;
     * }
     */
    @Override
    public UUID getUUID() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public BluetoothLeDevice getDevice() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public boolean isPrimary() {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public BluetoothLeGattCharacteristic findCharacteristic(UUID uuid) throws Exception {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public List<BluetoothLeGattCharacteristic> findCharacteristics() throws Exception {
        // TODO Auto-generated method stub
        return null;
    }

    public Map<UUID, BluetoothLeServiceImpl> getMapServices() {
        return m_services;
    }

    public List<BluetoothLeServiceImpl> getListServicesByUuid() {
        return l_serviesByUuid;
    }

    public void addService(BluetoothLeServiceImpl service) {
        m_services.put(service.getUuid(), service);
    }

    public void addServiceByUuid(BluetoothLeServiceImpl servicebyUuid) {
        l_serviesByUuid.add(servicebyUuid);
    }

    /*
     * print list of services on the console
     * 
     */
    public void listServicesOnReslut() {
        for (BluetoothLeServiceImpl service : UBloxCommand.getListServices().getMapServices().values()) {
            System.out.println(service.getConnHandle() + "," + service.getStart() + "," + service.getEnd() + ","
                    + service.getUuid());
        }

    }

    /*
     * print list service by uuid on the console
     * 
     */
    public void listServiceByUuidOnresult() {
        for (BluetoothLeServiceImpl service : UBloxCommand.getListServicesByUuid().getListServicesByUuid()) {
            System.out.println(service.getConnHandle() + "," + service.getStart() + "," + service.getEnd());
        }
    }

    /*
     * print characteristic service on the console
     * 
     */
    public void characteristicServiceOnResult() {
        System.out.println(UBloxCommand.getCharacteristic().getConnHandle() + ","
                + UBloxCommand.getCharacteristic().getAttHandle() + ","
                + UBloxCommand.getCharacteristic().getProperties() + "," + UBloxCommand.getCharacteristic().getUuid());

    }

}
