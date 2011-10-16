modules = {
    testgsp {
        resource url: '/css/simple-template.css.gsp', attrs: [type: 'css'], disposition: 'head'
        resource url: '/css/template-with-tags.css.gsp', attrs: [type: 'css'], disposition: 'head'
        resource url: '/js/fibonacci.js.gsp', attrs: [type: 'js'], disposition: 'head'
    }
}