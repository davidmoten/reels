# reels
<a href="https://github.com/davidmoten/reels/actions/workflows/ci.yml"><img src="https://github.com/davidmoten/reels/actions/workflows/ci.yml/badge.svg"/></a><br/>
[![codecov](https://codecov.io/gh/davidmoten/reels/branch/master/graph/badge.svg)](https://codecov.io/gh/davidmoten/reels)<br/>
[![Maven Central](https://maven-badges.herokuapp.com/maven-central/au.gov.amsa/reels/badge.svg?style=flat)](https://maven-badges.herokuapp.com/maven-central/au.gov.amsa/reels)<br/>

Actor framework for Java, non-blocking, performant

```
Benchmarks.actorCreateAndStop                     thrpt   10  1036159.638 ± 11147.430  ops/s
Benchmarks.ask                                    thrpt   10    49202.346 ±  1347.799  ops/s
Benchmarks.contendedConcurrencyComputationSticky  thrpt   10        0.508 ±     0.010  ops/s
Benchmarks.contendedConcurrencyForkJoin           thrpt   10        3.107 ±     0.159  ops/s
Benchmarks.contendedConcurrencyImmediate          thrpt   10        3.262 ±     0.261  ops/s
Benchmarks.groupRandomMessagesComputationSticky   thrpt   10       18.549 ±     0.328  ops/s
Benchmarks.groupRandomMessagesForkJoin            thrpt   10       18.710 ±     0.261  ops/s
Benchmarks.groupRandomMessagesImmediate           thrpt   10       18.509 ±     0.219  ops/s
```

