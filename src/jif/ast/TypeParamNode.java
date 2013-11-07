package jif.ast;

import jif.types.TypeParam;
import polyglot.ast.TypeNode;

public interface TypeParamNode extends ParamNode {

    TypeParamNode parameter(TypeParam param);

    TypeNode typeNode();

    TypeParamNode typeNode(TypeNode type);

}
