package org.nlogo.extensions.time.datatypes

import java.io.{BufferedReader, BufferedWriter, DataInputStream, File, FileInputStream, FileNotFoundException, FileWriter, IOException, InputStreamReader}
import java.net.MalformedURLException
import java.util.{ArrayList, Arrays, LinkedHashMap, List, TreeMap}
import scala.collection.immutable.Vector._

import org.nlogo.api.ExtensionException
import org.nlogo.core.{ExtensionObject, LogoList}
import org.nlogo.nvm.ExtensionContext
import org.nlogo.extensions.time._
import scala.collection.JavaConverters._

class LogoTimeSeries(colNames: LogoList) extends ExtensionObject {

  var times: TreeMap[LogoTime, TimeSeriesRecord] =
    new TreeMap[LogoTime, TimeSeriesRecord](new LogoTimeComparator())
  var columns: LinkedHashMap[String, TimeSeriesColumn] =
    new LinkedHashMap[String, TimeSeriesColumn]()
  var numRows: java.lang.Integer = 0

  for (colName <- colNames.asJava.asScala) {
    columns.put(colName.toString, new TimeSeriesColumn())
  }

  def this(filename: String, customFormat: String, context: ExtensionContext) = {
    this(LogoList(Vector[AnyRef]()): LogoList)
    parseTimeSeriesFile(filename, customFormat, context)
  }

  def this(filename: String, context: ExtensionContext) = {
    this(LogoList(Vector[AnyRef]()): LogoList)
    parseTimeSeriesFile(filename, context)
  }

  def add(time: LogoTime, list: LogoList): Unit = {
    val index: Int = times.size
    val record = new TimeSeriesRecord(time, index)
    var i: Int = 0
    for (colName <- columns.keySet.asScala) {
      columns.get(colName).add(list.get({ i += 1; i - 1 }).toString)
    }
    try times.put(time, record)
    catch {
      case e: NullPointerException =>
        if (time.dateType != times.keySet().toArray()(0).asInstanceOf[LogoTime].dateType) {
          throw new ExtensionException(
            "Cannot add a row with a LogoTime of type " + time.dateType.toString +
              " to a LogoTimeSeries of type " +
              times
                .keySet()
                .toArray()(0)
                .asInstanceOf[LogoTime]
                .dateType
                .toString +
              ".  Note, the first row added to the LogoTimeSeries object determines the data types for all columns.")
        } else {
          throw e
        }
    }
  }

  def getNumColumns(): java.lang.Integer = columns.size

  def write(filename: String, context: ExtensionContext): Unit = {
    var dataFile: File = null
    dataFile =
      if (filename.charAt(0) == '/' || filename.charAt(0) == '\\')
        new File(filename)
      else new File(context.workspace.getModelDir + "/" + filename)
    var fw: FileWriter = null
    fw = new FileWriter(dataFile.getAbsoluteFile)
    val bw: BufferedWriter = new BufferedWriter(fw)
    bw.write("TIMESTAMP")
    for (colName <- columns.keySet.asScala) {
      bw.write("," + colName)
    }
    bw.write("\n")
    for (logoTime <- times.keySet.asScala) {
      val time: TimeSeriesRecord = times.get(logoTime)
      bw.write(time.time.dump(false, false, false))
      for (colName <- columns.keySet.asScala) {
        bw.write("," + columns.get(colName).data.get(time.dataIndex))
      }
      bw.write("\n")
    }
    bw.flush()
    bw.close()
  }

  def parseTimeSeriesFile(filename: String, context: ExtensionContext): Unit = {
    parseTimeSeriesFile(filename, null, context)
  }

