package jif.types;

import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import polyglot.util.InternalCompilerError;

/* assumptions: directed graph, unweighted edges */
public class Graph {

    private Set nodes;
    private Map edges;

    /** inputs:  Set of objects representing nodes
                 Map of dependencies between nodes (directed graph)
                 Map is from object in Set to a Set of objects, which must
                 all be valid nodes 

                 if object is in the Keyset of the Map, then the Set 
                 associated with that map should not be empty
    */

    public Graph(Set nodes, Map edges)  {
        this.nodes = nodes;
        this.edges = edges;
        validateGraph();
    }

    /* validating that we are given a proper graph 

       A proper graph follows the following rules:

       1) there is at most one edge from node a to node b
       2) if there is an edge from node a to node b, then node
          a and node b are in the set of nodes (nodes.contains(a) ==
          nodes.contains(b) == true)

     */
    private void validateGraph() {
        if (nodes.isEmpty()) {
            if (edges.size() <= 0) {
                return;
            } else {
                // if there are edges and no nodes, invalid graph
                throw new InternalCompilerError("Graph has edges but no nodes!");
            }
        }
        
        if (edges.size() <= 0) {
            return;
        }

        // make sure that each source is an acutal node
        Object src = null;
        for (Iterator e = edges.keySet().iterator(); e.hasNext() ; ) {
            src = e.next();
            if (! nodes.contains(src)) {
                // found a source that's not a node
                throw new InternalCompilerError("Found an edge where the " +
                        "source node is not in the node set!");
            }
        }

        Set destSet = null;
        Object dest= null;
        // make sure all destnations of edges are valid nodes
        for (Iterator e = edges.values().iterator(); e.hasNext();) {
            destSet = (Set) e.next();
            if (destSet.size() <= 0) {
                throw new InternalCompilerError("Edge without a destination!");
            }
            for (Iterator f = destSet.iterator(); f.hasNext() ; ) {
                dest = f.next();
                if (! nodes.contains(dest)) {
                    // found a destination that's not a node
                    throw new InternalCompilerError("Found an edge where the " +
                            "destination node is not in the node set!");
                }
            }
        }
    }

/**************************************************************************/
/* DFS search information */
/* helper for the other two tasks needed */

    // if this is false, then the info in dfsFinishTimes and 
    // dfsStartTimes is not reliable.
    private boolean DFSDone = false;
    private int dfsTime;
    /* these two maps store mappings from nodes in the graph to
       start and finish times respectively.

       the keys in both of these maps will contain the same set 
       of nodes that are in the nodes variable.

       For an empty graph, both of these variables will be either be 
       empty LinkedHashMaps
     */
    private Map dfsFinishTimes = null;
    private Map dfsStartTimes = null;

    /* run the DFS search calculating start and finish times.
       
       Algorithm from CLR 1st edition section 23.3
     */
    public void DFS() {
        // initialize stuff
        DFSDone = false;
        dfsFinishTimes = new LinkedHashMap();
        dfsStartTimes = new LinkedHashMap();
        dfsTime = 0;

        Object n;
        for (Iterator e = nodes.iterator(); e.hasNext();) {
            n = e.next();
            if (! dfsStartTimes.containsKey(n)) {
                DFSVisit(n);
            }
        }
        DFSDone = true;
    }

    private void DFSVisit(Object n) {
        dfsStartTimes.put(n, new Integer(dfsTime));
        dfsTime += 1;
        // visit other edges
        // uses assumption of no empty sets in the edge map
        Set outEdges = null;
        if (edges.containsKey(n)) {
            outEdges = (Set) edges.get(n);
            Object x;
            for (Iterator e = outEdges.iterator(); e.hasNext();) {
                x = e.next();
                if (! dfsStartTimes.containsKey(x)) {
                    DFSVisit(x);
                }
            }
        }
        dfsFinishTimes.put(n, new Integer(dfsTime));
        dfsTime += 1;
    } 

/**************************************************************************/
/* Topological sort info */

    /* returns linked list of nodes such that 
       the nodes are in topological order

       basic idea is they are sorted by finish time of the DFS,
       highest finish time first. Algorithm is taken from CLR, 1st 
       edition, section 23.4 */

