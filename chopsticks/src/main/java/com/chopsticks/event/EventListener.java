
package com.chopsticks.event;

/**
 * EventListener
 *
 * @date 2017/9/18
 */
@FunctionalInterface
public interface EventListener<T> {

    /**
     * Start event
     *
     * @param e Event instance
     */
    void trigger(Event<T> e);

}