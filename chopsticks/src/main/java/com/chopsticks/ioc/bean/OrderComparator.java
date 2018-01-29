package com.chopsticks.ioc.bean;

import java.io.Serializable;
import java.util.Comparator;

import com.chopsticks.ioc.annotation.Order;

/**
 * Bean order by
 *
 *         2017/6/2
 */
public class OrderComparator<T> implements Serializable, Comparator<T> {

    @Override
    public int compare(T e1, T e2) {
        Order o1 = e1.getClass().getAnnotation(Order.class);
        Order o2 = e2.getClass().getAnnotation(Order.class);
        Integer order1 = null != o1 ? o1.value() : Integer.MAX_VALUE;
        Integer order2 = null != o2 ? o2.value() : Integer.MAX_VALUE;
        return order1.compareTo(order2);
    }

}
