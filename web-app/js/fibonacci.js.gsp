<%fib = {n -> return n <= 1 ? n : fib(n-1) + fib(n-2)}%>
var fibonacci = [<%=(0..10).collect{ fib.call(it) }.join(',')%>];