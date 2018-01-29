
package com.chopsticks.event;

import com.chopsticks.Chopsticks;

/**
 * Bean processor
 * <p>
 * When the Chopsticks program execution at startup time
 *
 * @date 2017/9/18
 */
@FunctionalInterface
public interface BeanProcessor {

    /**
     * Initialize the ioc container after execution
     *
     */
    void processor(Chopsticks chopsticks);

    /**
     * Initialize the ioc container before execution
     *
     */
    default void preHandle(Chopsticks chopsticks) {
    }

}