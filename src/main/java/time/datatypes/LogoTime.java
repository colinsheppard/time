package time.datatypes;

import org.joda.time.DateTime;
import org.joda.time.DateTimeFieldType;
import org.joda.time.Duration;
import org.joda.time.LocalDate;
import org.joda.time.LocalDateTime;
import org.joda.time.MonthDay;
import org.joda.time.Months;
import org.joda.time.Period;
import org.joda.time.chrono.ISOChronology;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.nlogo.agent.World;
import org.nlogo.api.ExtensionException;
import org.nlogo.core.ExtensionObject;

import time.TimeEnums.DateType;
import time.TimeEnums.PeriodType;
import time.TimeUtils;

public class LogoTime implements ExtensionObject{
	public DateType			dateType = null;
	public LocalDateTime 	datetime = null;
	public LocalDate 		date	 = null;
	public MonthDay 		monthDay = null;
	private DateTimeFormatter customFmt = null;
	private DateTimeFormatter defaultFmt = null;
	private Boolean 		isAnchored = false;
	private Double 			tickValue;
	private PeriodType 		tickType;
	private LocalDateTime 	anchorDatetime;
	private LocalDate 		anchorDate;
	private MonthDay 		anchorMonthDay;
	private World 			world;

	public LogoTime(LogoTime time) throws ExtensionException {
		this(time.show(time.defaultFmt));
	}
	public LogoTime(LocalDateTime dt) {
		this.datetime = dt;
		this.defaultFmt = DateTimeFormat.forPattern("yyyy-MM-dd HH:mm:ss.SSS");
		this.dateType = DateType.DATETIME;
	}
	public LogoTime(LocalDate dt) {
		this.date = dt;
		this.defaultFmt = DateTimeFormat.forPattern("yyyy-MM-dd");
		this.dateType = DateType.DATE;
	}
	public LogoTime(MonthDay dt) {
		this.monthDay = dt;
		this.defaultFmt = DateTimeFormat.forPattern("MM-dd");
		this.dateType = DateType.DAY;
	}
	public LogoTime(String dateString) throws ExtensionException {
		this(dateString,null);
	}
	public LogoTime(String dateString, String customFormat) throws ExtensionException {
		// First we parse the string to determine the date type
		if(customFormat == null){
			dateString = parseDateString(dateString);
		}else{
			if(customFormat.indexOf('H') >= 0 ||
					customFormat.indexOf('h') >= 0 || 
					customFormat.indexOf('K') >= 0 || 
					customFormat.indexOf('k') >= 0){
				this.dateType = DateType.DATETIME;
			}else if(customFormat.indexOf('Y') >= 0 || customFormat.indexOf('y') >= 0){
				this.dateType = DateType.DATE;
			}else{
				this.dateType = DateType.DAY;
			}
		}
		// Now initialize the defaultFmt
		switch(this.dateType){
		case DATETIME:
			this.defaultFmt = DateTimeFormat.forPattern("yyyy-MM-dd HH:mm:ss.SSS");
			break;
		case DATE:
			this.defaultFmt = DateTimeFormat.forPattern("yyyy-MM-dd");
			break;
		case DAY:
			this.defaultFmt = DateTimeFormat.forPattern("MM-dd");
			break;
		}
		// Now create the joda time object
		if(customFormat == null){
			switch(this.dateType){
			case DATETIME:
				this.datetime = (dateString.length() == 0 || dateString.equals("now")) ? new LocalDateTime() : new LocalDateTime(dateString);
				break;
			case DATE:
				this.date = new LocalDate(dateString);
				break;
			case DAY:
				this.monthDay = (new MonthDay()).parse(dateString, this.defaultFmt);
				break;
			}
		}else{
			this.customFmt = DateTimeFormat.forPattern(customFormat);
			switch(this.dateType){
			case DATETIME:
				this.datetime = LocalDateTime.parse(dateString, this.customFmt);
				break;
			case DATE:
				this.date = LocalDate.parse(dateString, this.customFmt);
				break;
			case DAY:
				this.monthDay = MonthDay.parse(dateString, this.customFmt);
				break;
			}
			//if(debug)printToConsole(getContext(), customFormat);
			//if(debug)printToConsole(getContext(), dateString);
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
	 * allow one specific way each to specify a DATE and a DAY. Single digit months, days, and hours are ok, but single
	 * digit minutes and seconds need a preceding zero (e.g. '06', not '6')
	 * 
	 * LEGIT
	 * 2012-11-10T09:08:07.654
	 * 2012-11-10T9:08:07.654
	 * 2012/11/10T09:08:07.654
	 * 2012-11-10 09:08:07.654
	 * 2012-11-10 9:08:07.654
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
	 * 2012-11-10 09:8:07.654
	 * 2012-11-10 09:08:7.654
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
			if(dateString.indexOf('T') == 10 & (dateString.indexOf(':') == 12 || len == 12)){ 
				// DATETIME without leading 0 on hour, pad it
				int firstColon = dateString.indexOf(':');
				dateString = dateString.substring(0, 11) + "0" + dateString.substring(11, len);
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
		return this.toString();
	}
	public String toString(){
		try {
			this.updateFromTick();
		} catch (ExtensionException e) {
			// ignore
		}
		switch(this.dateType){
		case DATETIME:
			return datetime.toString(this.customFmt == null ? this.defaultFmt : this.customFmt);
		case DATE:
			return date.toString(this.customFmt == null ? this.defaultFmt : this.customFmt);
		case DAY:
			return monthDay.toString(this.customFmt == null ? this.defaultFmt : this.customFmt);
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
		switch(this.dateType){
		case DATETIME:
			return this.datetime.toString(fmt);
		case DATE:
			return this.date.toString(fmt);
		case DAY:
			return this.monthDay.toString(fmt);
		}
		return "";
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
			per = new Period(0,TimeUtils.roundDouble(durVal),0,0,0,0,0,0);
			break;
		case YEAR:
			per = new Period(TimeUtils.roundDouble(durVal),0,0,0,0,0,0,0);
			break;
		default:
			throw new ExtensionException(pType+" type is not supported by the time:plus primitive");
		}
		switch(this.dateType){
		case DATETIME:
			if(per==null){
				return new LogoTime(((LocalDateTime)refTime).plus(new Duration(TimeUtils.dToL(durVal))));
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
		if(!timeA.isBefore(timeB)){
			LogoTime tempA = timeA;
			timeA = timeB;
			timeB = tempA;
		}
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
				return TimeUtils.intToDouble((new Period(this.datetime,endTime.datetime)).getYears());
			case DATE:
				return TimeUtils.intToDouble((new Period(this.date,endTime.date)).getYears());
			case DAY:
				throw new ExtensionException(pType+" type is not supported by the time:difference-between primitive with LogoTimes of type DAY");
			}
		case MONTH:
			switch(this.dateType){
			case DATETIME:
				return TimeUtils.intToDouble(Months.monthsBetween(this.datetime,endTime.datetime).getMonths());
			case DATE:
				return TimeUtils.intToDouble(Months.monthsBetween(this.date,endTime.date).getMonths());
			case DAY:
				return TimeUtils.intToDouble((new Period(this.monthDay,endTime.monthDay)).getMonths());
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