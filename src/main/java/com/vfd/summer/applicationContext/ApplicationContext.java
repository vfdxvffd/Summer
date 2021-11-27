package com.vfd.summer.applicationContext;

import com.vfd.summer.exception.DuplicateBeanClassException;
import com.vfd.summer.exception.NoSuchBeanException;
import com.vfd.summer.ioc.bean.BeanDefinition;

import java.util.Map;

/**
 * @PackageName: com.vfd.summer.ioc.applicationContext
 * @ClassName: ApplicationContext
 * @Description:
 * @author: vfdxvffd
 * @date: 2021/3/14 上午10:59
 */
public interface ApplicationContext {

    /**
     * 根据bean的name获取对象
     * @param beanName
     * @return
     */
    Object getBean(String beanName) throws Exception;

    /**
     * 根据bean的类型获取对象
     * @param beanType
     * @param <T>
     * @return
     */
    <T> T getBean(Class<T> beanType) throws Exception;

    /**
     * 同时根据bean的类型和name获取对象，若一个接口多个实现类，则可以通过接口类型和name去获取
     * @param name
     * @param beanType
     * @param <T>
     * @return
     */
    <T> T getBean(String name, Class<T> beanType) throws Exception;

    /**
     * 根据name获取相应的类型
     * @param name
     * @return
     */
    Class<?> getType(String name) throws NoSuchBeanException;

    /**
     * 根据类型获取该类型对应的所有bean,比如一个接口和它的所有实现类，用map返回<beanName, Object>
     * @param beanType
     * @param <T>
     * @return
     */
    <T> Map<String, T> getBeansOfType(Class<T> beanType) throws Exception;

    /**
     * 获取BeanDefinition的数量，即ioc中管理的所有类的数量
     * @return
     */
    int getBeanDefinitionCount();

    /**
     * 获取所有BeanDefinition的name属性
     * @return
     */
    String[] getBeanDefinitionNames();

    /**
     * 判断是否存在name为传入参数的bean
     * @param name
     * @return
     */
    boolean containsBean(String name);

    /**
     * 判断是否存在name为传入参数的BeanDefinition
     * 与上一个方法不同的是上一个判断的是实际的对象，而这个方法判断的是BeanDefinition
     * 若某个类为原型模式（非单例），则它的对象不会存储在ioc容器中，但BeanDefinition是一直存在的
     * @param beanName
     * @return
     */
    boolean containsBeanDefinition(String beanName);

    BeanDefinition getBeanDefinition (String beanName) throws NoSuchBeanException;

    BeanDefinition getBeanDefinition (String beanName, Class<?> beanType) throws NoSuchBeanException;

    BeanDefinition getBeanDefinition (Class<?> beanType) throws DuplicateBeanClassException, NoSuchBeanException;
}
