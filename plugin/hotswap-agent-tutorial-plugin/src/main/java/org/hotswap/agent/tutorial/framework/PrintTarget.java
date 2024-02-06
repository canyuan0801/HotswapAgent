package org.hotswap.agent.tutorial.framework;


public interface PrintTarget {


    default void print(String content) {
        System.out.println(content);
    }
}
