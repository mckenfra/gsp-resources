package org.grails.plugin.resource.gsp

import javax.servlet.ServletContext
import javax.servlet.http.Cookie

import org.apache.commons.logging.LogFactory

import org.codehaus.groovy.grails.commons.GrailsApplication;
import org.codehaus.groovy.grails.web.pages.discovery.GrailsConventionGroovyPageLocator
import org.codehaus.groovy.grails.web.pages.discovery.GroovyPageStaticResourceLocator;
import org.codehaus.groovy.grails.web.pages.FastStringWriter
import org.codehaus.groovy.grails.web.pages.GroovyPagesTemplateEngine
import org.codehaus.groovy.grails.web.servlet.GrailsApplicationAttributes;
import org.codehaus.groovy.grails.web.servlet.mvc.GrailsWebRequest

import org.grails.plugin.resource.gsp.servlet.BackgroundRequest
import org.grails.plugin.resource.gsp.servlet.BackgroundResponse

import org.springframework.context.ApplicationContext
import org.springframework.context.ApplicationContextAware
import org.springframework.web.context.ServletContextAware
import org.springframework.web.context.request.RequestContextHolder

/**
 * Similar to grails.gsp.PageRender, which is included in
 * grails 2.0.x. However, it includes a few enhancements:
 * <ol type="1">
 * 
 * <li>The original PageRenderer is restricted to rendering Views. I.e.
 * the GSPs must exist in the views directory. This class allows
 * specifying a GSP as a 'resource' - i.e. it can be located outside
 * the views directory.<br/></li>
 * 
 * <li>The page context is correctly set in the mock request object. With the
 * original grails PageRenderer in Grails 2.0.1, this is currently missing.
 * Therefore any calls to resource(dir:'/') in a GSP may not render
 * the path correctly.<br/></li>
 * 
 * <li>Other request attributes (e.g. session, remoteHost, cookies) that
 * are generally required for GSP rendering are also supported.</li>
 * 
 * </ol>
 *
 * @author Graeme Rocher / Francis McKenzie
 */
class GspResourcePageRenderer implements ApplicationContextAware, ServletContextAware {
    def log = LogFactory.getLog(this.class)
    
    // Passed in constructor
    protected GroovyPagesTemplateEngine templateEngine
    
    // Injected
    GrailsConventionGroovyPageLocator groovyPageLocator
    GroovyPageStaticResourceLocator grailsResourceLocator
    ApplicationContext applicationContext
    ServletContext servletContext

    GspResourcePageRenderer(GroovyPagesTemplateEngine templateEngine) {
        this.templateEngine = templateEngine
    }

    /**
     * Renders a page and returns the contents
     *
     * @param args The named arguments
     *
     * @arg view The view to render. Must be an absolute view path since the controller name is unknown.
     * @arg template The template to render. Must be an absolute template path since the controller name is unknown.
     * @arg resource The resource to render. Must be an absolute template path since the controller name is unknown.
     * @arg model The model to use for rendering
     *
     * @return The resulting string contents
     */
    String render(Map args) {
        def fsw = new FastStringWriter()
        renderToWriter(args, fsw)
        return fsw.toString()
    }

    /**
     * Renders a page and returns the contents
     *
     * @param args The named arguments
     * @param writer The target writer
     *
     * @arg view The view to render. Must be an absolute view path since the controller name is unknown.
     * @arg template The template to render. Must be an absolute template path since the controller name is unknown.
     * @arg resource The resource to render. Must be an absolute template path since the controller name is unknown.
     * @arg model The model to use for rendering
     *
     * @return The resulting string contents
     */
    void renderTo(Map args, Writer writer) {
        renderToWriter(args, writer)
    }
    /**
     * Renders a page and returns the contents
     *
     * @param args The named arguments
     * @param stream The target stream
     *
     * @arg view The view to render. Must be an absolute view path since the controller name is unknown.
     * @arg template The template to render. Must be an absolute template path since the controller name is unknown.
     * @arg resource The resource to render. Must be an absolute template path since the controller name is unknown.
     * @arg model The model to use for rendering
     *
     * @return The resulting string contents
     */

    void renderTo(Map args, OutputStream stream) {
        renderTo(args, new OutputStreamWriter(stream))
    }

    /**
     * Internal method - renders a page and returns the contents. The public
     * methods call this method.
     * 
     * @param args The named arguments
     * @param writer The target writer
     *
     * @arg view The view to render. Must be an absolute view path since the controller name is unknown.
     * @arg template The template to render. Must be an absolute template path since the controller name is unknown.
     * @arg resource The resource to render. Must be an absolute template path since the controller name is unknown.
     * @arg model The model to use for rendering
     *
     * @return The resulting string contents
     */
    protected void renderToWriter(Map args, Writer writer) {
        def source = null
        if (args.view) {
           source = groovyPageLocator.findViewByPath(args.view.toString())
        }
        else if (args.template) {
            source = groovyPageLocator.findTemplateByPath(args.template.toString())
        }
        else if (args.resource) {
            source = grailsResourceLocator.findResourceForURI(args.resource.toString())
        }
        if (source == null) {
            return
        }

        def oldRequestAttributes = RequestContextHolder.getRequestAttributes()
        try {
            // In case rendering is done before application is fully started up - need to initialise sevletContext
            if (! servletContext.getAttribute(GrailsApplicationAttributes.APPLICATION_CONTEXT)) {
                servletContext.setAttribute(GrailsApplicationAttributes.APPLICATION_CONTEXT, applicationContext)
            }
            List<Cookie> cookies = []
            def webRequest = new GrailsWebRequest(
                new BackgroundRequest(source.URI, servletContext, applicationContext, cookies),
                new BackgroundResponse(writer instanceof PrintWriter ? writer : new PrintWriter(writer), cookies),
                servletContext, applicationContext)
            RequestContextHolder.setRequestAttributes(webRequest)
            def template = templateEngine.createTemplate(source)
            if (template != null) {
                template.make(args.model ?: [:]).writeTo(writer)
            }
        } finally {
            RequestContextHolder.setRequestAttributes(oldRequestAttributes)
        }
    }    
}
