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
import java.util.List;
import java.util.StringTokenizer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Defines PAP/CHAP authentication in Linux
 *
 * @author ilya.binshtok
 *
 */
public class PppAuthSecrets {

    /*
     * #Secrets for authentication using PAP/CHAP
     * #client server secret IP addresses Provider
     * ISP@CINGULARGPRS.COM * CINGULAR1 * #att
     * mobileweb * password * #o2
     * user * pass * #orange
     * web * web * #vodaphone
     */

    private static final Logger s_logger = LoggerFactory.getLogger(PppAuthSecrets.class);

    private final String secretsFilename;

    private final ArrayList<String> providers;
    private final ArrayList<String> clients;
    private final ArrayList<String> servers;
    private final ArrayList<String> secrets;
    private final ArrayList<String> ipAddresses;

    /**
     * PppAuthSecrets constructor
     *
     * @param secretsFilename
     */
    public PppAuthSecrets(String secretsFilename) {

        this.secretsFilename = secretsFilename;

        this.providers = new ArrayList<>();
        this.clients = new ArrayList<>();
        this.servers = new ArrayList<>();
        this.secrets = new ArrayList<>();
        this.ipAddresses = new ArrayList<>();

        BufferedReader br = null;
        try {
            File secretsFile = new File(this.secretsFilename);

            if (secretsFile.exists()) {
                String currentLine = null;
                StringTokenizer st = null;

                br = new BufferedReader(new FileReader(this.secretsFilename));

                while ((currentLine = br.readLine()) != null) {
                    currentLine = currentLine.trim();

                    if (currentLine.indexOf('#') > 0) {
                        st = new StringTokenizer(currentLine);
                        this.clients.add(st.nextToken());
                        this.servers.add(st.nextToken());
                        this.secrets.add(st.nextToken());
                        this.ipAddresses.add(st.nextToken());
                        this.providers.add(st.nextToken().substring(1));
                    }
                }

                for (int i = 0; i < this.providers.size(); ++i) {
                    s_logger.debug(this.clients.get(i));
                    s_logger.debug(this.servers.get(i));
                    s_logger.debug(this.secrets.get(i));
                    s_logger.debug(this.ipAddresses.get(i));
                    s_logger.debug(this.providers.get(i));
                }
            } else {
                // Create an empty file
                s_logger.info("File does not exist - creating {}", this.secretsFilename);
                writeToFile();
            }
        } catch (Exception e) {
            s_logger.error("Could not initialize", e);
        } finally {
            if (br != null) {
                try {
                    br.close();
                } catch (IOException ex) {
                    s_logger.error("I/O Exception while closing BufferedReader!");
                }
            }
        }

    }

    /**
     * Add a new entry to the secrets file
     *
     * @param provider
     *            cellular provider for which this secret applies
     * @param client
     *            client/username for this secret
     * @param server
     *            server ip for which this entry requires
     * @param secret
     *            secret/password for this account
     * @param ipAddress
     *            ipaddress for this account
     * @throws Exception
     */
    public void addEntry(String provider, String client, String server, String secret, String ipAddress)
            throws Exception {
        boolean addNewEntry = true;

        // make sure this is not a duplicate entry, if it is, replace the old one
        for (int i = 0; i < this.providers.size(); ++i) {
            if (this.providers.get(i).compareTo(provider) == 0) {
                // found a provider match so replace the variables

                this.clients.set(i, client);

                this.servers.set(i, server);

                this.secrets.set(i, secret);

                this.ipAddresses.set(i, ipAddress);

                addNewEntry = false;
                break;
            }
        }

        if (addNewEntry) {
            this.clients.add(client);
            this.servers.add(server);
            this.secrets.add(secret);
            this.ipAddresses.add(ipAddress);
            this.providers.add(provider);
        }

        writeToFile();
    }

