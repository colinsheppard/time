package org.nlogo.extensions.time.datatypes

import java.util.ArrayList
import org.nlogo.extensions.time._

class TimeSeriesColumn() {
  var data: List[String] = List[String]()

  def add(value: String): Unit = {
      this.data = this.data :+ value
  }
}
