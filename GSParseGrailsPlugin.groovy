class GSParseGrailsPlugin {
    // the plugin version
    def version = "0.1"
    // the version or versions of Grails the plugin is designed for
    def grailsVersion = "1.3.7 > *"
    // the other plugins this plugin depends on
    def dependsOn = [:]
    // resources that are excluded from plugin packaging
    def pluginExcludes = [
            'grails-app/views/*.gsp',
            'web-app/**/*',
            '**/MyAppResources.groovy',
    ]

    def author = "Peter McNeil"
    def authorEmail = "peter@mcneils.net"
    def title = "Parse scripts, css, special files as GSP"
    def description = '''\\
This plugin adds parsing of resource (e.g. javascript or css) files in from the base path
directory as GSP files so you can use GSP tags and pass in models/data. Primary
motivation for this was providing a way to pass in relative paths to resources for
JS to access controllers for AJAX calls. For example you can do this in your js:-

$(function() {
    $("#task").autocomplete('${g.createLink(controller: "task", action: "suggestTask")}', {
        max: ${max},
        width: 300
    });
});

The g.createLink tag is now relative to the url, so if the URL to your controller
contains a /id, this js file will still work.

It also allows CSS to use and set variables, or use paths to resources easily.
So for example you can do this:

body {
    background: url('${g.resource(dir: "image", file: "thing.png")}');
    color: #${color};
}

To use the GSP parsing for javascript you use the controller url with the file path
add a URL Mapping like this to make the URLs pretty

"/resource/$path**"(controller: 'script', action: 'parse')

then just include something like this in your layout templates:

<script type="text/javascript" src="resource/js/application.js"></script>

The mime type is guessed for files with .js or .css endings otherwise set to text/plain.
You can override the mimetype using the type parameter. The primary type is
fixed as text.

e.g. resource/somewhere/application.blat?type=text%2Fjavascript

returns mime type "text/javascript"

"script/somewhere/myfile.blat?type=blat"

returns mime type "blat"

To set a parameter just add it to the URL. For example to set the color for the CSS
example above add ?color=FFF to the url.
'''

    // URL to the plugin's documentation
    def documentation = "http://nerderg.com/GSParse"

    def doWithWebDescriptor = { xml ->
        // TODO Implement additions to web.xml (optional), this event occurs before 
    }

    def doWithSpring = {
        // TODO Implement runtime spring config (optional)
    }

    def doWithDynamicMethods = { ctx ->
        // TODO Implement registering dynamic methods to classes (optional)
    }

    def doWithApplicationContext = { applicationContext ->
        // TODO Implement post initialization spring config (optional)
    }

    def onChange = { event ->
        // TODO Implement code that is executed when any artefact that this plugin is
        // watching is modified and reloaded. The event contains: event.source,
        // event.application, event.manager, event.ctx, and event.plugin.
    }

    def onConfigChange = { event ->
        // TODO Implement code that is executed when the project configuration changes.
        // The event is the same as for 'onChange'.
    }
}
