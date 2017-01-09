package time;

public class TimeEnums {
	public enum AddType {
		DEFAULT, SHUFFLE, REPEAT, REPEAT_SHUFFLED
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
}
