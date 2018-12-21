package org.nlogo.extensions.time.datatypes

import java.util.Comparator


class LogoTimeComparator extends Comparator[LogoTime] {
  def compare(a: LogoTime, b: LogoTime): Int = a.compareTo(b)
}
