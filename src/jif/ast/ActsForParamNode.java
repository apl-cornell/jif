package jif.ast;

import jif.types.ActsForParam;

/**
 * A placeholder in the AST for a Jif principal or label.
 */
public interface ActsForParamNode<Param extends ActsForParam>
        extends ParamNode {
    @Override
    Param parameter();

    ActsForParamNode<Param> parameter(Param parameter);
}
