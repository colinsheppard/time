package org.nlogo.extensions.time.primitives

import java.time.format.DateTimeFormatter
import org.nlogo.api.Argument
import org.nlogo.api.Context
import org.nlogo.api.Reporter
import org.nlogo.api.ExtensionException
import org.nlogo.api.LogoException
import org.nlogo.core.Syntax
import org.nlogo.core.SyntaxJ
import org.nlogo.nvm.ExtensionContext
import org.nlogo.extensions.time._
import org.nlogo.extensions.time.datatypes._
import org.nlogo.extensions.time.primitives._
import scala.collection.JavaConverters._

object TimePrimitives {

  class NewLogoTime extends Reporter {
    def getSyntax(): Syntax =
      SyntaxJ.reporterSyntax(Array(Syntax.StringType), Syntax.WildcardType)
    def report(args: Array[Argument], context: Context): AnyRef = {
      TimeExtension.context = context
      val time: LogoTime = new LogoTime(TimeUtils.getStringFromArgument(args, 0))
      time
    }
  }

  class Anchor extends Reporter {
    def getSyntax(): Syntax =
      SyntaxJ.reporterSyntax(
        Array(Syntax.WildcardType, Syntax.NumberType, Syntax.StringType),
        Syntax.WildcardType)
    def report(args: Array[Argument], context: Context): AnyRef = {
      val time: LogoTime = TimeUtils.getTimeFromArgument(args, 0)
      val newTime: LogoTime = new LogoTime(time)
      newTime.setAnchor(
        TimeUtils.getDoubleFromArgument(args, 1),
        TimeUtils.stringToPeriodType(TimeUtils.getStringFromArgument(args, 2)),
        context.asInstanceOf[ExtensionContext].workspace.world
      )
      newTime
    }
  }

  class Show extends Reporter {
    def getSyntax(): Syntax =
      SyntaxJ.reporterSyntax(Array(Syntax.WildcardType, Syntax.StringType),
                             Syntax.StringType)
    def report(args: Array[Argument], context: Context): AnyRef = {
      val time: LogoTime = TimeUtils.getTimeFromArgument(args, 0)
      val fmtString: String = TimeUtils.getStringFromArgument(args, 1)
      var fmt: DateTimeFormatter = null
      fmt =
        if (fmtString.trim().==(""))
          DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS")
        else DateTimeFormatter.ofPattern(fmtString)
      time.show(fmt)
    }
  }

  class Copy extends Reporter {
    def getSyntax(): Syntax =
      SyntaxJ.reporterSyntax(Array(Syntax.WildcardType),
                             Syntax.WildcardType)
    def report(args: Array[Argument], context: Context): AnyRef = {
      val time: LogoTime = TimeUtils.getTimeFromArgument(args, 0)
      new LogoTime(time)
    }
  }

  class CreateWithFormat extends Reporter {
    def getSyntax(): Syntax =
      SyntaxJ.reporterSyntax(Array(Syntax.StringType, Syntax.StringType),
                             Syntax.WildcardType)
    def report(args: Array[Argument], context: Context): AnyRef = {
      TimeExtension.context = context
      val customFormat = TimeUtils.getStringFromArgument(args, 1) match {
        case null => None
        case x => Some(x) }
      new LogoTime(TimeUtils.getStringFromArgument(args, 0),customFormat)
    }
  }

  class DifferenceBetween extends Reporter {
    def getSyntax(): Syntax =
      SyntaxJ.reporterSyntax(Array(Syntax.WildcardType,
                                   Syntax.WildcardType,
                                   Syntax.StringType),
                                   Syntax.NumberType)
    def report(args: Array[Argument], context: Context): AnyRef = {
      val startTime: LogoTime = TimeUtils.getTimeFromArgument(args, 0)
      val endTime: LogoTime = TimeUtils.getTimeFromArgument(args, 1)
      val pType: PeriodType =
        TimeUtils.stringToPeriodType(TimeUtils.getStringFromArgument(args, 2))
      startTime.getDifferenceBetween(pType, endTime)
    }
  }

  class Get extends Reporter {
    def getSyntax(): Syntax =
      SyntaxJ.reporterSyntax(Array(Syntax.StringType, Syntax.WildcardType), Syntax.NumberType)
    def report(args: Array[Argument], context: Context): AnyRef = {
      val periodType: PeriodType =
        TimeUtils.stringToPeriodType(TimeUtils.getStringFromArgument(args, 0))
      val time: LogoTime = TimeUtils.getTimeFromArgument(args, 1)
      time.get(periodType).doubleValue().asInstanceOf[AnyRef]
    }
  }

  class IsAfter extends Reporter {
    def getSyntax(): Syntax =
      SyntaxJ.reporterSyntax(
        Array(Syntax.WildcardType, Syntax.WildcardType),
        Syntax.BooleanType)
    def report(args: Array[Argument], context: Context): AnyRef = {
      val timeA: LogoTime = TimeUtils.getTimeFromArgument(args, 0)
      val timeB: LogoTime = TimeUtils.getTimeFromArgument(args, 1)
      (!(timeA.isBefore(timeB) || timeA.isEqual(timeB))).asInstanceOf[AnyRef]
    }
  }

  class IsBefore extends Reporter {
    def getSyntax(): Syntax =
      SyntaxJ.reporterSyntax(
        Array(Syntax.WildcardType, Syntax.WildcardType),
        Syntax.BooleanType)
    def report(args: Array[Argument], context: Context): AnyRef = {
      val timeA: LogoTime = TimeUtils.getTimeFromArgument(args, 0)
      val timeB: LogoTime = TimeUtils.getTimeFromArgument(args, 1)
      timeA.isBefore(timeB).asInstanceOf[AnyRef]
    }
  }

  class IsBetween extends Reporter {
    def getSyntax(): Syntax =
      SyntaxJ.reporterSyntax(Array(Syntax.WildcardType,
                                   Syntax.WildcardType,
                                   Syntax.WildcardType),
                                   Syntax.BooleanType)
    def report(args: Array[Argument], context: Context): AnyRef = {
      val timeA: LogoTime = TimeUtils.getTimeFromArgument(args, 0)
      val timeB: LogoTime = TimeUtils.getTimeFromArgument(args, 1)
      val timeC: LogoTime = TimeUtils.getTimeFromArgument(args, 2)
      timeA.isBetween(timeB, timeC).asInstanceOf[AnyRef]
    }
  }

  class IsEqual extends Reporter {
    def getSyntax(): Syntax =
      SyntaxJ.reporterSyntax(
        Array(Syntax.WildcardType, Syntax.WildcardType),
        Syntax.BooleanType)
    def report(args: Array[Argument], context: Context): AnyRef = {
      val timeA: LogoTime = TimeUtils.getTimeFromArgument(args, 0)
      val timeB: LogoTime = TimeUtils.getTimeFromArgument(args, 1)
      timeA.isEqual(timeB).asInstanceOf[AnyRef]
    }
  }

  class Plus extends Reporter {
    def getSyntax(): Syntax =
      SyntaxJ.reporterSyntax(
        Array(Syntax.WildcardType, Syntax.NumberType, Syntax.StringType),
        Syntax.WildcardType)
    def report(args: Array[Argument], context: Context): AnyRef = {
      val time: LogoTime = new LogoTime(TimeUtils.getTimeFromArgument(args, 0))
      time.plus(
        TimeUtils.stringToPeriodType(TimeUtils.getStringFromArgument(args, 2)),
        TimeUtils.getDoubleFromArgument(args, 1))
    }
  }
}
