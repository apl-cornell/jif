package jif.ast;

import java.util.List;

import polyglot.ast.Node;

public interface RifPolicyNode extends Node {

    public List<RifComponentNode> components();

}
