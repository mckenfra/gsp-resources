package org.grails.plugin.gspresources

import javax.servlet.ServletContext

import org.apache.commons.logging.LogFactory
import org.codehaus.groovy.grails.web.pages.discovery.GrailsConventionGroovyPageLocator
import org.codehaus.groovy.grails.web.pages.discovery.GroovyPageScriptSource
import org.codehaus.groovy.grails.web.pages.discovery.GroovyPageStaticResourceLocator
import org.springframework.core.io.FileSystemResource
import org.springframework.core.io.Resource
import org.springframework.web.context.ServletContextAware

/**
 * For finding GSP files, which can be views, templates or resources.
 *
 * @author Francis McKenzie
 */
class GspResourceLocator implements ServletContextAware {
    def log = LogFactory.getLog(getClass())

    static GSP_FILE_EXTENSIONS = ['.gsp']

    /**
     * Injected - for finding a view or template
     */
    GrailsConventionGroovyPageLocator groovyPageLocator
    /**
     * Injected - for finding a static resource
     */
    GroovyPageStaticResourceLocator grailsResourceLocator
    /**
     * Injected - for getting the real path of a resource
     */
    ServletContext servletContext

    /**
     * Checks for <code>.gsp</code> suffix in filename or uri
     */
    boolean isGsp(fileOrUri) {
        def name = fileOrUri?.toString()?.toLowerCase()
        return name && GSP_FILE_EXTENSIONS.any { name.endsWith(it) }
    }

    /**
     * Removes <code>.gsp</code> suffix from filename
     */
    String generateCompiledFilenameFromOriginal(filename) {
        String result = filename?.toString()
        if (result) {
            GSP_FILE_EXTENSIONS.each { ext ->
                result = result.replaceAll(/(?i)\Q${ext}\E/, '')
            }
        }
        return result
    }

    /**
     * Finds a GSP file, using grailsResourceLocator and groovyPageLocator.
     * <p>
     * Note that the GSP may be a resource, a view or a template. This method tries
     * to find the GSP by searching those 3 types in that order. If a GSP is found,
     * a map is returned with the following elements:
     * <p>
     * <ul>
     * <li><code>uri</code>: uri String of GSP</li>
     * <li><code>source</code>: the source that was found - either a {@link org.codehaus.groovy.grails.web.pages.discovery.GroovyPageScriptSource} or a {@link org.springframework.core.io.Resource}</li>
     * <li><code>resource</code>: the resource that was found</li>
     * <li><code>type</code>: either 'view', 'template' or 'resource' depending on what was found</li>
     * </ul>
     * <p>
     * If no GSP found, an empty map is returned.
     *
     * @param uri The external URI of the GSP
     * @return Map of results (empty if no GSP found) as described above
     */
    Map findGsp(uri) {
        Map result = [:]
        if (uri) {
            result = findGspResource(uri)
            if (!result) result = findGspView(uri)
            if (!result) result = findGspTemplate(uri)
        }
        return result
    }

    /**
     * Finds a GSP file resource, using grailsResourceLocator.
     * <p>
     * If a GSP is found, a map is returned with the following elements:
     * <p>
     * <ul>
     * <li><code>uri</code>: uri String of GSP</li>
     * <li><code>source</code>: the source that was found - a {@link org.springframework.core.io.Resource}</li>
     * <li><code>resource</code>: the resource that was found</li>
     * <li><code>type</code>: 'resource'</li>
     * </ul>
     * <p>
     * If no GSP found, an empty map is returned.
     *
     * @param uri The external URI of the GSP
     * @return Map of results (empty if no GSP found) as described above
     **/
    Map findGspResource(uri) {
        if (log.isDebugEnabled()) {
            log.debug "FINDING: GSP Resource ${uri}....."
        }
        Map result
        try {
            Resource resource = grailsResourceLocator.findResourceForURI(uri)
            if (resource) {
                // Check if GSP
                File file = resource.file
                if (isGsp(file)) {
                    result = [uri:uri.toString(), type:'resource', source:resource, resource:resource]
                } else {
                    if (log.isDebugEnabled()) {
                        log.debug "IGNORING: ${file}"
                    }
                }
            }
        } catch(err) {
            log.warn "Error finding GSP resource ${uri} - ${err}"
        }
        return result
    }

    /**
     * Finds a GSP view file, using groovyPageLocator.
     * <p>
     * If a GSP is found, a map is returned with the following elements:
     * <p>
     * <ul>
     * <li><code>uri</code>: uri String of GSP</li>
     * <li><code>source</code>: the source that was found - a {@link org.codehaus.groovy.grails.web.pages.discovery.GroovyPageScriptSource}</li>
     * <li><code>resource</code>: the resource that was found</li>
     * <li><code>type</code>: 'view'</li>
     * </ul>
     * <p>
     * If no GSP found, an empty map is returned.
     *
     * @param uri The external URI of the GSP
     * @return Map of results (empty if no GSP found) as described above
     **/
    Map findGspView(uri) {
        if (log.isDebugEnabled()) {
            log.debug "FINDING: GSP View ${uri}....."
        }
        Map result
        try {
            GroovyPageScriptSource resource = groovyPageLocator.findViewByPath(uri.toString())
            if (resource) {
                // Check if GSP
                File file = new File(servletContext.getRealPath(resource.URI))
                if (isGsp(file)) {
                    result = [uri:uri.toString(), type:'view', source:resource, resource:new FileSystemResource(file)]
                } else {
                    if (log.isDebugEnabled()) {
                        log.debug "IGNORING: ${file}"
                    }
                }
            }
        } catch(err) {
            log.warn "Error finding GSP view ${uri} - ${err}"
        }
        return result
    }

    /**
     * Finds a GSP template file, using groovyPageLocator.
     * <p>
     * If a GSP is found, a map is returned with the following elements:
     * <p>
     * <ul>
     * <li><code>uri</code>: uri String of GSP</li>
     * <li><code>source</code>: the source that was found - a {@link org.codehaus.groovy.grails.web.pages.discovery.GroovyPageScriptSource}</li>
     * <li><code>resource</code>: the resource that was found</li>
     * <li><code>type</code>: 'template'</li>
     * </ul>
     * <p>
     * If no GSP found, an empty map is returned.
     *
     * @param uri The external URI of the GSP
     * @return Map of results (empty if no GSP found) as described above
     **/
    Map findGspTemplate(uri) {
        if (log.isDebugEnabled()) {
            log.debug "FINDING: GSP Template ${uri}....."
        }
        Map result
        try {
            GroovyPageScriptSource resource = groovyPageLocator.findTemplateByPath(uri.toString())
            if (resource) {
                // Check if GSP
                File file = new File(servletContext.getRealPath(resource.URI))
                if (isGsp(file)) {
                    result = [uri:uri.toString(), type:'template', source:resource, resource:new FileSystemResource(file)]
                } else {
                    if (log.isDebugEnabled()) {
                        log.debug "IGNORING: ${file}"
                    }
                }
            }
        } catch(err) {
            log.warn "Error finding GSP template ${uri} - ${err}"
        }
        return result
    }
}
