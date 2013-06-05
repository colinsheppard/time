import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.NoSuchElementException;
import java.util.TreeSet;
import java.util.TreeMap;
import java.util.LinkedHashMap;
import java.util.logging.FileHandler;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;
import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;

import org.nlogo.agent.AgentSet.Iterator;
import org.nlogo.agent.ArrayAgentSet;
import org.nlogo.agent.TickCounter;
import org.nlogo.agent.TreeAgentSet;
import org.nlogo.agent.World;
import org.nlogo.api.*;
import org.nlogo.nvm.ExtensionContext;
import org.nlogo.nvm.Workspace.OutputDestination;

import org.joda.time.*;
import org.joda.time.chrono.ISOChronology;
import org.joda.time.format.*;


public class TimeExtension extends org.nlogo.api.DefaultClassManager {

	public enum AddType {
		DEFAULT, SHUFFLE, REPEAT
	}
	public enum DateType {
		DATETIME,DATE,DAY
	}
	public enum PeriodType {
		MILLI,SECOND,MINUTE,HOUR,DAY,DAYOFYEAR,DAYOFWEEK,WEEK,MONTH,YEAR
	}
	public enum DataType {
		BOOLEAN,INTEGER,DOUBLE,STRING;
	}
	public enum GetTSMethod{
		EXACT,NEAREST,LINEAR_INTERP;
	}
	public java.util.List<String> additionalJars() {
		java.util.List<String> list = new java.util.ArrayList<String>();
		list.add("joda-time-2.2.jar");
		return list;
	}

	private static final java.util.WeakHashMap<LogoSchedule, Long> schedules = new java.util.WeakHashMap<LogoSchedule, Long>();
	private static long nextSchedule = 0;
	private static final java.util.WeakHashMap<LogoEvent, Long> events = new java.util.WeakHashMap<LogoEvent, Long>();
	private static long nextEvent = 0;
	private static boolean debug = true;

	public void load(org.nlogo.api.PrimitiveManager primManager) {
		/**********************
		/* TIME PRIMITIVES
		/**********************/
		// time:create
		primManager.addPrimitive("create", new NewLogoTime());
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

		/********************************************
		/* DISCRETE EVENT SIMULATION PRIMITIVES
		/*******************************************/
		// time:size-of
		primManager.addPrimitive("size-of", new GetSize());
		// time:first-event
		primManager.addPrimitive("first-event", new FirstEvent());
		// time:next-event
		primManager.addPrimitive("next-event", new NextEvent());
		// time:add-event
		primManager.addPrimitive("add-event", new AddEvent());
		// time:add-event-shuffled
		primManager.addPrimitive("add-event-shuffled", new AddEventShuffled());
		// time:repeat-event
		primManager.addPrimitive("repeat-event", new RepeatEvent());
		// time:create-schedule
		primManager.addPrimitive("create-schedule", new NewLogoSchedule());
		// time:anchor-schedule
		primManager.addPrimitive("anchor-schedule", new AnchorSchedule());
		// time:go
		primManager.addPrimitive("go", new Go());
		// time:go-until
		primManager.addPrimitive("go-until", new GoUntil());

		/**********************
		/* TIME SERIES PRIMITIVES
		/**********************/
		// time:load-ts
		primManager.addPrimitive("ts-load", new TimeSeriesLoad());
		// time:get
		primManager.addPrimitive("ts-get", new TimeSeriesGet());
		// time:get-interp
		primManager.addPrimitive("ts-get-interp", new TimeSeriesGetInterp());
		// time:get-exact
		primManager.addPrimitive("ts-get-exact", new TimeSeriesGetExact());
		// time:get-row
		primManager.addPrimitive("ts-get-row", new TimeSeriesGetRow());
		// time:get-row-interp
		primManager.addPrimitive("ts-get-row-interp", new TimeSeriesGetRowInterp());
		// time:get-row-exact
		primManager.addPrimitive("ts-get-row-exact", new TimeSeriesGetRowExact());
	}
	public void clearAll() {
		schedules.clear();
		nextSchedule = 0;
	}
	public class LogoTimeComparator implements Comparator<LogoTime> {
		public int compare(LogoTime a, LogoTime b) {
			return a.compareTo(b);
		}
	}
	static class TimeSeriesRecord {
		public LogoTime time;
		public int dataIndex;

