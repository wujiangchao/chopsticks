package com.chopsticks.ioc;
/**
 * Bean Injector interface
 *
 * @author jack
 *
 */
public interface Injector {
    /**
     * Injection bean
     *
     * @param bean bean instance
     */
    void injection(Object bean);
}
