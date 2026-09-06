# Detekt and Kotlin classpath notes

Detekt uses Kotlin compiler infrastructure for type resolution. Rules such as `ForbiddenMethodCall` and
`DoubleMutabilityForCollection` can produce incomplete results when task sources, compiler options, or classpaths do not
match the compilation being analyzed.

## Current task wiring

The upstream detekt Gradle plugin creates the plain `detekt` task, source-set tasks, and type-resolved tasks for JVM,
Android, and JVM multiplatform compilations. This plugin uses one internal adapter to map each Kotlin compilation to one
type-resolved detekt task and makes the root `detekt` task depend on every mapped task:

- JVM tasks reuse the upstream type-resolved task and use the compilation's filtered Kotlin source sets, output,
  compile libraries, and friend paths.
- JS and native tasks use the compilation's filtered Kotlin source sets, detekt's CLI classpath, associated
  compilation outputs as friend paths, transitive outputs from corresponding sibling JVM compilations when they exist,
  and a lenient JVM-compatible view of declared dependencies.
- Tasks inherit the compilation's language/API versions, explicit API mode, opt-ins, and free compiler arguments. JVM
  tasks additionally inherit the JVM target and no-JDK mode.
- Multiplatform tasks enable detekt's multiplatform analysis mode.
- Every configured analysis task depends on its corresponding Kotlin compile task.
- Android application/library projects reuse upstream variant tasks and the upstream `detektMain`/`detektTest`
  aggregators. The root `detekt` task has no sources of its own and depends on both aggregators.

Task names use the upstream compilation-then-target order: `detektMain` for a standalone JVM main compilation,
`detektMainJvm` for KMP JVM main, and `detektTestJs` for KMP JS test. Upstream source-set and baseline tasks remain
available as distinct entry points, but the root lifecycle runs only the compilation tasks. Whenever compilation tasks
exist, the root `detekt` task has no sources of its own and therefore does not repeat syntax-only analysis.

## Known limitations

- Detekt's analysis engine cannot consume target-only `.klib` dependencies. JS and native analysis resolves dependencies
  that publish JVM variants and skips target-only artifacts; see [the support contract](support-contract.md).
- Repository-owned Android coverage currently targets AGP 9.3 with built-in Kotlin. The variant source/classpath wiring
  itself remains delegated to detekt's Android integration.
- JS/native tasks consume the configured `detekt.baseline`, but the plugin does not yet generate corresponding
  compilation-specific baselines.

Compose rules are loaded only when a JetBrains/Kotlin Compose compiler plugin is present or an Android
application/library explicitly enables `buildFeatures.compose`. Android plugin presence alone is not treated as Compose
enablement.

When findings differ between otherwise equivalent targets, inspect the analysis task's sources and classpath, enable
detekt debug logging, and treat any reported compiler errors as an analysis correctness failure. The remaining fixes and
acceptance criteria are tracked in [the architecture plan](architecture-plan.md).