		TimeSeriesRecord(LogoTime time,int i){
			this.time = time;
			this.dataIndex = i;
		}
	}
	@SuppressWarnings("unchecked")
	static class TimeSeriesColumn {
		public DataType dataType;
		@SuppressWarnings("rawtypes")
		public ArrayList data;

		TimeSeriesColumn(){
		}
		public void add(String value){
			if(this.dataType==null){
				try{
					Double.parseDouble(value);
					this.dataType = DataType.DOUBLE;
					this.data = new ArrayList<Double>();
					this.data.add(Double.parseDouble(value));
				}catch (Exception e3) {
					this.dataType = DataType.STRING;
					this.data = new ArrayList<String>();
					this.data.add(value);
				}
			}else{
				switch(dataType){
				case DOUBLE:
					this.data.add(Double.parseDouble(value));
					break;
				case STRING:
					this.data.add(value);
					break;
				}
			}
		}
	}
	static class LogoTimeSeries implements org.nlogo.api.ExtensionObject {
		TreeMap<LogoTime,TimeSeriesRecord> times = new TreeMap<LogoTime,TimeSeriesRecord>((new TimeExtension()).new LogoTimeComparator());
		LinkedHashMap<String,TimeSeriesColumn> columns = new LinkedHashMap<String,TimeSeriesColumn>();
		Integer numRows = 0;

		LogoTimeSeries(String filename) throws ExtensionException{
			parseTimeSeriesFile(filename);
		}
		public void parseTimeSeriesFile(String filename) throws ExtensionException{
			File dataFile = new File(filename);
			FileInputStream fstream;
			try {
				fstream = new FileInputStream(dataFile);
			} catch (FileNotFoundException e) {
				throw new ExtensionException(e.getMessage());
			}
			DataInputStream in = new DataInputStream(fstream);
			BufferedReader br = new BufferedReader(new InputStreamReader(in));
			int lineCount = 0;
			String delim = null, strLine = null;
			String[] lineData;

			// Read the header line and infer the delimiter (tab or comma)
			try {
				strLine = br.readLine();
			} catch (IOException e) {
				throw new ExtensionException(e.getMessage());
			}
			if(strLine==null)throw new ExtensionException("File "+dataFile+" is blank.");
			Boolean hasTab = strLine.contains("\t");
			Boolean hasCom = strLine.contains(",");
			if(hasTab && hasCom){
				throw new ExtensionException("Ambiguous file format in file "+dataFile+", the header line contains both a tab and a comma character, expecting one or the other.");
			}else if(hasTab){
				delim = "\t";
			}else if(hasCom){
				delim = ",";
			}else{
				throw new ExtensionException("Illegal file format in file "+dataFile+", the header line does not contain a tab or a comma character, expecting one or the other.");
			}
			// Parse the header and create the column objects (skipping the time column)
			String[] columnNames = strLine.split(delim);
			for(String columnName : Arrays.copyOfRange(columnNames, 1, columnNames.length)){
				columns.put(columnName, new TimeSeriesColumn());
			}
			// Read the rest of the data
			try{
				while ((strLine = br.readLine())!=null){
					lineData = strLine.split(delim);
					LogoTime newTime = new LogoTime(lineData[0]);
					times.put(newTime,new TimeSeriesRecord(newTime, numRows++));
					for(int colInd = 1; colInd <= columns.size(); colInd++){
						columns.get(columnNames[colInd]).add(lineData[colInd]);
					}
				}
			}catch (IOException e){
				throw new ExtensionException(e.getMessage());
			}
		}
		public Object getByTime(LogoTime time, String columnName, GetTSMethod getMethod) throws ExtensionException{
			ArrayList<String> columnList = new ArrayList<String>(columns.size());
			ArrayList<Object> resultList = new ArrayList<Object>(columns.size());
			if(columnName.equals("ALL_-_COLUMNS")){
				columnList.addAll(columns.keySet());
			}else if(!columns.containsKey(columnName)){
				throw new ExtensionException("The LogoTimeSeries does not contain the column "+columnName);
			}else{
				columnList.add(columnName);
			}
			for(String colName : columnList){
				if(times.get(time)!=null){
					resultList.add(columns.get(colName).data.get(times.get(time).dataIndex));
				}else{
					LogoTime higherKey = times.higherKey(time);
					LogoTime lowerKey = times.lowerKey(time);
					if(higherKey == null){
						resultList.add(columns.get(colName).data.get(times.get(lowerKey).dataIndex));
					}else if(lowerKey == null){
						resultList.add(columns.get(colName).data.get(times.get(higherKey).dataIndex));
					}else{
						switch(getMethod){
						case EXACT:
							resultList.add(columns.get(colName).data.get(times.get(time).dataIndex));
							break;
						case NEAREST:
							resultList.add(time.isCloserToAThanB(lowerKey, higherKey) ? 
									columns.get(colName).data.get(times.get(lowerKey).dataIndex) : 
									columns.get(colName).data.get(times.get(higherKey).dataIndex));
							break;
						case LINEAR_INTERP:
							if(columns.get(colName).data.get(0) instanceof String)throw new ExtensionException("Cannot interpolate between string values, use time:get instead.");
							resultList.add( (Double)columns.get(colName).data.get(times.get(lowerKey).dataIndex) + 
									((Double)columns.get(colName).data.get(times.get(higherKey).dataIndex) - (Double)columns.get(colName).data.get(times.get(lowerKey).dataIndex)) *
									lowerKey.getDifferenceBetween(PeriodType.MILLI, time) / lowerKey.getDifferenceBetween(PeriodType.MILLI, higherKey) );
							break;
						}
					}
				}
			}
			if(resultList.size()==1){
				return resultList.get(0);
			}else{
				return LogoList.fromJava(resultList);
			}
		}
		public String dump(boolean arg0, boolean arg1, boolean arg2) {
			String result = "TIMESTAMP";
			for(String colName : columns.keySet()){
				result += "," + colName;
			}
			result += "\n";
			for(LogoTime logoTime : times.keySet()){
				TimeSeriesRecord time = times.get(logoTime);
				result += time.time.dump(false,false,false);
				for(String colName : columns.keySet()){
					result += "," + columns.get(colName).data.get(time.dataIndex);
				}
				result += "\n";
			}
			return result;
		}
		public String getExtensionName() {
			return null;
		}
		public String getNLTypeName() {
			return null;
		}
		public boolean recursivelyEqual(Object arg0) {
			return false;
		}
	}
	public class LogoEvent implements org.nlogo.api.ExtensionObject {
		private final long id;
		public Double tick = null;
		public org.nlogo.nvm.CommandTask task = null;
		public org.nlogo.agent.AgentSet agents = null;
		public Double repeatInterval = null;
		public Boolean shuffleAgentSet = null;

