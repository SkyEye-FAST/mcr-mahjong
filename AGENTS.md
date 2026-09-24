# mcr-mahjong

- Single-module Kotlin/JVM library, Java 17 bytecode/API baseline.
- Keep Minecraft, native runtimes and Python out of the library and normal tests.
- Phase one preserves the pinned upstream's algorithm and tie-breaking behavior.
  Do not replace it with independently interpreted rules or silently fix upstream quirks.
- Public input validation may be stricter than C++; document the boundary.
- Keep routine regression tests deterministic and fast. Native differential tests
  are explicit opt-in developer checks, never a prerequisite for ordinary builds.
- Preserve MIT attribution. Update NOTICE and fixtures together when changing upstream.
- Sign every commit. Do not modify sibling repositories.
