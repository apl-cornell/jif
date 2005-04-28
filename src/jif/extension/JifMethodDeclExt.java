package jif.extension;

import java.util.Iterator;
import java.util.List;

import jif.ast.JifMethodDecl;
import jif.translate.ToJavaExt;
import jif.types.*;
import jif.types.label.ArgLabel;
import jif.types.label.Label;
import jif.visit.LabelChecker;
import polyglot.ast.Block;
import polyglot.ast.Node;
import polyglot.main.Report;
import polyglot.types.SemanticException;
import polyglot.types.Type;
import polyglot.util.InternalCompilerError;
import polyglot.util.StringUtil;

/** The Jif extension of the <code>JifMethodDecl</code> node.
 *
 *  @see jif.ast.JifMethodDecl
 */
public class JifMethodDeclExt extends JifProcedureDeclExt_c
{
    public JifMethodDeclExt(ToJavaExt toJava) {
        super(toJava);
    }

    public Node labelCheck(LabelChecker lc) throws SemanticException
    {
        JifMethodDecl mn = (JifMethodDecl) node();
        JifMethodInstance mi = (JifMethodInstance) mn.methodInstance();

        // check that the labels in the method signature conform to the
        // restrictions of the superclass and/or interface method declaration.
        overrideMethodLabelCheck(lc, mi);

	JifTypeSystem ts = lc.jifTypeSystem();
      	JifContext A = lc.jifContext();
	A = (JifContext) mn.enterScope(A);
        lc = lc.context(A);

	// First, check the arguments, adjusting the context.
	Label Li = checkArguments(mi, lc);

	Block body = null;
	PathMap X;

	if (! mn.flags().isAbstract() && ! mn.flags().isNative()) {
	    // Now, check the body of the method in the new context.

	    // Visit only the body, not the formal parameters.
	    body = (Block) lc.context(A).labelCheck(mn.body());
	    X = X(body);

	    if (Report.should_report(jif_verbose, 3))
		Report.report(3, "Body path labels = " + X);

	    addReturnConstraints(Li, X, mi, lc, mi.returnType());
	}
	else {
	    X = ts.pathMap();
	    X = X.N(A.entryPC()); //###
	}

	mn = (JifMethodDecl) X(mn.body(body), X);

	return mn;
    }

    /**
     * Check that this method instance <mi> conforms to the signatures of any
     * methods in the superclasses or interfaces that it is overriding.
     *
     * In particular, argument labels and start labels are contravariant,
     * return labels, return value labels and labels on exception types are
     * covariant.
     */
    protected void overrideMethodLabelCheck(LabelChecker lc, final JifMethodInstance mi) throws SemanticException {
        JifTypeSystem ts = lc.jifTypeSystem();

        for (Iterator iter = mi.implemented().iterator(); iter.hasNext(); ) {
            final JifMethodInstance mj = (JifMethodInstance) iter.next();

            if (! ts.isAccessible(mj, lc.context())) {
                continue;
            }
            labelCheckOverride(mi, mj, lc);
        }
    }

