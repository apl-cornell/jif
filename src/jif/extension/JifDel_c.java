package jif.extension;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import jif.ast.JifUtil;
import jif.types.JifTypeSystem;
import polyglot.ast.JLDel_c;
import polyglot.ast.Node;
import polyglot.types.SemanticException;
import polyglot.types.Type;
import polyglot.types.TypeSystem;
import polyglot.util.CodeWriter;
import polyglot.util.InternalCompilerError;
import polyglot.util.SerialVersionUID;
import polyglot.util.SubtypeSet;
import polyglot.visit.Translator;
import polyglot.visit.TypeBuilder;

/** An implementation of the <code>Jif</code> interface.
 */
public class JifDel_c extends JLDel_c implements JifDel {
    private static final long serialVersionUID = SerialVersionUID.generate();

    protected Set<Type> fatalExceptions = Collections.emptySet();

    /**
     * Set the exceptions thrown by this node that are treated as fatal.
     */
    @Override
    public void setFatalExceptions(TypeSystem ts, SubtypeSet fatalExceptions) {
        this.fatalExceptions = fatalExceptions;
    }

    /**
     * Get the exceptions thrown by this node that are treated as fatal.
     */
    @Override
    public Set<Type> fatalExceptions() {
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
        throw new InternalCompilerError(
                "cannot translate " + node() + "; still has a Jif extension");
    }

    @Override
    // This will be redundant for some classes,
    // but some subtypes invoke this method and
    // add exceptions afterward, while others
    // do not.
    public List<Type> throwTypes(TypeSystem ts) {
        List<Type> l = super.throwTypes(ts);
        if (l.isEmpty()) return l;
        l = new ArrayList<Type>(l);

        Set<Type> rem = new HashSet<Type>();
        if (fatalExceptions.isEmpty()) return l;
        for (Type t : l) {
            if (fatalExceptions.contains(t)) rem.add(t);
        }
        l.removeAll(rem);
        return l;
    }
}
