# KJCS PMD self-test

Proves that every custom check (KJCS-Xnn) and every built-in check whose properties
differ from their defaults still reports what it should, and nothing else, in both
rulesets of record:

- `../kura-java-ruleset.xml` - Basic Integrity: every KJCS rule except the SIL 2-only ones.
- `../kura-java-ruleset-sil2.xml` - SIL 2: the Basic Integrity rules plus the SIL 2-only rules
  (X10 for KJCS-101, X13 and X14 for KJCS-102).

Each fixture line that must be reported carries a marker naming the rule(s):

    f.delete();      // expect: KJCS-X15-DiscardedStatusResult

Unmarked lines are the compliant cases: any report on them is a failure. The Basic
Integrity run expects every marker except those naming a SIL 2-only rule; the SIL 2
run expects every marker.

The test also fails if the two rulesets differ in anything other than the three
SIL 2-only rules, so a change made to one ruleset and not the other is caught.

    PMD_HOME=/opt/pmd-bin-7.27.0 sh run.sh

Exit status 0 means both runs match exactly and the rulesets are consistent. Run it in
CI and after every PMD upgrade. Validated on PMD 7.27.0: Basic Integrity 28 expected,
28 reported; SIL 2 39 expected, 39 reported.
