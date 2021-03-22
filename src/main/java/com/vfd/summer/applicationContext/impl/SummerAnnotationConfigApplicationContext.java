package com.vfd.summer.applicationContext.impl;

import com.vfd.summer.aop.ProxyFactory;
import com.vfd.summer.aop.annotation.*;
import com.vfd.summer.applicationContext.ApplicationContext;
import com.vfd.summer.ioc.annotation.*;
import com.vfd.summer.ioc.bean.BeanDefinition;
import com.vfd.summer.ioc.exception.DataConversionException;
import com.vfd.summer.ioc.exception.DuplicateBeanClassException;
import com.vfd.summer.ioc.exception.DuplicateBeanNameException;
import com.vfd.summer.ioc.exception.NoSuchBeanException;
import com.vfd.summer.ioc.tools.MyTools;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.*;
import java.util.*;

/**
 * @PackageName: iocByName.applicationContext
 * @ClassName: SummerAnnotationConfigApplicationContext
 * @Description:
 * @author: vfdxvffd
 * @date: 2021/3/14 上午10:57
 */
public class SummerAnnotationConfigApplicationContext implements ApplicationContext {

    //缓存ioc里面的对象，key：beanName, value：object
    private final Map<String, Object> iocByName = new HashMap<>();
    //缓存ioc里面的对象，key：beanType, value：object
    private final Map<Class<?>, Object> iocByType = new HashMap<>();
    //保存此ioc容器中所有对象的beanName和beanDefinition的对应关系
    private final Map<String, BeanDefinition> allBeansByName = new HashMap<>();
    //保存此ioc容器中所有对象的beanType和beanDefinition的对应关系
    private final Map<Class<?>, BeanDefinition> allBeansByType = new HashMap<>();
    //保存所有的切面类
    //private final Set<Class<?>> aspect = new HashSet<>();
    //保存bean的type和name的一一对应关系，方面切面修改为代理类
    private final Map<Class<?>, String> beanTypeAndName = new HashMap<>();
    //保存所有类和切它的切面方法的集合
    private final Map<Class<?>, Set<Method>> aspect = new HashMap<>();

    private final Logger logger = LoggerFactory.getLogger(getClass());

    /**
     * 加载的时候就扫描并创建对象
     * @param basePackages
     */
    public SummerAnnotationConfigApplicationContext(String... basePackages) throws DataConversionException, DuplicateBeanClassException, DuplicateBeanNameException, NoSuchMethodException, NoSuchBeanException, ClassNotFoundException, IllegalAccessException {
        //遍历包，找到目标类(原材料)
        Set<BeanDefinition> beanDefinitions = findBeanDefinitions(basePackages);
        //根据原材料创建bean
        createObject(beanDefinitions);
        //自动装载并将切面类中的方法横切目标方法并装入ioc容器中
        autowireObject(beanDefinitions);
        //容器初始化日志
        logger.info("IOC容器初始化完成");
    }

    /**
     * 对每个不是懒加载且是单例模式的bean进行注入工作
     * @param beanDefinitions
     * @throws NoSuchMethodException
     * @throws DuplicateBeanClassException
     * @throws IllegalAccessException
     * @throws DuplicateBeanNameException
     * @throws NoSuchBeanException
     * @throws DataConversionException
     * @throws ClassNotFoundException
     */
    private void autowireObject (Set<BeanDefinition> beanDefinitions) throws NoSuchMethodException, DuplicateBeanClassException, IllegalAccessException, DuplicateBeanNameException, NoSuchBeanException, DataConversionException, ClassNotFoundException {
        for (BeanDefinition beanDefinition : beanDefinitions) {
            if (!beanDefinition.getLazy() && beanDefinition.getSingleton()) {
                autowireObject(beanDefinition);
            }
        }
        logger.info("对于单例模式且非懒加载模式的bean自动注入完成");
    }

    /**
     * 检查此beanClass对应的对象是否已经完成了注入工作
     * 如果传入的是接口需要先转化为实现类的类型
     * 如果IOC容器中此beanClass对应的对象已经是代理对象则说明此对象已经完成了注入工作，因为包装代理类只发生在autoWired方法中
     * @param beanClass
     * @return
     * @throws NoSuchBeanException
     * @throws DuplicateBeanNameException
     * @throws DuplicateBeanClassException
     * @throws NoSuchMethodException
     * @throws DataConversionException
     * @throws IllegalAccessException
     * @throws ClassNotFoundException
     */
    private boolean haveNotWired (Class<?> beanClass) throws NoSuchBeanException, DuplicateBeanNameException, DuplicateBeanClassException, NoSuchMethodException, DataConversionException, IllegalAccessException, ClassNotFoundException {
        if (beanClass.isInterface()) {          //如果是接口就要去IOC容器中找它的实现类
            beanClass = getImplClassByInterface(beanClass);
        }
        if (beanClass == null) {
            return true;
        }
        if (!allBeansByType.get(beanClass).getSingleton()) {
            //对于非单例
            return true;
        }
        if (Proxy.isProxyClass(getBean(beanClass).getClass())/*iocByType.get(beanClass).getClass())*/) {
            return false;
        }
        Field[] declaredFields = beanClass.getDeclaredFields();
        for (Field field : declaredFields) {
            Autowired autowired = field.getAnnotation(Autowired.class);
            field.setAccessible(true);
            Object o = field.get(getBean(beanClass));
            if (autowired != null && o == null) {
                return true;
            }
        }
        return false;
    }

