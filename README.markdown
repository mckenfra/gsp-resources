#GSP Resource plugin#
This plugin allows you to reference GSP-generated files as cacheable static resources. Consider the following:
**fibonacci.js.gsp**
<pre><code>&lt;%fib = {n -&gt; return n &lt;= 1 ? n : fib(n-1) + fib(n-2)}%&gt;
var fibonacci = [&lt;%=(0..10).collect{ fib.call(it) }.join(',')%&gt;];</code></pre>
which is then cacheable and serveable as a javascript file as
<pre><code>var fibonacci = [0,1,1,2,3,5,8,13,21,34,55];</code></pre>

##Installation##
<pre><code>grails install-plugin gsp-resources</code></pre>

##Usage##
<pre><code>'style' {
    resource url:'css/test.css.gsp',attrs:[rel: 'stylesheet/css', type:'css']
}
</code></pre>

##Limitations##
Since this plugin is meant to serve *static* resources, there is no inherent data-watching within GSPs. Changes to a GSP file itself will trigger recompilation, but changes to the data referenced within a GSP will not.

##User Guide##
<a href="https://mckenfra.github.com/gsp-resources/docs/guide/single.html">https://mckenfra.github.com/gsp-resources/docs/guide/single.html</a>

##Special Thanks##
Peter McNeil for his work on <a href="http://nerderg.com/GSParse">GSParse</a>, which was the inspiration for this plugin.
