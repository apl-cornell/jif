package jif.translate;

import polyglot.ast.*;
import polyglot.ext.jl.ast.*;
import jif.ast.*;
import polyglot.types.*;
import polyglot.ext.jl.types.*;
import jif.types.*;
import polyglot.visit.*;

public interface ToJavaExt extends Ext {
    NodeVisitor toJavaEnter(JifToJavaRewriter rw) throws SemanticException;
    Node toJava(JifToJavaRewriter rw) throws SemanticException;
}
