/*
 * Apache License
 * Version 2.0, January 2004
 * http://www.apache.org/licenses/
 *
 *    Copyright 2013 Aurelian Tutuianu
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */

package rapaio.data.mapping

import scala.collection.mutable

/**
 * User: Aurelian Tutuianu <padreati@yahoo.com>
 */
class Mapping {

  private var mapping = new mutable.MutableList[Int]

  def size: Int = mapping.size

  def apply(pos: Int): Int = {
    require(mapping.size > pos, "Value at pos " + pos + " does not exists")
    mapping(pos)
  }

  def add(pos: Int) {
    mapping += pos
  }

  def foreach[B](f: Int => B) = mapping.foreach[B](f)
}

object Mapping {
  def apply(list: List[Int]): Mapping = {
    val m = new Mapping
    m.mapping ++= list
    m
  }
}