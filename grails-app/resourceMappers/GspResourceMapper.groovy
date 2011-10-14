import grails.util.GrailsWebUtil
import groovy.text.SimpleTemplateEngine
import org.codehaus.groovy.grails.commons.ApplicationHolder
import org.codehaus.groovy.grails.commons.GrailsResourceUtils
import org.codehaus.groovy.grails.web.pages.GroovyPagesTemplateEngine
import org.grails.plugin.resource.mapper.MapperPhase
import org.springframework.web.context.WebApplicationContext

class GspResourceMapper {
    def phase = MapperPhase.GENERATION
    def priority = -1

    static defaultExcludes = ['**/*.js', '**/*.png', '**/*.gif', '**/*.jpg', '**/*.jpeg', '**/*.gz', '**/*.zip']
    static defaultIncludes = ['**/*.gsp']

    GroovyPagesTemplateEngine groovyPagesTemplateEngine

    private static String GSP_FILE_EXTENSIONS = ['.gsp']

    def map(resource, config) {
        File originalFile = getOriginalFileSystemFile(resource.sourceUrl)

        if (resource.sourceUrl && isFileGspFile(originalFile)) {
            File input = getOriginalFileSystemFile(resource.sourceUrl);
            File output = new File(generateCompiledFilenameFromOriginal(originalFile.absolutePath))
            String compiledText = compileGspFull(input)
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
        return new SimpleTemplateEngine().createTemplate(input).make().toString()
    }

    String compileGspFull(File input) {
        GrailsWebUtil.bindMockWebRequest((WebApplicationContext) ApplicationHolder.application.mainContext)
        StringWriter sw = new StringWriter()
        groovyPagesTemplateEngine.createTemplate(input).make().writeTo(sw)
        return sw.toString()
    }
}
