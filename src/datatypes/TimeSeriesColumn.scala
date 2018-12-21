package org.nlogo.extensions.time.datatypes

import java.util.ArrayList
import org.nlogo.extensions.time._

class TimeSeriesColumn() {
  var dataType: DataType = _
  var data: ArrayList[AnyRef] = _
//  var dataString: ArrayList[String] = _
//  var dataDouble: ArrayList[Double] = _
  def add(value: String): Unit = {
    if (this.dataType == null) {
      try {
        java.lang.Double.parseDouble(value)
        this.dataType = DoubleData
        this.data = new ArrayList[AnyRef]()
        this.data.add(java.lang.Double.parseDouble(value).asInstanceOf[AnyRef])
      } catch {
        case e3: Exception => {
          this.dataType = StringData
          this.data = new ArrayList[AnyRef]()
          this.data.add(value)
        }
      }
    } else {
      dataType match {
        case DoubleData => this.data.add(java.lang.Double.parseDouble(value).asInstanceOf[AnyRef])
        case StringData => this.data.add(value)
        case _ =>
      }
    }
  }
}
