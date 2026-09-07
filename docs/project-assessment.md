# Project assessment — 2026-09-05

Originally reviewed from commit `263515e` on 2026-09-05. Status refreshed on 2026-09-07 against `b4d5a88`.
The original fixes and subsequent implementation batches are committed; this documentation refresh is uncommitted.
Historical validation is retained below and is distinguished from the current checkout verification.

## Current implementation status

| Area | Status and evidence |
| --- | --- |
| Initial correctness fixes | Landed in `7b0309e`; all five fixes below remain represented in the implementation/tests. |
| Cacheable configuration composition | Landed in `02ce648`; `71d526e` adds exact cache-outcome, policy-edit, and override-order checks. |
| Baseline consumption | `4f9ff52` proves JVM/JS suppression, visibility of new findings, and restoration after baseline edits. Compilation-specific JS/native generation remains open. |
| Dependency-resolution diagnostics | `77558ce` warns for target-only variants and fails on unexpected resolution errors, with missing artifact/module/transitive and incompatible-JVM tests. |
| KMP project dependencies | Current coverage verifies a type-dependent finding through a producer's `expect`/`actual` API on JS/native, including the producer JVM artifact. |
| Fixture orchestration | `1796d7d` simplifies isolated fixture setup using checked-in settings; it still copies/configures the shared fixture tree. |
| Published Kotlin/JVM consumer | Landed in `b4d5a88`: temporary Maven publication of marker/plugin/rules, exact custom-rule finding, and corrected-source success without composite substitution. |
| Remaining release work, baseline task unification, rule semantics, CI/performance | Still open, except for the implemented slices noted above. |

