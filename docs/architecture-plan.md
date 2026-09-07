# Architecture plan

Status refreshed on 2026-09-07 against commit `b4d5a88`. Completed milestones below are present in committed code;
this status refresh is uncommitted for review. See [the project assessment](project-assessment.md) for remaining risks
and follow-up acceptance criteria. Each implementation step should remain independently reviewable and keep
`./gradlew build` green.

| Milestone | Current status |
| --- | --- |
| 1. Support contract | Implemented for the documented matrix; additional platforms and fidelity cases remain outside that contract. |
| 2. Compilation task topology | Implemented for analysis; unified baseline generation and root CLI-option propagation remain open. |
| 3. Android and Compose | Implemented for AGP 9.3 built-in Kotlin; Android cache and disabled-integration edge cases remain open. |
| 4. Configuration composition | Implemented, with cache invalidation and ordered-override regression coverage. |
| 5. Public and release boundaries | Partially implemented: isolated published Kotlin/JVM consumer coverage landed in `b4d5a88`; standalone rules, compatibility policy, and release metadata remain open. |

After these milestones, dependency-resolution hardening now distinguishes target-only dependencies from unexpected
failures. KMP project-dependency coverage also exercises `expect`/`actual` APIs. This completes part of assessment
priority 1, not its entire analysis-fidelity proposal.

## Recommended next reviewable step

Align baseline generation with compilation analysis, starting with **JVM source-filter parity**. The adapter replaces
analysis sources with filtered Kotlin source sets but does not apply that wiring to upstream baseline generators.
The existing baseline round trip proves suppression for ordinary shared sources; it does not prove that generation
honors those filters. This is a code-inspection gap, not a newly reproduced failure.

The first reviewable change should:

- Add a checked-in JVM fixture with included syntax/type-dependent findings and an excluded file containing a finding.
- Assert exact analysis findings before generation and exact baseline IDs afterward, proving the excluded file is absent.
- Make the corresponding JVM baseline task use the same filtered compilation sources if the regression exposes a mismatch.
- Consume the baseline, prove only existing included findings are suppressed, and prove an additional finding still fails.

Keep fixture inputs unchanged, with generated baselines under `build/`. Run the focused test and `./gradlew build`.
Then extend the shared compilation model to compiler inputs and JS/native baseline generators, with deterministic output
naming and round-trip coverage. Root baseline aggregation and `--auto-correct` propagation remain separate review steps.

The previously recommended published-consumer slice landed in `b4d5a88`: `PublishedConsumerProjectTest` publishes the
marker, plugin, and rules into a temporary Maven repository, asserts an exact packaged custom-rule finding, and passes
with corrected source. It uses neither composite substitution nor the user's Maven local repository. No-Kotlin-plugin
loading policy, standalone rules consumption, toolchains, compatibility matrices, and publication metadata remain open.

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
- Implemented: a Kotlin/JVM artifact-consumption test using a temporary Maven repository instead of composite-build
  substitution, including packaged ruleset discovery.
- Add plugin display name, description, tags, and publication POM metadata.
- Resolve `InjectConstructorParameterOrder` annotations by supported fully qualified names, or make those names
  configurable, so an unrelated annotation named `Inject` is not reported.
- Add user-facing rule documentation and test the published service-loader artifact.
- Keep tag publication gated on `check`, and add artifact/signing policy if distribution moves beyond GitHub Packages.
