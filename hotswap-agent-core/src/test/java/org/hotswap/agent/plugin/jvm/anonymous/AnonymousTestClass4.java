
package org.hotswap.agent.plugin.jvm.anonymous;


public class AnonymousTestClass4 {
    public String enclosing1() {
        AnonymousTestInterface2 interface2 = new AnonymousTestInterface2() {
            @Override
            public String test2() {
                return "enclosing2: AnonymousTestClass1.AnonymousTestInterface1.test2()";
            }
        };
        return interface2.test2();
    }
}
