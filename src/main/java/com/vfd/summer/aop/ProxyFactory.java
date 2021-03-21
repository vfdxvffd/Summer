package com.vfd.summer.aop;

import com.vfd.summer.aop.bean.JoinPoint;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.lang.reflect.Proxy;
import java.util.*;

/**
 * @PackageName: com.vfd.summer.aop
 * @ClassName: ProxyFactory
 * @Description: 代理工厂，生产代理对象，根据切面方法执行的时机生产代理对象
 * @author: vfdxvffd
 * @date: 2021/3/18 上午10:52
 */
public class ProxyFactory {

    private final Object realObj;

    public ProxyFactory(Object realObj) {
        this.realObj = realObj;
    }

    /**
     *
     * @param methodBeProxy 被代理的方法
     * @return
     */
    public Object getProxyInstance(Method methodBeProxy,
                                   List<Method> before, List<Object> beforeAspect,
                                   List<Method> after, List<Object> afterAspect,
                                   List<Method> afterThrowing, List<Object> throwingAspect,
                                   List<Method> afterReturning, List<Object> returningAspect) {
        return Proxy.newProxyInstance(realObj.getClass().getClassLoader(),
                realObj.getClass().getInterfaces(),
                (proxy, method, args) -> {
                    if (methodBeProxy.getName().equals(method.getName()) &&
                            Arrays.equals(methodBeProxy.getParameterTypes(), method.getParameterTypes())) {
                        Object result = null;
                        try {
                            invokeMethods(beforeAspect, before, method, args, null, null);
                            result = method.invoke(realObj, args);
                            invokeMethods(returningAspect, afterReturning, method, args, null, result);
                        } catch (Throwable throwable) {
                            if (afterThrowing.size() == 0)
                                throw throwable;
                            else
                                invokeMethods(throwingAspect, afterThrowing, method, args, throwable, null);
                        } finally {
                            invokeMethods(afterAspect, after, method, args, null, null);
                        }
                        return result;
                    } else {
                        return method.invoke(realObj, args);
                    }
                });
    }

    private void invokeMethods(List<Object> aspect, List<Method> methods, Method realMethod,
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

//    public Object getProxyInstanceBefore(Method proxyMethod, Object aspect, Method beforeMethod, Object[] beforeArgs) {
//        return Proxy.newProxyInstance(realObj.getClass().getClassLoader(),
//                realObj.getClass().getInterfaces(),
//                (proxy, method, args) -> {
//                    if (proxyMethod.getName().equals(method.getName()) &&
//                            Arrays.equals(proxyMethod.getParameterTypes(), method.getParameterTypes()) &&
//                            beforeMethod != null) {
//                        beforeMethod.invoke(aspect, beforeArgs);
//                    }
//                    return method.invoke(realObj, args);
//                });
//    }
//
//    public Object getProxyInstanceAfter(Method proxyMethod, Object aspect, Method afterMethod, Object[] afterArgs) {
//        return Proxy.newProxyInstance(realObj.getClass().getClassLoader(),
//                realObj.getClass().getInterfaces(),
//                (proxy, method, args) -> {
//                    Object result = method.invoke(realObj, args);
//                    if (proxyMethod.getName().equals(method.getName()) &&
//                            Arrays.equals(proxyMethod.getParameterTypes(), method.getParameterTypes()) &&
//                            afterMethod != null) {
//                        afterMethod.invoke(aspect, afterArgs);
//                    }
//                    return result;
//                });
//    }
//
//    public Object getProxyInstanceAfterThrowing(Method proxyMethod, Object aspect, Method afterMethod, Object[] afterArgs) {
//        return Proxy.newProxyInstance(realObj.getClass().getClassLoader(),
//                realObj.getClass().getInterfaces(),
//                (proxy, method, args) -> {
//                    Object result = null;
//                    try {
//                        result = method.invoke(realObj, args);
//                    } catch (Exception e) {
//                        if (proxyMethod.getName().equals(method.getName()) &&
//                                Arrays.equals(proxyMethod.getParameterTypes(), method.getParameterTypes()) &&
//                                afterMethod != null) {
//                            afterMethod.invoke(aspect, afterArgs);
//                        }
//                        e.printStackTrace();
//                    }
//                    return result;
//                });
//    }
}
