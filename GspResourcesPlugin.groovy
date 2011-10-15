class GspResourcesPlugin {
    def version = "0.1"
    def grailsVersion = "1.3.7 > *"
    def dependsOn = [:]
    def pluginExcludes = [
            'src/groovy/**/*',
            'grails-app/views/*.gsp',
            'web-app/**/*',
            '**/MyAppResources.groovy',
    ]

    def author = "Stefan Kendall"
    def authorEmail = "stefankendall@gmail.com"
    def title = "Pre-compile JS, CSS, and other static files as GSPs"
    def description = 'Use the resources plugin to include static files like main.css.gsp, so dynamically built CSS and JS can be served as proper files instead of inlined in a non-cacheable GSP file. Note that changes to the GSP will cause a recompile, but changes to GSP data will not.'

    def documentation = "http://grails.org/plugin/gsp-resources"

    def scm = [url: "https://github.com/stefankendall/gsp-resources"]
}
