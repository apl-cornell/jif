package jif.translate;

import polyglot.ast.*;
import polyglot.ext.jl.ast.*;
import jif.ast.*;
import polyglot.types.*;
import polyglot.ext.jl.types.*;
import jif.types.*;
import jif.types.label.Label;
import jif.visit.*;
import java.io.*;

public interface LabelToJavaExpr extends Serializable {
    public Expr toJava(Label label, JifToJavaRewriter rw) throws SemanticException;
}
