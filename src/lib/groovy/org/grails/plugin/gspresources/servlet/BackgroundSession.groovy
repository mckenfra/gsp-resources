package org.grails.plugin.gspresources.servlet

import java.io.PrintWriter;
import java.util.Enumeration;
import java.util.List;

import javax.servlet.ServletContext
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpSession
import javax.servlet.http.HttpSessionContext;

import org.apache.commons.collections.iterators.IteratorEnumeration

/**
 * A session object used during the GSP rendering pipeline for
 * render operations outside a web request.
 * <p>
 * Configured by passing in a map of request properties during instantiation.
 * <p>
 * Can be used in either a servlet 2.5 or servlet 3.0 container.
 * 
 * @author Francis McKenzie
 */
public class BackgroundSession implements HttpSession {
    
    /**
     * Instantiate class - dynamically adds any required servlet 3.0
     * methods while still allowing class to be loaded in a servlet 2.5
     * container.
     * 
     * @param servletContext The servlet context of the session
     * @param responseArgs Optional properties to set on session using methods of same name
     * @return A 'faked' session
     */
    public static HttpSession createInstance(ServletContext servletContext, Map sessionArgs = null) {
        return new BackgroundSession(servletContext, sessionArgs)
    }

    Map<String,Object> attributeMap
    long creationTime
    String id
    ServletContext servletContext
    
    protected BackgroundSession(ServletContext servletContext, Map sessionArgs) {
        attributeMap = BackgroundUtils.createAttributeMap(sessionArgs?.attributes)
        creationTime = System.currentTimeMillis()
        id = sessionArgs?.id ?: 'NOT-A-REAL-SESSION'
        servletContext = servletContext
    }
    
    long getLastAccessedTime() { 0 }
    
    int getMaxInactiveInterval() { 0 }
    
    void invalidate() { attributeMap.clear() }
    
    boolean isNew() { false }
    
    void setMaxInactiveInterval(int arg0) { /** No-op **/ }
    
    Enumeration<String> getAttributeNames() { new IteratorEnumeration((attributeMap?.keySet()?:[]).iterator()) }
    
    Object getAttribute(String name) { attributeMap?.get(name) }
    
    void setAttribute(String name, Object value) { attributeMap?.put(name, value) }
    
    void removeAttribute(String name) { attributeMap.remove(name) }
    
    // DEPRECATED METHODS
    
    @SuppressWarnings("deprecation")
    HttpSessionContext getSessionContext() {
        throw new UnsupportedOperationException("You cannot get session context in non-request rendering operations")
    }
    
    @SuppressWarnings("deprecation")
    String[] getValueNames() { (attributeMap?.keySet()?:[]) as String[] }
    
    @SuppressWarnings("deprecation")
    Object getValue(String name) { attributeMap?.get(name) }
    
    @SuppressWarnings("deprecation")
    void putValue(String name, Object value) { attributeMap?.put(name, value) }

    @SuppressWarnings("deprecation")
    void removeValue(String name) { attributeMap.remove(name) }
}
