package time.datatypes;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.TreeSet;

import org.nlogo.agent.Agent;
import org.nlogo.agent.AgentIterator;
import org.nlogo.agent.ArrayAgentSet;
import org.nlogo.agent.TickCounter;
import org.nlogo.agent.TreeAgentSet;
import org.nlogo.agent.World;
import org.nlogo.agent.AgentSet;
import org.nlogo.nvm.AnonymousCommand;
import org.nlogo.api.AnonymousProcedure;
import org.nlogo.api.Argument;
import org.nlogo.api.Context;
import org.nlogo.api.ExtensionException;
import org.nlogo.api.LogoException;
import org.nlogo.core.AgentKindJ;
import org.nlogo.core.ExtensionObject;
import org.nlogo.core.LogoList;
import org.nlogo.nvm.ExtensionContext;

import time.TimeEnums.AddType;
import time.TimeEnums.PeriodType;
import time.TimeExtension;
import time.TimeUtils;

public class LogoSchedule implements ExtensionObject{
		LogoEventComparator comparator = new LogoEventComparator();
		public TreeSet<LogoEvent> scheduleTree = new TreeSet<LogoEvent>(comparator);
		TickCounter tickCounter = null;
		
		// The following three fields track an anchored schedule
		LogoTime timeAnchor = null;
		PeriodType tickType = null;
		Double tickValue = null;

