import org.grails.plugin.gspresources.GspResourceLocator
import org.grails.plugin.gspresources.GspResourcePageRenderer
import org.grails.plugin.gspresources.GspResourceProcessor

class GspResourcesGrailsPlugin {
    def version = "0.4.3"
    def grailsVersion = "2.0.0 > *"
    def loadAfter = ['resources', 'servlets', 'groovyPages']
    def pluginExcludes = [
            'src/groovy/**/test/**',
            'grails-app/views/**',
            'grails-app/controllers/**',
            'grails-app/i18n/**',
            'web-app/**',
            '**/MyAppResources.groovy',
            'src/docs/**'
    ]

    def title = "GSP Resources"
    def description = 'Use resources plugin to serve dynamically-built CSS and JS as static files instead of dynamic (non-cacheable) GSPs.'

    def documentation = "http://mckenfra.github.com/gsp-resources/docs/guide/single.html"

    def license = "APACHE"
    def developers = [
        [name: "Stefan Kendall", email: "stefankendall@gmail.com"],
        [name: "Francis McKenzie", email: "francis.mckenzie@gmail.com"]
    ]
    def issueManagement = [system: "GITHUB", url: "https://github.com/mckenfra/gsp-resources/issues"]
    def scm = [url: "https://github.com/mckenfra/gsp-resources"]

    // Inject the renderer, gsp locator, and override the resources plugin processor
    def doWithSpring = {
        gspResourcePageRenderer(GspResourcePageRenderer, ref("groovyPagesTemplateEngine")) { bean ->
            bean.lazyInit = true
            groovyPageLocator = groovyPageLocator
            grailsResourceLocator = grailsResourceLocator
        }

        gspResourceLocator(GspResourceLocator) { bean ->
            bean.lazyInit = true
            groovyPageLocator = groovyPageLocator
            grailsResourceLocator = grailsResourceLocator
        }

        // Override processor in resources plugin
        grailsResourceProcessor(GspResourceProcessor) {
            grailsLinkGenerator = ref('grailsLinkGenerator')
            if (springConfig.containsBean('grailsResourceLocator')) {
                grailsResourceLocator = ref('grailsResourceLocator')
            }
            gspResourcePageRenderer = ref('gspResourcePageRenderer')
            gspResourceLocator = ref('gspResourceLocator')
            grailsApplication = ref('grailsApplication')
        }
    }

    // Start the resources processing if necessary - otherwise, may be done during bootstrap
    def doWithDynamicMethods = { appCtx ->
        if ( appCtx.grailsResourceProcessor.isStartOnPluginLoad() ) {
            appCtx.grailsResourceProcessor.start()
        }
    }
}