    /**
     * Writes current contents in ram back to the pap-secrets file
     *
     * @throws Exception
     *             for file IO errors
     */
    private void writeToFile() throws Exception {
        String authType = "";
        if (this.secretsFilename.indexOf("chap") >= 0) {
            authType = "CHAP";
        } else if (this.secretsFilename.indexOf("pap") >= 0) {
            authType = "PAP";
        }

        PrintWriter pw = null;
        FileOutputStream fos = null;
        try {
            fos = new FileOutputStream(this.secretsFilename);
            pw = new PrintWriter(fos);

            pw.write("#Secrets for authentication using " + authType + "\n");
            pw.write("#client					server			secret			IP addresses	#Provider\n");

            for (int i = 0; i < this.providers.size(); ++i) {
                pw.write(this.clients.get(i) + "\t");
                pw.write(this.servers.get(i) + "\t");
                pw.write(this.secrets.get(i) + "\t");
                pw.write(this.ipAddresses.get(i) + "\t");
                pw.write("#" + this.providers.get(i) + "\n");
            }

            pw.flush();
            fos.getFD().sync();
        } catch (IOException ioe) {
            throw ioe;
        } finally {
            if (fos != null) {
                try {
                    fos.close();
                } catch (IOException ex) {
                    s_logger.error("I/O Exception while closing BufferedReader!");
                }
            }

            if (pw != null) {
                pw.close();
            }
        }
    }

    /**
     * Removed an entry based on a 'type' where the type can be either provider,
     * client, server, secret, or ipaddress. Note if the type occurs more than
     * once, all entries of 'type' matching 'value' will be removed
     *
     * @param type
     *            can be either provider, client, server, secret, or ipaddress
     *
     * @param value
     *            is the value for the given type. For example, if the type is
     *            provider, then the value could be att
     *
     * @throws Exception
     *             for indexing problems
     */
    public void removeEntry(String type, String value) throws Exception {
        if ("provider".equals(type)) {
            checkAndRemove(this.providers, value);
        } else if ("client".equals(type)) {
            checkAndRemove(this.clients, value);
        } else if ("server".equals(type)) {
            checkAndRemove(this.servers, value);
        } else if ("secret".equals(type)) {
            checkAndRemove(this.secrets, value);
        } else if ("ipAddress".equals(type)) {
            checkAndRemove(this.ipAddresses, value);
        } else {
            // unaccepted type
        }

        writeToFile();
    }

    private void checkAndRemove(List<String> list, String value) throws Exception {
        for (int i = 0; i < list.size(); ++i) {
            if (list.get(i).compareTo(value) == 0) {
                removeEntry(i);
                i--;
            }
        }
    }

    /**
     * removed an entry based on an index
     *
     * @param index
     *            the index of the entry to be removed
     *
     * @throws Exception
     *             if the index is invalid
     */
    public void removeEntry(int index) throws Exception {
        try {
            this.clients.remove(index);
            this.servers.remove(index);
            this.secrets.remove(index);
            this.ipAddresses.remove(index);
            this.providers.remove(index);
        } catch (Exception e) {
            throw e;
        }

        writeToFile();
    }

    /**
     * Checks to see if an entry is already present
     *
     * @param provider
     *            cellular provider for which this secret applies
     * @param client
     *            client/username for this secret
     * @param server
     *            server ip for which this entry requires
     * @param secret
     *            secret/password for this account
     * @param ipAddress
     *            ipaddress for this account
     * @return boolean
     *         true - entry found
     *         false - entry not found
     */
    public boolean checkForEntry(String provider, String client, String server, String secret, String ipAddress) {
        for (int i = 0; i < this.providers.size(); ++i) {
            if (this.providers.get(i).compareTo(provider) == 0) {
                // found the provider so check everything else
                if (this.clients.get(i).compareTo(client) == 0 && this.servers.get(i).compareTo(server) == 0
                        && this.secrets.get(i).compareTo(secret) == 0
                        && this.ipAddresses.get(i).compareTo(ipAddress) == 0) {
                    return true;
                }
            }
        }

        // since we got here we didn't find a match
        return false;
    }

    /**
     * Return the secret as a string, given the other parameters. Return null if not found.
     *
     * @param provider
     *            cellular provider for which this secret applies
     * @param client
     *            client/username for this secret
     * @param server
     *            server ip for which this entry requires
     * @param ipAddress
     *            ipaddress for this account
     * @return String
     */
    public String getSecret(String provider, String client, String server, String ipAddress) {
        String secret = null;

        for (int i = 0; i < this.providers.size(); ++i) {
            if (this.providers.get(i).equals(provider)) {
                // found the provider so check everything else
                if (this.clients.get(i).equals(client) && this.servers.get(i).equals(server)
                        && this.ipAddresses.get(i).equals(ipAddress)) {
                    secret = this.secrets.get(i);
                    break;
                }
            }
        }

        return secret;
    }
}
