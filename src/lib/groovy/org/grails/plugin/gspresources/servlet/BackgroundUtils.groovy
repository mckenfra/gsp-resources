package org.grails.plugin.gspresources.servlet

import javax.servlet.http.Cookie

/**
 * Utility methods for converting values
 * 
 * @author Francis McKenzie
 */
public class BackgroundUtils {
    
    static Cookie[] createCookies(Object withCookies) {
        def result
        if (withCookies in Cookie[]) {
            result = withCookies
        } else if (withCookies in Collection || withCookies in Object[]) {
            result = []
            withCookies.each { if (it in Cookie) result << it }
        } else if (withCookies in Map) {
            result = []
            withCookies.each { result << new Cookie(it.key, it.value) }
        } else {
            result = []
        }
        return result as Cookie[]
    }
    
    static Map<String,String[]> createAttributeMap(Object withMap) {
        Map<String,String[]> result = [:] as Map<String,String[]>
        if (withMap in Map) {
            withMap.each { result.put(it.key.toString(), it.value) }
        }
        return result
    }
    
    static Map<String,String[]> createParameterMap(Object withMap) {
        Map<String,String[]> result = [:] as Map<String,String[]>
        if (withMap in Map) {
            withMap.each {
                def value = it.value
                def values
                if (value in Collection || value in Object[])
                    values = value
                else
                    values = value ? [value] : []
                result.put(it.key.toString(), values as String[])
            }
        }
        return result
    }
}