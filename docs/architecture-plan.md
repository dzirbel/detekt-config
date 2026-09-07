# Architecture plan

Status refreshed on 2026-09-07 against commit `1796d7d`. Completed milestones below are present in committed code;
this status refresh is uncommitted for review. See [the project assessment](project-assessment.md) for remaining risks
and follow-up acceptance criteria. Each implementation step should remain independently reviewable and keep
`./gradlew build` green.

| Milestone | Current status |
| --- | --- |
| 1. Support contract | Implemented for the documented matrix; additional platforms and fidelity cases remain outside that contract. |
| 2. Compilation task topology | Implemented for analysis; unified baseline generation and root CLI-option propagation remain open. |
| 3. Android and Compose | Implemented for AGP 9.3 built-in Kotlin; Android cache and disabled-integration edge cases remain open. |
| 4. Configuration composition | Implemented, with cache invalidation and ordered-override regression coverage. |
| 5. Public and release boundaries | Open; recommended next step is an isolated published-artifact consumer test. |

After these milestones, dependency-resolution hardening now distinguishes target-only dependencies from unexpected
failures. KMP project-dependency coverage also exercises `expect`/`actual` APIs. This completes part of assessment
priority 1, not its entire analysis-fidelity proposal.

## Recommended next reviewable step

Add a functional consumer test that publishes the plugin marker, plugin, and rules to a temporary Maven repository,
then applies the plugin in an isolated Kotlin/JVM build without `includeBuild` or rules dependency substitution. Assert
an exact custom-rule finding so the test proves packaged ruleset discovery as well as plugin/dependency resolution.
Use a repository under the test build directory; no remote publication is needed.

Keep this step focused on the supported Kotlin/JVM consumer. Track the previously observed no-Kotlin-plugin loading
failure separately, and decide its support policy before changing runtime dependencies. Publication metadata,
toolchains, compatibility matrices, and baseline task redesign should be separate review steps.

## 1. Define and enforce the support contract

The current contract and its enforced compatibility matrix are documented in
[the type-resolution support contract](support-contract.md). Android application/library coverage belongs to phase 3;
custom KMP topology coverage belongs to phase 2.

The fixtures cover JVM main/test/custom compilations, KMP JVM/JS main/test/custom compilations, native main/test,
and Android production/test variants. They assert compilation sources and outputs, type-dependent external-dependency
findings, compiler-error-free analysis, and lifecycle participation. Fixtures run in isolated temporary directories
under `plugin/build/test-fixtures`, sharing dependency caches while isolating project state and the fixture build cache.

Remaining support cases include native custom compilations, multiple JVM targets, generated sources, and the Android
edge cases listed in the assessment. Standalone Kotlin/JS remains unsupported with the current Kotlin version.

## 2. Replace the parallel task topology

This topology is implemented by the internal compilation adapter. Task names use compilation-then-target order (for
example, `detektMainJvm` and `detektTestJs`), allowing upstream JVM tasks to be reused while the plugin registers only
missing JS/native tasks. The root `detekt` task is source-empty and aggregates every compilation task exactly once.
Functional coverage includes custom JVM/JS KMP compilations and a twice-run, warning-rejecting configuration-cache test.

Provider-based wiring carries filtered source sets, language/API versions, opt-ins, free compiler arguments, JVM
classpath, friend paths, JVM target, no-JDK mode, and multiplatform mode. Plugin-created main tasks also inherit explicit
API mode. Baseline generators do not yet share this adapter, and root `--auto-correct` propagation remains open; see
assessment priority 2.

## 3. Make Android and Compose behavior explicit

This phase is implemented for AGP 9.3 projects using built-in Kotlin. Repository-owned application and library fixtures
cover both plugin application orders, debug/release production variants, and debug unit/instrumented-test components.
The root `detekt` lifecycle is source-empty and reuses upstream Android `detektMain` and `detektTest` aggregation rather
than recreating AGP's variant model.

Compose configuration and the Compose rules dependency are enabled only when the JetBrains or Kotlin Compose compiler
plugin is present, or when an Android application/library explicitly enables `android.buildFeatures.compose`. Applying
an Android plugin by itself no longer opts a project into Compose rules.

Remaining coverage includes Android configuration-cache reuse, Compose feature-only activation independent of the
compiler plugin, and disabled or Kotlin-free Android integration. These are follow-ups, not covered support promises.

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
