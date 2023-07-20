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
package fi.w1.wpa_supplicant1;

import java.util.List;
import java.util.Map;

import org.freedesktop.dbus.DBusPath;
import org.freedesktop.dbus.TypeRef;
import org.freedesktop.dbus.annotations.DBusInterfaceName;
import org.freedesktop.dbus.annotations.DBusProperty;
import org.freedesktop.dbus.annotations.DBusProperty.Access;
import org.freedesktop.dbus.exceptions.DBusException;
import org.freedesktop.dbus.interfaces.DBusInterface;
import org.freedesktop.dbus.messages.DBusSignal;
import org.freedesktop.dbus.types.UInt32;
import org.freedesktop.dbus.types.Variant;

/**
 * Auto-generated class.
 */
@DBusInterfaceName("fi.w1.wpa_supplicant1.Interface")
@DBusProperty(name = "Capabilities", type = Interface.PropertyCapabilitiesType.class, access = Access.READ)
@DBusProperty(name = "State", type = String.class, access = Access.READ)
@DBusProperty(name = "Scanning", type = Boolean.class, access = Access.READ)
@DBusProperty(name = "ApScan", type = UInt32.class, access = Access.READ_WRITE)
@DBusProperty(name = "BSSExpireAge", type = UInt32.class, access = Access.READ_WRITE)
@DBusProperty(name = "BSSExpireCount", type = UInt32.class, access = Access.READ_WRITE)
@DBusProperty(name = "Country", type = String.class, access = Access.READ_WRITE)
@DBusProperty(name = "Ifname", type = String.class, access = Access.READ)
@DBusProperty(name = "Driver", type = String.class, access = Access.READ)
@DBusProperty(name = "BridgeIfname", type = String.class, access = Access.READ_WRITE)
@DBusProperty(name = "ConfigFile", type = String.class, access = Access.READ)
@DBusProperty(name = "CurrentBSS", type = DBusPath.class, access = Access.READ)
@DBusProperty(name = "CurrentNetwork", type = DBusPath.class, access = Access.READ)
@DBusProperty(name = "CurrentAuthMode", type = String.class, access = Access.READ)
@DBusProperty(name = "Blobs", type = Interface.PropertyBlobsType.class, access = Access.READ)
@DBusProperty(name = "BSSs", type = Interface.PropertyBSSsType.class, access = Access.READ)
@DBusProperty(name = "Networks", type = Interface.PropertyNetworksType.class, access = Access.READ)
@DBusProperty(name = "FastReauth", type = Boolean.class, access = Access.READ_WRITE)
@DBusProperty(name = "ScanInterval", type = Integer.class, access = Access.READ_WRITE)
@DBusProperty(name = "PKCS11EnginePath", type = String.class, access = Access.READ)
@DBusProperty(name = "PKCS11ModulePath", type = String.class, access = Access.READ)
@DBusProperty(name = "DisconnectReason", type = Integer.class, access = Access.READ)
@DBusProperty(name = "AuthStatusCode", type = Integer.class, access = Access.READ)
@DBusProperty(name = "AssocStatusCode", type = Integer.class, access = Access.READ)
@DBusProperty(name = "RoamTime", type = UInt32.class, access = Access.READ)
@DBusProperty(name = "RoamComplete", type = Boolean.class, access = Access.READ)
@DBusProperty(name = "SessionLength", type = UInt32.class, access = Access.READ)
@DBusProperty(name = "BSSTMStatus", type = UInt32.class, access = Access.READ)
@DBusProperty(name = "Stations", type = Interface.PropertyStationsType.class, access = Access.READ)
@DBusProperty(name = "CtrlInterface", type = String.class, access = Access.READ_WRITE)
@DBusProperty(name = "CtrlInterfaceGroup", type = String.class, access = Access.READ_WRITE)
@DBusProperty(name = "EapolVersion", type = String.class, access = Access.READ_WRITE)
@DBusProperty(name = "Bgscan", type = String.class, access = Access.READ_WRITE)
@DBusProperty(name = "DisableScanOffload", type = String.class, access = Access.READ_WRITE)
@DBusProperty(name = "OpenscEnginePath", type = String.class, access = Access.READ_WRITE)
@DBusProperty(name = "OpensslCiphers", type = String.class, access = Access.READ_WRITE)
@DBusProperty(name = "PcscReader", type = String.class, access = Access.READ_WRITE)
@DBusProperty(name = "PcscPin", type = String.class, access = Access.READ_WRITE)
@DBusProperty(name = "ExternalSim", type = String.class, access = Access.READ_WRITE)
@DBusProperty(name = "DriverParam", type = String.class, access = Access.READ_WRITE)
@DBusProperty(name = "Dot11RSNAConfigPMKLifetime", type = String.class, access = Access.READ_WRITE)
@DBusProperty(name = "Dot11RSNAConfigPMKReauthThreshold", type = String.class, access = Access.READ_WRITE)
@DBusProperty(name = "Dot11RSNAConfigSATimeout", type = String.class, access = Access.READ_WRITE)
@DBusProperty(name = "UpdateConfig", type = String.class, access = Access.READ_WRITE)
@DBusProperty(name = "Uuid", type = String.class, access = Access.READ_WRITE)
@DBusProperty(name = "AutoUuid", type = String.class, access = Access.READ_WRITE)
@DBusProperty(name = "DeviceName", type = String.class, access = Access.READ_WRITE)
@DBusProperty(name = "Manufacturer", type = String.class, access = Access.READ_WRITE)
@DBusProperty(name = "ModelName", type = String.class, access = Access.READ_WRITE)
@DBusProperty(name = "ModelNumber", type = String.class, access = Access.READ_WRITE)
@DBusProperty(name = "SerialNumber", type = String.class, access = Access.READ_WRITE)
@DBusProperty(name = "DeviceType", type = String.class, access = Access.READ_WRITE)
@DBusProperty(name = "OsVersion", type = String.class, access = Access.READ_WRITE)
@DBusProperty(name = "ConfigMethods", type = String.class, access = Access.READ_WRITE)
@DBusProperty(name = "WpsCredProcessing", type = String.class, access = Access.READ_WRITE)
@DBusProperty(name = "WpsCredAddSae", type = String.class, access = Access.READ_WRITE)
@DBusProperty(name = "WpsVendorExtM1", type = String.class, access = Access.READ_WRITE)
@DBusProperty(name = "SecDeviceType", type = String.class, access = Access.READ_WRITE)
@DBusProperty(name = "P2pListenRegClass", type = String.class, access = Access.READ_WRITE)
@DBusProperty(name = "P2pListenChannel", type = String.class, access = Access.READ_WRITE)
@DBusProperty(name = "P2pOperRegClass", type = String.class, access = Access.READ_WRITE)
@DBusProperty(name = "P2pOperChannel", type = String.class, access = Access.READ_WRITE)
@DBusProperty(name = "P2pGoIntent", type = String.class, access = Access.READ_WRITE)
@DBusProperty(name = "P2pSsidPostfix", type = String.class, access = Access.READ_WRITE)
@DBusProperty(name = "PersistentReconnect", type = String.class, access = Access.READ_WRITE)
@DBusProperty(name = "P2pIntraBss", type = String.class, access = Access.READ_WRITE)
@DBusProperty(name = "P2pGroupIdle", type = String.class, access = Access.READ_WRITE)
@DBusProperty(name = "P2pGoFreqChangePolicy", type = String.class, access = Access.READ_WRITE)
@DBusProperty(name = "P2pPassphraseLen", type = String.class, access = Access.READ_WRITE)
@DBusProperty(name = "P2pPrefChan", type = String.class, access = Access.READ_WRITE)
@DBusProperty(name = "P2pNoGoFreq", type = String.class, access = Access.READ_WRITE)
@DBusProperty(name = "P2pAddCliChan", type = String.class, access = Access.READ_WRITE)
@DBusProperty(name = "P2pOptimizeListenChan", type = String.class, access = Access.READ_WRITE)
@DBusProperty(name = "P2pGoHt40", type = String.class, access = Access.READ_WRITE)
@DBusProperty(name = "P2pGoVht", type = String.class, access = Access.READ_WRITE)
@DBusProperty(name = "P2pGoHe", type = String.class, access = Access.READ_WRITE)
@DBusProperty(name = "P2pDisabled", type = String.class, access = Access.READ_WRITE)
@DBusProperty(name = "P2pGoCtwindow", type = String.class, access = Access.READ_WRITE)
@DBusProperty(name = "P2pNoGroupIface", type = String.class, access = Access.READ_WRITE)
@DBusProperty(name = "P2pIgnoreSharedFreq", type = String.class, access = Access.READ_WRITE)
@DBusProperty(name = "IpAddrGo", type = String.class, access = Access.READ_WRITE)
@DBusProperty(name = "IpAddrMask", type = String.class, access = Access.READ_WRITE)
@DBusProperty(name = "IpAddrStart", type = String.class, access = Access.READ_WRITE)
@DBusProperty(name = "IpAddrEnd", type = String.class, access = Access.READ_WRITE)
@DBusProperty(name = "P2pCliProbe", type = String.class, access = Access.READ_WRITE)
@DBusProperty(name = "P2pDeviceRandomMacAddr", type = String.class, access = Access.READ_WRITE)
@DBusProperty(name = "P2pDevicePersistentMacAddr", type = String.class, access = Access.READ_WRITE)
@DBusProperty(name = "P2pInterfaceRandomMacAddr", type = String.class, access = Access.READ_WRITE)
@DBusProperty(name = "BssMaxCount", type = String.class, access = Access.READ_WRITE)
@DBusProperty(name = "FilterSsids", type = String.class, access = Access.READ_WRITE)
@DBusProperty(name = "FilterRssi", type = String.class, access = Access.READ_WRITE)
@DBusProperty(name = "MaxNumSta", type = String.class, access = Access.READ_WRITE)
@DBusProperty(name = "ApIsolate", type = String.class, access = Access.READ_WRITE)
@DBusProperty(name = "DisassocLowAck", type = String.class, access = Access.READ_WRITE)
@DBusProperty(name = "Hs20", type = String.class, access = Access.READ_WRITE)
@DBusProperty(name = "Interworking", type = String.class, access = Access.READ_WRITE)
@DBusProperty(name = "Hessid", type = String.class, access = Access.READ_WRITE)
@DBusProperty(name = "AccessNetworkType", type = String.class, access = Access.READ_WRITE)
@DBusProperty(name = "GoInterworking", type = String.class, access = Access.READ_WRITE)
@DBusProperty(name = "GoAccessNetworkType", type = String.class, access = Access.READ_WRITE)
@DBusProperty(name = "GoInternet", type = String.class, access = Access.READ_WRITE)
@DBusProperty(name = "GoVenueGroup", type = String.class, access = Access.READ_WRITE)
@DBusProperty(name = "GoVenueType", type = String.class, access = Access.READ_WRITE)
@DBusProperty(name = "PbcInM1", type = String.class, access = Access.READ_WRITE)
@DBusProperty(name = "Autoscan", type = String.class, access = Access.READ_WRITE)
@DBusProperty(name = "WpsNfcDevPwId", type = String.class, access = Access.READ_WRITE)
@DBusProperty(name = "WpsNfcDhPubkey", type = String.class, access = Access.READ_WRITE)
@DBusProperty(name = "WpsNfcDhPrivkey", type = String.class, access = Access.READ_WRITE)
@DBusProperty(name = "WpsNfcDevPw", type = String.class, access = Access.READ_WRITE)
@DBusProperty(name = "ExtPasswordBackend", type = String.class, access = Access.READ_WRITE)
@DBusProperty(name = "P2pGoMaxInactivity", type = String.class, access = Access.READ_WRITE)
@DBusProperty(name = "AutoInterworking", type = String.class, access = Access.READ_WRITE)
@DBusProperty(name = "Okc", type = String.class, access = Access.READ_WRITE)
@DBusProperty(name = "Pmf", type = String.class, access = Access.READ_WRITE)
@DBusProperty(name = "SaeGroups", type = String.class, access = Access.READ_WRITE)
@DBusProperty(name = "DtimPeriod", type = String.class, access = Access.READ_WRITE)
@DBusProperty(name = "BeaconInt", type = String.class, access = Access.READ_WRITE)
@DBusProperty(name = "ApVendorElements", type = String.class, access = Access.READ_WRITE)
@DBusProperty(name = "IgnoreOldScanRes", type = String.class, access = Access.READ_WRITE)
@DBusProperty(name = "FreqList", type = String.class, access = Access.READ_WRITE)
@DBusProperty(name = "ScanCurFreq", type = String.class, access = Access.READ_WRITE)
@DBusProperty(name = "SchedScanInterval", type = String.class, access = Access.READ_WRITE)
@DBusProperty(name = "SchedScanStartDelay", type = String.class, access = Access.READ_WRITE)
@DBusProperty(name = "TdlsExternalControl", type = String.class, access = Access.READ_WRITE)
@DBusProperty(name = "OsuDir", type = String.class, access = Access.READ_WRITE)
@DBusProperty(name = "WowlanTriggers", type = String.class, access = Access.READ_WRITE)
@DBusProperty(name = "P2pSearchDelay", type = String.class, access = Access.READ_WRITE)
@DBusProperty(name = "MacAddr", type = String.class, access = Access.READ_WRITE)
@DBusProperty(name = "RandAddrLifetime", type = String.class, access = Access.READ_WRITE)
@DBusProperty(name = "PreassocMacAddr", type = String.class, access = Access.READ_WRITE)
@DBusProperty(name = "KeyMgmtOffload", type = String.class, access = Access.READ_WRITE)
@DBusProperty(name = "PassiveScan", type = String.class, access = Access.READ_WRITE)
@DBusProperty(name = "ReassocSameBssOptim", type = String.class, access = Access.READ_WRITE)
@DBusProperty(name = "WpsPriority", type = String.class, access = Access.READ_WRITE)
@DBusProperty(name = "FstGroupId", type = String.class, access = Access.READ_WRITE)
@DBusProperty(name = "FstPriority", type = String.class, access = Access.READ_WRITE)
@DBusProperty(name = "FstLlt", type = String.class, access = Access.READ_WRITE)
@DBusProperty(name = "CertInCb", type = String.class, access = Access.READ_WRITE)
@DBusProperty(name = "WpaRscRelaxation", type = String.class, access = Access.READ_WRITE)
@DBusProperty(name = "SchedScanPlans", type = String.class, access = Access.READ_WRITE)
@DBusProperty(name = "GasAddress3", type = String.class, access = Access.READ_WRITE)
@DBusProperty(name = "FtmResponder", type = String.class, access = Access.READ_WRITE)
@DBusProperty(name = "FtmInitiator", type = String.class, access = Access.READ_WRITE)
@DBusProperty(name = "GasRandAddrLifetime", type = String.class, access = Access.READ_WRITE)
@DBusProperty(name = "GasRandMacAddr", type = String.class, access = Access.READ_WRITE)
@DBusProperty(name = "DppConfigProcessing", type = String.class, access = Access.READ_WRITE)
@DBusProperty(name = "ColocIntfReporting", type = String.class, access = Access.READ_WRITE)
public interface Interface extends DBusInterface {

