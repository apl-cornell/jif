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
  protected JL delCallImpl() {
      return new JifCallDel();
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
  protected JL delInitializerImpl() {
      return new JifInitializerDel();
  }
  protected JL delLocalDeclImpl() {
      return new JifLocalDeclDel();
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
