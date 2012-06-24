// configuration for plugin testing - will not be included in the plugin zip

grails.app.context='/'

log4j = {
    // Example of changing the log pattern for the default console
    // appender:
    //
    //appenders {
    //    console name:'stdout', layout:pattern(conversionPattern: '%c{2} %m%n')
    //}

    error 'org.codehaus.groovy.grails.web.servlet',  //  controllers
            'org.codehaus.groovy.grails.web.pages', //  GSP
            'org.codehaus.groovy.grails.web.sitemesh', //  layouts
            'org.codehaus.groovy.grails.web.mapping.filter', // URL mapping
            'org.codehaus.groovy.grails.web.mapping', // URL mapping
            'org.codehaus.groovy.grails.commons', // core / classloading
            'org.codehaus.groovy.grails.plugins', // plugins
            'org.codehaus.groovy.grails.orm.hibernate', // hibernate integration
            'org.springframework',
            'org.hibernate',
            'net.sf.ehcache.hibernate'

    warn 'org.mortbay.log'
}

grails.views.default.codec="none" // none, html, base64
grails.views.gsp.encoding="UTF-8"

// For API docs
grails.doc.api.groovy = "http://groovy.codehaus.org/gapi"
grails.doc.api.java.io = "http://docs.oracle.com/javase/6/docs/api"
grails.doc.api.java.lang = "http://docs.oracle.com/javase/6/docs/api"
grails.doc.api.java.util = "http://docs.oracle.com/javase/6/docs/api"
grails.doc.api.javax.servlet = "http://docs.oracle.com/javaee/6/api"
grails.doc.api.org.codehaus.groovy.grails = "http://grails.org/doc/latest/api"
grails.doc.api.org.grails.plugin.resource = "http://grails-plugins.github.com/grails-resources/gapi"
grails.doc.api.org.springframework = "http://static.springsource.org/spring/docs/3.1.x/javadoc-api"
