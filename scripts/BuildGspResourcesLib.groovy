includeTargets << new File(gspResourcesPluginDir, "scripts/_GspResources.groovy")

target(buildGspResourcesLib: "Builds library") {
    build()
}

setDefaultTarget 'buildGspResourcesLib'