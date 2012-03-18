/**
 * Triggers start of resources processing, if
 * <code>grails.resources.processing.startup</code> setting in
 * <code>Config.groovy</code> is set to <code>delayed</code>
 * 
 * @author francismckenzie
 */
class GspResourcesBootStrap {
    def grailsResourceProcessor
    
    def init = { servletContext ->
        if ( grailsResourceProcessor.isStartOnPluginBootstrap() ) {
            grailsResourceProcessor.start()
        }
    }

    def destroy = {
    }
}
