package org.nlogo.extensions.time.datatypes

import java.io.{BufferedWriter, File, FileWriter, IOException, FileNotFoundException}
import java.util.{ArrayList, Arrays, LinkedHashMap, TreeMap}

import org.nlogo.api.ExtensionException
import org.nlogo.api.LogoException
import org.nlogo.core.{ExtensionObject, LogoList}
import org.nlogo.nvm.ExtensionContext
import org.nlogo.extensions.time._
import scala.util.{Try, Success, Failure}
import util.control.Breaks.{breakable, break}
import scala.util.Try
import scala.collection.JavaConverters._

class LogoTimeSeries extends ExtensionObject {
  var times: TreeMap[LogoTime, TimeSeriesRecord] =
    new TreeMap[LogoTime, TimeSeriesRecord](new LogoTimeComparator())
  var columns: LinkedHashMap[java.lang.String, TimeSeriesColumn] =
    new LinkedHashMap[java.lang.String, TimeSeriesColumn]()
  var numRows = 0

  def this(colNames: LogoList) = {
    this()
    for (colName <- colNames) {
      columns.put(colName.toString, new TimeSeriesColumn())
    }
  }

  def this(filename: String, customFormat: Option[String], context: ExtensionContext) = {
    this()
    parseTimeSeriesFile(filename, customFormat, context)
  }

  def this(filename: String, context: ExtensionContext) = {
    this()
    parseTimeSeriesFile(filename, context)
  }

  def add(time: LogoTime, list: LogoList): Unit = {
    val index: Int = times.size
    val record = new TimeSeriesRecord(time, index)
    columns.keySet.asScala.zipWithIndex.foreach{ case (colName, index) =>
      columns.get(colName).add(list.get(index).toString)
    }
    try times.put(time, record)
    catch {
      case e: NullPointerException =>
        if (time.dateType != times.keySet().toArray()(0).asInstanceOf[LogoTime].dateType) {
          throw new ExtensionException(
            s"""Cannot add a row with a LogoTime of type ${time.dateType.toString} to a LogoTimeSeries of type
                ${times.keySet().toArray()(0).asInstanceOf[LogoTime].dateType.toString}.
                Note, the first row added to the LogoTimeSeries object determines the data types for all columns.""")
        } else {
          throw e
        }
    }
  }

  def getNumColumns(): java.lang.Integer = columns.size

