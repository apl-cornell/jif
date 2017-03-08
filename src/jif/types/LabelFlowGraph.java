package jif.types;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import jif.Topics;
import jif.types.InformationFlowTrace.Direction;
import jif.types.label.JoinLabel;
import jif.types.label.Label;
import jif.types.label.VarLabel;
import jif.types.label.VarLabel_c;
import polyglot.main.Report;
import polyglot.util.CollectionUtil;
import polyglot.util.Position;

/*
 * A label flow graph shows all label flows that are related to a type error
 * It takes a trace recorded by the solver as input
 */

public class LabelFlowGraph extends Graph {
    List<InformationFlowTrace> tr;
    Set<String> files; // source codes involved
    FailedConstraintSnapshot jiferror; // used to highlight the error in graph
    boolean generated; // if the graph has been generated already, just reuse it
    LabelNode root; // starting node
    static int count = 1; // counter for jif errors. Report information flow paths for each error

    /* there are two kinds of edges in the flow graph:
     * 1) dynamics: corresponds to the constraints
     * 2) statics : such as join labels, pc labels that are provable with no constraint
     * the following two edge types correspond to static edges
     */
    final FlowEdge staticEdge = new FlowEdge(null);

    /*
     * fields for Jif option -report
     */
    public static final Collection<String> flowgraphtopic =
            CollectionUtil.list(Topics.labelFlow);
    // different levels of details
    public static final int messageOnly = 1; // concise path info
    public static final int detailedMessage = 2; // detailed path info, including explanation of each constraint
    public static final int showSlicedGraph = 3; // output the relevant graph (nodes related to the error) into a dot file
    public static final int showWholeGraph = 4; // output the whole graph into a dot file

    public static boolean shouldReport(int obscurity) {
        return Report.should_report(flowgraphtopic, obscurity);
    }

    public LabelFlowGraph(List<InformationFlowTrace> t,
            FailedConstraintSnapshot snapshot) {
        tr = t;
        generated = false;
        root = new LabelNode("ROOT",
                new VarLabel_c("ROOT", "fake label", null, null)); // root is just serves as the start point

        files = new HashSet<String>();
        this.jiferror = snapshot;
    }

    /*
     * an edge in graph is either static (join) or dynamic (a flow that generated from an equation)
     * the difference is determined by if equ is null for now
     * TODO: do we really need static links?
     * TODO: add meet labels
     */
    private class FlowEdge extends Edge {
        Equation equ;

        public FlowEdge(Equation eq) {
            this.equ = eq;
        }

        public int getLineno() {
            if (equ != null)
                return equ.position().line();
            else return 0;
        }

        @Override
        public String toString() {
            if (equ != null) {
                return "because of constraint: " + equ.constraint.toString();
            } else return "join";
        }

        public String toStringDetail() {
            if (equ != null) {
                return toString() + "\n(Why this constraint?) "
                        + equ.constraint.detailMsg();
            } else return toString();
        }

        public String toDotString() {
            if (equ != null) {
                return equ.constraint().lhs.toString()
                        + equ.constraint.kind.toString()
                        + equ.constraint.rhs.toString();
            } else return "join";
        }
    }

    /* this class just modifies the behavior of "equals" on the labels */
    private class LabelWrapper {
        Label label;

        public LabelWrapper(Label lbl) {
            this.label = lbl;
        }

        @Override
        public boolean equals(Object obj) {
            if (!(obj instanceof LabelWrapper)) return false;
            LabelWrapper labelwrap = (LabelWrapper) obj;
            return this.label == labelwrap.label;
        }

        @Override
        public int hashCode() {
            return this.label.hashCode();
        }
    }

    // a node in label-flow graph has an unique id (to generate dot graph), and a Jif label
    private class LabelNode extends Node {
        String uid;
        Label label;

        public LabelNode(String id, Label label) {
            super();
            this.uid = id;
            this.label = label;
        }

        // TODO: shall we distinguish pc labels for better explanation?
//        boolean ispc () {
//            return ( (label instanceof JoinLabel) && label.description()!=null && label.description().equals("pc label"));
//        }

        public String getName() {
            if (label == null) System.out.println("NULL!!");
            if (label instanceof VarLabel)
                return ((VarLabel) label).name() + (label.position() == null
                        ? "" : "@" + label.position().toString());
            else {
                return (label.description() == null ? "" : label.description())
                        + label.toString() + (label.position() == null ? ""
                                : "@" + label.position().toString());
            }
        }

        public Position position() {
            return label.position();
        }

        /* treat join labels in backtracking specially for better error message */
        @Override
        public boolean isend(boolean isbackward) {
            if (isbackward) {
                return !label.hasVariableComponents()
                        && !(label instanceof JoinLabel);
            } else return !label.hasVariableComponents();
        }

        @Override
        public String toString() {
            return "Current node: " + getName() + "\n";
        }

