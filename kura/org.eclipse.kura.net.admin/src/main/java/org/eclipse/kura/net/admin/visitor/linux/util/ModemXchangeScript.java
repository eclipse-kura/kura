/*******************************************************************************
 * Copyright (c) 2011, 2018 Eurotech and/or its affiliates
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Eurotech
 *******************************************************************************/
package org.eclipse.kura.net.admin.visitor.linux.util;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.ListIterator;

/**
 * This class defines Modem Exchange script that is used for
 * establishing PPP connection with service provider.
 *
 * @author ilya.binshtok
 *
 */
public class ModemXchangeScript {

    /* list of modem exchange send/expect pairs */
    private ArrayList<ModemXchangePair> modemXchangePairs = null;
    private ListIterator<ModemXchangePair> iterator;

    /**
     * ModemXchangeScript constructor
     */
    public ModemXchangeScript() {
        this.modemXchangePairs = new ArrayList<>();
    }

    /**
     * Adds 'send/expect' pair to the modem exchange script
     *
     * @param xchangePair
     *            - 'send/expect' pair
     */
    public void addmodemXchangePair(ModemXchangePair xchangePair) {
        this.modemXchangePairs.add(xchangePair);
    }

    /**
     * Reports first 'send/expect' pair of thew modem exchange script.
     *
     * @return ModemXchangePair
     */
    public ModemXchangePair getFirstModemXchangePair() {

        ModemXchangePair modemXchangePair = null;
        this.iterator = this.modemXchangePairs.listIterator(0);

        if (this.iterator.hasNext()) {
            modemXchangePair = this.iterator.next();
        }
        return modemXchangePair;
    }

    /**
     * Reports next 'send/expect' pair of thew modem exchange script.
     *
     * @return ModemXchangePair
     */
    public ModemXchangePair getNextModemXchangePair() {

        ModemXchangePair modemXchangePair = null;
        if (this.iterator.hasNext()) {
            modemXchangePair = this.iterator.next();
        }
        return modemXchangePair;
    }

    public void writeScript(String filename) throws Exception {
        ModemXchangePair modemXchangePair = null;

        PrintWriter writer = null;
        try {
            writer = new PrintWriter(new FileOutputStream(filename));

            modemXchangePair = getFirstModemXchangePair();
            while (modemXchangePair != null) {
                writer.println(modemXchangePair.toString());
                modemXchangePair = getNextModemXchangePair();
            }
        } finally {
            writer.close();
        }
    }

    public static ModemXchangeScript parseFile(String filename) throws IOException {
        ModemXchangeScript script = new ModemXchangeScript();
        File scriptFile = new File(filename);

        if (scriptFile.exists()) {
            BufferedReader reader = null;
            try {
                reader = new BufferedReader(new FileReader(scriptFile));

                String line = reader.readLine();
                String[] pair;
                while (line != null) {
                    pair = line.split("\\s", 2);
                    if (pair.length == 2) {
                        script.addmodemXchangePair(new ModemXchangePair(pair[1], pair[0]));
                    }
                    line = reader.readLine();
                }

            } finally {
                reader.close();
            }
        }

        return script;
    }
}
