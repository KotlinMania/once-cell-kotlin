import { createRequire } from 'node:module';
import { existsSync, readFileSync, writeFileSync } from 'node:fs';
import { createHash } from 'node:crypto';
import { resolve } from 'node:path';
import assert from 'node:assert/strict';

const repoRoot = resolve(import.meta.dirname, '..');
const require = createRequire(import.meta.url);
const sha256 = (filePath) => createHash('sha256').update(readFileSync(filePath)).digest('hex');

const canonicalPublicNames = [
  'OnceCell',
  'Lazy',
  'StringOnceCell',
  'IntOnceCell',
  'OnceBool',
  'OnceNonZeroUsize',
  'OnceBox',
  'OnceRef',
  'SetResult',
  'TryInsertResult',
  'StringSetResult',
  'StringTryInsertResult',
  'IntSetResult',
  'IntTryInsertResult',
];

const surfaces = [];

const jsPath = resolve(repoRoot, 'build/compileSync/js/test/testDevelopmentExecutable/kotlin/once-cell-kotlin.js');
if (existsSync(jsPath)) {
  const mod = require(jsPath);
  const topLevelKeys = Object.keys(mod);
  const publicNamesPresent = canonicalPublicNames.filter((name) => Object.hasOwn(mod, name));
  assert.equal(publicNamesPresent.length, 0, 'Unexpected fabricated top-level JS exports');
  assert.deepEqual(topLevelKeys, ['$_$'], 'Expected only compiler-internal linkage table $_$');
  surfaces.push({
    target: 'js',
    path: jsPath,
    sha256: sha256(jsPath),
    runtime_import: true,
    top_level_keys: topLevelKeys,
    compiler_internal_symbol_count: Object.keys(mod.$_$).length,
    canonical_public_names_present: publicNamesPresent,
    dts_emitted: existsSync(jsPath.replace(/\.js$/, '.d.ts')),
    classification: 'Kotlin compiler-internal linkage ($_$); KLIB library without @JsExport or plain-JS public entry',
  });
}

for (const [target, relPath] of [
  ['wasmJs', 'build/compileSync/wasmJs/test/testDevelopmentExecutable/kotlin/once-cell-kotlin-test.wasm'],
  ['wasmWasi', 'build/compileSync/wasmWasi/test/testDevelopmentExecutable/kotlin/once-cell-kotlin-test.wasm'],
]) {
  const fullPath = resolve(repoRoot, relPath);
  if (!existsSync(fullPath)) continue;
  const wasmBytes = readFileSync(fullPath);
  const module =
    target === 'wasmJs'
      ? new WebAssembly.Module(wasmBytes, { builtins: ['js-string'], importedStringConstants: "'" })
      : new WebAssembly.Module(wasmBytes);
  const exportsList = WebAssembly.Module.exports(module);
  const importsList = WebAssembly.Module.imports(module);
  const importModules = [...new Set(importsList.map((i) => i.module))].sort();
  assert(exportsList.some((entry) => entry.name === 'startUnitTests'));
  assert(!exportsList.some((entry) => canonicalPublicNames.includes(entry.name)));
  surfaces.push({
    target,
    path: fullPath,
    sha256: sha256(fullPath),
    binary_exports: exportsList,
    import_modules: importModules,
    import_count: importsList.length,
    instantiated: false,
    classification:
      target === 'wasmJs'
        ? 'Kotlin/WasmJS test executable importing JS host bridge and exporting startUnitTests only; not an external plain-JS/TS library API'
        : 'Kotlin/WasmWASI standalone WASI (wasi_snapshot_preview1) test binary exporting memory and startUnitTests only; no JS/TS surface',
  });
}

const outPath = process.argv[2];
const report = {
  node: process.version,
  repository: 'KotlinMania/once-cell-kotlin',
  purpose: 'Read-only external availability and ABI surface inspection of emitted JS, WasmJS, and WasmWASI artifacts',
  surfaces,
  external_plain_js_value_error_mutation_wrong_type_cases_executed: 0,
  supported_external_plain_js_contract_pass: false,
  wasi_js_surface_required: false,
  gap: 'once-cell-kotlin is a pure KMP KLIB library with generic container types (OnceCell<T>, Lazy<T>, OnceBox<T>, OnceRef<T>) and no @JsExport/@WasmExport or plain-JS/TS product binary configuration. Independent KLIB consumer tests execute on jsNodeTest, wasmJsNodeTest, and wasmWasiNodeTest separately from plain-JS/TS untyped callers.',
};

if (outPath) {
  writeFileSync(resolve(outPath), JSON.stringify(report, null, 2) + '\n');
}
console.log(JSON.stringify(report, null, 2));
