1. Update `rules` in `plugin/src/main/resources/versions.properties` (currently shared by the plugin and rules artifacts)
2. Run `./gradlew build`
3. Commit with message "Prepare release A.B.C"
4. Tag with `vA.B.C`
5. Push branch and tag

That's it! The publish workflow will automatically be run on the pushed tag and publish to the GitHub maven repository.
