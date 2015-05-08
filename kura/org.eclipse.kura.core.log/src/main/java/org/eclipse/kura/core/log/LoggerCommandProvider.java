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
/*
* Copyright (c) 2012 Eurotech Inc. All rights reserved.
*/

package org.eclipse.kura.core.log;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.Hashtable;

import org.apache.log4j.Level;
import org.eclipse.osgi.framework.console.CommandInterpreter;
import org.eclipse.osgi.framework.console.CommandProvider;
import org.osgi.service.component.ComponentContext;

public class LoggerCommandProvider {
	
	public void activate(ComponentContext componentContext) 
	{
		componentContext.getBundleContext().registerService(CommandProvider.class.getName(), new KuraLoggerCommandProvider(), null);
	}
	
	public void deactivate() {
	}
	
	//CommandProvider interface
	public class KuraLoggerCommandProvider implements CommandProvider {

		public void _getkuraloglevel(CommandInterpreter ci) {
			String argument = null;
			try {
				argument  = ci.nextArgument();
			} catch(NullPointerException e) {
				//just means no argument - so display all
			}

			Enumeration<?> currentLoggers = org.apache.log4j.LogManager.getCurrentLoggers();
			Hashtable<String, org.apache.log4j.Logger> loggerHashtable = new Hashtable<String, org.apache.log4j.Logger>();
			ArrayList<String> loggerNames = new ArrayList<String>();
					
			while (currentLoggers.hasMoreElements()) {
				org.apache.log4j.Logger log4jLogger = (org.apache.log4j.Logger)currentLoggers.nextElement();
				loggerHashtable.put(log4jLogger.getName(), log4jLogger);
				loggerNames.add(log4jLogger.getName());
			}

			if (argument == null) {
				// Display all
				ci.print("Default log level: " + org.apache.log4j.LogManager.getRootLogger().getEffectiveLevel().toString() + "\n");
				
				Collections.sort(loggerNames);
				for ( String key : loggerNames ) {					
					org.apache.log4j.Logger log4jLogger = loggerHashtable.get(key);
					ci.print(log4jLogger.getName() + ": " + log4jLogger.getEffectiveLevel().toString() + "\n");
				}
			} else {
				org.apache.log4j.Logger log4jLogger = loggerHashtable.get(argument);
				
				if (log4jLogger != null) {
					ci.print(log4jLogger.getName() + ": " + log4jLogger.getEffectiveLevel().toString() + "\n");
				} else {
					ci.print("Logger not found: " + argument);
				}
			}
		}
		
		public void _setkuraloglevel (CommandInterpreter ci) {
			String argName = null;
			String argLevel = null;
			try {
				argName  = ci.nextArgument();
				argLevel = ci.nextArgument();
				boolean setAll = (argName.equalsIgnoreCase("ALL"));
				
				Enumeration<?> currentLoggers = org.apache.log4j.LogManager.getCurrentLoggers();
						
				while (currentLoggers.hasMoreElements()) {
					org.apache.log4j.Logger log4jLogger = (org.apache.log4j.Logger)currentLoggers.nextElement();
					
					if (setAll || argName.equals(log4jLogger.getName())) {
						Level oldLevel = log4jLogger.getEffectiveLevel();
						log4jLogger.setLevel(Level.toLevel(argLevel, oldLevel));
						ci.print(log4jLogger.getName() + ": " + log4jLogger.getEffectiveLevel().toString() + "\n");
						
						if (!setAll) return;
					}
				}
				
				if (!setAll) ci.print("Could not find logger for: " + argName);
				
			} catch(Exception e) {
				ci.println("Invalid argument passed to setkuraloglevel");
				e.printStackTrace();
			}
		}
		
		public String getHelp() { 
			StringBuffer buffer = new StringBuffer(); 
			buffer.append("---Kura Logger Commands---\n");
			buffer.append("\tgetkuraloglevel [logger_name] - shows the Kura log level for all loggers, or for a specific logger if specified\n");
			buffer.append("\tsetkuraloglevel (ALL | <logger_name>) <log_level> - sets the Kura log level for a given logger, or all loggers if 'ALL'\n");
			buffer.append("\t\tThe valid log_level options are: ALL, TRACE, DEBUG, INFO, WARN, ERROR, FATAL, OFF\n");
			
			return buffer.toString(); 
		}
	}
}
