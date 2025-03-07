/*
 * Copyright 2020 Crown Copyright (Office for National Statistics)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package csvwcheck.models

import csvwcheck.models.Values.KeyValue

case class KeyValueWithContext(
                                rowNumber: Long,
                                keyValue: KeyValue,
                                var isDuplicate: Boolean = false
                         ) {

  /**
    * KeyWithContext object holds the rowNumber information for setting better errors.
    * Thus `rowNumber` attribute doesn't need to be considered when checking the equality of 2 KeyWithContext objects
    * or for the hashCode of the KeyWithContext object.
    */
  override def equals(obj: Any): Boolean =
    obj != null &&
      obj.isInstanceOf[KeyValueWithContext] &&
      this.keyValue.equals(obj.asInstanceOf[KeyValueWithContext].keyValue)

  override def hashCode(): Int = this.keyValue.hashCode()

  def keyValuesToString(): String = {
    val stringList = keyValue.map {
      case listOfAny: List[Any] =>
        listOfAny.map(s => s.toString).mkString(",")
      case i => i.toString
    }
    stringList.mkString(",")
  }

}
