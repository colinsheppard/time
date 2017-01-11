package time.primitives;
import java.util.ArrayList;

import org.nlogo.api.Argument;
import org.nlogo.api.Command;
import org.nlogo.api.Context;
import org.nlogo.api.Dump;
import org.nlogo.api.ExtensionException;
import org.nlogo.api.LogoException;
import org.nlogo.api.Reporter;
import org.nlogo.core.LogoList;
import org.nlogo.core.Syntax;
import org.nlogo.core.SyntaxJ;
import org.nlogo.nvm.ExtensionContext;

import time.TimeEnums.GetTSMethod;
import time.TimeUtils;
import time.datatypes.LogoTime;
import time.datatypes.LogoTimeSeries;

public class TimeSeriesPrimitives {
	public static class TimeSeriesAddRow implements Command{
		public Syntax getSyntax() {
			return SyntaxJ.commandSyntax(new int[]{Syntax.WildcardType(),Syntax.WildcardType()});
		}
		public void perform(Argument args[], Context context) throws ExtensionException, LogoException {
			LogoTimeSeries ts = TimeUtils.getTimeSeriesFromArgument(args, 0);
			LogoList list = TimeUtils.getListFromArgument(args, 1);
			Object timeObj = list.get(0);
			LogoTime time = null;
			if (timeObj instanceof String) {
				time = new LogoTime(timeObj.toString());
			}else if (timeObj instanceof LogoTime) {
				// Create a new logotime since they are mutable
				time = new LogoTime((LogoTime)timeObj);
			}else{			
				throw new ExtensionException("time: was expecting a LogoTime object as the first item in the list passed as argument 2, found this instead: " + Dump.logoObject(timeObj));
			}
			if(list.size() != (ts.getNumColumns()+1)) throw new ExtensionException("time: cannot add "+(list.size()-1)+" values to a time series with "+ts.getNumColumns()+" columns.");
			ts.add(time,list.logoSublist(1, list.size()));
		}
	}
	
	public static class TimeSeriesCreate implements Reporter{
		public Syntax getSyntax() {
			return SyntaxJ.reporterSyntax(new int[]{Syntax.WildcardType()},Syntax.WildcardType());
		}
		public Object report(Argument args[], Context context) throws ExtensionException, LogoException {
			LogoList columnList;
			try{
				columnList = TimeUtils.getListFromArgument(args, 0);
			}catch(ExtensionException e){
				String colName = TimeUtils.getStringFromArgument(args, 0);
				ArrayList<String> cols = new ArrayList<String>();
				cols.add(colName);
				columnList = LogoList.fromJava(cols);
			}
			LogoTimeSeries ts = new LogoTimeSeries(columnList);
			return ts;
		}
	}
	
	public static class TimeSeriesGet implements Reporter{
		public Syntax getSyntax() {
			return SyntaxJ.reporterSyntax(new int[]{Syntax.WildcardType(),Syntax.WildcardType(),Syntax.StringType()},Syntax.WildcardType());
		}
		public Object report(Argument args[], Context context) throws ExtensionException, LogoException {
			LogoTimeSeries ts = TimeUtils.getTimeSeriesFromArgument(args, 0);
			LogoTime time = TimeUtils.getTimeFromArgument(args, 1);
			ts.ensureDateTypeConsistent(time);
			String columnName = TimeUtils.getStringFromArgument(args, 2);
			if(columnName.equals("ALL") || columnName.equals("all")){
				columnName = "ALL_-_COLUMNS";
			}
			return ts.getByTime(time, columnName, GetTSMethod.NEAREST);
		}
	}
	
	public static class TimeSeriesGetExact implements Reporter{
		public Syntax getSyntax() {
			return SyntaxJ.reporterSyntax(new int[]{Syntax.WildcardType(),Syntax.WildcardType(),Syntax.StringType()},Syntax.WildcardType());
		}
		public Object report(Argument args[], Context context) throws ExtensionException, LogoException {
			LogoTimeSeries ts = TimeUtils.getTimeSeriesFromArgument(args, 0);
			LogoTime time = TimeUtils.getTimeFromArgument(args, 1);
			ts.ensureDateTypeConsistent(time);
			String columnName = TimeUtils.getStringFromArgument(args, 2);
			if(columnName.equals("ALL") || columnName.equals("all")){
				columnName = "ALL_-_COLUMNS";
			}
			return ts.getByTime(time, columnName, GetTSMethod.EXACT);
		}
	}
	
