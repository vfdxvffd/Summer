package com.vfd.summer.aop.proxyFactory.impl;

import com.vfd.summer.aop.proxyFactory.ProxyFactory;

import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.*;

/**
 * @PackageName: com.vfd.summer.aop
 * @ClassName: ProxyFactory
 * @Description: JDK默认的动态代理，需要实现接口，故可以使用接口类型的引用来接受代理对象（实现类）
 * @author: vfdxvffd
 * @date: 2021/3/18 上午10:52
 */
public class JDKProxyFactory implements ProxyFactory {

    private final Object realObj;

    public JDKProxyFactory(Object realObj) {
        this.realObj = realObj;
    }

    /**
     *
     * @param methodBeProxy 被代理的方法
     * @return
     */
    @Override
    public Object getProxyInstance(Method methodBeProxy,
                                   List<Method> before, List<Object> beforeAspect,
                                   List<Method> after, List<Object> afterAspect,
                                   List<Method> afterThrowing, List<Object> throwingAspect,
                                   List<Method> afterReturning, List<Object> returningAspect) {
        return Proxy.newProxyInstance(realObj.getClass().getClassLoader(),
                realObj.getClass().getInterfaces(),
                (proxy, method, args) -> {
                    Object result = null;
                    if (methodBeProxy.getName().equals(method.getName()) &&
                            Arrays.equals(methodBeProxy.getParameterTypes(), method.getParameterTypes())) {
                        // 需要保证当前的方法确实被某个切面类的切面方法横切了
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
                    } else {
                        result = method.invoke(realObj, args);
                    }
                    return result;
                });
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
