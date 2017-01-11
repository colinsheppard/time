package time.datatypes;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.TreeMap;

import time.TimeEnums;
import time.TimeEnums.GetTSMethod;
import time.TimeEnums.PeriodType;

import org.nlogo.api.ExtensionException;
import org.nlogo.core.ExtensionObject;
import org.nlogo.core.LogoList;
import org.nlogo.nvm.ExtensionContext;

public class LogoTimeSeries implements ExtensionObject{
	TreeMap<LogoTime,TimeSeriesRecord> times = new TreeMap<LogoTime,TimeSeriesRecord>(new LogoTimeComparator());
	LinkedHashMap<String,TimeSeriesColumn> columns = new LinkedHashMap<String,TimeSeriesColumn>();
	Integer numRows = 0;

	public LogoTimeSeries(LogoList colNames) throws ExtensionException{
		for(Object colName : colNames.toJava()){
			columns.put(colName.toString(), new TimeSeriesColumn());
		}
	}
	public LogoTimeSeries(String filename, String customFormat, ExtensionContext context) throws ExtensionException{
		parseTimeSeriesFile(filename, customFormat, context);
	}
	public LogoTimeSeries(String filename, ExtensionContext context) throws ExtensionException{
		parseTimeSeriesFile(filename, context);
	}
	public void add(LogoTime time, LogoList list) throws ExtensionException{
		int index = times.size();
		TimeSeriesRecord record = new TimeSeriesRecord(time, index);
		int i = 0;
		for(String colName : columns.keySet()){
			columns.get(colName).add(list.get(i++).toString());
		}
		try{
			times.put(time, record);
		}catch(NullPointerException e){
			if(time.dateType != ((LogoTime)times.keySet().toArray()[0]).dateType){
				throw new ExtensionException("Cannot add a row with a LogoTime of type "+time.dateType.toString()+
						" to a LogoTimeSeries of type "+((LogoTime)times.keySet().toArray()[0]).dateType.toString()+
						".  Note, the first row added to the LogoTimeSeries object determines the data types for all columns.");
			}else{
				throw e;
			}
		}
	}
	public Integer getNumColumns(){
		return columns.size();
	}
	public void write(String filename, ExtensionContext context) throws ExtensionException{
		File dataFile;
		if(filename.charAt(0)=='/' || filename.charAt(0)=='\\'){
			dataFile = new File(filename);
		}else{
			dataFile = new File(context.workspace().getModelDir()+"/"+filename);
		}
		FileWriter fw;
		try {
			fw = new FileWriter(dataFile.getAbsoluteFile());
			BufferedWriter bw = new BufferedWriter(fw);
			bw.write("TIMESTAMP");
			for(String colName : columns.keySet()){
				bw.write("," + colName);
			}
			bw.write("\n");
			for(LogoTime logoTime : times.keySet()){
				TimeSeriesRecord time = times.get(logoTime);
				bw.write(time.time.dump(false,false,false));
				for(String colName : columns.keySet()){
					bw.write("," + columns.get(colName).data.get(time.dataIndex));
				}
				bw.write("\n");
			}
			bw.flush();
			bw.close();
		} catch (IOException e) {
			throw new ExtensionException(e.getMessage());
		}
	}
	public void parseTimeSeriesFile(String filename, ExtensionContext context) throws ExtensionException{
			parseTimeSeriesFile(filename,null,context);
	}
	public void parseTimeSeriesFile(String filename, String customFormat, ExtensionContext context) throws ExtensionException{
		File dataFile;
		if(filename.charAt(0)=='/' || filename.charAt(0)=='\\' || filename.charAt(1)==':' || context.workspace().getModelDir()==null){
			dataFile = new File(filename);
		}else{
			try {
				dataFile = new File(context.attachCurrentDirectory(filename));
			} catch (MalformedURLException e) {
				throw new ExtensionException("Malformed filename URL: "+filename);
			}
		}
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

		// Read the header line after skipping commented lines and infer the delimiter (tab or comma)
		strLine = ";";
		while(strLine.trim().charAt(0)==';'){
			try {
				strLine = br.readLine();
			} catch (IOException e) {
				throw new ExtensionException(e.getMessage());
			}
			if(strLine==null)throw new ExtensionException("File "+dataFile+" is blank.");
		}
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
				LogoTime newTime = new LogoTime(lineData[0],customFormat);
				times.put(newTime,new TimeSeriesRecord(newTime, numRows++));
				for(int colInd = 1; colInd <= columns.size(); colInd++){
					columns.get(columnNames[colInd]).add(lineData[colInd]);
				}
			}
			br.close();
			in.close();
			fstream.close();
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
		LogoTime finalKey = null, higherKey = null, lowerKey = null;
		if(times.get(time)!=null){
			finalKey = time;
		}else{
			higherKey = times.higherKey(time);
			lowerKey = times.lowerKey(time);
			if(higherKey == null){
				finalKey = lowerKey;
			}else if(lowerKey == null){
				finalKey = higherKey;
			}else{
				switch(getMethod){
				case EXACT:
					throw new ExtensionException("The LogoTime "+time.dump(false, false, false)+" does not exist in the time series.");
				case NEAREST:
					finalKey = time.isCloserToAThanB(lowerKey, higherKey) ? lowerKey : higherKey;
					break;
				case LINEAR_INTERP:
					finalKey = time;
					break;
				}
			}
		}
		if(columnName.equals("ALL_-_COLUMNS"))resultList.add(finalKey);
		for(String colName : columnList){
			if(getMethod==GetTSMethod.LINEAR_INTERP){
				if(columns.get(colName).data.get(0) instanceof String)throw new ExtensionException("Cannot interpolate between string values, use time:get instead.");
				resultList.add( (Double)columns.get(colName).data.get(times.get(lowerKey).dataIndex) + 
					((Double)columns.get(colName).data.get(times.get(higherKey).dataIndex) - (Double)columns.get(colName).data.get(times.get(lowerKey).dataIndex)) *
					lowerKey.getDifferenceBetween(PeriodType.MILLI, time) / lowerKey.getDifferenceBetween(PeriodType.MILLI, higherKey) );
			}else{
				resultList.add(columns.get(colName).data.get(times.get(finalKey).dataIndex));
			}
		}
		if(resultList.size()==1){
			return resultList.get(0);
		}else{
			return LogoList.fromJava(resultList);
		}
	}
	public Object getRangeByTime(LogoTime timeLow, LogoTime timeHigh, String columnName) throws ExtensionException{
		if(!timeLow.isBefore(timeHigh)){
			LogoTime timeTemp = timeLow;
			timeLow = timeHigh;
			timeHigh = timeTemp;
		}
		ArrayList<String> columnList = new ArrayList<String>(columns.size());
		ArrayList<LogoList> resultList = new ArrayList<LogoList>(columns.size());
		if(columnName.equals("ALL_-_COLUMNS")){
			columnList.addAll(columns.keySet());
		}else if(columnName.equals("LOGOTIME")){
			// do nothing, keep columnList empty
		}else if(!columns.containsKey(columnName)){
			throw new ExtensionException("The LogoTimeSeries does not contain the column "+columnName);
		}else{
			columnList.add(columnName);
		}
		LogoTime lowerKey = timeLow;
		if(times.get(lowerKey) == null) lowerKey = times.higherKey(timeLow);
		LogoTime higherKey = timeHigh;
		if(times.get(higherKey) == null) higherKey = times.lowerKey(timeHigh);
		if(lowerKey == null || higherKey == null){
			if(columnName.equals("ALL_-_COLUMNS") || columnName.equals("LOGOTIME")){
				resultList.add(LogoList.fromVector(new scala.collection.immutable.Vector<Object>(0, 0, 0)));
			}
			for(String colName : columnList){
				resultList.add(LogoList.fromVector(new scala.collection.immutable.Vector<Object>(0, 0, 0)));
			}
		}else{
			if(columnName.equals("ALL_-_COLUMNS") || columnName.equals("LOGOTIME")){
				resultList.add(LogoList.fromJava(times.subMap(lowerKey, true, higherKey, true).keySet()));
			}
			for(String colName : columnList){
				resultList.add(LogoList.fromJava(columns.get(colName).data.subList(times.get(lowerKey).dataIndex, times.get(higherKey).dataIndex+1)));
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
	public void ensureDateTypeConsistent(LogoTime time) throws ExtensionException{
		if(times.size()>0){
			if(times.firstKey().dateType != time.dateType){
				throw(new ExtensionException("The LogoTimeSeries contains LogoTimes of type "+times.firstKey().dateType.toString()+
						" while the LogoTime "+time.toString()+" used in the search is of type "+time.dateType.toString()));
			}
		}
	}
	public String getExtensionName() {
		return "time";
	}
	public String getNLTypeName() {
		return "LogoTimeSeries";
	}
	public boolean recursivelyEqual(Object arg0) {
		return false;
	}
}