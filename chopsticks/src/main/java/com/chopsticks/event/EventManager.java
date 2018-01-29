
package com.chopsticks.event;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.chopsticks.ioc.bean.OrderComparator;

/**
 * Event manager
 *
 * @author biezhi
 * @date 2017/9/18
 */
public class EventManager {

    private Map<EventType, List<EventListener>> listenerMap = null;
    private OrderComparator<EventListener>      comparator  = new OrderComparator<>();

    public EventManager() {
        this.listenerMap = Stream.of(EventType.values()).collect(Collectors.toMap(v -> v, v -> new LinkedList<>()));
    }

    public <T> void addEventListener(EventType type, EventListener<T> listener) {
        listenerMap.get(type).add(listener);
    }

    public <T> void fireEvent(EventType type, T data) {
        listenerMap.get(type).stream()
                .sorted(comparator)
                .forEach(listener -> listener.trigger(new Event<>(type, data)));
    }

}