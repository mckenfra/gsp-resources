package org.grails.plugin.gspresources

import javax.servlet.ServletRequest
import javax.servlet.ServletResponse

import org.apache.commons.io.FilenameUtils
import org.apache.commons.logging.LogFactory
import org.grails.plugin.resource.ResourceMeta
import org.grails.plugin.resource.ResourceProcessor
import org.grails.plugin.resource.ResourceProcessorBatch
import org.springframework.core.io.Resource

/**
 * Overrides the resources plugin's ResourceProcessor, to provide additional
 * features required for GSP processing
 * <p>
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
    def glog = LogFactory.getLog(getClass())

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
        blocked = true
    }

    /**
     * Unblock the reloadAll() function
     */
    def unblock() {
        blocked = false
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
                if (glog.debugEnabled) {
                    glog.debug "Begin initial resources processing"
                }
            }
            firstReloadAllDone = true
            super.reloadAll()
        } else {
            if (glog.debugEnabled) {
                glog.debug "Block resources processing"
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
            glog.info "${startNow ? 'Start' : 'Delay'} resources processing at plugin load"
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
            glog.info "${startNow ? 'Start' : 'Delay'} resources processing at plugin bootstrap"
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
            glog.warn "Tried to start resources processing, but it has already been run!"
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
     * Therefore we have to explicitly help it to get the correct ordering.
     * <p>
     * Note that ordering is important to GSP resources, because we have to
     * ensure the resources the GSP depends on have been processed before the
     * GSP itself!
     *
     * @param batch All resources that have been changed.
     * @return A new batch ordered correctly.
     */
    ResourceProcessorBatch orderResourceBatch(ResourceProcessorBatch batch) {
        def orderedNames = super.modulesInDependencyOrder

        // Ensure we have module ordering already
        if (!orderedNames) return batch

        // Log
        if (glog.debugEnabled) {
            glog.debug "Reordering resources according to module dependency ordering: ${orderedNames}"
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

        return orderedBatch
    }

    /**
     * Processing of synthetic resources is an issue for this plugin,
     * as it creates a synthetic resource for every original GSP
     * resource. The synthetic resource does the actual rendering
     * of the GSP and creation of the target file.
     * <p>
     * The problem is that the resources plugin also creates synthetic
     * resources that do the bundling. If the bundling resource is
     * processed before a GSP synthetic resource in that module
     * being bundled, then a null pointer exception is thrown, because
     * the GSP output file does not exist yet.
     * <p>
     * So in a nutshell, this method just puts all GSP synthetic
     * resources at the front of the specified list of synthetics.
     * That way, they will be processed first.
     *
     * @param synthetics All synthetics that have changed.
     * @return A new list ordered correctly.
     */
    List orderSyntheticResources(List synthetics) {
        return synthetics?.sort { a,b ->
            a in GspResourceMeta ?
            (b in GspResourceMeta ? 0 : -1) :
            (b in GspResourceMeta ? 1 : 0)
        }
    }

    /**
     * Override this key method in the resources plugin, and modify
     * its behaviour as follows:
     * <p>
     * <ul>
     *
     * <li>First, reorder the resources in the batch to ensure dependency
     * ordering, which is critical to GSP resources.
     * See {@link #orderResourceBatch(ResourceProcessorBatch)}</li>
     *
     * <li>Run the same processing of resources as would
     * have been done by the superclass method, but with one
     * critical change: before processing any synthetic resources,
     * ensure the ordering is correct.
     * See {@link #orderSyntheticResources(List)}</li>
     *
     * <li>Finally, force reloading of bundles containing GSP resources.
     * See {@link #didPrepareResourceBatch(ResourceProcessorBatch)}</li>
     *
     * </ul>
     */
    @Override
    void prepareResourceBatch(ResourceProcessorBatch batch) {

        // Added for completeness - currently does nothing
        willPrepareResourceBatch(batch)

        // Ordering is essential for GSPs (but not for static resources)
        batch = orderResourceBatch(batch)

        // ********************************************************
        // WHAT FOLLOWS IS A STRAIGHT COPY-AND-PASTE OF THIS METHOD
        // IN 'resources' PLUGIN v1.2, BUT WITH A
        // ONE-LINE ADDITION.
        //
        // UNFORTUNATELY, DUE TO THE WAY THE METHOD IS CODED,
        // THERE IS NO ALTERNATIVE.
        //
        // OUR KEY PROBLEM IS WE NEED TO INFLUENCE THE ORDERING
        // OF PROCESSING OF THE SYNTHETIC RESOURCES, TO ENSURE
        // THE GSP-SYNTHETICS ARE PROCESSED BEFORE THE
        // BUNDLING-SYNTHETICS
        // ********************************************************
        if (glog.debugEnabled) {
            glog.debug "Preparing resource batch:"
            batch.each { r ->
                glog.debug "Batch includes resource: ${r.sourceUrl}"
            }
        }

        def affectedSynthetics = []
        batch.each { r ->
            r.reset()
            resourceInfo.evict(r.sourceUrl)

            prepareSingleDeclaredResource(r) {
                def u = r.sourceUrl
                allResourcesByOriginalSourceURI[u] = r
            }

            if (r.delegating) {
                if (!(affectedSynthetics.find { it == r.delegate })) {
                    affectedSynthetics << r.delegate
                }
            }
        }

        // Synthetic resources are only known after processing declared resources
        if (glog.debugEnabled) {
            glog.debug "Preparing synthetic resources"
        }

        // ********************************************************
        // NOTE: THIS IS THE KEY CHANGE
        affectedSynthetics = orderSyntheticResources(affectedSynthetics)
        // ********************************************************

        // The rest is the same
        for (r in affectedSynthetics) {
            if (glog.debugEnabled) {
                glog.debug "Preparing synthetic resource: ${r.sourceUrl}"
            }

            r.reset()
            resourceInfo.evict(r.sourceUrl)

            prepareSingleDeclaredResource(r) {
                def u = r.sourceUrl
                allResourcesByOriginalSourceURI[u] = r
            }
        }
        // ********************************************************
        // END OF COPY-AND-PASTE
        // ********************************************************

        // Ensure changes to GSPs trigger a refresh of containing bundle
        didPrepareResourceBatch(batch)
    }

    /**
     * Added for completeness - currently does nothing
     *
     * @param batch All resources that have been changed.
     */
    void willPrepareResourceBatch(ResourceProcessorBatch batch) {
        // Do nothing
    }

    /**
     * Forces processing of any bundles that contain GSP resources
     * in the specified batch.
     * <p>
     * Note this takes place at the very end of the processing cycle,
     * after all other resources have been processed.
     *
     * @param batch All resources that have changed.
     */
    void didPrepareResourceBatch(ResourceProcessorBatch batch) {
        // Get list of GSP and non-GSP synthetics
        def gspSynthetics = []
        def otherSynthetics = []
        batch.each { r ->
            if (r.delegating) {
                if (r.delegate in GspResourceMeta) {
                    gspSynthetics << r.delegate
                } else {
                    otherSynthetics << r.delegate
                }
            }
        }

        // Find any bundles that contain changed-GSPs, but have not yet
        // been processed.
        def bundlingSyntheticsNeedingProcessing = gspSynthetics.findAll {
            it.delegating && !(it.delegate in otherSynthetics)
        }.collect { it.delegate }.unique()

        // Process those bundle synthetics
        for (r in bundlingSyntheticsNeedingProcessing) {
            if (glog.debugEnabled) {
                glog.debug "Preparing synthetic resource, because GSP was modified: ${r.sourceUrl}"
            }

            r.reset()
            resourceInfo.evict(r.sourceUrl)
            prepareSingleDeclaredResource(r)
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

                // Hack so that any GSP-rendered file is permitted by the resources plugin
                result = super.getDefaultSettingsForURI(targetUri) ?: [
                    disposition:'head', type:'js'
                ]

                // Set type attribute in module definition if not explicitly configured
                if (!result?.attrs?.type) {
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
    boolean isDebugMode(ServletRequest request) {
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
                if (glog.debugEnabled) {
                    glog.debug "${isGspResource ? 'GSP' : 'NON-GSP'}: ${request.requestURI}"
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
    boolean processLegacyResource(request, response) {
        def debugGsp = request.getAttribute('resources.debug.gsp')
        if (debugGsp) {
            serveGsp(request,response)
            return true
        } else {
            return super.processLegacyResource(request, response)
        }
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
    void processModernResource(request, response) {
        def debugGsp = request.getAttribute('resources.debug.gsp')
        if (debugGsp) {
            serveGsp(request,response)
        } else {
            super.processModernResource(request, response)
        }
    }

    /**
     * Reads the rendered GSP from the file system, and writes it to the response.
     *
     * @param request The servlet request
     * @param response The servlet response to which the resource contents will be written
     */
    protected void serveGsp(request,response) {
        // Find the ResourceMeta for the request, or create it
        String uri = ResourceProcessor.removeQueryParams(extractURI(request, true))
        def inf
        try {
            inf = getResourceMetaForURI(uri, false)
        } catch (FileNotFoundException fnfe) {
            response.sendError(404, fnfe.message)
        }

        GspResourceMeta gspResource = findGspResource(inf)
        if (gspResource) {
            serveResource(request, response,
                gspResource.renderedFile,
                gspResource.renderedContentType,
                gspResource.renderedContentLength,
                gspResource.renderedLastModified
            )
        } else {
            response.sendError(404)
        }
    }

    /**
     * Reads the resource from the file system, and writes it to the response.
     *
     * @param request The servlet request
     * @param response The servlet response to which the resource contents will be written
     * @param file The file to serve
     * @param contentType The content type of the resource
     * @param contentLength The content length of the resource; defaults to file's contentLength
     * @param lastModified The lastModified of the resource; defaults to file's lastModified
     */
    protected void serveResource(ServletRequest request, ServletResponse response, File file,
        String contentType = null, Integer contentLength = null, Long lastModified = null) {
        if (file?.exists()) {
            if (glog.debugEnabled) {
                glog.debug "Serving resource ${request.requestURI}"
            }
            def data = file.newInputStream()
            try {
                response.contentType = contentType ?: getMimeType(file.toString())
                response.setContentLength(contentLength ?: file.size().toInteger())
                response.setDateHeader('Last-Modified', lastModified ?: file.lastModified())
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
     * @param resource The original GSP resource
     * @param gsp The GSP resource file itself
     */
    protected void addSyntheticGspResource(ResourceMeta resource, Resource gsp) {
        if (!resource || !gsp) {
            throw new NullPointerException("Missing arguments! Resource: ${resource} GSP: ${gsp}")
        }

        // Generate the url of the compiled resource
        resource.actualUrl = gspResourceLocator.generateCompiledFilenameFromOriginal(resource.originalUrl)

        // Force debug mode to link to the rendered resource, not the original GSP
//        resource.originalUrl = resource.sourceUrl = resource.actualUrl

        // Set content type
        resource.contentType = getMimeType(resource.actualUrl)

        // Set the rendered attribute
        if (resource.attributes == null) resource.attributes = [:]
        resource.attributes.'gsp.rendered' = resource.actualUrl

        // Set the type tag attribute
        if (resource.tagAttributes == null) resource.tagAttributes = [:]
        resource.tagAttributes.type = resource.tagAttributes.type ?: getResourceTypeFromUri(resource.actualUrl)

        // Now create the synthetic resource that this resource is going to delegate to
        def rendered = findSyntheticResourceById(resource.actualUrl)
        if (! rendered) {
            // Creates a new resource and empty file
            rendered = newSyntheticResource(resource.actualUrl, GspResourceMeta)
            rendered.id = resource.actualUrl
            rendered.gsp = gsp
            rendered.contentType = resource.contentType
            rendered.disposition = resource.disposition
            rendered.attributes = resource.attributes
            rendered.tagAttributes = resource.tagAttributes
            rendered.bundle = resource.bundle
            if (!rendered.bundle && resource.module.resources.size() > 1 && resource.tagAttributes.type in resource.module.bundleTypes) {
                def bundlePrefix = resource.module.defaultBundle ?: "bundle_${resource.module.name}"
                rendered.bundle = "${bundlePrefix}_${resource.disposition}"
            }
            rendered.originalModule = resource.module

            // Hack to ensure rendered file is included in bundle at same position
            // as declared in resources configuration.
            // If we don't do this, the rendered file is always included at the end
            // of the bundle, after all non-GSP-type resources.
            def bundleMapper = resourceMappers.find { it.name == 'bundle' }
            bundleMapper?.invokeIfNotExcluded(rendered)

            if (glog.debugEnabled) {
                glog.debug "Created synthetic GSP resource: ${resource.actualUrl}"
            }

        // Synthetic resource already created
        } else {
            if (glog.debugEnabled) {
                glog.debug "Synthetic GSP resource already exists: ${resource.actualUrl}"
            }
        }

        // When the rendered synthetic resource is eventually processed, it
        // changes its module to be that of the original GSP resource - this
        // ensures that it gets bundled in the correct order.
        // So we always need to reset the module during this earlier phase,
        // to ensure the rendered module is not processed too early
        // (i.e. incorrectly before other static resources)
        rendered.module = getModule(SYNTHETIC_MODULE)

        // Delegate
        resource.delegateTo(rendered)
    }

    /**
     * Returns true if the specified URI is for a GSP-generated resource.
     */
    boolean isGeneratedFromGsp(String uri) {
        return findResourceForURI(uri)?.attributes?.'gsp.rendered'
    }

    /**
     * Returns the GspResourceMeta for the rendered output of
     * the specified GSP resource - if it exists.
     */
    GspResourceMeta findGspResource(ResourceMeta resource) {
        if (resource instanceof GspResourceMeta) return resource
        else return findResourceForURI(resource.attributes.'gsp.rendered')
    }

    /**
     * Performs the rendering of the specified GSP, and returns the output.
     *
     * @param gsp The GSP resource itself
     * @return Output of rendering as String
     */
    protected String renderGsp(Resource gsp) {
        return gsp ? gspResourcePageRenderer.render([source:gsp]) : ""
    }
}
