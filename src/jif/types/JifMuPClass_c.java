package jif.types;

import polyglot.ext.jl.types.*;
import polyglot.ext.param.types.*;
import polyglot.types.*;
import polyglot.util.*;
import java.util.*;

/** An implementation of the <code>JifParsedPolyType</code> interface. 
 */
public class JifMuPClass_c extends MuPClass_c
{
    protected JifMuPClass_c() {
	super();
    }

    public JifMuPClass_c(JifTypeSystem ts, Position pos) {
	super(ts, pos);
    }

    public List formals() {
        JifPolyType pt = (JifPolyType) clazz;

        List l = new ArrayList(pt.params().size());

        for (Iterator i = pt.params().iterator(); i.hasNext(); ) {
            ParamInstance pi = (ParamInstance) i.next();
            l.add(pi.uid());
        }

        return l;
    }

    public String toString() {
	String s = "";

        for (Iterator i = formals().iterator(); i.hasNext(); ) {
            //ParamInstance pi = (ParamInstance) o; ###
	    UID puid = (UID) i.next();
            s += puid.toString();

            if (i.hasNext()) {
                s += ", ";
            }
        }

	if (! s.equals("")) {
	    s = "[" + s + "]";
	}

        return clazz.toString() + s;
    }
}
