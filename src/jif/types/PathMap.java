package jif.types;

import java.util.*;

import jif.types.label.Label;
import jif.types.label.NotTaken;
import polyglot.types.SemanticException;
import polyglot.types.Type;
import polyglot.util.CodeWriter;

/**
 * Implements the mapping from control flow paths to labels.
 * 
 * <p>All updates are functional, so
 * that old path maps are still accessible.  This means that all updates, etc.
 * create clones of the pathmap.  There may be a more efficient way to do this
 * sharing, with "lazy" copying of the necessary entries, but this is easy to
 * implement.</p>
 * 
 * @see Path
 */
public class PathMap
{
    protected Map<Path, Label> map;
    protected JifTypeSystem ts;

    public PathMap(JifTypeSystem ts) {
        this.ts = ts;
        this.map = new HashMap<Path, Label>(5);
    }

    /** Look up the label corresponding to p. */
    public Label get(Path p) {
        Label l = map.get(p);
        if (l == null) return ts.notTaken();
        return l;
    }

    /** Return a copy of this with "p" set to "L". */
    public PathMap set(Path p, Label L) {
        PathMap n = new PathMap(ts);
        n.map.putAll(map);

        if (L instanceof NotTaken) {
            n.map.remove(p);
        }
        else {
            n.map.put(p, L);
        }

        return n;
    }

    /** Convenience method to return the normal termination path label ({@link Path#N}). */
    public Label N() { return get(Path.N); }
    /** Convenience method to set the normal termination path label    ({@link Path#N})  */ 
    public PathMap N(Label label) { return set(Path.N, label); }

    /** Convenience method to return the normal value label            ({@link Path#NV}) */
    public Label NV() { return get(Path.NV); }
    /** Convenience method to set the normal value label               ({@link Path#NV}) */
    public PathMap NV(Label label) { return set(Path.NV, label); }

    /** Convenience method to return the the normal return path label  ({@link Path#R})  */
    public Label R() { return get(Path.R); }
    /** Convenience method to set the normal return path label         ({@link Path#R})  */
    public PathMap R(Label label) { return set(Path.R, label); }

    /** Convenience method to set the exception path label for the given exception type */ 
    public PathMap exception(Type type, Label label) {
        return set(ts.exceptionPath(type), label);
    }

    /**
     * If "type" is an unchecked exception, return this.  Otherwise, return an
     * updated copy with "label" joined to N, NV, and the exception path for
     * "type".S
     */ 
    public PathMap exc(Label label, Type type) {
        if (ts.isUncheckedException(type)) {
            return this;
        }

        Path C = ts.exceptionPath(type);
        Label c = ts.join(label, get(C));
        Label n = ts.join(label, N());
        Label nv = ts.join(label, NV());
        return this.N(n).NV(nv).set(C, c);
    }

    /** Return all paths in the map except NV */
    public Set<Path> paths() {
        Set<Path> s = allPaths();
        s.remove(Path.NV);
        return s;
    }

    /** Return all paths in the map including NV. */
    public Set<Path> allPaths() {
        return new LinkedHashSet<Path>(map.keySet());
    }

    /** Return a new PathMap mapping a path p to this.get(p) join m.get(p). */ 
    public PathMap join(PathMap m) {
        PathMap n = new PathMap(ts);
        n.map.putAll(map);

        // Iterate over the elements of X, joining those labels with the ones
        // in this and adding the ones that aren't there.
        for (Map.Entry<Path, Label> e :m.map.entrySet()) {
            Path p = e.getKey();
            Label l1 = e.getValue();
            Label l2 = n.get(p);
            n.map.put(p, ts.join(l1, l2));
        }

        return n;
    }

    public PathMap subst(LabelSubstitution subst) throws SemanticException {
        PathMap n = new PathMap(ts);

        for (Map.Entry<Path, Label> e : map.entrySet()) {
            Path p = e.getKey();
            Label L = e.getValue();
            n.map.put(p, L.subst(subst));
        }

        return n;
    }
    
    public PathMap subst(VarMap bounds) {
        PathMap n = new PathMap(ts);

        for (Map.Entry<Path, Label> e : map.entrySet()) {
            Path p = e.getKey();
            Label L = e.getValue();
            n.map.put(p, bounds.applyTo(L));
        }

        return n;
    }

    @Override
    public String toString() {
        String s = "";

        for (Iterator<Map.Entry<Path, Label>> i = map.entrySet().iterator(); i.hasNext(); ) {
            Map.Entry<Path, Label> e = i.next();
            Path p = e.getKey();
            Label L = e.getValue();

            s += p.toString() + ":" + L.toString();

            if (i.hasNext()) {
                s += " ";
            }
        }

        return s;
    }

    public void dump(CodeWriter w) {
        w.write("X[");
        w.begin(0);
        boolean first = true;

        for (Map.Entry<Path, Label> e : map.entrySet()) {
            Path p = e.getKey();
            Label L = e.getValue();

            if (! first) {
                w.allowBreak(0);
                first = false;
            }

            w.write(p.toString()+ ":" + L.toString());
        }

        w.end();
        w.write("]");
    }
}
