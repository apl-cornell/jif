package jif.lang;

import java.util.*;

public class ReadableByPrinPolicy extends ReaderPolicy
{
    
    public ReadableByPrinPolicy(Principal reader) {
        super(PrincipalUtil.topPrincipal(), reader);
    }

    
    public String componentString() {
        return "[readable by " + (PrincipalUtil.toString(reader())) + "]";
    }

}
