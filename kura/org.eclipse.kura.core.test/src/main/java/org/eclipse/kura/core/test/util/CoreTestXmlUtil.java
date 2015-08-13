package org.eclipse.kura.core.test.util;

import java.io.Reader;
import java.io.StringReader;
import java.text.MessageFormat;
import java.util.HashMap;
import java.util.Map;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.bind.UnmarshalException;
import javax.xml.bind.Unmarshaller;
import javax.xml.bind.ValidationEvent;
import javax.xml.bind.util.ValidationEventCollector;
import javax.xml.stream.FactoryConfigurationError;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;

public class CoreTestXmlUtil {
	
	@SuppressWarnings("rawtypes")
	private static Map<Class,JAXBContext> s_contexts = new HashMap<Class,JAXBContext>();	
	
	public static <T> T unmarshal(String s, Class<T> clazz) 
			throws JAXBException, XMLStreamException, FactoryConfigurationError 
		{
			StringReader sr = new StringReader(s);
			System.out.println("CoreTestXmlUtil: 30");
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
			System.out.println("CoreTestXmlUtil: 49");
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