    /**
     * 根据传入的接口类型，在所有加了注解的类中找，如果有Key实现了这个接口，就返回实现类的类型，如果没有Key实现此接口则返回null
     * 一般返回null的情况就是代码出错，建议纠查bug
     * @param type
     * @return
     */
    private Class<?> getImplClassByInterface (Class<?> type) {
        Set<Class<?>> set = allBeansByType.keySet();
        if (type.isInterface()) {
            for (Class<?> clazz : set) {
                Class<?>[] interfaces = clazz.getInterfaces();
                for (Class<?> anInterface : interfaces) {
                    if (anInterface.equals(type)) {
                        return clazz;
                    }
                }
            }
        }
        return null;
    }

    /**
     * 自动注入标记了@Autowired的属性，并且在注入完成后检查此类中的方法是否需要面向切面，如果需要则将其包装为代理对象加入IOC中，否则注入结束
     * 注入的流程如下：
     *      1、先检查这个bean要注入的域的每个域是否已经完成了注入工作，如果没有则跳转2，如果该域已经完成注入或者无需注入则直接跳转3
     *      2、递归调用为这个域的每个域注入值
     *      3、为这个bean的每个域注入值
     *      4、检查这个bean是否需要被切面切入，如果需要将其包装成代理对象存入IOC容器，如果不需要就直接存入IOC容器
     *
     * @param beanDefinition
     * @throws NoSuchMethodException
     * @throws IllegalAccessException
     * @throws DataConversionException
     * @throws DuplicateBeanNameException
     * @throws NoSuchBeanException
     * @throws DuplicateBeanClassException
     * @throws ClassNotFoundException
     */
    private void autowireObject (BeanDefinition beanDefinition) throws NoSuchMethodException, IllegalAccessException, DataConversionException, DuplicateBeanNameException, NoSuchBeanException, DuplicateBeanClassException, ClassNotFoundException {
        if (!beanDefinition.getSingleton()) {
            return;
        }
        //对于非单例模式在调用getBean时又会去调用createObject,但能调用autowired说明已经调用过一次createObject了，
        //为了避免重复调用我们直接在上面返回
        autowireObject(getBean(beanDefinition.getBeanName()));
        /*if (!haveNotWired(beanDefinition.getBeanClass())) {
            return;
        }
        Class<?> beanClass = beanDefinition.getBeanClass();
        String beanName = beanDefinition.getBeanName();
        Field[] declaredFields = beanClass.getDeclaredFields();
        for (Field field : declaredFields) {
            Autowired annotation = field.getAnnotation(Autowired.class);
            Qualifier annotation1 = field.getAnnotation(Qualifier.class);
            if (annotation != null) {                    //标注了Autowired注解
                if (haveNotWired(field.getType())) {     //判断这个域是否有还未注入的对象，如果有，先为其注入
                    Class<?> type = field.getType();
                    if (type.isInterface()) {
                        type = getImplClassByInterface(type);
                    }
                    autowireObject(allBeansByType.get(type));
                }
                //对标注了@Autowired的域进行赋值操作
                if (annotation1 != null) {      //标注了Qualifier的域，有自己的beanName
                    String qualifier = annotation1.value();
                    try {
                        field.setAccessible(true);
                        field.set(getBean(beanName),getBean(qualifier));
                    } catch (DuplicateBeanNameException | NoSuchBeanException | IllegalAccessException | DataConversionException | DuplicateBeanClassException | NoSuchMethodException e) {
                        e.printStackTrace();
                    }
                } else {        //by Type
                    try {
                        field.setAccessible(true);
                        field.set(getBean(beanName), getBean(field.getType()));
                    } catch (DuplicateBeanNameException | NoSuchBeanException | IllegalAccessException | DataConversionException | DuplicateBeanClassException | NoSuchMethodException e) {
                        e.printStackTrace();
                    }
                }
            }
        }
        setProxy(beanClass);*/
        /*if (aspect.containsKey(beanClass)) {         //判断这个注入的对象是否需要被代理，如果需要则代理
            Set<Method> methods = aspect.get(beanClass);
            Map<Method, Set<Method>> aspectBefore = new HashMap<>();
            Map<Method, Set<Method>> aspectAfter = new HashMap<>();
            Map<Method, Set<Method>> aspectAfterThrowing = new HashMap<>();
            Map<Method, Set<Method>> aspectAfterReturning = new HashMap<>();
            for (Method method : methods) {
                Before before = method.getAnnotation(Before.class);
                After after = method.getAnnotation(After.class);
                AfterThrowing afterThrowing = method.getAnnotation(AfterThrowing.class);
                AfterReturning afterReturning = method.getAnnotation(AfterReturning.class);
                if (before != null) {
                    //前置
                    Method proxyMethod = getProxyMethod(before.value(), beanClass);
                    Set<Method> aspectMSet = aspectBefore.getOrDefault(proxyMethod, new HashSet<>());
                    aspectMSet.add(method);
                    aspectBefore.put(proxyMethod, aspectMSet);
                }
                if (after != null) {
                    //后置
                    Method proxyMethod = getProxyMethod(after.value(), beanClass);
                    Set<Method> aspectMSet = aspectAfter.getOrDefault(proxyMethod, new HashSet<>());
                    aspectMSet.add(method);
                    aspectAfter.put(proxyMethod, aspectMSet);
                }
                if (afterThrowing != null) {
                    //异常
                    Method proxyMethod = getProxyMethod(afterThrowing.value(), beanClass);
                    Set<Method> aspectMSet = aspectAfterThrowing.getOrDefault(proxyMethod, new HashSet<>());
                    aspectMSet.add(method);
                    aspectAfterThrowing.put(proxyMethod, aspectMSet);
//                            ProxyFactory proxyFactory = new ProxyFactory(getBean(beanName));
//                            Object proxy = proxyFactory.getProxyInstanceAfterThrowing(getProxyMethod(afterThrowing.value(), beanClass),
//                                    getBean(method.getDeclaringClass()), method, null);
//                            iocByType.put(beanClass, proxy);
//                            iocByName.put(beanTypeAndName.get(beanClass), proxy);
                }
                if (afterReturning != null) {
                    //返回
                    Method proxyMethod = getProxyMethod(afterReturning.value(), beanClass);
                    Set<Method> aspectMSet = aspectAfterReturning.getOrDefault(proxyMethod, new HashSet<>());
                    aspectMSet.add(method);
                    aspectAfterReturning.put(proxyMethod, aspectMSet);
                }
            }

            for (Method declaredMethod : beanClass.getDeclaredMethods()) {

                Set<Method> beforeMethodSet = aspectBefore.getOrDefault(declaredMethod, new HashSet<>());
                List<Method> beforeMethods = new LinkedList<>(beforeMethodSet);
                List<Object> beforeObject = new LinkedList<>();
                for (Method beforeMethod : beforeMethods) {
                    beforeObject.add(getBean(beforeMethod.getDeclaringClass()));
                }

                Set<Method> returningMethodSet = aspectAfterReturning.getOrDefault(declaredMethod, new HashSet<>());
                List<Method> returningMethods = new LinkedList<>(returningMethodSet);
                List<Object> returningObject = new LinkedList<>();
                for (Method returningMethod : returningMethods) {
                    returningObject.add(getBean(returningMethod.getDeclaringClass()));
                }

                Set<Method> throwingMethodSet = aspectAfterThrowing.getOrDefault(declaredMethod, new HashSet<>());
                List<Method> throwingMethods = new LinkedList<>(throwingMethodSet);
                List<Object> throwingObject = new LinkedList<>();
                for (Method throwingMethod : throwingMethods) {
                    throwingObject.add(getBean(throwingMethod.getDeclaringClass()));
                }

                Set<Method> afterMethodSet = aspectAfter.getOrDefault(declaredMethod, new HashSet<>());
                List<Method> afterMethods = new LinkedList<>(afterMethodSet);
                List<Object> afterObject = new LinkedList<>();
                for (Method afterMethod : afterMethods) {
                    afterObject.add(getBean(afterMethod.getDeclaringClass()));
                }

                if (beforeMethods.size() != 0 || returningMethods.size() != 0 ||
                        throwingMethods.size() != 0 || afterMethods.size() != 0) {
                    ProxyFactory proxyFactory = new ProxyFactory(getBean(beanName));
                    Object proxy = proxyFactory.getProxyInstance(declaredMethod, beforeMethods, beforeObject,
                            afterMethods, afterObject, throwingMethods, throwingObject,
                            returningMethods, returningObject);
                    iocByType.put(beanClass, proxy);
                    iocByName.put(beanTypeAndName.get(beanClass), proxy);
                }
            }
        }*/
    }

