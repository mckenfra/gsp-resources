package org.grails.plugin.gspresources.servlet

import java.util.Collection;

import javax.servlet.ServletOutputStream
import javax.servlet.http.Cookie
import javax.servlet.http.HttpServletResponse

/**
 * A response object used during the GSP rendering pipeline for
 * render operations outside a web request
 */
public class BackgroundResponse implements HttpServletResponse {
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
    
    public BackgroundResponse(PrintWriter writer, List<Cookie> cookies) {
        this._writer = writer
        this._cookies = cookies
    }

    // Servlet 2.x
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
    // Servlet 2.x
    
    // Servlet 3.0
//    public Collection<String> getHeaders(String name) { this._headers.get(name) ?: [] }
//    
//    public Collection<String> getHeaderNames() { this._headers.keySet() }
//            
//    public String getHeader(String name) { this._headers.get(name)?.get(0) }
    // Servlet 3.0
}
