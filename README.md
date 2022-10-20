# reels
<a href="https://github.com/davidmoten/reels/actions/workflows/ci.yml"><img src="https://github.com/davidmoten/reels/actions/workflows/ci.yml/badge.svg"/></a><br/>
[![codecov](https://codecov.io/gh/davidmoten/reels/branch/master/graph/badge.svg)](https//:codecov.io/gh/davidmoten/reels)<br/>
[![Maven Central](https://maven-badges.herokuapp.com/maven-central/au.gov.amsa/reels/badge.svg?style=flat)](https://maven-badges.herokuapp.com/maven-central/au.gov.amsa/reels)<br/>

Actor framework for Java, non-blocking, performant. Developed as a reaction to the Akka licence change. Akka is a huge framework with a large number of extensions including persistence, web serving, streaming and more. My needs are limited to core of Akka, namely in-memory actor support (including supervision) and that's what this library provides.

Why would you trust this library with plenty of concurrency-sensitive code? I've been involved in concurrency-sensitive projects since 2014 as a frequent contributor to RxJava 1.x and the author of numerous popular RxJava libraries (rxjava-extras, rxjava-jdbc, rxjava-pool, rxjava-slf4j, rxjava-http, rxjava-aws). All these libraries involve a lot of concurrency-aware and performance-aware work so this is not new ground for me. Please raise an issue if you spot something.

Why do we need another Actor library on the JVM? I think there's space for an in-memory Actor library that has a small footprint (only *slf4j-api* as a logging dependency and jar is 87K), is easy to use (the API is both simple and discoverable), offers a goodly number of useful features, and is thread-safe and performant.

## Features
* Discoverable concise API (Akka is not, partly due to the way it's evolved and a lot of Scala library stuff)
* Custom supervisors including the ability to retry processing later and/or restart the actor (recreate the Actor object)
* Parent-child actor hierarchies
* Dead letter actor
* SLF4J logging (add the implementation that you like)
* Akka stop semantics (stopping an actor stops its children first)
* Jar size 87K with only one logging dependency (slf4j)
 
TODO 
* Lifecycle monitoring (DeathWatch) support
* Add Actor.preRestart, Actor.postRestart methods? Jury still out on this. NO
* Akka has a kill method that forces onMessage to throw an ActorKilledException. Not keen on this.

## How to build
```bash
mvn clean install
```

## Getting started
Add this dependency to your pom.xml:
```xml
<dependency>
  <groupId>com.github.davidmoten</groupId>
  <artifactId>reels</artifactId>
  <version>VERSION_HERE</version>
</dependency><
```
## Glossary
 TODO

* Actor
* ActorRef
* Context
* Supervisor
* SupervisedActorRef
* Message

## Actor Lifecycle

![Actor Lifecycle](src/docs/state-diagram.svg)

## Class Diagram

![Class Diagram](src/docs/class-diagram.svg)

## Usage

### Create a Context
* Create a `Context` (`ActorSystem` in Akka) object to create your actors and control their lifecycle
* `Scheduler`s (`Dispatcher`s in Akka) live outside your `Context` object and thus can be shared across `Context`s (for greater efficiency))

The simplest way to create a `Context` object is:

```java
Context context = Context.create();
```

You can use a builder to configure the `Context`:
```java
Context context = Context
  .builder()
  .supervisor(supervisor)
  .deadLetterActorFactory(factory)
  .scheduler(scheduler)
  .build();
```
### Create an Actor using matchers
Now use a `Context` to create typed Actors:

```java
Context context = Context.create();
ActorRef<String> a = context.matchAny(m -> System.out.println(m.content())).build();
a.tell("hi there");
Thread.sleep(1000);
```

`ActorRef.tell` is asynchronous (with the default scheduler) so we wait with `Thread.sleep` to see something happen. The result of course is that 
"hi there" will be written to the console and it will generally happen on a different thread to the call of `tell`.

Here's a "kitchen sink" example that demonstrates many options when creating actors:
```java
Context context = Context.create();

// create a parent actor (you can setup heirarchies) using a builder
ActorRef<Number> a = context 
    .<Number>matchAny(m -> {
        log.info("{}: parent received {}", m.self(), m.content());
        m.self().child("b").tell(m.content(), m.self());
    }) 
    .name("a") 
    .scheduler(Scheduler.single()) 
    .onStop(self -> log.info("{}: onStop", self)) 
    .build();

// create a child actor of `a` using a builder
context 
    // specify a series of matches to apply to incoming message content
    .<Number>matchEquals(1, m -> {
        log.info("{}: equal matched, sender = {}", m.self(), m.sender());
        m.sender().<Number>tell(9999));
    }) 
    .match(Integer.class, m -> log.info("{}: received integer {}", m.self(), m.content())) 
    .match(Double.class, m -> log.info("{}: received double {}", m.self(), m.content())) 
    .matchAny(m -> log.info("{}: received something else {}", m.self(), m.content())) 
    .name("b") 
    .onError(e -> log.error(e.getMessage(), e)) 
    .preStart(self -> log.info("{}: preStart", self)) 
    .onStop(self -> log.info("{}: onStop", self)) 
    .scheduler(Scheduler.computation()) 
    .parent(a) 
    .supervisor((m, actor, e) -> {
        log.error(e.getMessage(), e);
        actor.pause(30, TimeUnit.SECONDS);
        actor.retry();
    }) 
    .build();

a.tell(1);
a.tell(2);
a.tell(3.5);
a.tell(4f);

// give enough time to run (especially for b to respond to `a.tell(1)`
Thread.sleep(500);
context.shutdownGracefully().get(5000, TimeUnit.SECONDS);
```

Output:
```
2022-10-07T21:47:28:055 +1100 [ReelsSingle-1] INFO com.github.davidmoten.reels.ActorTest - a: parent received 1
2022-10-07T21:47:28:057 +1100 [ReelsSingle-1] INFO com.github.davidmoten.reels.ActorTest - a: parent received 2
2022-10-07T21:47:28:057 +1100 [ReelsComputation-1] INFO com.github.davidmoten.reels.ActorTest - b: preStart
2022-10-07T21:47:28:057 +1100 [ReelsComputation-1] INFO com.github.davidmoten.reels.ActorTest - b: equal matched, sender = Optional[a]
2022-10-07T21:47:28:058 +1100 [ReelsComputation-1] INFO com.github.davidmoten.reels.ActorTest - b: received integer 2
2022-10-07T21:47:28:059 +1100 [ReelsSingle-1] INFO com.github.davidmoten.reels.ActorTest - a: parent received 3.5
2022-10-07T21:47:28:059 +1100 [ReelsSingle-1] INFO com.github.davidmoten.reels.ActorTest - a: parent received 4.0
2022-10-07T21:47:28:059 +1100 [ReelsComputation-1] INFO com.github.davidmoten.reels.ActorTest - b: received double 3.5
2022-10-07T21:47:28:060 +1100 [ReelsSingle-1] INFO com.github.davidmoten.reels.ActorTest - a: parent received 9999
2022-10-07T21:47:28:060 +1100 [ReelsComputation-1] INFO com.github.davidmoten.reels.ActorTest - b: received something else 4.0
2022-10-07T21:47:28:060 +1100 [ReelsComputation-1] INFO com.github.davidmoten.reels.ActorTest - b: received integer 9999
2022-10-07T21:47:28:562 +1100 [ReelsComputation-1] INFO com.github.davidmoten.reels.ActorTest - b: onStop
2022-10-07T21:47:28:564 +1100 [ReelsSingle-1] INFO com.github.davidmoten.reels.ActorTest - a: onStop
```

#### A warning about using lambdas to create actors
Note that if you use a lambda and you make a reference to the enclosing class then the ActorRef will retain a reference to the enclosing object. We should be careful about this because that referred object will not be garbage collected till the ActorRef is. Consequentl, if you create an actor from another actor then you should be conscious about this lifecycle link.

### Create an Actor using your own class

You can also create an Actor class yourself instead of using the builder. Implement `Actor<T>` or extend `AbstractActor<T>`. Suppose you create a class called `MyActor` which extends `AbstractActor<Integer>`. You can create an ActorRef for this class with the Context as follows:

```java
ActorRef<Integer> actor = context.actorClass(MyActor.class).build();
```
or
```java
ActorRef<Integer> actor = context.actorFactory(() -> new MyActor()).build();
```

Note that you can also use the actorClass builder method with constructor arguments too:
```java
ActorRef<Integer> actor = context.actorClass(MyActor.class, "Fred Nurk", 1980).build();
```

## Send a message to an Actor
This sends an anonymous message (the Actor wil be supplied ActorRef.none() as the sender) to an actor:

```java
ActorRef<Thing> actor = ...
Thing thing = ...
actor.tell(thing);
```
To include a sender actor (for example as a reply-to actor):
```java
ActorRef<Thing> actor = ...
ActorRef<Object> replyTo = ...
Thing thing = ...
actor.tell(thing, replyTo);
```

## ask

Here's an example of `ask` where an actor does some calculation and returns an answer asynchronously (via a `CompletableFuture`):

```java
ActorRef<Integer> square = 
    context
      .<Integer>matchAny(m -> m.reply(m.content() * m.content())
      .build();
square
  .ask(23)
  .thenAccept(System.out::println)
  .join();
```
Output:
```
529
```

## Supervisors
When an error is thrown by your code in onMessage you have two options:
* catch and handle the error in-place and reset state as appropriate
* handle the error in a Supervisor, which has more functionality including the ability to restart (recreate the Actor instance), pause processing, and retry a message.

For example, a Supervisor for some JDBC work can check if the thrown exception is an instance of SQLTransientException and mark the message for a retry after a pause. 
If the thrown exception is an instance of SQLNonTransientConnectionException then we should retry with a new connection object. This [unit test](src/test/java/com/github/davidmoten/reels/RetryExampleTest.java) demonstrates this example.

If you don't specify your own Supervisor (either globally in the Context or specifically for your Actor) then the [default Supervisor](src/main/java/com/github/davidmoten/reels/internal/supervisor/SupervisorDefault.java) is used.

Note that the Supervisor is called also for errors thrown by `Actor.preStart` and `Actor.onStop`. However, those errors will arrive at the Supervisor wrapped in `PreStartException` and `OnStopException` respectively. 

The default Supervisor for an actor is the Supervisor of its parent.

Supervisors themselves should not throw. If they do TODO.

## Notes

* An Actor is created by a Context object. The Context object has an internal singleton root actor but is the parent for an Actor you create unless you provide it with an explicit parent. 
* When an actor is disposed no more children can be created for it  
* Dispose happens synchronously (the actor and all its children and descendants are disposed before the method returns) 
* Restarting an actor from a supervisor will dispose all that actors children

## Schedulers
Sending a message to an Actor is normally asynchronous. That is when you call `actorRef.tell(msg)` what happens under the covers is that the message is placed on a concurrent queue for that actor and the actor is notified to process its queue using an executor.

Schedulers wrap executors and are designed to be efficient for particular use cases. Actors obtain a Worker from each Scheduler for the lifetime of the actor. The standard schedulers are 

* `Scheduler.forkjoin()`, a singleton work-stealing pool of threads that is great for general purpose non-blocking work (wins benchmarks pretty handily and became twice as fast between Java 8 and Java 17). The default scheduler.
* `Scheduler.computation()` is an alias for `forkJoin()` and is for non-blocking work
* `Scheduler.computationSticky()`, a singleton pool of threads (size = number of processors) for non-blocking work. A Worker on this pool uses a randomly/round-robin assigned thread from the pool and that thread stays with the Worker till disposal of the Worker (that is a thread *sticks* to an actor). One thread can be in use by many Workers. Normally slower than `forkJoin()`.
* `Scheduler.io()`, a singleton unbounded thread pool designed for blocking work, unused threads are disposed of by an evicting thread after 60s of inactivity. Each Worker has one thread (and each thread in this pool has only one Worker). This scheduler was adapted from RxJava 3.x `Schedulers.io()`.
* `Scheduler.single()`, a singleton scheduler that is based on a single thread executor service
* `Scheduler.newSingle()`, creates a new single-thread-based scheduler
* `Scheduler.fromExecutor(ExecutorService)`, creates a new scheduler based on the given ExecutorService. Use one of these with a pool for blocking work where you have a lot of actors (to limit context switching and thread memory use)
* `Scheduler.test()` is for synchronous unit testing purposes and **should not be mixed with asynchronous scheduler use** in the same Context.
* `Scheduler.immediate()` is for synchronous execution of all tasks, limited delayed scheduling, and **should not be mixed with asynchronous scheduler use** in the same Context
 
## Blocking work

Make sure you use a blocking scheduler (especially `Scheduler.io()`) for any blocking work like database calls, file system IO, network IO.
 
## Benchmarks
To run benchmarks:

```bash
mvn clean install -P benchmark
```

Benchmarking indicates that reels is faster than Akka for three aspects tested:

* ask performance (15% faster)
* hub and spoke contended performance (3x faster)
* random messages around a ring performance, some contention, uses actor lookups (5x faster)
* long sequential chain then return (8x faster)

Using JDK 17 on i5-6200U with 4 cores:
```
Benchmark                                          Mode  Cnt       Score     Error  Units
Benchmarks.actorCreateAndStop                     thrpt   10  249599.500 ± 417.851  ops/s
Benchmarks.ask                                    thrpt   10       8.048 ±   0.022  ops/s
Benchmarks.contendedConcurrencyComputationSticky  thrpt   10       0.696 ±   0.053  ops/s
Benchmarks.contendedConcurrencyForkJoin           thrpt   10       5.679 ±   0.043  ops/s
Benchmarks.contendedConcurrencyImmediate          thrpt   10       9.422 ±   0.175  ops/s
Benchmarks.groupRandomMessagesComputationSticky   thrpt   10       1.921 ±   0.055  ops/s
Benchmarks.groupRandomMessagesForkJoin            thrpt   10      17.068 ±   0.238  ops/s
Benchmarks.groupRandomMessagesImmediate           thrpt   10      31.629 ±   0.384  ops/s
Benchmarks.groupRandomMessagesIo                  thrpt   10       0.867 ±   0.019  ops/s
BenchmarksAkka.ask                                thrpt   10       7.095 ±   0.175  ops/s
BenchmarksAkka.contendedConcurrency               thrpt   10       1.746 ±   0.017  ops/s
BenchmarksAkka.groupRandomMessages                thrpt   10       3.300 ±   0.102  ops/s
Benchmarks.sequential                                ss   10       2.514 ±   0.361   s/op
BenchmarksAkka.sequential                            ss   10      21.438 ±   0.587   s/op
```

