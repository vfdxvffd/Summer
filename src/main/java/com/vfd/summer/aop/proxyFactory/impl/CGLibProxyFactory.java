package com.vfd.summer.aop.proxyFactory.impl;

import com.vfd.summer.aop.proxyFactory.ProxyFactory;
import net.sf.cglib.proxy.Enhancer;
import net.sf.cglib.proxy.MethodInterceptor;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;

/**
 * @PackageName: com.vfd.summer.aop.proxyFactory.impl
 * @ClassName: CGLibProxyFactory
 * @Description: 使用CGLib来进行动态代理，主要是针对某些未实现接口的类来进行，CGLib由于采用子类继承父类的方法
 *               避免了而不需要实现接口，所以可以直接用父类的引用来接受代理对象（子类）
 * @author: vfdxvffd
 * @date: 2021/4/10 下午5:07
 */
public class CGLibProxyFactory implements ProxyFactory {

    private final Class<?> clazz;

    public CGLibProxyFactory(Class<?> clazz) {
        this.clazz = clazz;
    }

    @Override
    public Object getProxyInstance(Method methodBeProxy,
                                   List<Method> before, List<Object> beforeAspect,
                                   List<Method> after, List<Object> afterAspect,
                                   List<Method> afterThrowing, List<Object> throwingAspect,
                                   List<Method> afterReturning, List<Object> returningAspect) {
        Enhancer enhancer = new Enhancer();
        enhancer.setSuperclass(clazz);
        enhancer.setCallback((MethodInterceptor) (o, method, args, methodProxy) -> {
            Object result = null;
            if (methodBeProxy.getName().equals(method.getName()) &&
                    Arrays.equals(methodBeProxy.getParameterTypes(), method.getParameterTypes())) {
                try {
                    invokeMethods(beforeAspect, before, method, args, null, null);
                    result = methodProxy.invokeSuper(o, args);
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
                result = methodProxy.invokeSuper(o , args);
            }
            return result;
        });
        return enhancer.create();
    }
}
