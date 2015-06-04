/**
 * Copyright (c) 2011, 2014 Eurotech and/or its affiliates
 *
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *   Eurotech
 */
package org.eclipse.kura.core.configuration;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.xml.bind.annotation.adapters.XmlAdapter;

import org.eclipse.kura.KuraException;
import org.eclipse.kura.configuration.Password;
import org.eclipse.kura.core.configuration.XmlConfigPropertyAdapted.ConfigPropertyType;
import org.eclipse.kura.crypto.CryptoService;
import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.ServiceReference;

/**
 * Helper class to serialize a set of properties in XML.
 */
public class XmlConfigPropertiesAdapter extends XmlAdapter<XmlConfigPropertiesAdapted,Map<String,Object>> 
{
	@Override
	public XmlConfigPropertiesAdapted marshal(Map<String,Object> props) 
		throws Exception 
	{
		List<XmlConfigPropertyAdapted> adaptedValues = new ArrayList<XmlConfigPropertyAdapted>();
		if (props != null) {
			Iterator<String> keys = props.keySet().iterator();
			while (keys.hasNext()) {
	
				XmlConfigPropertyAdapted adaptedValue = new XmlConfigPropertyAdapted();
				
				String key = keys.next();			
				adaptedValue.setName(key);
	
				Object value = props.get(key);
				if (value instanceof String) {
		    		adaptedValue.setArray(false);
		    		adaptedValue.setType(ConfigPropertyType.stringType);
		    		adaptedValue.setValues( new String[] { value.toString() });
		    	}
		    	else if (value instanceof Long) {
		    		adaptedValue.setArray(false);
		    		adaptedValue.setType(ConfigPropertyType.longType);
		    		adaptedValue.setValues( new String[] { value.toString() });
		    	}
		    	else if (value instanceof Double) {
		    		adaptedValue.setArray(false);
		    		adaptedValue.setType(ConfigPropertyType.doubleType);
		    		adaptedValue.setValues( new String[] { value.toString() });
		    	}
		    	else if (value instanceof Float) {
		    		adaptedValue.setArray(false);
		    		adaptedValue.setType(ConfigPropertyType.floatType);
		    		adaptedValue.setValues( new String[] { value.toString() });
		    	}
		    	else if (value instanceof Integer) {
		    		adaptedValue.setArray(false);
		    		adaptedValue.setType(ConfigPropertyType.integerType);
		    		adaptedValue.setValues( new String[] { value.toString() });
		    	}
		    	else if (value instanceof Byte) {
		    		adaptedValue.setArray(false);
		    		adaptedValue.setType(ConfigPropertyType.byteType);
		    		adaptedValue.setValues( new String[] { value.toString() });
		    	}
		    	else if (value instanceof Character) {
		    		adaptedValue.setArray(false);
		    		adaptedValue.setType(ConfigPropertyType.charType);
		    		adaptedValue.setValues( new String[] { value.toString() });
		    	}
		    	else if (value instanceof Boolean) {
		    		adaptedValue.setArray(false);
		    		adaptedValue.setType(ConfigPropertyType.booleanType);
		    		adaptedValue.setValues( new String[] { value.toString() });
		    	}
		    	else if (value instanceof Short) {
		    		adaptedValue.setArray(false);
		    		adaptedValue.setType(ConfigPropertyType.shortType);
		    		adaptedValue.setValues( new String[] { value.toString() });
		    	}
		    	else if (value instanceof Password) {
		    		BundleContext bundleContext = FrameworkUtil.getBundle(this.getClass()).getBundleContext();
		    		ServiceReference<CryptoService> cryptoServiceRef = bundleContext.getServiceReference(CryptoService.class);
		    		CryptoService cryptoService = bundleContext.getService(cryptoServiceRef);
		    		
		    		adaptedValue.setArray(false);
		    		adaptedValue.setEncrypted(true);
		    		adaptedValue.setType(ConfigPropertyType.passwordType);
		    		adaptedValue.setValues( new String[] { cryptoService.encodeBase64(value.toString()) });
		    	}
		    	else if (value instanceof String[]) {
		    		adaptedValue.setArray(true);
		    		adaptedValue.setType(ConfigPropertyType.stringType);
		    		adaptedValue.setValues((String[]) value);
		    	}
		    	else if (value instanceof Long[]) {
		    		adaptedValue.setArray(true);
		    		adaptedValue.setType(ConfigPropertyType.longType);
		    		Long[]   nativeValues = (Long[]) value;
		    		String[] stringValues = new String[nativeValues.length];
		    		for (int i=0;i <nativeValues.length; i++) {
		    			if (nativeValues[i] != null) {
		    				stringValues[i] = nativeValues[i].toString();
		    			}
		    		}
		    		adaptedValue.setValues(stringValues);
		    	}
		    	else if (value instanceof Double[]) {
		    		adaptedValue.setArray(true);
		    		adaptedValue.setType(ConfigPropertyType.doubleType);
		    		Double[] nativeValues = (Double[]) value;
		    		String[] stringValues = new String[nativeValues.length];
		    		for (int i=0;i <nativeValues.length; i++) {
		    			if (nativeValues[i] != null) {
		    				stringValues[i] = nativeValues[i].toString();
		    			}
		    		}
		    		adaptedValue.setValues(stringValues);
		    	}
		    	else if (value instanceof Float[]) {
		    		adaptedValue.setArray(true);
		    		adaptedValue.setType(ConfigPropertyType.floatType);
		    		Float[] nativeValues = (Float[]) value;
		    		String[] stringValues = new String[nativeValues.length];
		    		for (int i=0;i <nativeValues.length; i++) {
		    			if (nativeValues[i] != null) {
		    				stringValues[i] = nativeValues[i].toString();
		    			}
		    		}
		    		adaptedValue.setValues(stringValues);
		    	}
		    	else if (value instanceof Integer[]) {
		    		adaptedValue.setArray(true);
		    		adaptedValue.setType(ConfigPropertyType.integerType);
		    		Integer[] nativeValues = (Integer[]) value;
		    		String[] stringValues = new String[nativeValues.length];
		    		for (int i=0;i <nativeValues.length; i++) {
		    			if (nativeValues[i] != null) {
		    				stringValues[i] = nativeValues[i].toString();
		    			}
		    		}
		    		adaptedValue.setValues(stringValues);
		    	}
		    	else if (value instanceof Byte[]) {
		    		adaptedValue.setArray(true);
		    		adaptedValue.setType(ConfigPropertyType.byteType);
		    		Byte[] nativeValues = (Byte[]) value;
		    		String[] stringValues = new String[nativeValues.length];
		    		for (int i=0;i <nativeValues.length; i++) {
		    			if (nativeValues[i] != null) {
		    				stringValues[i] = nativeValues[i].toString();
		    			}
		    		}
		    		adaptedValue.setValues(stringValues);
		    	}
		    	else if (value instanceof Character[]) {
		    		adaptedValue.setArray(true);
		    		adaptedValue.setType(ConfigPropertyType.charType);
		    		Character[] nativeValues = (Character[]) value;
		    		String[] stringValues = new String[nativeValues.length];
		    		for (int i=0;i <nativeValues.length; i++) {
		    			if (nativeValues[i] != null) {
		    				stringValues[i] = nativeValues[i].toString();
		    			}
		    		}
		    		adaptedValue.setValues(stringValues);
		    	}
		    	else if (value instanceof Boolean[]) {
		    		adaptedValue.setArray(true);
		    		adaptedValue.setType(ConfigPropertyType.booleanType);
		    		Boolean[] nativeValues = (Boolean[]) value;
		    		String[] stringValues = new String[nativeValues.length];
		    		for (int i=0;i <nativeValues.length; i++) {
		    			if (nativeValues[i] != null) {
		    				stringValues[i] = nativeValues[i].toString();
		    			}
		    		}
		    		adaptedValue.setValues(stringValues);
		    	}
		    	else if (value instanceof Short[]) {
		    		adaptedValue.setArray(true);
		    		adaptedValue.setType(ConfigPropertyType.shortType);
		    		Short[] nativeValues = (Short[]) value;
		    		String[] stringValues = new String[nativeValues.length];
		    		for (int i=0;i <nativeValues.length; i++) {
		    			if (nativeValues[i] != null) {
		    				stringValues[i] = nativeValues[i].toString();
		    			}
		    		}
		    		adaptedValue.setValues(stringValues);
		    	}
		    	else if (value instanceof Password[]) {
		    		BundleContext bundleContext = FrameworkUtil.getBundle(this.getClass()).getBundleContext();
		    		ServiceReference<CryptoService> cryptoServiceRef = bundleContext.getServiceReference(CryptoService.class);
		    		CryptoService cryptoService = bundleContext.getService(cryptoServiceRef);
		    		
		    		adaptedValue.setArray(true);
		    		adaptedValue.setEncrypted(true);
		    		adaptedValue.setType(ConfigPropertyType.passwordType);
		    		Password[] nativeValues = (Password[]) value;
		    		String[] stringValues = new String[nativeValues.length];
		    		for (int i=0;i <nativeValues.length; i++) {
		    			if (nativeValues[i] != null) {
		    				stringValues[i] = cryptoService.encodeBase64(nativeValues[i].toString());
		    			}
		    		}
		    		adaptedValue.setValues(stringValues);
		    	}
				
				if(adaptedValue.getValues() == null || adaptedValue.getValues().length > 0){
					adaptedValues.add(adaptedValue);
				}
			}
		}
		
		XmlConfigPropertiesAdapted result = new XmlConfigPropertiesAdapted();
		result.setProperties(adaptedValues.toArray( new XmlConfigPropertyAdapted[]{}));
		return result;
	}

	
	@Override
	public Map<String,Object> unmarshal(XmlConfigPropertiesAdapted adaptedPropsAdapted) 
		throws Exception 
	{
		Map<String,Object> properties = new HashMap<String, Object>();
		XmlConfigPropertyAdapted[] adaptedProps = adaptedPropsAdapted.getProperties();
		if (adaptedProps == null) {
			return properties;
		}
		for (XmlConfigPropertyAdapted adaptedProp : adaptedProps) {
			String propName  = adaptedProp.getName();
			ConfigPropertyType type = adaptedProp.getType();
			if (type != null) {
				Object propvalue = null;
				if (adaptedProp.getArray() == false) {
					switch (adaptedProp.getType()) {
					case stringType:
						propvalue = (String) adaptedProp.getValues()[0];
						break;
					case longType:
						propvalue = Long.parseLong(adaptedProp.getValues()[0]);
						break;
					case doubleType:
						propvalue = Double.parseDouble(adaptedProp.getValues()[0]);
						break;
					case floatType:
						propvalue = Float.parseFloat(adaptedProp.getValues()[0]);
						break;
					case integerType:
						propvalue = Integer.parseInt(adaptedProp.getValues()[0]);
						break;
					case byteType:
						propvalue = Byte.parseByte(adaptedProp.getValues()[0]);
						break;
					case charType:
						String s = adaptedProp.getValues()[0];
						propvalue = new Character(s.charAt(0));
						break;
					case booleanType:
						propvalue = Boolean.parseBoolean(adaptedProp.getValues()[0]);
						break;
					case shortType:
						propvalue = Short.parseShort(adaptedProp.getValues()[0]);
						break;
					case passwordType:
						BundleContext bundleContext = FrameworkUtil.getBundle(this.getClass()).getBundleContext();
			    		ServiceReference<CryptoService> cryptoServiceRef = bundleContext.getServiceReference(CryptoService.class);
			    		CryptoService cryptoService = bundleContext.getService(cryptoServiceRef);
			    		
						propvalue = (String) adaptedProp.getValues()[0];
						if (adaptedProp.isEncrypted()) {
							try {
								propvalue = new Password(cryptoService.decryptAes(((String) propvalue).toCharArray()));
							} catch (KuraException e) {
								propvalue = new Password(cryptoService.decodeBase64((String) propvalue));
							}
						}
						else {
							propvalue = new Password((String) propvalue);
						}
						break;
					}
				}
				else {
					//If we are dealing with an empty array, skip this element.
					//Starting from 1.2.0 an empty array will never be present in a snapshot.
					if(adaptedProp.getValues() == null){
						continue;
					}
					switch (adaptedProp.getType()) {
					case stringType:
						propvalue = adaptedProp.getValues();
						break;
					case longType:
						Long[] longValues = new Long[adaptedProp.getValues().length];
						for (int i=0; i<adaptedProp.getValues().length; i++) {
							if (adaptedProp.getValues()[i] != null) {
								longValues[i] = Long.parseLong(adaptedProp.getValues()[i]);
							}
						}
						propvalue = longValues;
						break;
					case doubleType:
						Double[] doubleValues = new Double[adaptedProp.getValues().length];
						for (int i=0; i<adaptedProp.getValues().length; i++) {
							if (adaptedProp.getValues()[i] != null) {
								doubleValues[i] = Double.parseDouble(adaptedProp.getValues()[i]);
							}
						}
						propvalue = doubleValues;
						break;
					case floatType:
						Float[] floatValues = new Float[adaptedProp.getValues().length];
						for (int i=0; i<adaptedProp.getValues().length; i++) {
							if (adaptedProp.getValues()[i] != null) {
								floatValues[i] = Float.parseFloat(adaptedProp.getValues()[i]);
							}
						}
						propvalue = floatValues;
						break;
					case integerType:
						Integer[] intValues = new Integer[adaptedProp.getValues().length];
						for (int i=0; i<adaptedProp.getValues().length; i++) {
							if (adaptedProp.getValues()[i] != null) {
								intValues[i] = Integer.parseInt(adaptedProp.getValues()[i]);
							}
						}
						propvalue = intValues;
						break;
					case byteType:
						Byte[] byteValues = new Byte[adaptedProp.getValues().length];
						for (int i=0; i<adaptedProp.getValues().length; i++) {
							if (adaptedProp.getValues()[i] != null) {
								byteValues[i] = Byte.parseByte(adaptedProp.getValues()[i]);
							}
						}
						propvalue = byteValues;
						break;
					case charType:
						Character[] charValues = new Character[adaptedProp.getValues().length];
						for (int i=0; i<adaptedProp.getValues().length; i++) {
							if (adaptedProp.getValues()[i] != null) {
								String s = adaptedProp.getValues()[i];
								charValues[i] = new Character(s.charAt(0));
							}
						}
						propvalue = charValues;
						break;
					case booleanType:
						Boolean[] booleanValues = new Boolean[adaptedProp.getValues().length];
						for (int i=0; i<adaptedProp.getValues().length; i++) {
							if (adaptedProp.getValues()[i] != null) {
								booleanValues[i] = Boolean.parseBoolean(adaptedProp.getValues()[i]);
							}
						}
						propvalue = booleanValues;
						break;
					case shortType:
						Short[] shortValues = new Short[adaptedProp.getValues().length];
						for (int i=0; i<adaptedProp.getValues().length; i++) {
							if (adaptedProp.getValues()[i] != null) {
								shortValues[i] = Short.parseShort(adaptedProp.getValues()[i]);
							}
						}
						propvalue = shortValues;
						break;
					case passwordType:
						BundleContext bundleContext = FrameworkUtil.getBundle(this.getClass()).getBundleContext();
			    		ServiceReference<CryptoService> cryptoServiceRef = bundleContext.getServiceReference(CryptoService.class);
			    		CryptoService cryptoService = bundleContext.getService(cryptoServiceRef);
			    		
						Password[] pwdValues = new Password[adaptedProp.getValues().length];
						for (int i=0; i<adaptedProp.getValues().length; i++) {
							if (adaptedProp.getValues()[i] != null) {
								if (adaptedProp.isEncrypted()) {
									try {
										pwdValues[i] = new Password(cryptoService.decryptAes(adaptedProp.getValues()[i].toCharArray()));
									} catch (KuraException e) {
										pwdValues[i] = new Password(cryptoService.decodeBase64(adaptedProp.getValues()[i]));
									}
								}
								else {
									pwdValues[i] = new Password(adaptedProp.getValues()[i]);
								}
							}
						}
						propvalue = pwdValues;
						break;
					}
				}
				properties.put(propName, propvalue);
			}
		}
		return properties;
	}
}
