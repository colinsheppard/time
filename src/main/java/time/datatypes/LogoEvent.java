package time.datatypes;
import org.nlogo.api.Agent;
import org.nlogo.agent.AgentSet;
import org.nlogo.nvm.AnonymousCommand;
import org.nlogo.api.ExtensionException;
import org.nlogo.core.ExtensionObject;

import time.TimeEnums.PeriodType;
import time.TimeExtension;
import time.TimeUtils;

public class LogoEvent implements ExtensionObject{
	final long id;
	public Double tick = null;
	public AnonymousCommand task = null;
	public AgentSet agents = null;
	public Double repeatInterval = null;
	public PeriodType repeatIntervalPeriodType = null;
	public Boolean shuffleAgentSet = null;

	LogoEvent(org.nlogo.agent.AgentSet agents, AnonymousCommand  task, Double tick, Double repeatInterval, PeriodType repeatIntervalPeriodType, Boolean shuffleAgentSet) {
		this.agents = agents;
		this.task = task;
		this.tick = tick;
		this.repeatInterval = repeatInterval;
		this.repeatIntervalPeriodType = repeatIntervalPeriodType;
		this.shuffleAgentSet = shuffleAgentSet;
		this.id = TimeExtension.nextEvent;
		TimeExtension.nextEvent++;
	}
	public void replaceData(Agent agent, AnonymousCommand task, Double tick) {
		this.agents = agents;
		this.task = task;
		this.tick = tick;
	}
	/*
	 * If a repeatInterval is set, this method uses it to update it's tick field and then adds itself to the
	 * schedule argument.  The return value indicates whether the event was added to the schedule again.
	 */
	public Boolean reschedule(LogoSchedule callingSchedule) throws ExtensionException{
		if(repeatInterval == null)return false;
		if(repeatIntervalPeriodType == null){ // in this case we assume that repeatInterval is in the same units as tick
			this.tick = this.tick + repeatInterval;
		}else{
			LogoTime currentTime = callingSchedule.getCurrentTime();
			if(TimeExtension.debug)TimeUtils.printToConsole(TimeExtension.context, "resheduling: "+ repeatInterval + " " + repeatIntervalPeriodType + " ahead of " + currentTime + " or " + currentTime.getDifferenceBetween(callingSchedule.tickType, currentTime.plus(repeatIntervalPeriodType, repeatInterval))/callingSchedule.tickValue);
			this.tick = this.tick + currentTime.getDifferenceBetween(callingSchedule.tickType, currentTime.plus(repeatIntervalPeriodType, repeatInterval))/callingSchedule.tickValue;
			if(TimeExtension.debug)TimeUtils.printToConsole(TimeExtension.context, "event scheduled for tick: " + this.tick); 
		}
		return TimeExtension.schedule.scheduleTree.add(this);
	}
	public boolean equals(Object obj) {
		return this == obj;
	}
	public String getExtensionName() {
		return "time";
	}
	public String getNLTypeName() {
		return "event";
	}
	public boolean recursivelyEqual(Object arg0) {
		return equals(arg0);
	}
	public String dump(boolean arg0, boolean arg1, boolean arg2) {
		String result = tick + "\t";
		if(agents!=null){
			for(Agent agent : agents.agents()){
				result += agent.toString() + ";";
			}
			result = result.substring(0, result.length()-1);
		}
		String[] splitArr = task.procedure().nameToken().toString().split(":");
		result += "\t" + ((task==null)?"": (splitArr.length>1 ? splitArr[1].substring(0, splitArr[1].length()-1)+" " : "")+task.procedure().displayName());
		return result;
	}
}