package org.nlogo.extensions.time.datatypes

import java.util.Comparator
import org.nlogo.extensions.time._

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
