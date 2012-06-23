package org.grails.plugin.resource.gsp

import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.logging.LogFactory;
import org.grails.plugin.resource.ResourceMeta;
import org.grails.plugin.resource.ResourceProcessor
import org.grails.plugin.resource.ResourceProcessorBatch;

/**
 * Overrides the resources plugin's ResourceProcessor, to provide additional
 * features required for GSP processing:
 * <ul>
 * <li>Optionally delay resources processing until the bootstrap phase,
 * to ensure any GSP dependencies (e.g. domain objects / other plugins) are
 * fully loaded before GSP compilation starts.</li>
 * 
 * <li>Ensure resource modules are loaded in correct order, according to
 * 'dependsOn' configuration settings. This ensures any <b>resources</b> your GSP
 * depends on are loaded before compilation starts.</li>
 * 
 * <li>Automatically detect the target file type of the GSP by the filename
 * extension. E.g. myfile.js.gsp will be automatically detected as being
 * a javascript resource</li>
 * </ul>
 * 
 * Note that the (optional) delaying of resources processing until bootstrap is
 * achieved by temporarily blocking the call to reloadAll(),
 * which the resources plugin calls on server startup. When the gsp-resources
 * plugin loads, it either allows the reloadAll() to go ahead right-away,
 * or delays it until the bootstrap phase, depending on the configuration
 * settings.
 * <p>
 * Configuration setting in <code>Config.groovy</code>:
 * <p>
 * <blockquote>
 * <code>grails.resources.processing.startup = "default" | "delayed" | false</code>
 * </blockquote>
 * <p>
 * Allowed values:
 * <table border="1" cellpadding="4" cellspacing="0">
 * <col width="25%" />
 * <col width="75%" />
 * <thead>
 * <tr><th>Value</th><th>Meaning</th></tr>
 * </thead>
 * <tbody>
 * <tr><td><code>"default"</code></td><td>No delay in resources processing - i.e. keeps same behaviour as resources plugin</td></tr>
 * <tr><td><code>"delayed"</code></td><td>Resources processing delayed until bootstrap phase</td></tr>
 * <tr><td><code>false</code></td><td>Resources processing is not automatically started during server startup</td></tr>
 * </tbody>
 * </table>
 * <p>
 * Note that if you want to start resources processing manually, you should set the
 * 'startup' configuration setting to false, and then you can trigger startup
 * of resources processing by doing:
 * <pre> 
 * class MyClass {
 *   // Automatically injected
 *   def grailsResourcesProcessor
 * 
 *   def myMethod() {
 *     // Start resources processing
 *     grailsResourcesProcessor.start()
 *   }
 * }
 * </pre>
 *
 * @author Francis McKenzie
 */
class GspResourceProcessor extends ResourceProcessor {
    def log = LogFactory.getLog(this.class)
    
    /**
     * Injected - for finding the underlying GSP file
     */
    GspResourceLocator gspResourceLocator
    /**
     * Injected - for rendering the GSP
     */
    GspResourcePageRenderer gspResourcePageRenderer
    
    // We only want to block the first reload
    protected boolean firstReloadAllDone = false 
    
    // Delayed by default
    protected boolean blocked = true
    
    // Set from configuration
    def startup = true
    
    /**
     * Block the reloadAll() function.
     * 
     * Note this is blocked by default. Note also that once reloadAll()
     * has run once, blocking no longer has any effect.
     */
    def block() {
        this.blocked = true
    }
    
    /**
     * Unblock the reloadAll() function
     */
    def unblock() {
        this.blocked = false
    }
    
    /**
     * Checks if blocked or not - if not, calls the superclass
     * reloadAll().
     * 
     * If the first reloadAll() has already been done, then blocking has no effect.
     */
    @Override
    void reloadAll() {
        if (!blocked || firstReloadAllDone) {
            if (!firstReloadAllDone) {
                if (log.isDebugEnabled()) {
                    log.debug "Begin initial resources processing"
                }
            }
            firstReloadAllDone = true
            super.reloadAll()
        } else {
            if (log.isDebugEnabled()) {
                log.debug "Block resources processing"
            }
        }
    }
    
    /**
     * Checks configuration to see if we want to start
     * resources processing at plugin load (i.e. maintain default behaviour
     * of resources plugin).
     */
    boolean isStartOnPluginLoad() {
        boolean startNow = ! (firstReloadAllDone || startup == false || /(?i)false|delayed/ =~ startup)
        if (! firstReloadAllDone) {
            log.info "${startNow ? 'Start' : 'Delay'} resources processing at plugin load"
        }
        return startNow
    }
    
    /**
     * Checks configuration to see if we want to start
     * resources processing during bootstrap of gsp-resources plugin
     * itself.
     * 
     * Note that you may want to explicitly start resources processing
     * yourself during your project's bootstrap (or later). This configuration
     * allows you to do that.
     */
    boolean isStartOnPluginBootstrap() {
        boolean startNow = ! (firstReloadAllDone || startup == false || /(?i)false/ =~ startup)
        if (! firstReloadAllDone) {
            log.info "${startNow ? 'Start' : 'Delay'} resources processing at plugin bootstrap"
        }
        return startNow
    }
    
