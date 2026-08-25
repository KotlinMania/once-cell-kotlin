# Immediate Actions - High-Value Files

Based on AST analysis, here are the concrete next steps.

## Summary

- **Files Present:** 5/5 (100.0%)
- **Function parity:** 77/78 matched (target 185) — 98.7%
- **Class/type parity:** 16/16 matched (target 34) — 100.0%
- **Combined symbol parity:** 93/94 matched (target 219) — 98.9%
- **Average inline-code cosine:** 0.45 (function body across 5 matched files)
- **Average documentation cosine:** 0.30 (doc text across 5 matched files)
- **Cheat-zeroed Files:** 0
- **Critical Issues:** 5 files with <0.60 function similarity

## Priority 1: Fix Incomplete High-Dependency Files

No incomplete high-dependency files detected.

## Priority 2: Port Missing High-Value Files

Critical missing files (>10 dependencies):

No missing high-value files detected.

## Detailed Work Items

Every matched file is listed below with function and type symbol parity.

### 1. imp_std

- **Target:** `imp.ImpStd`
- **Similarity:** 0.38
- **Dependents:** 0
- **Priority Score:** 12306.2
- **Functions:** 18/19 matched (target 28)
- **Missing functions:** `init`
- **Types:** 4/4 matched (target 5)
- **Missing types:** _none_
- **Tests:** 5/6 matched

### 2. lib

- **Target:** `sync.Lib`
- **Similarity:** 0.44
- **Dependents:** 0
- **Priority Score:** 2805.6
- **Functions:** 24/24 matched (target 64)
- **Missing functions:** _none_
- **Types:** 4/4 matched (target 13)
- **Missing types:** _none_

### 3. race

- **Target:** `race.Race`
- **Similarity:** 0.52
- **Dependents:** 0
- **Priority Score:** 2204.8
- **Functions:** 17/17 matched (target 73)
- **Missing functions:** _none_
- **Types:** 5/5 matched (target 12)
- **Missing types:** _none_

### 4. imp_pl

- **Target:** `imp.ImpPl`
- **Similarity:** 0.42
- **Dependents:** 0
- **Priority Score:** 1305.8
- **Functions:** 11/11 matched (target 13)
- **Missing functions:** _none_
- **Types:** 2/2 matched (target 3)
- **Missing types:** _none_
- **Tests:** 1/1 matched

### 5. imp_cs

- **Target:** `imp.ImpCs`
- **Similarity:** 0.50
- **Dependents:** 0
- **Priority Score:** 805.0
- **Functions:** 7/7 matched
- **Missing functions:** _none_
- **Types:** 1/1 matched
- **Missing types:** _none_

## Success Criteria

For each file to be considered "complete":
- **Similarity ≥ 0.85** (Excellent threshold)
- All public APIs ported
- All tests ported
- Documentation ported
- port-lint header present

