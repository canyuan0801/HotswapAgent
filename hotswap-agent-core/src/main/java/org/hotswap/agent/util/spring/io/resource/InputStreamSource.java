

package org.hotswap.agent.util.spring.io.resource;

import java.io.IOException;
import java.io.InputStream;


public interface InputStreamSource {


    InputStream getInputStream() throws IOException;

}