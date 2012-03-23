includeTargets << new File("${gspResourcesPluginDir}/scripts/_GspResources.groovy")

target(default: "Builds library") {
    build()
}
