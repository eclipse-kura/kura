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
package org.eclipse.kura.core.configuration.util;

import java.io.Reader;
import java.io.StringReader;
import java.io.StringWriter;
import java.io.Writer;
import java.text.MessageFormat;
import java.util.HashMap;
import java.util.Map;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.bind.MarshalException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.UnmarshalException;
import javax.xml.bind.Unmarshaller;
import javax.xml.bind.ValidationEvent;
import javax.xml.bind.util.ValidationEventCollector;
import javax.xml.stream.FactoryConfigurationError;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import com.sun.xml.*;

import org.eclipse.kura.core.configuration.XmlConfigPropertiesAdapter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class XmlUtil 
{
	@SuppressWarnings("unused")
	private static final Logger s_logger = LoggerFactory.getLogger(XmlUtil.class);
	
	@SuppressWarnings("rawtypes")
	private static Map<Class,JAXBContext> s_contexts = new HashMap<Class,JAXBContext>();	
	
	
	public static String marshal(Object object) throws JAXBException 
	{
		StringWriter sw = new StringWriter();
		marshal(object, sw);
		return sw.toString();
	}

	@SuppressWarnings("rawtypes")
	public static void marshal(Object object, Writer w) throws JAXBException 
	{
		Class clazz = object.getClass();
		JAXBContext context = s_contexts.get(clazz);
		if (context == null) {			
			context = JAXBContext.newInstance(clazz);
			s_contexts.put(clazz, context);
		}

		ValidationEventCollector valEventHndlr = new ValidationEventCollector();
		Marshaller marshaller = context.createMarshaller();
		marshaller.setSchema(null);
		marshaller.setProperty( Marshaller.JAXB_FORMATTED_OUTPUT, Boolean.TRUE);
		marshaller.setEventHandler(valEventHndlr);

		try {
			marshaller.marshal(object, w);
		}
		catch (Exception e) {		
			if (e instanceof JAXBException) {
				throw (JAXBException) e;
			}
			else {
				throw new MarshalException(e.getMessage(), e);	
			}			
		}
		if (valEventHndlr.hasEvents()) {			
			for (ValidationEvent valEvent : valEventHndlr.getEvents()) {
				if (valEvent.getSeverity() != ValidationEvent.WARNING) {
					// throw a new Marshall Exception if there is a parsing error
					throw new MarshalException(valEvent.getMessage(), valEvent.getLinkedException());							
				}
			}
		}
	}
	

	public static <T> T unmarshal(String s, Class<T> clazz) 
		throws JAXBException, XMLStreamException, FactoryConfigurationError 
	{
		StringReader sr = new StringReader(s);
		return unmarshal(sr, clazz);
	}


	public static <T> T unmarshal(Reader r, Class<T> clazz) 
		throws JAXBException, XMLStreamException, FactoryConfigurationError 
	{
		JAXBContext context = s_contexts.get(clazz);
		if (context == null) {			
			context = JAXBContext.newInstance(clazz);
			s_contexts.put(clazz, context);
		}

		ValidationEventCollector valEventHndlr = new ValidationEventCollector();
		XMLStreamReader xmlsr = XMLInputFactory.newFactory().createXMLStreamReader(r);
		Unmarshaller unmarshaller = context.createUnmarshaller();
		unmarshaller.setSchema(null);
		unmarshaller.setEventHandler(valEventHndlr);
		
		JAXBElement<T> elem = null;
		try {
			elem = unmarshaller.unmarshal(xmlsr, clazz);
		}
		catch (Exception e) {		
			if (e instanceof JAXBException) {
				throw (JAXBException) e;
			}
			else {
				throw new UnmarshalException(e.getMessage(), e);	
			}			
		}
		
		if (valEventHndlr.hasEvents()) {			
			for (ValidationEvent valEvent : valEventHndlr.getEvents()) {
				if (valEvent.getSeverity() != ValidationEvent.WARNING) {
					// throw a new Unmarshall Exception if there is a parsing error
					String msg = MessageFormat.format("Line {0}, Col: {1}: {2}",
							valEvent.getLocator().getLineNumber(),
							valEvent.getLocator().getColumnNumber(),
							valEvent.getLinkedException().getMessage());
					throw new UnmarshalException(msg, valEvent.getLinkedException());
				}
			}
		}
		return elem.getValue();
	}
}