  def write(filename: String, context: ExtensionContext): Unit = {
    val dataFile = filename.charAt(0) match {
      case '/' => new File(filename)
      case '\\' => new File(filename)
      case _ if context.workspace.getModelDir == null =>
        new File("./" + filename)
      case _ =>
        new File(context.workspace.getModelDir + "/" + filename)
    }

    val fw = new FileWriter(dataFile.getAbsoluteFile)
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
        bw.write("," + columns.get(colName).data(time.dataIndex))
      }
      bw.write("\n")
    }
    bw.flush()
    bw.close()
  }

  def parseTimeSeriesFile(filename: String, context: ExtensionContext): Unit = {
    parseTimeSeriesFile(filename, None, context)
  }

  def parseTimeSeriesFile(filename: String, customFormat: Option[String], context: ExtensionContext): Unit = {
    /* parseTimeSeriesFile parses the files and adds them into the LogoTimeSeries
       object. There are a couple global variables: columns and times
     */
    Try(io.Source.fromFile(context.attachCurrentDirectory(filename))) match {
      case Success(bufferedSource) =>
        var ind = 0
        var columnNames = Array[String]()
        for( line <- bufferedSource.getLines) {
          breakable {
            line match {
              case linedata if linedata.charAt(0) == ";" => break
              case _ if ind == 0 =>
                ind = ind + 1
                columnNames = line.split(',').map(_.trim)
                for ( columnName <- Arrays.copyOfRange(columnNames, 1, columnNames.length)){
                  columns.put(columnName, new TimeSeriesColumn())
                }
              case linedata =>
                val lineData = linedata.split(",").map(_.trim)
                val newTime = new LogoTime(lineData(0), customFormat)
                times.put(newTime, new TimeSeriesRecord(newTime, numRows))
                numRows = numRows + 1
                var colInd: Int = 1
                while (colInd <= columns.size) {
                  if(columns.get(columnNames(colInd)) == null)
                    throw new ExtensionException(s"Failed term: ${columnNames(colInd)} and keyset ${columns.keySet}")
                  columns.get(columnNames(colInd)).add(lineData(colInd))
                  colInd += 1
                }
            }
          }
        }
      case Failure(_) => throw new ExtensionException("File cannot be saved without loading/saving a model")
    }
  }

  def getByTime(time: LogoTime, columnName: String, getMethod: GetTSMethod): AnyRef = {
    val columnList: ArrayList[String] = new ArrayList[String](columns.size)
    val resultList: ArrayList[AnyRef] = new ArrayList[AnyRef](columns.size)
    if (columnName.==("ALL_-_COLUMNS")) {
      columnList.addAll(columns.keySet)
    } else if (!columns.containsKey(columnName)) {
      throw new ExtensionException("The LogoTimeSeries does not contain the column " + columnName)
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
      if (higherKey == null && lowerKey == null){
        higherKey = time
        lowerKey = time
      } else if (higherKey == null){
        finalKey = lowerKey
      } else if (lowerKey == null) {
        finalKey = higherKey
      } else {
        getMethod match {
          case Exact =>
            throw new ExtensionException("The LogoTime " + time.dump(false,false,false)
              + " does not exist in the time series.")
          case Nearest =>
            finalKey =
              if (time.isCloserToAThanB(lowerKey, higherKey)) lowerKey
              else higherKey
          case LinearInterp => finalKey = time
        }
      }
    }
    if (columnName == ("ALL_-_COLUMNS")) resultList.add(finalKey)
    for (colName <- columnList.asScala) {
      if (getMethod == LinearInterp) {
        if(lowerKey == null &&  higherKey == null) {
            resultList.add(columns.get(colName)
              .data.map(_.toDouble).apply(times.get(finalKey).dataIndex).asInstanceOf[AnyRef])
        } else {
          if(lowerKey == null){
            lowerKey = time
          } else if (higherKey == null) {
            higherKey = time
          }
          val lowerKeyCol = columns.get(colName).data.apply(times.get(lowerKey).dataIndex)
          val higherKeyCol = columns.get(colName).data.map(_.toDouble).apply(times.get(higherKey).dataIndex)
          val lowerKeyCol2 = columns.get(colName).data.map(_.toDouble).apply(times.get(lowerKey).dataIndex)
          val keyDiv = lowerKey.getDifferenceBetween(Milli, time) / lowerKey.getDifferenceBetween(Milli, higherKey)
         resultList.add((lowerKeyCol + (higherKeyCol - lowerKeyCol2) * keyDiv)
         .toString)
        }
      } else {
        resultList.add(columns.get(colName)
          .data.map(_.toDouble).apply(times.get(finalKey).dataIndex).asInstanceOf[AnyRef])
      }
    }
    if (resultList.size == 1) {
      resultList.get(0)
    } else {
      LogoList.fromJava(resultList)
    }
  }

  def getRangeByTime(timeLowArg: LogoTime, timeHighArg: LogoTime, columnName: String): AnyRef = {
    if(columnName == null){ throw new ExtensionException(s"$columnName is null") }
    var timeLow = timeLowArg
    var timeHigh = timeHighArg
    if (!timeLow.isBefore(timeHigh)) {
      val timeTemp: LogoTime = timeLow
      timeLow = timeHigh
      timeHigh = timeTemp
    }

    var columnList: ArrayList[String] = new ArrayList[String](columns.size)
    var resultList: ArrayList[LogoList] = new ArrayList[LogoList](columns.size)
    columnName match {
        case "ALL_-_COLUMNS" => columnList.addAll(columns.keySet)
        case "LOGOTIME" =>
        case str if !columns.containsKey(columnName) =>
          throw new ExtensionException(s"The LogoTimeSeries does not contain the column $columnName")
        case _ => columnList.add(columnName)
    }

    var lowerKey: LogoTime = timeLow
    var higherKey: LogoTime = timeHigh
    if (times.get(lowerKey) == null) lowerKey = times.higherKey(timeLow)
    if (times.get(higherKey) == null) higherKey = times.lowerKey(timeHigh)
    if (lowerKey == null || higherKey == null) {
      if (columnName == "ALL_-_COLUMNS" || columnName == "LOGOTIME") {
        resultList.add(LogoList.fromVector(scala.collection.immutable.Vector[Any](0.0, 0.0, 0.0).asInstanceOf[Vector[AnyRef]]))
      }
      for (colName <- columnList.asScala) {
        resultList.add(
          LogoList.fromVector(scala.collection.immutable.Vector[Any](0.0, 0.0, 0.0)
            .asInstanceOf[Vector[AnyRef]]))
      }
    } else {
      if (columnName.==("ALL_-_COLUMNS") || columnName.==("LOGOTIME")) {
        resultList.add(LogoList.fromJava(times.subMap(lowerKey, true, higherKey, true).keySet))
      }
      for (colName <- columnList.asScala) {
        resultList.add(LogoList.fromJava(columns.get(colName).data
          .slice(from = times.get(lowerKey).dataIndex, until = times.get(higherKey).dataIndex + 1).asJava))
      }
    }
    resultList.size match {
      case 1 =>
        val allDoubles = resultList.get(0).toVector
          .forall(x =>
            try {
              x.asInstanceOf[String].toDouble.asInstanceOf[AnyRef]; true
            } catch {
              case _: Throwable => false
            }
          )
        if(allDoubles)
          LogoList.fromVector(resultList.get(0).toVector
            .map(x => x.asInstanceOf[String].toDouble.asInstanceOf[AnyRef]))
        else
          resultList.get(0)
      case n =>
        LogoList.fromJava(resultList)
    }
  }

  def dump(arg0: Boolean, arg1: Boolean, arg2: Boolean): String = {
    var result: String = "TIMESTAMP"
    columns.keySet.asScala.foreach{ case (colName) => result += "," + colName }
    result += "\n"
    times.keySet.asScala.foreach{
      case (logoTime) =>
        val time: TimeSeriesRecord = times.get(logoTime)
        result += time.time.dump(false, false, false)
        for (colName <- columns.keySet.asScala) {
          result = result + "," + columns.get(colName).data.apply(time.dataIndex)
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
