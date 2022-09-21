# reels
<a href="https://github.com/davidmoten/reels/actions/workflows/ci.yml"><img src="https://github.com/davidmoten/reels/actions/workflows/ci.yml/badge.svg"/></a><br/>
[![codecov](https://codecov.io/gh/davidmoten/reels/branch/master/graph/badge.svg)](https://codecov.io/gh/davidmoten/reels)<br/>
[![Maven Central](https://maven-badges.herokuapp.com/maven-central/au.gov.amsa/reels/badge.svg?style=flat)](https://maven-badges.herokuapp.com/maven-central/au.gov.amsa/reels)<br/>

Actor framework for Java, non-blocking, performant

# Actor lifecycle

An Actor is created by a Context object. The Context object has a singleton root actor that is not accessible but is the parent for an Actor you create unless you provide it with an explicit parent. 

An Actor is either Active, Stopped or Disposed. Once created an Actor is Active and will process messages sent to it (via `ActorRef.tell`). If the Actor is disposed (via `ActorRef.dispose()` then the Actor will stop processing messages.


```
Benchmarks.actorCreateAndStop                     thrpt   10  1036159.638 ± 11147.430  ops/s
Benchmarks.ask                                    thrpt   10    49202.346 ±  1347.799  ops/s
Benchmarks.contendedConcurrencyComputationSticky  thrpt   10        0.508 ±     0.010  ops/s
Benchmarks.contendedConcurrencyForkJoin           thrpt   10        3.107 ±     0.159  ops/s
Benchmarks.contendedConcurrencyImmediate          thrpt   10        3.262 ±     0.261  ops/s
Benchmarks.groupRandomMessagesComputationSticky   thrpt   10       18.549 ±     0.328  ops/s
Benchmarks.groupRandomMessagesForkJoin            thrpt   10       18.710 ±     0.261  ops/s
Benchmarks.groupRandomMessagesImmediate           thrpt   10       18.509 ±     0.219  ops/s

Benchmark                                          Mode  Cnt       Score      Error  Units
Benchmarks.actorCreateAndStop                     thrpt   10  273525.889 ± 1547.441  ops/s
Benchmarks.ask                                    thrpt   10   42009.980 ±  570.231  ops/s
Benchmarks.contendedConcurrencyComputationSticky  thrpt   10       0.477 ±    0.066  ops/s
Benchmarks.contendedConcurrencyForkJoin           thrpt   10       1.582 ±    0.089  ops/s
Benchmarks.contendedConcurrencyImmediate          thrpt   10       2.710 ±    0.256  ops/s
Benchmarks.groupRandomMessagesComputationSticky   thrpt   10      11.538 ±    0.098  ops/s
Benchmarks.groupRandomMessagesForkJoin            thrpt   10      11.603 ±    0.050  ops/s
Benchmarks.groupRandomMessagesImmediate           thrpt   10      11.091 ±    0.455  ops/s
Benchmarks.groupRandomMessagesIo                  thrpt   10      10.398 ±    0.397  ops/s

2022-09-21

Benchmark                                          Mode  Cnt       Score     Error  Units
Benchmarks.actorCreateAndStop                     thrpt   10  246988.715 ± 920.241  ops/s
Benchmarks.ask                                    thrpt   10   51283.671 ±2435.835  ops/s
Benchmarks.contendedConcurrencyComputationSticky  thrpt   10       0.477 ±   0.052  ops/s
Benchmarks.contendedConcurrencyForkJoin           thrpt   10       1.609 ±   0.120  ops/s
Benchmarks.contendedConcurrencyImmediate          thrpt   10       2.702 ±   0.133  ops/s
Benchmarks.groupRandomMessagesComputationSticky   thrpt   10      11.594 ±   0.156  ops/s
Benchmarks.groupRandomMessagesForkJoin            thrpt   10      11.681 ±   0.066  ops/s
Benchmarks.groupRandomMessagesImmediate           thrpt   10      11.368 ±   0.150  ops/s
Benchmarks.groupRandomMessagesIo                  thrpt   10      11.539 ±   0.420  ops/s
```

