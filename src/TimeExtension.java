import org.nlogo.api.*;
import org.nlogo.nvm.ExtensionContext;
import org.nlogo.nvm.Workspace.OutputDestination;

import org.joda.time.*;
import org.joda.time.format.*;

public class TimeExtension extends org.nlogo.api.DefaultClassManager {
	
	public enum PeriodType {
		MILLI,SECOND,MINUTE,HOUR,DAY,DAYOFYEAR,DAYOFWEEK,WEEK,MONTH,YEAR
	}

	public java.util.List<String> additionalJars() {
		java.util.List<String> list = new java.util.ArrayList<String>();
		return list;
	}
	
	private static boolean debug = true;

	private static class LogoTime implements org.nlogo.api.ExtensionObject {
		public LocalDateTime datetime = new LocalDateTime();
		private DateTimeFormatter fmt = DateTimeFormat.forPattern("yyyy-MM-dd HH:mm:ss.SSS");
		private Boolean isAnchored = false;
		private Double tickCount;
		private PeriodType tickType;
		private LocalDateTime anchorTime;
		private World world;

		LogoTime(LocalDateTime dt) {
			this.datetime = dt;
		}
		LogoTime(String dt) {
			if(!(dt.trim().equals("") || dt.trim().toLowerCase().equals("now"))){
				this.datetime = this.datetime.parse(dt.trim());
			}
			// if we wanted to convert the time to UTC instead of the machine's default time zone, do this
			//this.datetime = (this.datetime.withChronology(ISOChronology.getInstance(DateTimeZone.forID("UTC"))));
		}
		public void setAnchor(Double tickCount, PeriodType tickType, World world) throws ExtensionException{
			if(tickType == PeriodType.DAYOFWEEK)throw new ExtensionException(tickType.toString() + " type is not a supported tick type");
			this.isAnchored = true;
			this.tickCount = tickCount;
			this.tickType = tickType;
			this.anchorTime = new LocalDateTime(this.datetime);
			this.world = world;
		}

		public String dump(boolean arg0, boolean arg1, boolean arg2) {
			this.updateFromTick();
			return datetime.toString(this.fmt);
		}
		
		public void updateFromTick(){
			if(!this.isAnchored)return;
			Double durDouble = world.ticks()*this.tickCount;
			Period per = null;
			switch(this.tickType){
			case WEEK:
				durDouble *= 7;
			case DAY:
			case DAYOFYEAR:
				durDouble *= 24;
			case HOUR:
				durDouble *= 60;
			case MINUTE:
				durDouble *= 60;
			case SECOND:
				durDouble *= 1000;
			case MILLI:
				break;
			case MONTH:
				per = new Period(0,roundDouble(durDouble),0,0,0,0,0,0);
				break;
			case YEAR:
				per = new Period(roundDouble(durDouble),0,0,0,0,0,0,0);
				break;
			default:
				return;
			}
			if(per==null){
				Duration dur = new Duration(dToL(durDouble));
				this.datetime = this.anchorTime.plus(dur);
			}else{
				this.datetime = this.anchorTime.plus(per);
			}
		}

		public String getExtensionName() {
			return "time";
		}

		public String getNLTypeName() {
			return "datetime";
		}

		public boolean recursivelyEqual(Object arg0) {
			return equals(arg0);
		}
	}

	///
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
			return time.datetime.toString(fmt);
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
			Integer result = null;
			switch(periodType){
			case MILLI:
				result = time.datetime.getMillisOfSecond();
				break;
			case SECOND:
				result = time.datetime.getSecondOfMinute();
				break;
			case MINUTE:
				result = time.datetime.getMinuteOfHour();
				break;
			case HOUR:
				result = time.datetime.getHourOfDay();
				break;
			case DAY:
				result = time.datetime.getDayOfMonth();
				break;
			case DAYOFYEAR:
				result = time.datetime.getDayOfYear();
				break;
			case DAYOFWEEK:
				result = time.datetime.getDayOfWeek();
				break;
			case WEEK:
				result = time.datetime.getWeekOfWeekyear();
				break;
			case MONTH:
				result = time.datetime.getMonthOfYear();
				break;
			case YEAR:
				result = time.datetime.getYear();
				break;
			}
			return result.doubleValue();
		}
	}
	public static class Plus extends DefaultReporter {
		public Syntax getSyntax() {
			return Syntax.reporterSyntax(new int[]{Syntax.WildcardType(),Syntax.NumberType(),Syntax.StringType()},
					Syntax.WildcardType());
		}
		public Object report(Argument args[], Context context) throws ExtensionException, LogoException {
			LogoTime time = getTimeFromArgument(args,0);
			Double doubleDur = getDoubleFromArgument(args, 1);
			Period per = null;
			switch(stringToPeriodType(getStringFromArgument(args, 2))){
			case WEEK:
				doubleDur *= 7;
			case DAY:
			case DAYOFYEAR:
				doubleDur *= 24;
			case HOUR:
				doubleDur *= 60;
			case MINUTE:
				doubleDur *= 60;
			case SECOND:
				doubleDur *= 1000;
			case MILLI:
				break;
			case MONTH:
				per = new Period(0,getIntFromArgument(args, 1),0,0,0,0,0,0);
				break;
			case YEAR:
				per = new Period(getIntFromArgument(args, 1),0,0,0,0,0,0,0);
				break;
			default:
				throw new ExtensionException(getStringFromArgument(args, 2)+" type is not supported by the time:plus primitive");
			}
			if(per==null){
				return new LogoTime(time.datetime.plus(new Duration(dToL(doubleDur))));
			}else{
				return new LogoTime(time.datetime.plus(per));
			}
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
}


