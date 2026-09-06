# Project assessment — 2026-09-05

Reviewed from commit `263515e`. Changes from this assessment are intentionally uncommitted.

## Overall assessment

The core is small and understandable: one compilation adapter, an extension with two options, bundled configuration,
and one custom rule. Functional coverage is unusually broad for the implementation's size. It checks actual
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
| Extension values can corrupt YAML | Raw multiline/control characters were interpolated into single quotes; the parser rejected a control character. Sequential substitution could also rewrite placeholder-like text inside a test path. | Escape control characters and line separators with double-quoted YAML when necessary; replace template tokens in one pass. Parse and round-trip strings, empty lists, and combined base/Compose configuration with duplicate keys forbidden. SnakeYAML is a test-only dependency. |
| Config generation silently targets a temporary file | Upstream derives the output from the last analysis config, which here is an already-existing temporary text resource. The regression observed `build/tmp/resource/string….txt`. | Default `detektGenerateConfig.configFile` to the root project's `config/detekt/detekt.yml`. Verify the task property and real file generation. Task-level overrides remain possible. |

The first targeted run failed on all five defects; the focused suite passed after the fixes. Documentation now links the
support contract and this assessment, and publishing instructions identify the actual shared version property.

## Proposed larger changes, in priority order

### 1. Make partial analysis explicit and measurable

