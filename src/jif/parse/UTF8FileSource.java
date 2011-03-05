package jif.parse;

import java.io.*;

import jif.types.CodeSource;
import jif.types.label.Label;
import polyglot.frontend.FileSource;

public class UTF8FileSource extends FileSource implements CodeSource {
    protected Label provider;

	public UTF8FileSource(File f, boolean userSpecified, Label provider)
	throws IOException 
    {
	super(f, userSpecified);
	this.provider = provider;
    }

    @Override
    protected Reader createReader(InputStream str) {
      try {
	return new polyglot.lex.EscapedUnicodeReader(
	    new InputStreamReader(str, "UTF-8"));
      } catch (UnsupportedEncodingException e) {
	System.err.println("Bad Java implementation: UTF-8 encoding must be supported");
	return null;
      }
    }

    @Override
    public Label provider() {
        return provider;
    }
}
