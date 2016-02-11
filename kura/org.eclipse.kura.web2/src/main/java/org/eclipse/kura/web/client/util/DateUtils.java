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
package org.eclipse.kura.web.client.util;

import java.util.Date;

import org.eclipse.kura.web.client.messages.Messages;

import com.google.gwt.core.client.GWT;
import com.google.gwt.i18n.client.DateTimeFormat;

public class DateUtils {

	private static final Messages MSGS = GWT.create(Messages.class);
	
	
	/**
	 * formatDate takes a date an return its string representation
	 */
	public static String formatDateTime(Date d) {

		Date dNow = new Date();

		DateTimeFormat dtf1 = DateTimeFormat.getFormat("yyyy.MM.dd 00:00:00.000 ZZZZ");
		DateTimeFormat dtf2 = DateTimeFormat.getFormat("yyyy.MM.dd HH:mm:ss.SSS ZZZZ");
		String today = dtf1.format(dNow);
		Date  dToday = dtf2.parse(today);

		String date = null;
		@SuppressWarnings("unused")
		long lSecDiff = (dNow.getTime() - d.getTime()) / 1000;
		double dDayDiff = ((double) (d.getTime() - dToday.getTime())) / (86400000); // 1000 * 60 * 60 * 24

		// if more in the future than tomorrow than format the date
		// even if it's just 2 days from today at midnight (day diff of 2 exactly)
		if (dDayDiff >= 2) {

			DateTimeFormat dtf = DateTimeFormat.getFormat(DateTimeFormat.PredefinedFormat.DATE_TIME_MEDIUM);
			date = dtf.format(d);
		}

		// if the modification time is still tomorrow, or
		// exactly at midnight tomorrow, then
		// return something like "Tomorrow 10:30 am"
		else if (dDayDiff >= 1) {

			DateTimeFormat dtf = DateTimeFormat.getFormat(DateTimeFormat.PredefinedFormat.TIME_MEDIUM);
			date = MSGS.tomorrow(dtf.format(d));
		}

		// if the time difference is less than 1 hour
		// return something like "30 minutes ago"
//		else if (lSecDiff >= 0 && lSecDiff < 3600) {
//
//			if ((lSecDiff / 60) < 1) {
//				date = lCtx.format(LocaleContext.RB.STR_LESS_THAN_ONE_MIN_AGO);
//			} else {
//				String mins = String.valueOf(lSecDiff / 60);
//				date = lCtx.format(LocaleContext.RB.STR_N_MIN_AGO, mins);
//			}
//		}

		// if the modification time is still today, or it is midnight
		// this same day (this morning), then
		// return something like "Today 10:30 am"
		else if (dDayDiff >= 0) {

			DateTimeFormat dtf = DateTimeFormat.getFormat(DateTimeFormat.PredefinedFormat.TIME_MEDIUM);
			date = MSGS.today(dtf.format(d));
		}

		// if the modification time is yesterday,
		// or exactly 1 day ago at midnight, then
		// return something like "Yesterday 10:30 am"
		else if (dDayDiff >= -1) {

			DateTimeFormat dtf = DateTimeFormat.getFormat(DateTimeFormat.PredefinedFormat.TIME_MEDIUM);
			date = MSGS.yesterday(dtf.format(d));
		} 
		else {

			DateTimeFormat dtf = DateTimeFormat.getFormat(DateTimeFormat.PredefinedFormat.DATE_TIME_MEDIUM);
			date = dtf.format(d);
		}
		
		return date;
	}
	
	public static int getYear(Date date) {
		return Integer.parseInt(DateTimeFormat.getFormat( "yyyy" ).format(date));
	}
	
	public static int getMonth(Date date) {
		//NB: The month in DateTimeFormat is not zero based [unlike Date() & Calendar()] so we need to subtract one when getting !!! 
		return Integer.parseInt(DateTimeFormat.getFormat( "MM" ).format(date))-1;
	}
	
	public static int getDayOfMonth(Date date) {
		return Integer.parseInt(DateTimeFormat.getFormat( "dd" ).format(date));
	}
	
	public static int getHour(Date date) {
		return Integer.parseInt(DateTimeFormat.getFormat( "HH" ).format(date));
	}
	
	public static int getMinute(Date date) {
		return Integer.parseInt(DateTimeFormat.getFormat( "mm" ).format(date));
	}
	
	public static int getSecond(Date date) {
		return Integer.parseInt(DateTimeFormat.getFormat( "ss" ).format(date));
	}
	
	public static int getMillisecond(Date date) {
		return Integer.parseInt(DateTimeFormat.getFormat( "SSS" ).format(date));
	}
	
	public static Date setYear(Date date, int year) {
		String dateString = DateTimeFormat.getFormat("yyyy.MM.dd.HH.mm.ss.SSS").format(date);
		
		StringBuilder sb = new StringBuilder();
		sb.append(pad(Integer.toString(year), 4));
		sb.append(dateString.substring(5));
		
		return DateTimeFormat.getFormat("yyyy.MM.dd.HH.mm.ss.SSS").parse(sb.toString());
	}
	
