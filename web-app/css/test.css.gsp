<%
    def ruleMap = [h1: 'red', h2: 'pink', h3: 'blue', h4: 'green']

    ruleMap.each {entry ->
%>
${entry.key}{
        color: ${entry.value}
}
<%
    }
%>

