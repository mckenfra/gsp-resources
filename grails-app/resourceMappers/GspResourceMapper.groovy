import groovy.text.SimpleTemplateEngine
import org.codehaus.groovy.grails.commons.GrailsResourceUtils
import org.grails.plugin.resource.mapper.MapperPhase

class GspResourceMapper {
    def phase = MapperPhase.GENERATION
    def priority = -1

    def grailsApplication

    static defaultExcludes = ['**/*.js', '**/*.png', '**/*.gif', '**/*.jpg', '**/*.jpeg', '**/*.gz', '**/*.zip']
    static defaultIncludes = ['**/*.gsp']

    private static String GSP_FILE_EXTENSIONS = ['.gsp']

    def map(resource, config) {
        File originalFile = getOriginalFileSystemFile(resource.sourceUrl)

        if (resource.sourceUrl && isFileGspFile(originalFile)) {
            File input = getOriginalFileSystemFile(resource.sourceUrl);
            File output = new File(generateCompiledFilenameFromOriginal(originalFile.absolutePath))
            String compiledText = compileGsp(input)
            output.write(compiledText, "UTF-8")

            resource.processedFile = output
            resource.actualUrl = generateCompiledFilenameFromOriginal(resource.originalUrl)
        }
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

    String compileGsp(File input) {
        grailsApplication.mainContext.classLoader.getResource(input.absolutePath)
        return new SimpleTemplateEngine().createTemplate(input).make().toString()
    }
}