    /**
     * Start resources processing for the first time. If already run,
     * then has no effect.
     */
    def start() {
        if (! firstReloadAllDone) {
           unblock()
           reloadAll()
        } else {
            log.warn "Tried to start resources processing, but it has already been run!"
        }
    }
    
    /**
     * Reads 'processing.startup' configuration setting, then calls superclass method
     */
    @Override
    void afterPropertiesSet() {
        startup = getConfigParamOrDefault('processing.startup', true)
        super.afterPropertiesSet()
    }
    
    /**
     * The resources plugin for some reason does not apply the module dependency
     * ordering to the resources batch when reloading modules.
     * <p>
     * This method first reorders the resource batch into the correct ordering,
     * then calls the superclass method.
     * <p>
     * Note that ordering is important to GSP resources, because we have to
     * ensure the resources the GSP depends on have been processed before the
     * GSP itself! 
     */
    @Override
    void prepareResourceBatch(ResourceProcessorBatch batch) {
        def orderedNames = super.modulesInDependencyOrder
        
        // Ensure we have module ordering already
        if (orderedNames) {
            
            // Log
            if (log.isDebugEnabled()) {
                log.debug "Reordering resources according to module dependency ordering: ${orderedNames}"
            }
            
            // Create new resources set with correct ordering
            LinkedHashSet<ResourceMeta> orderedResources = new LinkedHashSet<ResourceMeta>()
            orderedNames.each { name ->
                batch.each { r ->
                    if (r.module.name == name) {
                        orderedResources << r
                    }
                }
            }
            
            // Just in case - add any resources that haven't yet been added
            batch.each { orderedResources << it }
            
            // Copy ordered resources to new batch
            ResourceProcessorBatch orderedBatch = new ResourceProcessorBatch()
            orderedBatch.add(orderedResources as List)
            
            // Call superclass method with ordered batch
            super.prepareResourceBatch(orderedBatch)
            
        // Ordering not found - just pass through for normal processing
        } else {
            super.prepareResourceBatch(batch)
        }
    }
    
    /**
     * The resources plugin doesn't recognise resources ending with '.gsp'.
     * This method tries to find the target resources type by first stripping
     * off the '.gsp' (if it exists). This should work for GSP resource files
     * named like myscript.js.gsp
     * <p>
     * Note that if all else fails, you can use the 'attrs' resources
     * configuration setting to manually tell the resources plugin
     * the type of the compiled resource. 
     */
    @Override
    def getDefaultSettingsForURI(uri, typeOverride = null) {
        // Try superclass first
        def result = super.getDefaultSettingsForURI(uri, typeOverride)
        
        // No type - could be unrecognised because is GSP
        if (!result && uri && !typeOverride) {
            def bareUri = removeQueryParams(uri)
            if (gspResourceLocator.isGsp(bareUri)) {
                def targetUri = gspResourceLocator.generateCompiledFilenameFromOriginal(bareUri)
                result = super.getDefaultSettingsForURI(targetUri)
                
                // Set type attribute in module definition if not explicitly configured
                if (result && !(result.attrs?.type)) {
                    def type = getResourceTypeFromUri(targetUri)
                    if (type) {
                        if (result.attrs) {
                            result.attrs.type = type
                        } else {
                            result.attrs = [type:type]
                        }
                    }
                }
            }
        }
        
        return result
    }
    
    /**
     * Gets the resource type for the specified uri.
     * <p>
     * Note that this method treats some file types as javascript resources,
     * since the Resources plugin will not recognise them otherwise. These 
     * include: html, xhtml, xml, csv
     */
    def getResourceTypeFromUri(String uri) {
        if (gspResourceLocator.isGsp(uri)) {
            uri = gspResourceLocator.generateCompiledFilenameFromOriginal(uri)
        }
        String type = uri ? FilenameUtils.getExtension(uri) : null
        // Explicitly support xml/html files with this 'hack'
        type = type in ['html','htm','xhtml','xml','csv'] ? 'js' : type
        return type
    }
    
    /**
     * Special logic to handle the case where debugging is enabled
     * but the request is for a GSP-rendered resource.
     * <p>
     * Ordinarily, in debug mode all resources are served directly from
     * the file system, without any processing. But for GSP resources,
     * we DO need to do some processing, because the rendered GSP file
     * is not located within the app itself, but in a temporary directory on
     * the file system. We have to explicitly write the rendered GSP file
     * contents to the response.
     * <p>
     * So we override this method in order to tell the resources plugin
     * NOT to use debug mode for GSP resources.
     */
    @Override
    public boolean isDebugMode(ServletRequest request) {
        boolean isDebug = super.isDebugMode(request)
        if (isDebug) {
            
            // Check if a GSP resource is being requested
            boolean isGspResource
            
            // Shortcut - we've already checked the request before
            def debugGsp = request.getAttribute('resources.debug.gsp')
            if (debugGsp != null) {
                isGspResource = debugGsp
                
            // Not previously checked this request to see if is GSP - so check now
            } else {
                String uri = ResourceProcessor.removeQueryParams(extractURI(request, true))
                isGspResource = isGeneratedFromGsp(uri)
                if (log.debugEnabled) {
                    log.debug "${isGspResource ? 'GSP' : 'NON-GSP'}: ${request.requestURI}"
                }
                request.setAttribute('resources.debug.gsp', isGspResource)
            }
            
            // Only allow debug mode if not a GSP resource
            isDebug = !isGspResource
        }
        return isDebug
    }
    
