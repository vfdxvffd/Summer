package com.vfd.summer.extension;

import com.vfd.summer.applicationContext.impl.SummerAnnotationConfigApplicationContext;
import com.vfd.summer.ioc.bean.BeanDefinition;

import java.lang.reflect.Method;
import java.util.List;

/**
 * @PackageName: com.vfd.summer.extension
 * @ClassName: Extension
 * @Description: 为此框架的扩展向外提供一个接口
 * @author: vfdxvffd
 * @date: 2021/5/12 下午1:28
 */
public interface Extension {

    void doOperation0 (SummerAnnotationConfigApplicationContext context) throws Exception;

    void doOperation1 (SummerAnnotationConfigApplicationContext context) throws Exception;

    void doOperation2 (SummerAnnotationConfigApplicationContext context) throws Exception;

    void doOperation3 (SummerAnnotationConfigApplicationContext context) throws Exception;

    void doOperation4 (SummerAnnotationConfigApplicationContext context) throws Exception;

    void doOperation5 (SummerAnnotationConfigApplicationContext context, BeanDefinition beanDefinition) throws Exception;

    void doOperation6 (SummerAnnotationConfigApplicationContext context, Object o) throws Exception;

    void doOperation7 (SummerAnnotationConfigApplicationContext context, Object o) throws Exception;

    void doOperation8 (SummerAnnotationConfigApplicationContext context, Object o) throws Exception;

    // v1.1 更新内容，此操作对应于@Configuration注解处理之后的操作
    void doOperation9 (SummerAnnotationConfigApplicationContext context) throws Exception;

    // v1.2 更新内容，此操作在代理对象时候，操作四种方法（前置、后置、返回、抛异常）
    void doOperationWhenProxy (SummerAnnotationConfigApplicationContext context, Method methodBeProxy,
                                List<Method> before, List<Object> beforeAspect,
                                List<Method> after, List<Object> afterAspect,
                                List<Method> afterThrowing, List<Object> throwingAspect,
                                List<Method> afterReturning, List<Object> returningAspect) throws Exception;
}
