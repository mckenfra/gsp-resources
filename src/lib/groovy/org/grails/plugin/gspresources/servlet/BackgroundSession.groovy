package org.grails.plugin.gspresources.servlet

import java.util.Enumeration;

import javax.servlet.ServletContext
import javax.servlet.http.HttpSession
import javax.servlet.http.HttpSessionContext;

import org.apache.commons.collections.iterators.IteratorEnumeration

/**
 * A session object used during the GSP rendering pipeline for
 * render operations outside a web request
 * 
 * @author Francis McKenzie
 */
public class BackgroundSession implements HttpSession {
    // Internal
    protected ServletContext _servletContext
    protected Map _attributes = [:]
    
    public BackgroundSession(ServletContext servletContext) {
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
