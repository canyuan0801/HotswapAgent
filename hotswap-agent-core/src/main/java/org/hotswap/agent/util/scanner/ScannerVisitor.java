
package org.hotswap.agent.util.scanner;

import java.io.IOException;
import java.io.InputStream;


public interface ScannerVisitor {

    public void visit(InputStream file) throws IOException;
}
