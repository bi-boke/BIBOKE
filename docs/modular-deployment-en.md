# How to deploy java-bok after modularization

After modularization, java-bok is launched via shell script instead of typing command: `java -jar FullNode.jar`.

*`java -jar FullNode.jar` still works, but will be deprecated in future*.

## Download

```
git clone git@github.com:unprotocol/java-bok.git
```

## Compile

Change to project directory and run:
```
./gradlew build
```
java-bok-1.0.0.zip will be generated in java-bok/build/distributions after compilation.

## Unzip

Unzip java-bok-1.0.0.zip
```
cd java-bok/build/distributions
unzip -o java-bok-1.0.0.zip
```
After unzip, two directories will be generated in java-bok: `bin` and `lib`, shell scripts are located in `bin`, jars are located in `lib`.

## Startup

Use the corresponding script to start java-bok according to the OS type, use `*.bat` on Windows, Linux demo is as below:
```
# default
java-bok-1.0.0/bin/FullNode

# using config file, there are some demo configs in java-bok/framework/build/resources
java-bok-1.0.0/bin/FullNode -c config.conf

# when startup with SR mode，add parameter: -w
java-bok-1.0.0/bin/FullNode -c config.conf -w
```

## JVM configuration

JVM options can also be specified, located in `bin/java-bok.vmoptions`:
```
# demo
-XX:+UseConcMarkSweepGC
-XX:+PrintGCDetails
-Xloggc:./gc.log
-XX:+PrintGCDateStamps
-XX:+CMSParallelRemarkEnabled
-XX:ReservedCodeCacheSize=256m
-XX:+CMSScavengeBeforeRemark
```