#GSP Resource plugin#
This plugin allows you to reference GSP-generated files as cacheable static resources. Consider the following:
*fibonacci.js.gsp*
<pre><code>
<%fib = {n -> return n <= 1 ? n : fib(n-1) + fib(n-2)}%>
var fibonacci = [<%=(0..10).collect{ fib.call(it) }.join(',')%>];
</code></pre>
which is then cacheable and serveable as a javascript file as
<pre><code>
var fibonacci = [0,1,1,2,3,5,8,13,21,34,55];
</code></pre>

##Installation##
<pre><code>grails install-plugin gsp-resources</code></pre>

##Usage##
<pre><code>'style' {
    resource url:'css/test.css.gsp',attrs:[rel: 'stylesheet/css', type:'css']
}
</code></pre>

##Issues##