    public void Scan(Map<String, Variant<?>> args);

    public Map<String, Variant<?>> SignalPoll();

    public void Disconnect();

    public DBusPath AddNetwork(Map<String, Variant<?>> args);

    public void Reassociate();

    public void Reattach();

    public void Reconnect();

    public void RemoveNetwork(DBusPath path);

    public void RemoveAllNetworks();

    public void SelectNetwork(DBusPath path);

    public void NetworkReply(DBusPath path, String field, String value);

    public void AddBlob(String name, List<Byte> data);

    public List<Byte> GetBlob(String name);

    public void RemoveBlob(String name);

    public void SetPKCS11EngineAndModulePath(String pkcs11EnginePath, String pkcs11ModulePath);

    public void FlushBSS(UInt32 age);

    public void SubscribeProbeReq();

    public void UnsubscribeProbeReq();

    public void EAPLogoff();

    public void EAPLogon();

    public void AutoScan(String arg);

    public void TDLSDiscover(String peerAddress);

    public void TDLSSetup(String peerAddress);

    public String TDLSStatus(String peerAddress);

    public void TDLSTeardown(String peerAddress);

    public void TDLSChannelSwitch(Map<String, Variant<?>> args);

    public void TDLSCancelChannelSwitch(String peerAddress);

    public void VendorElemAdd(int frameId, List<Byte> ielems);

