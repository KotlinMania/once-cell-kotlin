# Immediate Actions - High-Value Files

Based on AST analysis, here are the concrete next steps.

## Summary

- **Files Present:** 5/18 (27.8%)
- **Function parity:** 75/169 matched (target 181) — 44.4%
- **Class/type parity:** 16/24 matched (target 34) — 66.7%
- **Combined symbol parity:** 91/193 matched (target 215) — 47.2%
- **Average inline-code cosine:** 0.44 (function body across 4 matched files)
- **Average documentation cosine:** 0.23 (doc text across 4 matched files)
- **Cheat-zeroed Files:** 1
- **Critical Issues:** 5 files with <0.60 function similarity

## Priority 1: Fix Incomplete High-Dependency Files

No incomplete high-dependency files detected.

## Priority 2: Port Missing High-Value Files

Critical missing files (>10 dependencies):

No missing high-value files detected.

## Detailed Work Items

Every matched file is listed below with function and type symbol parity.

### 1. once_cell.lib

- **Target:** `sync.Lib [STUB]`
- **Similarity:** 0.00
- **Dependents:** 0
- **Priority Score:** 12810.0
- **Functions:** 23/24 matched (target 62)
- **Missing functions:** `_dummy`
- **Types:** 4/4 matched (target 13)
- **Missing types:** _none_

### 2. once_cell.imp_std

- **Target:** `imp.ImpStd`
- **Similarity:** 0.38
- **Dependents:** 0
- **Priority Score:** 12306.2
- **Functions:** 18/19 matched (target 28)
- **Missing functions:** `init`
- **Types:** 4/4 matched (target 5)
- **Missing types:** _none_
- **Tests:** 5/6 matched

### 3. once_cell.race

- **Target:** `race.Race`
- **Similarity:** 0.47
- **Dependents:** 0
- **Priority Score:** 12205.3
- **Functions:** 16/17 matched (target 71)
- **Missing functions:** `_dummy`
- **Types:** 5/5 matched (target 12)
- **Missing types:** _none_

### 4. once_cell.imp_pl

- **Target:** `imp.ImpPl`
- **Similarity:** 0.42
- **Dependents:** 0
- **Priority Score:** 1305.8
- **Functions:** 11/11 matched (target 13)
- **Missing functions:** _none_
- **Types:** 2/2 matched (target 3)
- **Missing types:** _none_
- **Tests:** 1/1 matched

### 5. once_cell.imp_cs

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

