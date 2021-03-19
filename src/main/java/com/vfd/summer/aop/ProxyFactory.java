package aop;

import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.Arrays;

/**
 * @PackageName: aop
 * @ClassName: ProxyFactory
 * @Description:
 * @author: vfdxvffd
 * @date: 2021/3/18 上午10:52
 */
public class ProxyFactory {

    private final Object realObj;

    public ProxyFactory(Object realObj) {
        this.realObj = realObj;
    }

    //Object aspect, Method beforeMethod, Object[] beforeArgs, Method afterMethod, Object[] afterArgs
    public Object getProxyInstanceBefore(Method proxyMethod, Object aspect, Method beforeMethod, Object[] beforeArgs) {
        return Proxy.newProxyInstance(realObj.getClass().getClassLoader(),
                realObj.getClass().getInterfaces(),
                (proxy, method, args) -> {
                    if (proxyMethod.getName().equals(method.getName()) &&
                            Arrays.equals(proxyMethod.getParameterTypes(), method.getParameterTypes()) &&
                            beforeMethod != null) {
                        beforeMethod.invoke(aspect, beforeArgs);
                    }
                    return method.invoke(realObj, args);
                });
    }

    public Object getProxyInstanceAfter(Method proxyMethod, Object aspect, Method afterMethod, Object[] afterArgs) {
        return Proxy.newProxyInstance(realObj.getClass().getClassLoader(),
                realObj.getClass().getInterfaces(),
                (proxy, method, args) -> {
                    Object result = method.invoke(realObj, args);
                    if (proxyMethod.getName().equals(method.getName()) &&
                            Arrays.equals(proxyMethod.getParameterTypes(), method.getParameterTypes()) &&
                            afterMethod != null) {
                        afterMethod.invoke(aspect, afterArgs);
                    }
                    return result;
                });
    }

    public Object getProxyInstanceAfterThrowing(Method proxyMethod, Object aspect, Method afterMethod, Object[] afterArgs) {
        return Proxy.newProxyInstance(realObj.getClass().getClassLoader(),
                realObj.getClass().getInterfaces(),
                (proxy, method, args) -> {
                    Object result = null;
                    try {
                        result = method.invoke(realObj, args);
                    } catch (Exception e) {
                        if (proxyMethod.getName().equals(method.getName()) &&
                                Arrays.equals(proxyMethod.getParameterTypes(), method.getParameterTypes()) &&
                                afterMethod != null) {
                            afterMethod.invoke(aspect, afterArgs);
                        }
                    }
                    return result;
                });
    }
}