    public List<Byte> VendorElemGet(int frameId);

    public void VendorElemRem(int frameId, List<Byte> ielems);

    public void SaveConfig();

    public void AbortScan();

    public static class ScanDone extends DBusSignal {

        private final boolean success;

        public ScanDone(String _path, boolean _success) throws DBusException {
            super(_path, _success);
            this.success = _success;
        }

        public boolean getSuccess() {
            return this.success;
        }

    }

    public static class BSSAdded extends DBusSignal {

        private final DBusPath addedBSSPath;
        private final Map<String, Variant<?>> properties;

        public BSSAdded(String _path, DBusPath _addedBSSpath, Map<String, Variant<?>> _properties)
                throws DBusException {
            super(_path, _addedBSSpath, _properties);
            this.addedBSSPath = _addedBSSpath;
            this.properties = _properties;
        }

        public DBusPath getAddedBSSPath() {
            return this.addedBSSPath;
        }

        public Map<String, Variant<?>> getProperties() {
            return this.properties;
        }

    }

    public static class BSSRemoved extends DBusSignal {

        private final DBusPath removedBSSPath;

        public BSSRemoved(String _path, DBusPath _removedBSSPath) throws DBusException {
            super(_path, _removedBSSPath);
            this.removedBSSPath = _removedBSSPath;
        }

