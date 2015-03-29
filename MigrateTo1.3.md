# Setups

You need to set the Maven memory to avoid compile failure  
```shell
export MAVEN_OPTS="-Xmx512m -XX:MaxPermSize=128m"
```

# Code Style

## Column class

Since 1.3, Catalyst Expression is hidden from final user. A new class ```Column``` is created as a user interface. 
Internally, it is a wrapper around Expression. 

Before we use 2 types of Expressions in ```select```, ```groupBy``` etc., one is a Symbol, which refers to an original 
column of the Srdd, the other is a real Expression like ```Sqrt('a)```. When it was Symbol, with ```import sqlContext._```,
the Symbol is implicitly converted to analysis.UnresolvedAttribut, which is an Expression.

Since 1.3, an implicit conversion from Symbol to Column is provided by ```import sqlContext.implicits._```. However, 
both example code and convenience alternative methods are more toward using ```String``` to represent Column names instead 
of using Symbol. 
Also it specifically defined the "Symbol" or "String" represented column names as a class ```ColumnName``` which is a
subclass of ```Column```. 

Here are some examples:

Even without ```import sqlContext.implicits._```, you can do
```scala
df.select("field1")
```
The same thing can be written as
```scala
df.select(df("field1"))
```
Where ```df(...)``` literally look for the column withing ```df``` with name "field1" and return a ```Column```. 
You can also do
```scala
import df.sqlContext.implicts._
df.select($"field1")
```
The magic here is that there is an implicit class in ```sqlContext.implicits```, which extends 
[StringContext](http://www.scala-lang.org/files/archive/nightly/docs/library/index.html#scala.StringContext)

The problem is when to use which way to do things.

### Use String directly 
Although the original ```select``` taking ```Column```'s
```scala
def select(c: Column*)
```

One can use String in ```select``` directly, because ```select``` has a convenience alternative method which defined as
```scala
def select(s1: String, others: String*) 
```

As you can see to avoid ambiguity, instead of define ```select(s: String*)```, the String version actually split the 
parameter list. It provide the convenience as call it with explicit string parameters, it also caused a problem that we 
cant do this 
```scala
val keys=Seq("a", "b")
df.select(keys: _*) //will not work!
```

### Explicitly specify the dataframe for a column
```
df("a")
```
Is actually ```DataFrame.apply("a")```, literally search for column "a" in Dataframe "df". However sometimes we want to construct some general ```Column``` without
specifying any Dataframe. We need the next method to do so.

### General Column from String
```
$"a"
```
will create a general Column with name "a". Then how we use variables in this weird syntax? 

Actually ```$``` here is a method on ```StringContext```. An equivalent is ```s``` in ```s"dollar=${d}"```. With this analogy, 
we can figure out the way to use variables 
```
val name = "a"
$"$name"
```

Finally we have a way to select a List of fields
```scala
val keys=Seq("a", "b").map(l=>$"$l")
df.select(keys: _*)
```
 
### Symbol still works through implicit conversion
```scala
df.select('single * 2) 
```
will still work. 

However since it was through implicit conversion, the following doesn't work just like the string case:
```scala
val keys=Seq('a, 'b)
df.select(keys: _*) //doesn't work
``` 

You need to do
```scala
df.select(keys.map(l=>$"${l.name}"): _*)
```

### What "as" do

Here is what we do in the past to define new variables:
```scala
df.select('a * 2 as 'b)
```
It will still work in 1.3 as long as you ```import df.sqlContext.implicits._```. 

Here is what happened:

* The ```'a``` implicitly converted to a ColumnName (which is a Column)
* ```*``` is a method of ```Column``` which take 2 (as Any) as parameter 
* ```as``` is another method of ```Column```, which can take String or Symbol, 
```'b```, as parameter and return a ```Column```.

So basically if we consider ```as``` as an operator, left of it should be a ```Column``` 
and right of it should be either a ```Symbol``` or a ```String```.

To follow the 1.3 document examples, using String instead of Symbol to represent 
column names:
```scala
df.select($"a" * 2 as "b")
```

### No Expressions for the end users

As adding the ```Column``` class and refined the ```DataFrame``` interface, the end user interface is focused 
on only 2 concepts: 

* The DataFrame to hold the data, and
* The Column defines the element within a DataFrame
  
And

* ```select```, ```groupBy```, ```agg```, etc. are DataFrame methods.
* ```as```, ```+```, ```*```, ```substr```, ```in```, etc. are Column methods.

Other "Expressions" are interfaced through ```org.appache.spark.sql.functions```, such as

* ```avg```, ```sum```, ```abs```, ```sqrt```, etc.

For SMV, we should follow the same principle and provide interfaces through extending those 3 groups:

* DataFrameHelper (now SchemaRDDHelper) to extend DataFrame methods
* ColumnHelper (some of the nonaggregate functions now) to extend Column methods 
* package (or explicitly org.tresamigos.smv.functions._) to extend sql.functions