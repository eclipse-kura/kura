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
package org.eclipse.kura.web.client.util;

import java.util.MissingResourceException;

import org.eclipse.kura.web.client.messages.ValidationMessages;

import com.google.gwt.core.client.GWT;

public class MessageUtils 
{
	private static final ValidationMessages VMSGS = GWT.create(ValidationMessages.class);

	
	public static String get(String key) {
		try {
			return VMSGS.getString(key);
		}
		catch (MissingResourceException mre) {
			return "";
		}
	}

	public static String get(String key, Object... arguments) {
		try {
			String message = VMSGS.getString(key);
			if (arguments != null) {
				message = doFormat(message, arguments);
			}
			return message;						
		}
		catch (MissingResourceException mre) {
			return "";
		}
	}
	
	
	private static String doFormat(String s, Object[] arguments )
    {
        // A very simple implementation of format
        int i = 0;
        while (i < arguments.length)
        {
            String delimiter = "{" + i + "}";
            while( s.contains( delimiter ) )
            {
                s = s.replace( delimiter, String.valueOf( arguments[i] ) );
            }
            i++;
        }
        return s;
    }
}
