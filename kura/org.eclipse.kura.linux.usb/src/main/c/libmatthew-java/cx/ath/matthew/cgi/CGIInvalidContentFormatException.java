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
 * Thrown if both raw and text data are set in the same page.
 */
public class CGIInvalidContentFormatException extends Exception
{
   public CGIInvalidContentFormatException()
   {
      super("Cannot send both raw and text data");
   }
}
