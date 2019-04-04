package org.nlogo.extensions.time.datatypes

object LogoEventComparator extends Ordering[LogoEvent] {
  def compare(a: LogoEvent, b: LogoEvent): Int =
    if (a.tick < b.tick) {
      -1
    } else if (a.tick > b.tick) {
      1
    } else if (a.id < b.id) {
      -1
    } else if (a.id > b.id) {
      1
    } else {
      0
    }
}
