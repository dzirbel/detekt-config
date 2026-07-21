# Repository Guidelines

## Project Structure & Module Organization
This project provides an opinionated Gradle plugin that configures the detekt static analysis tool.

Primary functions:
- Configure detekt to work for projects of all types (KMP, JVM, Android, Compose, etc); in particular to use type
  resolution out of the box and run against all/a default source set (depending on context and configuration)
- Provide an opinionated configuration file, but allow toggling of a few options
- Provide a rule set with my own assortment of rules; these can be used independently or with the plugin

Structure:
- `plugin/`: Gradle plugin that wires detekt config and dependencies.
  - `plugin/src/main/kotlin`: plugin implementation.
  - `plugin/src/main/resources`: config assets (`base.yml`, `compose.yml`) and shared versions (`versions.properties`).
  - `plugin/src/test/kotlin` and `plugin/src/test/resources`: unit tests and TestKit fixture project.
- `rules/`: custom detekt rule set.
- `docs/`: reference notes for plugin behavior and troubleshooting (see `docs/detekt-classpath-notes.md`).

## Build, Test, and Development Commands
Always run Gradle commands to verify code changes. Make sure the build both passes and the expected tasks were run.
Use the Gradle wrapper directly. Do not override `GRADLE_USER_HOME` as a sandbox workaround; direct `./gradlew`
invocations are the repository-approved command form and should use the normal user-level Gradle cache.

- `./gradlew build`: compile, test, and run verification tasks.
- `./gradlew check`: aggregate verification tasks without full build packaging.
- `./gradlew test`: run all tests.
- `./gradlew :plugin:test`: plugin unit + TestKit tests.
- `./gradlew :rules:test`: ruleset tests only.

## Coding Style & Naming Conventions
- Kotlin + Kotlin DSL (`*.kt`, `*.kts`), 4-space indentation, standard Kotlin formatting.
- Packages use `io.github.dzirbel.*`; classes in PascalCase; tests named `*Test.kt`.
- Centralized versions live in `plugin/src/main/resources/versions.properties` (update here first).

## Testing Guidelines
- Frameworks: `kotlin.test`, `detekt-test`, and Gradle TestKit.
- `plugin/` includes a mixture of pure unit tests and functional/end-to-end tests against sample Gradle projects
  `plugin/src/test/resources` to assert plugin and task behavior via TestKit.
- Add rule behavior tests in `rules/src/test/kotlin` alongside the rule name.
