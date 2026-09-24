# Compatibility contract

The public API is the documented `top.skyeyefast.mcr` package. The
`top.skyeyefast.mcr.internal` package, packed integers, enum ordinals and test
protocol are implementation details and are not serialization contracts.
Consumers should store tile/fan names rather than internal numbers.

## Baseline

The baseline is the exact upstream revision and compile-time defaults in NOTICE.
This initial port is not an independently reinterpreted rules engine. It retains
decomposition order, exclusion order, special-hand priority and equal-score
selection. Any later semantic change needs a documented baseline change and
updated differential fixtures before release.

The baseline has 81 standard fans and one enabled extra fan:
`CONCEALED_KONG_AND_MELDED_KONG` (5 points). The 81-fan enumeration and that
compatibility entry must not be confused. Blessing fans and strict-1998 variants
are disabled. The two variants of pure shifted chows remain one fan.

## Deliberately retained behavior

* Seven pairs counts four identical tiles as two pairs.
* Structural useful/wait tables can include a fifth-copy wait. Such a tile has
  zero remaining copies through the public API; physically drawing it is rejected.
* Initial east self-draw uses upstream's special nine-gates interpretation,
  including its `r = 2` branch when the extra tile is a nine. This is frozen in a
  regression, not silently corrected to a different rule interpretation.
* The enabled knitted-straight setting can award an edge/closed/single wait when
  a winning tile is also interpretable as part of the knitted body.
* Last-copy and kong-involved flags are normalized exactly as in upstream.
* A tied regular decomposition can take priority for pure triple chow, triple
  pung or a seven-pairs alternative according to upstream traversal order.

## Intentional API-boundary differences

Invalid hands throw `IllegalArgumentException` before calculation rather than
exposing C++ error codes, invalid memory access or undefined output buffers.
Null elements in Java-supplied collections are rejected with `IllegalArgumentException`;
null passed for a non-null parameter uses Kotlin's standard `NullPointerException`.
Input value objects validate both construction and Kotlin `copy` calls. `FanCount`
also rejects multiplication overflow. Aggregate result constructors are private:
obtain results through `McrMahjong` in both Kotlin and Java. Internal packing and
array helpers are hidden from Java source with `@JvmSynthetic`.

All returned collections are immutable snapshots. Hand equality includes tile
and meld order, rather than testing structural equivalence. Evaluation retains no
caller-owned mutable collections or shared evaluation state; callers must not
modify a mutable input collection concurrently while a call is taking its snapshot.

Unavailable special forms are omitted from `HandAnalysis.forms`; the native
adapter represents them as `INT_MAX` with an empty useful table without invoking
upstream on an unsupported count. In particular, upstream's honors-and-knitted
wrapper can otherwise copy an uninitialized buffer.

The upstream discard callback uses `-1` when returning a tile that completes the
original hand. Here `DiscardAnalysis.shanten` consistently describes the remaining
13-tile hand, and `completesForms` preserves that extra information. Differential
tests reconstruct and compare the exact upstream stream. Its ineffective
`form_flag | FLAG` filtering is not exposed as a working public option: all
applicable forms are returned, and callers can filter the typed result.

Visible-tile accounting and the eight-point qualification convenience property
are wrapper functionality, not changes to upstream's shanten or fan algorithms.
The library does not validate a full game's chronology, discarder eligibility,
wall availability or all circumstances of a legal call.

## Dependencies and publishing

Java 17 is both the bytecode target and the Java API baseline. The sole direct
library dependency is Kotlin's standard library; JetBrains annotations is its
transitive dependency. JUnit and the optional C++ process are test-only.
The C++ oracle communicates over standard input/output; no native code is loaded
into the JVM or bundled in Maven artifacts.

The initial artifact is `top.skyeyefast:mcr-mahjong:0.1.0-SNAPSHOT`. Public API
changes must be explicit and accompanied by updated consumer tests. There is no
promise that internal Kotlin/JVM-mangled members are stable API.
