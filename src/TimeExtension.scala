package org.nlogo.extensions.time

import org.nlogo.extensions.time._
import org.nlogo.extensions.time.datatypes.LogoSchedule
import org.nlogo.extensions.time.primitives.DiscreteEventSchedulerPrimitives
import org.nlogo.extensions.time.primitives.TimePrimitives
import org.nlogo.extensions.time.primitives.TimeSeriesPrimitives
import org.nlogo.api.DefaultClassManager
import org.nlogo.api.PrimitiveManager
import org.nlogo.api.Context

object TimeExtension {
  val schedule: LogoSchedule = new LogoSchedule()
  var context: Context = _
  var nextEvent: Long = 0
  var debug: Boolean = false
}

class TimeExtension extends DefaultClassManager {
  override def additionalJars(): java.util.List[String] = {
    val list: java.util.List[String] = new java.util.ArrayList[String]()
    list
  }

  def load(primManager: org.nlogo.api.PrimitiveManager): Unit = {
    primManager.addPrimitive("create", new TimePrimitives.NewLogoTime())
    primManager.addPrimitive("create-with-format",
                             new TimePrimitives.CreateWithFormat())
    primManager.addPrimitive("anchor-to-ticks", new TimePrimitives.Anchor())
    primManager.addPrimitive("plus", new TimePrimitives.Plus())
    primManager.addPrimitive("show", new TimePrimitives.Show())
    primManager.addPrimitive("get", new TimePrimitives.Get())
    primManager.addPrimitive("copy", new TimePrimitives.Copy())
    primManager.addPrimitive("is-before", new TimePrimitives.IsBefore())
    primManager.addPrimitive("is-after", new TimePrimitives.IsAfter())
    primManager.addPrimitive("is-equal", new TimePrimitives.IsEqual())
    primManager.addPrimitive("is-between", new TimePrimitives.IsBetween())
    primManager.addPrimitive("difference-between",
                             new TimePrimitives.DifferenceBetween())
    primManager.addPrimitive("size-of-schedule",
                             new DiscreteEventSchedulerPrimitives.GetSize())
    primManager.addPrimitive("schedule-event",
                             new DiscreteEventSchedulerPrimitives.AddEvent())
    primManager.addPrimitive(
      "schedule-event-shuffled",
      new DiscreteEventSchedulerPrimitives.AddEventShuffled())
    primManager.addPrimitive(
      "schedule-repeating-event",
      new DiscreteEventSchedulerPrimitives.RepeatEvent())
    primManager.addPrimitive(
      "schedule-repeating-event-shuffled",
      new DiscreteEventSchedulerPrimitives.RepeatEventShuffled())
    primManager.addPrimitive(
      "schedule-repeating-event-with-period",
      new DiscreteEventSchedulerPrimitives.RepeatEventWithPeriod())
    primManager.addPrimitive(
      "schedule-repeating-event-shuffled-with-period",
      new DiscreteEventSchedulerPrimitives.RepeatEventShuffledWithPeriod())
    primManager.addPrimitive(
      "anchor-schedule",
      new DiscreteEventSchedulerPrimitives.AnchorSchedule())
    primManager.addPrimitive("go", new DiscreteEventSchedulerPrimitives.Go())
    primManager.addPrimitive("go-until",
                             new DiscreteEventSchedulerPrimitives.GoUntil())
    primManager.addPrimitive(
      "clear-schedule",
      new DiscreteEventSchedulerPrimitives.ClearSchedule())
    primManager.addPrimitive(
      "show-schedule",
      new DiscreteEventSchedulerPrimitives.ShowSchedule())
    primManager.addPrimitive("ts-create",
                             new TimeSeriesPrimitives.TimeSeriesCreate())
    primManager.addPrimitive("ts-load",
                             new TimeSeriesPrimitives.TimeSeriesLoad())
    primManager.addPrimitive(
      "ts-load-with-format",
      new TimeSeriesPrimitives.TimeSeriesLoadWithFormat())
    primManager.addPrimitive("ts-write",
                             new TimeSeriesPrimitives.TimeSeriesWrite())
    primManager.addPrimitive("ts-get",
                             new TimeSeriesPrimitives.TimeSeriesGet())
    primManager.addPrimitive("ts-get-interp",
                             new TimeSeriesPrimitives.TimeSeriesGetInterp())
    primManager.addPrimitive("ts-get-exact",
                             new TimeSeriesPrimitives.TimeSeriesGetExact())
    primManager.addPrimitive("ts-get-range",
                             new TimeSeriesPrimitives.TimeSeriesGetRange())
    primManager.addPrimitive("ts-add-row",
                             new TimeSeriesPrimitives.TimeSeriesAddRow())
  }

  override def clearAll(): Unit = {
    TimeExtension.schedule.clear()
 }
}
