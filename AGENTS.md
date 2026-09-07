# Repository Guidelines

## Agent Guidelines
- Always leave changes uncommitted for the user to review. Do not edit human-facing files (e.g. README).
- When possible, scope changes to implement a single feature, add a single test, fix a single bug, etc. to make them
  easy to review atomically.
- Readability of the tests is the primary goal. Aim to make them extremely straightforward and readable so it is obvious
  what they are testing, how, and that the code must be correct for it to pass. Tests should be end-to-end and
  exhaustive when possible to achieve this goal. Implementation code is less important and can use hacks for performance
  or necessity, but test code must remain pristine.

## Project Structure & Module Organization
This project provides an opinionated Gradle plugin that configures the detekt static analysis tool.

Primary functions:
- Configure detekt to work for projects of all types (KMP, JVM, Android, Compose, etc.); in particular to use type
  resolution out of the box and run against all/a default source set (depending on context and configuration)
- Provide an opinionated configuration file, but allow toggling or overriding options by consuming projects
- Provide a rule set with my own assortment of rules; these can be used independently or with the plugin

Structure:
- `plugin/`: Gradle plugin that wires detekt config and dependencies.
  - `plugin/src/main/kotlin`: plugin implementation.
  - `plugin/src/main/resources`: config assets (`base.yml`, `compose.yml`) and shared versions (`versions.properties`).
  - `plugin/src/test/kotlin` and `plugin/src/test/resources`: unit tests and TestKit fixture project.
  - `plugin/src/main/resources/io/github/dzirbel/detekt-config/versions.properties`: centralized versions
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

## Testing Guidelines
- Frameworks: `kotlin.test`, `detekt-test`, and Gradle TestKit.
- `plugin/` includes a mixture of pure unit tests and functional/end-to-end tests against sample Gradle projects
  `plugin/src/test/resources` to assert plugin and task behavior via TestKit.
- Samples should be realistic Gradle projects, with straightforward configuration to make them easy to read and
  evaluate. Tests should avoid modifying them (e.g. writing text into test fixture files) whenever possible.
- Add rule behavior tests in `rules/src/test/kotlin` alongside the rule name.