  def parseTimeSeriesFile(filename: String,customFormat: String,context: ExtensionContext): Unit = {
    var dataFile: File = null
    dataFile =
      if (filename.charAt(0) == '/' || filename.charAt(0) == '\\' ||
          filename.charAt(1) == ':' ||
          context.workspace.getModelDir == null){ new File(filename) }
      else new File(context.attachCurrentDirectory(filename))
    var fstream: FileInputStream = null
    fstream = new FileInputStream(dataFile)
    val in: DataInputStream = new DataInputStream(fstream)
    val br: BufferedReader = new BufferedReader(new InputStreamReader(in))
    val lineCount: Int = 0
    var delim: String = null
    var strLine: String = null
    var lineData: Array[String] = null
    strLine = ";"
    while (strLine.trim().charAt(0) == ';') {
      strLine = br.readLine()
      if (strLine == null)
        throw new ExtensionException("File " + dataFile + " is blank.")
    }
    val hasTab: java.lang.Boolean = strLine.contains("\t")
    val hasCom: java.lang.Boolean = strLine.contains(",")
    if (hasTab && hasCom) {
      throw new ExtensionException(
        "Ambiguous file format in file " + dataFile +
          ", the header line contains both a tab and a comma character, expecting one or the other.")
    } else if (hasTab) {
      delim = "\t"
    } else if (hasCom) {
      delim = ","
    } else {
      throw new ExtensionException(
        "Illegal file format in file " + dataFile +
          ", the header line does not contain a tab or a comma character, expecting one or the other.")
    }
    val columnNames: Array[String] = strLine.split(delim)
    for (columnName <- Arrays.copyOfRange(columnNames, 1, columnNames.length)) {
      columns.put(columnName, new TimeSeriesColumn())
    }
    strLine = br.readLine()
    while (strLine != null) {
      lineData = strLine.split(delim)
      val newTime: LogoTime = new LogoTime(lineData(0), customFormat)
      times.put(newTime, new TimeSeriesRecord(newTime, { numRows += 1; numRows - 1 }))
      var colInd: Int = 1
      while (colInd <= columns.size) {
        columns.get(columnNames(colInd - 1)).add(lineData(colInd))
        colInd += 1
        colInd - 1
      }
    }
    br.close()
    in.close()
    fstream.close()
  }

  def getByTime(time: LogoTime, columnName: String, getMethod: GetTSMethod): AnyRef = {
    val columnList: ArrayList[String] = new ArrayList[String](columns.size)
    val resultList: ArrayList[AnyRef] = new ArrayList[AnyRef](columns.size)
    if (columnName.==("ALL_-_COLUMNS")) {
      columnList.addAll(columns.keySet)
    } else if (!columns.containsKey(columnName)) {
      throw new ExtensionException(
        "The LogoTimeSeries does not contain the column " + columnName)
    } else {
      columnList.add(columnName)
    }
    var finalKey: LogoTime = null
    var higherKey: LogoTime = null
    var lowerKey: LogoTime = null
    if (times.get(time) != null) {
      finalKey = time
    } else {
      higherKey = times.higherKey(time)
      lowerKey = times.lowerKey(time)
      if (higherKey == null) {
        finalKey = lowerKey
      } else if (lowerKey == null) {
        finalKey = higherKey
      } else {
        getMethod match {
          case Exact =>
            throw new ExtensionException("The LogoTime " + time.dump(
              false,
              false,
              false) + " does not exist in the time series.")
          case Nearest =>
            finalKey =
              if (time.isCloserToAThanB(lowerKey, higherKey)) lowerKey
              else higherKey
          case LinearInterp => finalKey = time

        }
      }
    }
    if (columnName.==("ALL_-_COLUMNS")) resultList.add(finalKey)
    for (colName <- columnList.asScala) {
      if (getMethod == LinearInterp) {
        if (columns.get(colName).data.get(0).isInstanceOf[String])
          throw new ExtensionException(
            "Cannot interpolate between string values, use time:get instead.")
        resultList.add((
          columns
            .get(colName)
            .data
            .get(times.get(lowerKey).dataIndex)
            .asInstanceOf[java.lang.Double] +
            (columns
              .get(colName)
              .data
              .get(times.get(higherKey).dataIndex)
              .asInstanceOf[java.lang.Double] -
              columns
                .get(colName)
                .data
                .get(times.get(lowerKey).dataIndex)
                .asInstanceOf[java.lang.Double]) *
              lowerKey.getDifferenceBetween(Milli, time) / lowerKey.getDifferenceBetween(Milli, higherKey)).asInstanceOf[String])
      } else {
        resultList.add(
          columns.get(colName).data.get(times.get(finalKey).dataIndex))
      }
    }
    if (resultList.size == 1) {
      resultList.get(0).asInstanceOf[AnyRef]
    } else {
      LogoList.fromJava(resultList)
    }
  }

