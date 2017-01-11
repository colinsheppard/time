package time.primitives;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.nlogo.api.Argument;
import org.nlogo.api.Context;
import org.nlogo.api.Command;
import org.nlogo.api.ExtensionException;
import org.nlogo.api.LogoException;
import org.nlogo.api.Reporter;
import org.nlogo.core.Syntax;
import org.nlogo.core.SyntaxJ;

import time.TimeEnums.AddType;
import time.datatypes.LogoTime;
import time.TimeExtension;
import time.TimeUtils;

public class DiscreteEventSchedulerPrimitives {
	
	public static class AddEvent implements Command {
		public Syntax getSyntax() {
			return SyntaxJ.commandSyntax(new int[]{Syntax.WildcardType(),
					Syntax.WildcardType(),
					Syntax.WildcardType()});
		}
		public void perform(Argument args[], Context context) throws ExtensionException, LogoException {
			TimeExtension.schedule.addEvent(args,context,AddType.DEFAULT);
		}
	}
	
	public static class AddEventShuffled implements Command {
		public Syntax getSyntax() {
			return SyntaxJ.commandSyntax(new int[]{Syntax.WildcardType(),
					Syntax.WildcardType(),
					Syntax.WildcardType()});
		}
		public void perform(Argument args[], Context context) throws ExtensionException, LogoException {
			TimeExtension.schedule.addEvent(args,context,AddType.SHUFFLE);
		}
	}
	
	public static class AnchorSchedule implements Command {
		public Syntax getSyntax() {
			return SyntaxJ.commandSyntax(new int[]{Syntax.WildcardType(),Syntax.NumberType(),Syntax.StringType()});
		}
		public void perform(Argument args[], Context context)
				throws ExtensionException, LogoException {
			TimeExtension.schedule.anchorSchedule(TimeUtils.getTimeFromArgument(args, 0),TimeUtils.getDoubleFromArgument(args, 1),TimeUtils.stringToPeriodType(TimeUtils.getStringFromArgument(args, 2)));
		}
	}
	
	public static class ClearSchedule implements Command {
		public Syntax getSyntax() {
			return SyntaxJ.commandSyntax(new int[]{});
		}
		public void perform(Argument args[], Context context) throws ExtensionException, LogoException {
			TimeExtension.schedule.clear();
		}
	}
	
	public static class GetSize implements Reporter {
		public Syntax getSyntax() {
			return SyntaxJ.reporterSyntax(new int[]{},
					Syntax.NumberType());
		}
		public Object report(Argument args[], Context context)
				throws ExtensionException, LogoException {
			if(TimeExtension.debug)TimeUtils.printToConsole(context, "size of schedule: "+TimeExtension.schedule.scheduleTree.size());
			return new Double(TimeExtension.schedule.scheduleTree.size());
		}
	}
	
	public static class Go implements Command {
		public Syntax getSyntax() {
			return SyntaxJ.commandSyntax(new int[]{});
		}
		public void perform(Argument args[], Context context) throws ExtensionException, LogoException {
			TimeExtension.schedule.performScheduledTasks(args, context);
		}
	}
	
	public static class GoUntil implements Command {
		public Syntax getSyntax() {
			return SyntaxJ.commandSyntax(new int[]{Syntax.WildcardType()});
		}
		public void perform(Argument args[], Context context) throws ExtensionException, LogoException {
			LogoTime untilTime = null;
			Double untilTick = null;
			try{
				untilTime = TimeUtils.getTimeFromArgument(args, 0);
			}catch(ExtensionException e){
				untilTick = TimeUtils.getDoubleFromArgument(args, 0);
			}
			if(untilTime == null){
				TimeExtension.schedule.performScheduledTasks(args, context, untilTick);
			}else{
				TimeExtension.schedule.performScheduledTasks(args, context, untilTime);
			}
		}
	}
	
	public static class RepeatEvent implements Command {
		public Syntax getSyntax() {
			return SyntaxJ.commandSyntax(new int[]{Syntax.WildcardType(),
					Syntax.WildcardType(),
					Syntax.WildcardType(),
					Syntax.NumberType()});
		}
		public void perform(Argument args[], Context context) throws ExtensionException, LogoException {
			TimeExtension.schedule.addEvent(args,context,AddType.REPEAT);
		}
	}
	
	public static class RepeatEventShuffled implements Command {
		public Syntax getSyntax() {
			return SyntaxJ.commandSyntax(new int[]{Syntax.WildcardType(),
					Syntax.WildcardType(),
					Syntax.WildcardType(),
					Syntax.NumberType()});
		}
		public void perform(Argument args[], Context context) throws ExtensionException, LogoException {
			TimeExtension.schedule.addEvent(args,context,AddType.SHUFFLE);
		}
	}
	
	public static class RepeatEventShuffledWithPeriod implements Command {
		public Syntax getSyntax() {
			return SyntaxJ.commandSyntax(new int[]{Syntax.WildcardType(),
					Syntax.WildcardType(),
					Syntax.WildcardType(),
					Syntax.NumberType(),
					Syntax.StringType()});
		}
		public void perform(Argument args[], Context context) throws ExtensionException, LogoException {
			TimeExtension.schedule.addEvent(args,context,AddType.SHUFFLE);
		}
	}
	
	public static class RepeatEventWithPeriod implements Command {
		public Syntax getSyntax() {
			return SyntaxJ.commandSyntax(new int[]{Syntax.WildcardType(),
					Syntax.WildcardType(),
					Syntax.WildcardType(),
					Syntax.NumberType(),
					Syntax.StringType()});
		}
		public void perform(Argument args[], Context context) throws ExtensionException, LogoException {
			TimeExtension.schedule.addEvent(args,context,AddType.REPEAT);
		}
	}
	
	public static class ShowSchedule implements Reporter {
		public Syntax getSyntax() {
			return SyntaxJ.reporterSyntax(new int[]{},Syntax.StringType());
		}
		public Object report(Argument args[], Context context) throws ExtensionException, LogoException {
			return TimeExtension.schedule.dump(false,false,false);
		}
	}
	
	
}