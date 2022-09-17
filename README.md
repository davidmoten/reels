# reels
<a href="https://github.com/davidmoten/reels/actions/workflows/ci.yml"><img src="https://github.com/davidmoten/reels/actions/workflows/ci.yml/badge.svg"/></a><br/>
[![codecov](https://codecov.io/gh/davidmoten/reels/branch/master/graph/badge.svg)](https://codecov.io/gh/davidmoten/reels)<br/>
[![Maven Central](https://maven-badges.herokuapp.com/maven-central/au.gov.amsa/reels/badge.svg?style=flat)](https://maven-badges.herokuapp.com/maven-central/au.gov.amsa/reels)<br/>

Actor framework for Java, non-blocking, performant

```
Benchmark                                          Mode  Cnt      Score      Error  Units
Benchmarks.ask                                    thrpt   10  48681.750 ± 4282.880  ops/s
Benchmarks.contendedConcurrencyComputationSticky  thrpt   10      0.496 ±    0.011  ops/s
Benchmarks.contendedConcurrencyForkJoin           thrpt   10      3.087 ±    0.071  ops/s
Benchmarks.contendedConcurrencyImmediate          thrpt   10      3.403 ±    0.169  ops/s
Benchmarks.groupRandomMessagesComputationSticky   thrpt   10    256.102 ±    4.920  ops/s
Benchmarks.groupRandomMessagesForkJoin            thrpt   10    262.046 ±    4.029  ops/s
Benchmarks.groupRandomMessagesImmediate           thrpt   10    237.237 ±    9.217  ops/s
```

