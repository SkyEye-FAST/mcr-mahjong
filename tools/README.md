# Native differential verification

These tools are optional developer tools, not runtime or normal CI dependencies.
Run commands from the repository root. The upstream checkout and build outputs
stay in ignored `.reference/`, so a library `clean` cannot delete the reference
needed by differential verification. Do not commit or publish native binaries.

## Pin and compile the reference

```shell
git clone https://github.com/summerinsects/mahjong-algorithm.git .reference/upstream
git -C .reference/upstream checkout --detach 44a178af08bf11f82a8993fddbe2fe8876ddd8f3
git -C .reference/upstream rev-parse HEAD
```

Linux/macOS, with CMake 3.20+ and a C++17 compiler:

```shell
cmake -S tools -B .reference/oracle -DCMAKE_BUILD_TYPE=Release
cmake --build .reference/oracle
./gradlew differentialTest -PoraclePath="$PWD/.reference/oracle/oracle"
```

Windows, in a Visual Studio x64 Native Tools command prompt:

```bat
cmake -S tools -B .reference/oracle -G "NMake Makefiles" -DCMAKE_BUILD_TYPE=Release
cmake --build .reference/oracle
gradlew.bat differentialTest -PoraclePath=C:/Java/mcr-mahjong/.reference/oracle/oracle.exe
```

Adjust the absolute oracle path when the repository is elsewhere. No extra feature
defines are needed: the defaults pinned in NOTICE are the compatibility baseline.
CMake checks all nine files against `tools/upstream.sha256` before compiling and
embeds the manifest fingerprint. The adapter also asserts the feature switches
and fan-table size at compile time. Rebuild through CMake when source files change;
the old unchecked direct-compiler invocation is intentionally not supported.

## What is compared

The fixed-seed suite compares complete fan-count tables, all five forms' shanten
and effective-tile bitmaps, structural wait sets, and ordered per-discard/form
outputs. It includes fixed melds, concealed/exposed/promoted kongs, wind and win
flags, flowers, special hands and non-winning hands. All 214 active `test_points`
calls in the pinned `unit_test.cpp` are mandatory; repeated identical inputs are
checked once. Additional targeted inputs exercise seven pairs, all six knitted
permutations, both honors-and-knitted fans and kongs in the fourth group.

The suite verifies the source hashes again and compares the running oracle's
fingerprint and all 82 fan values before evaluating hands. Missing or changed
source files, a stale oracle, an unexpected case inventory or an unresponsive
oracle are failures, not skipped tests. Each oracle exchange has a bounded timeout.
Both shanten-only and effective-tile calculation paths are checked.

`F|notation|flags|prevalentWind|seatWind|flowers` returns a total followed by all
enabled fan counts in upstream order, excluding `FAN_NONE`. Winds are 0 through 3.
`S|notation` returns five `shanten,useful-bitmap` fields followed by the wait bitmap.
`D|notation` returns the full ordered discard callback stream. The bitmaps are in
upstream tile order: characters, bamboo, dots, honors. Bracket notation here belongs
only to the test adapter, not to the public parser.
`V` returns the source-manifest fingerprint and the enabled fan values.

Inapplicable forms return `INT_MAX` and zero bits in the adapter. Upstream's buffer
contents are not defined for those inputs, so they are not meaningful differential
targets. All applicable outputs are compared without normalization.

## Frozen regression data

`src/test/resources/fan-regression.tsv` contains compact full-table expectations.
Omitted fan entries must be zero; testing only the total is insufficient.
`analysis-regression.tsv` contains exact shanten/useful/wait output and SHA-256
digests of complete discard streams (UTF-8, no terminal newline). Regular tests
need neither a C++ compiler nor the upstream checkout.

To propose a compact fan corpus from the pinned upstream on Windows:

```powershell
./tools/generate-fixtures.ps1
./tools/generate-analysis-fixtures.ps1
```

Both generators check the checkout revision and emit text to standard output;
the fan generator also reports coverage to standard error. Review the output before updating fixtures;
retain targeted bug regressions even when the coverage selector would omit them.
Never regenerate expected values from the Kotlin implementation under test.
