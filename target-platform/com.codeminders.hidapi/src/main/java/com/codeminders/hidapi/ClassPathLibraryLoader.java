package com.codeminders.hidapi;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;

public class ClassPathLibraryLoader {

	private static final String[] HID_LIB_NAMES = { "/linux/x86_64/libhidapi-jni.so", "/linux/x86/libhidapi-jni.so", "/mac/x86_64/libhidapi-jni.jnilib",
			"/mac/x86/libhidapi-jni.jnilib", "/win/x86_64/hidapi-jni.dll", "/win/x86/hidapi-jni.dll" };

	public static boolean loadNativeHIDLibrary() {
		boolean isHIDLibLoaded = false;
		// for(String path : HID_LIB_NAMES)
		// {
		// try {
		// // have to use a stream
		// InputStream in =
		// ClassPathLibraryLoader.class.getResourceAsStream(path);
		// s.append(path+ " doesn't resolve!");
		// if (in != null) {
		// try {
		// // always write to different location
		// String tempName = path.substring(path.lastIndexOf('/') + 1);
		// File fileOut = File.createTempFile(tempName.substring(0,
		// tempName.lastIndexOf('.')),
		// tempName.substring(tempName.lastIndexOf('.'), tempName.length()));
		// fileOut.deleteOnExit();
		//
		// OutputStream out = new FileOutputStream(fileOut);
		// byte[] buf = new byte[1024];
		// int len;
		// while ((len = in.read(buf)) > 0){
		// out.write(buf, 0, len);
		// }
		//
		// out.close();
		// s.append(fileOut.toString()).append(" ");
		// Runtime.getRuntime().load(fileOut.toString());
		// isHIDLibLoaded = true;
		// } finally {
		// in.close();
		// }
		// }
		// } catch (Exception e) {
		// // ignore
		// } catch (UnsatisfiedLinkError e) {
		// // ignore
		// }
		//
		// if (isHIDLibLoaded) {
		// break;
		// }
		// }
		try {
			System.loadLibrary("libhidapi-jni.so");
			isHIDLibLoaded = true;
		} catch (Exception ex) {
			// IGNORE
		}
		return isHIDLibLoaded;
	}

}
