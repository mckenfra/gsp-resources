import org.codehaus.groovy.grails.web.pages.GroovyPagesTemplateEngine
import org.codehaus.groovy.grails.commons.GrailsApplication
import org.codehaus.groovy.grails.plugins.web.taglib.ApplicationTagLib

class GspResourceMapperTest extends GroovyTestCase {
    GspResourceMapper gspResourceMapper
    GroovyPagesTemplateEngine groovyPagesTemplateEngine
    GrailsApplication grailsApplication

    public void setUp() {
        gspResourceMapper = new GspResourceMapper()
        gspResourceMapper.groovyPagesTemplateEngine = groovyPagesTemplateEngine
        gspResourceMapper.grailsApplication = grailsApplication
    }

    public void test_compile_empty_template() {
        assertCompilationOutput(new File('web-app/css/empty-template.css.gsp'), "")
    }

    public void test_compile_invalid_template() {
        shouldFail {
            gspResourceMapper.compileGsp(new File('web-app/js/invalid-template.js.gsp'))
        }
    }

    public void test_compile_simple_template() {
        assertCompilationOutput(new File('web-app/css/simple-template.css.gsp'),
                "h1{color: red}h2{color: pink}h3{color: blue}h4{color: green}")
    }

    public void test_compile_template_with_tags() {
        assertCompilationOutput(new File('web-app/css/template-with-tags.css.gsp'),
                "h1{border: solid 3px black}h2{border: solid 3px gray}h3{border: solid 3px black}h4{border: solid 3px gray}")
    }

    public void test_compile_static_reference() {
        assertCompilationOutput(new File('web-app/js/static-reference.js.gsp'), "var x=1;")
    }

    public void test_compile_gsp_inner_function() {
        assertCompilationOutput(new File('web-app/js/fibonacci.js.gsp'), "var fibonacci = [0,1,1,2,3,5,8,13,21,34,55];")
    }

    public void test_compile_gsp_createLink() {
        assertCompilationOutput(new File('web-app/js/createLink.js.gsp'), "var link='/test/index';")
    }

    public void test_compile_gsp_createLink_does_not_change_taglib_metaclass() {
        MetaClass metaClass = ApplicationTagLib.metaClass
        gspResourceMapper.compileGsp(new File('web-app/js/createLink.js.gsp'));
        assertEquals("ApplicationTagLib metaclass has been changed", null, metaClass.hasProperty('request'))
        assertEquals("ApplicationTagLib metaclass has been changed", null, metaClass.hasProperty('grailsApplication'))
    }

    private void assertCompilationOutput(File file, String expectedOutput) {
        String output = gspResourceMapper.compileGsp(file)
        assertEquals("Expected:\n$expectedOutput\n\nActual:\n$output", expectedOutput.trim(), output.trim())
    }
}
