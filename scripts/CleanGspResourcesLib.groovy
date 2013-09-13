includeTargets << new File(gspResourcesPluginDir, "scripts/_GspResources.groovy")

target(cleanGspResourcesLib: "Cleans library") {
    clean()
}

setDefaultTarget 'cleanGspResourcesLib'