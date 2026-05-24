# Immediate Actions - High-Value Files

Based on AST analysis, here are the concrete next steps.

## Summary

- **Files Present:** 1/5 (20.0%)
- **Function parity:** 13/100 matched (target 24) — 13.0%
- **Class/type parity:** 2/16 matched (target 11) — 12.5%
- **Combined symbol parity:** 15/116 matched (target 35) — 12.9%
- **Average inline-code cosine:** 0.15 (function body across 1 matched files)
- **Average documentation cosine:** 0.56 (doc text across 1 matched files)
- **Cheat-zeroed Files:** 0
- **Critical Issues:** 1 files with <0.60 function similarity

## Priority 1: Fix Incomplete High-Dependency Files

No incomplete high-dependency files detected.

## Priority 2: Port Missing High-Value Files

Critical missing files (>10 dependencies):

No missing high-value files detected.

## Detailed Work Items

Every matched file is listed below with function and type symbol parity.

### 1. lib

- **Target:** `sync.Lib`
- **Similarity:** 0.15
- **Dependents:** 0
- **Priority Score:** 132808.5
- **Functions:** 13/24 matched
- **Missing functions:** `default`, `fmt`, `clone`, `clone_from`, `eq`, `from`, `take`, `force_mut`, `deref`, `deref_mut`, `_dummy`
- **Types:** 2/4 matched (target 11)
- **Missing types:** `Void`, `Target`

## Success Criteria

For each file to be considered "complete":
- **Similarity ≥ 0.85** (Excellent threshold)
- All public APIs ported
- All tests ported
- Documentation ported
- port-lint header present

