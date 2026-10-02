# `.mvn`

This directory marks the multi-module project root for the Maven launcher, which
sets `${maven.multiModuleProjectDirectory}` from it.

`kura/pom.xml` uses that property to locate the EN 50716 static-analysis
configuration of the Kura Java Coding Standard (KJCS):

- `tools/pmd/kura-java-ruleset.xml` — the Basic Integrity ruleset, applied to
  every module through `kura.pmd.ruleset`
- `tools/pmd/kura-java-ruleset-sil2.xml` — the SIL 2 ruleset (Basic Integrity
  plus the SIL 2-only rules), selected by a SIL 2 bundle setting
  `kura.pmd.ruleset` in its own POM
- `tools/pmd/pmd-baseline.properties` — the register of known non-conformities
- `tools/pmd/selftest/` — the self-test of both rulesets
- the `en50716-consolidated` execution of `maven-pmd-plugin` — one consolidated
  report (`target/pmd.xml`) for the whole codebase

Without this marker the property resolves to the current working directory, and
`mvn` invoked from inside a module would not find these files.