        public DBusPath getRemovedBSSPath() {
            return this.removedBSSPath;
        }

    }

    public static class BlobAdded extends DBusSignal {

        private final String blobName;

        public BlobAdded(String _path, String _blobName) throws DBusException {
            super(_path, _blobName);
            this.blobName = _blobName;
        }

        public String getBlobName() {
            return this.blobName;
        }

    }

    public static class BlobRemoved extends DBusSignal {

        private final String blobName;

        public BlobRemoved(String _path, String _blobName) throws DBusException {
            super(_path, _blobName);
            this.blobName = _blobName;
        }

        public String getBlobName() {
            return this.blobName;
        }

    }

    public static class NetworkAdded extends DBusSignal {

        private final DBusPath addedNetworkPath;
        private final Map<String, Variant<?>> properties;

        public NetworkAdded(String _path, DBusPath _addedNetworkPath, Map<String, Variant<?>> _properties)
                throws DBusException {
            super(_path, _addedNetworkPath, _properties);
            this.addedNetworkPath = _addedNetworkPath;
            this.properties = _properties;
        }

        public DBusPath getAddedNetworkPath() {
            return this.addedNetworkPath;
        }

        public Map<String, Variant<?>> getProperties() {
            return this.properties;
        }

    }

    public static class NetworkRemoved extends DBusSignal {

