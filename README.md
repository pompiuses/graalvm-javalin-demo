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
The following exception is thrown on native image startup (full stacktrace below): <br>
```Exception in thread "main" java.lang.NoSuchMethodError: java.lang.Thread$Builder$OfVirtual.unstarted(java.lang.Runnable)```

It seems the tracing agent fails to correctly pick up a reflective call done by Javalin's [ConcurrencyUtil.kt](https://github.com/javalin/javalin/blob/master/javalin/src/main/java/io/javalin/util/ConcurrencyUtil.kt#L100).

The tracing agent has, seemingly correctly, added the following to ```reflect-config.json```, but to no effect.
```
{
  "name":"java.lang.Thread$Builder$OfVirtual",
  "methods":[{"name":"name","parameterTypes":["java.lang.String"] }, {"name":"unstarted","parameterTypes":["java.lang.Runnable"] }]
}
```

Full stacktrace:
```
Exception in thread "main" java.lang.NoSuchMethodError: java.lang.Thread$Builder$OfVirtual.unstarted(java.lang.Runnable)
	at org.graalvm.nativeimage.builder/com.oracle.svm.core.methodhandles.Util_java_lang_invoke_MethodHandleNatives.resolve(Target_java_lang_invoke_MethodHandleNatives.java:345)
	at java.base@20.0.1/java.lang.invoke.MethodHandleNatives.resolve(MethodHandleNatives.java:199)
	at org.graalvm.nativeimage.builder/com.oracle.svm.core.methodhandles.Util_java_lang_invoke_MethodHandle.invokeInternal(Target_java_lang_invoke_MethodHandle.java:137)
	at java.base@20.0.1/java.lang.invoke.MethodHandle.invokeBasic(MethodHandle.java:76)
	at org.graalvm.nativeimage.builder/com.oracle.svm.core.methodhandles.MethodHandleIntrinsicImpl.execute(MethodHandleIntrinsicImpl.java:181)
	at org.graalvm.nativeimage.builder/com.oracle.svm.core.methodhandles.Util_java_lang_invoke_MethodHandle.invokeInternal(Target_java_lang_invoke_MethodHandle.java:142)
	at java.base@20.0.1/java.lang.invoke.MethodHandle.invokeBasic(MethodHandle.java:76)
	at java.base@20.0.1/java.lang.invoke.LambdaForm$NamedFunction.invokeWithArguments(LambdaForm.java:96)
	at java.base@20.0.1/java.lang.invoke.LambdaForm.interpretName(LambdaForm.java:949)
	at java.base@20.0.1/java.lang.invoke.LambdaForm.interpretWithArguments(LambdaForm.java:926)
	at java.base@20.0.1/java.lang.invoke.MethodHandle.invokeBasic(MethodHandle.java:82)
	at java.base@20.0.1/java.lang.invoke.MethodHandle.invokeBasic(MethodHandle.java:0)
	at java.base@20.0.1/java.lang.invoke.Invokers$Holder.invoke_MT(Invokers$Holder)
	at io.javalin.util.ReflectiveVirtualThreadBuilder.unstarted(ConcurrencyUtil.kt:117)
	at io.javalin.util.NamedVirtualThreadFactory.newThread(ConcurrencyUtil.kt:91)
	at java.base@20.0.1/java.util.concurrent.ThreadPerTaskExecutor.newThread(ThreadPerTaskExecutor.java:219)
	at java.base@20.0.1/java.util.concurrent.ThreadPerTaskExecutor$ThreadBoundFuture.<init>(ThreadPerTaskExecutor.java:337)
	at java.base@20.0.1/java.util.concurrent.ThreadPerTaskExecutor.submit(ThreadPerTaskExecutor.java:285)
	at java.base@20.0.1/java.util.concurrent.ThreadPerTaskExecutor.submit(ThreadPerTaskExecutor.java:293)
	at io.javalin.util.LoomUtil$LoomThreadPool.execute(ConcurrencyUtil.kt:63)
	at org.eclipse.jetty.io.SelectorManager.execute(SelectorManager.java:139)
	at org.eclipse.jetty.io.ManagedSelector.doStart(ManagedSelector.java:119)
	at org.eclipse.jetty.util.component.AbstractLifeCycle.start(AbstractLifeCycle.java:93)
	at org.eclipse.jetty.util.component.ContainerLifeCycle.start(ContainerLifeCycle.java:171)
	at org.eclipse.jetty.util.component.ContainerLifeCycle.doStart(ContainerLifeCycle.java:121)
	at org.eclipse.jetty.io.SelectorManager.doStart(SelectorManager.java:239)
	at org.eclipse.jetty.util.component.AbstractLifeCycle.start(AbstractLifeCycle.java:93)
	at org.eclipse.jetty.util.component.ContainerLifeCycle.start(ContainerLifeCycle.java:171)
	at org.eclipse.jetty.util.component.ContainerLifeCycle.doStart(ContainerLifeCycle.java:114)
	at org.eclipse.jetty.server.AbstractConnector.doStart(AbstractConnector.java:367)
	at org.eclipse.jetty.server.AbstractNetworkConnector.doStart(AbstractNetworkConnector.java:75)
	at org.eclipse.jetty.server.ServerConnector.doStart(ServerConnector.java:228)
	at org.eclipse.jetty.util.component.AbstractLifeCycle.start(AbstractLifeCycle.java:93)
	at org.eclipse.jetty.server.Server.doStart(Server.java:428)
	at org.eclipse.jetty.util.component.AbstractLifeCycle.start(AbstractLifeCycle.java:93)
	at io.javalin.jetty.JettyServer.start(JettyServer.kt:82)
	at io.javalin.Javalin.start(Javalin.java:171)
	at io.javalin.Javalin.start(Javalin.java:148)
	at demo.App.main(App.java:9)
```
