package time;

import time.datatypes.LogoSchedule;
import time.primitives.DiscreteEventSchedulerPrimitives;
import time.primitives.TimePrimitives;
import time.primitives.TimeSeriesPrimitives;

import org.nlogo.api.Context;

public class TimeExtension extends org.nlogo.api.DefaultClassManager {

	public java.util.List<String> additionalJars() {
		java.util.List<String> list = new java.util.ArrayList<String>();
		list.add("joda-time-2.2.jar");
		return list;
	}

	public static final LogoSchedule schedule = new LogoSchedule();
	public static Context context;
	public static long nextEvent = 0;
	public static boolean debug = false;

	public void load(org.nlogo.api.PrimitiveManager primManager) {
		/**********************
		/* TIME PRIMITIVES
		/**********************/
		primManager.addPrimitive("create", new TimePrimitives.NewLogoTime());
		primManager.addPrimitive("create-with-format", new TimePrimitives.CreateWithFormat());
		primManager.addPrimitive("anchor-to-ticks", new TimePrimitives.Anchor());
		primManager.addPrimitive("plus", new TimePrimitives.Plus());
		primManager.addPrimitive("show", new TimePrimitives.Show());
		primManager.addPrimitive("get", new TimePrimitives.Get());
		primManager.addPrimitive("copy", new TimePrimitives.Copy());
		primManager.addPrimitive("is-before", new TimePrimitives.IsBefore());
		primManager.addPrimitive("is-after", new TimePrimitives.IsAfter());
		primManager.addPrimitive("is-equal", new TimePrimitives.IsEqual());
		primManager.addPrimitive("is-between", new TimePrimitives.IsBetween());
		primManager.addPrimitive("difference-between", new TimePrimitives.DifferenceBetween());

		/********************************************
		/* DISCRETE EVENT SIMULATION PRIMITIVES
		/*******************************************/
		primManager.addPrimitive("size-of-schedule", new DiscreteEventSchedulerPrimitives.GetSize());
		primManager.addPrimitive("schedule-event", new DiscreteEventSchedulerPrimitives.AddEvent());
		primManager.addPrimitive("schedule-event-shuffled", new DiscreteEventSchedulerPrimitives.AddEventShuffled());
		primManager.addPrimitive("schedule-repeating-event", new DiscreteEventSchedulerPrimitives.RepeatEvent());
		primManager.addPrimitive("schedule-repeating-event-shuffled", new DiscreteEventSchedulerPrimitives.RepeatEventShuffled());
		primManager.addPrimitive("schedule-repeating-event-with-period", new DiscreteEventSchedulerPrimitives.RepeatEventWithPeriod());
		primManager.addPrimitive("schedule-repeating-event-shuffled-with-period", new DiscreteEventSchedulerPrimitives.RepeatEventShuffledWithPeriod());
		primManager.addPrimitive("anchor-schedule", new DiscreteEventSchedulerPrimitives.AnchorSchedule());
		primManager.addPrimitive("go", new DiscreteEventSchedulerPrimitives.Go());
		primManager.addPrimitive("go-until", new DiscreteEventSchedulerPrimitives.GoUntil());
		primManager.addPrimitive("clear-schedule", new DiscreteEventSchedulerPrimitives.ClearSchedule());
		primManager.addPrimitive("show-schedule", new DiscreteEventSchedulerPrimitives.ShowSchedule());

		/**********************
		/* TIME SERIES PRIMITIVES
		/**********************/
		primManager.addPrimitive("ts-create", new TimeSeriesPrimitives.TimeSeriesCreate());
		primManager.addPrimitive("ts-load", new TimeSeriesPrimitives.TimeSeriesLoad());
		primManager.addPrimitive("ts-load-with-format", new TimeSeriesPrimitives.TimeSeriesLoadWithFormat());
		primManager.addPrimitive("ts-write", new TimeSeriesPrimitives.TimeSeriesWrite());
		primManager.addPrimitive("ts-get", new TimeSeriesPrimitives.TimeSeriesGet());
		primManager.addPrimitive("ts-get-interp", new TimeSeriesPrimitives.TimeSeriesGetInterp());
		primManager.addPrimitive("ts-get-exact", new TimeSeriesPrimitives.TimeSeriesGetExact());
		primManager.addPrimitive("ts-get-range", new TimeSeriesPrimitives.TimeSeriesGetRange());
		primManager.addPrimitive("ts-add-row", new TimeSeriesPrimitives.TimeSeriesAddRow());
	}
	public void clearAll() {
		this.schedule.clear();
	}



}
