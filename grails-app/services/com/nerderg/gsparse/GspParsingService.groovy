/*
    Copyright 2010 Peter McNeil

    This file is part of GSParse.

    Licensed under the Apache License, Version 2.0 (the "License"); you may not
    use this file except in compliance with the License. You may obtain a copy
    of the License at http://www.apache.org/licenses/LICENSE-2.0

    Unless required by applicable law or agreed to in writing, software
    distributed under the License is distributed on an "AS IS" BASIS,
    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
    See the License for the specific language governing permissions and
    limitations under the License.
*/
package com.nerderg.gsparse

import groovy.text.Template

class GspParsingService {
    static transactional = false
    def groovyPagesTemplateEngine

    def parseFile(file, data) {
        def sw = new StringWriter()
        try {
            Template t = groovyPagesTemplateEngine.createTemplate(file)
            Writable w = t.make(data)
            w.writeTo(sw)
            return sw.getBuffer()
        } finally {
            sw.close()
        }
    }
}