**Priority: high.** The non-JVM dependency projection uses an unqualified lenient artifact view in
[`DetektCompilationAdapter.kt`](../plugin/src/main/kotlin/io/github/dzirbel/DetektCompilationAdapter.kt).
Gradle's leniency suppresses resolution failures generally, including missing artifacts; it does not distinguish an
expected target-only dependency from a broken repository or unavailable JVM variant.
[Gradle artifact-view documentation](https://docs.gradle.org/current/userguide/artifact_views.html).

Other fidelity risks visible in the adapter:

- The complete detekt CLI classpath doubles as a non-JVM analysis classpath. It supplies the desired standard library,
  but also exposes implementation dependencies that the consumer never declared.
- Associated JVM outputs are selected by compilation name across every JVM target. There is no explicit mapping between
  a non-JVM target and its intended JVM counterpart when a project has multiple JVM targets.
- The current fixtures verify coroutine APIs and ordinary shared code, but do not cover real `expect`/`actual`
  declarations, target-only APIs, generated sources, or a dependency on another KMP project.
- Compiler-error summaries are rejected by fixture assertions; the plugin does not provide an equivalent consumer-facing
  analysis-health gate. A clean task outcome can therefore overstate the scope of successful analysis.

**Implementation:** introduce a small analysis-input model that records sources, compiler options, selected artifacts,
associated outputs, and unresolved components. Expose diagnostics and distinguish intentionally unsupported dependencies
from unexpected failures. Allow an explicit JVM-counterpart mapping when the choice is ambiguous. Investigate a minimal
standard-library classpath instead of adding the entire CLI runtime; keep that change behind real dependency tests.

**Acceptance:** add fixtures for `expect`/`actual`, a JVM-compatible KMP project dependency, a target-only dependency,
missing JVM artifacts, multiple JVM targets, and generated/excluded sources. Expected limitations must be observable;
unexpected resolution failures must fail verification. Preserve the existing supported-subset contract rather than
claiming that JVM projection fully models native or JS semantics.

### 2. Treat analysis, baselines, and lifecycle options as one task model

**Priority: high.** Analysis now honors a configured JS/native baseline, but the adapter still only creates `Detekt`
tasks. It has no matching JS/native compilation baseline generators. The plain `detektBaseline` task still uses
upstream default source roots and syntax-only analysis, while the root `detekt` task aggregates type-resolved
compilations. Existing JVM baseline task wiring is also not updated when this adapter changes analysis sources.

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

**Priority: high.** All functional fixtures use a composite build and rules dependency substitution. They validate source
integration well, but cannot prove plugin-marker/POM correctness or the dependencies available to an external consumer.
`TasksTest` also puts the Kotlin Gradle plugin on every test's classpath.

An additional isolated consumer applying only `io.github.dzirbel.detekt-config` failed even on `help` with
`org/jetbrains/kotlin/gradle/plugin/KotlinBasePlugin`. The existing `no other plugins` unit test passes in its richer
classpath. This does not invalidate the tested Kotlin fixtures, but it does show that the unit test overstates standalone
plugin applicability. The failure comes through the current detekt integration; adding an unconditional runtime Kotlin
Gradle plugin dependency would risk forcing a compiler-plugin version onto consumers.

Both locally built artifacts contain Java 21 class files (major version 65), while neither module declares a toolchain or
explicit target. That is the observed build output, not a documented compatibility promise. The catalog pins one
Kotlin/AGP combination; the `kotlin-dsl` plugin obtains its compilation dependency version from the Gradle distribution.

**Implementation:** publish both artifacts and the plugin marker into a temporary Maven repository and consume them
without `includeBuild`. Decide whether projects with no Kotlin plugin are supported; provide a clear diagnostic or fix
the upstream loading boundary accordingly. Declare build toolchains and minimum runtime targets. Test a small deliberate
Gradle/Kotlin/AGP matrix, including task-level compiler-option overrides, Android cache reuse, and plugin application
orders. Add publication metadata, sources artifacts, and a tag/version consistency check.

**Acceptance:** a fresh consumer resolves the plugin and rules solely from the temporary repository, the packaged
ruleset loads through `ServiceLoader`, supported versions behave identically, and unsupported combinations produce useful
diagnostics. Verify the rules artifact independently of the config plugin. Keep plugin/rules versions coupled for now
unless there is a concrete release need to separate them.

### 4. Make configuration composition a public, cacheable boundary

**Priority: medium.** The escaping defect is fixed, but full-file template substitution still couples immutable rule
policy to extension-owned data. The plugin replaces upstream's config collection with a generated resource; there is no
documented project-override API or precedence contract. Generating a defaults file does not make that file an effective
override.

**Implementation:** retain static resources and generate extension overrides into a stable task output. Prove detekt's
nested-map/list merge precedence before choosing layered files; otherwise use a structured YAML writer. Expose and
document additional consumer configuration files. Namespace bundled resources instead of loading globally generic
`base.yml`, `compose.yml`, and `versions.properties` names; resource collisions are a risk to test, not a reproduced
failure here.

**Acceptance:** a consumer can override one nested rule without losing unrelated policy; empty lists replace defaults
as documented; repeated cached builds do not regenerate unchanged files; extension changes invalidate the right tasks;
an unrelated plugin's similarly named resource cannot alter this plugin's behavior. Maintain parsed-YAML regression
coverage. Detekt documents rule validation and path filtering, but these do not establish a custom multi-file precedence
contract: [configuration documentation](https://detekt.dev/docs/introduction/configurations/).

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
use dedicated warning-sensitive compatibility tests. Replace the stale, partly completed instructions in
[`architecture-plan.md`](architecture-plan.md) with tracked milestones as the next phases begin.

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

## Validation

- Initial `./gradlew build`: successful in 2m 36s; 50 plugin tests passed. Rules tests were initially up to date.
- Regression run before fixes: 20 tests executed, with five expected failures covering the defects above.
- Focused run after fixes: 31 tests passed, including configuration-cache reuse, source filters, file generation, and
  baseline consumption.
- Final `./gradlew build :rules:test --rerun --console=plain`: successful in 2m 42s. All 57 plugin tests and 9 rules
  tests passed with no skips. Both test tasks executed; plugin validation and both artifact builds passed. The rules-test
  task was explicitly forced to run instead of relying on its earlier up-to-date result.
- `git diff --check`: passed. No commits or publication were made.

Validation was performed on the local Linux host. No macOS/Windows run or package publication was performed. The isolated
consumer failure described above is intentionally reported as outstanding; the normal supported-fixture suite does not
cover that case.
