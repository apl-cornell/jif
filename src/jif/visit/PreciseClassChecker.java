package jif.visit;

import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import jif.ast.DowngradeExpr;
import jif.extension.JifPreciseClassDel;
import polyglot.ast.Assign;
import polyglot.ast.Binary;
import polyglot.ast.Cast;
import polyglot.ast.Expr;
import polyglot.ast.Field;
import polyglot.ast.Instanceof;
import polyglot.ast.Local;
import polyglot.ast.LocalDecl;
import polyglot.ast.NodeFactory;
import polyglot.ast.Special;
import polyglot.ast.Term;
import polyglot.ast.TypeNode;
import polyglot.ast.Unary;
import polyglot.frontend.Job;
import polyglot.types.FieldInstance;
import polyglot.types.LocalInstance;
import polyglot.types.Type;
import polyglot.types.TypeSystem;
import polyglot.visit.DataFlow;
import polyglot.visit.FlowGraph;
import polyglot.visit.FlowGraph.EdgeKey;
import polyglot.visit.FlowGraph.Peer;
import polyglot.visit.NodeVisitor;

/**
 * Visitor which determines at which program points more precise information
 * is known about the runtime class of local variables and
 * final access paths. This information is then stored in the appropriate
 * delegates.
 */
public class PreciseClassChecker
        extends DataFlow<PreciseClassChecker.DataFlowItem> {
    public PreciseClassChecker(Job job, TypeSystem ts, NodeFactory nf) {
        super(job, ts, nf, true /* forward analysis */);
        EDGE_KEY_CLASS_CAST_EXC = null;
    }

    public PreciseClassChecker(Job job) {
        this(job, job.extensionInfo().typeSystem(),
                job.extensionInfo().nodeFactory());
    }

    private FlowGraph.ExceptionEdgeKey EDGE_KEY_CLASS_CAST_EXC;

    @Override
    public NodeVisitor begin() {
        EDGE_KEY_CLASS_CAST_EXC = new FlowGraph.ExceptionEdgeKey(
                typeSystem().ClassCastException());
        return super.begin();

    }

    public DataFlowItem createItem(FlowGraph<DataFlowItem> graph, Term n) {
        return new DataFlowItem();
    }

    static class DataFlowItem extends DataFlow.Item {
        // Maps AccessPaths of Sets of ClassTypes
        Map<AccessPath, Set<Type>> classTypes;

        DataFlowItem() {
            classTypes = new HashMap<AccessPath, Set<Type>>();
        }

        DataFlowItem(Map<AccessPath, Set<Type>> classTypes) {
            this.classTypes = classTypes;
        }

        DataFlowItem(DataFlowItem d) {
            classTypes = new HashMap<AccessPath, Set<Type>>(d.classTypes);
        }

        @Override
        public boolean equals(Object o) {
            if (o instanceof DataFlowItem) {
                return this.classTypes == ((DataFlowItem) o).classTypes
                        || this.classTypes
                                .equals(((DataFlowItem) o).classTypes);
            }
            return false;
        }

        @Override
        public int hashCode() {
            return classTypes.hashCode();
        }

        @Override
        public String toString() {
            return "[" + classTypes + "]";
        }
    }

    /**
     * Create an initial Item for the dataflow analysis. By default, the
     * set of not null variables is empty.
     */
    @Override
    protected DataFlowItem createInitialItem(FlowGraph<DataFlowItem> graph,
            Term node, boolean entry) {
        return new DataFlowItem();
    }

    @Override
    protected Map<EdgeKey, DataFlowItem> flow(List<DataFlowItem> inItems,
            List<EdgeKey> inItemKeys, FlowGraph<DataFlowItem> graph,
            Peer<DataFlowItem> peer) {
        return this.flowToBooleanFlow(inItems, inItemKeys, graph, peer);
    }

    /**
     * If a local variable is initialized with a non-null expression, then
     * the variable is not null. If a local variable is assigned non-null
     * expression then the variable is not null; if a local variable is assigned
     * a possibly null expression, then the local variable is possibly null.
     */
    @Override
    public Map<EdgeKey, DataFlowItem> flow(DataFlowItem trueItem,
            DataFlowItem falseItem, DataFlowItem otherItem,
            FlowGraph<DataFlowItem> graph, Peer<DataFlowItem> peer) {
        DataFlowItem dfIn = safeConfluence(trueItem, FlowGraph.EDGE_KEY_TRUE,
                falseItem, FlowGraph.EDGE_KEY_FALSE, otherItem,
                FlowGraph.EDGE_KEY_OTHER, peer, graph);

        if (peer.isEntry()) {
            return itemToMap(dfIn, peer.succEdgeKeys());
        }

        final Term n = peer.node();

        if (n instanceof Instanceof) {
            Instanceof io = (Instanceof) n;
            Expr e = io.expr();
            AccessPath ap = findAccessPathForExpr(e);
            if (ap != null) {
                // on the true branch of an instanceof, we know that
                // the path is in fact an instance of the compare type.
                Map<AccessPath, Set<Type>> trueBranch =
                        addClass(dfIn.classTypes, ap, io.compareType().type());
                return itemsToMap(new DataFlowItem(trueBranch), dfIn, dfIn,
                        peer.succEdgeKeys());
            }
        } else if (n instanceof Cast) {
            Cast cst = (Cast) n;
            Expr ex = cst.expr();
            AccessPath ap = findAccessPathForExpr(ex);
            if (ap != null) {
                // on the non-ClassCastException edges, we know that
                // the cast succeeded, and var instance is in fact an
                // instance of the cast type.
                Map<EdgeKey, DataFlowItem> m =
                        itemToMap(dfIn, peer.succEdgeKeys());
                for (Map.Entry<EdgeKey, DataFlowItem> element : m.entrySet()) {
                    Entry<EdgeKey, DataFlowItem> e = element;
                    if (!e.getKey().equals(EDGE_KEY_CLASS_CAST_EXC)) {
                        DataFlowItem df = e.getValue();
                        e.setValue(new DataFlowItem(addClass(df.classTypes, ap,
                                cst.castType().type())));
                    }
                }
                return m;
            }
        } else if (n instanceof LocalDecl) {
            LocalDecl x = (LocalDecl) n;
            // remove the precise class information...
            Map<AccessPath, Set<Type>> m = killClasses(dfIn.classTypes,
                    new AccessPathLocal(x.localInstance()));
            return itemToMap(new DataFlowItem(m), peer.succEdgeKeys());
        } else if (n instanceof Assign) {
            Assign x = (Assign) n;
            // remove the precise class information...
            AccessPath ap = findAccessPathForExpr(x.left());
            if (ap != null) {
                Map<AccessPath, Set<Type>> m = killClasses(dfIn.classTypes, ap);
                return itemToMap(new DataFlowItem(m), peer.succEdgeKeys());
            }
        } else if (n instanceof Expr && ((Expr) n).type().isBoolean()
                && (n instanceof Binary || n instanceof Unary)) {
            if (trueItem == null) trueItem = dfIn;
            if (falseItem == null) falseItem = dfIn;

            Map<EdgeKey, DataFlowItem> ret = flowBooleanConditions(trueItem,
                    falseItem, dfIn, graph, peer);
            if (ret == null) {
                ret = itemToMap(dfIn, peer.succEdgeKeys());
            }
            return ret;
        } else
            if (n instanceof DowngradeExpr && ((Expr) n).type().isBoolean()) {
            if (trueItem == null) trueItem = dfIn;
            if (falseItem == null) falseItem = dfIn;
            return itemsToMap(trueItem, falseItem, dfIn, peer.succEdgeKeys());
        }

        return itemToMap(dfIn, peer.succEdgeKeys());
    }

    private Map<AccessPath, Set<Type>> killClasses(
            Map<AccessPath, Set<Type>> map, AccessPath ap) {
        Map<AccessPath, Set<Type>> m = new HashMap<AccessPath, Set<Type>>(map);
        boolean changed = (m.remove(ap) != null);
        if (ap instanceof AccessPathLocal) {
            // go through the map and remove any access paths rooted at this local
            for (Iterator<Map.Entry<AccessPath, Set<Type>>> iter =
                    m.entrySet().iterator(); iter.hasNext();) {
                Entry<AccessPath, Set<Type>> entry = iter.next();
                AccessPath key = entry.getKey();
                if (ap.equals(key.findRoot())) {
                    iter.remove();
                    changed = true;
                }
            }

        }
        return changed ? m : map;
    }

    private Map<AccessPath, Set<Type>> addClass(Map<AccessPath, Set<Type>> map,
            AccessPath ap, Type type) {
        if (!type.isClass()) {
            // don't bother adding this type.
            return map;
        }
        Map<AccessPath, Set<Type>> m = new HashMap<AccessPath, Set<Type>>(map);
        Set<Type> s = m.get(ap);
        if (s == null) {
            s = new LinkedHashSet<Type>();
        } else {
            s = new LinkedHashSet<Type>(s);
        }
        m.put(ap, s);
        s.add(type);
        return m;
    }

    /**
     * The confluence operator is intersection: a variable is not null only
     * if it is not null on all paths flowing in.
     */
    @Override
    protected DataFlowItem confluence(List<DataFlowItem> items,
            Peer<DataFlowItem> peer, FlowGraph<DataFlowItem> graph) {
        return intersect(items);
    }

    /**
     * Utility method takes the intersection of a List of DataFlowItems,
     * by intersecting all of their classTypes sets.
     */
    private static DataFlowItem intersect(List<DataFlowItem> items) {
        // take the intersection of all the maps of the
        // DataFlowItems, by examining the smallest one

        // find the smallest Map.
        Map<AccessPath, Set<Type>> smallest = null;

        for (int i = 0; i < items.size(); i++) {
            Map<AccessPath, Set<Type>> candidate = items.get(i).classTypes;
            if (candidate.isEmpty()) {
                // any intersection will be empty.
                return new DataFlowItem(
                        Collections.<AccessPath, Set<Type>> emptyMap());
            }
            if (smallest == null || smallest.size() > candidate.size()) {
                smallest = candidate;
            }
        }

        Map<AccessPath, Set<Type>> intersectMap =
                new HashMap<AccessPath, Set<Type>>(smallest);
        for (int i = 0; i < items.size(); i++) {
            Map<AccessPath, Set<Type>> m = items.get(i).classTypes;
            // go through the entries of intersectMap
            for (Iterator<Map.Entry<AccessPath, Set<Type>>> iter =
                    intersectMap.entrySet().iterator(); iter.hasNext();) {
                Entry<AccessPath, Set<Type>> e = iter.next();
                if (m.containsKey(e.getKey())) {
                    Set<Type> s = new LinkedHashSet<Type>(e.getValue());
                    Set<Type> t = m.get(e.getKey());
                    s.retainAll(t); // could be more precise here, a la SubtypeSet
                } else {
                    // no entry for the set, the intersection is empty, so remove
                    // the key.
                    iter.remove();
                }
            }
        }

        return new DataFlowItem(intersectMap);
    }

    /**
     * "Check" the nodes of the graph for the precise class analysis. This actually
     * consists of setting the preciseClass field in the Jif extensions to
     * nodes, so that their exceptionCheck methods can decide whether to
     * suppress the ClassCastExceptions that they would otherwise declare
     * would be thrown.
     */
    @Override
    protected void check(FlowGraph<DataFlowItem> graph, Term n, boolean entry,
            DataFlowItem inItem, Map<EdgeKey, DataFlowItem> outItems) {
        if (n.del() instanceof JifPreciseClassDel) {
            DataFlowItem dfi = inItem;
            JifPreciseClassDel jpcd = (JifPreciseClassDel) n.del();
            AccessPath ap = findAccessPathForExpr(jpcd.getPreciseClassExpr());
            if (ap != null) {
                jpcd.setPreciseClass(dfi.classTypes.get(ap));
            }
        }
    }

    static AccessPath findAccessPathForExpr(Expr expr) {
        if (expr instanceof Special) {
            return new AccessPathThis();
        }
        if (expr instanceof Local) {
            return new AccessPathLocal(((Local) expr).localInstance());
        }
        if (expr instanceof Field) {
            Field f = (Field) expr;
            if (f.flags().isFinal()) {
                AccessPath target = null;
                if (f.target() instanceof Expr) {
                    target = findAccessPathForExpr((Expr) f.target());
                } else if (f.target() instanceof TypeNode) {
                    target = new AccessPathClass(
                            ((TypeNode) f.target()).type());
                }
                if (target == null) return null;
                return new AccessPathFinalField(target, f.fieldInstance());
            }
        }
        if (expr instanceof DowngradeExpr) {
            DowngradeExpr de = (DowngradeExpr) expr;
            return findAccessPathForExpr(de.expr());
        }
        if (expr instanceof Cast) {
            Cast ce = (Cast) expr;
            return findAccessPathForExpr(ce.expr());
        }
        return null;
    }

    static abstract class AccessPath {
        public abstract AccessPath findRoot();
    }

    static class AccessPathLocal extends AccessPath {
        final LocalInstance li;

        public AccessPathLocal(LocalInstance li) {
            this.li = li;
        }

        @Override
        public AccessPath findRoot() {
            return this;
        }

        @Override
        public int hashCode() {
            return li.hashCode();
        }

        @Override
        public boolean equals(Object o) {
            return (o instanceof AccessPathLocal
                    && ((AccessPathLocal) o).li.equals(this.li));
        }

        @Override
        public String toString() {
            return li.name();
        }
    }

    static class AccessPathFinalField extends AccessPath {
        final AccessPath target;
        final FieldInstance fi;

        public AccessPathFinalField(AccessPath target, FieldInstance fi) {
            this.target = target;
            this.fi = fi;
        }

        @Override
        public AccessPath findRoot() {
            return target.findRoot();
        }

        @Override
        public int hashCode() {
            return fi.hashCode() + target.hashCode();
        }

        @Override
        public boolean equals(Object o) {
            if (o instanceof AccessPathFinalField) {
                AccessPathFinalField that = (AccessPathFinalField) o;
                return that.fi.equals(this.fi)
                        && that.target.equals(this.target);
            }
            return false;
        }

        @Override
        public String toString() {
            return target + "." + fi.name();
        }
    }

    static class AccessPathThis extends AccessPath {
        @Override
        public AccessPath findRoot() {
            return this;
        }

        @Override
        public int hashCode() {
            return -45;
        }

        @Override
        public boolean equals(Object o) {
            return (o instanceof AccessPathThis);
        }

        @Override
        public String toString() {
            return "this";
        }
    }

    static class AccessPathClass extends AccessPath {
        final Type type;

        public AccessPathClass(Type type) {
            this.type = type;
        }

        @Override
        public AccessPath findRoot() {
            return this;
        }

        @Override
        public int hashCode() {
            return type.hashCode();
        }

        @Override
        public boolean equals(Object o) {
            return (o instanceof AccessPathClass
                    && ((AccessPathClass) o).type.equals(this.type));
        }

        @Override
        public String toString() {
            return type.toString();
        }
    }
}
