import java.io.IOException;
import java.util.logging.FileHandler;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;

import org.nlogo.api.*;
import org.nlogo.nvm.ExtensionContext;
import org.nlogo.nvm.Workspace.OutputDestination;

import org.joda.time.*;
import org.joda.time.chrono.ISOChronology;
import org.joda.time.format.*;

public class TimeExtension extends org.nlogo.api.DefaultClassManager {

	public enum DateType {
		DATETIME,DATE,DAY
	}
	public enum PeriodType {
		MILLI,SECOND,MINUTE,HOUR,DAY,DAYOFYEAR,DAYOFWEEK,WEEK,MONTH,YEAR
	}

	public java.util.List<String> additionalJars() {
		java.util.List<String> list = new java.util.ArrayList<String>();
		return list;
	}

	private static boolean debug = true;

	private static class LogoTime implements org.nlogo.api.ExtensionObject {
		public DateType			dateType = null;
		public LocalDateTime 	datetime = null;
		public LocalDate 		date	 = null;
		public MonthDay 		monthDay = null;
		private DateTimeFormatter fmt = null;
		private Boolean 		isAnchored = false;
		private Double 			tickCount;
		private PeriodType 		tickType;
		private LocalDateTime 	anchorDatetime;
		private LocalDate 		anchorDate;
		private MonthDay 		anchorMonthDay;
		private World 			world;

		LogoTime(LocalDateTime dt) {
			this.datetime = dt;
			this.fmt = DateTimeFormat.forPattern("yyyy-MM-dd HH:mm:ss.SSS");
			this.dateType = DateType.DATETIME;
		}
		LogoTime(LocalDate dt) {
			this.date = dt;
			this.fmt = DateTimeFormat.forPattern("yyyy-MM-dd");
			this.dateType = DateType.DATE;
		}
		LogoTime(MonthDay dt) {
			this.monthDay = dt;
			this.fmt = DateTimeFormat.forPattern("MM-dd");
			this.dateType = DateType.DAY;
		}
		LogoTime(String dateString) throws ExtensionException {
			dateString = parseDateString(dateString);
			switch(this.dateType){
			case DATETIME:
				this.datetime = (dateString.length() == 0 || dateString.equals("now")) ? new LocalDateTime() : new LocalDateTime(dateString);
				this.fmt = DateTimeFormat.forPattern("yyyy-MM-dd HH:mm:ss.SSS");
				break;
			case DATE:
				this.date = new LocalDate(dateString);
				this.fmt = DateTimeFormat.forPattern("yyyy-MM-dd");
				break;
			case DAY:
				this.fmt = DateTimeFormat.forPattern("MM-dd");
				this.monthDay = (new MonthDay()).parse(dateString, this.fmt);
				break;
			}
		}
		/* 
		 * parseDateString
		 * 
		 * Accommodate shorthand and human readability, allowing substitution of space for 'T' and '/' for '-'.
		 * Also accommodate all three versions of specifying a full DATETIME (month, day, week -based) but only
		 * allow one specific way each to specify a DATE and a DAY. Single digit months and days are ok, but single
		 * digit hours, minutes, and seconds need a preceding zero (e.g. '06', not '6')
		 * 
		 * LEGIT
		 * 2012-11-10T09:08:07.654
		 * 2012/11/10T09:08:07.654
		 * 2012-11-10 09:08:07.654
		 * 2012/11/10 09:08:07.654
		 * 2012-11-10 09:08:07		// assumes 0 for millis
		 * 2012-11-10 09:08			// assumes 0 for seconds and millis
		 * 2012-11-10 09			// assumes 0 for minutes, seconds, and millis
		 * 2012-1-1 09:08:07.654
		 * 2012-01-1 09:08:07.654
		 * 2012-1-01 09:08:07.654
		 * 2012-01-01
		 * 2012-1-01
		 * 2012-01-1
		 * 01-01
		 * 1-01
		 * 01-1
		 * 
		 * NOT LEGIT
		 * 2012-11-10 9:08:07.654
		 * 2012-11-10 09:08:07.654
		 */
		//
		//
		String parseDateString(String dateString) throws ExtensionException{
			dateString = dateString.replace('/', '-').replace(' ', 'T').trim();
			int len = dateString.length();
			// First add 0's to pad single digit months / days if necessary
			int firstDash = dateString.indexOf('-');
			if(firstDash == 1 || firstDash == 2){ // DAY 
				if(firstDash == 1){ // month is single digit
					dateString = "0" + dateString;
					len++;
				}
				// Now check the day for a single digit
				if(len == 4){
					dateString = dateString.substring(0, 3) + "0" + dateString.substring(3, 4);
					len++;
				}else if(len < 5){
					throw new ExtensionException("Illegal time string: '" + dateString + "'"); 
				}
			}else if(firstDash != 4 && firstDash != -1){
				throw new ExtensionException("Illegal time string: '" + dateString + "'"); 
			}else{ // DATETIME or DATE
				int secondDash = dateString.lastIndexOf('-');
				if(secondDash == 6){ // month is single digit
					dateString = dateString.substring(0, 5) + "0" + dateString.substring(5, len);
					len++;
				}
				if(len == 9 || dateString.indexOf('T') == 9){ // day is single digit
					dateString = dateString.substring(0, 8) + "0" + dateString.substring(8, len);
					len++;
				}
			}
			if(len == 23 || len ==21 || len == 3 || len == 0){ // a full DATETIME
				this.dateType = DateType.DATETIME;
			}else if(len == 19 || len == 17){ // a DATETIME without millis
				this.dateType = DateType.DATETIME;
				dateString += ".000"; 
			}else if(len == 16 || len == 14){ // a DATETIME without seconds or millis
				this.dateType = DateType.DATETIME;
				dateString += ":00.000"; 
			}else if(len == 13 || len == 11){ // a DATETIME without minutes, seconds or millis
				this.dateType = DateType.DATETIME;
				dateString += ":00:00.000"; 
			}else if(len == 10){ // a DATE
				this.dateType = DateType.DATE;
			}else if(len == 5){ // a DAY
				this.dateType = DateType.DAY;
			}else{
				throw new ExtensionException("Illegal time string: '" + dateString + "'"); 
			}
			return dateString;
		}
		public void setAnchor(Double tickCount, PeriodType tickType, World world) throws ExtensionException{
			if(tickType == PeriodType.DAYOFWEEK)throw new ExtensionException(tickType.toString() + " type is not a supported tick type");
			this.isAnchored = true;
			this.tickCount = tickCount;
			this.tickType = tickType;
			switch(this.dateType){
			case DATETIME:
				this.anchorDatetime = new LocalDateTime(this.datetime);
				break;
			case DATE:
				this.anchorDate = new LocalDate(this.date);
				break;
			case DAY:
				this.anchorMonthDay = new MonthDay(this.monthDay);
				break;
			}
			this.world = world;
		}

