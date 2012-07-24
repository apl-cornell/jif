package jif.types;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import polyglot.ext.param.types.MuPClass_c;
import polyglot.util.Position;

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

    @Override
    public List formals() {
        JifPolyType pt = (JifPolyType) clazz;

        List l = new ArrayList(pt.params().size());

        for (Iterator i = pt.params().iterator(); i.hasNext(); ) {
            ParamInstance pi = (ParamInstance) i.next();
            l.add(pi);
        }

        return l;
    }

    @Override
    public String toString() {
	String s = "";

        for (Iterator i = formals().iterator(); i.hasNext(); ) {
	    ParamInstance pi =  (ParamInstance)i.next();
            s += pi.name();

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
