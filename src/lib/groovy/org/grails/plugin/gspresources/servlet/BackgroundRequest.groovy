package org.grails.plugin.gspresources.servlet

import java.io.IOException;
import java.lang.reflect.InvocationHandler
import java.lang.reflect.Method
import java.lang.reflect.Proxy
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

import org.apache.commons.collections.iterators.IteratorEnumeration

/**
 * A request object used during the GSP rendering pipeline for
 * render operations outside a web request.
 * <p>
 * Configured by passing in a map of request properties during instantiation.
 * <p>
 * It conforms to servlet 2.5, so must be compiled using this version. However,
 * it can then be deployed in either a servlet 2.5 or 3.0 container.
 * 
 * @author Francis McKenzie
 */
public class BackgroundRequest implements HttpServletRequest {
    
    /**
     * Instantiate class - dynamically adds any required servlet 3.0
     * methods while still allowing class to be loaded in a servlet 2.5
     * container.
     * <p>
     * 
     * @param requestURI The URI of the request
     * @param servletContext The servlet context of the request
     * @param requestArgs Optional properties to set on request using methods of same name, including:
     * @param attributes A map of attributes to add to the request
     * @param cookies A map of cookies to add to the request
     * @param headers A map of headers to add to the request
     * @param params A map of params to add to the request
     * @return A 'faked' request
     */
    public static HttpServletRequest createInstance(
        Object requestURI,
        Object servletContext,
        Map requestArgs = null)
    {
        BackgroundRequest request = new BackgroundRequest(requestURI, servletContext, requestArgs)
        return BackgroundRequest.createProxyInstance(request)
    }
    
    protected static HttpServletRequest createProxyInstance(final BackgroundRequest request) {
        return (HttpServletRequest) Proxy.newProxyInstance(HttpServletRequest.classLoader, [HttpServletRequest] as Class[], new InvocationHandler() {
            Object invoke(proxy, Method method, Object[] args) {
                String methodName = method.name

                // Servlet 3.0 methods
                if (methodName == 'getDispatcherType') {
                    return Enum.valueOf(Class.forName('javax.servlet.DispatcherType'), 'REQUEST')
                } else if (methodName == 'startAsync') {
                    throw new UnsupportedOperationException("You cannot get start async in non-request rendering operations")
                } else if (methodName == 'getAsyncContext') {
                    throw new UnsupportedOperationException("You cannot get get async context in non-request rendering operations")
                } else if (methodName == 'getParts') {
                    throw new UnsupportedOperationException("You cannot get get parts in non-request rendering operations")
                } else if (methodName == 'getPart') {
                    throw new UnsupportedOperationException("You cannot get get part in non-request rendering operations")
                }
                
                // Other methods
                return request.invokeMethod(methodName, args)
            }
        })
    }
    
    Map<String,Object> attributeMap
    String characterEncoding
    String content
    String contentType
    Cookie[] cookies
    Map<String,String[]> headerMap
    String localAddr
    Locale locale
    Enumeration<Locale> locales
    String localName
    int localPort
    String method
    Map<String,String[]> parameterMap
    String pathInfo
    String pathTranslated
    String protocol
    String queryString
    String remoteAddr
    String remoteHost
    int remotePort
    String remoteUser
    String requestedSessionId
    String requestURI
    String scheme
    String serverName
    int serverPort
    ServletContext servletContext
    String servletPath
    HttpSession session
    Principal userPrincipal
    
