// TODO make plugin a regular project in the build rather than included plugin build, so :check will work
pluginManagement {
    includeBuild("plugin")
}

includeBuild("samples")
