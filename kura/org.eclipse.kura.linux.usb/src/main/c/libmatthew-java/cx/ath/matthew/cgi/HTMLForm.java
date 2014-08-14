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

package cx.ath.matthew.cgi;

import java.util.Iterator;
import java.util.Vector;


/**
 * Class to manage drawing HTML forms
 */
public class HTMLForm
{
   private String target;
   private String submitlabel;
   private String tableclass;
   private Vector fields;
   private boolean post = true;
   
    /**
    * @param target The module to submit to
    */
   public HTMLForm(String target)
   {
      this(target, "Submit", null);
   }
  
   /**
    * @param target The module to submit to
    * @param submitlabel The string to display on the submit button
    */
   public HTMLForm(String target, String submitlabel)
   {
      this(target, submitlabel, null);
   }

   /**
    * @param target The module to submit to
    * @param submitlabel The string to display on the submit button
    * @param tableclass The class= parameter for the generated table
    */
   public HTMLForm(String target, String submitlabel, String tableclass)
   {
      this.target = target;
      this.submitlabel = submitlabel;
      this.tableclass = tableclass;
      fields = new Vector();
   }

   /**
    * Add a field to be displayed in the form.
    *
    * @param field A Field subclass.
    */
   public void addField(Field field)
   {
      fields.add(field);
   }

   /**
    * Set GET method rather than POST
    * @param enable Enable/Disable GET
    */
   public void setGET(boolean enable)
   {
      post = !enable;
   }
   
   /**
    * Shows the form.
    * @param cgi The CGI instance that is handling output
    */
   public void display(CGI cgi)
   {
      try {
         cgi.out("<form action='"+CGITools.escapeChar(target,'"')+"' method='"+
               (post?"post":"get")+"'>");
         if (null == tableclass)
            cgi.out("<table>");
         else
            cgi.out("<table class='"+tableclass+"'>");
         
         Iterator i = fields.iterator();
         while (i.hasNext()) {
            Field f = (Field) i.next();
          if (f instanceof NewTable) {
             cgi.out(f.print());
          }
            if (!(f instanceof HiddenField) && !(f instanceof SubmitButton) && !(f instanceof NewTable)) {
               cgi.out("   <tr>");
               cgi.out("      <td>"+f.label+"</td>");
               cgi.out("      <td>"+f.print()+"</td>");
               cgi.out("   </tr>");
            }
         }
         cgi.out("   <tr>");
         cgi.out("      <td colspan='2' style='text-align:center;'>");
         i = fields.iterator();
         while (i.hasNext()) {
            Field f = (Field) i.next();
            if (f instanceof HiddenField || f instanceof SubmitButton) {
               cgi.out("         "+f.print());
            }
         }      
         cgi.out("         <input type='submit' name='submit' value='"+CGITools.escapeChar(submitlabel,'\'')+"' />");
         cgi.out("      </td>");
         cgi.out("   </tr>");
         cgi.out("</table>");
         cgi.out("</form>");
      } catch (CGIInvalidContentFormatException CGIICFe) {}
   }
}



