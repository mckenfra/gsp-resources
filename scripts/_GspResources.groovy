includeTargets << grailsScript("_GrailsInit")
includeTargets << grailsScript("_GrailsCompile")

srcDir="src/lib/groovy"
tgtDir="target/lib"
libDir="lib"
jarFile="${libDir}/background-servlet.jar"

target(init: "Create dirs") {
    Ant.mkdir(dir: "${tgtDir}")
    Ant.mkdir(dir: "${libDir}")
}

target(compile: "compile the library source") {
    depends("init")
    groovyc(srcdir: "${srcDir}", destdir: "${tgtDir}")
}

target(jar: "create the library jar") {
    depends("compile")
    Ant.jar(destfile: "${jarFile}", basedir:"${tgtDir}")
}

target(clean: "delete built library and classes") {
    Ant.delete(dir:  "${tgtDir}")
    Ant.delete(file: "${jarFile}")
}

target(build: "Build the library") {
    depends("jar")
}
