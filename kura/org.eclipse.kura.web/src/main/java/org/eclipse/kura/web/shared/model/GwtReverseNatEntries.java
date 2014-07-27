package org.eclipse.kura.web.shared.model;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class GwtReverseNatEntries implements Serializable {
		
	/**
	 * 
	 */
	private static final long serialVersionUID = -347524482384552722L;
	private List<GwtReverseNatEntry> m_revNatEntries = null;
	
	public GwtReverseNatEntries() {
		m_revNatEntries = new ArrayList<GwtReverseNatEntry>();
	}
	
	public GwtReverseNatEntries(List<GwtReverseNatEntry> list) {
		m_revNatEntries = new ArrayList<GwtReverseNatEntry>();
		m_revNatEntries.addAll(list);
	}
	
	public void add(GwtReverseNatEntry revNatEntry) {
		boolean entryFound = false;
		for(GwtReverseNatEntry entry : m_revNatEntries) {
			if (equals(entry, revNatEntry)) {
				entryFound = true;
				break;
			}
		}
		if (!entryFound) {
			m_revNatEntries.add(revNatEntry);
		}
	}
	
	public void remove(GwtReverseNatEntry revNatEntry) {
		for (int i = 0; i < m_revNatEntries.size(); i++) {
			GwtReverseNatEntry entry = m_revNatEntries.get(i);
			if (equals(entry, revNatEntry)) {
				m_revNatEntries.remove(i);
				break;
			}
		}
	}
	
	public List<GwtReverseNatEntry> getEntries() {
		return m_revNatEntries;
	}
	
	public int getNumberOfEntries() {
		return m_revNatEntries.size();
	}
	
	public boolean equals(Object o) {
		if (!(o instanceof GwtReverseNatEntries)) {
			return false;
		}
		
		GwtReverseNatEntries other = (GwtReverseNatEntries)o;
		if (m_revNatEntries != null) {
			if (other.m_revNatEntries == null) {
				return false;
			}
			
			if (m_revNatEntries.size() != other.m_revNatEntries.size()) {
				return false;
			}
			
			for (GwtReverseNatEntry thisRevNatEntry : m_revNatEntries) {
				boolean isMatch = false;
				for (GwtReverseNatEntry otherRevNatEntry : other.m_revNatEntries) {
					if (thisRevNatEntry.equals(otherRevNatEntry)) {
						isMatch = true;
					}
				}
				if (!isMatch) {
					return false;
				}
			}
		} else {
			if (other.m_revNatEntries != null) {
				return false;
			}
		}
		
		return true;
	}
	
	private boolean equals (GwtReverseNatEntry revNatEntry1, GwtReverseNatEntry revNatEntry2) {
		
		boolean ret = false;
		if (revNatEntry1.getOutInterface().equals(revNatEntry2.getOutInterface())
				&& revNatEntry1.getProtocol().equals(revNatEntry2.getProtocol())
				&& revNatEntry1.getOutInterface().equals(revNatEntry2.getOutInterface())
				&& revNatEntry1.getOutInterface().equals(revNatEntry2.getOutInterface())) {	
			ret = true;
		}
		return ret;
	}

}