	public static Date setMonth(Date date, int month) {
		String dateString = DateTimeFormat.getFormat("yyyy.MM.dd.HH.mm.ss.SSS").format(date);
		
		StringBuilder sb = new StringBuilder();
		sb.append(dateString.substring(0, 5));
		//NB: The month in DateTimeFormat is not zero based [unlike Date() & Calendar()] so we need to add one when setting !!! 
		sb.append(pad(Integer.toString(month+1), 2));
		sb.append(dateString.substring(7));
		
		return DateTimeFormat.getFormat("yyyy.MM.dd.HH.mm.ss.SSS").parse(sb.toString());
	}
	
	public static Date setDayOfMonth(Date date, int dayOfMonth) {
		String dateString = DateTimeFormat.getFormat("yyyy.MM.dd.HH.mm.ss.SSS").format(date);
		
		StringBuilder sb = new StringBuilder();
		sb.append(dateString.substring(0, 8));
		sb.append(pad(Integer.toString(dayOfMonth), 2));
		sb.append(dateString.substring(10));
		
		return DateTimeFormat.getFormat("yyyy.MM.dd.HH.mm.ss.SSS").parse(sb.toString());
	}
	
	public static Date setToLastDayOfMonth(Date date) {
		String dateString = DateTimeFormat.getFormat("yyyy.MM.dd.HH.mm.ss.SSS").format(date);
		
		StringBuilder sb = new StringBuilder();
		sb.append(dateString.substring(0, 8));
		
		switch(getMonth(date)) {
			case 0 : // Jan
				sb.append("31");
				break;
			case 1 : // Feb
				if(isLeapYear(getYear(date))) {
					sb.append("29");
				} else {
					sb.append("28");
				}
				break;
			case 2 : // Mar
				sb.append("31");
				break;
			case 3 : // Apr
				sb.append("30");
				break;
			case 4 : // May
				sb.append("31");
				break;
			case 5 : // June
				sb.append("30");
				break;
			case 6 : // July
				sb.append("31");
				break;
			case 7 : // August
				sb.append("31");
				break;
			case 8 : // Sept
				sb.append("30");
				break;
			case 9 : // Oct
				sb.append("31");
				break;
			case 10 : // Nov
				sb.append("30");
				break;
			case 11 : // Dec
				sb.append("31");
				break;
			default :
				break;
		}
		
		sb.append(dateString.substring(10));
		
		return DateTimeFormat.getFormat("yyyy.MM.dd.HH.mm.ss.SSS").parse(sb.toString());
	}
	
	public static Date setHour(Date date, int hour) {
		String dateString = DateTimeFormat.getFormat("yyyy.MM.dd.HH.mm.ss.SSS").format(date);
		
		StringBuilder sb = new StringBuilder();
		sb.append(dateString.substring(0, 11));
		sb.append(pad(Integer.toString(hour), 2));
		sb.append(dateString.substring(13));
		
		return DateTimeFormat.getFormat("yyyy.MM.dd.HH.mm.ss.SSS").parse(sb.toString());
	}
	
	public static Date setMinute(Date date, int minute) {
		String dateString = DateTimeFormat.getFormat("yyyy.MM.dd.HH.mm.ss.SSS").format(date);
		
		StringBuilder sb = new StringBuilder();
		sb.append(dateString.substring(0, 14));
		sb.append(pad(Integer.toString(minute), 2));
		sb.append(dateString.substring(16));
		
		return DateTimeFormat.getFormat("yyyy.MM.dd.HH.mm.ss.SSS").parse(sb.toString());
	}
	
	public static Date setSecond(Date date, int second) {
		String dateString = DateTimeFormat.getFormat("yyyy.MM.dd.HH.mm.ss.SSS").format(date);
		
		StringBuilder sb = new StringBuilder();
		sb.append(dateString.substring(0, 17));
		sb.append(pad(Integer.toString(second), 2));
		sb.append(dateString.substring(19));
		
		return DateTimeFormat.getFormat("yyyy.MM.dd.HH.mm.ss.SSS").parse(sb.toString());
	}
	
	public static Date setMillisecond(Date date, int millisecond) {
		String dateString = DateTimeFormat.getFormat("yyyy.MM.dd.HH.mm.ss.SSS").format(date);
		
		StringBuilder sb = new StringBuilder();
		sb.append(dateString.substring(0, 20));
		sb.append(pad(Integer.toString(millisecond), 3));
		
		return DateTimeFormat.getFormat("yyyy.MM.dd.HH.mm.ss.SSS").parse(sb.toString());
	}
	
	private static String pad(String data, int size) {
		if(data.length() < size) {
			StringBuffer sb = new StringBuffer();
			for(int i=0; i<(size-data.length()); i++) {
				sb.append("0");
			}
			sb.append(data);
			return sb.toString();
		}
			
		return data;
	}
	
	private static boolean isLeapYear(int year) {
		if(year % 4 == 0) {
			return true;
		} else {
			return false;
		}
	}
}
