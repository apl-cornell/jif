package jif.parse;

import java.io.*;

import jif.types.CodeSource;
import jif.types.principal.Principal;
import polyglot.frontend.FileSource;

public class UTF8FileSource extends FileSource implements CodeSource {
    protected Principal provider;

	public UTF8FileSource(File f, boolean userSpecified, Principal provider)
	throws IOException 
    {
	super(f, userSpecified);
	this.provider = provider;
    }

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
	public Principal provider() {
		return provider;
	}
}