        public String printNodeToDotString() {
            return uid + " [label=\"" + getName() + "\\n" + label.position()
                    + "\"];\n";
        }

        public String printLinkToDotString() {
            String ret = "";
            for (Node node : outs.keySet()) {
                LabelNode n = (LabelNode) node;
                String linkinfo = ((FlowEdge) outs.get(n)).toDotString();
                if (n.shouldprint) {
                    ret += this.uid + "->" + n.uid + " [label=\"" + linkinfo
                            + "\"];\n";
                }
            }
            return ret;
        }

        // get the position of current node and out links that links to printable nodes
        public Set<Integer> getPositions() {
            Set<Integer> ret = new HashSet<Integer>();
            if (this.position() != null) ret.add(this.position().line());
            for (Node n : outs.keySet()) {
                if (n.shouldprint)
                    ret.add(((FlowEdge) outs.get(n)).getLineno());
            }
            return ret;
        }
    }

    /*
     * generate dot file
     */
    private class ToDotVisitor implements NodeVisitor {
        Set<Integer> sourcePosition = new HashSet<Integer>();
        String nodes = "";
        String links = "";

        @Override
        public void discoverVertex(Node n) {
            return;
        }

        @Override
        public void leaveVertex(Node n) {
            return;
        }

        @Override
        public void visit(Node node) {
            if (node instanceof LabelNode) {
                LabelNode n = (LabelNode) node;
                if (!n.shouldprint) return;
                sourcePosition.addAll(n.getPositions());
                nodes += n.printNodeToDotString();
                links += n.printLinkToDotString();
            }
        }

        String getNodeString() {
            return nodes;
        }

        String getLinkString() {
            return links;
        }
    }

    /*
     *  map Jif labels to graph nodes
     */
    Map<LabelWrapper, LabelNode> lblToNode =
            new HashMap<LabelWrapper, LabelNode>(); // map from uid to a the id corresponding var
    int varCounter = 0;

    /* get the corresponding node in graph. Create one if none exists */
    public LabelNode getNode(Label label) {
        LabelWrapper lbl = new LabelWrapper(label);
        if (!lblToNode.containsKey(lbl)) {
            String vid = "v" + varCounter;
            LabelNode n = new LabelNode(vid, label);
            varCounter++;
            // record the source files involved
            if (label.position() != null) {
                files.add(label.position().path());
            }
            /* make the node reachable from root */
            addEdge(root, n, staticEdge);
            lblToNode.put(lbl, n);
        }
        return lblToNode.get(lbl);
    }

    public void generateGraph() {
        if (generated || tr == null) return;

        // generate the dynamic links
        for (InformationFlowTrace t : tr) {
            LabelNode to = getNode(t.varlbl);
            LabelNode source = getNode(t.lblflows);

            Edge edge = new FlowEdge(t.equ);
            addEdge(source, to, edge);

            if (t.dir == Direction.BOTH) {
                addEdge(to, source, edge);
            }
        }

        // add the failed constraint to the graph
        Equation con = jiferror.failedConstraint;
        if (con instanceof LabelEquation) {
            LabelEquation e = (LabelEquation) con;
            // retrieve the unmodified lhs and rhs
            Label lhs = e.lhs();
            Label rhs = e.rhs();

            LabelNode to = getNode(rhs);
            LabelNode from = getNode(lhs);
            addEdge(from, to, new FlowEdge(e));
        }

        /* generate static links
         * there are two types of static links:
         * 1. flow from components to a join label
         * 2. provable flows between labels according to the current valuation
         * the second is not essential
         * TODO: handle meet
         */
        // only need to handle nodes in the graph
        List<LabelNode> workingList =
                new ArrayList<LabelNode>(lblToNode.values());
        Set<LabelNode> processed = new HashSet<LabelNode>();

        // first, handle the join labels
        while (workingList.size() != 0) {
            LabelNode currentnode = workingList.get(0);
            workingList.remove(0);
            processed.add(currentnode);
            Label lbl = currentnode.label;

            // generate the source node
            Collection<Label> sourceset;
            if (lbl instanceof JoinLabel) {
                JoinLabel join = (JoinLabel) lbl;
                sourceset = (join).joinComponents();
            } else {
                continue;
            }

            for (Label srclbl : sourceset) {
                LabelNode srcnode = getNode(srclbl);
                if (!processed.contains(srclbl)
                        && !workingList.contains(srclbl))
                    workingList.add(getNode(srclbl));
                addEdge(srcnode, currentnode, staticEdge);
            }

            // next, handle the provable flows
//            for (Label component : sourceset) {
//                if (jiferror.failedConstraint.env().leq(
//                        jiferror.bounds.applyTo(lbl),
//                        jiferror.bounds.applyTo(component))) {
//                    addEdge(getNode(lbl), getNode(component), staticEdge);
//                }
//            }
        }

        generated = true;
    }

