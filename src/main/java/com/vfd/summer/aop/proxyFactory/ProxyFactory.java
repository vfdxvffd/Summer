package com.vfd.summer.aop.proxyFactory;

import com.vfd.summer.aop.bean.JoinPoint;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.Iterator;
import java.util.List;

/**
 * @PackageName: com.vfd.summer.aop.proxyFactory
 * @ClassName: ProxyFactory
 * @Description: 代理对象产生的工厂，有两种实现类，一种采用JDK默认的动态代理实现，一种采用CGLib来实现动态代理
 * @author: vfdxvffd
 * @date: 2021/4/10 下午5:05
 */
public interface ProxyFactory {

    Object getProxyInstance(Method methodBeProxy,
                            List<Method> before, List<Object> beforeAspect,
                            List<Method> after, List<Object> afterAspect,
                            List<Method> afterThrowing, List<Object> throwingAspect,
                            List<Method> afterReturning, List<Object> returningAspect);

    /**
     * 执行通知方法（即执行切面的方法）
     * @param aspect 切面类的实例化对象，用来执行对应方法
     * @param methods 真正执行的方法
     * @param realMethod 被代理的方法
     * @param realArgs 被代理方法的参数
     * @param t 被代理的方法执行过程中可能出现的异常
     * @param o 被代理的方法执行的返回值
     * @throws IllegalAccessException
     * @throws InvocationTargetException
     */
    default void invokeMethods(List<Object> aspect, List<Method> methods, Method realMethod,
                               Object[] realArgs, Throwable t, Object o) throws IllegalAccessException, InvocationTargetException {
        if (methods != null && methods.size() > 0) {
            Iterator<Method> methodIterator = methods.iterator();
            Iterator<Object> objectIterator = aspect.iterator();
            while (methodIterator.hasNext() && objectIterator.hasNext()) {
                Method method1 = methodIterator.next();
                Object object = objectIterator.next();
                Parameter[] parameters = method1.getParameters();
                Object[] args = new Object[parameters.length];
                for (int i = 0; i < parameters.length; i++) {
                    if (parameters[i].getType().equals(JoinPoint.class)) {
                        args[i] = new JoinPoint(realMethod.getName(), realArgs, realMethod.getReturnType());
                    } else if (parameters[i].getType().equals(Throwable.class)) {
                        args[i] = t;
                    } else if (parameters[i].getType().equals(Object.class)) {
                        args[i] = o;
                    }
                }
                method1.invoke(object, args);
            }
        }
    }
}
