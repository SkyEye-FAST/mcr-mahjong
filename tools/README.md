# Native differential verification

These tools are optional developer tools, not runtime or normal CI dependencies.
Run commands from the repository root. The upstream checkout and build outputs
stay in ignored `build/`; do not commit or publish native binaries.

## Pin and compile the reference

```shell
git clone https://github.com/summerinsects/mahjong-algorithm.git build/upstream
git -C build/upstream checkout --detach 44a178af08bf11f82a8993fddbe2fe8876ddd8f3
git -C build/upstream rev-parse HEAD
```

Linux/macOS, with a C++17 compiler:

```shell
c++ -std=c++17 -O2 -Ibuild/upstream tools/oracle.cpp \
  build/upstream/fan_calculator.cpp build/upstream/shanten.cpp \
  build/upstream/stringify.cpp -o build/oracle
./gradlew differentialTest -PoraclePath="$PWD/build/oracle"
```

Windows, in a Visual Studio x64 Native Tools command prompt:

```bat
cl /nologo /std:c++17 /EHsc /O2 /Ibuild\upstream tools\oracle.cpp build\upstream\fan_calculator.cpp build\upstream\shanten.cpp build\upstream\stringify.cpp /Fobuild\ /Febuild\oracle.exe
gradlew.bat differentialTest -PoraclePath=C:/Java/mcr-mahjong/build/oracle.exe
```

Adjust the absolute oracle path when the repository is elsewhere. No extra feature
defines are needed: the defaults pinned in NOTICE are the compatibility baseline.

## What is compared

The fixed-seed suite compares complete fan-count tables, all five forms' shanten
and effective-tile bitmaps, structural wait sets, and ordered per-discard/form
outputs. It includes fixed melds, concealed/exposed/promoted kongs, wind and win
flags, flowers, special hands and non-winning hands. When `build/upstream/unit_test.cpp`
exists, its `test_points` cases are also extracted and checked. Without that file,
the frozen inputs and seeded cases still run; the upstream-case portion is skipped.

`F|notation|flags|prevalentWind|seatWind|flowers` returns a total followed by all
enabled fan counts in upstream order, excluding `FAN_NONE`. Winds are 0 through 3.
`S|notation` returns five `shanten,useful-bitmap` fields followed by the wait bitmap.
`D|notation` returns the full ordered discard callback stream. The bitmaps are in
upstream tile order: characters, bamboo, dots, honors. Bracket notation here belongs
only to the test adapter, not to the public parser.

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
