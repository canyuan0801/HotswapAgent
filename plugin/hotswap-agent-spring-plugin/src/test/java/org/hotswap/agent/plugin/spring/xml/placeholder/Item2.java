
package org.hotswap.agent.plugin.spring.xml.placeholder;

import org.springframework.beans.factory.annotation.Value;

public class Item2 {
    @Value("${item.name}")
    private String name;

    private String name2;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getName2() {
        return name2;
    }

    public void setName2(String name2) {
        this.name2 = name2;
    }
}
