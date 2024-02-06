
package org.hotswap.agent.plugin.proxy;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;


@Retention(RetentionPolicy.CLASS)
public @interface FieldRedefinitionPolicy {

    RedefinitionPolicy value();
}