		public String dump(boolean arg0, boolean arg1, boolean arg2) {
			try {
				this.updateFromTick();
			} catch (ExtensionException e) {
				// ignore
			}
			switch(this.dateType){
			case DATETIME:
				return datetime.toString(this.fmt);
			case DATE:
				return date.toString(this.fmt);
			case DAY:
				return monthDay.toString(this.fmt);
			}
			return "";
		}

		public void updateFromTick() throws ExtensionException {
			if(!this.isAnchored)return;

			switch(this.dateType){
			case DATETIME:
				this.datetime = this.plus(this.anchorDatetime,this.tickType, this.world.ticks()*this.tickCount).datetime;
				break;
			case DATE:
				this.date = this.plus(this.anchorDate,this.tickType, this.world.ticks()*this.tickCount).date;
				break;
			case DAY:
				this.monthDay = this.plus(this.anchorMonthDay,this.tickType, this.world.ticks()*this.tickCount).monthDay;
				break;
			}
		}

		public String getExtensionName() {
			return "time";
		}

		public String getNLTypeName() {
			return "logotime";
		}

		public boolean recursivelyEqual(Object arg0) {
			return equals(arg0);
		}
		public String show(DateTimeFormatter fmt){
			return (this.date == null) ? this.datetime.toString(fmt) : this.date.toString(fmt);
		}
		public Integer get(PeriodType periodType) throws ExtensionException{
			Integer result = null;
			try{
				switch(periodType){
				case MILLI:
					switch(this.dateType){
					case DATETIME:
						result =  datetime.getMillisOfSecond();
						break;
					case DATE:
						result =  date.get(DateTimeFieldType.millisOfSecond());
						break;
					case DAY:
						result =  monthDay.get(DateTimeFieldType.millisOfSecond());
						break;
					}
					break;
				case SECOND:
					switch(this.dateType){
					case DATETIME:
						result =  datetime.getSecondOfMinute();
						break;
					case DATE:
						result =  date.get(DateTimeFieldType.secondOfMinute());
						break;
					case DAY:
						result =  monthDay.get(DateTimeFieldType.secondOfMinute());
						break;
					}
					break;
				case MINUTE:
					switch(this.dateType){
					case DATETIME:
						result =  datetime.getMinuteOfHour();
						break;
					case DATE:
						result =  date.get(DateTimeFieldType.minuteOfHour());
						break;
					case DAY:
						result =  monthDay.get(DateTimeFieldType.minuteOfHour());
						break;
					}
					break;
				case HOUR:
					switch(this.dateType){
					case DATETIME:
						result =  datetime.getHourOfDay();
						break;
					case DATE:
						result =  date.get(DateTimeFieldType.hourOfDay());
						break;
					case DAY:
						result =  monthDay.get(DateTimeFieldType.hourOfDay());
						break;
					}
					break;
				case DAY:
					switch(this.dateType){
					case DATETIME:
						result =  datetime.getDayOfMonth();
						break;
					case DATE:
						result =  date.get(DateTimeFieldType.dayOfMonth());
						break;
					case DAY:
						result =  monthDay.get(DateTimeFieldType.dayOfMonth());
						break;
					}
					break;
				case DAYOFYEAR:
					switch(this.dateType){
					case DATETIME:
						result =  datetime.getDayOfYear();
						break;
					case DATE:
						result =  date.get(DateTimeFieldType.dayOfYear());
						break;
					case DAY:
						result =  monthDay.get(DateTimeFieldType.dayOfYear());
						break;
					}
					break;
				case DAYOFWEEK:
					switch(this.dateType){
					case DATETIME:
						result =  datetime.getDayOfWeek();
						break;
					case DATE:
						result =  date.get(DateTimeFieldType.dayOfWeek());
						break;
					case DAY:
						result =  monthDay.get(DateTimeFieldType.dayOfWeek());
						break;
					}
					break;
				case WEEK:
					switch(this.dateType){
					case DATETIME:
						result =  datetime.getWeekOfWeekyear();
						break;
					case DATE:
						result =  date.get(DateTimeFieldType.weekOfWeekyear());
						break;
					case DAY:
						result =  monthDay.get(DateTimeFieldType.weekOfWeekyear());
						break;
					}
					break;
				case MONTH:
					switch(this.dateType){
					case DATETIME:
						result =  datetime.getMonthOfYear();
						break;
					case DATE:
						result =  date.get(DateTimeFieldType.monthOfYear());
						break;
					case DAY:
						result =  monthDay.get(DateTimeFieldType.monthOfYear());
						break;
					}
					break;
				case YEAR:
					switch(this.dateType){
					case DATETIME:
						result =  datetime.getYear();
						break;
					case DATE:
						result =  date.get(DateTimeFieldType.year());
						break;
					case DAY:
						result =  monthDay.get(DateTimeFieldType.year());
						break;
					}
					break;
				}
			}catch(IllegalArgumentException e){
				throw new ExtensionException("Period type "+periodType.toString()+" is not defined for the time "+this.dump(true,true,true));
			}
			return result;
		}
		public LogoTime plus(PeriodType pType, Double durVal) throws ExtensionException{
			switch(this.dateType){
			case DATETIME:
				return this.plus(this.datetime,pType,durVal);
			case DATE:
				return this.plus(this.date,pType,durVal);
			case DAY:
				return this.plus(this.monthDay,pType,durVal);
			}
			return null;
		}
		public LogoTime plus(Object refTime, PeriodType pType, Double durVal) throws ExtensionException{
			Period per = null;
			switch(pType){
			case WEEK:
				durVal *= 7;
			case DAY:
			case DAYOFYEAR:
				durVal *= 24;
			case HOUR:
				durVal *= 60;
			case MINUTE:
				durVal *= 60;
			case SECOND:
				durVal *= 1000;
			case MILLI:
				break;
			case MONTH:
				per = new Period(0,roundDouble(durVal),0,0,0,0,0,0);
				break;
			case YEAR:
				per = new Period(roundDouble(durVal),0,0,0,0,0,0,0);
				break;
			default:
				throw new ExtensionException(pType+" type is not supported by the time:plus primitive");
			}
			switch(this.dateType){
			case DATETIME:
				if(per==null){
					return new LogoTime(((LocalDateTime)refTime).plus(new Duration(dToL(durVal))));
				}else{
					return new LogoTime(((LocalDateTime)refTime).plus(per));
				}
			case DATE:
				if(per==null){
					Integer dayDurVal = ((Double)(durVal / (24.0*60.0*60.0*1000.0))).intValue();
					return new LogoTime(((LocalDate)refTime).plusDays(dayDurVal));
				}else{
					return new LogoTime(((LocalDate)refTime).plus(per));
				}
			case DAY:
				if(per==null){
					Integer dayDurVal = ((Double)(durVal / (24.0*60.0*60.0*1000.0))).intValue();
					return new LogoTime(((MonthDay)refTime).plusDays(dayDurVal));
				}else{
					return new LogoTime(((MonthDay)refTime).plus(per));
				}
			}
			return null;
		}
		public boolean isBefore(LogoTime timeB)throws ExtensionException{
			if(this.dateType != timeB.dateType)throw new ExtensionException("time comparisons only work if the LogoTime's are the same variety, but you called with a "+this.dateType.toString()+" and a "+timeB.dateType.toString());
			switch(this.dateType){
			case DATETIME:
				return this.datetime.isBefore(timeB.datetime);
			case DATE:
				return this.date.isBefore(timeB.date);
			case DAY:
				return this.monthDay.isBefore(timeB.monthDay);
			}
			return true;
		}
		public boolean isEqual(LogoTime timeB)throws ExtensionException{
			if(this.dateType != timeB.dateType)throw new ExtensionException("time comparisons only work if the LogoTime's are the same variety, but you called with a "+this.dateType.toString()+" and a "+timeB.dateType.toString());
			switch(this.dateType){
			case DATETIME:
				return this.datetime.isEqual(timeB.datetime);
			case DATE:
				return this.date.isEqual(timeB.date);
			case DAY:
				return this.monthDay.isEqual(timeB.monthDay);
			}
			return true;
		}
		public boolean isBetween(LogoTime timeA, LogoTime timeB)throws ExtensionException{
			if(this.dateType != timeA.dateType || this.dateType != timeB.dateType)throw new ExtensionException("time comparisons only work if the LogoTime's are the same variety, but you called with a "+
					this.dateType.toString()+", a "+timeA.dateType.toString()+", and a "+timeB.dateType.toString());
			switch(this.dateType){
			case DATETIME:
				return ((this.datetime.isAfter(timeA.datetime) && this.datetime.isBefore(timeB.datetime)) || this.datetime.isEqual(timeA.datetime) || this.datetime.isEqual(timeB.datetime));
			case DATE:
				return ((this.date.isAfter(timeA.date) && this.date.isBefore(timeB.date)) || this.date.isEqual(timeA.date) || this.date.isEqual(timeB.date));
			case DAY:
				return ((this.monthDay.isAfter(timeA.monthDay) && this.monthDay.isBefore(timeB.monthDay)) || this.monthDay.isEqual(timeA.monthDay) || this.monthDay.isEqual(timeB.monthDay));
			}
			return true;
		}
		public Double getDifferenceBetween(PeriodType pType, LogoTime endTime)throws ExtensionException{
			if(this.dateType != endTime.dateType)throw new ExtensionException("time comparisons only work if the LogoTimes are the same variety, but you called with a "+
					this.dateType.toString()+" and a "+endTime.dateType.toString());
			Double durVal = 1.0;
			switch(pType){
			case YEAR:
				switch(this.dateType){
				case DATETIME:
					return intToDouble((new Period(this.datetime,endTime.datetime)).getYears());
				case DATE:
					return intToDouble((new Period(this.date,endTime.date)).getYears());
				case DAY:
					throw new ExtensionException(pType+" type is not supported by the time:difference-between primitive with LogoTimes of type DAY");
				}
			case MONTH:
				switch(this.dateType){
				case DATETIME:
					return intToDouble((new Period(this.datetime,endTime.datetime)).getMonths());
				case DATE:
					return intToDouble((new Period(this.date,endTime.date)).getMonths());
				case DAY:
					return intToDouble((new Period(this.monthDay,endTime.monthDay)).getMonths());
				}
			case WEEK:
				durVal /= 7.0;
			case DAY:
			case DAYOFYEAR:
				durVal /= 24.0;
			case HOUR:
				durVal /= 60.0;
			case MINUTE:
				durVal /= 60.0;
			case SECOND:
				durVal /= 1000.0;
			case MILLI:
				DateTime refDateTime = new DateTime(ISOChronology.getInstanceUTC());
				switch(this.dateType){
				case DATETIME:
					return durVal * (new Duration(this.datetime.toDateTime(refDateTime),endTime.datetime.toDateTime(refDateTime))).getMillis();
				case DATE:
					return durVal * (new Duration(this.date.toDateTime(refDateTime),endTime.date.toDateTime(refDateTime))).getMillis();
				case DAY:
					return durVal * (new Duration(this.monthDay.toLocalDate(2000).toDateTime(refDateTime),endTime.monthDay.toLocalDate(2000).toDateTime(refDateTime))).getMillis();
				}
			default:
				throw new ExtensionException(pType+" type is not supported by the time:difference-between primitive");
			}
		}
	}

