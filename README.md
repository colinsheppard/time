Time Zone List: http://joda-time.sourceforge.net/timezones.html

# NetLogo Time Extension

## What is it?

This package contains the NetLogo time extension, which provides NetLogo with a set of common date and time operations.  This extension is powered by the [Joda Time API for Java](http://joda-time.sourceforge.net/), and while this README trys to explain the subtle details of how dates and times are treated, it is recommended that you review at least the front page of Joda Time's website and preferably the "Key Concepts" topics in the navigation menu.  This documentation will make use of the terminology established by Joda Time (e.g. there's a meaningful difference between an *interval*, a *duration*, and a *period*.)

## Examples

See the example models in the extension subfolder "examples" for a demonstration of usage.

## Behavior

time has the following notable behavior:

* **Time has Millisecond Resolution** - This is a fundamental feature of Joda Time and cannot be changed.  The biggest reason Joda Time does not support micro or nano seconds is performance, going to that resolution would require the use of BigInts which would substantially slow down computations.  [Read more on this topic](http://joda-time.sourceforge.net/faq.html#submilli)

* **Daylight Savings is Ignored** - All times are treated as local, or "zoneless", and daylight savings time (DST) is ignored.  It is assumed that most Netlogo users don't need to convert times between time zones or be able to follow the rules of Daylight Savings for any particular locale.  Instead, users are much more likely to need the ability to load a data time series and perform date and time operations without worrying about when DST starts and whether an hour of their time series will get skipped in the Spring or repeated in the Fall.  It should be noted that Joda Time definitely can handle DST for most locales on Earth, but that capability is not extended to Netlogo here and won't be unless by popular demand.

* **Leap Days are Included** - While we simplify things by excluding time zones and DST, leap days are kept to allow users to reliably use real world time series in their Netlogo model.

* **Decimal versus Whole Number Time Periods** - In this extension, decimal values can be used by the *plus* and *anchor-to-ticks* primitives for seconds, minutes, hours, days, and weeks.  But because of the ambiguity in what 1 month or year is (due to the varying number of days in a month or a year), decimal values used for months and years are rounded to the nearest whole number.  If you want to increment a time variable by one 365-day year, then just increment by 365 days.

## Primitives

Coming soon.

## Building

Use the NETLOGO environment variable to tell the Makefile which NetLogoLite.jar to compile against.  For example:

    NETLOGO=/Applications/NetLogo\\\ 5.0 make

If compilation succeeds, `time.jar` will be created.

## Author

Colin Sheppard

## Feedback? Bugs? Feature Requests?

Please visit the [github issue tracker](https://github.com/colinsheppard/Time-Extension/issues?state=open) to submit comments, bug reports, or requests for features.  I'm also more than willing to accept pull requests.

## Credits

This extension is powered by [Joda Time](http://joda-time.sourceforge.net/) but inspired by the [Ecoswarm Time Manager Library](http://www.humboldt.edu/ecomodel/software.htm).

## Terms of Use

[![CC0](http://i.creativecommons.org/p/zero/1.0/88x31.png)](http://creativecommons.org/publicdomain/zero/1.0/)

The NetLogo dynamic scheduler extension is in the public domain.  To the extent possible under law, Colin Sheppard has waived all copyright and related or neighboring rights.

