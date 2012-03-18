package org.grails.plugin.resource.gsp

import java.io.IOException;
import java.lang.reflect.Modifier;
import java.security.Principal
import java.util.Collection;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentLinkedQueue

import javax.servlet.AsyncContext;
import javax.servlet.AsyncListener;
import javax.servlet.DispatcherType;
import javax.servlet.RequestDispatcher
import javax.servlet.ServletContext
import javax.servlet.ServletException;
import javax.servlet.ServletInputStream
import javax.servlet.ServletOutputStream
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.Cookie
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse
import javax.servlet.http.HttpSession
import javax.servlet.http.HttpSessionContext;
import javax.servlet.http.Part;

import org.apache.commons.collections.iterators.IteratorEnumeration
import org.apache.commons.logging.LogFactory
import org.codehaus.groovy.grails.commons.GrailsApplication;
import org.codehaus.groovy.grails.commons.metaclass.MetaClassEnhancer;
import org.codehaus.groovy.grails.web.pages.FastStringWriter
import org.codehaus.groovy.grails.web.pages.GroovyPagesTemplateEngine
import org.codehaus.groovy.grails.web.pages.GroovyPagesUriSupport
import org.codehaus.groovy.grails.web.pages.TagLibraryLookup;
import org.codehaus.groovy.grails.web.servlet.GrailsApplicationAttributes;
import org.codehaus.groovy.grails.web.servlet.mvc.GrailsWebRequest
import org.springframework.context.ApplicationContext
import org.springframework.context.ApplicationContextAware
import org.springframework.core.io.Resource
import org.springframework.core.io.ResourceLoader
import org.springframework.web.context.ServletContextAware
import org.springframework.web.context.request.RequestContextHolder
import org.springframework.web.context.support.ServletContextResourceLoader
import org.codehaus.groovy.grails.web.pages.discovery.GroovyPageLocator
import org.codehaus.groovy.grails.web.pages.discovery.GroovyPageScriptSource
import org.codehaus.groovy.grails.web.pages.discovery.GroovyPageResourceScriptSource
import org.codehaus.groovy.grails.web.pages.discovery.GroovyPageCompiledScriptSource
import org.codehaus.groovy.grails.web.pages.discovery.GrailsConventionGroovyPageLocator
import org.codehaus.groovy.grails.web.pages.discovery.GroovyPageStaticResourceLocator;
import org.grails.plugin.resource.JavaScriptBundleResourceMeta;

