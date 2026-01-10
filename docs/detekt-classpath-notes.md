# Detekt + Kotlin classpath notes

Detekt uses Kotlin compiler infrastructure to build a binding context for type resolution. Many rules (like
ForbiddenMethodCall and DoubleMutabilityForCollection) need that binding context to resolve call targets and types. If
type resolution fails or uses an incompatible Kotlin stdlib on the classpath, those rules may silently produce fewer
findings.

Key points for this repository:
- Detekt tasks (e.g., detektMain/detektJsMain) are created by the detekt Gradle plugin. For JVM targets, the plugin
  sets the detekt task classpath to the Kotlin compilation output + compile dependency files.
- Detekt's multiplatform integration only enables type resolution for JVM/Android targets by default; this repo opts
  into type resolution for all detekt main tasks (including JS/native) by wiring compilation outputs and dependency
  files into the detekt task classpath.
- The plugin logic in this repo augments detekt tasks after evaluation:
  - It adds compilation output + compile dependency files when available (falling back to compile classpath
    configurations when no compilation is found).
  - Native targets skip compile dependency files (to avoid early Kotlin/Native distribution resolution); they use
    compilation outputs plus detekt's classpath to keep type resolution on.
  - It adds explicit dependencies on the Kotlin and Java compilation tasks so detekt runs after compilation.
  - It falls back to the detekt CLI classpath if the compile classpath is missing or empty.
  - It filters out Kotlin 2.x stdlib jars from the detekt task classpath and falls back to detekt's own classpath,
    which is aligned to the detekt/Kotlin version it bundles. This avoids Kotlin version mismatch during analysis.

If JVM tasks report fewer warnings than KMP/JS tasks on identical source, check:
- The detekt task classpath contents (especially Kotlin stdlib version compatibility).
- Whether the detekt task is getting the compile classpath from the correct source set.
- Whether the task is running with type resolution enabled and not silently falling back.