		LogoEvent(org.nlogo.agent.AgentSet agents, CommandTask task, Double tick, Double repeatInterval, Boolean shuffleAgentSet) {
			this.agents = agents;
			this.task = (org.nlogo.nvm.CommandTask) task;
			this.tick = tick;
			this.repeatInterval = repeatInterval;
			this.shuffleAgentSet = shuffleAgentSet;
			events.put(this, nextEvent);
			this.id = nextEvent;
			nextEvent++;
		}
		public void replaceData(Agent agent, CommandTask task, Double tick) {
			this.agents = agents;
			this.task = (org.nlogo.nvm.CommandTask) task;
			this.tick = tick;
		}
		/*
		 * If a repeatInterval is set, this method uses it to update it's tick field and then adds itself to the
		 * schedule argument.  The return value indicates whether the event was added to the schedule again.
		 */
		public Boolean reschedule(LogoSchedule schedule){
			if(repeatInterval == null)return false;
			this.tick = this.tick + repeatInterval;
			return schedule.schedule.add(this);
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
			return tick + ((agents==null)?"":agents.toString()) + ((task==null)?"":task.toString()) + ((repeatInterval==null)?"":repeatInterval.toString());
		}
	}
	private static class LogoSchedule implements org.nlogo.api.ExtensionObject {
		private final long id;
		LogoEventComparator comparator = (new TimeExtension()).new LogoEventComparator();
		TreeSet<LogoEvent> schedule = new TreeSet<LogoEvent>(comparator);
		// The following three fields track an anchored schedule
		LogoTime timeAnchor = null;
		PeriodType tickType = null;
		Double tickValue = null;

