
package org.hotswap.agent.util.scanner;

import java.io.IOException;


public interface Scanner {

    void scan(ClassLoader classLoader, String path, ScannerVisitor visitor) throws IOException;
}
