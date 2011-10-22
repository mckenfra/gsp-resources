import grails.util.GrailsWebUtil
import org.codehaus.groovy.grails.commons.GrailsApplication
import org.codehaus.groovy.grails.web.pages.GroovyPagesTemplateEngine
import org.grails.plugin.resource.mapper.MapperPhase
import org.codehaus.groovy.grails.plugins.web.taglib.ApplicationTagLib
import org.springframework.mock.web.MockHttpServletRequest
import javax.servlet.http.HttpServletRequest

class GspResourceMapper {
    def phase = MapperPhase.GENERATION

    def priority = -1

    static defaultExcludes = ['**/*.js', '**/*.png', '**/*.gif', '**/*.jpg', '**/*.jpeg', '**/*.gz', '**/*.zip']
    static defaultIncludes = ['**/*.gsp']

    GrailsApplication grailsApplication
    GroovyPagesTemplateEngine groovyPagesTemplateEngine

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
        grailsApplication.parentContext.getResource(sourcePath).file
    }

    String compileGsp(File input) {
        StringWriter gspWriter = new StringWriter()
        GrailsWebUtil.bindMockWebRequest(grailsApplication.mainContext)
        prepareAndWriteGsp(input, gspWriter)

        return gspWriter.toString()
    }

    protected def prepareAndWriteGsp(File input, StringWriter gspWriter) {
        prepareApplicationTagLibMetaclass(gspWriter)
        try {
            groovyPagesTemplateEngine.createTemplate(input).make().writeTo(gspWriter)
        }
        catch (Exception e) {
            throw e
        }
        finally {
            restoreApplicationTagLibMetaclass()
        }
    }

    def applicationTagLibGetOut
    def applicationTagLibGrailsApplication
    def applicationTagLibRequest

    protected void prepareApplicationTagLibMetaclass(StringWriter gspWriter) {
        applicationTagLibGetOut = ApplicationTagLib.metaClass.getOut
        applicationTagLibGrailsApplication = ApplicationTagLib.metaClass.grailsApplication
        applicationTagLibRequest = ApplicationTagLib.metaClass.request
        ApplicationTagLib.metaClass.getOut = {-> gspWriter}
        ApplicationTagLib.metaClass.grailsApplication = grailsApplication
        ApplicationTagLib.metaClass.request = new MockHttpServletRequest()
    }

    protected def restoreApplicationTagLibMetaclass() {
        ApplicationTagLib.metaClass.getOut = applicationTagLibGetOut
        ApplicationTagLib.metaClass.grailsApplication = applicationTagLibGrailsApplication
        ApplicationTagLib.metaClass.request = applicationTagLibRequest
    }
}