    protected BackgroundRequest(Object requestURI, Object servletContext, Map requestArgs) {
        attributeMap = BackgroundUtils.createAttributeMap(requestArgs?.attributes)
        characterEncoding = requestArgs?.characterEncoding ?: 'UTF-8'
        content = requestArgs?.content
        contentType = requestArgs?.contentType
        cookies = BackgroundUtils.createCookies(requestArgs?.cookies)
        headerMap = BackgroundUtils.createParameterMap(requestArgs?.headers)
        localAddr = requestArgs?.localAddr ?: '127.0.0.1'
        def loc = requestArgs?.locale
        locale = loc in Locale ? loc : (loc ? new Locale(loc) : Locale.getDefault())
        localName = requestArgs?.localName ?: 'localhost'
        localPort = requestArgs?.localPort ?: 80
        method = requestArgs?.method ?: 'GET'
        parameterMap = BackgroundUtils.createParameterMap(requestArgs?.params)
        pathInfo = requestArgs?.pathInfo ?: ''
        pathTranslated = requestArgs?.pathTranslated ?: ''
        protocol = requestArgs?.protocol ?: ''
        queryString = requestArgs?.queryString ?: (requestURI in URL ? requestURI.query : '')
        remoteAddr = requestArgs?.remoteAddr ?: localAddr
        remoteHost = requestArgs?.remoteHost ?: localName
        remotePort = requestArgs?.remotePort ?: localPort
        remoteUser = requestArgs?.remoteUser
        requestedSessionId = requestArgs?.requestedSessionId
        def uri = requestURI ?: requestArgs?.requestURI
        this.requestURI = uri in URL ? uri.URI : uri
        scheme = requestArgs?.scheme
        serverName = requestArgs?.serverName ?: localName
        serverPort = requestArgs?.serverPort ?: localPort
        this.servletContext = servletContext
        servletPath = requestArgs?.servletPath ?: '/'
        session = requestArgs?.session in HttpSession ? requestArgs.session : null
        userPrincipal = requestArgs?.userPrincipal in Principal ? requestArgs.userPrincipal : null
    }
    
    boolean isAsyncStarted() { false }
    
    boolean isAsyncSupported() { false }
    
    String getAuthType() { '' }
    
    int getContentLength() { content ? content.length() : 0 }
    
    String getContextPath() { servletContext?.contextPath ?: '' }
     
    ServletInputStream getInputStream() {
        final InputStream input = new ByteArrayInputStream((content?:'').getBytes(contentType))
        return new ServletInputStream() {
            int read() { input.read() }
        }
    }
    
    Enumeration<Locale> getLocales() { new IteratorEnumeration(Locale.getAvailableLocales().iterator()) }
    
    BufferedReader getReader() { new BufferedReader(new StringReader(content?:'')) }
    
    boolean isRequestedSessionIdValid() { true }
    
    boolean isRequestedSessionIdFromCookie() { false }

    boolean isRequestedSessionIdFromURL() { true }

    boolean isRequestedSessionIdFromUrl() { true }
    
    StringBuffer getRequestURL() { new StringBuffer(requestURI?:'') }
    
    boolean isSecure() { false }
    
    long getDateHeader(String name) {
        if (! (headerMap?.get(name)) ) return -1
        try { Long.valueOf(headerMap.get(name)[0]) }
        catch(e) { throw new IllegalArgumentException(name) }
    }

    String getHeader(String name) { headerMap?.get(name) ? headerMap.get(name)[0] : null }

    Enumeration<String> getHeaders(String name) {
        def values = headerMap?.get(name) ?: []
        new IteratorEnumeration(values.iterator())
    }

    Enumeration<String> getHeaderNames() { new IteratorEnumeration((headerMap?.keySet()?:[]).iterator()) }

    int getIntHeader(String name) {
        if (! (headerMap?.get(name)) ) return -1
        try { Integer.valueOf(headerMap.get(name)[0]) }
        catch(e) { throw new NumberFormatException(name) }
    }

    boolean isUserInRole(String role) { false }

    String getRealPath(String path) { path }
    
    public HttpSession getSession(boolean create) {
        session || !create || !servletContext ? session : (session = BackgroundSession.createInstance(servletContext))
    }

    Object getAttribute(String name) { attributeMap ? attributeMap.get(name) : null }

    Enumeration<String> getAttributeNames() { new IteratorEnumeration((attributeMap?.keySet()?:[]).iterator()) }

    void setAttribute(String name, Object o) { attributeMap.put(name, o) }

    void removeAttribute(String name) { attributeMap?.remove(name) }

    String getParameter(String name) { parameterMap?.get(name) ? parameterMap.get(name)[0] : null }

    Enumeration<String> getParameterNames() { new IteratorEnumeration((parameterMap?.keySet()?:[]).iterator()) }

    String[] getParameterValues(String name) { parameterMap ? parameterMap.get(name) : [] as String[] }

    RequestDispatcher getRequestDispatcher(String path) { servletContext?.getRequestDispatcher(path) }

    public boolean authenticate(HttpServletResponse response)
            throws IOException, ServletException { false }

    public void login(String username, String password) throws ServletException { /** No-op **/ }

    public void logout() throws ServletException { /** No-op **/ }
}