**Recommended next step:** align baseline generation with compilation analysis, beginning with a JVM source-filter
regression and any required source-wiring fix. Assert exact baseline contents, suppression, and visibility of new
findings using checked-in fixture inputs. This is the first bounded slice of priority 2; JS/native generators and root
lifecycle options follow separately. The previously recommended published-consumer test is now committed.
See [the architecture plan](architecture-plan.md#recommended-next-reviewable-step) for scope and acceptance criteria.

## Overall assessment

The core is small and understandable: one compilation adapter, an extension with two policy options plus ordered YAML
overrides, bundled configuration, and one custom rule. Functional coverage is unusually broad for the implementation's size. It checks actual
type-dependent findings, compilation outputs, JVM/JS/native/Android tasks, Compose detection, and configuration-cache
reuse. Reusing upstream JVM/Android tasks is a sound direction.

The next investment should be analysis fidelity and consumer compatibility. A successful analysis task is not sufficient
evidence that every relevant symbol, source filter, baseline, or command-line option was honored. Several defects below
survived the existing green suite. Adding platforms or rules before tightening these boundaries would expand the support
burden without resolving that uncertainty.

## Straightforward fixes implemented

| Defect | Previous behavior and evidence | Change and coverage |
| --- | --- | --- |
| Source-set filters discarded | The adapter rebuilt file trees from source directories. An excluded `Excluded.kt` compiled successfully but still failed `detektMain` on `println`. | Use each Kotlin `SourceDirectorySet` directly, preserving its filters. A TestKit regression checks compilation and analysis together. |
| KMP/custom tests miss test exemptions | `commonTest` and JVM/JS `integrationTest` fixtures reported `MagicNumber` despite the intended test exemption. | Add `**/*Test/**` to the default globs. Existing clean JVM/JS lifecycle and configuration-cache tests now include non-exempt numeric literals in these directories. Correct the extension's documentation from regex to glob. |
| JS/native tasks ignore baselines | Newly registered tasks had a null `baseline` even after `detekt.baseline` was configured. | Give plugin-created tasks a lazy baseline convention while retaining upstream JVM/Android selection. Cover JS/native properties and a real shared-code baseline consumed by JS analysis. |
| Extension values can corrupt YAML | Raw multiline/control characters were interpolated into single quotes; the parser rejected a control character. Sequential substitution could also rewrite placeholder-like text inside a test path. | The original escaping fix has been superseded by structured YAML composition with SnakeYAML as a runtime dependency. Parsed-YAML tests retain control-character, literal-placeholder, empty-list, and duplicate-key regressions. |
| Config generation silently targets a temporary file | Upstream derives the output from the last analysis config, which here is an already-existing temporary text resource. The regression observed `build/tmp/resource/string….txt`. | Default `detektGenerateConfig.configFile` to the root project's `config/detekt/detekt.yml`. Verify the task property and real file generation. Task-level overrides remain possible. |

The first targeted run failed on all five defects; the focused suite passed after the fixes. Documentation now links the
support contract and this assessment, and publishing instructions identify the actual shared version property.

## Proposed larger changes, in priority order

### 1. Make partial analysis explicit and measurable

**Priority: high; partially implemented.** The non-JVM dependency projection still uses a lenient artifact view in
[`DetektCompilationAdapter.kt`](../plugin/src/main/kotlin/io/github/dzirbel/DetektCompilationAdapter.kt).
Its failures are now checked by `AnalysisResolution.kt`: dependencies whose candidate library variants are all non-JVM
are skipped with a warning; other failures fail analysis-classpath resolution. Tests cover JS/native diagnostics and
configuration-cache reuse, missing artifacts and repair, missing modules/transitive dependencies, and incompatible JVM
variants. Classification reflects over Gradle's internal structured variant failure and fails closed if that structure
changes; compatibility across Gradle versions remains untested.
[Gradle artifact-view documentation](https://docs.gradle.org/current/userguide/artifact_views.html).

Other fidelity risks visible in the adapter:

- The complete detekt CLI classpath doubles as a non-JVM analysis classpath. It supplies the desired standard library,
  but also exposes implementation dependencies that the consumer never declared.
- Associated JVM outputs are selected by compilation name across every JVM target. There is no explicit mapping between
  a non-JVM target and its intended JVM counterpart when a project has multiple JVM targets.
- Fixtures now verify coroutine APIs and a KMP project dependency with an `expect`/`actual` API. Target-only
  dependency diagnostics are covered, but fidelity when source uses target-only APIs, generated sources, and multiple
  JVM targets remains untested.
- Compiler-error summaries are rejected by fixture assertions; the plugin does not provide an equivalent consumer-facing
  analysis-health gate. A clean task outcome can therefore overstate the scope of successful analysis.

**Remaining implementation:** introduce a small analysis-input model that records sources, compiler options, selected
artifacts, associated outputs, and unresolved components. Extend the existing resolution diagnostics into an analysis-health
contract, including compiler-error summaries. Allow an explicit JVM-counterpart mapping when the choice is ambiguous.
Investigate a minimal standard-library classpath instead of adding the entire CLI runtime; keep that change behind real dependency tests.

**Remaining acceptance:** add multiple-JVM-target, generated-source, and target-only-API fidelity fixtures. Preserve
the existing excluded-source, KMP project dependency, `expect`/`actual`, and resolution-failure tests. Expected limitations
must be observable; unexpected resolution failures must fail verification. Preserve the existing supported-subset contract rather than
claiming that JVM projection fully models native or JS semantics.

### 2. Treat analysis, baselines, and lifecycle options as one task model

**Priority: high.** Analysis now honors a configured JS/native baseline, but the adapter still only creates `Detekt`
tasks. It has no matching JS/native compilation baseline generators. The plain `detektBaseline` task still uses
upstream default source roots and syntax-only analysis, while the root `detekt` task aggregates type-resolved
compilations. Existing JVM baseline task wiring is also not updated when this adapter changes analysis sources.

`KmpBaselineProjectTest` now generates an upstream JVM baseline and proves selective shared-code suppression on both
JVM and JS, including new findings and baseline-only edits with configuration-cache reuse. This strengthens consumption
coverage; it does not implement JS/native generators or validate native baseline round trips.

There is another compatibility gap in the source-empty lifecycle: upstream's `--auto-correct` is a property of the
selected `Detekt` task. Setting it on root `detekt` does not forward it to the dependent compilation tasks. Setting
`detekt.autoCorrect` in the extension or selecting compilation tasks directly is the current route to those properties.
The option gap follows from the task wiring and upstream property implementation; it was not separately exercised in
this assessment. See [upstream tasks and options](https://detekt.dev/docs/gettingstarted/gradle/) and
[baseline guidance](https://detekt.dev/docs/next/introduction/baseline/).

**Implementation:** derive analysis and baseline tasks from the same compilation specification, including filters,
classpath, compiler flags, and friend paths. Define deterministic baseline naming and whether shared-code findings use a
merged baseline or separate per-compilation files. Define which root options propagate and test them explicitly; preserve
the public root task name.

**Acceptance:** generating then consuming a baseline suppresses both syntax and type-dependent findings on JVM/JS/native;
new findings still fail; baseline tasks respect source exclusions; root auto-correction changes an intentionally
misformatted fixture; baseline outputs never collide across targets.

### 3. Test the real consumer and publication boundary

**Priority: high; partially implemented.** `PublishedConsumerProjectTest` now publishes the actual plugin marker, plugin,
and rules publications into a temporary Maven repository. Its isolated Kotlin/JVM consumer exclusively resolves this
project's groups there, asserts an exact `InjectConstructorParameterOrder` finding, and passes with corrected source.
This validates marker/POM resolution and packaged ruleset discovery through the config plugin for the current JVM setup.
The other functional fixtures still use a composite build and rules dependency substitution.
`TasksTest` also puts the Kotlin Gradle plugin on every test's classpath.

During the original assessment, an isolated consumer applying only `io.github.dzirbel.detekt-config` failed even on
`help` with `org/jetbrains/kotlin/gradle/plugin/KotlinBasePlugin`. The existing `no other plugins` unit test passes in its richer
classpath. This does not invalidate the tested Kotlin fixtures, but it does show that the unit test overstates standalone
plugin applicability. The failure comes through the current detekt integration; adding an unconditional runtime Kotlin
Gradle plugin dependency would risk forcing a compiler-plugin version onto consumers.

The original assessment observed Java 21 class files (major version 65) in both locally built artifacts. Neither module
declares a toolchain or explicit target. That is the observed build output, not a documented compatibility promise. The catalog pins one
Kotlin/AGP combination; the `kotlin-dsl` plugin obtains its compilation dependency version from the Gradle distribution.

**Remaining implementation:** test the published rules artifact independently of the config plugin. Decide whether
projects with no Kotlin plugin are supported; provide a clear diagnostic or fix
the upstream loading boundary accordingly. Declare build toolchains and minimum runtime targets. Test a small deliberate
Gradle/Kotlin/AGP matrix, including task-level compiler-option overrides, Android cache reuse, and plugin application
orders. Add publication metadata, sources artifacts, and a tag/version consistency check.

**Acceptance:** a fresh consumer resolves the plugin and rules solely from the temporary repository, the packaged
ruleset loads through `ServiceLoader`, supported versions behave identically, and unsupported combinations produce useful
diagnostics. Verify the rules artifact independently of the config plugin. Keep plugin/rules versions coupled for now
unless there is a concrete release need to separate them.

### 4. Make configuration composition a public, cacheable boundary

**Implemented:** `GenerateDetektConfig` is cacheable and writes stable `build/detekt/config.yml` output. Namespaced
base/Compose resources, extension overrides, and ordered `detektConfig.config` files are merged with structured YAML;
nested maps preserve siblings and later lists/scalars replace earlier values. SnakeYAML is a runtime dependency.
See [the architecture milestone](architecture-plan.md#4-separate-static-configuration-from-generated-overrides) and
[the public configuration contract](../README.md#additional-configuration).

`ConfigFileTest` covers parsed YAML and malformed inputs. `ConfigCacheProjectTest` asserts exact findings and task
outcomes for unchanged reuse, build-cache restoration, file/extension edits, and reversal of override-file order.
The original configuration-composition proposal is complete; retain these regressions when changing the public API.

### 5. Define custom-rule annotation semantics before expanding the ruleset

**Priority: medium.** `InjectConstructorParameterOrder` matches only the annotation's short name. By inspection,
`@unrelated.Inject` is treated as dependency injection, while `import javax.inject.Inject as DI` followed by `@DI` is
missed. Existing tests cover fully qualified `javax.inject.Inject` but not aliasing, unrelated annotations, or Jakarta.

**Implementation:** define supported annotation identities, including whether Guice and custom annotations are allowed.
Prefer semantic annotation resolution for type-resolved runs, with a documented policy for syntax-only use. A handwritten
import matcher alone would still have trouble with shadowing and type aliases. Document case-sensitive sorting and why
the rule does not automatically reorder constructor parameters, since doing so can change positional-call behavior.

**Acceptance:** test javax/Jakarta annotations, aliases, fully qualified names, unrelated `Inject` annotations, shadowing,
secondary constructors, and standalone packaged ruleset execution.

### 6. Optimize work based on measurements, and strengthen the CI signal

**Priority: medium.** Every adapted analysis task depends on its compile task, and root analysis includes every
compilation. Shared source sets are analyzed for several targets. These choices help correctness but can make native and
large KMP builds expensive. Sibling JVM outputs create additional compilation dependencies. Do not remove these edges
without proving that type-dependent rules still have the required outputs.

**Implementation:** measure clean, warm, and one-file-change task graphs on a larger fixture, separating compiler time,
analysis time, artifact resolution, and configuration. Then introduce explicit target/compilation selection and prune
only redundant work proven unnecessary. Separately, split fast unit/JVM feedback from heavier Android/native fixture
coverage, preserving the full suite as a gate. Each current test copies the complete fixture tree; copying only the
selected project and shared settings is a modest, measurable harness optimization.

CI currently runs on pushes and manual dispatch, with one toolchain per OS. Add pull-request coverage if PRs are part of
the workflow, explicitly provision the required Android SDK, and preserve test reports on failure. The repository's
`build` runs tests and plugin validation but does not run detekt against its own implementation; add independent
self-analysis without creating a composite-build bootstrap cycle. Gradle warnings are globally hidden in fixtures, so
use dedicated warning-sensitive compatibility tests. The refreshed
[`architecture-plan.md`](architecture-plan.md) now tracks completed milestones and remaining work.

**Acceptance:** report measured changes to clean/warm/edit latency and task counts; selected compilation analysis remains
complete; changes in shared code invalidate every required target; PRs exercise a fast gate; release verification still
runs the complete supported matrix.

## Additional support cases to resolve

These are code-review risks, not newly reproduced failures:

- The Android callback clears root sources unconditionally, while upstream can omit its aggregators when Android
  integration is disabled. Cover `detekt.android.disabled`, Kotlin-disabled Android modules, and late configuration so
  an unsupported setup cannot accidentally turn verification into a successful no-op.
- The KMP adapter visits every non-common platform but only treats `KotlinPlatformType.jvm` as JVM. Explicitly decide how
  legacy `androidJvm`, the newer Android KMP plugin, Wasm, and metadata-only projects fit the contract before claiming
  support for them.
- Android Compose feature detection uses reflection and `afterEvaluate`. Cover feature-only activation independently
  from Compose compiler plugin activation and test configuration-cache reuse before redesigning that integration.

## Historical validation — original 2026-09-05 assessment

- Initial `./gradlew build`: successful in 2m 36s; 50 plugin tests passed. Rules tests were initially up to date.
- Regression run before fixes: 20 tests executed, with five expected failures covering the defects above.
- Focused run after fixes: 31 tests passed, including configuration-cache reuse, source filters, file generation, and
  baseline consumption.
- Final `./gradlew build :rules:test --rerun --console=plain`: successful in 2m 42s. All 57 plugin tests and 9 rules
  tests passed with no skips. Both test tasks executed; plugin validation and both artifact builds passed. The rules-test
  task was explicitly forced to run instead of relying on its earlier up-to-date result.
- `git diff --check`: passed. No commits or publication were made.

Validation was performed on the local Linux host. No macOS/Windows run or package publication was performed. The isolated
consumer failure described above has not been rechecked in this refresh; the current supported-fixture suite still does
not cover that case.

## Historical validation — plan refresh at `1796d7d` on 2026-09-07

- `./gradlew build :rules:test --rerun --console=plain`: successful in 2m 41s on Linux. Both test tasks executed:
  69 plugin tests and 9 rules tests passed with no failures, errors, or skips. Artifact assembly and plugin validation
  were up to date; the root configuration cache was reused.
- `git diff --check`: passed. This refresh changes only the architecture plan, assessment, and support contract;
  no implementation changes, commits, or publication were made.
- No new cross-OS or published-consumer validation was performed. The no-Kotlin-plugin failure remains historical
  evidence, not a newly reproduced result.


## Current checkout validation — `b4d5a88`, 2026-09-07

- `./gradlew :plugin:test --tests io.github.dzirbel.PublishedConsumerProjectTest --rerun --console=plain`:
  successful in 19s; the test task executed, with 1 test and no failures, errors, or skips. This rechecked temporary
  publication and both failing/corrected consumer builds. The initial invocation without `--rerun` restored cached results.
- Reviewed the compilation adapter, existing baseline round trip, and published-consumer fixture. JVM baseline
  source-filter parity remains an inspection-based gap to reproduce in the next step.
- `git diff --check`: passed. Only the architecture plan and assessment changed; changes remain uncommitted for review.
- The full build, cross-OS matrix, standalone rules consumer, and no-Kotlin-plugin case were not rerun in this
  documentation-only refresh. Earlier full-suite results above are historical.
