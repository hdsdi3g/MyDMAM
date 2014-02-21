#{if session.username && controllers.Secure.checkview(_arg)}
    #{doBody /}
#{/if}