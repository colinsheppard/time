package time;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.logging.FileHandler;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;

import org.nlogo.api.Argument;
import org.nlogo.api.Context;
import org.nlogo.api.Dump;
import org.nlogo.api.ExtensionException;
import org.nlogo.api.LogoException;
import org.nlogo.api.OutputDestination;
import org.nlogo.api.OutputDestinationJ;
import org.nlogo.core.LogoList;
import org.nlogo.nvm.ExtensionContext;
import org.nlogo.window.OutputArea;

import time.TimeEnums.PeriodType;
import time.datatypes.LogoSchedule;
import time.datatypes.LogoTime;
import time.datatypes.LogoTimeSeries;

public class TimeUtils {

	/***********************
	 * Convenience Methods
	 ***********************/
	public static Long dToL(double d){
		return ((Double)d).longValue();
	}
	public static PeriodType stringToPeriodType(String sType) throws ExtensionException{
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
	public static LogoTime getTimeFromArgument(Argument args[], Integer argIndex) throws ExtensionException, LogoException {
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
	public static Double getDoubleFromArgument(Argument args[], Integer argIndex) throws ExtensionException, LogoException {
		Object obj = args[argIndex].get();
		if (!(obj instanceof Double)) {
			throw new ExtensionException("time: was expecting a number as argument "+(argIndex+1)+", found this instead: " + Dump.logoObject(obj));
		}
		return (Double) obj;
	}
	public static LogoList getListFromArgument(Argument args[], Integer argIndex) throws ExtensionException, LogoException {
		Object obj = args[argIndex].get();
		if (!(obj instanceof LogoList)) {
			throw new ExtensionException("time: was expecting a list as argument "+(argIndex+1)+", found this instead: " + Dump.logoObject(obj));
		}
		return (LogoList) obj;
	}
	public static Integer getIntFromArgument(Argument args[], Integer argIndex) throws ExtensionException, LogoException {
		Object obj = args[argIndex].get();
		if (obj instanceof Double) {
			// Round to nearest int
			return roundDouble((Double)obj);
		}else if (!(obj instanceof Integer)) {
			throw new ExtensionException("time: was expecting a number as argument "+(argIndex+1)+", found this instead: " + Dump.logoObject(obj));
		}
		return (Integer) obj;
	}
	public static Long getLongFromArgument(Argument args[], Integer argIndex) throws ExtensionException, LogoException {
		Object obj = args[argIndex].get();
		if (obj instanceof Double) {
			return ((Double)obj).longValue();
		}else if (!(obj instanceof Integer)) {
			throw new ExtensionException("time: was expecting a number as argument "+(argIndex+1)+", found this instead: " + Dump.logoObject(obj));
		}
		return (Long) obj;
	}
	public static String getStringFromArgument(Argument args[], Integer argIndex) throws ExtensionException, LogoException {
		Object obj = args[argIndex].get();
		if (!(obj instanceof String)) {
			throw new ExtensionException("time: was expecting a string as argument "+(argIndex+1)+", found this instead: " + Dump.logoObject(obj));
		}
		return (String) obj;
	}
	public static LogoTimeSeries getTimeSeriesFromArgument(Argument args[], Integer argIndex) throws ExtensionException, LogoException {
		LogoTimeSeries ts = null;
		Object obj = args[argIndex].get();
		if (obj instanceof LogoTimeSeries) {
			ts = (LogoTimeSeries)obj;
		}else{
			throw new ExtensionException("time: was expecting a LogoTimeSeries object as argument "+(argIndex+1)+", found this instead: " + Dump.logoObject(obj));
		}
		return ts;
	}
	public static Integer roundDouble(Double d){
		return ((Long)Math.round(d)).intValue();
	}
	public static Double intToDouble(int i){
		return (new Integer(i)).doubleValue();
	}
	public static void printToLogfile(String msg){
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
	// Convenience method, to extract a schedule object from an Argument.
	public static LogoSchedule getScheduleFromArguments(Argument args[], int index) throws ExtensionException, LogoException {
		Object obj = args[index].get();
		if (!(obj instanceof LogoSchedule)) {
			throw new ExtensionException("Was expecting a LogoSchedule as argument "+(index+1)+" found this instead: " + Dump.logoObject(obj));
		}
		return (LogoSchedule) obj;
	}
	public static void printToConsole(Context context, String msg) throws ExtensionException{
		try {
			ExtensionContext extcontext = (ExtensionContext) context;
			extcontext.workspace().outputObject(msg,null, true, true,OutputDestinationJ.OUTPUT_AREA());
		    Files.write(Paths.get("/Users/critter/Dropbox/netlogo/time/debugging/log.txt"), (msg+"\n").getBytes(), StandardOpenOption.APPEND);
		} catch (LogoException e) {
			throw new ExtensionException(e);
		} catch (IOException e) {
			throw new ExtensionException(e);
		}
	}
	public static void setContext(Context context) {
		TimeExtension.context = context;
	}

}