	//
	public void load(org.nlogo.api.PrimitiveManager primManager) {
		// time:create
		primManager.addPrimitive("create", new Create());
		// time:anchor-to-ticks
		primManager.addPrimitive("anchor-to-ticks", new Anchor());
		// time:plus
		primManager.addPrimitive("plus", new Plus());
		// time:show
		primManager.addPrimitive("show", new Show());
		// time:get
		primManager.addPrimitive("get", new Get());
		// time:is-before
		primManager.addPrimitive("is-before", new IsBefore());
		// time:is-after
		primManager.addPrimitive("is-after", new IsAfter());
		// time:is-equal
		primManager.addPrimitive("is-equal", new IsEqual());
		// time:is-between
		primManager.addPrimitive("is-between", new IsBetween());
		// time:difference-between
		primManager.addPrimitive("difference-between", new DifferenceBetween());
	}

	public static class Create extends DefaultReporter {
		public Syntax getSyntax() {
			return Syntax.reporterSyntax(new int[]{Syntax.StringType()},
					Syntax.WildcardType());
		}
		public Object report(Argument args[], Context context) throws ExtensionException, LogoException {
			LogoTime time = new LogoTime(args[0].getString());
			return time;
		}
	}
	public static class Anchor extends DefaultReporter {
		public Syntax getSyntax() {
			return Syntax.reporterSyntax(new int[]{Syntax.WildcardType(),Syntax.NumberType(),Syntax.StringType()},
					Syntax.WildcardType());
		}
		public Object report(Argument args[], Context context) throws ExtensionException, LogoException {
			LogoTime time = getTimeFromArgument(args, 0);
			time.setAnchor(getDoubleFromArgument(args, 1),
					stringToPeriodType(getStringFromArgument(args, 2)),
					((ExtensionContext)context).workspace().world());
			return time;
		}
	}
	public static class Show extends DefaultReporter {
		public Syntax getSyntax() {
			return Syntax.reporterSyntax(new int[]{Syntax.WildcardType(),Syntax.StringType()},
					Syntax.StringType());
		}
		public Object report(Argument args[], Context context) throws ExtensionException, LogoException {
			LogoTime time = getTimeFromArgument(args, 0);
			String fmtString = getStringFromArgument(args, 1);
			DateTimeFormatter fmt = null;
			if(fmtString.trim().equals("")){
				fmt = DateTimeFormat.forPattern("yyyy-MM-dd HH:mm:ss.SSS");
			}else{
				fmt = DateTimeFormat.forPattern(fmtString);
			}
			return time.show(fmt);
		}
	}
	public static class Get extends DefaultReporter {
		public Syntax getSyntax() {
			return Syntax.reporterSyntax(new int[]{Syntax.StringType(),Syntax.WildcardType()},
					Syntax.NumberType());
		}
		public Object report(Argument args[], Context context) throws ExtensionException, LogoException {
			PeriodType periodType = stringToPeriodType(getStringFromArgument(args, 0));
			LogoTime time = getTimeFromArgument(args, 1);
			return time.get(periodType).doubleValue();
		}
	}
	public static class Plus extends DefaultReporter {
		public Syntax getSyntax() {
			return Syntax.reporterSyntax(new int[]{Syntax.WildcardType(),Syntax.NumberType(),Syntax.StringType()},
					Syntax.WildcardType());
		}
		public Object report(Argument args[], Context context) throws ExtensionException, LogoException {
			LogoTime time = getTimeFromArgument(args,0);
			return time.plus(stringToPeriodType(getStringFromArgument(args, 2)), getDoubleFromArgument(args, 1));
		}
	}
	public static class IsBefore extends DefaultReporter {
		public Syntax getSyntax() {
			return Syntax.reporterSyntax(new int[]{Syntax.WildcardType(),Syntax.WildcardType()},
					Syntax.BooleanType());
		}
		public Object report(Argument args[], Context context) throws ExtensionException, LogoException {
			LogoTime timeA = getTimeFromArgument(args,0);
			LogoTime timeB = getTimeFromArgument(args,1);
			return timeA.isBefore(timeB);
		}
	}
	public static class IsAfter extends DefaultReporter {
		public Syntax getSyntax() {
			return Syntax.reporterSyntax(new int[]{Syntax.WildcardType(),Syntax.WildcardType()},
					Syntax.BooleanType());
		}
		public Object report(Argument args[], Context context) throws ExtensionException, LogoException {
			LogoTime timeA = getTimeFromArgument(args,0);
			LogoTime timeB = getTimeFromArgument(args,1);
			return !timeA.isBefore(timeB);
		}
	}
	public static class IsEqual extends DefaultReporter {
		public Syntax getSyntax() {
			return Syntax.reporterSyntax(new int[]{Syntax.WildcardType(),Syntax.WildcardType()},
					Syntax.BooleanType());
		}
		public Object report(Argument args[], Context context) throws ExtensionException, LogoException {
			LogoTime timeA = getTimeFromArgument(args,0);
			LogoTime timeB = getTimeFromArgument(args,1);
			return timeA.isEqual(timeB);
		}
	}
	public static class IsBetween extends DefaultReporter {
		public Syntax getSyntax() {
			return Syntax.reporterSyntax(new int[]{Syntax.WildcardType(),Syntax.WildcardType(),Syntax.WildcardType()},
					Syntax.BooleanType());
		}
		public Object report(Argument args[], Context context) throws ExtensionException, LogoException {
			LogoTime timeA = getTimeFromArgument(args,0);
			LogoTime timeB = getTimeFromArgument(args,1);
			LogoTime timeC = getTimeFromArgument(args,2);
			return timeA.isBetween(timeB,timeC);
		}
	}
	public static class DifferenceBetween extends DefaultReporter {
		public Syntax getSyntax() {
			return Syntax.reporterSyntax(new int[]{Syntax.WildcardType(),Syntax.WildcardType(),Syntax.StringType()},
					Syntax.NumberType());
		}
		public Object report(Argument args[], Context context) throws ExtensionException, LogoException {
			LogoTime startTime = getTimeFromArgument(args,0);
			LogoTime endTime = getTimeFromArgument(args,1);
			PeriodType pType = stringToPeriodType(getStringFromArgument(args, 2));
			return startTime.getDifferenceBetween(pType, endTime);
		}
	}

