import org.codehaus.groovy.grails.commons.GrailsResourceUtils
import org.grails.plugin.resource.mapper.MapperPhase

class SassResourceMapper {
    def phase = MapperPhase.GENERATION
    def priority = -1

    static defaultExcludes = ['**/*.js', '**/*.png', '**/*.gif', '**/*.jpg', '**/*.jpeg', '**/*.gz', '**/*.zip']
    static defaultIncludes = ['**/*.gsp']

    private static String GSP_FILE_EXTENSIONS = ['.gsp']

    def map(resource, config) {
        File originalFile = resource.processedFile

        if (resource.sourceUrl && isFileGspFile(originalFile)) {
            File input = getOriginalFileSystemFile(resource.sourceUrl);
            File output = new File(generateCompiledFilenameFromOriginal(originalFile.absolutePath))
            output << "Hello, World"
            resource.processedFile = output
//            resource.contentType = 'text/css'
            //            resource.sourceUrlExtension = 'css'
            //            resource.tagAttributes.rel = 'stylesheet'


            resource.actualUrl = generateCompiledFilenameFromOriginal(resource.originalUrl)
        }
    }

    private File getConfigFile() {
        def configFile = new File("grails-app/conf/GrassConfig.groovy")
        def defaultConfigFile = new File("grails-app/conf/DefaultGrassConfig.groovy")
        return configFile.exists() ? configFile : defaultConfigFile
    }

    private boolean isFileGspFile(File file) {
        return GSP_FILE_EXTENSIONS.any { file.name.toLowerCase().endsWith(it) }
    }

    private String generateCompiledFilenameFromOriginal(String original) {
        return original.replaceAll(/(?i)\.gsp/, '')
    }

    private File getOriginalFileSystemFile(String sourcePath) {
        new File(GrailsResourceUtils.WEB_APP_DIR + sourcePath);
    }
}
