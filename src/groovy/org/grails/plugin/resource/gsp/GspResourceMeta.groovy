package org.grails.plugin.resource.gsp

import org.apache.commons.logging.LogFactory
import org.grails.plugin.resource.ResourceMeta
import org.grails.plugin.resource.gsp.GspResourcePageRenderer

/**
 * Synthetic resource that compiles a GSP to a target file.
 *
 * @author Stefan Kendell
 * @author Francis McKenzie
 */
class GspResourceMeta extends ResourceMeta {
    def log = LogFactory.getLog(this.class)

    // Set when this resource meta is created by GspResourceMapper
    GspResourceLocator gspResourceLocator
    GspResourcePageRenderer gspResourcePageRenderer
    Map gsp
    
    /**
     * Overridden method that gets called by grailsResourceProcessor when processing this
     * resource (either on startup or when changed).
     * 
     * This method triggers the GSP compilation.
     */
    @Override
    void beginPrepare(grailsResourceProcessor) {
        if (grailsResourceProcessor && gspResourcePageRenderer && gspResourceLocator && gsp && gsp.file && gsp.type && gsp.uri) {
            
            // Generate the target file from the GSP
            String outputFilename = gspResourceLocator.generateCompiledFilenameFromOriginal(gsp.file.absolutePath)
            File output = new File(outputFilename)
            
            // Compile
            if (log.isDebugEnabled()) {
                log.debug "Compiling GSP - From: ${gsp.file} To: ${output}"
            }
            String compiledText = compileGsp(gsp)
            
            // Check returned something
            if (compiledText) {
                output.write(compiledText, "UTF-8")
                output.setLastModified(gsp.file.lastModified())
                
                // Update this resource
                this.processedFile = output
                this.actualUrl = this.sourceUrl
                
                if (log.isDebugEnabled()) {
                    log.debug "Compiled GSP - From: ${gsp.file} To: ${output}"
                }
               
            // Compile returned nothing - log a warning, and give up
            } else {
                log.warn "No output from GSP compilation: ${gsp.file}"
            }
        
        // This GspResourceMeta has not been set up properly
        } else {
            throw new IllegalArgumentException("Some required variables missing for GSP ${gsp}")
        }
    }

    /**
     * Performs the compilation of the specified GSP, and returns the output.
     * 
     * Note that the GSP will be looked up by URI (i.e. not by actual file path)
     * 
     * @param gsp Map containing 'uri', 'file' and 'type' elements
     * @return Output of compilation as String
     */
    protected String compileGsp(Map gsp) {
        return gsp ? gspResourcePageRenderer.render([ (gsp.type) : gsp.uri ]) : ""
    }
}