        private final DBusPath removedNetworkPath;

        public NetworkRemoved(String _path, DBusPath _removedNetworkPath) throws DBusException {
            super(_path, _removedNetworkPath);
            this.removedNetworkPath = _removedNetworkPath;
        }

        public DBusPath getRemovedNetworkPath() {
            return this.removedNetworkPath;
        }

    }

    public static class NetworkSelected extends DBusSignal {

        private final DBusPath selectedNetworkPath;

        public NetworkSelected(String _path, DBusPath _selectedNetworkPath) throws DBusException {
            super(_path, _selectedNetworkPath);
            this.selectedNetworkPath = _selectedNetworkPath;
        }

        public DBusPath getSelectedNetworkPath() {
            return this.selectedNetworkPath;
        }

    }

    public static class PropertiesChanged extends DBusSignal {

        private final Map<String, Variant<?>> properties;

        public PropertiesChanged(String _path, Map<String, Variant<?>> _properties) throws DBusException {
            super(_path, _properties);
            this.properties = _properties;
        }

        public Map<String, Variant<?>> getProperties() {
            return this.properties;
        }

    }

    public static class ProbeRequest extends DBusSignal {

        private final Map<String, Variant<?>> args;

        public ProbeRequest(String _path, Map<String, Variant<?>> _args) throws DBusException {
            super(_path, _args);
            this.args = _args;
        }

