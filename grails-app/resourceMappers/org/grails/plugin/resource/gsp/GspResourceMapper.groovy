package org.grails.plugin.resource.gsp

import org.apache.commons.logging.LogFactory
import org.grails.plugin.resource.mapper.MapperPhase
import org.grails.plugin.resource.gsp.GspResourceLocator

/**
 * A mapper for compiling GSP resource files.
 * <p>
 * <b>Notes</b>
 * <p>
 * Processing of GSP resources must be deferred until after other synthetic resources
 * have been processed. This is because a GSP may contain <r:require /> tags, which will only
 * render correctly when the resources plugin has finished preparing the relevant resources.
 * <p>
 * So the compilation of the GSP has to be delayed to the synthetic-resource processing phase,
 * rather than the non-synthetic-resource processing phase. This is achieved by setting the
 * initial GSP resource to be a 'delegating' resource, delegating to a target synthetic
 * resource that has the desired post-compile URI (i.e. with .gsp suffix removed).
 * The actual GSP compilation is then done when the synthetic resource is processed.
 * <p>
 * Note that if your GSP contains <r:require /> tags, you will have to explicitly
 * list these in the 'dependsOn' setting for the module that contains the GSP in your
 * Resoures.groovy config file.</li>
 *
 * @author Stefan Kendell
 * @author Francis McKenzie
 */
class GspResourceMapper {
    def log = LogFactory.getLog(this.class)
    
    def phase = MapperPhase.GENERATION

    def priority = -1

    static defaultExcludes = ['**/*.js', '**/*.png', '**/*.gif', '**/*.jpg', '**/*.jpeg', '**/*.gz', '**/*.zip']
    static defaultIncludes = ['**/*.gsp']

    /**
     * Injected - for finding the underlying GSP file
     */
    GspResourceLocator gspResourceLocator
    /**
     * Injected - for preparing the GSP resource
     */
    GspResourceProcessor grailsResourceProcessor
    
    /**
     * Ensures the specified resource is a GSP, then creates the delegating
     * resource to render the GSP.
     * 
     * @param resource The resource to map
     * @param config The resources plugin config
     */
    def map(resource, config) {
        if (log.isDebugEnabled()) {
            log.debug "Checking if is a GSP: ${resource.sourceUrl}"
        }
        
        // Check if GSP
        Map gsp = gspResourceLocator.findGsp(resource.sourceUrl)
        if (gsp) {
            grailsResourceProcessor.prepareGspResource(resource, gsp)
        } else {
            if (log.debugEnabled) {
                log.debug "Not a GSP: ${resource.sourceUrl}"
            }
        }
    }
}
