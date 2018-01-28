package com.chopsticks.kit;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import org.omg.CORBA.Environment;

import com.chopsticks.mvc.http.HttpMethod;

import lombok.NoArgsConstructor;
import lombok.Value;

/**
 * Blade kit
 *
 * @author biezhi
 * 2017/5/31
 */
@NoArgsConstructor
public class ChopsticksKit {

    /**
     * Get @Inject Annotated field
     *
     * @param ioc         ioc container
     * @param classDefine classDefine
     * @return return FieldInjector
     */
    private static List<FieldInjector> getInjectFields(Ioc ioc, ClassDefine classDefine) {
        List<FieldInjector> injectors = new ArrayList<>(8);
        for (Field field : classDefine.getDeclaredFields()) {
            if (null != field.getAnnotation(InjectWith.class) || null != field.getAnnotation(Inject.class)) {
                injectors.add(new FieldInjector(ioc, field));
            }
        }
        if (injectors.size() == 0) {
            return new ArrayList<>();
        }
        return injectors;
    }

    /**
     * Get @Value Annotated field
     * @param environment
     * @param classDefine
     * @return
     */
    private static List<ValueInjector> getValueInjectFields(Environment environment,ClassDefine classDefine) {
        List<ValueInjector> valueInjectors = new ArrayList<>(8);
        //handle class annotation
        if (null != classDefine.getType().getAnnotation(Value.class)) {
            String suffix = classDefine.getType().getAnnotation(Value.class).name();
            Arrays.stream(classDefine.getDeclaredFields()).forEach(field -> valueInjectors.add(
                    new ValueInjector(environment,field,suffix+"."+field.getName())
            ));
        }else {
            Arrays.stream(classDefine.getDeclaredFields()).
                    filter(field -> null != field.getAnnotation(Value.class)).
                    map(field -> new ValueInjector(
                                    environment,field,field.getAnnotation(Value.class).name())
                    ).forEach(valueInjectors::add);
        }
        return valueInjectors;
    }
    public static void injection(Ioc ioc, BeanDefine beanDefine) {
        ClassDefine         classDefine    = ClassDefine.create(beanDefine.getType());
        List<FieldInjector> fieldInjectors = getInjectFields(ioc, classDefine);
        Object              bean           = beanDefine.getBean();
        for (FieldInjector fieldInjector : fieldInjectors) {
            fieldInjector.injection(bean);
        }
    }
    public static void injectionValue(Environment environment,BeanDefine beanDefine) {
        ClassDefine         classDefine    = ClassDefine.create(beanDefine.getType());
        List<ValueInjector> valueFileds    = getValueInjectFields(environment,classDefine);
        Object              bean           = beanDefine.getBean();
        valueFileds.stream().forEach(fieldInjector -> fieldInjector.injection(bean));
    }
    public static boolean isEmpty(Collection<?> c) {
        return null == c || c.isEmpty();
    }

    public static <T> boolean isEmpty(T[] arr) {
        return null == arr || arr.length == 0;
    }

    public static boolean isNotEmpty(Collection<?> c) {
        return null != c && !c.isEmpty();
    }

    public static boolean isWebHook(HttpMethod httpMethod) {
        return httpMethod == HttpMethod.BEFORE || httpMethod == HttpMethod.AFTER;
    }

    public static boolean notIsWebHook(HttpMethod httpMethod) {
        return !isWebHook(httpMethod);
    }

    public static boolean epollIsAvailable() {
        try {
            Object obj = Class.forName("io.netty.channel.epoll.Epoll").getMethod("isAvailable").invoke(null);
            return null != obj && Boolean.valueOf(obj.toString()) && System.getProperty("os.name").toLowerCase().contains("linux");
        } catch (Exception e) {
            return false;
        }
    }

}
