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
public class DefaultErrorHandler implements CGIErrorHandler
{
   /**
    * This is called if an exception is not caught in the CGI.
    * It should handle printing the error message nicely to the user,
    * and then exit gracefully.
    */
   public void print(boolean headers_sent, Exception e)
   {
      if (!headers_sent) {
         System.out.println("Content-type: text/html");
         System.out.println("");
         System.out.println("<!DOCTYPE HTML PUBLIC \"-//IETF//DTD HTML 2.0//EN\">");
         System.out.println("<HTML><HEAD>");
         System.out.println("<TITLE>Exception in CGI</TITLE>");
         System.out.println("</HEAD><BODY>");
      }
      System.out.println("<HR>");
      System.out.println("<H1>"+e.getClass().toString()+"</H1>");
      System.out.println("<P>");
      System.out.println("Exception Message: "+e.getMessage());
      System.out.println("</P>");
      System.out.println("<P>");
      System.out.println("Stack Trace:");
      System.out.println("</P>");
      System.out.println("<PRE>");
      e.printStackTrace(System.out);
      System.out.println("</PRE>");
      System.out.println("<HR>");
      if (!headers_sent) {
         System.out.println("</BODY></HTML>");
      }
      System.exit(1);
   }
}
