package org.eclipse.kura.net.modem;

import java.util.List;

import org.eclipse.kura.KuraException;
import org.eclipse.kura.net.NetConfig;

public interface CellularModem {

	/**
     * Reports modem's model
     * 
     * @return model, null if not known
     */
    public String getModel() throws KuraException;
    
    /**
     * Returns modem's manufacturer identification
     * 
     * @return manufacturer, null if not known
     */
    public String getManufacturer() throws KuraException;
    
    /**
     * Answers modem's serial number (IMEI/MEID/ESN)
     * 
     * @return serial number, null if not known
     * @throws KuraException
     */
    public String getSerialNumber() throws KuraException;
    
    /**
     * Answers International Mobile Subscribe Identity (IMSI)
     * @return IMSI number, null if not known
     * @throws KuraException
     */
    public String getMobileSubscriberIdentity() throws KuraException;
    
    /**
     * Answers Integrated Circuit Card Identification (ICCID)
     * @return ICCID, "N/A" if not applicable
     * @throws KuraException
     */
    public String getIntegratedCirquitCardId() throws KuraException;
    
    /**
     * Reports modem's revision identification
     * 
     * @return revision ID, null if not known
     */
    public String getRevisionID() throws KuraException;
    
    
    public boolean isReachable() throws KuraException;
     
    /**
     * resets the modem and tries to restore the state 
     * of the modem driver. (e.g. PPP connection, status thread) 
     * 
     * @throws Exception
     */
    public void reset() throws KuraException;
     
    /**
     * Reports signal strength in dBm
     * 
     * @throws Exception
     * @return signal strength
     */
    public int getSignalStrength() throws KuraException;
    
    /**
     * Reports modem registration status
     * 
     * @throws Exception
     * @return modem registration status as {@link ModemRegistrationStatus}
     */ 
    public ModemRegistrationStatus getRegistrationStatus() throws KuraException;
    
    /**
     * Reports number of bytes tarnsmitted during a call
     * 
     * @return number of bytes transmitted
     * @throws Exception
     */
    public long getCallTxCounter() throws KuraException;
    
    /**
     * Reports number of bytes received during a call
     * 
     * @return number of bytes received
     * @throws Exception
     */
    public long getCallRxCounter() throws KuraException;
    
    /**
     * Reports Service Type 
     * 
     * @throws Exception
     * @return service indication
     */
    public String getServiceType() throws KuraException;
    
    
    /**
     * Returns the associated UsbModemDevice
     * 
     * @return <code>UsbModemDevice</code>
     */
    public ModemDevice getModemDevice();    
    
    public String getDataPort() throws KuraException;
    
    public String getAtPort() throws KuraException;
    
    public String getGpsPort() throws KuraException;
    
    public boolean isGpsSupported() throws KuraException;
    
    public void enableGps() throws KuraException;
    
    public void disableGps() throws KuraException;
    
    public List<NetConfig> getConfiguration();
    
    public void setConfiguration(List<NetConfig> netConfigs);
    
    public ModemTechnologyType getTechnologyType ();
}