	public static class TimeSeriesGetInterp implements Reporter{
		public Syntax getSyntax() {
			return SyntaxJ.reporterSyntax(new int[]{Syntax.WildcardType(),Syntax.WildcardType(),Syntax.StringType()},Syntax.WildcardType());
		}
		public Object report(Argument args[], Context context) throws ExtensionException, LogoException {
			LogoTimeSeries ts = TimeUtils.getTimeSeriesFromArgument(args, 0);
			LogoTime time = TimeUtils.getTimeFromArgument(args, 1);
			ts.ensureDateTypeConsistent(time);
			String columnName = TimeUtils.getStringFromArgument(args, 2);
			if(columnName.equals("ALL") || columnName.equals("all")){
				columnName = "ALL_-_COLUMNS";
			}
			return ts.getByTime(time, columnName, GetTSMethod.LINEAR_INTERP);
		}
	}

	public static class TimeSeriesGetRange implements Reporter{
		public Syntax getSyntax() {
			return SyntaxJ.reporterSyntax(new int[]{Syntax.WildcardType(),Syntax.WildcardType(),Syntax.WildcardType(),Syntax.StringType()},Syntax.WildcardType());
		}
		public Object report(Argument args[], Context context) throws ExtensionException, LogoException {
			LogoTimeSeries ts = TimeUtils.getTimeSeriesFromArgument(args, 0);
			LogoTime timeA = TimeUtils.getTimeFromArgument(args, 1);
			ts.ensureDateTypeConsistent(timeA);
			LogoTime timeB = TimeUtils.getTimeFromArgument(args, 2);
			ts.ensureDateTypeConsistent(timeB);
			String columnName = TimeUtils.getStringFromArgument(args, 3);
			if(columnName.equals("logotime")){
				columnName = "LOGOTIME";
			}
			if(columnName.equals("ALL") || columnName.equals("all")){
				columnName = "ALL_-_COLUMNS";
			}
			return ts.getRangeByTime(timeA, timeB, columnName);
		}
	}
	
	public static class TimeSeriesLoad implements Reporter{
		public Syntax getSyntax() {
			return SyntaxJ.reporterSyntax(new int[]{Syntax.StringType()},Syntax.WildcardType());
		}
		public Object report(Argument args[], Context context) throws ExtensionException, LogoException {
			String filename = TimeUtils.getStringFromArgument(args, 0);
			LogoTimeSeries ts = new LogoTimeSeries(filename, (ExtensionContext) context);
			return ts;
		}
	}
	
	public static class TimeSeriesLoadWithFormat implements Reporter{
		public Syntax getSyntax() {
			return SyntaxJ.reporterSyntax(new int[]{Syntax.StringType(),Syntax.StringType()},Syntax.WildcardType());
		}
		public Object report(Argument args[], Context context) throws ExtensionException, LogoException {
			String filename = TimeUtils.getStringFromArgument(args, 0);
			String format = TimeUtils.getStringFromArgument(args, 1);
			LogoTimeSeries ts = new LogoTimeSeries(filename, format, (ExtensionContext) context);
			return ts;
		}
	}
	
	public static class TimeSeriesWrite implements Command{
		public Syntax getSyntax() {
			return SyntaxJ.commandSyntax(new int[]{Syntax.WildcardType(),Syntax.StringType()});
		}
		public void perform(Argument args[], Context context) throws ExtensionException, LogoException {
			LogoTimeSeries ts = TimeUtils.getTimeSeriesFromArgument(args, 0);
			String filename = TimeUtils.getStringFromArgument(args, 1);
			ts.write(filename,(ExtensionContext)context);
		}
	}
}