	private static void printToConsole(Context context, String msg) throws ExtensionException{
		try {
			ExtensionContext extcontext = (ExtensionContext) context;
			extcontext.workspace().outputObject(msg,null, true, true,OutputDestination.OUTPUT_AREA);
		} catch (LogoException e) {
			throw new ExtensionException(e);
		}
	}
	private static Long dToL(double d){
		return ((Double)d).longValue();
	}
	private static TimeExtension.PeriodType stringToPeriodType(String sType) throws ExtensionException{
		sType = sType.trim().toLowerCase();
		if(sType.substring(sType.length()-1).equals("s"))sType = sType.substring(0,sType.length()-1);
		if(sType.equals("milli")){
			return PeriodType.MILLI;
		}else if(sType.equals("second")){
			return PeriodType.SECOND;
		}else if(sType.equals("minute")){
			return PeriodType.MINUTE;
		}else if(sType.equals("hour")){
			return PeriodType.HOUR;
		}else if(sType.equals("day") || sType.equals("dayofmonth") || sType.equals("dom")){
			return PeriodType.DAY;
		}else if(sType.equals("doy") || sType.equals("dayofyear") || sType.equals("julianday") || sType.equals("jday")){
			return PeriodType.DAYOFYEAR;
		}else if(sType.equals("dayofweek") || sType.equals("dow") || sType.equals("weekday") || sType.equals("wday")){
			return PeriodType.DAYOFWEEK;
		}else if(sType.equals("week")){
			return PeriodType.WEEK;
		}else if(sType.equals("month")){
			return PeriodType.MONTH;
		}else if(sType.equals("year")){
			return PeriodType.YEAR;
		}else{
			throw new ExtensionException("illegal time period type: "+sType);
		}
	}

