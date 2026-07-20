# Type-resolution support contract

The plugin supports the compilation types below. "Supported" means the repository has functional coverage for the
applicable contract checks; "provisional" means behavior is currently delegated to detekt and is not yet covered here.

| Project type | Main | Test | Custom | Dependency model | Status |
| --- | --- | --- | --- | --- | --- |
| Kotlin/JVM | Yes | Yes | Yes | Compilation classpath and friend paths | Supported |
| KMP JVM | Yes | Yes | Not yet covered | Compilation classpath and friend paths | Supported |
| KMP JS | Yes | Yes | Not yet covered | JVM-compatible dependency variants and sibling JVM project output | Supported subset |
| KMP native | Yes | Yes | Not yet covered | JVM-compatible variants of declared multiplatform dependencies | Supported subset |
| Android JVM | Variant-specific | Variant-specific | Variant-specific | Upstream detekt Android integration | Provisional |
| Standalone Kotlin/JS | No | No | No | The plugin was removed from current Kotlin releases | Unsupported |

Every supported analysis task must:

- analyze the Kotlin source-set hierarchy belonging to one compilation;
- resolve project output and declared dependencies that are representable to detekt's JVM analysis engine;
- inherit the compilation's language version, API version, opt-ins, free compiler arguments, and applicable friend
  paths;
- inherit JVM target and no-JDK settings for JVM compilations;
- produce no Kotlin compiler-error summary; and
- participate once in both `check` and the root `detekt` lifecycle.

Detekt's analysis engine consumes JVM classpath artifacts. JS and native tasks therefore resolve a separate, lenient
JVM-compatible view of their declared dependency graph. Cross-platform libraries that publish a JVM variant are
type-resolved; target-only `.klib` dependencies are skipped because detekt cannot load them as analysis classpath
entries. When a KMP project has a JVM target, non-JVM test tasks also use its associated compilation output as the JVM
representation of shared project code. Kotlin standard-library artifacts are excluded from the dependency projection so
they do not conflict with the Kotlin version embedded in detekt.

Functional fixtures enforce the contract in isolated temporary builds. JVM coverage includes main, test, custom source
sets, project output, and an external dependency. KMP JS and native fixtures exercise main and test compilations and use
an external coroutine API in a type-resolved finding. Clean KMP JVM/JS tasks assert that no compiler-error summary is
emitted. Android fixtures and explicit custom KMP compilation coverage are deferred to the Android and topology phases
of the architecture plan, respectively.
