package org.eclipse.kura.web.client.util;

import com.google.gwt.safehtml.shared.SafeHtmlUtils;

public class GwtSafeHtmlUtils {

	public static String htmlEscape(String unsafeHtml)
	{
		if (unsafeHtml == null){
			return null;
		}
		return SafeHtmlUtils.htmlEscape(unsafeHtml);
	}
	
	public static String inputSanitize(String unsafeHtml)
	{
		if (unsafeHtml == null){
			return null;
//		} else if (!unsafeHtml.matches("[0-9A-Za-z/.@*#:\\ \\_\\-]+")){
//			return "";
		}
		return SafeHtmlUtils.htmlEscape(unsafeHtml);
	}

	public static String htmlUnescape(String safeHtml)
	{
		if (safeHtml == null)
			return null;

		if (safeHtml.indexOf("&lt;") != -1) {
			safeHtml = safeHtml.replace("&lt;", "<");
		}
		if (safeHtml.indexOf("&gt;") != -1) {
			safeHtml = safeHtml.replace("&gt;", ">");
		}
		if (safeHtml.indexOf("&quot;") != -1) {
			safeHtml = safeHtml.replace("&quot;", "\"");
		}
		if (safeHtml.indexOf("&#39;") != -1) {
			safeHtml = safeHtml.replace("&#39;", "'");
		}
		if (safeHtml.indexOf("&amp;") != -1) {
			safeHtml = safeHtml.replace("&amp;", "&");
		}
		return safeHtml;
	}
}
