package com.voc.jira.plugins.jira.util;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import com.voc.jira.plugins.jira.servlet.IErrorKeeper;

public class Time {

	private static final String LAST_MONTH_PERCENT_DONE = "lastMonthPercentDone";
	private static final String LOOKBACK = "lookback";
	private static final String SCOPE = "scope";
	static final String DATES_INVERTED = "beginDate must be before endDate.";

	public static void beginningOfMonth(Calendar cal) {
		cal.set(Calendar.DAY_OF_MONTH, 1);
	}

	public static void endOfMonth(Calendar cal) {
		cal.set(Calendar.DAY_OF_MONTH,
				cal.getActualMaximum(Calendar.DAY_OF_MONTH));
	}

	private static final SimpleDateFormat sdf = new SimpleDateFormat(
			"yyyy-MM-dd");

	public static String format(Date d) {
		return sdf.format(d);
	}

	public static LinkedHashMap<String, Date> getMonthEnds(Date begin, Date end) {
		LinkedHashMap<String, Date> r = new LinkedHashMap<String, Date>();
		Calendar cal = Calendar.getInstance();
		cal.setTime(begin);
		endOfMonth(cal);
		r.put(sdf.format(cal.getTime()), cal.getTime());
		while (cal.getTime().before(end)) {
			beginningOfMonth(cal);
			cal.add(Calendar.MONTH, 1);
			endOfMonth(cal);
			r.put(format(cal.getTime()), cal.getTime());
		}
		return r;
	}

	public static Date getMonthBeginning(String end) {
		Calendar cal = Calendar.getInstance();
		Date ending;
		try {
			ending = sdf.parse(end.trim());
		} catch (ParseException e) {
			e.printStackTrace();
			ending = cal.getTime();
		}
		return getMonthBeginning(ending);
	}

	public static Date getMonthBeginning(Date ending) {
		Calendar cal = Calendar.getInstance();
		cal.setTime(ending);
		beginningOfMonth(cal);
		return cal.getTime();
	}

	public static Date getYesterday(Date date) {
		Calendar cal = Calendar.getInstance();
		cal.setTime(date);
		cal.add(Calendar.DAY_OF_YEAR, -1);
		return cal.getTime();
	}

	public static Date getDefaultStartDate() {
		Calendar cal = Calendar.getInstance();
		Time.beginningOfMonth(cal);
		cal.add(Calendar.MONTH, -4);
		return cal.getTime();
	}

	public static Date getDefaultEndDate() {
		return getEndOfMonth(-1);
	}

	public static Date getEndOfMonth(int monthsRelativeToToday) {
		return getEndOfMonthCal(monthsRelativeToToday).getTime();
	}

	public static Calendar getEndOfMonthCal(int monthsRelativeToToday) {
		Calendar cal = Calendar.getInstance();
		Time.beginningOfMonth(cal);
		cal.add(Calendar.MONTH, monthsRelativeToToday);
		Time.endOfMonth(cal);
		return cal;
	}

	public static Date getBeginningOfMonth(int monthsRelativeToToday) {
		return getBeginningOfMonthCal(monthsRelativeToToday).getTime();
	}

	public static Calendar getBeginningOfMonthCal(int monthsRelativeToToday) {
		Calendar cal = Calendar.getInstance();
		Time.beginningOfMonth(cal);
		cal.add(Calendar.MONTH, monthsRelativeToToday);
		return cal;
	}

	private static Scope getScope(HttpServletRequest req,
			Map<String, Object> context) {
		String param = req.getParameter(SCOPE);
		if (param == null) {
			return Scope.ABSOLUTE;
		}
		Scope s = "true".equalsIgnoreCase(param.trim()) ? Scope.ABSOLUTE
				: Scope.RELATIVE;
		context.put(SCOPE, s);
		return s;
	}

	public static void getScope(HttpServletRequest req,
			Map<String, Object> context, IErrorKeeper ek, String startKey,
			String endKey) {
		Scope scope = getScope(req, context);

		if (scope == Scope.ABSOLUTE) {
			getAbsoluteScope(req, context, ek, startKey, endKey);
		} else {
			getRelativeScope(req, context, ek, startKey, endKey);
		}
	}

