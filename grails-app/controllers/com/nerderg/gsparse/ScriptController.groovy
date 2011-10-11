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

class ScriptController {

    def gspParsingService

    def parse = {
        def gmfe = grailsApplication.config.grails.mime.file.extensions
        def mimeTypes = grailsApplication.config.grails.mime.types
        def path = gmfe ? params.path + ".$request.format" : params.path
        def file = grailsApplication.parentContext.getResource(path).file
        def type = params.type

        if (!type) {
            type = gmfe ? mimeTypes[request.format] : mimeTypes[path.split(/\./)[-1]]
            if (!type) {
                type = "text/html"
            }
        }

        if (file.exists()) {
            def buf = gspParsingService.parseFile(file, params)
            render(text: buf.toString(), contentType: type, encoding: "UTF-8")
        } else {
            response.sendError(404)
        }
    }
}
