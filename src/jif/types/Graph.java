package jif.types;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public abstract class Graph {

    abstract class Edge {
    }

    abstract class Node {
        Map<Node, Edge> ins;
        Map<Node, Edge> outs;
        boolean shouldprint;

        public Node() {
            ins = new HashMap<Node, Edge>();
            outs = new HashMap<Node, Edge>();
            shouldprint = false;
        }

        public void acceptForward(NodeVisitor v, List<Node> visited) {
            if (visited.contains(this)) return;
            v.discoverVertex(this);
            v.visit(this);
            visited.add(this);
            for (Node n : outs.keySet()) {
                n.acceptForward(v, visited);
            }
            v.leaveVertex(this);
        }

        public void acceptBackward(NodeVisitor v, List<Node> visited) {
            if (visited.contains(this)) return;
            v.discoverVertex(this);
            v.visit(this);
            visited.add(this);
            for (Node n : ins.keySet()) {
                n.acceptBackward(v, visited);
            }
            v.leaveVertex(this);
        }

        abstract boolean isend(boolean isbackward);
    }

    public interface NodeVisitor {
        public void discoverVertex(Node n);

        public void visit(Node n);

        public void leaveVertex(Node n);
    }

    protected void addEdge(Node from, Node to, Edge edge) {
        from.outs.put(to, edge);
        to.ins.put(from, edge);
    }

    // find all paths from end nodes to the failed edge. Direction depends on field forward
    // This is a DFS search on the graph
    // TODO: the current implementation doesn't always return all paths
    protected class PathFinder implements NodeVisitor {
        ArrayList<Node> currentpath;
        Set<List<Node>> results;
        boolean isbackward;

        public PathFinder(Set<List<Node>> results, boolean isBackward) {
            this.results = results;
            currentpath = new ArrayList<Node>();
            this.isbackward = isBackward;
        }

        @Override
        public void discoverVertex(Node n) {
            if (isbackward)
                currentpath.add(0, n);
            else currentpath.add(n);
        }

        @Override
        public void leaveVertex(Node n) {
            if (isbackward)
                currentpath.remove(0);
            else currentpath.remove(currentpath.size() - 1);
        }

        @Override
        public void visit(Node n) {
            if (n.isend(isbackward)) {
                @SuppressWarnings("unchecked")
                List<Node> clone = (List<Node>) currentpath.clone();
                results.add(clone);
            }
        }
    }

    public Set<List<Node>> getBackwardPaths(Node start) {
        Set<List<Node>> ret = new HashSet<List<Node>>();
        List<Node> visited = new ArrayList<Node>();
        start.acceptBackward(new PathFinder(ret, true), visited);
        return ret;
    }

    public Set<List<Node>> getForwardPaths(Node start) {
        Set<List<Node>> ret = new HashSet<List<Node>>();
        List<Node> visited = new ArrayList<Node>();
        start.acceptForward(new PathFinder(ret, false), visited);
        return ret;
    }

    // this visitor just labels all node visited as "shouldprint"
    protected class LabellingVisitor implements NodeVisitor {
        @Override
        public void discoverVertex(Node n) {
            return;
        }

        @Override
        public void leaveVertex(Node n) {
            return;
        }

        @Override
        public void visit(Node n) {
            n.shouldprint = true;
        }
    }

    public void labelAll(Node root) {
        List<Node> visited = new ArrayList<Node>();
        root.acceptForward(new LabellingVisitor(), visited);
        root.shouldprint = false;
    }

}