	private static void getRelativeScope(HttpServletRequest req,
			Map<String, Object> context, IErrorKeeper ek, String startKey,
			String endKey) {
		int lb = getLookback(req, context, ek);
		context.put(endKey, getEndOfMonth(0));
		context.put(startKey, getBeginningOfMonth(-lb));
		getLastMonthPercentDone(context);
	}

	private static double getLastMonthPercentDone(Map<String, Object> context) {
		int end = getEndOfMonthCal(0).get(Calendar.DAY_OF_MONTH);
		int today = Calendar.getInstance().get(Calendar.DAY_OF_MONTH);
		double pct = (double) today / (double) end;
		context.put(LAST_MONTH_PERCENT_DONE, pct);
		return pct;
	}

	private static int getLookback(HttpServletRequest req,
			Map<String, Object> context, IErrorKeeper ek) {
		String param = req.getParameter(LOOKBACK);
		if (param == null) {
			return 4;
		}
		try {
			return Integer.parseInt(param.trim());
		} catch (Exception e) {
			String msg = String.format(
					"Invalid lookback (positive integer): %s", param);
			ek.insertErrorMessage(context, msg);
			e.printStackTrace();
			return 4;
		}
	}

	static void getAbsoluteScope(HttpServletRequest req,
			Map<String, Object> context, IErrorKeeper ek, String startKey,
			String endKey) {
		context.put(startKey, Time.getStartDate(req, context, ek));
		context.put(endKey, Time.getEndDate(req, context, ek));
		context.put(LAST_MONTH_PERCENT_DONE, 1.0);
		if (((Date)context.get(startKey)).after((Date)context.get(endKey))) {
			ek.insertErrorMessage(context, DATES_INVERTED);
		}
	}

	public static Date getStartDate(HttpServletRequest req,
			Map<String, Object> context, IErrorKeeper ek) {
		String param = req.getParameter("beginDate");
		if (param == null) {
			return Time.getDefaultStartDate();
		}
		try {
			return sdf.parse(param.trim());
		} catch (ParseException e) {
			String msg = String.format("Invalid Start Date yyyy-mm-dd: %s",
					param);
			ek.insertErrorMessage(context, msg);
			e.printStackTrace();
			return Time.getDefaultStartDate();
		}
	}

	public static Date getEndDate(HttpServletRequest req,
			Map<String, Object> context, IErrorKeeper ek) {
		String param = req.getParameter("endDate");
		if (param == null) {
			return Time.getDefaultEndDate();
		}
		try {
			return sdf.parse(param);
		} catch (ParseException e) {
			String msg = String
					.format("Invalid End Date yyyy-mm-dd: %s", param);
			ek.insertErrorMessage(context, msg);
			e.printStackTrace();
			return Time.getDefaultEndDate();
		}
	}

	public static Date getMonthsEarlier(String monthEnd, int n) {
		try {
			Date x = sdf.parse(monthEnd);
			Calendar cal = Calendar.getInstance();
			cal.setTime(x);
			cal.add(Calendar.MONTH, -n);
			return cal.getTime();
		} catch (ParseException e) {
			return null;
		}
	}

	public static Date getMonthsLater(String monthEnd, int n) {
		return getMonthsEarlier(monthEnd, -n);
	}

	public static final String START_DATE = "startDate";
	public static final String END_DATE = "endDate";

	public static int secondsRemainingTillMidnight() {
		Calendar c = Calendar.getInstance();
		c.add(Calendar.DAY_OF_MONTH, 1);
		c.set(Calendar.HOUR_OF_DAY, 0);
		c.set(Calendar.MINUTE, 0);
		c.set(Calendar.SECOND, 0);
		c.set(Calendar.MILLISECOND, 0);
		long howMany = (c.getTimeInMillis() - System.currentTimeMillis()) / 1000;
		return (int)howMany;
	}
}
