package jif.extension;

import jif.types.JifFieldInstance;
import jif.types.JifTypeSystem;
import jif.types.SemanticDetailedException;
import jif.types.label.Label;
import jif.visit.ConstChecker;
import polyglot.ast.Expr;
import polyglot.ast.FieldDecl;
import polyglot.ast.Node;
import polyglot.types.ArrayType;
import polyglot.types.FieldInstance;
import polyglot.types.SemanticException;
import polyglot.types.Type;
import polyglot.util.Position;
import polyglot.util.SerialVersionUID;
import polyglot.visit.AmbiguityRemover;
import polyglot.visit.TypeBuilder;
import polyglot.visit.TypeChecker;

/** The Jif extension of the <code>FieldDecl</code> node.
 * 
 *  @see polyglot.ast.FieldDecl
 */
public class JifFieldDeclDel extends JifDel_c {
    private static final long serialVersionUID = SerialVersionUID.generate();

    public JifFieldDeclDel() {
    }

    @Override
    public Node buildTypes(TypeBuilder tb) throws SemanticException {
        // Set the flag JifFieldInstance.hasInitializer correctly.
        FieldDecl fd = (FieldDecl) super.buildTypes(tb);
        JifFieldInstance jfi = (JifFieldInstance) fd.fieldInstance();
        JifTypeSystem ts = (JifTypeSystem) tb.typeSystem();
        if (jfi == null) {
            // the only reason the jfi would be null is if
            // something has gone horribly wrong. Just give up now...
            return fd;
        }
        Expr init = fd.init();
        jfi.setHasInitializer(init != null);

        jfi.setLabel(ts.freshLabelVariable(fd.position(), fd.name(),
                "label of the field " + tb.currentClass().name() + "."
                        + fd.name()));
        return fd;
    }

    @Override
    public Node disambiguate(AmbiguityRemover ar) throws SemanticException {
        JifTypeSystem jts = (JifTypeSystem) ar.typeSystem();

        FieldDecl n = (FieldDecl) node();
        FieldInstance fi = n.fieldInstance();

        if (n.declType().isCanonical()) {
            if (!jts.isLabeled(n.declType())) {
                Type lblType = n.declType();
                Position pos = lblType.position();
                Label defaultLbl = jts.defaultSignature().defaultFieldLabel(n);

                lblType = jts.labeledType(pos, lblType, defaultLbl);
                n = n.type(n.type().type(lblType));
            }

            fi.setType(n.declType());
        }

        return n;
    }

    @Override
    public Node typeCheck(TypeChecker tc) throws SemanticException {
        FieldDecl fd = (FieldDecl) node();
        if (fd.name().indexOf('$') >= 0 && !((JifTypeSystem) tc.typeSystem())
                .isMarkerFieldName(fd.name())) {
            // check that the field isn't one of the special marker fields
            throw new SemanticException(
                    "Field names can not contain the character '$'.");
        }

        if (fd.fieldInstance().flags().isStatic()) {
            Expr init = fd.init();

            if (init != null) {
                // if the static field has an initializer, then the
                // initialization expression must be constant.
                ConstChecker cc = new ConstChecker(tc.context().currentClass());
                init.visit(cc);
                if (!cc.isConst()) {
                    throw new SemanticDetailedException(
                            "Jif does not support "
                                    + "static fields without constant initializers.",
                            "The initializer of a static field of a class is "
                                    + "executed when the class is loaded. Information may be "
                                    + "leaked if the time of class loading is observable. "
                                    + "To prevent this covert channel, Jif requires static "
                                    + "field initializers to be constant.",
                            fd.position());
                }
            }
        }

        // if the declared type is an array type, make sure it is the same all the way through
        if (fd.fieldInstance().type().isArray()) {
            JifTypeSystem jts = (JifTypeSystem) tc.typeSystem();
            ArrayType at = jts.unlabel(fd.fieldInstance().type()).toArray();
            JifLocalDeclDel.checkArrayTypeConsistency(at);
        }
        return super.typeCheck(tc);
    }
}
