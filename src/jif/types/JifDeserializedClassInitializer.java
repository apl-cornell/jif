package jif.types;

import jif.ExtensionInfo;
import polyglot.types.DeserializedClassInitializer;
import polyglot.types.TypeSystem;
import polyglot.util.InternalCompilerError;

public class JifDeserializedClassInitializer
        extends DeserializedClassInitializer {

    public JifDeserializedClassInitializer(TypeSystem ts) {
        super(ts);
    }

    @Override
    public void initTypeObject() {
        if (this.init) return;
        super.initTypeObject();
        ExtensionInfo extInfo = (ExtensionInfo) ts.extensionInfo();
        if (((JifClassType) ct).isUnsafe()
                && !extInfo.getJifOptions().skipLabelChecking) {
            //XXX: it would be nice to throw a SemanticException or
            //     something else here...
            throw new InternalCompilerError("Cannot load class " + ct.fullName()
                    + ". Label checks are currently enabled, "
                    + "but it was compiled without label checks.");
        }
    }
}
