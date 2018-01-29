
package com.chopsticks.event;

import com.chopsticks.Chopsticks;
import com.chopsticks.mvc.WebContext;

/**
 * Event
 *
 * @date 2017/9/18
 */
public class Event<T> {

    public EventType eventType;
    private T data;

    public Event(EventType eventType, T data) {
        this.eventType = eventType;
        this.data = data;
    }

    public Chopsticks blade(){
        return WebContext.chopsticks();
    }

    public T data(){
        return this.data;
    }
}