  def getRangeByTime(timeLowArg: LogoTime,
                     timeHighArg: LogoTime,
                     columnName: String): AnyRef = {
    var timeLow = timeLowArg
    var timeHigh = timeHighArg
    if (!timeLow.isBefore(timeHigh)) {
      var timeTemp: LogoTime = timeLow
      timeLow = timeHigh
      timeHigh = timeTemp
    }
    val columnList: ArrayList[String] = new ArrayList[String](columns.size)
    val resultList: ArrayList[LogoList] = new ArrayList[LogoList](columns.size)
    if (columnName.==("ALL_-_COLUMNS")) {
      columnList.addAll(columns.keySet)
    } else if (columnName.==("LOGOTIME")) {} else if (!columns.containsKey(
                                                        columnName)) {
      throw new ExtensionException(
        "The LogoTimeSeries does not contain the column " + columnName)
    } else {
      columnList.add(columnName)
    }
    var lowerKey: LogoTime = timeLow
    if (times.get(lowerKey) == null) lowerKey = times.higherKey(timeLow)
    var higherKey: LogoTime = timeHigh
    if (times.get(higherKey) == null) higherKey = times.lowerKey(timeHigh)
    if (lowerKey == null || higherKey == null) {
      if (columnName.==("ALL_-_COLUMNS") || columnName.==("LOGOTIME")) {
        resultList.add(
          LogoList.fromVector(scala.collection.immutable.Vector[Any](0, 0, 0).asInstanceOf[Vector[AnyRef]])
        )
      }
      for (colName <- columnList.asScala) {
         resultList.add(
          LogoList.fromVector(scala.collection.immutable.Vector[Any](0, 0, 0).asInstanceOf[Vector[AnyRef]])
         )
      }
    } else {
      if (columnName.==("ALL_-_COLUMNS") || columnName.==("LOGOTIME")) {
        resultList.add(
          LogoList.fromJava(
            times.subMap(lowerKey, true, higherKey, true).keySet))
      }
      for (colName <- columnList.asScala) {
        resultList.add(
          LogoList.fromJava(
            columns
              .get(colName)
              .data
              .subList(times.get(lowerKey).dataIndex,
                       times.get(higherKey).dataIndex + 1)))
      }
    }
    if (resultList.size == 1) {
      resultList.get(0)
    } else {
      LogoList.fromJava(resultList)
    }
  }

  def dump(arg0: Boolean, arg1: Boolean, arg2: Boolean): String = {
    var result: String = "TIMESTAMP"
    for (colName <- columns.keySet.asScala) {
      result += "," + colName
    }
    result += "\n"
    for (logoTime <- times.keySet.asScala) {
      val time: TimeSeriesRecord = times.get(logoTime)
      result += time.time.dump(false, false, false)
      for (colName <- columns.keySet.asScala) {
        result = result + "," + columns.get(colName).data.get(time.dataIndex)
      }
      result += "\n"
    }
    result
  }

  def ensureDateTypeConsistent(time: LogoTime): Unit = {
    if (times.size > 0) {
      if (times.firstKey().dateType != time.dateType) {
        throw (new ExtensionException(
          "The LogoTimeSeries contains LogoTimes of type " + times
            .firstKey()
            .dateType
            .toString +
            " while the LogoTime " +
            time.toString +
            " used in the search is of type " +
            time.dateType.toString))
      }
    }
  }
  def getExtensionName(): String = "time"
  def getNLTypeName(): String = "LogoTimeSeries"
  def recursivelyEqual(arg0: AnyRef): Boolean = false

}
