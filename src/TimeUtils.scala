package org.nlogo.extensions.time

import java.io.IOException
import java.nio.file.Files
import java.nio.file.Paths
import java.nio.file.StandardOpenOption
import java.util.logging.FileHandler
import java.util.logging.Logger
import java.util.logging.SimpleFormatter
import org.nlogo.api.Argument
import org.nlogo.api.Context
import org.nlogo.api.Dump
import org.nlogo.api.ExtensionException
import org.nlogo.api.LogoException
import org.nlogo.api.OutputDestination
import org.nlogo.api.OutputDestinationJ
import org.nlogo.core.LogoList
import org.nlogo.nvm.ExtensionContext
import org.nlogo.window.OutputArea
import org.nlogo.extensions.time._
import org.nlogo.extensions.time.datatypes.{ LogoTimeSeries, LogoTime, LogoSchedule }

object TimeUtils {

 /**********************
 * Convenience Methods *
 ***********************/
  def dToL(d: Double): java.lang.Long =
    d.asInstanceOf[java.lang.Double].longValue()

  def stringToPeriodType(sTypeArg: String): PeriodType = {
    var sType: String = sTypeArg.trim().toLowerCase().asInstanceOf[String]
    if (sType.substring(sType.length - 1).==("s")){
      sType = sType.substring(0, sType.length - 1)
    }
    sType match {
      case "milli" => Milli
      case "second" => Second
      case "minute" => Hour
      case stype if stype == "day" || stype == "dayofmonth" || stype == "dom" => Day
      case stype if stype == "dayofyear" || stype == "doy" || stype == "julianday" || stype == "jday"
          => DayOfYear
      case stype if stype == "dayofweek" || stype == "dow" || stype == "weekday" || stype == "wday"
          => DayOfWeek
      case "week" => Week
      case "month" => Month
      case "year" => Year
    }
  }

  def getTimeFromArgument(args: Array[Argument],
                          argIndex: java.lang.Integer): LogoTime = {
    var time: LogoTime = null
    val obj: AnyRef = args(argIndex).get
    if (obj.isInstanceOf[String]) {
      time = new LogoTime(args(argIndex).getString)
    } else if (obj.isInstanceOf[LogoTime]) {
      time = obj.asInstanceOf[LogoTime]
    } else {
      throw new ExtensionException(
        "time: was expecting a LogoTime object as argument " +
          (argIndex + 1) +
          ", found this instead: " +
          Dump.logoObject(obj))
    }
    time.updateFromTick()
    time
  }

  def getDoubleFromArgument(args: Array[Argument],
                            argIndex: java.lang.Integer): java.lang.Double = {
    val obj: AnyRef = args(argIndex).get
    if (!(obj.isInstanceOf[java.lang.Double])) {
      throw new ExtensionException(
        "time: was expecting a number as argument " + (argIndex + 1) +
          ", found this instead: " +
          Dump.logoObject(obj))
    }
    obj.asInstanceOf[java.lang.Double]
  }

  def getListFromArgument(args: Array[Argument],
                          argIndex: java.lang.Integer): LogoList = {
    val obj: AnyRef = args(argIndex).get
    if (!(obj.isInstanceOf[LogoList])) {
      throw new ExtensionException(
        "time: was expecting a list as argument " + (argIndex + 1) +
          ", found this instead: " +
          Dump.logoObject(obj))
    }
    obj.asInstanceOf[LogoList]
  }

  def getIntFromArgument(args: Array[Argument],
                         argIndex: java.lang.Integer): java.lang.Integer = {
    val obj: AnyRef = args(argIndex).get
    if (obj.isInstanceOf[java.lang.Double]) {
      // Round to nearest int
      roundDouble(obj.asInstanceOf[java.lang.Double])
    } else if (!(obj.isInstanceOf[java.lang.Integer])) {
      throw new ExtensionException(
        "time: was expecting a number as argument " + (argIndex + 1) +
          ", found this instead: " +
          Dump.logoObject(obj))
    }
    obj.asInstanceOf[java.lang.Integer]
  }

  def getLongFromArgument(args: Array[Argument],
                          argIndex: java.lang.Integer): java.lang.Long = {
    val obj: AnyRef = args(argIndex).get
    if (obj.isInstanceOf[java.lang.Double]) {
      obj.asInstanceOf[java.lang.Double].longValue()
    } else if (!(obj.isInstanceOf[java.lang.Integer])) {
      throw new ExtensionException(
        "time: was expecting a number as argument " + (argIndex + 1) +
          ", found this instead: " +
          Dump.logoObject(obj))
    }
    obj.asInstanceOf[java.lang.Long]
  }

  def getStringFromArgument(args: Array[Argument],
                            argIndex: java.lang.Integer): String = {
    val obj: AnyRef = args(argIndex).get
    if (!(obj.isInstanceOf[String])) {
      throw new ExtensionException(
        "time: was expecting a string as argument " + (argIndex + 1) +
          ", found this instead: " +
          Dump.logoObject(obj))
    }
    obj.asInstanceOf[String]
  }

  def getTimeSeriesFromArgument(
      args: Array[Argument],
      argIndex: java.lang.Integer): LogoTimeSeries = {
    var ts: LogoTimeSeries = null
    val obj: AnyRef = args(argIndex).get
    if (obj.isInstanceOf[LogoTimeSeries]) {
      ts = obj.asInstanceOf[LogoTimeSeries]
    } else {
      throw new ExtensionException(
        "time: was expecting a LogoTimeSeries object as argument " +
          (argIndex + 1) +
          ", found this instead: " +
          Dump.logoObject(obj))
    }
    ts
  }

  def roundDouble(d: java.lang.Double): java.lang.Integer =
    Math.round(d).asInstanceOf[java.lang.Long].intValue()

  def intToDouble(i: Int): java.lang.Double =
    (new java.lang.Integer(i)).doubleValue()

  def printToLogfile(msg: String): Unit = {
    val logger: Logger = Logger.getLogger("MyLog")
    var fh: FileHandler = null
    try {
      // This block configure the logger with handler and formatter
      fh = new FileHandler("logfile.txt", true)
      logger.addHandler(fh)
      //logger.setLevel(Level.ALL);
      val formatter: SimpleFormatter = new SimpleFormatter()
      fh.setFormatter(formatter)
      // the following statement is used to log any messages
      logger.info(msg)
      fh.close()
    } catch {
      case e: SecurityException => e.printStackTrace()

      case e: IOException => e.printStackTrace()

    }
  }

  // Convenience method, to extract a schedule object from an Argument.
  def getScheduleFromArguments(args: Array[Argument],
                               index: Int): LogoSchedule = {
    val obj: AnyRef = args(index).get
    if (!(obj.isInstanceOf[LogoSchedule])) {
      throw new ExtensionException(
        "Was expecting a LogoSchedule as argument " + (index + 1) +
          " found this instead: " +
          Dump.logoObject(obj))
    }
    obj.asInstanceOf[LogoSchedule]
  }

  def printToConsole(context: Context, msg: String): Unit = {
    val extcontext: ExtensionContext = context.asInstanceOf[ExtensionContext]
    extcontext
      .workspace
      .outputObject(msg, null, true, true, OutputDestinationJ.OUTPUT_AREA)
    Files.write(
      Paths.get("/Users/critter/Dropbox/netlogo/time/debugging/log.txt"),
      (msg + "\n").getBytes,
      StandardOpenOption.APPEND)
  }

  def setContext(context: Context): Unit = {
    TimeExtension.context = context
  }
}
