/*******************************************************************************
 * Copyright (c) 2011, 2016 Eurotech and/or its affiliates
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Eurotech
 *******************************************************************************/
package org.eclipse.kura.windows.position;

/**
 * Implements NMEA sentences parser functions.
 * 
 */
public class NMEAParser {
	
	private int m_fixQuality;
	private String m_timeNmea;
	private String m_dateNmea;
	private double m_longNmea;
	private double m_latNmea;
	private double m_speedNmea;
	private double m_altNmea;
	private double m_trackNmea;
	private double m_DOPNmea;
	private double m_PDOPNmea;
	private double m_HDOPNmea;
	private double m_VDOPNmea;
	private int    m_3DfixNmea;
	private int    m_nrSatellites;
	private static boolean m_validPosition;
	
	/**
	 * Fill the fields of GPS position depending of the type of the sentence
	 * 
	 * @param sentence most recent sentence String from GPS modem
	 */
	public void parseSentence(String sentence) {
		// first remove the end "*"+chksum
		int starpos = sentence.indexOf('*');
		String s_sentence = sentence.substring(0, starpos);
		
		String[] tokens = s_sentence.split(",");
		
		/*
		 * Starting from 4.0 NMEA specs the GPS device can send messages representing different talkers
		 * 
		 * $GP = GPS
		 * $GS = Glonass
		 * $GN = GNSS, that is GPS + Glonass + possibly others
		 */
		if(!tokens[0].startsWith("$G")){
			//Not a valid token. Return.
			return;
		}
		
	    if (tokens[0].endsWith("GGA")) {
	    	if(tokens.length>9){
	    		m_validPosition=true;
	    		if(!tokens[1].isEmpty()) m_timeNmea=tokens[1];else m_validPosition=false;
		    	if(!tokens[2].isEmpty()) m_latNmea=convertPositionlat(tokens[2],tokens[3]);else m_validPosition=false;
		    	if(!tokens[4].isEmpty()) m_longNmea=convertPositionlon(tokens[4],tokens[5]);else m_validPosition=false;
		    	if(!tokens[6].isEmpty()){
		    		m_fixQuality=Integer.parseInt(tokens[6]);
		    		if(m_fixQuality==0) m_validPosition=false;
		    	}
		    	else m_validPosition=false;
		    	if(!tokens[7].isEmpty()) m_nrSatellites=Integer.parseInt(tokens[7]);else m_validPosition=false;
		    	if(!tokens[8].isEmpty()) m_DOPNmea=Double.parseDouble(tokens[8]);else m_validPosition=false;
		    	if(!tokens[9].isEmpty()) m_altNmea=Double.parseDouble(tokens[9]);else m_validPosition=false;
	    	}
	    	else m_validPosition=false;
	    }
	    else if (tokens[0].endsWith("GLL")) {
	    	if(tokens.length>5){
	    		m_validPosition=true;
	    		if(!tokens[1].isEmpty()) m_latNmea=convertPositionlat(tokens[1],tokens[2]);else m_validPosition=false;
	    		if(!tokens[3].isEmpty()) m_longNmea=convertPositionlon(tokens[3],tokens[4]);else m_validPosition=false;
	    		if(!tokens[5].isEmpty()) m_timeNmea=tokens[5];else m_validPosition=false;
	    		if(!tokens[6].isEmpty()){ // check validity
	    			if(!new String("A").equals(tokens[6]))
	    				m_validPosition=false;
	    		}
	    		else m_validPosition=false;
	    	}
	    	else m_validPosition=false;
	    }
	    else if (tokens[0].endsWith("RMC")) {
	    	if(tokens.length>8){
	    		m_validPosition=true;
	    		if(!tokens[1].isEmpty()) m_timeNmea=tokens[1];
	    		if(!tokens[2].isEmpty()){ // check validity
	    			if(!new String("A").equals(tokens[2]))
	    				m_validPosition=false;
	    		}
	    		else m_validPosition=false;
	    		if(!tokens[3].isEmpty()) m_latNmea=convertPositionlat(tokens[3],tokens[4]);else m_validPosition=false; 
	    		if(!tokens[5].isEmpty()) m_longNmea=convertPositionlon(tokens[5],tokens[6]);else m_validPosition=false;
	    		if(!tokens[7].isEmpty()) m_speedNmea=Double.parseDouble(tokens[7])/1.94384449; // conversion speed in knots to m/s : 1 m/s = 1.94384449 knots
	    		if(!tokens[8].isEmpty()) m_trackNmea=Double.parseDouble(tokens[8]);
	    		if(!tokens[9].isEmpty()) m_dateNmea=tokens[9];else m_validPosition=false;
	    	}
	    	else m_validPosition=false;
	    }
	    else if (tokens[0].endsWith("GSA")) {
	    	if(tokens.length>5){
	    		m_validPosition=true;
	    		if(!tokens[2].isEmpty()){
	    			m_3DfixNmea=Integer.parseInt(tokens[2]);
	    			if(m_3DfixNmea==1) m_validPosition=false; 
	    		}
	    		else m_validPosition=false;
		    	int index = tokens.length - 3;
		    	if(!tokens[index].isEmpty()) m_PDOPNmea=Double.parseDouble(tokens[index]);else m_validPosition=false;
		    	if(!tokens[index+1].isEmpty()) m_HDOPNmea=Double.parseDouble(tokens[index+1]);else m_validPosition=false;
		    	if(!tokens[index+2].isEmpty()) m_VDOPNmea=Double.parseDouble(tokens[index+2]);else m_validPosition=false;
	    	}
	    	else m_validPosition=false;
	    }
	    else if (tokens[0].endsWith("VTG")) {
	    	if((tokens.length>7)&&(!tokens[7].isEmpty())){ 
	    		m_speedNmea=Double.parseDouble(tokens[7])*0.277777778; // conversion km/h in m/s : 1 km/h -> 0,277777778 m/s;
	    	}
	    }
	}

