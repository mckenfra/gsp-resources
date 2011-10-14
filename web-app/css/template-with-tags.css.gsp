<%
    def ruleMap = [h1: 'black', h2: 'gray', h3: 'black', h4: 'gray']

%>
<g:each in="${ruleMap.entrySet()}" var="entry">${entry.key}{border: solid 3px ${entry.value}}</g:each>

