package org.grails.plugin.gspresources.servlet

import javax.servlet.http.Cookie

/**
 * Utility methods for converting values
 *
 * @author Francis McKenzie
 */
class BackgroundUtils {

    static Cookie[] createCookies(withCookies) {
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

    static Map<String,String[]> createAttributeMap(withMap) {
        Map<String,String[]> result = [:] as Map<String,String[]>
        if (withMap in Map) {
            withMap.each { result.put(it.key.toString(), it.value) }
        }
        return result
    }

    static Map<String,String[]> createParameterMap(withMap) {
        Map<String,String[]> result = [:]
        if (withMap in Map) {
            withMap.each { key, value ->
                def values
                if (value in Collection || value in Object[])
                    values = value
                else
                    values = value ? [value] : []
                result.put(key.toString(), values as String[])
            }
        }
        return result
    }
}
