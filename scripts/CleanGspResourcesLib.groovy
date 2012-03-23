includeTargets << new File("${gspResourcesPluginDir}/scripts/_GspResources.groovy")

target(default: "Cleans library") {
    clean()
}