        public Map<String, Variant<?>> getArgs() {
            return this.args;
        }

    }

    public static class Certification extends DBusSignal {

        private final Map<String, Variant<?>> certification;

        public Certification(String _path, Map<String, Variant<?>> _certification) throws DBusException {
            super(_path, _certification);
            this.certification = _certification;
        }

        public Map<String, Variant<?>> getCertification() {
            return this.certification;
        }

    }

    public static class EAP extends DBusSignal {

        private final String status;
        private final String parameter;

        public EAP(String _path, String _status, String _parameter) throws DBusException {
            super(_path, _status, _parameter);
            this.status = _status;
            this.parameter = _parameter;
        }

        public String getStatus() {
            return this.status;
        }

        public String getParameter() {
            return this.parameter;
        }

    }

    public static class StaAuthorized extends DBusSignal {

        private final String mac;

        public StaAuthorized(String _path, String _mac) throws DBusException {
            super(_path, _mac);
            this.mac = _mac;
        }

        public String getMac() {
            return this.mac;
        }

    }

    public static class StaDeauthorized extends DBusSignal {

        private final String mac;

        public StaDeauthorized(String _path, String _mac) throws DBusException {
            super(_path, _mac);
            this.mac = _mac;
        }

        public String getMac() {
            return this.mac;
        }

    }

    public static class StationAdded extends DBusSignal {

        private final DBusPath addedStationPath;
        private final Map<String, Variant<?>> properties;

        public StationAdded(String _path, DBusPath _addedStationPath, Map<String, Variant<?>> _properties)
                throws DBusException {
            super(_path, _addedStationPath, _properties);
            this.addedStationPath = _addedStationPath;
            this.properties = _properties;
        }

        public DBusPath getAddedStationPath() {
            return this.addedStationPath;
        }

        public Map<String, Variant<?>> getProperties() {
            return this.properties;
        }

    }

    public static class StationRemoved extends DBusSignal {

        private final DBusPath removedStationPath;

        public StationRemoved(String _path, DBusPath _removedStationPath) throws DBusException {
            super(_path, _removedStationPath);
            this.removedStationPath = _removedStationPath;
        }

        public DBusPath getRemovedStationPath() {
            return this.removedStationPath;
        }

    }

    public static class NetworkRequest extends DBusSignal {

        private final DBusPath networkPath;
        private final String field;
        private final String text;

        public NetworkRequest(String _path, DBusPath _networkPath, String _field, String _text) throws DBusException {
            super(_path, _networkPath, _field, _text);
            this.networkPath = _networkPath;
            this.field = _field;
            this.text = _text;
        }

        public DBusPath getNetworkPath() {
            return this.networkPath;
        }

        public String getField() {
            return this.field;
        }

        public String getText() {
            return this.text;
        }

    }

    public static interface PropertyCapabilitiesType extends TypeRef<Map<String, Variant>> {

    }

    public static interface PropertyBlobsType extends TypeRef<Map<String, List<Byte>>> {

    }

    public static interface PropertyBSSsType extends TypeRef<List<DBusPath>> {

    }

    public static interface PropertyNetworksType extends TypeRef<List<DBusPath>> {

    }

    public static interface PropertyStationsType extends TypeRef<List<DBusPath>> {

    }
}
