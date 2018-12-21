package org.nlogo.extensions.time.datatypes

import java.time.Duration
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.MonthDay
import java.time.Period
import java.time.ZoneOffset
import java.time.temporal.ChronoUnit
import java.time.chrono.{ Chronology, IsoChronology }
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle.{ FULL }
import org.nlogo.agent.World
import org.nlogo.api.ExtensionException
import org.nlogo.core.ExtensionObject
import org.nlogo.extensions.time._

class LogoTime(dt: LocalDateTime) extends ExtensionObject {
  var dateType: DateType = DateTime
  var datetime: LocalDateTime = dt
  var date: LocalDate = null
  var monthDay: MonthDay = null
  private var customFmt: DateTimeFormatter = null
  private var defaultFmt: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS")
  private var isAnchored: java.lang.Boolean = false
  private var tickValue: java.lang.Double = _
  private var tickType: PeriodType = _
  private var anchorDatetime: LocalDateTime = _
  private var anchorDate: LocalDate = _
  private var anchorMonthDay: MonthDay = _
  private var world: World = _

  def this(time: LogoTime) = {
    this(null: LocalDateTime)
    this.show(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS"))
  }

  def this(dt: LocalDate) = {
    this(null: LocalDateTime)
    this.date = dt
    this.defaultFmt = DateTimeFormatter.ofPattern("yyyy-MM-dd")
    this.dateType = Date
  }

  def this(dt: MonthDay) = {
    this(null: LocalDateTime)
    this.monthDay = dt
    this.defaultFmt = DateTimeFormatter.ofPattern("MM-dd")
    this.dateType = DayDate
  }

  def this(dateStringArg: String, customFormat: String) = {
    this(null: LocalDateTime)
    var dateString: String = dateStringArg
    // First we parse the string to determine the date type
    if (customFormat == null) {
      dateString = parseDateString(dateString)
    } else {
      this.dateType =
        if (customFormat.indexOf('H') >= 0 || customFormat.indexOf('h') >= 0 ||
            customFormat.indexOf('K') >= 0 ||
            customFormat.indexOf('k') >= 0) DateTime
        else if (customFormat.indexOf('Y') >= 0 || customFormat.indexOf('y') >= 0)
          Date
        else DayDate
    }
    // Now initialize the defaultFmt
    this.dateType match {
      case DateTime => this.defaultFmt = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS")
      case Date => this.defaultFmt = DateTimeFormatter.ofPattern("yyyy-MM-dd")
      case DayDate => this.defaultFmt = DateTimeFormatter.ofPattern("MM-dd")

    }
    if (customFormat == null) {
      this.dateType match {
        case DateTime =>
          this.datetime =
            if ((dateString.length == 0 || dateString.==("now")))
              LocalDateTime.now
            else LocalDateTime.parse(dateString)
        case Date => this.date = LocalDate.parse(dateString)
        case DayDate => this.monthDay = MonthDay.parse(dateString, this.defaultFmt)

      }
    } else {
      this.customFmt = DateTimeFormatter.ofPattern(customFormat)
      this.dateType match {
        case DateTime => this.datetime = LocalDateTime.parse(dateString, this.customFmt)
        case Date => this.date = LocalDate.parse(dateString, this.customFmt)
        case DayDate => this.monthDay = MonthDay.parse(dateString, this.customFmt)
      }
    }
  }

  def this(dateString: String) = this(dateString, null)

  def compareTo(that: LogoTime): Int = {
    this.dateType match {
      case DateTime => this.datetime.compareTo(that.datetime)
      case Date => this.date.compareTo(that.date)
      case DayDate => this.monthDay.compareTo(that.monthDay)
      case _ => -999
    }
  }

  def isCloserToAThanB(timeA: LogoTime, timeB: LogoTime): java.lang.Boolean = {
    val refDateTime: LocalDateTime = LocalDateTime.now(ZoneOffset.UTC)
    var millisToA: java.lang.Long = null
    var millisToB: java.lang.Long = null
    this.dateType match {
      case DateTime =>{
        millisToA = Math.abs(
          (Duration.between(timeA.datetime.`with`(refDateTime),
                        this.datetime.`with`(refDateTime))).toMillis)
        millisToB = Math.abs(
          (Duration.between(timeB.datetime.`with`(refDateTime),
            this.datetime.`with`(refDateTime))).toMillis)
      }
      case Date =>
        millisToA = Math.abs(
          (Duration.between(timeA.date.`with`(refDateTime),
                        this.date.`with`(refDateTime))).toMillis)
        millisToB = Math.abs(
          (Duration.between(timeB.date.`with`(refDateTime),
                        this.date.`with`(refDateTime))).toMillis)
      case DayDate =>
        millisToA = Math.abs((Duration.between(
          timeA.monthDay.atYear(2000).`with`(refDateTime),
          this.monthDay.atYear(2000).`with`(refDateTime))).toMillis)
        millisToB = Math.abs((Duration.between(
          timeB.monthDay.atYear(2000).`with`(refDateTime),
          this.monthDay.atYear(2000).`with`(refDateTime))).toMillis)

    }
    millisToA < millisToB
  }

  def parseDateString(dateStringT: String): String = {
    var dateString: String = dateStringT.replace('/', '-').replace(' ', 'T').trim()
    var len: Int = dateString.length
    // First add 0's to pad single digit months / days if necessary
    val firstDash: Int = dateString.indexOf('-')
    if (firstDash == 1 || firstDash == 2) {
    // DAY
      if (firstDash == 1) {
      // month is single digit
        dateString = "0" + dateString { len += 1; len - 1 }
      }
      // Now check the day for a single digit
      if (len == 4) {
        dateString = dateString.substring(0, 3) + "0" + dateString.substring(
            3,
            4) { len += 1; len - 1 }
      } else if (len < 5) {
        throw new ExtensionException(
          "Illegal time string: '" + dateString + "'")
      }
    } else if (firstDash != 4 && firstDash != -1) {
      throw new ExtensionException("Illegal time string: '" + dateString + "'")
    } else {
      // DATETIME or DATE
      val secondDash: Int = dateString.lastIndexOf('-')
      if (secondDash == 6) {
      // month is single digit
        dateString = dateString.substring(0, 5) + "0" + dateString.substring(
            5,
            len) { len += 1; len - 1 }
      }
      if (len == 9 || dateString.indexOf('T') == 9) {
      // day is single digit
        dateString = dateString.substring(0, 8) + "0" + dateString.substring(
            8,
            len) { len += 1; len - 1 }
      }
      if (dateString.indexOf('T') == 10 & (dateString.indexOf(':') == 12 || len == 12)) {
      // DATETIME without leading 0 on hour, pad it
        val firstColon: Int = dateString.indexOf(':')
        dateString = dateString.substring(0, 11) + "0" + dateString.substring(
            11,
            len) { len += 1; len - 1 }
      }
    }
    if (len == 23 || len == 21 || len == 3 || len == 0) {
      // a full DATETIME
      this.dateType = DateTime
    } else if (len == 19 || len == 17) {
      // a DATETIME without millis
      this.dateType = DateTime
      dateString += ".000"
    } else if (len == 16 || len == 14) {
      // a DATETIME without seconds or millis
      this.dateType = DateTime
      dateString += ":00.000"
    } else if (len == 13 || len == 11) {
      // a DATETIME without minutes, seconds or millis
      this.dateType = DateTime
      dateString += ":00:00.000"
    } else if (len == 10) {
      // a DATE
      this.dateType = Date
    } else if (len == 5) {
      // a DAY
      this.dateType = DayDate
    } else {
      throw new ExtensionException("Illegal time string: '" + dateString + "'")
    }
    dateString
  }

  def setAnchor(tickCount: java.lang.Double,
                tickType: PeriodType,
                world: World): Unit = {
    if (tickType == DayOfWeek)
      throw new ExtensionException(
        tickType.toString + " type is not a supported tick type")
    this.isAnchored = true
    this.tickValue = tickCount
    this.tickType = tickType
    this.dateType match {
      case DateTime => this.anchorDatetime = LocalDateTime.from(this.datetime)
      case Date => this.anchorDate = LocalDate.from(this.date)
      case DayDate => this.anchorMonthDay = MonthDay.from(this.monthDay)
    }
    this.world = world
  }

  def dump(arg1: Boolean, arg2: Boolean, arg3: Boolean): String = this.toString

  override def toString(): String = {
    try this.updateFromTick()
    catch {
      case e: ExtensionException => {}
    }
    this.dateType match {
      case DateTime =>
        datetime.format(
          if (this.customFmt == null) this.defaultFmt else this.customFmt)
      case Date =>
        date.format(
          if (this.customFmt == null) this.defaultFmt else this.customFmt)
      case DayDate =>
        monthDay.format(
          if (this.customFmt == null) this.defaultFmt else this.customFmt)

      case _ => ""
    }
  }

  def updateFromTick(): Unit = {
    if (!this.isAnchored) return
    this.dateType match {
      case DateTime =>
        this.datetime = this
          .plus(this.anchorDatetime,
                this.tickType,
                this.world.ticks * this.tickValue)
          .datetime
      case Date =>
        this.date = this
          .plus(this.anchorDate,
                this.tickType,
                this.world.ticks * this.tickValue)
          .date
      case DayDate =>
        this.monthDay = this
          .plus(this.anchorMonthDay,
                this.tickType,
                this.world.ticks * this.tickValue)
          .monthDay
    }
  }

  def getExtensionName(): String = "time"
  def getNLTypeName(): String = "logotime"
  def recursivelyEqual(arg0: AnyRef): Boolean = equals(arg0)

  def show(fmt: DateTimeFormatter): String = {
    this.dateType match {
      case DateTime => this.datetime.format(fmt)
      case Date => this.date.format(fmt)
      case DayDate => this.monthDay.format(fmt)
      case _ => ""
    }
  }

  def get(periodType: PeriodType): java.lang.Integer = {
    periodType match {
      case Milli =>
        this.dateType match { // getChronology().millisOfSecond().get(getMillis());
          case DateTime => (datetime.getSecond * 1000)
          case Date => LocalDateTime.from(date).getSecond() * 1000
          case DayDate => LocalDateTime.from(monthDay).getSecond() * 1000

        }
      case Second =>
        this.dateType match {
          case DateTime => datetime.getSecond * 1000
          case Date =>LocalDateTime.from(date).getSecond() * 1000
          case DayDate =>LocalDateTime.from(monthDay).getSecond() * 1000

        }
      case Minute =>
        this.dateType match {
          case DateTime => datetime.getMinute()
          case Date => LocalDateTime.from(date).getMinute()
          case DayDate => LocalDateTime.from(monthDay).getMinute()

        }
      case Hour =>
        this.dateType match {
          case DateTime => datetime.getHour
          case Date => LocalDateTime.from(date).getHour()
          case DayDate => LocalDateTime.from(monthDay).getHour()

        }
      case Day =>
        this.dateType match {
          case DateTime => datetime.getDayOfMonth()
          case Date => LocalDateTime.from(date).getDayOfMonth()
          case DayDate => LocalDateTime.from(monthDay).getDayOfMonth()

        }
      case DayOfYear =>
        this.dateType match {
          case DateTime => datetime.getDayOfYear
          case Date => LocalDateTime.from(date).getDayOfYear()
          case DayDate => LocalDateTime.from(monthDay).getDayOfYear()
        }
      case DayOfWeek =>
        this.dateType match {
          case DateTime => datetime.getDayOfWeek.getValue()
          case Date => LocalDateTime.from(date).getDayOfWeek().getValue()
          case DayDate => LocalDateTime.from(monthDay).getDayOfWeek().getValue()
        }
      case Week =>
        this.dateType match {
          case DateTime => datetime.getDayOfYear() / 52
          case Date => LocalDateTime.from(date).getDayOfYear() / 52
          case DayDate => LocalDateTime.from(monthDay).getDayOfYear() / 52

        }
      case Month =>
        this.dateType match {
          case DateTime => datetime.getMonthValue()
          case Date => LocalDateTime.from(date).getMonthValue()
          case DayDate => LocalDateTime.from(monthDay).getMonthValue()

        }
      case Year =>
        this.dateType match {
          case DateTime => datetime.getYear()
          case Date => LocalDateTime.from(date).getYear()
          case DayDate =>LocalDateTime.from(monthDay).getYear()

        }
      case _ => null
    }
  }

  def plus(pType: PeriodType, durVal: java.lang.Double): LogoTime = {
    this.dateType match {
      case DateTime => this.plus(this.datetime, pType, durVal)
      case Date => this.plus(this.date, pType, durVal)
      case DayDate => this.plus(this.monthDay, pType, durVal)
      case _ => null
    }
  }

  def plus(refTime: AnyRef,
           pType: PeriodType,
           durValArg: java.lang.Double): LogoTime = {
    var per: Period = null
    var durVal: java.lang.Double = durValArg
    pType match {
      case Week => durVal *= 7*24*60*60*1000
      case Day | DayOfYear => durVal *= 24*60*60*1000
      case Hour => durVal *= 60*60*1000
      case Minute => durVal *= 60*1000
      case Second => durVal *= 1000
      case Milli => //break
      case Month =>
        per = Period.of(0, TimeUtils.roundDouble(durVal), 0)
      case Year =>
        per = Period.of(TimeUtils.roundDouble(durVal), 0, 0)
      case _ =>
        throw new ExtensionException(
          pType + " type is not supported by the time:plus primitive")
    }
    this.dateType match {
      case DateTime =>
        if (per == null) {
          new LogoTime(refTime
              .asInstanceOf[LocalDateTime]
              .plus(Duration.of(TimeUtils.dToL(durVal), ???))) // you need to set it to the correct unit
        } else {
          new LogoTime(refTime.asInstanceOf[LocalDateTime].plus(per))
        }
      case Date =>
        if (per == null) {
          val dayDurVal: java.lang.Integer =
            (durVal / (24.0 * 60.0 * 60.0 * 1000.0))
              .asInstanceOf[java.lang.Double]
              .intValue()
          new LogoTime(refTime.asInstanceOf[LocalDate].plusDays(dayDurVal.asInstanceOf[Long]))
        } else {
          new LogoTime(refTime.asInstanceOf[LocalDate].plus(per))
        }
      case DayDate =>
        if (per == null) {
          val dayDurVal: java.lang.Integer =
            (durVal / (24.0 * 60.0 * 60.0 * 1000.0))
              .asInstanceOf[java.lang.Double]
              .intValue()
          new LogoTime(refTime.asInstanceOf[LocalDateTime].plusDays(dayDurVal.asInstanceOf[Long]))
        } else {
          new LogoTime(refTime.asInstanceOf[LocalDateTime].plus(per))
        }
      case _ => null
    }
  }

  def isBefore(timeB: LogoTime): Boolean = {
    if (this.dateType != timeB.dateType)
      throw new ExtensionException(
        "time comparisons only work if the LogoTime's are the same variety, but you called with a " +
          this.dateType.toString +
          " and a " +
          timeB.dateType.toString)
    this.dateType match {
      case DateTime => this.datetime.isBefore(timeB.datetime)
      case Date => this.date.isBefore(timeB.date)
      case DayDate => this.monthDay.isBefore(timeB.monthDay)
      case _ => true
    }
  }

  def isEqual(timeB: LogoTime): Boolean = {
    if (this.dateType != timeB.dateType)
      throw new ExtensionException(
        "time comparisons only work if the LogoTime's are the same variety, but you called with a " +
          this.dateType.toString +
          " and a " +
          timeB.dateType.toString)
    this.dateType match {
      case DateTime => this.datetime.isEqual(timeB.datetime)
      case Date => this.date.isEqual(timeB.date)
      case DayDate => this.monthDay.equals(timeB.monthDay)
      case _ => true
    }
  }

  def isBetween(timeAArg: LogoTime, timeBArg: LogoTime): Boolean = {
    var timeA: LogoTime = timeAArg
    var timeB: LogoTime = timeBArg
    if (!timeA.isBefore(timeB)) {
      val tempA: LogoTime = timeA
      timeA = timeB
      timeB = tempA
    }
    if (this.dateType != timeA.dateType || this.dateType != timeB.dateType)
      throw new ExtensionException(
        "time comparisons only work if the LogoTime's are the same variety, but you called with a " +
          this.dateType.toString +
          ", a " +
          timeA.dateType.toString +
          ", and a " +
          timeB.dateType.toString)
    this.dateType match {
      case DateTime =>
        ((this.datetime.isAfter(timeA.datetime) && this.datetime
          .isBefore(timeB.datetime)) ||
          this.datetime.isEqual(timeA.datetime) ||
          this.datetime.isEqual(timeB.datetime))
      case Date =>
        ((this.date.isAfter(timeA.date) && this.date.isBefore(timeB.date)) ||
          this.date.isEqual(timeA.date) ||
          this.date.isEqual(timeB.date))
      case DayDate =>
        ((this.monthDay.isAfter(timeA.monthDay) && this.monthDay
          .isBefore(timeB.monthDay)) ||
          this.monthDay.equals(timeA.monthDay) ||
          this.monthDay.equals(timeB.monthDay))
      case _ => true
    }
  }

  def getDifferenceBetween(pType: PeriodType,
                           endTime: LogoTime): java.lang.Double = {
    if (this.dateType != endTime.dateType){
      throw new ExtensionException(
        "time comparisons only work if the LogoTimes are the same variety, but you called with a " +
          this.dateType.toString + " and a " + endTime.dateType.toString)
    }
    var durVal: java.lang.Double = 1.0
    pType match {
      case Year =>
        this.dateType match {
          case DateTime =>
            TimeUtils.intToDouble(
              (Period.between(this.datetime.asInstanceOf[LocalDate], endTime.datetime.asInstanceOf[LocalDate])).getYears)
          case Date =>
            TimeUtils.intToDouble(
              (Period.between(this.date, endTime.date)).getYears)
          case DayDate =>
            throw new ExtensionException(
              pType +
                " type is not supported by the time:difference-between primitive with LogoTimes of type DAY")

        }
      case Month =>
        this.dateType match {
          case DateTime =>
            TimeUtils.intToDouble(
              (Period.between(this.datetime.asInstanceOf[LocalDate], endTime.datetime.asInstanceOf[LocalDate])).getMonths)
          case Date =>
            TimeUtils.intToDouble(
              (Period.between(this.date, endTime.date)).getMonths)
          case DayDate =>
            TimeUtils.intToDouble(
              (Period.between(LocalDate.from(this.monthDay), LocalDate.from(endTime.monthDay))).getMonths)

        }
      case Week | Day | DayOfYear | Hour | Minute | Second | Milli =>
        var refDateTime: LocalDateTime = LocalDateTime.now(ZoneOffset.UTC)
        pType match {
          case Week => durVal /= (7.0 * 24.0 * 60.0 * 1000)
          case Day | DayOfYear => durVal /= (7.0 * 24.0 * 60.0 * 1000)
          case Hour => durVal /= (24.0 * 60.0 * 1000)
          case Minute => durVal /= (60.0 * 1000)
          case Second => durVal /= 1000
          case _ =>
        }
        this.dateType match {
          case DateTime =>
            durVal *
              (Duration.between(
                this.datetime.`with`(refDateTime),
                endTime.datetime.`with`(refDateTime))).toMillis
          case Date =>
            durVal *
              (Duration.between(
                this.date.`with`(refDateTime),
                endTime.date.`with`(refDateTime))).toMillis
          case DayDate =>
            durVal *
              (Duration.between(
                this.monthDay.atYear(2000).`with`(refDateTime),
                endTime.monthDay.atYear(2000).`with`(refDateTime))).toMillis
        }
      case _ =>
        throw new ExtensionException(
          pType + " type is not supported by the time:difference-between primitive")
    }
  }
}
