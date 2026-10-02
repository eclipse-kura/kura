#!/bin/sh
# KJCS PMD self-test. Runs both rulesets of record on the fixtures in src/ and compares
# every reported (file, line, rule) with the "expect: <rule>" markers in the fixtures:
#   - kura-java-ruleset.xml (Basic Integrity): every marker except the rules that only
#     the SIL 2 ruleset contains (X10, X13, X14);
#   - kura-java-ruleset-sil2.xml (SIL 2): every marker.
# It also checks that the two rulesets differ only by the SIL 2-only rules.
# Sources are parsed at the language baseline of KJCS (Java SE 8).
# Exit 0 only if both runs match exactly and the rulesets are consistent.
# Run on every build and after every PMD upgrade.
set -eu
HERE=$(cd "$(dirname "$0")" && pwd)
PMD="${PMD_HOME:?set PMD_HOME}/bin/pmd"
for RS in bi sil2; do
  case $RS in bi) F=kura-java-ruleset.xml ;; sil2) F=kura-java-ruleset-sil2.xml ;; esac
  "$PMD" check --no-cache --no-progress --use-version java-1.8 -f csv -R "$HERE/../$F" \
        -d "$HERE/src" -r "$HERE/actual-$RS.csv" >/dev/null 2>&1 || true
done
python3 - "$HERE" <<'PY'
import csv, os, re, sys
import xml.etree.ElementTree as ET
here = sys.argv[1]
NS = "{http://pmd.sourceforge.net/ruleset/2.0.0}"

def rules(f):
    root = ET.parse(os.path.join(here, "..", f)).getroot()
    out = {}
    for r in root.iter(NS + "rule"):
        key = r.get("name") or r.get("ref").split("/")[-1]
        out[key] = ET.tostring(r)
    return out

bi, sil2 = rules("kura-java-ruleset.xml"), rules("kura-java-ruleset-sil2.xml")
ok = True
sil2_only = sorted(set(sil2) - set(bi))
if sorted(k.split("-")[1] for k in sil2_only) != ["X10", "X13", "X14"] or set(bi) - set(sil2):
    print("RULESET MISMATCH: SIL 2-only rules", sil2_only, "; BI-only rules", sorted(set(bi) - set(sil2)))
    ok = False
for k in sorted(set(bi) & set(sil2)):
    if bi[k] != sil2[k]:
        print("RULESET MISMATCH: rule differs between the two rulesets:", k); ok = False

exp = set()
for root, _, files in os.walk(os.path.join(here, "src")):
    for f in files:
        for n, line in enumerate(open(os.path.join(root, f)), 1):
            m = re.search(r"expect:\s*([\w\-, ]+)", line)
            if m:
                for r in m.group(1).split(","):
                    exp.add((f, n, r.strip()))

for rs, names in (("bi", set(bi)), ("sil2", set(sil2))):
    want = {e for e in exp if e[2] in names}
    act = set()
    for row in csv.DictReader(open(os.path.join(here, "actual-%s.csv" % rs))):
        act.add((os.path.basename(row["File"]), int(row["Line"]), row["Rule"]))
    missing, extra = sorted(want - act), sorted(act - want)
    for m in missing: print(rs.upper(), "MISSING (rule did not fire):", *m)
    for e in extra:   print(rs.upper(), "UNEXPECTED (rule fired):   ", *e)
    print("%-4s expected %d, reported %d, missing %d, unexpected %d" % (rs.upper(), len(want), len(act), len(missing), len(extra)))
    ok = ok and not missing and not extra
unknown = {e[2] for e in exp} - set(sil2)
if unknown:
    print("MARKERS NAMING NO RULE:", sorted(unknown)); ok = False
sys.exit(0 if ok else 1)
PY
