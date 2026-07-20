# Detekt and Kotlin classpath notes

Detekt uses Kotlin compiler infrastructure for type resolution. Rules such as `ForbiddenMethodCall` and
`DoubleMutabilityForCollection` can produce incomplete results when task sources, compiler options, or classpaths do not
match the compilation being analyzed.

## Current task wiring

The upstream detekt Gradle plugin creates the plain `detekt` task, source-set tasks, and type-resolved tasks for JVM,
Android, and JVM multiplatform compilations. This plugin additionally observes Kotlin compilations and makes its root
`detekt` task depend on one task per non-common compilation:

- JVM tasks use the compilation's Kotlin source-set directories, output, compile libraries, and friend paths.
- JS and native tasks use the compilation's Kotlin source-set directories, detekt's CLI classpath, associated
  compilation outputs as friend paths, a sibling JVM target's associated output when one exists, and a lenient
  JVM-compatible view of declared dependencies.
- Tasks inherit the compilation's language/API versions, opt-ins, and free compiler arguments. JVM tasks additionally
  inherit the JVM target and no-JDK mode.
- Multiplatform tasks enable detekt's multiplatform analysis mode.
- Every configured analysis task depends on its corresponding Kotlin compile task.

The custom task names use target-then-compilation order, such as `detektJvmMain` and `detektJsTest`. Upstream detekt can
also register source-set tasks and JVM tasks with different names, so a multiplatform project currently exposes more
detekt tasks than the root lifecycle task runs.

## Known limitations

- Detekt's analysis engine cannot consume target-only `.klib` dependencies. JS and native analysis resolves dependencies
  that publish JVM variants and skips target-only artifacts; see [the support contract](support-contract.md).
- The root `detekt` task remains an upstream `Detekt` analysis task rather than a pure lifecycle task. In a standard JVM
  layout it can analyze the default main and test directories again, without the compilation classpath, after the
  type-resolved tasks pass.
- Android behavior comes from the upstream detekt integration and does not have repository-owned TestKit coverage.

When findings differ between otherwise equivalent targets, inspect the analysis task's sources and classpath, enable
detekt debug logging, and treat any reported compiler errors as an analysis correctness failure. The remaining fixes and
acceptance criteria are tracked in [the architecture plan](architecture-plan.md).
