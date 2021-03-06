# Spark Model Variables (SMV)

Spark Model Variables enables users to quickly build model variables on Apache Spark platform.

## Build
To build this package, use maven as follows:
```shell
$ mvn clean install
```
You must use maven version 3.0.4 or newer to build this project. In some system, instead of comand `mvn`, you may need to use `mvn3`.

## Run Spark Shell with SMV

We can pre-load SMV jar when run spark-shell. 

```shell
$ ADD_JARS=/path/to/smv/smv-1.0-SNAPSHOT.jar spark-shell -i sparkshellinclude.scala
```
where `sparkshellinclude.scala` will be loaded for convenience. It could look like the follows,

```scala
import org.apache.spark.sql.SQLContext
import org.tresamigos.smv._
val sqlContext = new SQLContext(sc)
import sqlContext._
import org.apache.spark.sql.catalyst.expressions._
```

Or you can use the existing script under ```shell``` directory.
```shell
./shell/run.sh
```
You may need to modify the script a little for your own environment.

You can put utility functions for the interactive shell in the ```shell_init.scala``` file. 

## Full example for ad hoc data discovery

```scala
val sqlContext = new SQLContext(sc)
import sqlContext._

val srdd = sqlContext.csvFileWithSchema("/data/input", "/data/input.schema")
val mini_srdd = srdd.select('tx_id, 'tx_amt, 'tx_date, 'tx_type)

// create EDD base tasks (see description under EDD section below)
val edd = mini_srdd.edd.addBaseTasks()

// add histogram to enumerated "amount" types.
edd.addAmountHistogramTasks('tx_amt)

// add generic histogram calcuation for given fields.
edd.addHistogramTasks('tx_date, 'tx_type)

// generate the histogram and save it.
edd.createReport.saveAsFile(outreport)
```

## Ad Hoc Data Discovery VS. App Development 
The nature of Data Application development is the circle of Data Discovery -> Variable Coding -> Data Discovery. 
SMV is designed to support seamlessly switch between those 2 modes. 

In a nutshell, SMV extends Spark Shell with additional function and components to provide an interactive environment 
for ad hoc Data Discovery; Also the SMV provides an [Application Framework](docs/appFramework.md) for easier Variable 
Development with Spark (and Scala).

With the SmvApp framework and all the additional functions is to make user's experience closer to solve the data problem 
with minimal Spark/Scala programing knowledge and skills. 

## [Application Framework](docs/appFramework.md)

## Smv Functions
* [CSV Handling](docs/csvHandling.md)
* [Extended Data Dictionary](docs/Edd.md)
* [Column Helper functions](docs/ColumnFunctions.md)
* [DataFrame Helper functions](docs/DF_Functions.md)
* [DQM - Experimental](docs/Dqm.md)

## Migrate to Spark 1.3
Since Spark 1.3.0, there quite some interface changes on SparkSQL. Please refer [Migrate to Spark 1.3](docs/MigrateTo1.3.md) for details.