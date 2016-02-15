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
 * Interface to handle exceptions in the CGI.
 */
public interface CGIErrorHandler
{
   /**
    * This is called if an exception is not caught in the CGI.
    * It should handle printing the error message nicely to the user,
    * and then exit gracefully.
    */
   public void print(boolean headers_sent, Exception e);
}
