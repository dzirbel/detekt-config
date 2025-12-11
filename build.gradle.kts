tasks.register("check") {
    dependsOn(gradle.includedBuild("plugin").task(":check"))
}