    public LinkedList topoSort() {
        // if DFS not done yet, do DFS, then continue
        if (! DFSDone) {
            DFS();
        }

        Map finishSort = new LinkedHashMap(dfsFinishTimes);
        Object max = null;
        LinkedList l = new LinkedList();

        // time is always >= 0
        int maxFinish = -1;
        /* find the max finish time, take the node associated with
           that finish time and add it to the end of the list
           Then remove that node from consideration and repeat until
           we are finished with all the nodes
         */
        while (finishSort.size() > 0) {
            Object tmp = null;
            max = null;
            for (Iterator e = finishSort.keySet().iterator(); e.hasNext(); ) {
                tmp = e.next();
                int tmpInt = ((Integer) finishSort.get(tmp)).intValue();
                if (tmpInt > maxFinish) {
                    maxFinish = tmpInt;
                    max = tmp;
                }
            }

/*            if (max == null) {
                System.err.println("null in sorting");
                System.err.println(finishSort.size());
                System.err.println("max finish: " + maxFinish);
                System.exit(-1);
            } */

            l.add(max);
            finishSort.remove(max);
            maxFinish = -1;
        }
        return l;
    }

/**************************************************************************/

    /* returns list of sets. each set contains all the nodes in 
       a strongly connected component in the graph

       Uses the algorithm in CLR first edition section 23.5
     */
    public List getStrongConnectedComponents() {
        LinkedList l = new LinkedList();
        Set currentComponent, visited;
        visited = new LinkedHashSet();
        // first DFS run
        if (! DFSDone) {
            DFS();
        }
        // computing G^T edges
        Map reversedEdges = reverseEdges(edges);
        // the order in which we consider nodes for the second DFS
        LinkedList order = topoSort();
        Object n;
        // every time we start a new 'tree', create a new
        // component set and add elements to it
        for (Iterator e = order.iterator(); e.hasNext();){
            n = e.next();
            if (! visited.contains(n)) {
                visited.add(n);
                currentComponent = new LinkedHashSet();
                currentComponent.add(n);

                getStrongDFSHelper(visited, currentComponent, n, reversedEdges);

                l.add(currentComponent);
            }
        }
        return l;
    }

   /* Helper function for the connected components similar to the
      helper fuction for DFS. Difference is that this one doesn't use
      private class variables and does add stuff to a set for the current
      componenet that is being worked on.

      Couldn't think of a good way to combine the two helper functions (or
      to write DFS in such a way that I could use DFS twice with the minor
      tweaks as options cleanly). I wish I was using SML.
    */
   private void getStrongDFSHelper(Set visited, Set currentComponent,
                                    Object n, Map edges){
        Set outEdges;
        // if there are edges, explore them
        if (edges.containsKey(n)) {
            outEdges = (Set) edges.get(n);
            Object x;
            for (Iterator e = outEdges.iterator(); e.hasNext();) {
                x = e.next();
                if (! visited.contains(x)) {
                    visited.add(x);
                    currentComponent.add(x);
                    getStrongDFSHelper(visited, currentComponent, x, edges);
                }
            }
        }
    }
 
    /* reverses the edges in a Map inputEdges.
       inputEdges must have the same structure as the edges variable
       and use a subset of the nodes that are part of the current graph.
     */
    private Map reverseEdges (Map inputEdges) {
        // initializing a hash map to store all edges going out of
        // a node to the empty set first
        Map reversed = new LinkedHashMap();
        Object o;
        for (Iterator e = nodes.iterator(); e.hasNext();){
            o = e.next();
            reversed.put(o, new LinkedHashSet());
        }

        // finding all the edges in the original graph and adding
        // the reversed edge to our new edge map
        Object oldSrc, newSrc;
        Set oldDests, newDests;
        for (Iterator e = inputEdges.keySet().iterator(); e.hasNext();){
            oldSrc = e.next();
            oldDests = (Set) inputEdges.get(oldSrc);
            for (Iterator f = oldDests.iterator(); f.hasNext();){
                newSrc = f.next();
                newDests = (Set) reversed.get(newSrc);
                newDests.add(oldSrc);
            }
        }
        // removing all the mappings to empty sets (no edges out 
        // of those nodes)
        Set check;
        for (Iterator e = reversed.keySet().iterator(); e.hasNext();){
            o = e.next();
            check = (Set) reversed.get(o);
            if (check.size() <= 0) {
                // taking advantage of the map-set-iterator relationship
                e.remove();
            }
        }
        return reversed;
    }

/**************************************************************************/
/* Methods for creating a new graph with the nodes being sets of strongly
   connected components
 */

    /* returns a graph based on the strongly connected components of this 
       graph. The nodes are the strongly connected components, which are 
       represented by Sets that contain the old nodes. The edges are formed
       by checking to see if there is an edge out of any of the old nodes
       into another node that is in another componenet, and if that exists
       then there is an edge going to that componenet.
     */
    public Graph getSuperNodeGraph() {
        return getSuperNodeGraph(getStrongConnectedComponents());
    }

