package org.freedesktop.modemmanager1.modem;

import java.util.List;
import java.util.Map;
import org.freedesktop.dbus.DBusPath;
import org.freedesktop.dbus.TypeRef;
import org.freedesktop.dbus.annotations.DBusInterfaceName;
import org.freedesktop.dbus.annotations.DBusProperty;
import org.freedesktop.dbus.annotations.DBusProperty.Access;
import org.freedesktop.dbus.interfaces.DBusInterface;
import org.freedesktop.dbus.types.UInt32;
import org.freedesktop.dbus.types.Variant;
import org.freedesktop.modemmanager1.modem.PropertyPcoStruct;

/**
 * Auto-generated class.
 */
@DBusInterfaceName("org.freedesktop.ModemManager1.Modem.Modem3gpp")
@DBusProperty(name = "Imei", type = String.class, access = Access.READ)
@DBusProperty(name = "RegistrationState", type = UInt32.class, access = Access.READ)
@DBusProperty(name = "OperatorCode", type = String.class, access = Access.READ)
@DBusProperty(name = "OperatorName", type = String.class, access = Access.READ)
@DBusProperty(name = "EnabledFacilityLocks", type = UInt32.class, access = Access.READ)
@DBusProperty(name = "SubscriptionState", type = UInt32.class, access = Access.READ)
@DBusProperty(name = "EpsUeModeOperation", type = UInt32.class, access = Access.READ)
@DBusProperty(name = "Pco", type = Modem3gpp.PropertyPcoType.class, access = Access.READ)
@DBusProperty(name = "InitialEpsBearer", type = DBusPath.class, access = Access.READ)
@DBusProperty(name = "InitialEpsBearerSettings", type = Modem3gpp.PropertyInitialEpsBearerSettingsType.class, access = Access.READ)
public interface Modem3gpp extends DBusInterface {


    public void Register(String operatorId);
    public List<Map<String, Variant<?>>> Scan();
    public void SetEpsUeModeOperation(UInt32 mode);
    public void SetInitialEpsBearerSettings(Map<String, Variant<?>> settings);


    public static interface PropertyPcoType extends TypeRef<List<PropertyPcoStruct>> {




    }

    public static interface PropertyInitialEpsBearerSettingsType extends TypeRef<Map<String, Variant>> {




    }
}