    /**
     * 同上个函数实现过程类似，但此方法是通过对象进行的注入，例如对于延迟加载的bean
     * @param object
     * @throws NoSuchMethodException
     * @throws IllegalAccessException
     * @throws ClassNotFoundException
     * @throws DataConversionException
     * @throws DuplicateBeanNameException
     * @throws NoSuchBeanException
     * @throws DuplicateBeanClassException
     */
    private void autowireObject (Object object) throws NoSuchMethodException, IllegalAccessException, ClassNotFoundException, DataConversionException, DuplicateBeanNameException, NoSuchBeanException, DuplicateBeanClassException {
        if (!haveNotWired(object.getClass())) {
            return;
        }
        Class<?> beanClass = object.getClass();
        Field[] declaredFields = beanClass.getDeclaredFields();
        for (Field field : declaredFields) {
            Autowired annotation = field.getAnnotation(Autowired.class);
            Qualifier annotation1 = field.getAnnotation(Qualifier.class);
            if (annotation != null) {
                if (haveNotWired(field.getType())) {     //判断这个域是否有还未注入的对象，如果有，先为其注入
                    Class<?> type = field.getType();
                    if (type.isInterface()) {
                        type = getImplClassByInterface(type);
                    }
                    autowireObject(allBeansByType.get(type));
                    // autowireObject(getBean(type));    此处如果直接递归调自己用getBean(type)来注入
                    // 假如type对应的是非单例模式的bean，那在getBean中会创建一次bean，但未加入IOC容器，
                    // 在后面field.set(object, getBean(field.getType())); 或者field.set(object, getBean(qualifier));
                    // 时又会重复创建
                }
                //对标注了@Autowired的域进行赋值操作
                if (annotation1 != null) {      //标注了Qualifier的域，有自己的beanName
                    String qualifier = annotation1.value();
                    try {
                        field.setAccessible(true);
                        //非单例的注入方式，因为非单例创建的bean不会加入到IOC容器中，所以此处直接使用object而不使用getBean,可以避免循环创建
                        field.set(object, getBean(qualifier));
                    } catch (DuplicateBeanNameException | NoSuchBeanException | IllegalAccessException | DataConversionException | DuplicateBeanClassException | NoSuchMethodException | ClassNotFoundException e) {
                        e.printStackTrace();
                    }
                } else {        //by Type
                    try {
                        field.setAccessible(true);
                        field.set(object, getBean(field.getType()));
                    } catch (DuplicateBeanNameException | NoSuchBeanException | IllegalAccessException | DataConversionException | DuplicateBeanClassException | NoSuchMethodException | ClassNotFoundException e) {
                        e.printStackTrace();
                    }
                }
                /*if (aspect.containsKey(beanClass)) {         //判断这个注入的对象是否需要被代理，如果需要则代理
                    Set<Method> methods = aspect.get(object.getClass());
                    Map<Method, Set<Method>> aspectBefore = new HashMap<>();
                    Map<Method, Set<Method>> aspectAfter = new HashMap<>();
                    Map<Method, Set<Method>> aspectAfterThrowing = new HashMap<>();
                    Map<Method, Set<Method>> aspectAfterReturning = new HashMap<>();
                    for (Method method : methods) {
                        Before before = method.getAnnotation(Before.class);
                        After after = method.getAnnotation(After.class);
                        AfterThrowing afterThrowing = method.getAnnotation(AfterThrowing.class);
                        AfterReturning afterReturning = method.getAnnotation(AfterReturning.class);
                        if (before != null) {
                            //前置
                            Method proxyMethod = getProxyMethod(before.value(), beanClass);
                            Set<Method> aspectMSet = aspectBefore.getOrDefault(proxyMethod, new HashSet<>());
                            aspectMSet.add(method);
                            aspectBefore.put(proxyMethod, aspectMSet);
                        }
                        if (after != null) {
                            //后置
                            Method proxyMethod = getProxyMethod(after.value(), beanClass);
                            Set<Method> aspectMSet = aspectAfter.getOrDefault(proxyMethod, new HashSet<>());
                            aspectMSet.add(method);
                            aspectAfter.put(proxyMethod, aspectMSet);
                        }
                        if (afterThrowing != null) {
                            //异常
                            Method proxyMethod = getProxyMethod(afterThrowing.value(), beanClass);
                            Set<Method> aspectMSet = aspectAfterThrowing.getOrDefault(proxyMethod, new HashSet<>());
                            aspectMSet.add(method);
                            aspectAfterThrowing.put(proxyMethod, aspectMSet);
                        }
                        if (afterReturning != null) {
                            //返回
                            Method proxyMethod = getProxyMethod(afterReturning.value(), beanClass);
                            Set<Method> aspectMSet = aspectAfterReturning.getOrDefault(proxyMethod, new HashSet<>());
                            aspectMSet.add(method);
                            aspectAfterReturning.put(proxyMethod, aspectMSet);
                        }
                    }

                    for (Method declaredMethod : beanClass.getDeclaredMethods()) {

                        Set<Method> beforeMethodSet = aspectBefore.getOrDefault(declaredMethod, new HashSet<>());
                        List<Method> beforeMethods = new LinkedList<>(beforeMethodSet);
                        List<Object> beforeObject = new LinkedList<>();
                        for (Method beforeMethod : beforeMethods) {
                            beforeObject.add(getBean(beforeMethod.getDeclaringClass()));
                        }

                        Set<Method> returningMethodSet = aspectAfterReturning.getOrDefault(declaredMethod, new HashSet<>());
                        List<Method> returningMethods = new LinkedList<>(returningMethodSet);
                        List<Object> returningObject = new LinkedList<>();
                        for (Method returningMethod : returningMethods) {
                            returningObject.add(getBean(returningMethod.getDeclaringClass()));
                        }

                        Set<Method> throwingMethodSet = aspectAfterThrowing.getOrDefault(declaredMethod, new HashSet<>());
                        List<Method> throwingMethods = new LinkedList<>(throwingMethodSet);
                        List<Object> throwingObject = new LinkedList<>();
                        for (Method throwingMethod : throwingMethods) {
                            throwingObject.add(getBean(throwingMethod.getDeclaringClass()));
                        }

                        Set<Method> afterMethodSet = aspectAfter.getOrDefault(declaredMethod, new HashSet<>());
                        List<Method> afterMethods = new LinkedList<>(afterMethodSet);
                        List<Object> afterObject = new LinkedList<>();
                        for (Method afterMethod : afterMethods) {
                            afterObject.add(getBean(afterMethod.getDeclaringClass()));
                        }

                        if (beforeMethods.size() != 0 || returningMethods.size() != 0 ||
                                throwingMethods.size() != 0 || afterMethods.size() != 0) {
                            ProxyFactory proxyFactory = new ProxyFactory(getBean(beanClass));
                            Object proxy = proxyFactory.getProxyInstance(declaredMethod, beforeMethods, beforeObject,
                                    afterMethods, afterObject, throwingMethods, throwingObject,
                                    returningMethods, returningObject);
                            iocByType.put(beanClass, proxy);
                            iocByName.put(beanTypeAndName.get(beanClass), proxy);
                        }
                    }
                }*/
            }
        }
        setProxy(beanClass);        // 设置代理（内部会先检查是否需要代理）
    }