    /* same as the above but can take a previous run of 
       getStrongConnectedComponents() as input. If any other is put
       in, I(the code) takes no responsibility whatsoever in what happens.
     */
    public Graph getSuperNodeGraph(List superNodes) {
        Set newNodeSet = new LinkedHashSet(superNodes);
        Map newEdgeMap = new LinkedHashMap();

        // initialize everything with no edges
        for (Iterator e = newNodeSet.iterator(); e.hasNext();){
            newEdgeMap.put(e.next(), new LinkedHashSet());
        }

        Object src, tgt;
        Set srcSet, destSet, edgeSet;
        for (Iterator e = nodes.iterator(); e.hasNext();){
            src = e.next();
            // every node should be in the list of superNodes
            // srcSet should never be null ever!
            srcSet = findNode(newNodeSet, src);
            // if there were edges out of that node
            if (edges.containsKey(src)) {
                edgeSet = (Set) edges.get(src);
                for (Iterator f = edgeSet.iterator(); f.hasNext();) {
                    tgt = f.next();
                    // if the target isn't in our component,
                    // need to add a new edge
                    if (! srcSet.contains(tgt)) {
                        destSet = findNode(newNodeSet, tgt);
                        // destSet should never be null ever!
                        ((Set) newEdgeMap.get(srcSet)).add(destSet);
                    }
                }
            }
            // if there weren't any edges, we're done with this node 
        } // end for

        // filter out components with no edges out of them
        for (Iterator e = newEdgeMap.keySet().iterator(); e.hasNext(); ){
            src = e.next();
            // if there are no acutal edges, remove that mapping
            if ( ((Set) newEdgeMap.get(src)).isEmpty()) {
                e.remove();
            }
        }
        return new Graph(newNodeSet, newEdgeMap);
    }

    // helper function for above: find which interal set Object o is in
    private Set findNode(Set hs, Object o) {
        Set s;
        for (Iterator e = hs.iterator(); e.hasNext();) {
            s = (Set) e.next();
            if (s.contains(o)) {
                return s;
            }
        }
        return null;
    }

    /** inputs: Set of objects representing nodes
                Map of dependencies between the nodes (directed graph)

        output: List of Sets. Each set contains a strongly connected component
                of the graph, and the list is sorted topologically with the
                first set(component) being the least dependant.

        invariants: every object in the map is a member of the set
    */

/**************************************************************************/


    public String toString() {
        StringBuffer sb = new StringBuffer();
        Object o;
        for (Iterator e = nodes.iterator(); e.hasNext();) {
            o = e.next();
            sb.append(o); sb.append(": \n");
            if (edges.containsKey(o)) {
                for (Iterator f=((Set) edges.get(o)).iterator();f.hasNext();){
                    sb.append("     ");
                    sb.append(f.next());
                    sb.append("\n ");
                }
            }
            sb.append("\n");
        }

        return sb.toString();
    }

    // for printing output of getStrongConnectedComponents()
/*    public static String connectedToString(List connected) {

        StringBuffer sb = new StringBuffer();
        sb.append("Connected components:\n");
        for (Iterator e = connected.iterator(); e.hasNext();){
            Set s = (Set) e.next();
            sb.append(" - ");
            for (Iterator f = s.iterator(); f.hasNext();){
                sb.append(f.next().toString());
                sb.append(", ");
            }
            sb.append("\n");
        }
        return sb.toString();
    } */

    /* for printing a graph where the the nodes are sets.
        vey uesful for the strongly connected graphs
     */ 
    public String toStringSetNodes() {

        StringBuffer sb = new StringBuffer();

        Set node, targets;
        for (Iterator e = nodes.iterator(); e.hasNext();){
            // printing the initial node
            node = (Set) e.next();
            sb.append(superPrintNode(node));
            sb.append(": \n");

            // printing targets for that node
            if (edges.containsKey(node)) {
                targets = (Set) edges.get(node);
                for (Iterator f = targets.iterator(); f.hasNext();) {
                    sb.append("     ");
                    sb.append(superPrintNode((Set) f.next()));
                    sb.append("\n");
                }
            }
            sb.append("\n");
        }
        sb.append("\n");
        return sb.toString();
    }

    // helper for output of superToString
    private String superPrintNode(Set s) {
        StringBuffer sb = new StringBuffer();
        sb.append("(");
        boolean first = true;

        for (Iterator e = s.iterator(); e.hasNext();) {
            if (!first) {
                sb.append(", ");
            } else {
                first = false;
            }
            sb.append(e.next().toString());
        }
        sb.append(")");
        return sb.toString();
    }



}
