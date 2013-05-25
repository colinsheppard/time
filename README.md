# NetLogo Time Extension

## What is it?

This package contains the NetLogo time extension, which provides NetLogo with a set of common date and time operations.  This extension is powered by the [Joda Time API for Java](http://joda-time.sourceforge.net/), and while this README trys to explain the subtle details of how dates and times are treated, it is recommended that you review at least the front page of Joda Time's website and preferably the "Key Concepts" topics in the navigation menu.  This documentation will make use of the terminology established by Joda Time (e.g. there's a meaningful difference between an *interval*, a *duration*, and a *period*.)

## Examples

See the example models in the extension subfolder "examples" for a demonstration of usage.

## Behavior

time has the following notable behavior:

* **It enables a new data type called a "logotime"** - A logotime is a flexible data structure that will represent your time data as one of three varieties depending on how you create the logotime object.  A logotime can be a DATETIME, a DATE, and a DAY:
  * A DATIME is a fully specified instant in time (e.g. January 2, 2000 at 3:04am and 5.678 seconds).
  * A DATE is a fully specified day in time but lacks any information about the time of day (e.g. January 2, 2000).
  * A DAY is a generic date that lacks a year (e.g. January 2).<br/>

  Depending on which variety of logotime you are storing, the behavior of the time extension primitives will vary.  For example, the difference between two DATETIMES will have millisecond resolution, while the difference between two DATES or two DAYS will only have resolution to the nearest whole day.

* **You create logotime objects by passing a string representation** - The time:create primitive was designed to both follow the standard used by joda-time, but also make date time parsing more convenient by allowing a wider range of delimiters and formats.  For example, the following are all valid DATETIME strings: 
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

* **Time recognizes "period types"** - In order to make it easy to specify a time duration like 2 "days" or 4 "weeks", the time extension will accept strings to specify a period type.  The following is the list of period types and strings that time recognizes (note, any of these period type strings can be pluralized and are case IN-sensitive):
  
  * YEAR: "year" 
  * MONTH: "month"
  * WEEK: "week"
  * DAY: "day", "dayofmonth", "dom"
  * DAYOFYEAR: "dayofyear", "doy", "julianday", "jday"
  * DAYOFWEEK: "dayofweek", "dow", "weekday", "wday"
  * HOUR: "hour"
  * MINUTE: "minute"
  * SECOND: "second"
  * MILLI: "milli"<br/>

* **Time has Millisecond Resolution** - This is a fundamental feature of Joda Time and cannot be changed.  The biggest reason Joda Time does not support micro or nano seconds is performance, going to that resolution would require the use of BigInts which would substantially slow down computations.  [Read more on this topic](http://joda-time.sourceforge.net/faq.html#submilli)

* **Daylight Savings is Ignored** - All times are treated as local, or "zoneless", and daylight savings time (DST) is ignored.  It is assumed that most Netlogo users don't need to convert times between time zones or be able to follow the rules of Daylight Savings for any particular locale.  Instead, users are much more likely to need the ability to load a data time series and perform date and time operations without worrying about when DST starts and whether an hour of their time series will get skipped in the Spring or repeated in the Fall.  It should be noted that Joda Time definitely can handle DST for most locales on Earth, but that capability is not extended to Netlogo here and won't be unless by popular demand.

* **Leap Days are Included** - While we simplify things by excluding time zones and DST, leap days are kept to allow users to reliably use real world time series in their Netlogo model.

* **Decimal versus Whole Number Time Periods** - In this extension, decimal values can be used by the *plus* and *anchor-to-ticks* primitives for seconds, minutes, hours, days, and weeks (milliseconds can't be fractional because they are the base unit of time).  These units are treated as *durations* because they can unambiguously be converted from a decimal number to a whole number of milliseconds.  But there is ambiguity in how many milliseconds there are in 1 month or 1 year, so month and year increments are treated as *periods* which are by definition whole number valued. So if I use the *time:plus* primitive to add 1 month to the date "2012-02-02", I'll get "2012-03-02" and if I add another month I get "2012-04-02" even though those have varying days between them.  If you try to use a fractional number, it will be rounded to the nearest integer and then added. If you want to increment a time variable by one and a half 365-day years, then just increment by 1.5 * 365 days instead of 1.5 years.

## Primitives


**time:create**

*time:create time-string*

Reports a logotime based on parsing the *time-string* argument.  A logotime is a custom data type included with this extension, which is used to store time in the form of a DATETIME, a DATE, or a DAY.  All other primitives associated with this extension take one or more logotimes as as an argument.  See the "Behavior" section above for more information on the behavior of logotime objects. 

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

Reports a logotime resulting from the addition of some time interval to the *logotime* argument.  The time interval to be added is specified by the *number* and *period-type-string* arguments.  Valid period types are YEAR, MONTH, WEEK, DAY, DAYOFYEAR, HOUR, MINUTE, SECOND, and MILLI. 

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

**time:anchor-to-ticks**

*time:anchor-to-ticks logotime number intervalstring*

Reports a new logotime object which is "anchored" to the native time tracking mechanism in Netlogo (i.e the value of *ticks*).  Once anchored, this logotime object will always hold the value of the current time as tracked by *ticks*.  Any of the three varieties of logotime can be achored to the tick, the time value of the logotime argument is assumed to be the time at tick zero.  The *number* and *intervalstring* arguments describe the worth of one tick (e.g. a tick can be worth 1 day, 2 hours, 90 seconds, etc.)

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




## Installation

Two jars need to be present in the same directory as your Netlogo model or in the extensions directory of your Netlogo application (i.e. [NETLOGO]/extensions/time/):

* time.jar
* joda-time-2.2.jar

Both of these files are included under the lib directory of this repository.  The time.jar file has been compiled against NetLogo 5.0.4, if you are using a different version of netlogo then it is recommended you compile the extension yourself (see "Building" below).

For more information on Netlogo extensions:
[http://ccl.northwestern.edu/netlogo/docs/extensions.html](http://ccl.northwestern.edu/netlogo/docs/extensions.html)

## Building

Use the NETLOGO environment variable to tell the Makefile which NetLogoLite.jar to compile against.  For example:

    NETLOGO=/Applications/NetLogo\\\ 5.0 make

If compilation succeeds, `time.jar` will be created.  

## Author

Colin Sheppard

## Feedback? Bugs? Feature Requests?

Please visit the [github issue tracker](https://github.com/colinsheppard/Time-Extension/issues?state=open) to submit comments, bug reports, or requests for features.  I'm also more than willing to accept pull requests.

## Credits

This extension is powered by [Joda Time](http://joda-time.sourceforge.net/) and inspired by the [Ecoswarm Time Manager Library](http://www.humboldt.edu/ecomodel/software.htm).

## Terms of Use

[![CC0](http://i.creativecommons.org/p/zero/1.0/88x31.png)](http://creativecommons.org/publicdomain/zero/1.0/)

The NetLogo dynamic scheduler extension is in the public domain.  To the extent possible under law, Colin Sheppard has waived all copyright and related or neighboring rights.