    /**
     * Special logic to handle the case where debugging is enabled
     * but the request is for a GSP-rendered resource.
     * <p>
     * Ordinarily, in debug mode all resources are served directly from
     * the file system, without any processing. But for GSP resources,
     * we DO need to do some processing, because the rendered GSP file
     * is not located within the app itself, but in a temporary directory on
     * the file system. We have to explicitly write the rendered GSP file
     * contents to the response.
     * <p>
     * So we override this method in order to serve the rendered file,
     * but ONLY in debug mode. In non-debug mode, the resources plugin
     * handles everything as normal.
     */
    @Override
    public boolean processLegacyResource(request, response) {
        def debugGsp = request.getAttribute('resources.debug.gsp')
        if (debugGsp) {
            // Find the ResourceMeta for the request, or create it
            String uri = ResourceProcessor.removeQueryParams(extractURI(request, true))
            def inf
            try {
                inf = getResourceMetaForURI(uri, false)
            } catch (FileNotFoundException fnfe) {
                response.sendError(404, fnfe.message)
                return true
            }
            
            serveResource(inf, request, response)
            return true
        } else {
            return super.processLegacyResource(request, response)
        }
    }
    
    /**
     * Reads the resource from the file system, and writes it to the response.
     * 
     * @param inf The resource to read
     * @param request The servlet request
     * @param response The servlet response to which the resource contents will be written
     */
    protected void serveResource(ResourceMeta inf, ServletRequest request, ServletResponse response) {
        // If we have a file, go for it
        if (inf?.exists()) {
            if (log.debugEnabled) {
                log.debug "Serving resource ${request.requestURI}"
            }
            def data = inf.newInputStream()
            try {
                // Now set up the response
                response.contentType = inf.contentType
                response.setContentLength(inf.contentLength)
                response.setDateHeader('Last-Modified', inf.originalLastMod)
                response.outputStream << data
            } finally {
                data?.close()
            }
        } else {
            response.sendError(404)
        }
    }
    
    /**
     * Delegates the specified GSP resource to a synthetic resource that does
     * the actual rendering. 
     * 
     * @param resource The resource representing the GSP URI
     * @param gsp The map containing the GSP file details
     */
    protected void prepareGspResource(ResourceMeta resource, Map gsp) {
        if (!resource || !gsp) {
            throw new NullPointerException("Missing arguments! Resource: ${resource} GSP: ${gsp}")
        }

        // Generate the url of the compiled resource
        resource.actualUrl = gspResourceLocator.generateCompiledFilenameFromOriginal(resource.originalUrl)

        // Even in debug mode, we want to see the rendered resource not the original GSP
        resource.originalUrl = resource.actualUrl
        
        // Set the rendered by tag attribute
        if (!resource.attributes) resource.attributes = [:]
        resource.attributes.'generated.from.gsp' = true

        // Set the type tag attribute
        if (!resource.tagAttributes) resource.tagAttributes = [:]
        resource.tagAttributes.type = getResourceTypeFromUri(resource.actualUrl)

        // Now create the synthetic resource that this resource is going to delegate to
        def gspResource = findSyntheticResourceById(resource.actualUrl)
        if (! gspResource) {
            // Creates a new resource and empty file
            gspResource = newSyntheticResource(resource.actualUrl, GspResourceMeta)
            gspResource.id = resource.actualUrl
            gspResource.contentType = resource.contentType
            gspResource.disposition = resource.disposition
            gspResource.gsp = gsp
            gspResource.attributes = resource.attributes
            gspResource.tagAttributes = resource.tagAttributes
            gspResource.gspResourceLocator = gspResourceLocator
            gspResource.gspResourcePageRenderer = gspResourcePageRenderer
            if (log.debugEnabled) {
                log.debug "Created synthetic GSP resource: ${resource.actualUrl}"
            }
        } else {
            if (log.debugEnabled) {
                log.debug "Synthetic GSP resource already exists: ${resource.actualUrl}"
            }
        }
        
        // Delegate
        resource.delegateTo(gspResource)
    }
    
    /**
     * Returns true if the specified URI is for a GSP-generated resource.
     */
    public boolean isGeneratedFromGsp(String uri) {
        return findResourceForURI(uri)?.attributes?.'generated.from.gsp'
    }    
}
