package org.grails.plugin.gspresources

import javax.servlet.ServletContext
import javax.servlet.http.Cookie

import org.apache.commons.logging.LogFactory

import org.codehaus.groovy.grails.commons.GrailsApplication;
import org.codehaus.groovy.grails.web.pages.discovery.GrailsConventionGroovyPageLocator
import org.codehaus.groovy.grails.web.pages.discovery.GroovyPageScriptSource;
import org.codehaus.groovy.grails.web.pages.discovery.GroovyPageStaticResourceLocator;
import org.codehaus.groovy.grails.web.pages.FastStringWriter
import org.codehaus.groovy.grails.web.pages.GroovyPagesTemplateEngine
import org.codehaus.groovy.grails.web.servlet.GrailsApplicationAttributes;
import org.codehaus.groovy.grails.web.servlet.mvc.GrailsWebRequest

import org.grails.plugin.gspresources.servlet.BackgroundRequest
import org.grails.plugin.gspresources.servlet.BackgroundResponse

import org.springframework.context.ApplicationContext
import org.springframework.context.ApplicationContextAware
import org.springframework.core.io.Resource;
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
    
    /**
     * Injected - for finding a view or template
     */
    GrailsConventionGroovyPageLocator groovyPageLocator
    /**
     * Injected - for finding a static resource
     */
    GroovyPageStaticResourceLocator grailsResourceLocator
    /**
     * Injected - required for rendering
     */
    ApplicationContext applicationContext
    /**
     * Injected - required for rendering
     */
    ServletContext servletContext

    GspResourcePageRenderer(GroovyPagesTemplateEngine templateEngine) {
        this.templateEngine = templateEngine
    }

    /**
     * Renders a page and returns the contents
     *
     * @param args.view The URI of the view to render. Must be an absolute view path since the controller name is unknown.
     * @param args.template The URI of the template to render. Must be an absolute template path since the controller name is unknown.
     * @param args.resource The URI of the resource to render. Must be an absolute template path since the controller name is unknown.
     * @param args.source The resource to render - either a {@link org.codehaus.groovy.grails.web.pages.discovery.GroovyPageScriptSource} or a {@link org.springframework.core.io.Resource}
     * @param args.model The model to use for rendering
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
     * @param args.view The URI of the view to render. Must be an absolute view path since the controller name is unknown.
     * @param args.template The URI of the template to render. Must be an absolute template path since the controller name is unknown.
     * @param args.resource The URI of the resource to render. Must be an absolute template path since the controller name is unknown.
     * @param args.source The resource to render - either a {@link org.codehaus.groovy.grails.web.pages.discovery.GroovyPageScriptSource} or a {@link org.springframework.core.io.Resource}
     * @param args.model The model to use for rendering
     * @param writer The target writer
     *
     * @return The resulting string contents
     */
    void renderTo(Map args, Writer writer) {
        renderToWriter(args, writer)
    }
    /**
     * Renders a page and returns the contents
     *
     * @param args.view The URI of the view to render. Must be an absolute view path since the controller name is unknown.
     * @param args.template The URI of the template to render. Must be an absolute template path since the controller name is unknown.
     * @param args.resource The URI of the resource to render. Must be an absolute template path since the controller name is unknown.
     * @param args.source The resource to render - either a {@link org.codehaus.groovy.grails.web.pages.discovery.GroovyPageScriptSource} or a {@link org.springframework.core.io.Resource}
     * @param args.model The model to use for rendering
     * @param stream The target stream
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
     * @param args.view The URI of the view to render. Must be an absolute view path since the controller name is unknown.
     * @param args.template The URI of the template to render. Must be an absolute template path since the controller name is unknown.
     * @param args.resource The URI of the resource to render. Must be an absolute template path since the controller name is unknown.
     * @param args.source The resource to render - either a {@link org.codehaus.groovy.grails.web.pages.discovery.GroovyPageScriptSource} or a {@link org.springframework.core.io.Resource}
     * @param args.model The model to use for rendering
     * @param writer The target writer
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
        else if (args.source && (args.source instanceof GroovyPageScriptSource || args.source instanceof Resource)) {
            source = args.source
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