	double convertPositionlat(String pos, String direction){
		double floatLatDegrees = 0;
		double floatLatMinutes = 0;
		String s;

		if(pos.length()<6) return 0;
		
		// This copies the arrays to temporary arrays
		s=pos.substring(0, 2);
		floatLatDegrees=Double.parseDouble(s);
		s=pos.substring(2);
		floatLatMinutes=Double.parseDouble(s);		
		floatLatDegrees = floatLatDegrees + floatLatMinutes/60;
		if (direction.contains("S")) {
			floatLatDegrees = floatLatDegrees * -1;
		}		
		return floatLatDegrees;
	}
	
	double convertPositionlon(String pos, String direction){
		double floatLonDegrees = 0;
		double floatLonMinutes = 0;
		String s;

		if(pos.length()<6) return 0;
		
		// This copies the arrays to temporary arrays
		s=pos.substring(0, 3);
		floatLonDegrees=Double.parseDouble(s);
		s=pos.substring(3);
		floatLonMinutes=Double.parseDouble(s);		
		floatLonDegrees = floatLonDegrees + floatLonMinutes/60;
		if (direction.contains("W")) {
			floatLonDegrees = floatLonDegrees * -1;
		}
		return floatLonDegrees;
	}
	
	public String get_timeNmea() {
		return m_timeNmea;
	}

	public int get_fixQuality() {
		return m_fixQuality;
	}

	public String get_dateNmea() {
		return m_dateNmea;
	}

	public double get_longNmea() {
		return m_longNmea;
	}

	public double get_latNmea() {
		return m_latNmea;
	}

	public double get_speedNmea() {
		return m_speedNmea;
	}

	public double get_altNmea() {
		return m_altNmea;
	}

	public double get_trackNmea() {
		return m_trackNmea;
	}

	public double get_DOPNmea() {
		return m_DOPNmea;
	}

	public double get_PDOPNmea() {
		return m_PDOPNmea;
	}

	public double get_HDOPNmea() {
		return m_HDOPNmea;
	}

	public double get_VDOPNmea() {
		return m_VDOPNmea;
	}

	public int get_3DfixNmea() {
		return m_3DfixNmea;
	}

	public int get_nrSatellites() {
		return m_nrSatellites;
	}

	public boolean is_validPosition() {
		return m_validPosition;
	}

}