		public LogoSchedule() {
		}
		public boolean equals(Object obj) {
			return this == obj;
		}
		public boolean isAnchored(){
			return timeAnchor != null;
		}
		public void anchorSchedule(LogoTime time, Double tickValue, PeriodType tickType){
			try {
				this.timeAnchor = new LogoTime(time);
				this.tickType = tickType;
				this.tickValue = tickValue;
			} catch (ExtensionException e) {
				e.printStackTrace();
			}
		}
		public Double timeToTick(LogoTime time) throws ExtensionException{
			if(this.timeAnchor.dateType != time.dateType)throw new ExtensionException("Cannot schedule event to occur at a LogoTime of type "+time.dateType.toString()+" because the schedule is anchored to a LogoTime of type "+this.timeAnchor.dateType.toString()+".  Types must be consistent.");
			return this.timeAnchor.getDifferenceBetween(this.tickType, time)/this.tickValue;
		}
		public void addEvent(Argument args[], Context context, AddType addType) throws ExtensionException, LogoException {
			String primName = null;
			Double eventTick = null;
			
			switch(addType){
			case DEFAULT:
				primName = "add";
				if(args.length<3)throw new ExtensionException("time:add must have 3 arguments: schedule agent task tick/time");
				break;
			case SHUFFLE:
				primName = "add-shuffled";
				if(args.length<3)throw new ExtensionException("time:add-shuffled must have 3 arguments: schedule agent task tick/time");
				break;
			case REPEAT:
				primName = "repeat";
				if(args.length<4)throw new ExtensionException("time:repeat must have 4 or 5 arguments: schedule agent task tick/time number (period-type)");
				break;
			case REPEAT_SHUFFLED:
				primName = "repeat-shuffled";
				if(args.length<4)throw new ExtensionException("time:repeat-shuffled must have 4 or 5 arguments: schedule agent task tick/time number (period-type)");
				break;
			}
			if (!(args[0].get() instanceof Agent) && !(args[0].get() instanceof AgentSet) && !((args[0].get() instanceof String) && args[0].get().toString().toLowerCase().equals("observer"))) 
				throw new ExtensionException("time:"+primName+" expecting an agent, agentset, or the string \"observer\" as the first argument");
			if (!(args[1].get() instanceof AnonymousCommand)) throw new ExtensionException("time:"+primName+" expecting a command task as the second argument");
			if(((AnonymousCommand)args[1].get()).formals().length > 0) throw new ExtensionException("time:"+primName+" expecting as the second argument a command task that takes no arguments of its own, but found a task which expects its own arguments, this kind of task is unsupported by the time extension.");
			if(args[2].get().getClass().equals(Double.class)){
				eventTick = args[2].getDoubleValue();
			}else if(args[2].get().getClass().equals(LogoTime.class)){
				if(!this.isAnchored())throw new ExtensionException("A LogoEvent can only be scheduled to occur at a LogoTime if the discrete event schedule has been anchored to a LogoTime, see time:anchor-schedule");
				eventTick = this.timeToTick(TimeUtils.getTimeFromArgument(args, 2));
			}else{
				throw new ExtensionException("time:"+primName+" expecting a number or logotime as the third argument");
			}
			if (eventTick < ((ExtensionContext)context).workspace().world().ticks()) throw new ExtensionException("Attempted to schedule an event for tick "+ eventTick +" which is before the present 'moment' of "+((ExtensionContext)context).workspace().world().ticks());
			
			PeriodType repeatIntervalPeriodType = null;
			Double repeatInterval = null;
			if(addType == AddType.REPEAT || addType == AddType.REPEAT_SHUFFLED){
				if (!args[3].get().getClass().equals(Double.class)) throw new ExtensionException("time:repeat expecting a number as the fourth argument");
				repeatInterval = args[3].getDoubleValue();
				if (repeatInterval <= 0) throw new ExtensionException("time:repeat the repeat interval must be a positive number");
				if(args.length == 5){
					if(!this.isAnchored())throw new ExtensionException("A LogoEvent can only be scheduled to repeat using a period type if the discrete event schedule has been anchored to a LogoTime, see time:anchor-schedule");
					repeatIntervalPeriodType = TimeUtils.stringToPeriodType(TimeUtils.getStringFromArgument(args, 4));
					if(repeatIntervalPeriodType != PeriodType.MONTH && repeatIntervalPeriodType != PeriodType.YEAR){
						repeatInterval = this.timeAnchor.getDifferenceBetween(this.tickType, this.timeAnchor.plus(repeatIntervalPeriodType, repeatInterval))/this.tickValue;
						if(TimeExtension.debug)TimeUtils.printToConsole(context, "from:"+repeatIntervalPeriodType+" to:"+this.tickType+" interval:"+repeatInterval);
						repeatIntervalPeriodType = null;
					}else{
						if(TimeExtension.debug)TimeUtils.printToConsole(context, "repeat every: "+ repeatInterval + " " + repeatIntervalPeriodType);
					}
				}
			}
			Boolean shuffleAgentSet = (addType == AddType.SHUFFLE || addType == AddType.REPEAT_SHUFFLED);

			AgentSet agentSet = null;
			if (args[0].get() instanceof org.nlogo.agent.Agent){
				Agent theAgent = (org.nlogo.agent.Agent)args[0].getAgent();
				agentSet = new ArrayAgentSet(AgentKindJ.Turtle(),theAgent.toString(),new Agent[]{ theAgent });
			}else if(args[0].get() instanceof AgentSet){
				agentSet = (AgentSet) args[0].getAgentSet();
			}else{
				// leave agentSet as null to signal observer should be used
			}
			LogoEvent event = new LogoEvent(agentSet,(AnonymousCommand) args[1].getCommand(),eventTick,repeatInterval,repeatIntervalPeriodType,shuffleAgentSet);
			if(TimeExtension.debug)TimeUtils.printToConsole(context,"scheduling event: "+event.dump(false, false, false));
			scheduleTree.add(event);
		}
		TickCounter getTickCounter() throws ExtensionException{
			if(tickCounter==null)throw new ExtensionException("Tick counter has not been initialized in time extension.");
			return tickCounter;
		}
		TickCounter getTickCounter(ExtensionContext context){
			if(tickCounter==null){
				tickCounter = context.workspace().world().tickCounter;
			}
			return tickCounter;
		}
		public void performScheduledTasks(Argument args[], Context context) throws ExtensionException, LogoException {
			performScheduledTasks(args,context,Double.MAX_VALUE);
		}	
		public void performScheduledTasks(Argument args[], Context context, LogoTime untilTime) throws ExtensionException, LogoException {
			if(!this.isAnchored())throw new ExtensionException("time:go-until can only accept a LogoTime as a stopping time if the schedule is anchored using time:anchore-schedule");
			if(TimeExtension.debug)TimeUtils.printToConsole(context,"timeAnchor: "+this.timeAnchor+" tickType: "+this.tickType+" tickValue:"+this.tickValue + " untilTime:" + untilTime);
			Double untilTick = this.timeAnchor.getDifferenceBetween(this.tickType, untilTime)/this.tickValue;
			performScheduledTasks(args,context,untilTick);
		}
		public void performScheduledTasks(Argument args[], Context context, Double untilTick) throws ExtensionException, LogoException {
			ExtensionContext extcontext = (ExtensionContext) context;
			Object[] emptyArgs = new Object[1]; // This extension is only for CommandTasks, so we know there aren't any args to pass in
			LogoEvent event = scheduleTree.isEmpty() ? null : scheduleTree.first();
			ArrayList<org.nlogo.agent.Agent> theAgents = new ArrayList<org.nlogo.agent.Agent>();
			while(event != null && event.tick <= untilTick){
				if(TimeExtension.debug)TimeUtils.printToConsole(context,"performing event-id: "+event.id+" for agent: "+event.agents+" at tick:"+event.tick + " ");
				if(TimeExtension.debug)TimeUtils.printToConsole(context,"tick counter before: "+getTickCounter(extcontext)+", "+getTickCounter(extcontext).ticks());
				getTickCounter(extcontext).tick(event.tick-getTickCounter(extcontext).ticks());
				if(TimeExtension.debug)TimeUtils.printToConsole(context,"tick counter after: "+getTickCounter(extcontext)+", "+getTickCounter(extcontext).ticks());
				
				if(event.agents == null){
					if(TimeExtension.debug)TimeUtils.printToConsole(context,"single agent");
					org.nlogo.nvm.Context nvmContext = new org.nlogo.nvm.Context(extcontext.nvmContext().job,
																					(org.nlogo.agent.Agent)extcontext.getAgent().world().observer(),
																					extcontext.nvmContext().ip,
																					extcontext.nvmContext().activation,
																					extcontext.workspace());
					event.task.perform(nvmContext, emptyArgs);
				}else{
					AgentIterator iter = null;
					if(event.shuffleAgentSet){
						iter = event.agents.shufflerator(extcontext.nvmContext().job.random);
					}else{
						iter = event.agents.iterator();
					}
					ArrayList<Agent> copy = new ArrayList<Agent>();
					while(iter.hasNext()){
						copy.add(iter.next());
					}
					for(Agent theAgent : copy){
						if(theAgent == null || theAgent.id == -1)continue;
						org.nlogo.nvm.Context nvmContext = new org.nlogo.nvm.Context(extcontext.nvmContext().job,theAgent,
								extcontext.nvmContext().ip,extcontext.nvmContext().activation,extcontext.workspace());
						if(extcontext.nvmContext().stopping)return;
						event.task.perform(nvmContext, emptyArgs);
						if(nvmContext.stopping)return;
					}
				}
				// Remove the current event as is from the schedule
				scheduleTree.remove(event);

				// Reschedule the event if necessary
				event.reschedule(this);

				// Grab the next event from the schedule
				event = scheduleTree.isEmpty() ? null : scheduleTree.first();
			}
			if(untilTick!=null && untilTick < Double.MAX_VALUE && untilTick > getTickCounter(extcontext).ticks()) getTickCounter(extcontext).tick(untilTick-getTickCounter(extcontext).ticks());
		}
		public LogoTime getCurrentTime() throws ExtensionException{
			if(!this.isAnchored())return null;
			if(TimeExtension.debug)TimeUtils.printToConsole(TimeExtension.context, "current time is: " + this.timeAnchor.plus(this.tickType,getTickCounter().ticks() / this.tickValue));
			return this.timeAnchor.plus(this.tickType,getTickCounter().ticks() / this.tickValue);
		}
		public String dump(boolean readable, boolean exporting, boolean reference) {
			StringBuilder buf = new StringBuilder();
			if (exporting) {
				buf.append("LogoSchedule");
				if (!reference) {
					buf.append(":");
				}
			}
			if (!(reference && exporting)) {
				buf.append(" [ ");
				java.util.Iterator<LogoEvent> iter = scheduleTree.iterator();
				while(iter.hasNext()){
					buf.append(((LogoEvent)iter.next()).dump(true, true, true));
					buf.append("\n");
				}
				buf.append("]");
			}
			return buf.toString();
		}
		public String getExtensionName() {
			return "time";
		}
		public String getNLTypeName() {
			return "schedule";
		}
		public boolean recursivelyEqual(Object arg0) {
			return equals(arg0);
		}
		public void clear() {
			scheduleTree.clear();
		}
	}