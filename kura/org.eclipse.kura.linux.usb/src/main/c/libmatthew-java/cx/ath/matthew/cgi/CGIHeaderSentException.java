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
package cx.ath.matthew.cgi;

/**
 * Thrown if the headers have already been sent and CGI.header is called.
 */
public class CGIHeaderSentException extends Exception
{
   public CGIHeaderSentException()
   {
      super("Headers already sent by CGI");
   }
}
