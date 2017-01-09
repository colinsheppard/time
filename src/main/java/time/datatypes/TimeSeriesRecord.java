package time.datatypes;

class TimeSeriesRecord {
	public LogoTime time;
	public int dataIndex;

	TimeSeriesRecord(LogoTime time,int i){
		this.time = time;
		this.dataIndex = i;
	}
}