    private void setProxy(Class<?> clazz) throws NoSuchMethodException, ClassNotFoundException, NoSuchBeanException, DuplicateBeanNameException, DuplicateBeanClassException, IllegalAccessException, DataConversionException {
        if (aspect.containsKey(clazz)) {         //判断这个注入的对象是否需要被代理，如果需要则代理
            Set<Method> methods = aspect.get(clazz);
            Map<Method, Set<Method>> aspectBefore = new HashMap<>();
            Map<Method, Set<Method>> aspectAfter = new HashMap<>();
            Map<Method, Set<Method>> aspectAfterThrowing = new HashMap<>();
            Map<Method, Set<Method>> aspectAfterReturning = new HashMap<>();
            for (Method method : methods) {
                Before before = method.getAnnotation(Before.class);
                After after = method.getAnnotation(After.class);
                AfterThrowing afterThrowing = method.getAnnotation(AfterThrowing.class);
                AfterReturning afterReturning = method.getAnnotation(AfterReturning.class);
                if (before != null) addMethodsForAspect(before.value(), clazz, method, aspectBefore);
                if (after != null)  addMethodsForAspect(after.value(), clazz, method, aspectAfter);
                if (afterThrowing != null)  addMethodsForAspect(afterThrowing.value(), clazz, method, aspectAfterThrowing);
                if (afterReturning != null) addMethodsForAspect(afterReturning.value(), clazz, method, aspectAfterReturning);
                /*if (before != null) {
                    //前置
                    Method proxyMethod = getProxyMethod(before.value(), clazz);
                    Set<Method> aspectMSet = aspectBefore.getOrDefault(proxyMethod, new HashSet<>());
                    aspectMSet.add(method);
                    aspectBefore.put(proxyMethod, aspectMSet);
                }
                if (after != null) {
                    //后置
                    Method proxyMethod = getProxyMethod(after.value(), clazz);
                    Set<Method> aspectMSet = aspectAfter.getOrDefault(proxyMethod, new HashSet<>());
                    aspectMSet.add(method);
                    aspectAfter.put(proxyMethod, aspectMSet);
                }
                if (afterThrowing != null) {
                    //异常
                    Method proxyMethod = getProxyMethod(afterThrowing.value(), clazz);
                    Set<Method> aspectMSet = aspectAfterThrowing.getOrDefault(proxyMethod, new HashSet<>());
                    aspectMSet.add(method);
                    aspectAfterThrowing.put(proxyMethod, aspectMSet);
                }
                if (afterReturning != null) {
                    //返回
                    Method proxyMethod = getProxyMethod(afterReturning.value(), clazz);
                    Set<Method> aspectMSet = aspectAfterReturning.getOrDefault(proxyMethod, new HashSet<>());
                    aspectMSet.add(method);
                    aspectAfterReturning.put(proxyMethod, aspectMSet);
                }*/
            }

            for (Method declaredMethod : clazz.getDeclaredMethods()) {

                List<Method> beforeMethods = new LinkedList<>(aspectBefore.getOrDefault(declaredMethod, new HashSet<>()));
                List<Object> beforeObject = getObjByAspect(beforeMethods);

                List<Method> returningMethods = new LinkedList<>(aspectAfterReturning.getOrDefault(declaredMethod, new HashSet<>()));
                List<Object> returningObject = getObjByAspect(returningMethods);

                List<Method> throwingMethods = new LinkedList<>(aspectAfterThrowing.getOrDefault(declaredMethod, new HashSet<>()));
                List<Object> throwingObject = getObjByAspect(throwingMethods);

                List<Method> afterMethods = new LinkedList<>(aspectAfter.getOrDefault(declaredMethod, new HashSet<>()));
                List<Object> afterObject = getObjByAspect(afterMethods);

                if (beforeMethods.size() != 0 || returningMethods.size() != 0 ||
                        throwingMethods.size() != 0 || afterMethods.size() != 0) {
                    ProxyFactory proxyFactory = new ProxyFactory(getBean(clazz));
                    Object proxy = proxyFactory.getProxyInstance(declaredMethod, beforeMethods, beforeObject,
                            afterMethods, afterObject, throwingMethods, throwingObject,
                            returningMethods, returningObject);
                    iocByType.put(clazz, proxy);
                    iocByName.put(beanTypeAndName.get(clazz), proxy);
                }
            }
            logger.debug("class:{}代理对象设置完成",clazz);
        }
    }

