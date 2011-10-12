import org.codehaus.groovy.grails.commons.GrailsResourceUtils
import org.grails.plugin.resource.mapper.MapperPhase
import groovy.text.Template
import org.codehaus.groovy.grails.web.pages.GroovyPagesTemplateEngine

class GspResourceMapper {
    def phase = MapperPhase.GENERATION
    def priority = -1

    GroovyPagesTemplateEngine groovyPagesTemplateEngine

    static defaultExcludes = ['**/*.js', '**/*.png', '**/*.gif', '**/*.jpg', '**/*.jpeg', '**/*.gz', '**/*.zip']
    static defaultIncludes = ['**/*.gsp']

    private static String GSP_FILE_EXTENSIONS = ['.gsp']

    def map(resource, config) {
        File originalFile = getOriginalFileSystemFile(resource.sourceUrl)

        if (resource.sourceUrl && isFileGspFile(originalFile)) {
            File input = getOriginalFileSystemFile(resource.sourceUrl);
            File output = new File(generateCompiledFilenameFromOriginal(originalFile.absolutePath))
            StringBuffer buffer = compileGsp(input)
            output.write(buffer.toString(), "UTF-8")

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

    StringBuffer compileGsp(File input) {
        def sw = new StringWriter()
//        try {
//            Template t = groovyPagesTemplateEngine.createTemplate(input)
//            Writable w = t.make([:])
//            w.writeTo(sw)
            return sw.getBuffer()
//        } finally {
//            sw.close()
//        }
    }
}
