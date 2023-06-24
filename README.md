# graalvm-javalin-demo

#### Environment Tested

- JDK: Oracle GraalVM 20.0.1+9.1 (build 20.0.1+9-jvmci-23.0-b12)
- OS: MacOS 13.4.1 Ventura
- Hardware: Apple M1 Max

#### Build the native image

From this directory do the following in a terminal.
1. Build fat jar <br>
  ```mvn clean package```
2. Start application with tracing agent <br>
  ```java -agentlib:native-image-agent=config-output-dir=target/META-INF/native-image --enable-preview -jar target/graalvm-demo-1.0-SNAPSHOT-jar-with-dependencies.jar```
3. Then manually hit ```http://localhost:7070``` in your browser to trigger all execution paths for the tracing agent to pick up.
4. Stop the application.
5. Build native image <br>
  ```native-image -jar target/graalvm-demo-1.0-SNAPSHOT-jar-with-dependencies.jar -H:ConfigurationFileDirectories=target/META-INF/native-image -o target/app --no-fallback --enable-preview```
6. Start native image <br>
  ```./target/app```

#### Problem
The following exception is thrown on native image startup: <br>
```java.lang.NoSuchMethodError: java.lang.Thread$Builder$OfVirtual.unstarted(java.lang.Runnable)```

It seems the tracing agent fails to correctly pick up a reflective call done by Javalin's ```ReflectiveVirtualThreadBuilder``` [located here](https://github.com/javalin/javalin/blob/master/javalin/src/main/java/io/javalin/util/ConcurrencyUtil.kt#L100).