	/*
	 * Convenience methods, to error check and extract a object from an Argument. 
	 */
	private static LogoTime getTimeFromArgument(Argument args[], Integer argIndex) throws ExtensionException, LogoException {
		LogoTime time = null;
		Object obj = args[argIndex].get();
		if (obj instanceof String) {
			time = new LogoTime(args[argIndex].getString());
		}else if (obj instanceof LogoTime) {
			time = (LogoTime) obj;
		}else{			
			throw new ExtensionException("time: was expecting a LogoTime object as argument "+(argIndex+1)+", found this instead: " + Dump.logoObject(obj));
		}
		time.updateFromTick();
		return time;
	}
	private static Double getDoubleFromArgument(Argument args[], Integer argIndex) throws ExtensionException, LogoException {
		Object obj = args[argIndex].get();
		if (!(obj instanceof Double)) {
			throw new ExtensionException("time: was expecting a number as argument "+(argIndex+1)+", found this instead: " + Dump.logoObject(obj));
		}
		return (Double) obj;
	}
	private static Integer getIntFromArgument(Argument args[], Integer argIndex) throws ExtensionException, LogoException {
		Object obj = args[argIndex].get();
		if (obj instanceof Double) {
			// Round to nearest int
			return roundDouble((Double)obj);
		}else if (!(obj instanceof Integer)) {
			throw new ExtensionException("time: was expecting a number as argument "+(argIndex+1)+", found this instead: " + Dump.logoObject(obj));
		}
		return (Integer) obj;
	}
	private static Long getLongFromArgument(Argument args[], Integer argIndex) throws ExtensionException, LogoException {
		Object obj = args[argIndex].get();
		if (obj instanceof Double) {
			return ((Double)obj).longValue();
		}else if (!(obj instanceof Integer)) {
			throw new ExtensionException("time: was expecting a number as argument "+(argIndex+1)+", found this instead: " + Dump.logoObject(obj));
		}
		return (Long) obj;
	}
	private static String getStringFromArgument(Argument args[], Integer argIndex) throws ExtensionException, LogoException {
		Object obj = args[argIndex].get();
		if (!(obj instanceof String)) {
			throw new ExtensionException("time: was expecting a string as argument "+(argIndex+1)+", found this instead: " + Dump.logoObject(obj));
		}
		return (String) obj;
	}
	private static Integer roundDouble(Double d){
		return ((Long)Math.round(d)).intValue();
	}
	private static Double intToDouble(int i){
		return (new Integer(i)).doubleValue();
	}
	private static void printToLogfile(String msg){
		Logger logger = Logger.getLogger("MyLog");  
		FileHandler fh;  

		try {  
			// This block configure the logger with handler and formatter  
			fh = new FileHandler("logfile.txt",true);
			logger.addHandler(fh);  
			//logger.setLevel(Level.ALL);  
			SimpleFormatter formatter = new SimpleFormatter();  
			fh.setFormatter(formatter);  
			// the following statement is used to log any messages  
			logger.info(msg);
			fh.close();
		} catch (SecurityException e) {  
			e.printStackTrace();  
		} catch (IOException e) {  
			e.printStackTrace();  
		}  
	}
}


