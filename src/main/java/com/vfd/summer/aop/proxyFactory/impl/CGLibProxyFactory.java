package com.vfd.summer.aop.proxyFactory.impl;

import com.vfd.summer.aop.bean.ProxyMethod;
import com.vfd.summer.aop.proxyFactory.ProxyFactory;
import net.sf.cglib.proxy.Enhancer;
import net.sf.cglib.proxy.MethodInterceptor;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * @PackageName: com.vfd.summer.aop.proxyFactory.impl
 * @ClassName: CGLibProxyFactory
 * @Description: 使用CGLib来进行动态代理，主要是针对某些未实现接口的类来进行，CGLib由于采用子类继承父类的方法
 *               避免了而不需要实现接口，所以可以直接用父类的引用来接受代理对象（子类）
 * @author: vfdxvffd
 * @date: 2021/4/10 下午5:07
 */
public class CGLibProxyFactory implements ProxyFactory {

    private final Object realObj;

    public CGLibProxyFactory(Object realObj) {
        this.realObj = realObj;
    }

    @SuppressWarnings("all")
    @Override
    public Object getProxyInstance(Map<Method, ProxyMethod> method2ProxyMethod) {
            Enhancer enhancer = new Enhancer();
            enhancer.setSuperclass(realObj.getClass());
            enhancer.setCallback((MethodInterceptor) (o, method, args, methodProxy) -> {
            Object result = null;
            ProxyMethod proxyMethod = method2ProxyMethod.get(method);
            if (proxyMethod != null) {
                try {
                    invokeMethods(proxyMethod.getBeforeObject(), proxyMethod.getBeforeMethods(), method, args, null, null);
                    result = methodProxy.invoke(realObj, args);
                    invokeMethods(proxyMethod.getReturningObject(), proxyMethod.getReturningMethods(), method, args, null, result);
                } catch (Throwable throwable) {
                    if (proxyMethod.getThrowingMethods().size() == 0)
                        throw throwable;
                    else
                        invokeMethods(proxyMethod.getThrowingObject(), proxyMethod.getThrowingMethods(), method, args, throwable, null);
                } finally {
                    invokeMethods(proxyMethod.getAfterObject(), proxyMethod.getAfterMethods(), method, args, null, null);
                }
                return result;
            } else {
                return methodProxy.invoke(realObj , args);
            }
        });
        return enhancer.create();
    }
}