/**
 * This is almost a straight copy of grails.gsp.PageRender, which is included in
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
    GrailsApplication grailsApplication
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
        renderViewToWriter(args, fsw)
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
        renderViewToWriter(args, writer)
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


    private void renderViewToWriter(Map args, Writer writer) {
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
            def webRequest = new GrailsWebRequest(new PageRenderRequest(source.URI, servletContext, applicationContext, cookies),
                  new PageRenderResponse(writer instanceof PrintWriter ? writer : new PrintWriter(writer), cookies),
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
    
    protected GroovyPageScriptSource findResource(String basePath) {
        return groovyPageLocator.findViewByPath(basePath)
    }

    /**
     * A request object used during the GSP rendering pipeline for
     * render operations outside a web request
     */
    class PageRenderRequest implements HttpServletRequest {
        // Internal
        protected ServletContext _servletContext
        protected String _contextPath
        protected String _requestURI
        protected Map _params
        protected Map _attributes
        protected Cookie[] _cookies
        protected HttpSession _session
        
        // Public fields
        String contentType
        String characterEncoding = "UTF-8"

        public PageRenderRequest(Object requestURI, Object servletContext, Object applicationContext, List<Cookie> cookies) {
            this._requestURI = requestURI.toString()
            this._servletContext = servletContext
            this._contextPath = servletContext.contextPath
            this._params = [:]
            this._attributes = [:]
            this._attributes.put(GrailsApplicationAttributes.APP_URI_ATTRIBUTE, servletContext.contextPath)
            this._attributes.put('applicationContext', applicationContext)
            this._session = new PageRenderSession(servletContext)
            this._cookies = cookies as Cookie[]
        }
        
        String getAuthType() { null }

        public Cookie[] getCookies() { this._cookies }

        long getDateHeader(String name) { -1L }

        String getHeader(String name) { null }

        Enumeration<String> getHeaders(String name) { new IteratorEnumeration([].iterator()) }

        Enumeration<String> getHeaderNames() { new IteratorEnumeration([].iterator()) }

        int getIntHeader(String name) { -1 }

        String getMethod() { "GET" }

        String getPathInfo() { "" }

        String getPathTranslated() { "" }

        String getContextPath() { this._contextPath }

        String getQueryString() { "" }

        String getRemoteUser() { null }

        boolean isUserInRole(String role) { false }

        Principal getUserPrincipal() { null }

        String getRequestedSessionId() { this._session.id }

        public String getRequestURI() { this._requestURI }
        
        StringBuffer getRequestURL() { new StringBuffer(getRequestURI()) }

        String getServletPath() { "/" }

        public HttpSession getSession(boolean create) { this._session }

        public HttpSession getSession() { this._session }

        boolean isRequestedSessionIdValid() { true }

        boolean isRequestedSessionIdFromCookie() { false }

        boolean isRequestedSessionIdFromURL() { true }

        boolean isRequestedSessionIdFromUrl() { true }

        Object getAttribute(String name) { this._attributes.get(name) }

        Enumeration<String> getAttributeNames() { new IteratorEnumeration(this._attributes.keySet().iterator()) }

        int getContentLength() { 0 }

        ServletInputStream getInputStream() {
            throw new UnsupportedOperationException("You cannot read the input stream in non-request rendering operations")
        }

        String getParameter(String name) { this._params.get(name) }

        Enumeration<String> getParameterNames() { new IteratorEnumeration(this._params.keySet().iterator()) }

        String[] getParameterValues(String name) { this._params.values() as String[] }

        Map getParameterMap() { this._params }

        String getProtocol() {
            throw new UnsupportedOperationException("You cannot read the protocol in non-request rendering operations")
        }

        String getScheme() {
            throw new UnsupportedOperationException("You cannot read the scheme in non-request rendering operations")
        }

        String getServerName() {
            throw new UnsupportedOperationException("You cannot read server name in non-request rendering operations")
        }

        int getServerPort() {
            throw new UnsupportedOperationException("You cannot read the server port in non-request rendering operations")
        }

        BufferedReader getReader() {
            throw new UnsupportedOperationException("You cannot read input in non-request rendering operations")
        }

        String getRemoteAddr() { "127.0.0.1" }

        String getRemoteHost() { "localhost" }

        void setAttribute(String name, Object o) { this._attributes.put(name, o) }

        void removeAttribute(String name) { this._attributes.remove(name) }

        Locale getLocale() { Locale.getDefault() }

        Enumeration<Locale> getLocales() { new IteratorEnumeration(Locale.getAvailableLocales().iterator()) }

        boolean isSecure() { false }

        RequestDispatcher getRequestDispatcher(String path) { this._servletContext.getRequestDispatcher(path) }

        String getRealPath(String path) { this._requestURI }

        int getRemotePort() { 80 }

        String getLocalName() { "localhost" }

        String getLocalAddr() { "127.0.0.1" }

        int getLocalPort() { 80 }

        public ServletContext getServletContext() { this._servletContext }

        public AsyncContext startAsync() {
            throw new UnsupportedOperationException("You cannot get start async in non-request rendering operations")
        }

        public AsyncContext startAsync(ServletRequest servletRequest,
                ServletResponse servletResponse) {
            throw new UnsupportedOperationException("You cannot get start async in non-request rendering operations")
        }

        public boolean isAsyncStarted() { false }

        public boolean isAsyncSupported() { false }

        public AsyncContext getAsyncContext() {
            throw new UnsupportedOperationException("You cannot get get async context in non-request rendering operations")
        }

        public DispatcherType getDispatcherType() { DispatcherType.REQUEST }

        public boolean authenticate(HttpServletResponse response)
                throws IOException, ServletException { false }

        public void login(String username, String password)
                throws ServletException { /** No-op **/ }

        public void logout() throws ServletException { /** No-op **/ }

        public Collection<Part> getParts() throws IOException,
                IllegalStateException, ServletException { null }

        public Part getPart(String name) throws IOException,
                IllegalStateException, ServletException { null }
    }

    /**
     * A response object used during the GSP rendering pipeline for
     * render operations outside a web request
     */
    class PageRenderResponse implements HttpServletResponse {
        // Internal
        protected PrintWriter _writer
        protected List<Cookie> _cookies
        protected boolean _isCommitted
        protected Map _headers = [:]
        
        // Public fields
        String characterEncoding = "UTF-8"
        String contentType
        Locale locale = Locale.getDefault()
        int bufferSize = 0
        int contentLength = 0
        int status = 0
        
        public PageRenderResponse(PrintWriter writer, List<Cookie> cookies) {
            this._writer = writer
            this._cookies = cookies
        }

        public PrintWriter getWriter() { this._writer }
        
        void addCookie(Cookie cookie) { this._cookies.add(cookie) }

        boolean containsHeader(String name) { return this._headers.containsKey(name) }

        String encodeURL(String url) { url }

        String encodeRedirectURL(String url) { url }

        String encodeUrl(String url) { url }

        String encodeRedirectUrl(String url) { url }

        void sendError(int sc, String msg) {
            this.status = sc
            this._writer.print msg
        }

        void sendError(int sc) { this.status = sc }

        void sendRedirect(String location) { this.status = 302 }

        public Collection<String> getHeaders(String name) { this._headers.get(name) ?: [] }

        public Collection<String> getHeaderNames() { this._headers.keySet() }
        
        public String getHeader(String name) { this._headers.get(name)?.get(0) }

        void setHeader(String name, String value) { this._headers.put(name, [value] as List<String>) }

        void addHeader(String name, String value) {
            def values = this._headers.get(name)
            if (!values) {
                values = [] as List<String>
                this._headers.put(name, values)
            }
            values.add(value)
        }

        void setDateHeader(String name, long date) { setHeader(name, ""+date) }

        void addDateHeader(String name, long date) { addHeader(name, ""+date) }

        void setIntHeader(String name, int value) { setHeader(name, ""+value) }

        void addIntHeader(String name, int value) { addHeader(name, ""+value) }

        void setStatus(int sc, String sm) {
            this.status = sc
            this._writer.print(sm)
        }

        ServletOutputStream getOutputStream() {
            throw new UnsupportedOperationException("You cannot use the OutputStream in non-request rendering operations. Use getWriter() instead")
        }

        public boolean isCommitted() { this._isCommitted }

        void flushBuffer() { this._isCommitted = true }

        void resetBuffer() { /** No-op **/ }

        void reset() {
            this.status = 0
            this._headers.clear()
        }
    }
    
    /**
     * A session object used during the GSP rendering pipeline for
     * render operations outside a web request
     */
    class PageRenderSession implements HttpSession {
        // Internal
        protected ServletContext _servletContext
        protected Map _attributes = [:]
        
        public PageRenderSession(ServletContext servletContext) {
            this._servletContext = servletContext
        }
        public long getCreationTime() { 0 }
        
        public String getId() { "NOT-A-REAL-SESSION" }
        
        public long getLastAccessedTime() { 0 }
        
        public int getMaxInactiveInterval() { 0 }
        
        public ServletContext getServletContext() { this._servletContext }
        
        public void invalidate() { this._attributes.clear() }
        
        public boolean isNew() { false }
        
        public void setMaxInactiveInterval(int arg0) { /** No-op **/ }
        
        public Enumeration<String> getAttributeNames() { new IteratorEnumeration(this._attributes.keySet().iterator()) }
        
        public Object getAttribute(String name) { this._attributes.get(name) }
        
        public void setAttribute(String name, Object value) { this._attributes.put(name, value) }
        
        public void removeAttribute(String name) { this._attributes.remove(name) }
        
        // DEPRECATED METHODS
        
        @SuppressWarnings("deprecation")
        public HttpSessionContext getSessionContext() {
            throw new UnsupportedOperationException("You cannot get session context in non-request rendering operations")
        }
        
        @SuppressWarnings("deprecation")
        public String[] getValueNames() { this._attributes.keySet() as String[] }
        
        @SuppressWarnings("deprecation")
        public Object getValue(String name) { this._attributes.get(name) }
        
        @SuppressWarnings("deprecation")
        public void putValue(String name, Object value) { this._attributes.put(name, value) }

        @SuppressWarnings("deprecation")
        public void removeValue(String name) { this._attributes.remove(name) }
    }
}
