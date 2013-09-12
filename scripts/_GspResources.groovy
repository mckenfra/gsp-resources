includeTargets << grailsScript("_GrailsInit")
includeTargets << grailsScript("_GrailsCompile")

srcDir="src/lib/groovy"
tgtDir="target/lib"
libDir="lib"
jarFile="${libDir}/gspresources-background-servlet-2.4.jar"
servlet25Library="${grailsHome}/lib/javax.servlet/servlet-api/jars/servlet-api-2.4.jar"
grailsLibDir="${grailsHome}/lib"
grailsDistDir="${grailsHome}/dist"

target(init: "Create dirs") {
    Ant.mkdir(dir: "${tgtDir}")
    Ant.mkdir(dir: "${libDir}")
}

target(compile: "compile the library source") {
    depends("init")
    
    // Compile for servlet 3.0 - default classpath is enough
    //groovyc(srcdir: "${srcDir}", destdir: "${tgtDir}")
    
    // Build classpath for compile for servlet 2.5
    def grailsLibJars = Ant.fileset(dir: "${grailsLibDir}") {
        include(name: "**/servlet-api-*.jar")
        include(name: "**/commons-collections/**")
        include(name: "**/org.springframework/**")
        include(name: "**/org.slf4j/**")
        include(name: "**/org.codehaus.groovy/**")
    }
    def grailsDistJars = Ant.fileset(dir: "${grailsDistDir}") {
        include(name: "**/grails-core-*.jar")
        include(name: "**/grails-web-*.jar")
    }

    Ant.path(id: "servlet25") {
        pathelement(location:"${servlet25Library}")
        grailsLibJars.each {
            pathelement(location:it)
        }
        grailsDistJars.each {
            pathelement(location:it)
        }
    }
    
    // Compile for servlet 2.5 - use custom classpath
    groovyc(srcdir: "${srcDir}", destdir: "${tgtDir}", fork:true, failonerror:true, classpathref:"servlet25")
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
