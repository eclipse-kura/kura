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
package org.eclipse.kura.cloud.app.command;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public class UnZip
{
    private static final String INPUT_ZIP_FILE = "/tmp/test.zip";
	//private static final String INPUT_ZIP_FILE = "/tmp/zip_with_script.zip";
	private static final String OUTPUT_FOLDER = "/tmp/";

	public static void main( String[] args )
	{
		//UnZip unZip = new UnZip();
		//unZip.unZipIt(INPUT_ZIP_FILE,OUTPUT_FOLDER);
		
		// Create a ZipInputStream from the bag of bytes of the file
		byte[] zipBytes = null;
		try {
			zipBytes = getFileBytes(new File(INPUT_ZIP_FILE));
			unZipBytes(zipBytes, OUTPUT_FOLDER);
		} catch (IOException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		
		try {
			unZipFile(INPUT_ZIP_FILE, OUTPUT_FOLDER);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	static public void unZipBytes(byte[] bytes, String outputFolder) throws IOException {
		ZipInputStream zis = new ZipInputStream(new ByteArrayInputStream(bytes));
		unZipZipInputStream(zis, outputFolder);
	}
	
	static public void unZipFile(String filename, String outputFolder) throws IOException {
		//get the zip file content
		File file = new File(filename);
		ZipInputStream zis = new ZipInputStream(new FileInputStream(file));
		unZipZipInputStream(zis, outputFolder);
	}
	
	static private void unZipZipInputStream(ZipInputStream zis, String outFolder) throws IOException {
		String outputFolder = outFolder;
		if (outputFolder == null) {
			outputFolder = System.getProperty("user.dir");
		}
		
		//create output directory is not exists
		File folder = new File(outputFolder);
		if(!folder.exists()){
			folder.mkdirs();
		}

		ZipEntry ze = zis.getNextEntry();

		while(ze!=null){
			byte[] buffer = new byte[1024];

			String fileName = ze.getName();
			File newFile = new File(outputFolder + File.separator + fileName);

			//System.out.println("file unzip : "+ newFile.getAbsoluteFile());

			//create all non exists folders
			//else you will hit FileNotFoundException for compressed folder
			//new File(newFile.getParent()).mkdirs();
			
			if (newFile.isDirectory()) {
				newFile.mkdirs();
				ze = zis.getNextEntry();
				continue;
			}
			
			if (newFile.getParent() != null) {
				File parent = new File(newFile.getParent());
				parent.mkdirs();
			}

			FileOutputStream fos = new FileOutputStream(newFile);

			int len;
			while ((len = zis.read(buffer)) > 0) {
				fos.write(buffer, 0, len);
			}

			fos.close();   
			ze = zis.getNextEntry();
		}

		zis.closeEntry();
		zis.close();

		//System.out.println("Done");
	}

	private static byte[] getFileBytes(File file) throws IOException {
		ByteArrayOutputStream ous = null;
		InputStream ios = null;
		try {
			byte[] buffer = new byte[4096];
			ous = new ByteArrayOutputStream();
			ios = new FileInputStream(file);
			int read = 0;
			while ((read = ios.read(buffer)) != -1)
				ous.write(buffer, 0, read);
		} finally {
			try {
				if (ous != null)
					ous.close();
			} catch (IOException e) {
				// swallow, since not that important
			}
			try {
				if (ios != null)
					ios.close();
			} catch (IOException e) {
				// swallow, since not that important
			}
		}
		return ous.toByteArray();
	}
}