    /**
     * proxyMethod是被代理的方法（通过注解中的value和clazz获取），将所有要切它的方法加入到集合中再放入以它为key的map中
     * @param value 为切面方法注解的value,表示切的哪个方法
     * @param clazz 被代理方法的类
     * @param method
     * @param map
     * @throws NoSuchMethodException
     * @throws ClassNotFoundException
     */
    private void addMethodsForAspect(String value, Class<?> clazz, Method method, Map<Method, Set<Method>> map) throws NoSuchMethodException, ClassNotFoundException {
        Method proxyMethod = getProxyMethod(value, clazz);
        Set<Method> aspectMSet = map.getOrDefault(proxyMethod, new HashSet<>());
        aspectMSet.add(method);
        map.put(proxyMethod, aspectMSet);
    }

    /**
     * 通过切面方法获得要执行此切面方法的切面类的对象
     * @param methods
     * @return
     * @throws NoSuchMethodException
     * @throws ClassNotFoundException
     * @throws DuplicateBeanNameException
     * @throws IllegalAccessException
     * @throws DuplicateBeanClassException
     * @throws NoSuchBeanException
     * @throws DataConversionException
     */
    private List<Object> getObjByAspect(List<Method> methods) throws NoSuchMethodException, ClassNotFoundException, DuplicateBeanNameException, IllegalAccessException, DuplicateBeanClassException, NoSuchBeanException, DataConversionException {
        List<Object> objects = new LinkedList<>();
        for (Method method : methods) {
            objects.add(getBean(method.getDeclaringClass()));
        }
        return objects;
    }

