package org.eclipse.kura.web.shared.model;

import java.io.Serializable;

import org.eclipse.kura.web.client.util.KuraBaseModel;

public class GwtModemSimCardEntry extends KuraBaseModel implements Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = -1872524387503242012L;
	
	public GwtModemSimCardEntry() {}
	
	public boolean isActive() {
		if (get("isActive") != null) {
			return (Boolean) get("isActive");
		}
		else {
			return false;
		}
	}

	public void setActive(boolean isActive) {
		set("isActive", isActive);
	}
	
	public GwtSimCardSlot getSimSlot() {
		if (get("simSlot") != null) {
			return GwtSimCardSlot.valueOf((String)get("simSlot"));
		} else {
			return null;
		}
	}
	
	public void setSimSlot(GwtSimCardSlot simSlot) {
		set("simSlot", simSlot.name());
	}
	
	public String getInternationalMobileSubscriberIdentity() {
		return get("IMSI");
	}
	
	
	public void setInternationalMobileSubscriberIdentity(String imsi) {
		set("IMSI", imsi);
	}
	
	public String getIntegratedCircuitCardIdentification() {
		return get("ICCID");
	}
	
	public void setIntegratedCircuitCardIdentification(String iccid) {
		set("ICCID", iccid);
	}
	
	public String getSubscriberNumber() {
		return get("subscriberNumber");
	}
	
	public void setSubscriberNumber(String subscriberNumber) {
		set("subscriberNumber", subscriberNumber);
	}
	
	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append("Slot: ").append(getSimSlot());
		sb.append("; isActive=").append(isActive());
		sb.append("; IMSI=").append(getInternationalMobileSubscriberIdentity());
		sb.append("; ICCID=").append(getIntegratedCircuitCardIdentification());
		sb.append("; Subscriber number=").append(getSubscriberNumber());
		return sb.toString();
	}
}
