import org.grails.plugin.resource.gsp.GspResourceLocator
import org.grails.plugin.resource.gsp.GspResourcePageRenderer
import org.grails.plugin.resource.override.DelayableResourceProcessor

class GspResourcesGrailsPlugin {
    def version = "0.3"
    def grailsVersion = "2.0.0 > *"
    def dependsOn = [
        'resources': '1.1.5 > *',
        'servlets': '2.0.0 > *',
        'groovyPages': '2.0.0 > *'
    ]
    def pluginExcludes = [
            'src/groovy/**/test/*',
            'grails-app/views/*.gsp',
            'grails-app/controllers/**/*',
            'web-app/**/*',
            '**/MyAppResources.groovy',
    ]

    def author = "Stefan Kendall, Francis McKenzie"
    def authorEmail = "stefankendall@gmail.com, francis.mckenzie@gmail.com"
    def title = "GSP Resources"
    def description = 'Use resources plugin to serve dynamically-built CSS and JS as static files instead of dynamic (non-cacheable) GSPs.'

    def documentation = "http://grails.org/plugin/gsp-resources"

    def scm = [url: "https://github.com/mckenfra/gsp-resources"]
	
    // Inject the renderer, gsp locator, and override the resources plugin processor
	def doWithSpring = {
		gspResourcePageRenderer(GspResourcePageRenderer, ref("groovyPagesTemplateEngine")) { bean ->
			bean.lazyInit = true
			groovyPageLocator = groovyPageLocator
			grailsResourceLocator = grailsResourceLocator
            grailsApplication = application
		}
        
        gspResourceLocator(GspResourceLocator) { bean ->
            bean.lazyInit = true
            groovyPageLocator = groovyPageLocator
            grailsResourceLocator = grailsResourceLocator
        }
        
        // Override processor in resources plugin
        grailsResourceProcessor(DelayableResourceProcessor) {
            grailsLinkGenerator = ref('grailsLinkGenerator')
            if (springConfig.containsBean('grailsResourceLocator')) {
                grailsResourceLocator = ref('grailsResourceLocator')
            }
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