    /**
     * 对每个非懒加载且是单例模式bean创建对象
     * @param beanDefinitions
     * @throws DataConversionException
     * @throws DuplicateBeanClassException
     * @throws DuplicateBeanNameException
     * @throws NoSuchMethodException
     */
    private void createObject(Set<BeanDefinition> beanDefinitions) throws DataConversionException, DuplicateBeanClassException, DuplicateBeanNameException, NoSuchMethodException {
        for (BeanDefinition beanDefinition : beanDefinitions) {
            if (!beanDefinition.getLazy() && beanDefinition.getSingleton()) {        //如果是懒加载模式则先不将其放到ioc容器中
                createObject(beanDefinition);
            }
        }
        logger.info("所有单例模式且非懒加载模式的bean创建完成");
    }

    /**
     * 为bean创建对象，如果有@Value注解则需要为其赋值
     * 为了防止忘记加set方法的问题，所以summer摒弃了spring的选择性的set方法注入，而是全局采用直接对属性设置访问权限并直接赋值
     * 如果是单例模式的创建则在检查完beanName和beanClass的冲突无误后加入IOC容器中，如果非单例则直接返回
     * @param beanDefinition
     * @return
     * @throws DataConversionException
     * @throws DuplicateBeanNameException
     * @throws DuplicateBeanClassException
     * @throws NoSuchMethodException
     */
    private Object createObject(BeanDefinition beanDefinition) throws DataConversionException, DuplicateBeanNameException, DuplicateBeanClassException, NoSuchMethodException {
        Class<?> beanClass = beanDefinition.getBeanClass();
        String beanName = beanDefinition.getBeanName();
        try {
            Object object = beanClass.getConstructor().newInstance();
            //对对象的属性赋值
            Field[] fields = beanClass.getDeclaredFields(); //拿到所有的域
            for (Field field : fields) {
                //注入标记了@Value的值
                Value annotation = field.getAnnotation(Value.class);
                if (annotation != null) {
                    String value = annotation.value();
                    Object val = convertVal(value, field);
                    field.setAccessible(true);
                    field.set(object, val);
                }
            }
            if (beanDefinition.getSingleton()) {    //如果是单例模式则加入ioc容器中
                if (iocByName.containsKey(beanName)) {
                    throw new DuplicateBeanNameException(beanName);
                }
                if (iocByType.containsKey(beanClass)) {
                    throw new DuplicateBeanClassException(beanClass);
                }
                iocByName.put(beanName, object);       //将创建的对象存入以beanName为键的缓存中
                iocByType.put(beanClass, object);      //将创建的对象存入以beanType为键的缓存中
                beanTypeAndName.put(beanClass, beanName);
            } else {
                //非单例则直接返回
                return object;
            }
        } catch (InstantiationException | InvocationTargetException | IllegalAccessException e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * 将@Value注解中String类型的值转化为相应的值
     * @param value
     * @param field
     * @return
     * @throws DataConversionException
     */
    private Object convertVal(String value, Field field) throws DataConversionException {
        Object val = null;
        switch (field.getType().getName()) {
            case "int":
            case "java.lang.Integer":
                try {
                    val = Integer.parseInt(value);
                } catch (NumberFormatException e) {
                    throw new DataConversionException(value, field.getType().getName());
                }
                break;
            case "java.lang.String":
                val = value;
                break;
            case "char":
                if (value.length() < 1)
                    throw new DataConversionException(value, field.getType().getName());
                val = value.charAt(0);
                break;
            case "long":
            case "java.lang.Long":
                try {
                    val = Long.parseLong(value);
                } catch (NumberFormatException e) {
                    throw new DataConversionException(value, field.getType().getName());
                }
                break;
            case "short":
            case "java.lang.Short":
                try {
                    val = Short.parseShort(value);
                } catch (NumberFormatException e) {
                    throw new DataConversionException(value, field.getType().getName());
                }
                break;
            case "double":
            case "java.lang.Double":
                try {
                    val = Double.parseDouble(value);
                } catch (NumberFormatException e) {
                    throw new DataConversionException(value, field.getType().getName());
                }
                break;
            case "boolean":
            case "java.lang.Boolean":
                try {
                    val = Boolean.parseBoolean(value);
                } catch (NumberFormatException e) {
                    throw new DataConversionException(value, field.getType().getName());
                }
                break;
            case "float":
            case "java.lang.Float":
                try {
                    val = Float.parseFloat(value);
                } catch (NumberFormatException e) {
                    throw new DataConversionException(value, field.getType().getName());
                }
                break;
            default:
                throw new DataConversionException(value, field.getType().getName());
        }
        return val;
    }

    /**
     * 获取某个包下的所有类
     * 将标注了@Aspect的类中的切面方法和切的对应的类以“类->方法集合”的映射方式加入map中
     * @param basePackages
     * @return
     */
    private Set<BeanDefinition> findBeanDefinitions(String... basePackages) throws IllegalStateException, ClassNotFoundException {
        Set<BeanDefinition> beanDefinitions = new HashSet<>();
        for (String basePackage : basePackages) {
            //1、获取包下的所有类
            Set<Class<?>> classes = MyTools.getClasses(basePackage);
            for (Class<?> clazz : classes) {
                //2、遍历这些类，找到添加了注解的类
                //先将带有Aspect注解的类保存起来
                if (clazz.getAnnotation(Aspect.class) != null) {
                    Method[] methods = clazz.getDeclaredMethods();
                    for (Method method : methods) {
                        Before before = method.getAnnotation(Before.class);
                        After after = method.getAnnotation(After.class);
                        AfterThrowing afterThrowing = method.getAnnotation(AfterThrowing.class);
                        AfterReturning afterReturning = method.getAnnotation(AfterReturning.class);
                        if (before != null) {
                            keepAspectMethod(method, before.value());
                        }
                        if (after != null) {
                            keepAspectMethod(method, after.value());
                        }
                        if (afterThrowing != null) {
                            keepAspectMethod(method, afterThrowing.value());
                        }
                        if (afterReturning != null) {
                            keepAspectMethod(method, afterReturning.value());
                        }
                    }
                }
                Component componentAnnotation = clazz.getAnnotation(Component.class);
                Repository repository = clazz.getAnnotation(Repository.class);
                Service service = clazz.getAnnotation(Service.class);
                Controller controller = clazz.getAnnotation(Controller.class);
                String beanName = null;
                if (componentAnnotation != null)    beanName = componentAnnotation.value();
                if (repository != null)    beanName = repository.value();
                if (service != null)    beanName = service.value();
                if (controller != null)    beanName = controller.value();
                if (beanName != null) {      //如果此类带了@Component、@Repository、@Service、@Controller注解之一
                    if ("".equals(beanName)) {    //没有添加beanName则默认是类的首字母小写
                        //获取类名首字母小写
                        String className = clazz.getName().replaceAll(clazz.getPackage().getName() + ".", "");
                        beanName = className.substring(0, 1).toLowerCase() + className.substring(1);
                    }
                    //3、将这些类封装成BeanDefinition，装载到集合中
                    Boolean lazy = clazz.getAnnotation(Lazy.class) != null;
                    boolean singleton = true;
                    Scope scope = clazz.getAnnotation(Scope.class);
                    if (scope != null) {
                        String value = scope.value();
                        if ("prototype".equals(value)) {        //指定为非单例模式321
                            singleton = false;
                        } else if (!"singleton".equals(value)) { //非法值
                            throw new IllegalStateException();
                        }
                    }
                    BeanDefinition beanDefinition = new BeanDefinition(beanName, clazz, lazy, singleton);
                    beanDefinitions.add(beanDefinition);
                    allBeansByName.put(beanName, beanDefinition);
                    allBeansByType.put(clazz, beanDefinition);
                    beanTypeAndName.put(clazz, beanName);
                }
            }
            logger.info("扫描package:{}完成",basePackage);
        }
        return beanDefinitions;
    }

    /**
     * 对于含有@Before、@After、@AfterThrowing注解的方法，将其对应的类以“类->方法集合”存入map中
     * @param method
     * @param value2
     * @throws ClassNotFoundException
     */
    private void keepAspectMethod(Method method, String value2) throws ClassNotFoundException {
        String className = value2.substring(0, value2.substring(0, value2.indexOf("(")).lastIndexOf("."));
        Class<?> aClass = Class.forName(className);
        Set<Method> set = aspect.getOrDefault(aClass, new HashSet<>());
        set.add(method);
        aspect.put(aClass, set);
    }

//    private void setProxyObj() throws ClassNotFoundException, NoSuchMethodException, NoSuchBeanException, DuplicateBeanNameException, DuplicateBeanClassException, DataConversionException, IllegalAccessException {
//        for (Class<?> clazz : aspect) {
//            Method[] methods = clazz.getDeclaredMethods();
//            for (Method method : methods) {
//                com.vfd.summer.aop.annotation.Before before = method.getAnnotation(com.vfd.summer.aop.annotation.Before.class);
//                com.vfd.summer.aop.annotation.After after = method.getAnnotation(com.vfd.summer.aop.annotation.After.class);
//                com.vfd.summer.aop.annotation.AfterThrowing afterThrowing = method.getAnnotation(com.vfd.summer.aop.annotation.AfterThrowing.class);
//                if (before != null) {
//                    //前置
//                    String value = before.value();  //value应该为全方法名  com.vfd.com.vfd.summer.ioc.Student.setName(String)
//                    String className = value.substring(0,value.substring(0,value.indexOf("(")).lastIndexOf("."));
//                    Class<?> aClass = Class.forName(className);
//                    ProxyFactory proxyFactory = new ProxyFactory(getBean(aClass));
//                    Object proxy = proxyFactory.getProxyInstanceBefore(getProxyMethod(value, aClass), getBean(clazz), method, null);
//                    iocByType.put(aClass, proxy);
//                    iocByName.put(beanTypeAndName.get(aClass), proxy);
//                }
//                if (after != null) {
//                    //后置
//                    String value = after.value();  //value应该为全方法名  com.vfd.com.vfd.summer.ioc.Student.setName(String)
//                    String className = value.substring(0,value.substring(0,value.indexOf("(")).lastIndexOf("."));
//                    Class<?> aClass = Class.forName(className);
//                    ProxyFactory proxyFactory = new ProxyFactory(getBean(aClass));
//                    Object proxy = proxyFactory.getProxyInstanceAfter(getProxyMethod(value, aClass), getBean(clazz), method, null);
//                    iocByType.put(aClass, proxy);
//                    iocByName.put(beanTypeAndName.get(aClass), proxy);
//                }
//                if (afterThrowing != null) {
//                    //抛出异常
//                    String value = afterThrowing.value();
//                    String className = value.substring(0,value.substring(0,value.indexOf("(")).lastIndexOf("."));
//                    Class<?> aClass = Class.forName(className);
//                    ProxyFactory proxyFactory = new ProxyFactory(getBean(aClass));
//                    Object proxy = proxyFactory.getProxyInstanceAfterThrowing(getProxyMethod(value, aClass), getBean(clazz), method, null);
//                    iocByType.put(aClass, proxy);
//                    iocByName.put(beanTypeAndName.get(aClass), proxy);
//                }
//            }
//        }
//    }

    /**
     * 根据方法全方法名获取方法对象，通过对value,即@Before、@After、@AfterThrowing注解中的方法全方法名进行处理获取到方法
     * @param value
     * @param aClass
     * @return
     * @throws ClassNotFoundException
     * @throws NoSuchMethodException
     */
    private Method getProxyMethod(String value, Class<?> aClass) throws ClassNotFoundException, NoSuchMethodException {
        String fullMethodName = value.substring(0, value.indexOf("("));     //全方法名
        String methodName = fullMethodName.substring(fullMethodName.lastIndexOf(".")+1);
        String argStr = value.substring(value.indexOf("(")+1,value.indexOf(")"));
        argStr = argStr.replaceAll(" ","");
        String[] args = argStr.split(",");
        Class<?>[] argType = new Class[args.length];
        if (!"".equals(argStr)) {
            for (int i = 0; i < args.length; i++) {
                argType[i] = Class.forName(args[i]);
            }
        }
        Method proxyMethod = null;
        if (!"".equals(argStr)) {
            proxyMethod = aClass.getDeclaredMethod(methodName, argType);
        } else {
            proxyMethod = aClass.getDeclaredMethod(methodName);
        }
        return proxyMethod;         //返回被代理的方法
    }

    /**
     * 根据beanName获取IOC容器中的bean
     * @param beanName
     * @return
     * @throws NoSuchBeanException
     * @throws DataConversionException
     * @throws DuplicateBeanClassException
     * @throws DuplicateBeanNameException
     * @throws NoSuchMethodException
     * @throws IllegalAccessException
     * @throws ClassNotFoundException
     */
    @Override
    public Object getBean(String beanName) throws NoSuchBeanException, DataConversionException, DuplicateBeanClassException, DuplicateBeanNameException, NoSuchMethodException, IllegalAccessException, ClassNotFoundException {
        Object bean = iocByName.getOrDefault(beanName, null);
        if (bean == null) {     //bean为null则说明没有在初始化的时候进行加载
            BeanDefinition beanDefinition = allBeansByName.get(beanName);
            if (beanDefinition == null) {
                throw new NoSuchBeanException();
            } else if (beanDefinition.getSingleton()){
                createObject(beanDefinition);
                autowireObject(beanDefinition);
            } else {
                Object object = createObject(beanDefinition);
                if (object != null) {
                    autowireObject(object);
                } else {
                    return null;
                }
                return object;
            }
        } else {
            return bean;
        }
        return iocByName.getOrDefault(beanName, null);
    }

    /**
     * 根据beanType获取IOC中的bean,支持传入接口，然后返回该接口实现类对应的bean
     * @param beanType
     * @param <T>
     * @return
     * @throws NoSuchBeanException
     * @throws DataConversionException
     * @throws DuplicateBeanClassException
     * @throws DuplicateBeanNameException
     * @throws NoSuchMethodException
     * @throws IllegalAccessException
     * @throws ClassNotFoundException
     */
    @Override
    public <T> T getBean(Class<T> beanType) throws NoSuchBeanException, DataConversionException, DuplicateBeanClassException, DuplicateBeanNameException, NoSuchMethodException, IllegalAccessException, ClassNotFoundException {
        Object o = iocByType.getOrDefault(beanType, null);
        if (o == null) {
            if (beanType.isInterface()) {       //如果是接口类型，则在其中找它的实现类
                Set<Map.Entry<Class<?>, Object>> entries = iocByType.entrySet();    //此处是在ioc容器中查找实现类，可能对于延迟加载的bean会出问题
                for (Map.Entry<Class<?>, Object> entry : entries) {
                    Class<?>[] interfaces = entry.getKey().getInterfaces();
                    for (Class<?> anInterface : interfaces) {
                        if (anInterface.equals(beanType)) {
                            //ioc中有一个实现了此接口的bean
                            return (T) entry.getValue();
                        }
                    }
                }
            }
            BeanDefinition beanDefinition = null;
            if (beanType.isInterface()) {
                 beanDefinition = allBeansByType.get(getImplClassByInterface(beanType));
            } else {
                beanDefinition = allBeansByType.get(beanType);
            }
            if (beanDefinition == null) {
                throw new NoSuchBeanException();
            } else if (beanDefinition.getSingleton()){
                createObject(beanDefinition);
                autowireObject(beanDefinition);
            } else {
                //非单例则不需要将其加入ioc容器
                Object object = createObject(beanDefinition);
                if (object != null) {
                    autowireObject(object);
                } else {
                    return null;
                }
                return (T) object;
            }
        } else {
            return (T) o;
        }
        return (T) iocByType.getOrDefault(beanType, null);
    }

}
