// configuration for plugin testing - will not be included in the plugin zip

grails.app.context='/'

log4j = {
	error 'org.codehaus.groovy.grails',
	      'org.springframework',
	      'org.hibernate',
	      'net.sf.ehcache.hibernate'
}

// For API docs
grails.doc.api.groovy = "http://groovy.codehaus.org/gapi"
grails.doc.api.java.io = "http://docs.oracle.com/javase/6/docs/api"
grails.doc.api.java.lang = "http://docs.oracle.com/javase/6/docs/api"
grails.doc.api.java.util = "http://docs.oracle.com/javase/6/docs/api"
grails.doc.api.javax.servlet = "http://docs.oracle.com/javaee/6/api"
grails.doc.api.org.codehaus.groovy.grails = "http://grails.org/doc/latest/api"
grails.doc.api.org.grails.plugin.resource = "http://grails-plugins.github.com/grails-resources/gapi"
grails.doc.api.org.springframework = "http://static.springsource.org/spring/docs/3.1.x/javadoc-api"
