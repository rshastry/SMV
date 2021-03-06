/*
 * This file is licensed under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.tresamigos.smv

// TODO: Still need to add more test cases mainly type promotion.
class SchemaDiscoveryTest extends SparkTestUtil {
  sparkTest("Test schema discovery 1 line header") {
    val strRDD = sqlContext.sparkContext.textFile(testDataDir +  "SchemaDiscoveryTest/test1.csv")
    val schema = sqlContext.discoverSchema(strRDD,10, CsvAttributes.defaultCsvWithHeader)
    val entries = schema.entries

    assert(entries.length === 8)

    assert(entries(0).structField.name === "id")
    assert(entries(0).typeName === "Integer")
    assert(entries(1).structField.name === "name")
    assert(entries(1).typeName === "String")
    assert(entries(2).structField.name === "age")
    assert(entries(2).typeName === "Integer")
    assert(entries(3).structField.name === "weight")
    assert(entries(3).typeName === "Float")
    assert(entries(4).structField.name === "active")
    assert(entries(4).typeName === "Boolean")
    assert(entries(5).structField.name === "address")
    assert(entries(5).typeName === "String")
    assert(entries(6).structField.name === "registration_date")
    assert(entries(6).typeName === "Timestamp")
    assert(entries(7).structField.name === "last_active_date")
    assert(entries(7).typeName === "Timestamp")
  }

  sparkTest("Test schema discovery no header") {
    val strRDD = sqlContext.sparkContext.textFile(testDataDir +  "SchemaDiscoveryTest/test2.csv")
    val schema = sqlContext.discoverSchema(strRDD,10, CsvAttributes())
    val entries = schema.entries

    assert(entries.length === 5)

    assert(entries(0).structField.name === "f1")
    assert(entries(0).typeName === "Integer")
    assert(entries(1).structField.name === "f2")
    assert(entries(1).typeName === "String")
    assert(entries(2).structField.name === "f3")
    assert(entries(2).typeName === "Integer")
    assert(entries(3).structField.name === "f4")
    assert(entries(3).typeName === "Float")
    assert(entries(4).structField.name === "f5")
    assert(entries(4).typeName === "Boolean")
  }

  sparkTest("Test schema discovery type promotion") {
    val strRDD = sqlContext.sparkContext.textFile(testDataDir +  "SchemaDiscoveryTest/test3.csv")
    val schema = sqlContext.discoverSchema(strRDD,10, CsvAttributes.defaultCsvWithHeader)
    val entries = schema.entries

    assert(entries.length === 6)

    assert(entries(0).structField.name === "id")
    assert(entries(0).typeName === "Long")
    assert(entries(1).structField.name === "name")
    assert(entries(1).typeName === "String")
    assert(entries(2).structField.name === "age")
    assert(entries(2).typeName === "Integer")
    assert(entries(3).structField.name === "salary")
    assert(entries(3).typeName === "Float")
    assert(entries(4).structField.name === "active")
    assert(entries(4).typeName === "String")
    assert(entries(5).structField.name === "last_active_date")
    assert(entries(5).typeName === "String")
  }
}

