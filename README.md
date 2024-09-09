This repository contains my personal [detekt](https://github.com/detekt/detekt) configuration, published as a Gradle
plugin for re-use between projects. It's an opinionated but fairly standard configuration, which works across JVM,
Multiplatform, and Android projects.

In particular, the plugin:
- Adds the base detekt plugin, the [detekt-formatting](https://detekt.dev/docs/rules/formatting/) plugin (based on
  [ktlint](https://github.com/pinterest/ktlint)), and the [compose-rules](https://github.com/mrmans0n/compose-rules)
  plugin if Compose is applied to the project
- Applies a detekt [configuration YAML file](https://detekt.dev/docs/introduction/configurations/) based on my personal
  style
- Adapts the configuration to the project it is applied to, e.g. changing rules based on whether Compose is present
- SoonTM: Encourages/forces use of detekt [type resolution](https://detekt.dev/docs/gettingstarted/gradle#using-type-resolution)
  out of the box, to avoid rules silently being ignored when running the base `detekt`/`check` tasks
- In the future, I may also create and include my own custom rules here

### Configuration

The gradle plugin is designed to be used with little to no configuration. To add it:

#### 1. Add the maven repository

TODO

#### 2. Apply the plugin

```
plugins {
    id("io.github.dzirbel.detekt-config") version "1.0.0"
}
```

#### 3. Optionally configure

```
detektConfig {
    // add configuration options here
    // see plugin/src/main/kotlin/io/github/dzirbel/DetektConfigExtension.kt for available options
}
```

See the projects in [`samples`](samples) for examples across different project types and configurations.

### Publishing

TODO