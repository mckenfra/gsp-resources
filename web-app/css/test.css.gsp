<%
    def ruleMap = [h1: 'red', h2: 'pink', h3: 'blue', h4: 'green']
%>

<g:each in="${ruleMap.entrySet()}" var="entry">
    ${entry.key}{
        color: ${entry.value}
    }
</g:each>