    /**
     * Label check that mi is allowed to override mj.
     *
     * mj is a Jif method instance that mi is attempting to
     * override. Previous type checks have made sure that things like
     * abstractness, access flags, throw sets, etc. are ok.
     * we need to check that the labels conform.
     * @throws SemanticException
     */
    protected static void labelCheckOverride(final JifMethodInstance mi, final JifMethodInstance mj, LabelChecker lc) throws SemanticException {
        // construct a JifContext here, that equates the arg labels of
        // mi and mj.
        JifContext A = lc.context();
        A = (JifContext) A.pushBlock();
        JifTypeSystem ts = lc.typeSystem();

        if (mi.formalTypes().size() != mj.formalTypes().size()) {
            throw new InternalCompilerError("Different number of arguments!");
        }

        // loop through the args, and equate the arg labels
        JifClassType miContainer = (JifClassType)mi.container().toClass();
        JifClassType mjContainer = (JifClassType)mj.container().toClass();
        A.addAssertionLE(miContainer.thisLabel(), mjContainer.thisLabel());
        A.addAssertionLE(mjContainer.thisLabel(), miContainer.thisLabel());

        Iterator iteri = mi.formalTypes().iterator();
        Iterator iterj = mj.formalTypes().iterator();
        while (iteri.hasNext() && iterj.hasNext() ) {
            Type ti = (Type)iteri.next();
            Type tj = (Type)iterj.next();
            ArgLabel ai = (ArgLabel)ts.labelOfType(ti);
            ArgLabel aj = (ArgLabel)ts.labelOfType(tj);
            A.addAssertionLE(ai, aj);
            A.addAssertionLE(aj, ai);
        }

        LabelChecker newlc = lc.context(A);


        // argument labels are contravariant:
        //      each argument label of mi may be more restrictive than the
        //      correponding argument label in mj
        Iterator miargs = mi.formalTypes().iterator();
        Iterator mjargs = mj.formalTypes().iterator();
	    int c=0;
        while (miargs.hasNext() && mjargs.hasNext()) {
            Type i = (Type)miargs.next();
            Type j = (Type)mjargs.next();
            ArgLabel ai = (ArgLabel)ts.labelOfType(i);
            ArgLabel aj = (ArgLabel)ts.labelOfType(j);
		final int argIndex = ++c;
            newlc.constrain(new LabelConstraint(new NamedLabel("sup_arg_"+argIndex,
                                                               "label of " + StringUtil.nth(argIndex) + " arg of overridden method",
                                                               aj.upperBound()),
                                                LabelConstraint.LEQ,
                                                new NamedLabel("sub_arg_"+argIndex,
                                                               "label of " + StringUtil.nth(argIndex) + " arg of overridding method",
                                                               ai.upperBound()),
                                                A.labelEnv(),
                                                mi.position()) {
                            public String msg() {
                                return "Cannot override " + mj.signature() +
                                       " in " + mj.container() + " with " +
                                       mi.signature() + " in " +
                                       mi.container() + ". The label of the " +
                                       StringUtil.nth(argIndex) + " argument " +
                                       "of the overriding method cannot " +
                                       "be less restrictive than in " +
                                       "the overridden method.";

                            }
                       }
            );
        }


        // start labels are contravariant:
        //    the start label on mi may be more restrictive than the start
        //    label on mj
        NamedLabel starti = new NamedLabel("sub_start_label",
                                           "Start label of method " + mi.name() + " in " + mi.container(),
                                           mi.startLabel());
        NamedLabel startj = new NamedLabel("sup_start_label",
                                           "Start label of method " + mj.name() + " in " + mj.container(),
                                           mj.startLabel());
        newlc.constrain(new LabelConstraint(startj,
                                            LabelConstraint.LEQ,
                                            starti,
                                            A.labelEnv(),
                                            mi.position()) {
                        public String msg() {
                            return "Cannot override " + mj.signature() +
                                   " in " + mj.container() + " with " +
                                   mi.signature() + " in " +
                                   mi.container() + ". The start label of the " +
                                   "overriding method " +
                                   "cannot be less restrictive than in " +
                                   "the overridden method.";

                        }
                        public String detailMsg() {
                            return msg() +
                                " The start label of a method is a lower " +
                                "bound on the observable side effects that " +
                                "the method may perform (such as updates to fields).";

                        }
                   }
        );

        // return labels are covariant
        //      the return label on mi may be less restrictive than the
        //      return label on mj
        NamedLabel reti = new NamedLabel("sub_return_label",
                                         "return label of method " + mi.name() + " in " + mi.container(),
                                         mi.returnLabel());
        NamedLabel retj = new NamedLabel("sup_return_label",
                                         "return label of method " + mj.name() + " in " + mj.container(),
                                         mj.returnLabel());
        newlc.constrain(new LabelConstraint(reti,
                                            LabelConstraint.LEQ,
                                            retj,
                                            A.labelEnv(),
                                            mi.position()) {
                    public String msg() {
                        return "Cannot override " + mj.signature() +
                               " in " + mj.container() + " with " +
                               mi.signature() + " in " +
                               mi.container() + ". The return label of the " +
                               "overriding method " +
                               "cannot be more restrictive than in " +
                               "the overridden method.";

                    }
                    public String detailMsg() {
                        return msg() +
                            " The return label of a method is an upper " +
                            "bound on the information that can be gained " +
                            "by observing that the method terminates normally.";

                    }
                   }
        );


        // return value labels are covariant
        //      the return value label on mi may be less restrictive than the
        //      return value label on mj
        NamedLabel retVali = new NamedLabel("sub_return_val_label",
                               "label of the return value of method " + mi.name() + " in " + mi.container(),
                               mi.returnValueLabel());
        NamedLabel retValj = new NamedLabel("sup_return_val_label",
                               "label of the return value of method " + mj.name() + " in " + mj.container(),
                               mj.returnValueLabel());
        newlc.constrain(new LabelConstraint(retVali,
                                            LabelConstraint.LEQ,
                                            retValj,
                                            A.labelEnv(),
                                            mi.position()) {
                    public String msg() {
                        return "Cannot override " + mj.signature() +
                               " in " + mj.container() + " with " +
                               mi.signature() + " in " +
                               mi.container() + ". The return value label of the " +
                               "overriding method " +
                               "cannot be more restrictive than in " +
                               "the overridden method.";

                    }
                    public String detailMsg() {
                        return msg() +
                            " The return value label of a method is the " +
                            "label of the value returned by the method.";

                    }
                   }
        );

        // exception labels are covariant
        //          the label of an exception E on mi may be less restrictive
        //          than the label of any exception E' on mj, where E<=E'
        Iterator miExc = mi.throwTypes().iterator();
        List mjExc = mj.throwTypes();

        while (miExc.hasNext()) {
            final LabeledType exi = (LabeledType)miExc.next();

            // find the corresponding exception(s) in mhExc
            for (Iterator mjExcIt = mjExc.iterator(); mjExcIt.hasNext(); ) {
                final LabeledType exj = (LabeledType)mjExcIt.next();
                if (ts.isSubtype(exi.typePart(), exj.typePart())) {
                    newlc.constrain(new LabelConstraint(new NamedLabel("exc_label_"+exi.typePart().toString(),
                                                                       "",//"label on the exception " + exi.typePart().toString(),
                                                                       exi.labelPart()),
                                                        LabelConstraint.LEQ,
                                                        new NamedLabel("exc_label_"+exi.typePart().toString(),
                                                                       "",
                                                                       exj.labelPart()),
                                                        A.labelEnv(),
                                                        mi.position()) {
                                public String msg() {
                                    return "Cannot override " + mj.signature() +
                                           " in " + mj.container() + " with " +
                                           mi.signature() + " in " +
                                           mi.container() + ". The label of the " +
                                           exi.typePart().toString() +
                                           "exception in overriding method " +
                                           "cannot be more restrictive " +
                                           "than the label of the " +
                                           exj.typePart().toString() +
                                           "exception in " +
                                           "the overridden method.";

                                }
                                public String detailMsg() {
                                    return "Cannot override " + mj.signature() +
                                    " in " + mj.container() + " with " +
                                    mi.signature() + " in " +
                                    mi.container() + ". If the exception " +
                                    exi.typePart().toString() + " is thrown " +
                                    "by " + mi.signature() + " in " +
                                    mi.container() + " then more information " +
                                    "may be revealed than is permitted by " +
                                    "the overridden method throwing " +
                                    "the exception " +
                                    exj.typePart().toString() + ".";

                                }
                               }
                    );
                }
            }
        }

    }
}
