package org.grails.plugin.gspresources.servlet

import java.io.IOException;
import java.security.Principal
import java.util.Collection;
import java.util.Enumeration;

import javax.servlet.RequestDispatcher
import javax.servlet.ServletContext
import javax.servlet.ServletException;
import javax.servlet.ServletInputStream
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.Cookie
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse
import javax.servlet.http.HttpSession

// Servlet 3.0
//import javax.servlet.AsyncContext;
//import javax.servlet.DispatcherType;
//import javax.servlet.http.Part;
// Servlet 3.0

import org.apache.commons.collections.iterators.IteratorEnumeration
import org.codehaus.groovy.grails.web.servlet.GrailsApplicationAttributes;

/**
 * A request object used during the GSP rendering pipeline for
 * render operations outside a web request
 * 
 * Based on grails.gsp.PageRenderer, with following improvements:
 * 
 * <li>The page context is correctly set in the mock request object. With the
 * original grails PageRenderer in Grails 2.0.1, this is currently missing.
 * Therefore any calls to resource(dir:'/') in a GSP may not render
 * the path correctly.<br/></li>
 * 
 * <li>Other request attributes (e.g. session, remoteHost, cookies) that
 * are generally required for GSP rendering are also supported.</li>
 * 
 * @author Graeme Rocher / Francis McKenzie
 */
public class BackgroundRequest implements HttpServletRequest {
    // Internal
    protected String _requestURI
    protected String _contextPath
    protected ServletContext _servletContext
    protected Map _params
    protected Map _attributes
    protected Cookie[] _cookies
    protected HttpSession _session
    
    // Public fields
    String contentType
    String characterEncoding = "UTF-8"

    public BackgroundRequest(Object requestURI, Object servletContext, Object applicationContext, List<Cookie> cookies) {
        this._requestURI = requestURI.toString()
        this._servletContext = servletContext
        this._contextPath = servletContext.contextPath
        this._params = [:]
        this._attributes = [:]
        this._attributes.put(GrailsApplicationAttributes.APP_URI_ATTRIBUTE, servletContext.contextPath)
        this._attributes.put('applicationContext', applicationContext)
        this._cookies = cookies as Cookie[]
    }
    
    // SERVLET 2.x
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

    String getRequestedSessionId() { this._session?.id }

    public String getRequestURI() { this._requestURI }
    
    StringBuffer getRequestURL() { new StringBuffer(getRequestURI()) }

    String getServletPath() { "/" }

    public HttpSession getSession(boolean create) {
        _session || !create ? _session : (_session = new BackgroundSession(_servletContext))
    }

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
    // SERVLET 2.x
    
    // SERVLET 3.0
    public boolean isAsyncStarted() { false }

    public boolean isAsyncSupported() { false }

    public boolean authenticate(HttpServletResponse response)
            throws IOException, ServletException { false }

    public void login(String username, String password) throws ServletException { /** No-op **/ }

    public void logout() throws ServletException { /** No-op **/ }

    public ServletContext getServletContext() { this._servletContext }

    def getDispatcherType() { Enum.valueOf(Class.forName('javax.servlet.DispatcherType'), 'REQUEST') }

    def startAsync() {
        throw new UnsupportedOperationException("You cannot get start async in non-request rendering operations")
    }

    def startAsync(ServletRequest servletRequest,
            ServletResponse servletResponse) {
        throw new UnsupportedOperationException("You cannot get start async in non-request rendering operations")
    }

    def getAsyncContext() {
        throw new UnsupportedOperationException("You cannot get get async context in non-request rendering operations")
    }

    def getParts() throws IOException,
            IllegalStateException, ServletException {
        throw new UnsupportedOperationException("You cannot get get parts in non-request rendering operations")
    }

    def getPart(String name) throws IOException,
            IllegalStateException, ServletException {
        throw new UnsupportedOperationException("You cannot get get part in non-request rendering operations")
    }
    // SERVLET 3.0
}
