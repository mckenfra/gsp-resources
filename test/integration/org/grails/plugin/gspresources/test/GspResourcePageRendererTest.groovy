package org.grails.plugin.gspresources.test

import org.codehaus.groovy.grails.plugins.web.taglib.ApplicationTagLib
import org.grails.plugin.gspresources.GspResourcePageRenderer

class GspResourcePageRendererTest extends GroovyTestCase {
    // Injected
    GspResourcePageRenderer gspResourcePageRenderer

    void test_compile_empty_template() {
        assertCompilationOutput('/css/empty-template.css.gsp', "")
    }

    void test_compile_invalid_template() {
        assertCompilationOutput('/js/invalid-template.js.gsp',"")
    }

    void test_compile_simple_template() {
        assertCompilationOutput('/css/simple-template.css.gsp',
                "h1{color: red}h2{color: pink}h3{color: blue}h4{color: green}")
    }

    void test_compile_template_with_tags() {
        assertCompilationOutput('/css/template-with-tags.css.gsp',
                "h1{border: solid 3px black}h2{border: solid 3px gray}h3{border: solid 3px black}h4{border: solid 3px gray}")
    }

    void test_compile_static_reference() {
        assertCompilationOutput('/js/static-reference.js.gsp', "var x=1;")
    }

    void test_compile_gsp_inner_function() {
        assertCompilationOutput('/js/fibonacci.js.gsp', "var fibonacci = [0,1,1,2,3,5,8,13,21,34,55];")
    }

    void test_compile_gsp_createLink() {
        assertCompilationOutput('web-app/js/createLink.js.gsp', "var link='/test/index';")
    }

    void test_compile_gsp_createLink_does_not_change_taglib_metaclass() {
        MetaClass metaClass = ApplicationTagLib.metaClass
        gspResourcePageRenderer.render(['resource':'/js/createLink.js.gsp'])
        assertEquals("ApplicationTagLib metaclass has been changed", null, metaClass.hasProperty('request'))
        assertEquals("ApplicationTagLib metaclass has been changed", null, metaClass.hasProperty('grailsApplication'))
    }

    private void assertCompilationOutput(String uri, String expectedOutput) {
        String output = gspResourcePageRenderer.render(['resource':uri])
        assertEquals("Expected:\n$expectedOutput\n\nActual:\n$output", expectedOutput.trim(), output.trim())
    }
}
