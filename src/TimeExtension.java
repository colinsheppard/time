import org.nlogo.api.*;
import org.nlogo.nvm.ExtensionContext;
import org.nlogo.nvm.Workspace.OutputDestination;

import org.joda.time.*;
import org.joda.time.format.*;

public class TimeExtension extends org.nlogo.api.DefaultClassManager {
	
	public enum PeriodType {
		SECOND,MINUTE,HOUR,DAY,WEEK,MONTH,YEAR
	}

	public java.util.List<String> additionalJars() {
		java.util.List<String> list = new java.util.ArrayList<String>();
		return list;
	}
	
	private static boolean debug = true;

	private static class LogoTime implements org.nlogo.api.ExtensionObject {
		public DateTime datetime;
		private DateTimeFormatter fmt = DateTimeFormat.forPattern("yyyy-MM-dd HH:mm:ss.SSS");
		private Boolean isAnchored = false;
		private Double tickCount;
		private PeriodType tickType;
		private DateTime anchorTime;
		private World world;

		LogoTime(DateTime dt) {
			this.datetime = dt;
		}
		LogoTime(String dt) {
			if(dt.trim().equals("") || dt.trim().toLowerCase().equals("now")){
				this.datetime = new DateTime();
			}else{
				this.datetime = new DateTime(dt.trim());
			}
			// if we wanted to convert the time to UTC instead of the machine's default time zone, do this
			//this.datetime = (this.datetime.withChronology(ISOChronology.getInstance(DateTimeZone.forID("UTC"))));
		}
		public void setAnchor(Double tickCount, PeriodType tickType, World world){
			this.isAnchored = true;
			this.tickCount = tickCount;
			this.tickType = tickType;
			this.anchorTime = new DateTime(this.datetime);
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
			case HOUR:
				durDouble *= 60;
			case MINUTE:
				durDouble *= 60;
			case SECOND:
				durDouble *= 1000;
				break;
			case DAY:
				per = new Period(0,0,0,roundDouble(durDouble),0,0,0,0);
				break;
			case WEEK:
				per = new Period(0,0,roundDouble(durDouble),0,0,0,0,0);
				break;
			case MONTH:
				per = new Period(0,roundDouble(durDouble),0,0,0,0,0,0);
				break;
			case YEAR:
				per = new Period(roundDouble(durDouble),0,0,0,0,0,0,0);
				break;
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
		// time:anchor
		primManager.addPrimitive("anchor", new Anchor());
		// time:add
		primManager.addPrimitive("add", new Add());
		// time:advance
		primManager.addPrimitive("advance", new Advance());
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
			LogoTime time = null;
			if(args[0].get() instanceof LogoTime){
				time = getTimeFromArgument(args, 0);
			}else{
				time = new LogoTime(getStringFromArgument(args, 0));
			}
			time.setAnchor(getDoubleFromArgument(args, 1),
					stringToPeriodType(getStringFromArgument(args, 2)),
					((ExtensionContext)context).workspace().world());
			return time;
		}
	}
	public static class Add extends DefaultReporter {
		public Syntax getSyntax() {
			return Syntax.reporterSyntax(new int[]{Syntax.WildcardType(),Syntax.NumberType(),Syntax.StringType()},
					Syntax.WildcardType());
		}
		public Object report(Argument args[], Context context) throws ExtensionException, LogoException {
			LogoTime time = getTimeFromArgument(args,0);
			Duration dur = null;
			switch(stringToPeriodType(getStringFromArgument(args, 2))){
			case SECOND:
				dur = new Duration(dToL(getDoubleFromArgument(args, 1)*1000));
				break;
			case MINUTE:
				dur = new Duration(dToL(getDoubleFromArgument(args, 1)*60*1000));
				break;
			case HOUR:
				dur = new Duration(dToL(getDoubleFromArgument(args, 1)*3600*1000));
				break;
			case DAY:
				dur = new Duration(dToL(getDoubleFromArgument(args, 1)*24*3600*1000));
				break;
			case WEEK:
				dur = new Duration(dToL(getDoubleFromArgument(args, 1)*7*24*3600*1000));
				break;
			case MONTH:
				dur = new Duration(dToL(getDoubleFromArgument(args, 1)*(365.0/12)*24*3600*1000));
				break;
			case YEAR:
				dur = new Duration(dToL(getDoubleFromArgument(args, 1)*365*24*3600*1000));
				break;
			}
			return new LogoTime(time.datetime.plus(dur));
		}
	}
	public static class Advance extends DefaultReporter {
		public Syntax getSyntax() {
			return Syntax.reporterSyntax(new int[]{Syntax.WildcardType(),Syntax.NumberType(),Syntax.StringType()},
					Syntax.WildcardType());
		}
		public Object report(Argument args[], Context context) throws ExtensionException, LogoException {
			LogoTime time = getTimeFromArgument(args,0);
			Period per = null;
			switch(stringToPeriodType(getStringFromArgument(args, 2))){
			case SECOND:
				per = new Period(0,0,0,0,0,0,getIntFromArgument(args, 1),0);
				break;
			case MINUTE:
				per = new Period(0,0,0,0,0,getIntFromArgument(args, 1),0,0);
				break;
			case HOUR:
				per = new Period(0,0,0,0,getIntFromArgument(args, 1),0,0,0);
				break;
			case DAY:
				per = new Period(0,0,0,getIntFromArgument(args, 1),0,0,0,0);
				break;
			case WEEK:
				per = new Period(0,0,getIntFromArgument(args, 1),0,0,0,0,0);
				break;
			case MONTH:
				per = new Period(0,getIntFromArgument(args, 1),0,0,0,0,0,0);
				break;
			case YEAR:
				per = new Period(getIntFromArgument(args, 1),0,0,0,0,0,0,0);
				break;
			}
			return new LogoTime(time.datetime.plus(per));
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
		if(sType.equals("second")){
			return PeriodType.SECOND;
		}else if(sType.equals("minute")){
			return PeriodType.MINUTE;
		}else if(sType.equals("hour")){
			return PeriodType.HOUR;
		}else if(sType.equals("day")){
			return PeriodType.DAY;
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
		Object obj = args[argIndex].get();
		if (!(obj instanceof LogoTime)) {
			throw new ExtensionException("time: was expecting a LogoTime object as argument "+(argIndex+1)+", found this instead: " + Dump.logoObject(obj));
		}
		((LogoTime)obj).updateFromTick();
		return (LogoTime) obj;
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


