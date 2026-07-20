# Architecture plan

This plan separates correctness gaps from structural improvements. Each phase should keep `./gradlew build` green and
add functional coverage before changing the supported task graph.

## 1. Define and enforce the support contract

The current contract and its enforced compatibility matrix are documented in
[the type-resolution support contract](support-contract.md). Android remains provisional until phase 3 adds
repository-owned fixtures; custom KMP topology coverage belongs to phase 2.

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

Add minimal Android library and application TestKit fixtures, including one non-Compose project and one Compose project.
Verify variant and nested-test task aggregation, source/classpath accuracy, and plugin application order.

Stop treating every Android project as Compose. Detect the Compose compiler plugin or Android Compose feature state, and
add Compose configuration and dependencies only when Compose is enabled. Preserve support for both plugin application
orders and document the exact detection contract.

## 4. Separate static configuration from generated overrides

Replace full-file string concatenation and placeholder substitution with a configuration assembly boundary that keeps
the static base/Compose files immutable and generates only extension-owned values. First prove detekt's multi-file merge
semantics; if nested maps cannot be safely overlaid, use a YAML writer rather than hand-built indentation.

Add tests that parse the final YAML, reject unresolved or duplicate placeholders, cover empty lists and quoting, and
allow consumers to append a project-owned config file without copying this repository's base configuration.

## 5. Harden the public and release boundaries

- Decide whether plugin and rules versions are intentionally locked together. Then use one typed build-time source and
  generate any runtime properties resource from it.
- Add plugin display name, description, tags, publication POM metadata, and an artifact-consumption test that uses a
  temporary Maven repository instead of composite-build substitution.
- Resolve `InjectConstructorParameterOrder` annotations by supported fully qualified names, or make those names
  configurable, so an unrelated annotation named `Inject` is not reported.
- Add user-facing rule documentation and test the published service-loader artifact.
- Keep tag publication gated on `check`, and add artifact/signing policy if distribution moves beyond GitHub Packages.
