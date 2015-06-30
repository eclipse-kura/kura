package com.eurotech.denali.util;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.eurotech.denali.comm.ConsoleFactory;

public class JvmUtil {

	public static void idle(int seconds) {
		try {
			long milliSec = 1000 * seconds;
			Thread.sleep(milliSec);
		} catch (InterruptedException e) {
			throw new RuntimeException(e);
		}
	}

	public static String getPatternText(Pattern pattern, String input) {
		String result = "";
		Matcher matcher = pattern.matcher(input);
		if (matcher.find()) {
			result = matcher.group().trim();
		}
		return result;
	}

	public static boolean isPatternTextPresent(Pattern pattern, String input) {
		Matcher matcher = pattern.matcher(input);
		return matcher.find() ? true : false;
	}

	public static List<String> getPatternList(Pattern pattern, String input) {
		List<String> matchList = new ArrayList<String>();
		Matcher matcher = pattern.matcher(input);
		while (matcher.find()) {
			matchList.add(matcher.group().trim());
		}
		return matchList;
	}

	public static boolean waitForOSGIServices(ConsoleFactory factory) {
		int i = 0;
		boolean status = factory.verfiyKuraWebBundleStatus();
		while (i++ < 15 && !status) {
			JvmUtil.idle(20);
			status = factory.verfiyKuraWebBundleStatus();
		}
		return status;
	}
}
