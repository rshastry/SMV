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

import org.apache.spark.sql.catalyst.expressions._
import org.apache.spark.sql.types._
import org.apache.spark.sql.functions._

class SmvCDSTest extends SparkTestUtil {

  sparkTest("Test RunSum") {
    val ssc = sqlContext; import ssc.implicits._
    val srdd = createSchemaRdd("k:String; t:Integer; v:Double", "z,1,0.2;z,2,1.4;z,5,2.2;a,1,0.3;")

    val res = srdd.smvGroupBy('k).
      smvApplyCDS(TimeInLastN("t", 3)).
      agg(first('k) as 'k, first('t) as 't, sum('v) as 'nv1, count('v) as 'nv2)
      
    assertSrddSchemaEqual(res, "k: String; t: Integer; nv1: Double; nv2: Long")
    assertUnorderedSeqEqual(res.collect.map(_.toString), Seq(
      "[a,1,0.3,1]",
      "[z,1,0.2,1]",
      "[z,2,1.5999999999999999,2]",
      "[z,5,2.2,1]"))
  }
  
  sparkTest("Test Anchor RunSum") {
    val ssc = sqlContext; import ssc.implicits._
    val srdd = createSchemaRdd("k:String; t:Integer; v:Double", "z,1,0.2;z,2,1.4;z,5,2.2;a,1,0.3;")

    val res = srdd.smvGroupBy('k).
      smvApplyCDS(TimeInLastNFromAnchors("at", $"t", Seq((3, (1, 4)), (4, (2, 5))))).
      agg(first('k) as 'k, first('at) as 'at, sum('v) as 'nv1, count('v) as 'nv2)
      
    assertSrddSchemaEqual(res, "k: String; at: Integer; nv1: Double; nv2: Long")
    assertUnorderedSeqEqual(res.collect.map(_.toString), Seq(
      "[a,3,0.3,1]",
      "[z,3,1.5999999999999999,2]",
      "[z,4,1.4,1]"))
  }
  
  sparkTest("Test out of order CDS keys") {
    val ssc = sqlContext; import ssc.implicits._
    val srdd = createSchemaRdd("time_type:String;v:String;time_value:Integer",
      "k1,a,10;k1,b,100;k2,d,3;k2,c,12")

    val f = udf({in:Int => in - 3})
    val cds = SmvCDSRange(
      Seq("time_type", "time_value"),
      ('_time_value > f('time_value) && ('_time_value <= 'time_value))
    )
    val s2 = srdd.selectMinus('time_type).selectPlus(lit("MONTHR3") as 'time_type)
    val res = s2.smvGroupBy('v).smvApplyCDS(cds).agg(first('v) as 'v, first('time_type) as 'time_type, first('time_value) as 'time_value, count('v) as 'cv)
    assertSrddSchemaEqual(res, "v: String; time_type: String; time_value: Integer; cv: Long")
    assertUnorderedSeqEqual(res.collect.map(_.toString), Seq(
      "[d,MONTHR3,3,1]",
      "[c,MONTHR3,12,1]",
      "[a,MONTHR3,10,1]",
      "[b,MONTHR3,100,1]"))
  }

  sparkTest("Test SmvCDSTopNRecs") {
    val ssc = sqlContext; import ssc.implicits._
    val srdd = sqlContext.createSchemaRdd("k:String; t:Integer; v:Double",
      """z,1,0.2;
         z,5,2.2;
         z,-5,0.8;
         z,3,1.1;
         z,2,1.4;
         z,,3.0;
         a,1,0.3""")

    // test TopN (with descending ordering)
    val t_cds = SmvCDSTopNRecs(2, 't.desc)
    val t_res = srdd.smvGroupBy('k).smvApplyCDS(t_cds).toDF

    assertSrddSchemaEqual(t_res, "k: String; t: Integer; v: Double")
    assertUnorderedSeqEqual(t_res.collect.map(_.toString), Seq(
      "[a,1,0.3]",
      "[z,3,1.1]",
      "[z,5,2.2]"))

    // test BottomN (using ascending ordering)
    val b_cds = SmvCDSTopNRecs(2, 't.asc)
    val b_res = srdd.smvGroupBy('k).smvApplyCDS(b_cds).toDF

    assertSrddSchemaEqual(b_res, "k: String; t: Integer; v: Double")
    assertUnorderedSeqEqual(b_res.collect.map(_.toString), Seq(
      "[a,1,0.3]",
      "[z,1,0.2]",
      "[z,-5,0.8]"))
  }
  
  sparkTest("Test SmvCDSTopNRecs multiple orderings") {
    val ssc = sqlContext; import ssc.implicits._
    val srdd = sqlContext.createSchemaRdd("k:String; t:Integer; v:Double",
      """z,1,0.2;
         z,5,2.2;
         z,2,1.4;
         z,2,3.0;
         a,1,0.3""")
             // test TopN (with descending ordering)
             
    val tv_cds = SmvCDSTopNRecs(2, 't.desc, 'v.asc)
    val res = srdd.smvGroupBy('k).smvApplyCDS(tv_cds).toDF

    assertSrddSchemaEqual(res, "k: String; t: Integer; v: Double")
    assertUnorderedSeqEqual(res.collect.map(_.toString), Seq(
      "[a,1,0.3]",
      "[z,2,1.4]",
      "[z,5,2.2]"))
  }
}