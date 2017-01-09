package time.datatypes;
import java.util.ArrayList;

import time.TimeEnums.DataType;

@SuppressWarnings("unchecked") class TimeSeriesColumn {
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