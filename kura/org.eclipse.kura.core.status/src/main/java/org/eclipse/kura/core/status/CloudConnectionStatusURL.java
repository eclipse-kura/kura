package org.eclipse.kura.core.status;

import java.util.Locale;
import java.util.Properties;

public class CloudConnectionStatusURL {

	public static final String S_CCS		= "ccs:";
	public static final String S_LED		= "led:";
	public static final String S_LOG		= "log";
	public static final String S_NONE		= "none";
	
	public static final int TYPE_LED = 0;
	public static final int TYPE_LOG = 1;
	public static final int TYPE_NONE = 2;
	
	
	public static Properties parseURL(String ccsUrl){
		
		String urlImage = ccsUrl.toLowerCase(Locale.ENGLISH);
		
		Properties props = new Properties();
		
		if(urlImage.startsWith(S_CCS)){
			urlImage = urlImage.replace(S_CCS, "");
			props.put("url", ccsUrl);
			if(urlImage.startsWith(S_LED)){
				//Cloud Connection Status on LED
				urlImage = urlImage.replace(S_LED, "");
				try{
					int LEDPin = Integer.parseInt(urlImage.trim());
					props.put("notification_type", TYPE_LED);
					props.put("led", LEDPin);
				}catch(Exception ex){
					//Do nothing
				}
			}else if (urlImage.startsWith(S_LOG)){
				props.put("notification_type", TYPE_LOG);
			}else if(urlImage.startsWith(S_NONE)){
				props.put("notification_type", TYPE_NONE);				
			}
		}
		
		return props;
	}
}
