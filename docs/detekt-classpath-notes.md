# Detekt and Kotlin classpath notes

Detekt uses Kotlin compiler infrastructure for type resolution. Rules such as `ForbiddenMethodCall` and
`DoubleMutabilityForCollection` can produce incomplete results when task sources, compiler options, or classpaths do not
match the compilation being analyzed.

## Current task wiring

The upstream detekt Gradle plugin creates the plain `detekt` task, source-set tasks, and type-resolved tasks for JVM,
Android, and JVM multiplatform compilations. This plugin additionally observes Kotlin compilations and makes its root
`detekt` task depend on one task per non-common compilation:

- JVM tasks use the Kotlin compile task's sources, the compilation output, and compile libraries.
- JS and native tasks use the Kotlin compile task's sources and detekt's own CLI classpath.
- Multiplatform tasks enable detekt's multiplatform analysis mode.
- Every configured analysis task depends on its corresponding Kotlin compile task.

The custom task names use target-then-compilation order, such as `detektJvmMain` and `detektJsTest`. Upstream detekt can
also register source-set tasks and JVM tasks with different names, so a multiplatform project currently exposes more
detekt tasks than the root lifecycle task runs.

## Known limitations

- JS test analysis currently emits Kotlin compiler errors. The functional tests preserve this behavior explicitly, but
  those errors mean type-resolved findings may be incomplete.
- Native analysis uses detekt's CLI classpath rather than the compilation dependency classpath. Its current fixtures find
  the expected issues, but they do not prove resolution of external native dependencies.
- The root `detekt` task remains an upstream `Detekt` analysis task rather than a pure lifecycle task. In a standard JVM
  layout it can analyze the default main and test directories again, without the compilation classpath, after the
  type-resolved tasks pass.
- The plugin realizes each Kotlin compile task during configuration to obtain its sources and libraries.
- Android behavior comes from the upstream detekt integration and does not have repository-owned TestKit coverage.

When findings differ between otherwise equivalent targets, inspect the analysis task's sources and classpath, enable
detekt debug logging, and treat any reported compiler errors as an analysis correctness failure. The intended fixes and
acceptance criteria are tracked in [the architecture plan](architecture-plan.md).
