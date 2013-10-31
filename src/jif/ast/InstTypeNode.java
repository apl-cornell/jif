package jif.ast;

import java.util.List;

import polyglot.ast.TypeNode;

/** Instantiated type node.
 */
public interface InstTypeNode extends TypeNode {
    TypeNode base();

    InstTypeNode base(TypeNode base);

    List<ParamNode> params();

    InstTypeNode params(List<ParamNode> params);
}
