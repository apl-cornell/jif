package jif.extension;

import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import jif.ast.JifUtil;
import jif.types.JifTypeSystem;
import polyglot.ast.JL_c;
import polyglot.ast.Node;
import polyglot.types.SemanticException;
import polyglot.types.TypeSystem;
import polyglot.util.CodeWriter;
import polyglot.util.InternalCompilerError;
import polyglot.util.SubtypeSet;
import polyglot.visit.Translator;
import polyglot.visit.TypeBuilder;

/** An implementation of the <code>Jif</code> interface. 
 */
public class JifJL_c extends JL_c implements JifJL
{
    @SuppressWarnings("unchecked")
    protected Set fatalExceptions = Collections.EMPTY_SET;
    
    public void fatalExceptions(TypeSystem ts, SubtypeSet fatalExceptions) {
        this.fatalExceptions = fatalExceptions;
    }
    
    @SuppressWarnings("unchecked")
    public Set fatalExceptions() {
        return fatalExceptions;
    }

    @Override
    public Node buildTypes(TypeBuilder tb) throws SemanticException {
        JifTypeSystem ts = (JifTypeSystem) tb.typeSystem();
        Node n = super.buildTypes(tb);
        return JifUtil.updatePathMap(n, ts.pathMap());
    }

    @Override
    public void translate(CodeWriter w, Translator tr) {
        throw new InternalCompilerError("cannot translate " + node() +
        "; still has a Jif extension");
    }

    @SuppressWarnings("unchecked")
    @Override
    // This will be redundant for some classes, 
    // but some subtypes invoke this method and
    // add exceptions afterward, while others 
    // do not. 
    public List throwTypes(TypeSystem ts) {
        List l = super.throwTypes(ts);
        if(fatalExceptions.isEmpty())
            return l;
        for (Iterator it = l.iterator(); it.hasNext();)
            if (fatalExceptions.contains(it.next()))
                it.remove();
        return l;
    }
}
