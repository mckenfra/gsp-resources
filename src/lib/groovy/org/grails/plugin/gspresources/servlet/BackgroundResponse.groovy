package org.grails.plugin.gspresources.servlet

import javax.servlet.ServletOutputStream
import javax.servlet.http.Cookie
import javax.servlet.http.HttpServletResponse

/**
 * A response object used during the GSP rendering pipeline for
 * render operations outside a web request.
 * <p>
 * Configured by passing in a map of response properties during instantiation.
 * <p>
 * Can be used in either a servlet 2.5 or servlet 3.0 container.
 *
 * @author Francis McKenzie
 */
class BackgroundResponse implements HttpServletResponse {

    /**
     * Instantiate class - dynamically adds any required servlet 3.0
     * methods while still allowing class to be loaded in a servlet 2.5
     * container.
     *
     * @param writer The writer for the output
     * @param responseArgs Optional properties to set on response using methods of same name, including:
     * @param cookies A map of cookies to add to the response
     * @param headers A map of headers to add to the response
     * @return A 'faked' response
     */
    static HttpServletResponse createInstance(Writer writer, Map responseArgs = null) {
        return new BackgroundResponse(writer, responseArgs)
    }

    int bufferSize
    String characterEncoding
    boolean committed
    int contentLength
    String contentType
    List<Cookie> cookies
    Map<String,List<String>> headerMap
    Locale locale
    int status
    PrintWriter writer

    protected BackgroundResponse(Writer writer, Map responseArgs) {
        bufferSize = 0
        characterEncoding = responseArgs?.characterEncoding ?: 'UTF-8'
        committed = false
        contentLength = responseArgs?.contentLength ?: 0
        cookies = BackgroundUtils.createCookies(responseArgs?.cookies) as List<Cookie>
        headerMap = BackgroundUtils.createParameterMap(responseArgs?.headers)
        def loc = responseArgs?.locale
        locale = loc in Locale ? loc : (loc ? new Locale(loc) : Locale.getDefault())
        status = responseArgs?.status ?: 200
        this.writer = writer instanceof PrintWriter ? writer : new PrintWriter(writer)
    }

    void addCookie(Cookie cookie) { cookies?.add(cookie) }

    void setContentLength(int contentLength) {
        this.contentLength = contentLength
        setHeader('Content-Length', contentLength)
    }

    void setContentType(String contentType) {
        this.contentType = contentType
        setHeader('Content-Type', contentType)
    }

    boolean containsHeader(String name) { return headerMap?.containsKey(name) }

    String encodeURL(String url) { url }

    String encodeRedirectURL(String url) { url }

    String encodeUrl(String url) { url }

    String encodeRedirectUrl(String url) { url }

    void sendError(int sc, String msg) {
        status = sc
        writer?.print msg
    }

    void sendError(int sc) { status = sc }

    void sendRedirect(String location) { status = 302 }

    void setHeader(String name, String value) {
        Map<String,List<String>> headers = headerMap
        if (!headers || !name) return
        if (value == null) headers.remove(name)
        else headers.put(name, [value] as List<String>)
    }

    void addHeader(String name, String value) {
        Map<String,List<String>> headers = headerMap
        if (!headers) return
        List<String> values = headers.get(name)
        if (!values) {
            values = [] as List<String>
            headers.put(name, values)
        }
        values.add(value)
    }

    void setDateHeader(String name, long date) { setHeader(name, "${date}") }

    void addDateHeader(String name, long date) { addHeader(name, "${date}") }

    void setIntHeader(String name, int value) { setHeader(name, "${value}") }

    void addIntHeader(String name, int value) { addHeader(name, "${value}") }

    void setStatus(int sc, String sm) {
        status = sc
        writer?.print(sm)
    }

    ServletOutputStream getOutputStream() {
        throw new UnsupportedOperationException("You cannot use the OutputStream in non-request rendering operations. Use getWriter() instead")
    }

    void flushBuffer() { committed = true }

    void resetBuffer() { /** No-op **/ }

    void reset() {
        status = 0
        headerMap?.clear()
    }

    Collection<String> getHeaders(String name) { headerMap?.get(name) ?: Collections.emptyList() }

    Collection<String> getHeaderNames() { headerMap?.keySet() ?: Collections.emptySet() }

    String getHeader(String name) {
        List<String> header = headerMap?.get(name)
        header ? header[0] : null
    }
}