		LogoSchedule() {
			schedules.put(this, nextSchedule);
			this.id = nextSchedule;
			nextSchedule++;
		}
		public boolean equals(Object obj) {
			return this == obj;
		}
		public boolean isAnchored(){
			return timeAnchor != null;
		}
		public void anchorSchedule(LogoTime time, Double tickValue, PeriodType tickType){
			try {
				timeAnchor = new LogoTime(time);
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
		public String dump(boolean readable, boolean exporting, boolean reference) {
			StringBuilder buf = new StringBuilder();
			if (exporting) {
				buf.append(id);
				if (!reference) {
					buf.append(":");
				}
			}
			if (!(reference && exporting)) {
				buf.append(" [ ");
				java.util.Iterator iter = schedule.iterator();
				while(iter.hasNext()){
					buf.append(((LogoEvent)iter.next()).dump(true, true, true));
					buf.append(" ");
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
	}
	/*
	 * The LogoEventComparator first compares based on tick (which is a Double) and then on id 
	 * so if there is a tie for tick, the event that was created first get's executed first allowing
	 * for a more intuitive execution.
	 */
	public class LogoEventComparator implements Comparator<LogoEvent> {
		public int compare(LogoEvent a, LogoEvent b) {
			if(a.tick < b.tick){
				return -1;
			}else if(a.tick > b.tick){
				return 1;
			}else if(a.id < b.id){
				return -1;
			}else if(a.id > b.id){
				return 1;
			}else{
				return 0;
			}
		}
	}

	private static class LogoTime implements org.nlogo.api.ExtensionObject {
		public DateType			dateType = null;
		public LocalDateTime 	datetime = null;
		public LocalDate 		date	 = null;
		public MonthDay 		monthDay = null;
		private DateTimeFormatter fmt = null;
		private Boolean 		isAnchored = false;
		private Double 			tickValue;
		private PeriodType 		tickType;
		private LocalDateTime 	anchorDatetime;
		private LocalDate 		anchorDate;
		private MonthDay 		anchorMonthDay;
		private World 			world;

		LogoTime(LogoTime time) throws ExtensionException {
			this(time.dump(false,false,false));
		}
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
		int compareTo(LogoTime that){
			switch(this.dateType){
			case DATETIME:
				return this.datetime.compareTo(that.datetime);
			case DATE:
				return this.date.compareTo(that.date);
			case DAY:
				return this.monthDay.compareTo(that.monthDay);
			}
			return -999;
		}
		public Boolean isCloserToAThanB(LogoTime timeA, LogoTime timeB){
			DateTime refDateTime = new DateTime(ISOChronology.getInstanceUTC());
			Long millisToA = null, millisToB = null;

			switch(this.dateType){
			case DATETIME:
				millisToA = Math.abs((new Duration(timeA.datetime.toDateTime(refDateTime),this.datetime.toDateTime(refDateTime))).getMillis());
				millisToB = Math.abs((new Duration(timeB.datetime.toDateTime(refDateTime),this.datetime.toDateTime(refDateTime))).getMillis());
				break;
			case DATE:
				millisToA = Math.abs((new Duration(timeA.date.toDateTime(refDateTime),this.date.toDateTime(refDateTime))).getMillis());
				millisToB = Math.abs((new Duration(timeB.date.toDateTime(refDateTime),this.date.toDateTime(refDateTime))).getMillis());
				break;
			case DAY:
				millisToA = Math.abs((new Duration(timeA.monthDay.toLocalDate(2000).toDateTime(refDateTime),this.monthDay.toLocalDate(2000).toDateTime(refDateTime))).getMillis());
				millisToB = Math.abs((new Duration(timeB.monthDay.toLocalDate(2000).toDateTime(refDateTime),this.monthDay.toLocalDate(2000).toDateTime(refDateTime))).getMillis());
				break;
			}
			return millisToA < millisToB;
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
			this.tickValue = tickCount;
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
		public String dump(boolean arg1, boolean arg2, boolean arg3) {
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
				this.datetime = this.plus(this.anchorDatetime,this.tickType, this.world.ticks()*this.tickValue).datetime;
				break;
			case DATE:
				this.date = this.plus(this.anchorDate,this.tickType, this.world.ticks()*this.tickValue).date;
				break;
			case DAY:
				this.monthDay = this.plus(this.anchorMonthDay,this.tickType, this.world.ticks()*this.tickValue).monthDay;
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
					return intToDouble(Months.monthsBetween(this.datetime,endTime.datetime).getMonths());
				case DATE:
					return intToDouble(Months.monthsBetween(this.date,endTime.date).getMonths());
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

	public static class NewLogoTime extends DefaultReporter {
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
			LogoTime time = new LogoTime(getTimeFromArgument(args,0));
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
			return !(timeA.isBefore(timeB) || timeA.isEqual(timeB));
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
	private static LogoTimeSeries getTimeSeriesFromArgument(Argument args[], Integer argIndex) throws ExtensionException, LogoException {
		LogoTimeSeries ts = null;
		Object obj = args[argIndex].get();
		if (obj instanceof LogoTimeSeries) {
			ts = (LogoTimeSeries)obj;
		}else{
			throw new ExtensionException("time: was expecting a LogoTimeSeries object as argument "+(argIndex+1)+", found this instead: " + Dump.logoObject(obj));
		}
		return ts;
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
	// Convenience method, to extract a schedule object from an Argument.
	private static LogoSchedule getScheduleFromArguments(Argument args[], int index) throws ExtensionException, LogoException {
		Object obj = args[index].get();
		if (!(obj instanceof LogoSchedule)) {
			throw new ExtensionException("Was expecting a LogoSchedule as argument "+(index+1)+" found this instead: " + Dump.logoObject(obj));
		}
		return (LogoSchedule) obj;
	}
	public static class NewLogoSchedule extends DefaultReporter {
		public Syntax getSyntax() {
			return Syntax.reporterSyntax(new int[]{},
					Syntax.WildcardType());
		}
		public Object report(Argument args[], Context context)
				throws ExtensionException, LogoException {
			LogoSchedule sched = new LogoSchedule();
			return sched;
		}
	}
	public static class AnchorSchedule extends DefaultCommand {
		public Syntax getSyntax() {
			return Syntax.commandSyntax(new int[]{Syntax.WildcardType(),Syntax.WildcardType(),Syntax.NumberType(),Syntax.StringType()});
		}
		public void perform(Argument args[], Context context)
				throws ExtensionException, LogoException {
			LogoSchedule sched = getScheduleFromArguments(args,0);
			sched.anchorSchedule(getTimeFromArgument(args, 1),getDoubleFromArgument(args, 2),stringToPeriodType(getStringFromArgument(args, 3)));
		}
	}

	public static class FirstEvent extends DefaultReporter {
		public Syntax getSyntax() {
			return Syntax.reporterSyntax(new int[]{Syntax.WildcardType()},
					Syntax.WildcardType());
		}
		public Object report(Argument args[], Context context)
				throws ExtensionException, LogoException {
			LogoSchedule sched = getScheduleFromArguments(args,0);
			return sched.schedule.first();
		}
	}

	public static class NextEvent extends DefaultReporter {
		public Syntax getSyntax() {
			return Syntax.reporterSyntax(new int[]{Syntax.WildcardType()},
					Syntax.WildcardType());
		}
		public Object report(Argument args[], Context context)
				throws ExtensionException, LogoException {
			LogoSchedule sched = getScheduleFromArguments(args,0);
			Object toReturn = sched.schedule.first();
			sched.schedule.remove(toReturn);
			return toReturn;
		}
	}

	public static class GetSize extends DefaultReporter {
		public Syntax getSyntax() {
			return Syntax.reporterSyntax(new int[]{Syntax.WildcardType()},
					Syntax.NumberType());
		}
		public Object report(Argument args[], Context context)
				throws ExtensionException, LogoException {
			LogoSchedule sched = getScheduleFromArguments(args,0);
			if(debug)printToConsole(context, "size of schedule: "+sched.schedule.size());
			return new Double(sched.schedule.size());
		}
	}

	public static class AddEvent extends DefaultCommand {
		public Syntax getSyntax() {
			return Syntax.commandSyntax(new int[]{Syntax.WildcardType(),
					Syntax.WildcardType(),
					Syntax.WildcardType(),
					Syntax.WildcardType()});
		}
		public void perform(Argument args[], Context context) throws ExtensionException, LogoException {
			addEvent(args,context,AddType.DEFAULT);
		}
	}

	public static class AddEventShuffled extends DefaultCommand {
		public Syntax getSyntax() {
			return Syntax.commandSyntax(new int[]{Syntax.WildcardType(),
					Syntax.WildcardType(),
					Syntax.WildcardType(),
					Syntax.WildcardType()});
		}
		public void perform(Argument args[], Context context) throws ExtensionException, LogoException {
			addEvent(args,context,AddType.SHUFFLE);
		}
	}

	public static class RepeatEvent extends DefaultCommand {
		public Syntax getSyntax() {
			return Syntax.commandSyntax(new int[]{Syntax.WildcardType(),
					Syntax.WildcardType(),
					Syntax.WildcardType(),
					Syntax.WildcardType(),
					Syntax.NumberType()});
		}
		public void perform(Argument args[], Context context) throws ExtensionException, LogoException {
			addEvent(args,context,AddType.REPEAT);
		}
	}

	private static void addEvent(Argument args[], Context context, AddType addType) throws ExtensionException, LogoException {
		String primName = null;
		Double eventTick = null;
		switch(addType){
		case DEFAULT:
			primName = "add";
			if(args.length<4)throw new ExtensionException("time:add must have 4 arguments: schedule agent task tick/time");
			break;
		case SHUFFLE:
			primName = "add-shuffled";
			if(args.length<4)throw new ExtensionException("time:add-shuffled must have 4 arguments: schedule agent task tick/time");
			break;
		case REPEAT:
			primName = "repeat";
			if(args.length<5)throw new ExtensionException("time:repeat must have 5 arguments: schedule agent task tick/time number");
			break;
		}

		if (!(args[0].get() instanceof LogoSchedule)) throw new ExtensionException("time:"+primName+" expecting a schedule as the first argument");
		LogoSchedule sched = getScheduleFromArguments(args,0);
		if (!(args[1].get() instanceof Agent) && !(args[1].get() instanceof AgentSet)) throw new ExtensionException("time:"+primName+" expecting an agent or agentset as the second argument");
		if (!(args[2].get() instanceof CommandTask)) throw new ExtensionException("time:"+primName+" expecting a command task as the third argument");
		if(args[3].get().getClass().equals(Double.class)){
			eventTick = args[3].getDoubleValue();
		}else if(args[3].get().getClass().equals(LogoTime.class)){
			if(!sched.isAnchored())throw new ExtensionException("A LogoEvent can only be scheduled to occur at a LogoTime if the LogoScedule has been anchored to a LogoTime, see time:anchor-schedule");
			eventTick = sched.timeToTick(getTimeFromArgument(args, 3));
		}else{
			throw new ExtensionException("time:"+primName+" expecting a number or logotime as the fourth argument");
		}
		if (eventTick < ((ExtensionContext)context).workspace().world().ticks()) throw new ExtensionException("Attempted to schedule an event for tick "+ eventTick +" which is before the present 'moment' of "+((ExtensionContext)context).workspace().world().ticks());
		Double repeatInterval = null;
		if(addType == AddType.REPEAT){
			if (!args[4].get().getClass().equals(Double.class)) throw new ExtensionException("time:repeat expecting a number as the fifth argument");
			if (args[4].getDoubleValue() <= 0) throw new ExtensionException("time:repeat the repeat interval must be a positive number");
			repeatInterval = args[4].getDoubleValue();
		}
		Boolean shuffleAgentSet = (addType == AddType.SHUFFLE);

		org.nlogo.agent.AgentSet agentSet = null;
		if (args[1].get() instanceof org.nlogo.agent.Agent){
			org.nlogo.agent.Agent theAgent = (org.nlogo.agent.Agent)args[1].getAgent();
			agentSet = new ArrayAgentSet(theAgent.getAgentClass(),1,false,(World) theAgent.world());
			agentSet.add(theAgent);
		}else{
			agentSet = (org.nlogo.agent.AgentSet) args[1].getAgentSet();
		}
		if(debug)printToConsole(context,"scheduling agents: "+agentSet+" task: "+args[2].getCommandTask().toString()+" tick: "+eventTick+" shuffled: "+shuffleAgentSet );
		LogoEvent event = (new TimeExtension()).new LogoEvent(agentSet,args[2].getCommandTask(),eventTick,repeatInterval,shuffleAgentSet);
		sched.schedule.add(event);
	}

	public static class Go extends DefaultCommand {
		public Syntax getSyntax() {
			return Syntax.commandSyntax(new int[]{Syntax.WildcardType()});
		}
		public void perform(Argument args[], Context context) throws ExtensionException, LogoException {
			performScheduledTasks(args, context);
		}
	}

	public static class GoUntil extends DefaultCommand {
		public Syntax getSyntax() {
			return Syntax.commandSyntax(new int[]{Syntax.WildcardType(),
					Syntax.NumberType()});
		}
		public void perform(Argument args[], Context context) throws ExtensionException, LogoException {
			performScheduledTasks(args, context, true);
		}
	}
	private static void performScheduledTasks(Argument args[], Context context) throws ExtensionException, LogoException {
		performScheduledTasks(args,context,false);
	}	
	private static void performScheduledTasks(Argument args[], Context context, Boolean isGoUntil) throws ExtensionException, LogoException {
		ExtensionContext extcontext = (ExtensionContext) context;
		LogoSchedule sched = getScheduleFromArguments(args,0);
		Double untilTick = null;
		if(isGoUntil){
			if (!args[1].get().getClass().equals(Double.class)) throw new ExtensionException("time:go-until expecting a number as the second argument");
			untilTick = args[1].getDoubleValue();
		}else{
			untilTick = Double.MAX_VALUE;
		}
		TickCounter tickCounter = extcontext.workspace().world().tickCounter;
		Object[] emptyArgs = new Object[0]; // This extension is only for CommandTasks, so we know there aren't any args to pass in
		LogoEvent event = sched.schedule.isEmpty() ? null : sched.schedule.first();
		ArrayList<org.nlogo.agent.Agent> theAgents = new ArrayList<org.nlogo.agent.Agent>();
		while(event != null && event.tick <= untilTick){
			if(debug)printToConsole(context,"performing event-id: "+event.id+" for agent: "+event.agents+" at tick:"+event.tick);
			tickCounter.tick(event.tick-tickCounter.ticks());

			if(event.shuffleAgentSet){
				Iterator iter = event.agents.shufflerator(extcontext.nvmContext().job.random);
				while(iter.hasNext()){
					org.nlogo.nvm.Context nvmContext = new org.nlogo.nvm.Context(extcontext.nvmContext().job,iter.next(),extcontext.nvmContext().ip,extcontext.nvmContext().activation);
					if(extcontext.nvmContext().stopping)return;
					event.task.perform(nvmContext, emptyArgs);
					if(nvmContext.stopping)return;
				}
			}else{
				org.nlogo.agent.Agent[] source = null;
				org.nlogo.agent.Agent[] copy = null;
				if(event.agents instanceof ArrayAgentSet){
					source = event.agents.toArray();
					copy = new org.nlogo.agent.Agent[event.agents.count()];
					System.arraycopy(source, 0, copy, 0, source.length);
				}else if(event.agents instanceof TreeAgentSet){
					copy = event.agents.toArray();
				}
				for(org.nlogo.agent.Agent theAgent : copy){
					if(theAgent == null || theAgent.id == -1)continue;
					org.nlogo.nvm.Context nvmContext = new org.nlogo.nvm.Context(extcontext.nvmContext().job,theAgent,extcontext.nvmContext().ip,extcontext.nvmContext().activation);
					if(extcontext.nvmContext().stopping)return;
					event.task.perform(nvmContext, emptyArgs);
					if(nvmContext.stopping)return;
				}
			}

			// Remove the current event as is from the schedule
			sched.schedule.remove(event);

			// Reschedule the event if necessary
			event.reschedule(sched);

			// Grab the next event from the schedule
			event = sched.schedule.isEmpty() ? null : sched.schedule.first();
		}
		if(isGoUntil && untilTick > tickCounter.ticks()) tickCounter.tick(untilTick-tickCounter.ticks());
	}
	public static class TimeSeriesLoad extends DefaultReporter{
		public Syntax getSyntax() {
			return Syntax.reporterSyntax(new int[]{Syntax.StringType()},Syntax.WildcardType());
		}
		public Object report(Argument args[], Context context) throws ExtensionException, LogoException {
			String filename = getStringFromArgument(args, 0);
			LogoTimeSeries ts = new LogoTimeSeries(filename);
			return ts;
		}
	}
	public static class TimeSeriesGet extends DefaultReporter{
		public Syntax getSyntax() {
			return Syntax.reporterSyntax(new int[]{Syntax.WildcardType(),Syntax.WildcardType(),Syntax.StringType()},Syntax.WildcardType());
		}
		public Object report(Argument args[], Context context) throws ExtensionException, LogoException {
			LogoTimeSeries ts = getTimeSeriesFromArgument(args, 0);
			LogoTime time = getTimeFromArgument(args, 1);
			String columnName = getStringFromArgument(args, 2);
			return ts.getByTime(time, columnName, GetTSMethod.NEAREST);
		}
	}
	public static class TimeSeriesGetExact extends DefaultReporter{
		public Syntax getSyntax() {
			return Syntax.reporterSyntax(new int[]{Syntax.WildcardType(),Syntax.WildcardType(),Syntax.StringType()},Syntax.WildcardType());
		}
		public Object report(Argument args[], Context context) throws ExtensionException, LogoException {
			LogoTimeSeries ts = getTimeSeriesFromArgument(args, 0);
			LogoTime time = getTimeFromArgument(args, 1);
			String columnName = getStringFromArgument(args, 2);
			return ts.getByTime(time, columnName, GetTSMethod.EXACT);
		}
	}
	public static class TimeSeriesGetInterp extends DefaultReporter{
		public Syntax getSyntax() {
			return Syntax.reporterSyntax(new int[]{Syntax.WildcardType(),Syntax.WildcardType(),Syntax.StringType()},Syntax.WildcardType());
		}
		public Object report(Argument args[], Context context) throws ExtensionException, LogoException {
			LogoTimeSeries ts = getTimeSeriesFromArgument(args, 0);
			LogoTime time = getTimeFromArgument(args, 1);
			String columnName = getStringFromArgument(args, 2);
			return ts.getByTime(time, columnName, GetTSMethod.LINEAR_INTERP);
		}
	}
	public static class TimeSeriesGetRow extends DefaultReporter{
		public Syntax getSyntax() {
			return Syntax.reporterSyntax(new int[]{Syntax.WildcardType(),Syntax.WildcardType()},Syntax.WildcardType());
		}
		public Object report(Argument args[], Context context) throws ExtensionException, LogoException {
			LogoTimeSeries ts = getTimeSeriesFromArgument(args, 0);
			LogoTime time = getTimeFromArgument(args, 1);
			return ts.getByTime(time, "ALL_-_COLUMNS", GetTSMethod.NEAREST);
		}
	}
	public static class TimeSeriesGetRowInterp extends DefaultReporter{
		public Syntax getSyntax() {
			return Syntax.reporterSyntax(new int[]{Syntax.WildcardType(),Syntax.WildcardType()},Syntax.WildcardType());
		}
		public Object report(Argument args[], Context context) throws ExtensionException, LogoException {
			LogoTimeSeries ts = getTimeSeriesFromArgument(args, 0);
			LogoTime time = getTimeFromArgument(args, 1);
			return ts.getByTime(time, "ALL_-_COLUMNS", GetTSMethod.LINEAR_INTERP);
		}
	}
	public static class TimeSeriesGetRowExact extends DefaultReporter{
		public Syntax getSyntax() {
			return Syntax.reporterSyntax(new int[]{Syntax.WildcardType(),Syntax.WildcardType()},Syntax.WildcardType());
		}
		public Object report(Argument args[], Context context) throws ExtensionException, LogoException {
			LogoTimeSeries ts = getTimeSeriesFromArgument(args, 0);
			LogoTime time = getTimeFromArgument(args, 1);
			return ts.getByTime(time, "ALL_-_COLUMNS", GetTSMethod.EXACT);
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
}