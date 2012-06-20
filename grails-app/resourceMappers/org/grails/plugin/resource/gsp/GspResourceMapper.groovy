package org.grails.plugin.resource.gsp

import org.apache.commons.logging.LogFactory
import org.grails.plugin.resource.mapper.MapperPhase
import org.grails.plugin.resource.ResourceMeta
import org.grails.plugin.resource.gsp.GspResourceLocator
import org.grails.plugin.resource.gsp.GspResourceMeta
import org.grails.plugin.resource.gsp.GspResourcePageRenderer

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

    // Injected
    GspResourceLocator gspResourceLocator
    GspResourcePageRenderer gspResourcePageRenderer
    def grailsResourceProcessor
    
    def map(resource, config) {
        if (log.isDebugEnabled()) {
            log.debug "Checking if is a GSP: ${resource.sourceUrl}"
        }
        
        // Check if GSP
        Map gsp = gspResourceLocator.findGsp(resource.sourceUrl)
        if (gsp) {
            
            // Generate the url of the compiled resource
            String actualUrl = gspResourceLocator.generateCompiledFilenameFromOriginal(resource.originalUrl)

            // Now create the synthetic resource that this resource is going to delegate to
            def gspResource = grailsResourceProcessor.findSyntheticResourceById(actualUrl)
            if (! gspResource) {
                // Creates a new resource and empty file
                gspResource = grailsResourceProcessor.newSyntheticResource(actualUrl, GspResourceMeta)
                gspResource.id = actualUrl
                gspResource.contentType = resource.contentType
                gspResource.disposition = resource.disposition
                gspResource.gsp = gsp
                gspResource.attributes = resource.attributes
                gspResource.tagAttributes = resource.tagAttributes
                gspResource.gspResourceLocator = gspResourceLocator
                gspResource.gspResourcePageRenderer = gspResourcePageRenderer
                ensureHasTypeAttribute(gspResource)
                if (log.isDebugEnabled()) {
                    log.debug "Created synthetic GSP resource: ${actualUrl}"
                }
            } else {
                if (log.isDebugEnabled()) {
                    log.debug "Synthetic GSP resource already exists: ${actualUrl}"
                }
            }
            resource.delegateTo(gspResource)
            // Ensure the type attribute is set on the original resource too,
            // otherwise we get 'unrecognised type' exceptions
            ensureHasTypeAttribute(resource)
        } else {
            if (log.isDebugEnabled()) {
                log.debug "Not a GSP: ${resource.sourceUrl}"
            }
        }
    }
    
    /**
     * Ensures the 'type' attribute is set for the GSP resource in the 
     * resource meta 'tagAttributes' field.
     * 
     * @param resource The resource meta for the GSP resource
     */
    protected void ensureHasTypeAttribute(ResourceMeta resource) {
        if (!resource) {
            throw new NullPointerException("No ResourceMeta specified!")
        }
        
        // Already set
        if (resource.tagAttributes?.type) {
            return
        }
        
        // Ensure we have the right processor
        if (! (grailsResourceProcessor instanceof GspResourceProcessor)) {
            log.warn "Unable to set GSP resource type - grailsResourceProcessor not recognised: ${grailsResourceProcessor?.class}"
            return
        }
        
        // Set the type explicitly
        def type = grailsResourceProcessor.getResourceTypeFromUri(resource.actualUrl)
        if (type) {
            if (resource.tagAttributes) {
                resource.tagAttributes.type = type
            } else {
                resource.tagAttributes = [type:type]
            }
        }
    }
}
