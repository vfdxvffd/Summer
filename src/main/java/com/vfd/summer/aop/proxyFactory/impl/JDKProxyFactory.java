package com.vfd.summer.aop.proxyFactory.impl;

import com.vfd.summer.aop.bean.ProxyMethod;
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
    @SuppressWarnings("all")
    @Override
    public Object getProxyInstance(Map<Method, ProxyMethod> method2ProxyMethod) {
        return Proxy.newProxyInstance(realObj.getClass().getClassLoader(),
                realObj.getClass().getInterfaces(),
                (proxy, method, args) -> {
                    Object result = null;
                    ProxyMethod proxyMethod = method2ProxyMethod.get(method);
                    if (proxyMethod != null) {
                        try {
                            invokeMethods(proxyMethod.getBeforeObject(), proxyMethod.getBeforeMethods(), method, args, null, null);
                            result = method.invoke(realObj, args);
                            invokeMethods(proxyMethod.getReturningObject(), proxyMethod.getReturningMethods(), method, args, null, result);
                        } catch (Throwable throwable) {
                            if (proxyMethod.getThrowingMethods().size() == 0)
                                throw throwable;
                            else
                                invokeMethods(proxyMethod.getThrowingObject(), proxyMethod.getThrowingMethods(), method, args, throwable, null);
                        } finally {
                            invokeMethods(proxyMethod.getAfterObject(), proxyMethod.getAfterMethods(), method, args, null, null);
                        }
                    } else {
                        result = method.invoke(realObj, args);
                    }
                    return result;
                });
    }
}
