

package org.hotswap.agent.util.spring.io.resource;

import java.io.IOException;


@SuppressWarnings("serial")
public class NestedIOException extends IOException {

    static {




        NestedExceptionUtils.class.getName();
    }


    public NestedIOException(String msg) {
        super(msg);
    }


    public NestedIOException(String msg, Throwable cause) {
        super(msg);
        initCause(cause);
    }


    @Override
    public String getMessage() {
        return NestedExceptionUtils.buildMessage(super.getMessage(), getCause());
    }

}