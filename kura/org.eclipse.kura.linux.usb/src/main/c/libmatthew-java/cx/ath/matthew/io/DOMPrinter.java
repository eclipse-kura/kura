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
package cx.ath.matthew.io;

import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;

import org.w3c.dom.Document;
import org.w3c.dom.DocumentType;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * Print a DOM tree to the given OutputStream
 */
public class DOMPrinter
{
   /**
    * Print the given node and all its children.
    * @param n The Node to print.
    * @param os The Stream to print to.
    */
   public static void printNode(Node n, OutputStream os)
   {
      PrintStream p = new PrintStream(os);
      printNode(n, p);
   }
   /**
    * Print the given node and all its children.
    * @param n The Node to print.
    * @param p The Stream to print to.
    */
   public static void printNode(Node n, PrintStream p)
   {
      if (null != n.getNodeValue()) p.print(n.getNodeValue());
      else {
         p.print("<"+n.getNodeName());      
         if (n.hasAttributes()) {
            NamedNodeMap nnm = n.getAttributes();
            for (int i = 0; i < nnm.getLength(); i++) {
               Node attr = nnm.item(i);
               p.print(" "+attr.getNodeName()+"='"+attr.getNodeValue()+"'");
            }
         }
         if (n.hasChildNodes()) {
            p.print(">");
            NodeList nl = n.getChildNodes();
            for (int i = 0; i < nl.getLength(); i++)
               printNode(nl.item(i), p);
            p.print("</"+n.getNodeName()+">");
         } else {
            p.print("/>");
         }
      }
   }
   /**
    * Print the given document and all its children.
    * @param d The Document to print.
    * @param p The Stream to print to.
    */
   public static void printDOM(Document d, PrintStream p)
   {
      DocumentType dt = d.getDoctype();
      if (null != dt) {
         p.print("<!DOCTYPE "+dt.getName());
         String pub = dt.getPublicId();
         String sys = dt.getSystemId();
         if (null != pub) p.print(" PUBLIC \""+pub+"\" \""+sys+"\"");
         else if (null != sys) p.print(" SYSTEM \""+sys+"\"");
         p.println(">");
      }
      Element e = d.getDocumentElement();
      printNode(e, p);
   }
   /**
    * Print the given document and all its children.
    * @param d The Document to print.
    * @param os The Stream to print to.
    */
   public static void printDOM(Document d, OutputStream os)
   {
      PrintStream p = new PrintStream(os);
      printDOM(d, p);
   }
}

