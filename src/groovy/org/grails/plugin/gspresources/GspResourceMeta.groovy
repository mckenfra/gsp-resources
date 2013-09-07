package org.grails.plugin.gspresources

import org.apache.commons.logging.LogFactory
import org.grails.plugin.resource.ResourceMeta
import org.grails.plugin.resource.ResourceModule
import org.springframework.core.io.FileSystemResource;
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
     * The original GSP resource on the file system
     * <p>
     * Set when this resource meta is created by {@link GspResourceMapper}
     */
    Resource gsp
    /**
     * The resource module of the original GSP resource
     * <p>
     * Set when this resource meta is created by {@link GspResourceMapper}
     */
    ResourceModule gspModule
    /**
     * The rendered output file.
     * <p>
     * Used in debug mode.
     */
    File renderedFile
    /**
     * The content length of the rendered output file.
     * <p>
     * Used in debug mode.
     */
    Integer renderedContentLength
    /**
     * The content type of the rendered output file.
     * <p>
     * Used in debug mode.
     */
    String renderedContentType
    /**
     * The last modified of the rendered output file.
     * <p>
     * Used in debug mode.
     */
    Long renderedLastModified
    
    /**
     * Overridden method that gets called by grailsResourceProcessor when processing this
     * resource (either on startup or when changed).
     * <p>
     * This method triggers the GSP compilation.
     */
    @Override
    void beginPrepare(grailsResourceProcessor) {
        if (grailsResourceProcessor && gsp?.file) {

            // Generate the target file from the GSP
            actualUrl = sourceUrl
            processedFile = grailsResourceProcessor.makeFileForURI(actualUrl)
            processedFile.createNewFile()
            
            // Compile
            if (log.isDebugEnabled()) {
                log.debug "Compiling GSP - From: ${gsp.file} To: ${processedFile}"
            }
            String compiledText = grailsResourceProcessor.renderGsp(gsp)
            
            // Check returned something
            if (compiledText) {
                processedFile.write(compiledText, "UTF-8")
                
                if (log.isDebugEnabled()) {
                    log.debug "Compiled GSP - From: ${gsp.file} To: ${processedFile}"
                    log.debug processedFile.text
                }
               
            // Compile returned nothing - log a warning
            } else {
                log.warn "No output from GSP compilation: ${gsp.file}"
            }
            
            // The original resource is the rendered output, which may get changed later
            setOriginalResource(new FileSystemResource(processedFile))
            
            // Set content type - gets reset before prepare
            contentType = grailsResourceProcessor.getMimeType(actualUrl)
            
            // Same last modified as GSP itself
            processedFile.setLastModified(gsp.file.lastModified())
            
            // Store result for use in debug mode
            renderedFile = new File(processedFile.toString())
            renderedContentLength = contentLength
            renderedContentType = contentType
            renderedLastModified = processedFile.lastModified()
            
            // Hack to ensure rendered file is included in bundle at same position
            // as declared in resources configuration.
            // If we don't do this, the rendered file is always included at the end
            // of the bundle, after all non-GSP-type resources.
            if (gspModule) module = gspModule
            
        // This GspResourceMeta has not been set up properly
        } else {
            throw new IllegalArgumentException("Some required variables missing for GSP ${gsp?.file}")
        }
    }
}