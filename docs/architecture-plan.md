# Architecture plan

See the later [project assessment](project-assessment.md) for verified fixes and the current proposed priorities.

This plan separates correctness gaps from structural improvements. Each phase should keep `./gradlew build` green and
add functional coverage before changing the supported task graph.

## 1. Define and enforce the support contract

The current contract and its enforced compatibility matrix are documented in
[the type-resolution support contract](support-contract.md). Android application/library coverage belongs to phase 3;
custom KMP topology coverage belongs to phase 2.

Create a small compatibility matrix covering JVM, Android JVM, and KMP JVM/JS/native projects. If compatibility with the
removed standalone Kotlin/JS plugin remains a goal, test it in a separately version-pinned legacy fixture rather than the
current Kotlin fixture build. For each project type, cover main, test, and custom compilations where the Kotlin plugin
exposes them. A supported type-resolved task must:

- analyze exactly the compilation's source hierarchy;
- resolve project and external dependency symbols;
- inherit relevant compiler options and friend paths;
- emit no Kotlin compiler-error summary; and
- participate once in `check` and the root `detekt` lifecycle.

Turn the current JS compiler-error expectations into failing regression tests, then fix the classpath and compiler-option
wiring until those warnings disappear. Add an external-dependency finding for JS and native, analogous to the JVM
fixture, so successful syntax-only analysis cannot satisfy the tests.

Run functional fixtures from isolated temporary project directories instead of writing Gradle, Kotlin, and compilation
state into `src/test/resources`. Keep dependency caches shared for runtime, but prevent tests from depending on artifacts
left by an earlier test or local invocation.

## 2. Replace the parallel task topology

This topology is implemented by the internal compilation adapter. Task names use compilation-then-target order (for
example, `detektMainJvm` and `detektTestJs`), allowing upstream JVM tasks to be reused while the plugin registers only
missing JS/native tasks. The root `detekt` task is source-empty and aggregates every compilation task exactly once.
Functional coverage includes custom JVM/JS KMP compilations and a twice-run, warning-rejecting configuration-cache test.

Introduce one internal compilation-to-detekt adapter rather than independently registering a second family of tasks.
Reuse and configure upstream type-resolved tasks for JVM and Android when they exist; register tasks only for unsupported
JS/native compilations. Keep one documented naming order and one aggregation path. Make the root `detekt` entry point a
true lifecycle task (or empty the upstream plain task's sources) whenever per-compilation tasks exist, so a clean JVM
project is not analyzed a second time without type resolution.

Provider-based wiring now carries source directories, language/API versions, opt-ins, free compiler arguments, JVM
classpath, friend paths, JVM target, no-JDK mode, and multiplatform mode without realizing compile tasks during
configuration. Preserve that wiring during the topology replacement, add explicit API mode, and add a
configuration-cache functional test that runs twice and rejects configuration warnings.

Acceptance criterion: a KMP JVM/JS fixture exposes and runs one type-resolved analysis task per compilation through the
root lifecycle, while upstream source-set or baseline tasks remain available only when they serve a distinct purpose.

## 3. Make Android and Compose behavior explicit

This phase is implemented for AGP 9.3 projects using built-in Kotlin. Repository-owned application and library fixtures
cover both plugin application orders, debug/release production variants, and debug unit/instrumented-test components.
The root `detekt` lifecycle is source-empty and reuses upstream Android `detektMain` and `detektTest` aggregation rather
than recreating AGP's variant model.

Compose configuration and the Compose rules dependency are enabled only when the JetBrains or Kotlin Compose compiler
plugin is present, or when an Android application/library explicitly enables `android.buildFeatures.compose`. Applying
an Android plugin by itself no longer opts a project into Compose rules.

Add minimal Android library and application TestKit fixtures, including one non-Compose project and one Compose project.
Verify variant and nested-test task aggregation, source/classpath accuracy, and plugin application order.

Stop treating every Android project as Compose. Detect the Compose compiler plugin or Android Compose feature state, and
add Compose configuration and dependencies only when Compose is enabled. Preserve support for both plugin application
orders and document the exact detection contract.

## 4. Separate static configuration from generated overrides

Implemented: immutable namespaced base/Compose resources, structured YAML assembly, and ordered project overrides via
`detektConfig.config`. A cacheable `generateDetektConfig` task produces `build/detekt/config.yml`; analysis consumes its
output with an inferred task dependency. Nested maps retain siblings, while later lists/scalars replace earlier values.
See [the README](../README.md#additional-configuration) for precedence and the public API.

Parsed-YAML tests retain control-character and literal-placeholder regressions. Functional coverage verifies nested
project overrides, configuration-cache reuse, build-cache restoration, and invalidation after a project-file edit.

## 5. Harden the public and release boundaries

- Decide whether plugin and rules versions are intentionally locked together. Then use one typed build-time source and
  generate any runtime properties resource from it.
- Add plugin display name, description, tags, publication POM metadata, and an artifact-consumption test that uses a
  temporary Maven repository instead of composite-build substitution.
- Resolve `InjectConstructorParameterOrder` annotations by supported fully qualified names, or make those names
  configurable, so an unrelated annotation named `Inject` is not reported.
- Add user-facing rule documentation and test the published service-loader artifact.
- Keep tag publication gated on `check`, and add artifact/signing policy if distribution moves beyond GitHub Packages.
