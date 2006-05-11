package jif.ast;

import jif.extension.*;
import polyglot.ast.JL;
import polyglot.ext.jl.ast.AbstractDelFactory_c;

/**
 * Constructs Jif delegates
 **/
public class JifDelFactory_c extends AbstractDelFactory_c {

  protected JifDelFactory_c() {
    super();
  }

  protected JL delArrayAccessImpl() {
      return new JifArrayAccessDel();
  }
  protected JL delArrayAccessAssignImpl() {
      return new JifArrayAccessAssignDel();
  }
  
  protected JL delArrayInitImpl() {
    return new JifArrayInitDel();
}

protected JL delBinaryImpl() {
      return new JifBinaryDel();
  }
  protected JL delCallImpl() {
      return new JifCallDel();
  }
  protected JL delCastImpl() {
      return new JifCastDel();
  }
  protected JL delClassDeclImpl() {
      return new JifClassDeclDel();
  }
  protected JL delFieldDeclImpl() {
      return new JifFieldDeclDel();
  }
  protected JL delFieldImpl() {
      return new JifFieldDel();
  }
  protected JL delFieldAssignImpl() {
      return new JifFieldAssignDel();
  }
  protected JL delFormalImpl() {
      return new JifFormalDel();
  }
  protected JL delIfImpl() {
      return new JifIfDel();
  }
  protected JL delInitializerImpl() {
      return new JifInitializerDel();
  }
  protected JL delInstanceofImpl() {
      return new JifInstanceOfDel();
  }
  protected JL delLocalDeclImpl() {
      return new JifLocalDeclDel();
  }
  
  protected JL delNewArrayImpl() {
      return new JifNewArrayDel();
  }

  protected JL delThrowImpl() {
      return new JifThrowDel();
  }
  protected JL delTypeNodeImpl() {
      return new JifTypeNodeDel();
  }

  protected JL delConstructorCallImpl() {
      return new JifConstructorCallDel();
  }
  protected JL delMethodDeclImpl() {
      return new JifMethodDeclDel();
  }
  protected JL delConstructorDeclImpl() {
      return new JifProcedureDeclDel();
  }
}
