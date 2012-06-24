package org.grails.plugin.gspresources

import org.apache.commons.logging.LogFactory
import org.grails.plugin.resource.ResourceMeta
import org.springframework.core.io.Resource;

/**
 * Synthetic resource that compiles a GSP to a target file.
 *
 * @author Stefan Kendell
 * @author Francis McKenzie
 */
class GspResourceMeta extends ResourceMeta {
    def log = LogFactory.getLog(this.class)

    /**
     * Set when this resource meta is created by {@link GspResourceMapper} - does the rendering
     */
    GspResourcePageRenderer gspResourcePageRenderer
    /**
     * Set when this resource meta is created by {@link GspResourceMapper} - the GSP source
     */
    Resource gsp
    
    /**
     * Overridden method that gets called by grailsResourceProcessor when processing this
     * resource (either on startup or when changed).
     * <p>
     * This method triggers the GSP compilation.
     */
    @Override
    void beginPrepare(grailsResourceProcessor) {
        if (grailsResourceProcessor && gspResourcePageRenderer && gsp?.file) {

            // Generate the target file from the GSP
            actualUrl = sourceUrl
            processedFile = grailsResourceProcessor.makeFileForURI(actualUrl)
            processedFile.createNewFile()
            
            // Set content type
            def uriWithoutFragment = actualUrl
            if (actualUrl.contains('#')) {
                uriWithoutFragment = actualUrl.substring(0, actualUrl.indexOf('#'))
            }
            contentType = grailsResourceProcessor.getMimeType(uriWithoutFragment)
            
            // Compile
            if (log.isDebugEnabled()) {
                log.debug "Compiling GSP - From: ${gsp.file} To: ${processedFile}"
            }
            String compiledText = compileGsp(gsp)
            
            // Check returned something
            if (compiledText) {
                processedFile.write(compiledText, "UTF-8")
                contentLength = processedFile.size().toInteger()
                
                if (log.isDebugEnabled()) {
                    log.debug "Compiled GSP - From: ${gsp.file} To: ${processedFile}"
                }
               
            // Compile returned nothing - log a warning
            } else {
                log.warn "No output from GSP compilation: ${gsp.file}"
            }
            
            // Same last modified as GSP itself
            processedFile.setLastModified(gsp.file.lastModified())
            
            // Store the details about the rendered resource in the resource's attributes,
            // as they may be needed later if debug mode is used and the raw resources
            // needs to be served.
            grailsResourceProcessor.finaliseSyntheticGspResource(this)
       
        // This GspResourceMeta has not been set up properly
        } else {
            throw new IllegalArgumentException("Some required variables missing for GSP ${gsp?.file}")
        }
    }
    
    /**
     * Performs the compilation of the specified GSP, and returns the output.
     * 
     * @param gsp The GSP resource itself
     * @return Output of compilation as String
     */
    protected String compileGsp(Resource gsp) {
        return gsp ? gspResourcePageRenderer.render([source:gsp]) : ""
    }
}