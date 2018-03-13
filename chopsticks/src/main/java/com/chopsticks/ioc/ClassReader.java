
package com.chopsticks.ioc;

import java.util.Set;

import com.chopsticks.ioc.bean.ClassInfo;
import com.chopsticks.ioc.bean.Scanner;

/**
 * 一个类读取器的接口
 *
 * @since 1.0
 */
public interface ClassReader {

    Set<ClassInfo> readClasses(Scanner scanner);

}