    // this function is used to filter out letters that can not pretty print in the dot format
    // such as " and \n
    private String sanitaze(String s) {
        if (s != null)
            return s.replace('"', '\'').replace("\\", "\\\\");
        else return s;
    }

    public String toDotString() {
        String ret = "";
        ToDotVisitor v = new ToDotVisitor();
        List<Node> visited = new ArrayList<Node>();
        root.acceptForward(v, visited);

        ret += "digraph G1 {\n";
        // print source code
        for (String s : files) {
            BufferedReader reader = null;
            try {
                reader = new BufferedReader(new FileReader(s));
                String line = reader.readLine();
                int linenum = 1;
                ret += "source [shape=box, label=\"";
                while (line != null) {
//                    if (v.getSourcePosition().contains(linenum)) {
                    ret += linenum + ":\t" + sanitaze(line) + "\\l";
//                    }
                    line = reader.readLine();
                    linenum++;
                }
                ret += "\"];\n";
            } catch (IOException e) {
                continue;
            } finally {
                if (reader != null) {
                    try {
                        reader.close();
                    } catch (IOException e) {
                    }
                }
            }
        }

        ret += "node [color = grey, style = filled];\n";
        ret += v.getNodeString();
        ret += v.getLinkString();
        ret += "}\n";
        return ret;
    }

    public void slicing(Node backward, Node forward) {
        List<Node> visited = new ArrayList<Node>();
        backward.acceptBackward(new LabellingVisitor(), visited);
        visited = new ArrayList<Node>();
        forward.acceptForward(new LabellingVisitor(), visited);
    }

    public void showErrorPath() {
        FailedConstraintSnapshot snapshot = jiferror;
        boolean detail = shouldReport(detailedMessage);

        if (!generated) generateGraph();

        if (snapshot.failedConstraint instanceof LabelEquation) {

            /* for a failed constrain a <= b, we know that there must be some label that flows into a, but
             * cannot flow into some label that b flows into
             * so we backtrack from label a for the source of path and start from label b for sinks nodes
             */
            LabelEquation equ = (LabelEquation) snapshot.failedConstraint;
            Set<List<Node>> leftPaths = getBackwardPaths(getNode(equ.lhs()));
            Set<List<Node>> rightPaths = getForwardPaths(getNode(equ.rhs()));

            for (List<Node> leftpath : leftPaths) {
                LabelNode leftmost = (LabelNode) leftpath.get(0);

                for (List<Node> rightpath : rightPaths) {
                    boolean skip = false;
                    LabelNode rightmost = null;
                    for (Node n : rightpath) {
                        rightmost = (LabelNode) n;
                        if (leftpath.contains(n)) {
                            skip = true;
                            break;
                        }
                    }

                    if (!skip
                            && !snapshot.failedConstraint.env()
                                    .leq(snapshot.bounds
                                            .applyTo(leftmost.label),
                                    snapshot.bounds.applyTo(rightmost.label))) {
                        System.out.println("\n----Start of one path----");
                        System.out.println(leftmost.getName());
                        LabelNode prev = leftmost;
                        FlowEdge edge;
                        for (int i = 1; i < leftpath.size(); i++) {
                            LabelNode next = (LabelNode) leftpath.get(i);
                            edge = (FlowEdge) prev.outs.get(next);
                            System.out.println(
                                    "--> (" + (detail ? edge.toStringDetail()
                                            : edge.toString()) + ")");
                            System.out.println(next.getName());
                            prev = next;
                        }
                        edge = new FlowEdge(snapshot.failedConstraint);
                        System.out.println(
                                "-> (" + (detail ? edge.toStringDetail()
                                        : edge.toString()) + ")");
                        prev = (LabelNode) rightpath.get(0);
                        System.out.println(prev.getName());
                        for (int i = 1; i < rightpath.size(); i++) {
                            LabelNode next = (LabelNode) rightpath.get(i);
                            edge = (FlowEdge) prev.outs.get(next);
                            System.out.println(
                                    "--> (" + (detail ? edge.toStringDetail()
                                            : edge.toString()) + ")");
                            System.out.println(next.getName());
                            prev = next;
                        }
                        System.out.println("----End of one path----\n");
                    }
                }
            }
        }
    }

    public void writeToDotFile() {
        String filename;

        FailedConstraintSnapshot snapshot = jiferror;
        filename = "error" + count + ".dot";
        count++;
        if (!generated) generateGraph();

        try {
            FileWriter fstream = new FileWriter(filename);
            BufferedWriter out = new BufferedWriter(fstream);
            if (!shouldReport(showWholeGraph)) {
                LabelEquation equ = (LabelEquation) snapshot.failedConstraint;
                slicing(getNode(equ.lhs()), getNode(equ.rhs()));
            } else labelAll(root);
            root.shouldprint = false;
            out.write(toDotString());
            out.close();
        } catch (IOException e) {
            System.out.println("Unable to write the DOT file to: " + filename);
        }
    }
}
