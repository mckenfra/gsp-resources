#GSP Resource plugin#
This plugin allows you to reference GSP-generated files as cacheable static resources. Consider the following:
**fibonacci.js.gsp**
<pre><code>&lt;%fib = {n -&gt; return n &lt;= 1 ? n : fib(n-1) + fib(n-2)}%&gt;
var fibonacci = [&lt;%=(0..10).collect{ fib.call(it) }.join(',')%&gt;];</code></pre>
which is then cacheable and serveable as a javascript file as
<pre><code>var fibonacci = [0,1,1,2,3,5,8,13,21,34,55];</code></pre>

##User Guide##
Full documentation <a href="http://mckenfra.github.com/gsp-resources/guide/single.html">here</a>

##Special Thanks##
Stefan Kendall for creating the original <a href="https://github.com/stefankendall/gsp-resources">gsp-resources plugin</a>
