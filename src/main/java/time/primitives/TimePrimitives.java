package time.primitives;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.nlogo.api.Argument;
import org.nlogo.api.Context;
import org.nlogo.api.Reporter;
import org.nlogo.api.ExtensionException;
import org.nlogo.api.LogoException;
import org.nlogo.core.Syntax;
import org.nlogo.core.SyntaxJ;
import org.nlogo.nvm.ExtensionContext;

import time.TimeExtension;
import time.TimeUtils;
import time.TimeEnums.PeriodType;
import time.datatypes.LogoTime;

public class TimePrimitives {
	public static class NewLogoTime implements Reporter {
		public Syntax getSyntax() {
			return SyntaxJ.reporterSyntax(new int[]{Syntax.StringType()},
					Syntax.WildcardType());
		}
		public Object report(Argument args[], Context context) throws ExtensionException, LogoException {
			TimeExtension.context = context; // for debugging
			LogoTime time = new LogoTime(TimeUtils.getStringFromArgument(args, 0));
			return time;
		}
	}

	public static class Anchor implements Reporter {
		public Syntax getSyntax() {
			return SyntaxJ.reporterSyntax(new int[]{Syntax.WildcardType(),Syntax.NumberType(),Syntax.StringType()},
					Syntax.WildcardType());
		}
		public Object report(Argument args[], Context context) throws ExtensionException, LogoException {
			LogoTime time = TimeUtils.getTimeFromArgument(args, 0);
			LogoTime newTime = new LogoTime(time);
			newTime.setAnchor(TimeUtils.getDoubleFromArgument(args, 1),
					TimeUtils.stringToPeriodType(TimeUtils.getStringFromArgument(args, 2)),
					((ExtensionContext)context).workspace().world());
			return newTime;
		}
	}
	
	public static class Show implements Reporter {
		public Syntax getSyntax() {
			return SyntaxJ.reporterSyntax(new int[]{Syntax.WildcardType(),Syntax.StringType()},
					Syntax.StringType());
		}
		public Object report(Argument args[], Context context) throws ExtensionException, LogoException {
			LogoTime time = TimeUtils.getTimeFromArgument(args, 0);
			String fmtString = TimeUtils.getStringFromArgument(args, 1);
			DateTimeFormatter fmt = null;
			if(fmtString.trim().equals("")){
				fmt = DateTimeFormat.forPattern("yyyy-MM-dd HH:mm:ss.SSS");
			}else{
				fmt = DateTimeFormat.forPattern(fmtString);
			}
			return time.show(fmt);
		}
	}
	
	public static class Copy implements Reporter {
		public Syntax getSyntax() {
			return SyntaxJ.reporterSyntax(new int[]{Syntax.WildcardType()},
					Syntax.WildcardType());
		}
		public Object report(Argument args[], Context context) throws ExtensionException, LogoException {
			LogoTime time = TimeUtils.getTimeFromArgument(args, 0);
			return new LogoTime(time);
		}
	}
	
	public static class CreateWithFormat implements Reporter {
		public Syntax getSyntax() {
			return SyntaxJ.reporterSyntax(new int[]{Syntax.StringType(),Syntax.StringType()},
					Syntax.WildcardType());
		}
		public Object report(Argument args[], Context context) throws ExtensionException, LogoException {
			TimeExtension.context = context; // for debugging
			LogoTime time = new LogoTime(TimeUtils.getStringFromArgument(args, 0),TimeUtils.getStringFromArgument(args, 1));
			return time;
		}
	}
	
	public static class DifferenceBetween implements Reporter {
		public Syntax getSyntax() {
			return SyntaxJ.reporterSyntax(new int[]{Syntax.WildcardType(),Syntax.WildcardType(),Syntax.StringType()},
					Syntax.NumberType());
		}
		public Object report(Argument args[], Context context) throws ExtensionException, LogoException {
			LogoTime startTime = TimeUtils.getTimeFromArgument(args,0);
			LogoTime endTime = TimeUtils.getTimeFromArgument(args,1);
			PeriodType pType = TimeUtils.stringToPeriodType(TimeUtils.getStringFromArgument(args, 2));
			return startTime.getDifferenceBetween(pType, endTime);
		}
	}
	
	public static class Get implements Reporter {
		public Syntax getSyntax() {
			return SyntaxJ.reporterSyntax(new int[]{Syntax.StringType(),Syntax.WildcardType()},
					Syntax.NumberType());
		}
		public Object report(Argument args[], Context context) throws ExtensionException, LogoException {
			PeriodType periodType = TimeUtils.stringToPeriodType(TimeUtils.getStringFromArgument(args, 0));
			LogoTime time = TimeUtils.getTimeFromArgument(args, 1);
			return time.get(periodType).doubleValue();
		}
	}
	
	public static class IsAfter implements Reporter {
		public Syntax getSyntax() {
			return SyntaxJ.reporterSyntax(new int[]{Syntax.WildcardType(),Syntax.WildcardType()},
					Syntax.BooleanType());
		}
		public Object report(Argument args[], Context context) throws ExtensionException, LogoException {
			LogoTime timeA = TimeUtils.getTimeFromArgument(args,0);
			LogoTime timeB = TimeUtils.getTimeFromArgument(args,1);
			return !(timeA.isBefore(timeB) || timeA.isEqual(timeB));
		}
	}
	
	public static class IsBefore implements Reporter {
		public Syntax getSyntax() {
			return SyntaxJ.reporterSyntax(new int[]{Syntax.WildcardType(),Syntax.WildcardType()},
					Syntax.BooleanType());
		}
		public Object report(Argument args[], Context context) throws ExtensionException, LogoException {
			LogoTime timeA = TimeUtils.getTimeFromArgument(args,0);
			LogoTime timeB = TimeUtils.getTimeFromArgument(args,1);
			return timeA.isBefore(timeB);
		}
	}
	
	public static class IsBetween implements Reporter {
		public Syntax getSyntax() {
			return SyntaxJ.reporterSyntax(new int[]{Syntax.WildcardType(),Syntax.WildcardType(),Syntax.WildcardType()},
					Syntax.BooleanType());
		}
		public Object report(Argument args[], Context context) throws ExtensionException, LogoException {
			LogoTime timeA = TimeUtils.getTimeFromArgument(args,0);
			LogoTime timeB = TimeUtils.getTimeFromArgument(args,1);
			LogoTime timeC = TimeUtils.getTimeFromArgument(args,2);
			return timeA.isBetween(timeB,timeC);
		}
	}
	
	public static class IsEqual implements Reporter {
		public Syntax getSyntax() {
			return SyntaxJ.reporterSyntax(new int[]{Syntax.WildcardType(),Syntax.WildcardType()},
					Syntax.BooleanType());
		}
		public Object report(Argument args[], Context context) throws ExtensionException, LogoException {
			LogoTime timeA = TimeUtils.getTimeFromArgument(args,0);
			LogoTime timeB = TimeUtils.getTimeFromArgument(args,1);
			return timeA.isEqual(timeB);
		}
	}
	
	public static class Plus implements Reporter {
		public Syntax getSyntax() {
			return SyntaxJ.reporterSyntax(new int[]{Syntax.WildcardType(),Syntax.NumberType(),Syntax.StringType()},
					Syntax.WildcardType());
		}
		public Object report(Argument args[], Context context) throws ExtensionException, LogoException {
			LogoTime time = new LogoTime(TimeUtils.getTimeFromArgument(args,0));
			return time.plus(TimeUtils.stringToPeriodType(TimeUtils.getStringFromArgument(args, 2)), TimeUtils.getDoubleFromArgument(args, 1));
		}
	}
}