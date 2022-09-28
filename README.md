# reels
<a href="https://github.com/davidmoten/reels/actions/workflows/ci.yml"><img src="https://github.com/davidmoten/reels/actions/workflows/ci.yml/badge.svg"/></a><br/>
[![codecov](https://codecov.io/gh/davidmoten/reels/branch/master/graph/badge.svg)](https://codecov.io/gh/davidmoten/reels)<br/>
[![Maven Central](https://maven-badges.herokuapp.com/maven-central/au.gov.amsa/reels/badge.svg?style=flat)](https://maven-badges.herokuapp.com/maven-central/au.gov.amsa/reels)<br/>

Actor framework for Java, non-blocking, performant

# Actor lifecycle

* An Actor is created by a Context object. The Context object has a singleton root actor that is not accessible but is the parent for an Actor you create unless you provide it with an explicit parent. 
* An Actor is either **Active**, **Stopping** or **Disposed**. 
* Once created an Actor is **Active** and will process messages sent to it (via `ActorRef.tell`). 
* If the Actor is disposed (via `ActorRef.dispose()` then the Actor will stop processing messages (after the currently running message if one is being processed).  
* If an Actor is stopped then all messages still queued and future messages sent to that Actor will go to the Dead Letter actor (owned by `Context`)
* A custom Dead Letter actor can be set
* Children are stopped before a parent
* Calling dispose on an actor does not wait (or provide a future to be waited upon) for the actor to finish processing nor does it send messages that arrive to the actor after disposal to the Dead Letter actor. * Dispose does not run postStop
* When an actor is disposed no more children can be created for it  

```
Benchmarks.actorCreateAndStop                     thrpt   10  1036159.638 ± 11147.430  ops/s
Benchmarks.ask                                    thrpt   10    49202.346 ±  1347.799  ops/s
Benchmarks.contendedConcurrencyComputationSticky  thrpt   10        0.508 ±     0.010  ops/s
Benchmarks.contendedConcurrencyForkJoin           thrpt   10        3.107 ±     0.159  ops/s
Benchmarks.contendedConcurrencyImmediate          thrpt   10        3.262 ±     0.261  ops/s
Benchmarks.groupRandomMessagesForkJoin            thrpt   10       18.710 ±     0.261  ops/s

Benchmark                                          Mode  Cnt       Score      Error  Units
Benchmarks.actorCreateAndStop                     thrpt   10  273525.889 ± 1547.441  ops/s
Benchmarks.ask                                    thrpt   10   42009.980 ±  570.231  ops/s
Benchmarks.contendedConcurrencyComputationSticky  thrpt   10       0.477 ±    0.066  ops/s
Benchmarks.contendedConcurrencyForkJoin           thrpt   10       1.582 ±    0.089  ops/s
Benchmarks.contendedConcurrencyImmediate          thrpt   10       2.710 ±    0.256  ops/s
Benchmarks.groupRandomMessagesForkJoin            thrpt   10      11.603 ±    0.050  ops/s

2022-09-21:

Benchmark                                          Mode  Cnt       Score     Error  Units
Benchmarks.actorCreateAndStop                     thrpt   10  246988.715 ± 920.241  ops/s
Benchmarks.ask                                    thrpt   10   51283.671 ±2435.835  ops/s
Benchmarks.contendedConcurrencyComputationSticky  thrpt   10       0.477 ±   0.052  ops/s
Benchmarks.contendedConcurrencyForkJoin           thrpt   10       1.609 ±   0.120  ops/s
Benchmarks.contendedConcurrencyImmediate          thrpt   10       2.702 ±   0.133  ops/s
Benchmarks.groupRandomMessagesForkJoin            thrpt   10      11.681 ±   0.066  ops/s

Some perf work:

Benchmark                                          Mode  Cnt       Score       Error  Units
Benchmarks.actorCreateAndStop                     thrpt   10  842466.597 ± 11081.635  ops/s
Benchmarks.ask                                    thrpt   10   52782.734 ±   168.250  ops/s
Benchmarks.contendedConcurrencyComputationSticky  thrpt   10       0.491 ±     0.009  ops/s
Benchmarks.contendedConcurrencyForkJoin           thrpt   10       3.259 ±     0.270  ops/s
Benchmarks.contendedConcurrencyImmediate          thrpt   10       3.446 ±     0.296  ops/s
Benchmarks.groupRandomMessagesForkJoin            thrpt   10      16.360 ±     0.232  ops/s

Benchmark                                          Mode  Cnt       Score      Error  Units
Benchmarks.actorCreateAndStop                     thrpt   10  822067.036 ± 3634.627  ops/s
Benchmarks.ask                                    thrpt   10   52505.270 ±  135.025  ops/s
Benchmarks.contendedConcurrencyComputationSticky  thrpt   10       0.495 ±    0.015  ops/s
Benchmarks.contendedConcurrencyForkJoin           thrpt   10       3.180 ±    0.227  ops/s
Benchmarks.contendedConcurrencyImmediate          thrpt   10       3.457 ±    0.421  ops/s
Benchmarks.groupRandomMessagesComputationSticky   thrpt   10       1.730 ±    0.036  ops/s
Benchmarks.groupRandomMessagesForkJoin            thrpt   10      16.982 ±    0.237  ops/s
Benchmarks.groupRandomMessagesImmediate           thrpt   10      22.215 ±    0.992  ops/s
Benchmarks.groupRandomMessagesIo                  thrpt   10       0.789 ±    0.017  ops/s

post Actor method signature changes (allocation work):

Benchmark                                          Mode  Cnt       Score      Error  Units
Benchmarks.actorCreateAndStop                     thrpt   10  847536.701 ± 4350.074  ops/s
Benchmarks.ask                                    thrpt   10       5.284 ±    0.020  ops/s
Benchmarks.contendedConcurrencyComputationSticky  thrpt   10       0.525 ±    0.011  ops/s
Benchmarks.contendedConcurrencyForkJoin           thrpt   10       3.396 ±    0.421  ops/s
Benchmarks.contendedConcurrencyImmediate          thrpt   10       3.021 ±    0.252  ops/s
Benchmarks.groupRandomMessagesComputationSticky   thrpt   10       1.690 ±    0.019  ops/s
Benchmarks.groupRandomMessagesForkJoin            thrpt   10      16.928 ±    0.332  ops/s
Benchmarks.groupRandomMessagesImmediate           thrpt   10      21.397 ±    0.031  ops/s
Benchmarks.groupRandomMessagesIo                  thrpt   10       0.814 ±    0.019  ops/s
```

Anonymous naming change:
```
Benchmark                                          Mode  Cnt        Score      Error  Units
Benchmarks.actorCreateAndStop                     thrpt   10  1072695.807 ± 8297.390  ops/s
Benchmarks.ask                                    thrpt   10        5.363 ±    0.051  ops/s
Benchmarks.contendedConcurrencyComputationSticky  thrpt   10        0.500 ±    0.009  ops/s
Benchmarks.contendedConcurrencyForkJoin           thrpt   10        3.326 ±    0.259  ops/s
Benchmarks.contendedConcurrencyImmediate          thrpt   10        3.674 ±    0.427  ops/s
Benchmarks.groupRandomMessagesComputationSticky   thrpt   10        1.707 ±    0.052  ops/s
Benchmarks.groupRandomMessagesForkJoin            thrpt   10       16.859 ±    0.174  ops/s
Benchmarks.groupRandomMessagesImmediate           thrpt   10       18.922 ±    3.095  ops/s

Benchmark                                          Mode  Cnt       Score      Error  Units
Benchmarks.actorCreateAndStop                     thrpt   10  991539.985 ± 2534.087  ops/s
Benchmarks.ask                                    thrpt   10       5.320 ±    0.034  ops/s
Benchmarks.contendedConcurrencyComputationSticky  thrpt   10       0.400 ±    0.039  ops/s
Benchmarks.contendedConcurrencyForkJoin           thrpt   10       5.062 ±    0.636  ops/s
Benchmarks.contendedConcurrencyImmediate          thrpt   10       6.392 ±    0.487  ops/s
Benchmarks.groupRandomMessagesComputationSticky   thrpt   10       1.646 ±    0.204  ops/s
Benchmarks.groupRandomMessagesForkJoin            thrpt   10      17.013 ±    0.261  ops/s
Benchmarks.groupRandomMessagesImmediate           thrpt   10      22.577 ±    0.638  ops/s
Benchmarks.groupRandomMessagesIo                  thrpt   10       0.833 ±    0.019  ops/s
Benchmarks.sequential                                ss   10       5.919 ±    0.889   s/op

Benchmark                                          Mode  Cnt        Score      Error  Units
Benchmarks.actorCreateAndStop                     thrpt   10  1013957.228 ± 5393.657  ops/s
Benchmarks.ask                                    thrpt   10        5.276 ±    0.047  ops/s
Benchmarks.contendedConcurrencyComputationSticky  thrpt   10        0.521 ±    0.020  ops/s
Benchmarks.contendedConcurrencyForkJoin           thrpt   10        5.837 ±    0.094  ops/s
Benchmarks.contendedConcurrencyImmediate          thrpt   10        8.431 ±    0.117  ops/s
Benchmarks.groupRandomMessagesComputationSticky   thrpt   10        1.688 ±    0.029  ops/s
Benchmarks.groupRandomMessagesForkJoin            thrpt   10       16.926 ±    0.269  ops/s
Benchmarks.groupRandomMessagesImmediate           thrpt   10       21.326 ±    0.123  ops/s
Benchmarks.groupRandomMessagesIo                  thrpt   10        0.862 ±    0.017  ops/s
Benchmarks.sequential                                ss   10        4.899 ±    0.433   s/op

Benchmark                                          Mode  Cnt        Score      Error  Units
Benchmarks.actorCreateAndStop                     thrpt   10  1077140.852 ± 7744.401  ops/s
Benchmarks.ask                                    thrpt   10        5.176 ±    0.016  ops/s
Benchmarks.contendedConcurrencyComputationSticky  thrpt   10        0.489 ±    0.010  ops/s
Benchmarks.contendedConcurrencyForkJoin           thrpt   10        6.056 ±    0.112  ops/s
Benchmarks.contendedConcurrencyImmediate          thrpt   10        7.591 ±    0.134  ops/s
Benchmarks.groupRandomMessagesComputationSticky   thrpt   10        1.532 ±    0.020  ops/s
Benchmarks.groupRandomMessagesForkJoin            thrpt   10       16.845 ±    0.296  ops/s
Benchmarks.groupRandomMessagesImmediate           thrpt   10       21.934 ±    0.357  ops/s
Benchmarks.groupRandomMessagesIo                  thrpt   10        0.783 ±    0.029  ops/s
Benchmarks.sequential                                ss   10        4.827 ±    0.500   s/op

Benchmark                                          Mode  Cnt       Score      Error  Units
Benchmarks.actorCreateAndStop                     thrpt   10  249825.851 ± 1208.358  ops/s
Benchmarks.ask                                    thrpt    8       5.400 ±    0.061  ops/s
Benchmarks.contendedConcurrencyComputationSticky  thrpt   10       0.847 ±    0.059  ops/s
Benchmarks.contendedConcurrencyForkJoin           thrpt   10       5.999 ±    0.088  ops/s
Benchmarks.contendedConcurrencyImmediate          thrpt   10       8.123 ±    0.039  ops/s
Benchmarks.groupRandomMessagesComputationSticky   thrpt   10       1.656 ±    0.012  ops/s
Benchmarks.groupRandomMessagesForkJoin            thrpt   10      18.441 ±    0.147  ops/s
Benchmarks.groupRandomMessagesImmediate           thrpt   10      25.841 ±    1.135  ops/s
Benchmarks.groupRandomMessagesIo                  thrpt   10       0.824 ±    0.023  ops/s
Benchmarks.sequential                                ss   10       2.559 ±    0.700   s/op
```
