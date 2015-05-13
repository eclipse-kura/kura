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
package org.eclipse.kura.web;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.security.Key;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Arrays;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;

import org.eclipse.kura.crypto.CryptoService;
import org.eclipse.kura.db.DbService;
import org.eclipse.kura.web.server.util.ServiceLocator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class AuthenticationManager 
{
	private static AuthenticationManager s_instance;
	private static final Logger s_logger = LoggerFactory.getLogger(AuthenticationManager.class);

	private char[] password;

	protected AuthenticationManager(char[] psw) {
		password= psw;
		s_instance= this;
	}

	public static AuthenticationManager getInstance() {
		return s_instance;
	}
	
	protected void updatePassword(char[] psw){
		password= psw;
	}

	public boolean authenticate(String username, String password)
	{
		try {			

			CryptoService cryptoService = ServiceLocator.getInstance().getService(CryptoService.class);
			String sha1Password= cryptoService.sha1Hash(password);
			
			return Arrays.equals(sha1Password.toCharArray(), this.password);
		}catch (Exception e) {
		}
		return false;
	}
	
	// -------------------------------------------------
	//
	//    Private methods
	//
	// -------------------------------------------------

	public static char[] isDBInitialized(DbService dbService, String dataDir){

		Connection conn = null;
		BufferedReader br = null;
		char[] result= null;
		PreparedStatement stmt = null;
		try{
			conn = dbService.getConnection();
			if(conn.getMetaData().getURL().startsWith("jdbc:hsqldb:mem")){
				s_logger.info("db is file");
				File adminFile = new File(dataDir + "/ap_store");
				if(adminFile.exists() && !adminFile.isDirectory()){

					br = new BufferedReader(new FileReader(adminFile));
					String[] adminString = br.readLine().split(":", 2);

					CryptoService cryptoService = ServiceLocator.getInstance().getService(CryptoService.class);
					result = cryptoService.sha1Hash(decryptAes(adminString[1])).toCharArray();
				}
			}else{
				ResultSet rs = null;
				stmt = conn.prepareStatement("SELECT password FROM dn_user WHERE username = 'admin';");
				rs = stmt.executeQuery();
				if (rs != null && rs.next()) {
					String pass= rs.getString("password");
					result= pass.toCharArray();
				}
			}
		}catch(SQLException e){
			s_logger.info("SQLException");
		} catch(Exception e1){
			s_logger.info("DB Exception");
		}finally {
			try{
				if(br != null){
					br.close();
				}
			}catch (Exception ex){
			}
			try{
				if(stmt != null){
					stmt.close();
				}
			}catch(Exception ex){
			}
		}
		return result;
	}
	
	private static String decryptAes(String encryptedValue) 
			throws Exception 
		{
			Key  key = new SecretKeySpec("rv;ipse329183!@#".getBytes(), "AES");
	        Cipher c = Cipher.getInstance("AES");
	        c.init(Cipher.DECRYPT_MODE, key);
	        CryptoService cryptoService = ServiceLocator.getInstance().getService(CryptoService.class);
	        byte[] decodedValue  = cryptoService.decodeBase64(encryptedValue).getBytes("UTF-8");
	        byte[] decryptedBytes = c.doFinal(decodedValue);
	        String decryptedValue = new String(decryptedBytes);
	        return decryptedValue;
	    }
	
}
