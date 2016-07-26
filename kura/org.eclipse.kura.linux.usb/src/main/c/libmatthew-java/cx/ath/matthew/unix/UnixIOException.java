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
package cx.ath.matthew.unix;

import java.io.IOException;

/**
 * An IO Exception which occurred during UNIX Socket IO
 */
public class UnixIOException extends IOException
{
   private int no;
   private String message;
   public UnixIOException(int no, String message)
   {
      super(message);
      this.message = message;
      this.no = no;
   }
}
