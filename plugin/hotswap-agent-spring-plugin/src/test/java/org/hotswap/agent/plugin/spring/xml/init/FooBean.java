
package org.hotswap.agent.plugin.spring.xml.init;

import org.springframework.beans.factory.InitializingBean;

public class FooBean implements InitializingBean {
    private int value;
    private static int staticValue;

    @Override
    public void afterPropertiesSet() throws Exception {
        staticValue = value;
    }

    public int getValue() {
        return value;
    }

    public void setValue(int value) {
        this.value = value;
    }

    public static int getStaticValue() {
        return staticValue;
    }
}
