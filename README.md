# NetLogo Time Extension

* [What is it?](#what-is-it)
* [Installation](#installation)
* [Examples](#examples)
* [Behavior](#behavior)
* [Primitives](#primitives)
* [Building](#building)
* [Author](#author)
* [Feedback](#feedback-bugs-feature-requests)
* [Credits](#credits)
* [Terms of use](#terms-of-use)

## What is it?

This package contains the NetLogo **time extension**, which provides NetLogo with a set of common date and time operations and discrete event scheduling capabilities.  

**Dates and Times**

This extension is powered by the [Joda Time API for Java](http://joda-time.sourceforge.net/), and while this README describes some of the subtle details of how dates and times are treated, it is recommended that you review at least the front page of Joda Time's website and preferably the "Key Concepts" topics in the navigation menu.  This documentation will make use of the terminology established by Joda Time (e.g. there's a meaningful difference between an *interval*, a *duration*, and a *period*.)

**Dicrete Event Scheduling**

*Note:*  Formerly this capability was published as the **Dynamic Scheduler Extension**, but that extension has been merged into the **time extension** in order to integrate the functionality of both.

The **time extension** enables a different approach toward scheduling actions in Netlogo.  Traditionally, a Netlogo modeler puts a series of actions or procedure calls into the "go" procedure which is executed at the beginning of every tick.  Sometimes it is more natural or more efficient to instead say "have agent X execute procudure Y at time Z".  This is what discrete event scheduling (also know as "dynamic scheduling"") enables.

**When is discrete event scheduling useful?**

Discrete event scheduling is most useful for models where agents spend a lot of time sitting idle despite knowing when they need to act next. Sometimes in a netlogo model, you end up testing a certain condition or set of conditions for every agent every tick (usually in the form of an “ask”), just waiting for the time to be ripe.... this can get cumbersome and expensive.  In some models, you might know in advance exactly when a particular agent needs to act. Dynamic scheduling cuts out all of those superfluous tests.  The action is performed only when needed, with no condition testing and very little overhead.

For example, if an agent is a state machine and spends most of the time in the state “at rest” and has a predictable schedule that knows that the agent should transition to the state “awake” at tick 105, then using a dynamic scheduler allows you to avoid code that looks like: "if ticks = 105 \[ do-something \]", which has to be evaluated every tick!

## Installation

First, [download the latest version of the extension](https://github.com/colinsheppard/Time-Extension/tags). Note, the latest version of this extension was compiled against Netlogo 5.0.4, if you are using a different version of Netlogo you might consider building your own jar file ([see building section below](#building)).

Unzip the archive, and look under the lib directory, there should be two jar files:

* time.jar
* joda-time-2.2.jar

Rename the lib directory "time" and move it to the "extensions" directory inside your Netlogo application folder (i.e. [NETLOGO]/extensions/time/).  Or you can place the two jar files in the same directory as your netlogo model in which you want to use this extension.

For more information on Netlogo extensions:
[http://ccl.northwestern.edu/netlogo/docs/extensions.html](http://ccl.northwestern.edu/netlogo/docs/extensions.html)

## Examples

See the example models in the extension subfolder "examples" for a demonstration of usage.

## Data Types

The **time extension** introduces some new data types (more detail about these is provided in the [behavior section](#behavior)):

* **LogoTime** - A LogoTime object stores a time stamp, it can track a full date and time, or just a date (with no associated time).

* **LogoEvent** - A LogoEvent encapsulates a who, a what, and a when.  It allows you to define, for example, that you want turtle 7 to execute it's go-forward procedure at tick 10.

* **LogoSchedule** - A LogoSchedule object stores a sorted list of LogoEvents and manages the dispatch of those events.

## Behavior

The **time extension** has the following notable behavior:

* **LogoTimes can store DATETIMEs, DATEs, or DAYs** - A LogoTime is a flexible data structure that will represent your time data as one of three varieties depending on how you create the LogoTime object.  A LogoTime can be a DATETIME, a DATE, and a DAY:
  * A DATIME is a fully specified instant in time (e.g. January 2, 2000 at 3:04am and 5.678 seconds).
  * A DATE is a fully specified day in time but lacks any information about the time of day (e.g. January 2, 2000).
  * A DAY is a generic date that lacks a year (e.g. January 2).<br/>

  Depending on which variety of LogoTime you are storing, the behavior of the **time extension** primitives will vary.  For example, the difference between two DATETIMES will have millisecond resolution, while the difference between two DATES or two DAYS will only have resolution to the nearest whole day.

* **You create LogoTime objects by passing a string** - The time:create primitive was designed to both follow the standard used by joda-time, but also make date time parsing more convenient by allowing a wider range of delimiters and formats.  For example, the following are all valid DATETIME strings: 
  * "2000-01-02T03:04:05.678"
  * "2000-01-02 03:04:05"
  * "2000-01-02 03:04"
  * "2000-01-02 03"
  * "2000/01/02 03:04:05.678"
  * "2000-1-02 03:04:05.678"
  * "2000-01-2 03:04:05.678"
  * "2000-1-2 03:04:05.678"<br/>

  The following are all valid DATE strings:
  * "2000-01-02"
  * "2000-01-2"
  * "2000-1-02"<br/>

  The following are all valid DAY strings:
  * "01-02"
  * "01-2"
  * "1-02"<br/>

  Note, if you do not include a the time in your string, the **time extension** will assume you want a DATE, if you want a DATETIME that happens to be at midnight, specify the time as zeros: "2000-01-02 00:00".

* **Time extension recognizes "period types"** - In order to make it easy to specify a time duration like 2 "days" or 4 "weeks", the **time extension** will accept strings to specify a period type.  The following is a table of the period types and strings that **time** recognizes (note, any of these period type strings can be pluralized and are case **in**sensitive):
  
  | PERIOD TYPE | Valid string specifiers		|
  | ------------|-----------------------------------------|
  | YEAR	      | "year"					|
  | MONTH	      | "month"					|
  | WEEK	      | "week"					|
  | DAY	      | "day", "dayofmonth", "dom"		|
  | DAYOFYEAR   | "dayofyear", "doy", "julianday", "jday" |
  | DAYOFWEEK   | "dayofweek", "dow", "weekday", "wday"   |
  | HOUR	      | "hour"					|
  | MINUTE      | "minute"				|
  | SECOND      | "second"				|
  | MILLI	      | "milli"					|

* **Time extension has Millisecond Resolution** - This is a fundamental feature of Joda Time and cannot be changed.  The biggest reason Joda Time does not support micro or nano seconds is performance, going to that resolution would require the use of BigInts which would substantially slow down computations.  [Read more on this topic](http://joda-time.sourceforge.net/faq.html#submilli)

* **Daylight Savings is Ignored** - All times are treated as local, or "zoneless", and daylight savings time (DST) is ignored.  It is assumed that most Netlogo users don't need to convert times between time zones or be able to follow the rules of Daylight Savings for any particular locale.  Instead, users are much more likely to need the ability to load a time series and perform date and time operations without worrying about when DST starts and whether an hour of their time series will get skipped in the Spring or repeated in the Fall.  It should be noted that Joda Time definitely can handle DST for most locales on Earth, but that capability is not extended to Netlogo here and won't be unless by popular demand.

* **Leap Days are Included** - While we simplify things by excluding time zones and DST, leap days are kept to allow users to reliably use real world time series in their Netlogo model.

* **Decimal versus Whole Number Time Periods** - In this extension, decimal values can be used by the *plus* and *anchor-to-ticks* primitives for seconds, minutes, hours, days, and weeks (milliseconds can't be fractional because they are the base unit of time).  These units are treated as *durations* because they can unambiguously be converted from a decimal number to a whole number of milliseconds.  But there is ambiguity in how many milliseconds there are in 1 month or 1 year, so month and year increments are treated as *periods* which are by definition whole number valued. So if I use the *time:plus* primitive to add 1 month to the date "2012-02-02", I'll get "2012-03-02" and if I add another month I get "2012-04-02" even though those have varying days between them.  If you try to use a fractional number, it will be rounded to the nearest integer and then added. If you want to increment a time variable by one and a half 365-day years, then just increment by 1.5 * 365 days instead of 1.5 years.

* **LogoEvents are dispatched in order, ties go to the first created** - If multiple LogoEvents are scheduled for the exact same time, they are dispatched in the order in which they are added to the LogoSchedule.

* **LogoEvents can be created for an agentset** - When an agentset is scheduled to perform an task, the individual agents execute the procedure in a non-random order, which is different from *ask* which shuffles the agents.  Of note is that this is the only way I'm aware of to accomplish an unsorted *ask* in Netlogo while still allowing for the death and creation of agents during execution.  Based on some simple benchmarking it appears that not shuffling can produce a ~15% speedup in execution time.  To shuffle the order, use the *add-shuffled* primitive which will execute the actions in random order with low overhead.

* **LogoEvents won't break if an agent dies** - If an agent is scheduled to perform a task in the future but dies before the event is dispatched, the event will be silently skipped.

* **LogoEvents can be scheduled to occur at a LogoTime** - LogoTimes are acceptable alternative to specifying a tick number for the time when an event should occur, however the LogoSchedule must be "anchored" to a reference time so it knows , see *time:anchor-schedule** below for an example of anchoring.

## Primitives

**time:create**

*time:create time-string*

Reports a LogoTime based on parsing the *time-string* argument.  A LogoTime is a custom data type included with this extension, which is used to store time in the form of a DATETIME, a DATE, or a DAY.  All other primitives associated with this extension take one or more LogoTimes as as an argument.  See the "Behavior" section above for more information on the behavior of LogoTime objects. 

    ;; Create a datetime, a date, and a day
    let t-datetime time:create "2000-01-02 03:04:05.678"
    let t-date time:create "2000-01-02"
    let t-day time:create "01-02"

---------------------------------------

**time:show**

*time:show logotime string-format*

Reports a string containing the *logotime* formatted according the *string-format* argument. 
    
    let t-datetime time:create "2000-01-02 03:04:05.678"

    print time:show t-datetime "EEEE, MMMM d, yyyy"
    ;; prints "Sunday, January 2, 2000"

See the following link for a full description of the available format options:

[http://joda-time.sourceforge.net/api-release/org/joda/time/format/DateTimeFormat.html](http://joda-time.sourceforge.net/api-release/org/joda/time/format/DateTimeFormat.html)

---------------------------------------

**time:get**

*time:get period-type-string logotime*

Retrieves the numeric value from the *logotime* argument corresponding to the *period-type-string* argument.  For DATETIME's, all period types are valid, for DATE's only period types of a day or higher are valid, for DAY's the only valid period types are "day" and "month".

    let t-datetime (time:create "2000-01-02 03:04:05.678")

    print time:get "year" t-datetime
    ;;prints "2000"

    print time:get "month" t-datetime
    ;;prints "1"

    print time:get "dayofyear" t-datetime
    ;;prints "2"

    print time:get "hour" t-datetime
    ;;prints "3"

    print time:get "second" t-datetime
    ;;prints "5"

---------------------------------------

**time:plus**

*time:plus logotime number period-type-string*

Reports a LogoTime resulting from the addition of some time interval to the *logotime* argument.  The time interval to be added is specified by the *number* and *period-type-string* arguments.  Valid period types are YEAR, MONTH, WEEK, DAY, DAYOFYEAR, HOUR, MINUTE, SECOND, and MILLI. 

    let t-datetime (time:create "2000-01-02 03:04:05.678")
    
    ;; Add some interval to the datetime
    print time:plus t-datetime 1.0 "seconds"  
    ;; prints "{{time:logotime 2000-01-02 03:04:06.678}}"

    print time:plus t-datetime 1.0 "minutes"  
    ;; prints "{{time:logotime 2000-01-02 03:05:05.678}}"

    print time:plus t-datetime (60.0 * 24) "minutes"  
    ;; prints "{{time:logotime 2000-01-03 03:04:05.678}}"

    print time:plus t-datetime 1 "week"  
    ;; prints "{{time:logotime 2000-01-09 03:04:05.678}}"

    print time:plus t-datetime 1.0 "weeks"  
    ;; prints "{{time:logotime 2000-01-09 03:04:05.678}}"

    print time:plus t-datetime 1.0 "months"  
    ;; note that decimal months or years are rounded to the nearest whole number
    ;; prints "{{time:logotime 2000-02-02 03:04:05.678}}"

    print time:plus t-datetime 1.0 "years"   
    ;; prints "{{time:logotime 2001-01-02 03:04:05.678}}"


---------------------------------------

**time:is-before**<br/>
**time:is-after**<br/>
**time:is-equal**<br/>
**time:is-between**

*time:is-before logotime1 logotime2*<br/>
*time:is-after  logotime1 logotime2*<br/>
*time:is-equal  logotime1 logotime2*<br/>
*time:is-between  logotime1 logotime2 logotime3*

Reports a boolean for the test of whether *logotime1* is before/after/equal-to *logotime2*.  The is-between primitive returns true if *logotime1* is between *logotime2* and *logotime3*.  All LogoTime arguments must be of the same variety (DATETIME, DATE, or DAY). 

	print time:is-before (time:create "2000-01-02") (time:create "2000-01-03")
	;;prints "true"

  	print time:is-before (time:create "2000-01-03") (time:create "2000-01-02")
	;;prints "false"

  	print time:is-after  (time:create "2000-01-03") (time:create "2000-01-02")
	;;prints "true"

  	print time:is-equal  (time:create "2000-01-02") (time:create "2000-01-02")
	;;prints "true"

  	print time:is-equal  (time:create "2000-01-02") (time:create "2000-01-03")
	;;prints "false"

 	print time:is-between (time:create "2000-03-08")  (time:create "1999-12-02") (time:create "2000-05-03")
	;;prints "true"

---------------------------------------

**time:difference-between**

*time:difference-between logotime1 logotime2 period-type-string*

Reports the amount of time between *logotime1* and *logotime2* in units of *period-type-string*.  Note that if the period type is YEAR or MONTH, then the reported value will be a whole number based soley on the month and year components of the LogoTimes.

	print time:difference-between (time:create "2000-01-02 00:00") (time:create "2000-02-02 00:00") "days"
	;;prints "31"

  	print time:difference-between (time:create "2000-01-02") (time:create "2001-02-02") "days"
	;;prints "397"

  	print time:difference-between (time:create "01-02") (time:create "01-01") "hours"
	;;prints "-24"

	print time:difference-between (time:create "2000-01-02") (time:create "2000-02-15") "months"
	;;prints "1"

---------------------------------------

**time:anchor-to-ticks**

*time:anchor-to-ticks logotime number intervalstring*

Reports a new LogoTime object which is "anchored" to the native time tracking mechanism in Netlogo (i.e the value of *ticks*).  Once anchored, this LogoTime object will always hold the value of the current time as tracked by *ticks*.  Any of the three varieties of LogoTime can be achored to the tick, the time value of the logotime argument is assumed to be the time at tick zero.  The *number* and *intervalstring* arguments describe the worth of one tick (e.g. a tick can be worth 1 day or 2 hours or 90 seconds, etc.)

    set tick-datetime time:anchor-to-ticks (time:create "2000-01-02 03:04:05.678") 1 "hour"
    set tick-date time:anchor-to-ticks (time:create "2000-01-02") 2 "days"
    set tick-day time:anchor-to-ticks (time:create "01-02") 3 "months"

    reset-ticks
    tick
    print (word "tick " ticks)  ;; prints "tick 1" 
    print (word "tick-datetime " tick-datetime)  ;; prints "tick-dateime {{time:logotime 2000-01-02 04:04:05.678}}"
    print (word "tick-date " tick-date)  ;; prints "tick-datetime {{time:logotime 2000-01-04}}""
    print (word "tick-day " tick-day)  ;; prints "tick-day {{time:logotime 04-02}}""


    tick
    print (word "tick " ticks)  ;; prints "tick 2" 
    print (word "tick-datetime " tick-datetime)  ;; prints "tick-dateime {{time:logotime 2000-01-02 05:04:05.678}}"
    print (word "tick-date " tick-date)  ;; prints "tick-datetime {{time:logotime 2000-01-06}}""
    print (word "tick-day " tick-day)  ;; prints "tick-day {{time:logotime 07-02}}"" 

---------------------------------------


**time:create-schedule**

*time:create-schedule*

Reports a LogoSchedule, a custom data type included with this extension, which is used to store events and dispatch them.  All other primitives associated with discrete event scheduling take a LogoSchedule data type as the first argument, so it's usually necessary to store the schedule as a global variable.

    set logoschedule time:create-schedule

---------------------------------------

**time:anchor-schedule**

*time:anchor-schedule logoschedule logotime number intervalstring*

Anchors *logoschedule* to the native time tracking mechanism in Netlogo (i.e the value of *ticks*).  Once anchored, LogoTimes can be used for discrete event scheduleing (e.g. schedule agent 3 to perform some task on June 10, 2013).  The value of the *logotime* argument is assumed to be the time at tick zero.  The *number* and *intervalstring* arguments describe the worth of one tick (e.g. a tick can be worth 1 day, 2 hours, 90 seconds, etc.)

    time:anchor-schedule logoschedule time:create "2013-05-30" 1 "hour"

---------------------------------------

**time:add-event** 

*time:add-event logoschedule agent task tick-or-time*  
*time:add-event logoschedule agentset task tick-or-time*

Add an event to *logoschedule*.  The order events are added is not important, events will be dispatched in order of the time passed as the last argument. An *agent* or an *agentset* can be passed as the second argument along with a *task* as the third, which is executed by the agent(s) at *tick-or-time* (either a number indicating the tick or a LogoTime), which is a time greater than or equal to the present moment (*>= ticks*).  If *tick-or-time* is a LogoTime, then *logoschedule* must be anchored.

    time:add logoschedule turtles task go-forward 1.0

---------------------------------------

**time:add-event-shuffled** 

*time:add-event-shuffled logoschedule agent task tick-or-time*  
*time:add-event-shuffled logoschedule agentset task tick-or-time*

Add an event to *logoschedule* and shuffle the agentset during execution.  This is identical to *time:add* but the individuals in the agentset execute the action in random order.

    time:add-shuffled logoschedule turtles task go-forward 1.0

---------------------------------------

**time:repeat-event** 

*time:repeat-event logoschedule agent task tick-or-time repeat-interval-number*  
*time:repeat-event logoschedule agentset task tick-or-time-number repeat-interval-number*

Add a repeating event to *logoschedule*.  This behaves almost identical to *time:add-event* except after the event is dispatched it is immediately rescheduled *repeat-interval-number* ticks into the future using the same *agent*/*agentset* and *task*. 

    time:repeat-event logoschedule turtles task go-forward 2.5 1.0

---------------------------------------

**time:clear**

*time:clear logoschedule*

Clear all events from *logoschedule*.

    time:clear logoschedule

---------------------------------------

**time:go** 

*time:go logoschedule*

Dispatch all of the events in *logoschedule*.  It's important to note that this will continue until *logoschedule* is empty.  If repeating events are in *logoschedule* or if procedures in *logoschedule* end up scheduling new events, it's possible for this to become an infinite loop.  See the example model "DiscreteEventScheduling.nlogo" for an example of how to stop a LogoSchedule.

    time:go logoschedule

---------------------------------------

**time:go-until** 

*time:go-until logoschedule halt-tick-or-time*

Dispatch all of the events in *logoschedule* up until *halt-tick-or-time*.  If the temporal extent of your model is known in advance, this variant on *time:go* is the recommended way to dispatch your model.

    time:go-until logoschedule 100.0

---------------------------------------

**time:size-of** 

*time:size-of logoschedule*

Reports the number of events in the schedule.

    if time:size-of logoschedule > 0[
      time:go logoschedule
    ]

---------------------------------------

**time:ts-load** 

*time:ts-load filepath*

Loads time series data from a text file (comma or tab separated) and reports a LogoTimeSeries object.  The first line of the file is assumed to be a header line, the data in the LogoTimeSeries object is accessible by name.  Do not use "all" or "ALL" for a header name as this keyword is reserved (see time:ts-get below).  The first column of the file must be dates of datetimes that can be parsed by this extension (see the [behavior section](#behavior) for acceptable string formats).  Finally, if the date/time stamps do not appear in chronological order in the text file, they will be automatically sorted into order when loaded.

    let ts time:ts-load "time-series-data.csv"

---------------------------------------

**time:ts-get** 

*time:ts-get logotimeseries logotime column-name*

Reports the value from the *column-name* column of the *logotimeseries* in the row matching *logotime*.  If there is not an exact match with *logotime*, the row with the nearest date/time will be used.  If "ALL" or "all" is specified as the column name, then the entire row, including the logotime, is returned as a list.

    print time:ts-get ts (time:create "2000-01-01 10:00:00") "flow"
    ;; prints the value from the flow column in the row containing a time stamp of 2000-01-01 10:00:00

---------------------------------------

**time:ts-get-interp** 

*time:ts-get-interp logotimeseries logotime column-name*

Behaves almost identical to time:ts-get, but if there is not an exact match with the date/time stamp, then the value is linearly interpolated between the two nearest values.  This command will throw an exception if the values in the column are strings instead of numeric.  

    print time:ts-get-interp ts ()time:create "2000-01-01 10:30:00") "flow"

---------------------------------------

**time:ts-get-exact** 

*time:ts-get-exact logotimeseries logotime column-name*

Behaves almost identical to time:ts-get, but if there is not an exact match with the date/time stamp, then an exception is thrown.  

    print time:ts-get-exact ts (time:create "2000-01-01 10:30:00") "flow"

---------------------------------------

**time:ts-get-range** 

*time:ts-get-range logotimeseries logotime1 logotime2 column-name*

Reports a list of all of the values from the *column-name* column of the *logotimeseries* in the rows between *logotime1* and *logotime2* (inclusively).  If "ALL" or "all" is specified as the column name, then a list of lists is reported, with one sub-list for each column in *logotimeseries*, including the date/time column.

    print time:ts-get-range time-series time:create "2000-01-02 12:30:00" time:create "2000-01-03 00:30:00" "all"

## Building

Use the NETLOGO environment variable to tell the Makefile which NetLogoLite.jar to compile against.  For example:

    NETLOGO=/Applications/NetLogo\\\ 5.0 make

If compilation succeeds, `time.jar` will be created.  

## Author

Colin Sheppard

## Feedback? Bugs? Feature Requests?

Please visit the [github issue tracker](https://github.com/colinsheppard/Time-Extension/issues?state=open) to submit comments, bug reports, or requests for features.  I'm also more than willing to accept pull requests.

## Credits

This extension is in part powered by [Joda Time](http://joda-time.sourceforge.net/) and inspired by the [Ecoswarm Time Manager Library](http://www.humboldt.edu/ecomodel/software.htm).

## Terms of Use

[![CC0](http://i.creativecommons.org/p/zero/1.0/88x31.png)](http://creativecommons.org/publicdomain/zero/1.0/)

The NetLogo dynamic scheduler extension is in the public domain.  To the extent possible under law, Colin Sheppard has waived all copyright and related or neighboring rights.

