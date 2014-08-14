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
/*
* Copyright (c) 2013 Eurotech Inc. All rights reserved.
*/

package org.eclipse.kura.net.admin.visitor.linux.util;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
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
	#Secrets for authentication using PAP/CHAP
	#client					server			secret			IP addresses	Provider
	ISP@CINGULARGPRS.COM	*               CINGULAR1		*				#att
	mobileweb				* 				password		*				#o2
	user					*				pass			*				#orange
	web						*				web				*				#vodaphone
	*/
		
	private static final Logger s_logger = LoggerFactory.getLogger(PppAuthSecrets.class);

	private String m_secretsFilename;
	
	private ArrayList<String> providers;
	private ArrayList<String> clients;
	private ArrayList<String> servers;
	private ArrayList<String> secrets;
	private ArrayList<String> ipAddresses;

	/**
	 * PppAuthSecrets constructor
	 * 
	 * @param secretsFilename
	 */
	public PppAuthSecrets(String secretsFilename) {
		
	    m_secretsFilename = secretsFilename;
	    		
		providers = new ArrayList<String>();
		clients = new ArrayList<String>();
		servers = new ArrayList<String>();
		secrets = new ArrayList<String>();
		ipAddresses = new ArrayList<String>();

		try {
	        File secretsFile = new File(m_secretsFilename);
	        
	        if(secretsFile.exists()) {
    	        BufferedReader br = null;
    	        String currentLine = null;
    	        StringTokenizer st = null;
    		    
    			br = new BufferedReader(new FileReader(m_secretsFilename));
    			
    			while((currentLine = br.readLine()) != null) {
    				currentLine = currentLine.trim();
    				
    				if(currentLine.indexOf('#') > 0) {
    					st = new StringTokenizer(currentLine);
    					clients.add(st.nextToken());
    					servers.add(st.nextToken());
    					secrets.add(st.nextToken());
    					ipAddresses.add(st.nextToken());
    					providers.add(st.nextToken().substring(1));
    				}
    			}
    			
    			br.close();
    			
    			for(int i=0; i<providers.size(); ++i) {
    				s_logger.debug((String)clients.get(i)+"\t");
    				s_logger.debug((String)servers.get(i)+"\t");
    				s_logger.debug((String)secrets.get(i)+"\t");
    				s_logger.debug((String)ipAddresses.get(i)+"\t");
    				s_logger.debug((String)providers.get(i)+"\n");
    			}
	        } else {
	            // Create an empty file
	            s_logger.info("File does not exist - creating " + m_secretsFilename);
	            this.writeToFile();
	        }
		} catch(Exception e) {
			s_logger.error("Could not initialize", e);
		}
		
	}
	
	
	/**
	 * Add a new entry to the secrets file
	 * 
	 * @param provider cellular provider for which this secret applies
	 * @param client client/username for this secret
	 * @param server server ip for which this entry requires
	 * @param secret secret/password for this account
	 * @param ipAddress ipaddress for this account
	 * @throws Exception 
	 */
	public void addEntry(String provider, String client, String server, String secret, String ipAddress) throws Exception {
	    boolean addNewEntry = true;
	    
		//make sure this is not a duplicate entry, if it is, replace the old one
		for(int i=0; i<this.providers.size(); ++i) {
			if(((String)(this.providers.get(i))).compareTo(provider) == 0) {
				//found a provider match so replace the variables
				this.clients.remove(i);
				this.clients.add(i, client);

				this.servers.remove(i);
				this.servers.add(i, server);
				
				this.secrets.remove(i);
				this.secrets.add(i, secret);
				
				this.ipAddresses.remove(i);
				this.ipAddresses.add(i, ipAddress);
				
				addNewEntry = false;
				break;
			}
		}
		
		if(addNewEntry) {
    		clients.add(client);
    		servers.add(server);
    		secrets.add(secret);
    		ipAddresses.add(ipAddress);
    		providers.add(provider);
		}
		
		writeToFile();
	}
	
	/**
	 * Writes current contents in ram back to the pap-secrets file
	 * 
	 * @throws Exception for file IO errors
	 */
	private void writeToFile() throws Exception {		
		String authType = "";
		if (m_secretsFilename.indexOf("chap") >= 0) {
			authType = "CHAP";
		} else if (m_secretsFilename.indexOf("pap") >= 0) {
			authType = "PAP";
		}
		
		PrintWriter pw = null;
		try {
			FileOutputStream fos = new FileOutputStream(m_secretsFilename);
			pw = new PrintWriter(fos);

			pw.write("#Secrets for authentication using " + authType + "\n");
			pw.write("#client					server			secret			IP addresses	#Provider\n");
			
			for(int i=0; i<providers.size(); ++i) {
				pw.write((String)clients.get(i)+"\t");
				pw.write((String)servers.get(i)+"\t");
				pw.write((String)secrets.get(i)+"\t");
				pw.write((String)ipAddresses.get(i)+"\t");
				pw.write("#"+(String)providers.get(i)+"\n");
			}
            
			pw.flush();
			fos.getFD().sync();
			pw.close();
			fos.close();
		} catch (IOException ioe) {
			throw ioe;
		} finally {
			if (pw != null) {
				pw.close();
				pw = null;
			}
		}
	}
	
	/**
	 * Removed an entry based on a 'type' where the type can be either provider,
	 * client, server, secret, or ipaddress.  Note if the type occurs more than
	 * once, all entries of 'type' matching 'value' will be removed
	 * 
	 * @param type can be either provider, client, server, secret, or ipaddress
	 * 
	 * @param value is the value for the given type.  For example, if the type is
	 * 				provider, then the value could be att
	 * 
	 * @throws Exception for indexing problems
	 */
	public void removeEntry(String type, String value) throws Exception {
		if(type.compareTo("provider") == 0) {
			for(int i=0; i<providers.size(); ++i) {
				if(((String)(providers.get(i))).compareTo(value) == 0) {
					try {
						this.removeEntry(i);
					} catch(Exception e) {
						throw e;
					}
				}
			}
		} else if(type.compareTo("client") == 0) {
			for(int i=0; i<clients.size(); ++i) {
				if(((String)(clients.get(i))).compareTo(value) == 0) {
					try {
						this.removeEntry(i);
					} catch(Exception e) {
						throw e;
					}
				}
			}
		} else if(type.compareTo("server") == 0) {
			for(int i=0; i<servers.size(); ++i) {
				if(((String)(servers.get(i))).compareTo(value) == 0) {
					try {
						this.removeEntry(i);
					} catch(Exception e) {
						throw e;
					}
				}
			}
		} else if(type.compareTo("secret") == 0) {
			for(int i=0; i<secrets.size(); ++i) {
				if(((String)(secrets.get(i))).compareTo(value) == 0) {
					try {
						this.removeEntry(i);
					} catch(Exception e) {
						throw e;
					}
				}
			}
		} else if(type.compareTo("ipAddress") == 0) {
			for(int i=0; i<ipAddresses.size(); ++i) {
				if(((String)(ipAddresses.get(i))).compareTo(value) == 0) {
					try {
						this.removeEntry(i);
					} catch(Exception e) {
						throw e;
					}
				}
			}
		} else {
			//unaccepted type
		}
		
		writeToFile();
	}
	
	/**
	 * removed an entry based on an index
	 * 
	 * @param index the index of the entry to be removed
	 * 
	 * @throws Exception if the index is invalid
	 */
	public void removeEntry(int index) throws Exception {
		try {
			clients.remove(index);
			servers.remove(index);
			secrets.remove(index);
			ipAddresses.remove(index);
			providers.remove(index);
		} catch(Exception e) {
			throw e;
		}
		
		writeToFile();
	}
	
	/**
	 * Checks to see if an entry is already present
	 * 
	 * @param provider cellular provider for which this secret applies
	 * @param client client/username for this secret
	 * @param server server ip for which this entry requires
	 * @param secret secret/password for this account
	 * @param ipAddress ipaddress for this account
	 * @return boolean
	 * 		true - entry found
	 * 		false - entry not found
	 */
	public boolean checkForEntry(String provider, String client, String server, String secret, String ipAddress) {
		for(int i=0; i<providers.size(); ++i) {
			if(((String)(providers.get(i))).compareTo(provider) == 0) {
				//found the provider so check everything else
				if(((String)(clients.get(i))).compareTo(client) == 0 &&
						((String)(servers.get(i))).compareTo(server) == 0 &&
						((String)(secrets.get(i))).compareTo(secret) == 0 &&
						((String)(ipAddresses.get(i))).compareTo(ipAddress) == 0) {
					return true;
				}
			}
		}
		
		//since we got here we didn't find a match
		return false;
	}
	   
    /**
     * Return the secret as a string, given the other parameters.  Return null if not found.
     * 
     * @param provider cellular provider for which this secret applies
     * @param client client/username for this secret
     * @param server server ip for which this entry requires
     * @param ipAddress ipaddress for this account
     * @return String
     */
    public String getSecret(String provider, String client, String server, String ipAddress) {
        String secret = null;
        
        for(int i=0; i<providers.size(); ++i) {
            if(providers.get(i).equals(provider)) {
                //found the provider so check everything else
                if(clients.get(i).equals(client) &&
                        servers.get(i).equals(server) &&
                        ipAddresses.get(i).equals(ipAddress)) {
                    secret = secrets.get(i);
                    break;
                }
            }
        }
        
        return secret;
    }
}
