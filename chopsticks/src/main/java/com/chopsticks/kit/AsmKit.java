package com.chopsticks.kit;


import org.objectweb.asm.*;

import java.io.IOException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class AsmKit {
	  /**
     * Cached method names
     */
    private static final Map<Method, String[]> METHOD_NAMES_POOL = new ConcurrentHashMap<>(64);

    /**
     * Compare whether the parameter type is consistent
     *
     * @param types   the type of the asm({@link Type})
     * @param classes java type({@link Class})
     * @return return param type equals
     */
    private static boolean sameType(Type[] types, Class<?>[] classes) {
        if (types.length != classes.length) return false;
        for (int i = 0; i < types.length; i++) {
            if (!Type.getType(classes[i]).equals(types[i])) return false;
        }
        return true;
    }

    /**
     * get method param names
     *
     * @param m method
     * @return return method param names
     */
    public static String[] getMethodParamNames(final Method m) throws IOException {
        if (METHOD_NAMES_POOL.containsKey(m)) return METHOD_NAMES_POOL.get(m);

        final String[] paramNames = new String[m.getParameterTypes().length];
        final String   n          = m.getDeclaringClass().getName();
        ClassReader    cr;
        try {
            cr = new ClassReader(n);
        } catch (IOException e) {
            return null;
        }
        cr.accept(new ClassVisitor(Opcodes.ASM5) {
            @Override
            public MethodVisitor visitMethod(final int access, final String name, final String desc, final String signature, final String[] exceptions) {
                final Type[] args = Type.getArgumentTypes(desc);
                // The method name is the same and the number of parameters is the same
                if (!name.equals(m.getName()) || !sameType(args, m.getParameterTypes())) {
                    return super.visitMethod(access, name, desc, signature, exceptions);
                }
                MethodVisitor v = super.visitMethod(access, name, desc, signature, exceptions);
                return new MethodVisitor(Opcodes.ASM5, v) {
                    @Override
                    public void visitLocalVariable(String name, String desc, String signature, Label start, Label end, int index) {
                        int i = index - 1;
                        // if it is a static method, the first is the parameter
                        // if it's not a static method, the first one is "this" and then the parameter of the method
                        if (Modifier.isStatic(m.getModifiers())) {
                            i = index;
                        }
                        if (i >= 0 && i < paramNames.length) {
                            paramNames[i] = name;
                        }
                        super.visitLocalVariable(name, desc, signature, start, end, index);
                    }
                };
            }
        }, 0);
        METHOD_NAMES_POOL.put(m, paramNames);
        return paramNames;
}

}
