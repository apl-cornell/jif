package jif.visit;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import jif.ExtensionInfo;
import jif.JifScheduler;
import jif.ast.JifUtil;
import jif.extension.JifFieldDeclExt;
import jif.types.JifClassType;
import jif.types.JifFieldInstance;
import jif.types.JifSubstType;
import jif.types.JifTypeSystem;
import jif.types.LabelSubstitution;
import jif.types.Solver;
import jif.types.VarMap;
import jif.types.label.AccessPath;
import jif.types.label.AccessPathField;
import jif.types.label.Label;
import jif.types.label.VarLabel;
import polyglot.ast.ClassBody;
import polyglot.ast.ClassDecl;
import polyglot.ast.ClassMember;
import polyglot.ast.Field;
import polyglot.ast.FieldDecl;
import polyglot.ast.Node;
import polyglot.ast.NodeFactory;
import polyglot.frontend.CyclicDependencyException;
import polyglot.frontend.Job;
import polyglot.frontend.MissingDependencyException;
import polyglot.frontend.goals.Goal;
import polyglot.types.FieldInstance;
import polyglot.types.ParsedClassType;
import polyglot.types.ParsedTypeObject;
import polyglot.types.SemanticException;
import polyglot.types.Type;
import polyglot.util.InternalCompilerError;
import polyglot.visit.ContextVisitor;
import polyglot.visit.NodeVisitor;

/** A visitor used to resolving field labels. We want to resolve
 *  field labels of all the classes before the label checking pass,
 *  because these field labels might be included in other labels, and 
 *  thus need to be resolved first.
 *  
 *  This visitor also adds dependencies so that the label checking pass for
 *  this job will not run until the FieldLabelResolver for all jobs it is 
 *  dependent on has run.
 */
public class FieldLabelResolver extends ContextVisitor {
    @SuppressWarnings("hiding")
    private final JifTypeSystem ts;
    private VarMap bounds;
    private Map<Label, Label> fieldVarBounds;

    public FieldLabelResolver(Job job, JifTypeSystem ts, NodeFactory nf) {
        super(job, ts, nf);
        this.job = job;
        this.ts = ts;
    }

    @Override
    public NodeVisitor enterCall(Node n) throws SemanticException {
        if (n instanceof ClassDecl) {
            this.fieldVarBounds = new HashMap<Label, Label>();
        }

        if (n instanceof ClassBody) {
            // labelcheck the class body
            ClassBody d = (ClassBody) n;
            labelCheckClassBody(d);
        }

        if (n instanceof Field) {
            // this class uses field f. Make sure that the field label resolver
            // pass for the container of f is run before we run label checking.
            FieldInstance fi = ((Field) n).fieldInstance();

// Jif Dependency bugfix
            if (fi == null) {
                Field f = (Field) n;
                JifScheduler sched =
                        (JifScheduler) this.job().extensionInfo().scheduler();
                Type tp = ts.unlabel(f.target().type());
                Job next;
                if (tp instanceof ParsedClassType) {
                    ParsedClassType pct = (ParsedClassType) tp;
                    next = pct.job();
                } else {
                    next = this.job();
                }
                Goal g = sched.TypeChecked(next);
                throw new MissingDependencyException(g);
            }

            JifScheduler scheduler =
                    (JifScheduler) typeSystem().extensionInfo().scheduler();

            Type ct = fi.container();
            while (ct instanceof JifSubstType) {
                ct = ((JifSubstType) ct).base();
            }

            if (ct instanceof ParsedClassType) {
                // the container of fi is a class that is being compiled in
                // this compiler execution
                ParsedTypeObject pct = (ParsedClassType) ct;
                if (pct.job() != null && pct.job() != this.job) {
                    // add a dependency from the label checking of this.job to the
                    // field label inference of pct
                    try {
                        scheduler.addPrerequisiteDependency(
                                scheduler.LabelsDoubleChecked(this.job),
                                scheduler.FieldLabelInference(pct.job()));
                    } catch (CyclicDependencyException e) {
                        throw new InternalCompilerError(e);
                    }
                }
            }
        }

        return this;
    }

    private void labelCheckClassBody(ClassBody d) throws SemanticException {
        JifClassType ct = (JifClassType) context().currentClassScope();

        LabelChecker lc = ((ExtensionInfo) ct.typeSystem().extensionInfo())
                .createLabelChecker(job, true, false, false, false);

        if (lc == null) {
            throw new InternalCompilerError("Could not label check " + ct + ".",
                    d.position());
        }

        List<ClassMember> members = d.members();
        for (ClassMember m : members) {
            if (m instanceof FieldDecl) {
                JifFieldDeclExt ext = (JifFieldDeclExt) JifUtil.jifExt(m);
                ext.labelCheckField(lc, ct);
            }
        }

        Solver solver = lc.solver();
        this.bounds = solver.solve();
    }

    /**
     * @throws SemanticException  
     */
    @Override
    public Node leaveCall(Node old, Node n, NodeVisitor v)
            throws SemanticException {
        if (n instanceof FieldDecl) {
            FieldDecl f = (FieldDecl) n;
            JifFieldInstance fi = (JifFieldInstance) f.fieldInstance();
            if (fi.label() instanceof VarLabel) {
                this.fieldVarBounds.put(fi.label(),
                        bounds.boundOf((VarLabel) fi.label()));
            }

            Type lbledType = ts.labeledType(f.declType().position(),
                    ts.unlabel(f.declType()), fi.label());
            return f.type(f.type().type(lbledType));

        }

        if (n instanceof ClassBody) {
            // need to go through the entire class body and replace the 
            // variables that we have now solved for.
            LabelSubstitutionVisitor lsv = new LabelSubstitutionVisitor(
                    new FieldVarLabelSubst(this.fieldVarBounds), false);
            n = n.del().visitChildren(lsv);

        }
        return n;
    }

    private static class FieldVarLabelSubst extends LabelSubstitution {
        Map<Label, Label> map;

        public FieldVarLabelSubst(Map<Label, Label> fieldVarBounds) {
            map = fieldVarBounds;
        }

        @Override
        public Label substLabel(Label L) {
            Label b = map.get(L);
            if (b != null) {
                return b;
            }
            return L;
        }

        @Override
        public AccessPath substAccessPath(AccessPath ap)
                throws SemanticException {
            ap = super.substAccessPath(ap);
            if (ap instanceof AccessPathField) {
                // Also perform substitution within the access path's field
                // instance.
                AccessPathField apf = (AccessPathField) ap;
                JifFieldInstance fi = (JifFieldInstance) apf.fieldInstance();
                fi.setLabel(substLabel(fi.label()));
            }
            return ap;
        }
    }
}
