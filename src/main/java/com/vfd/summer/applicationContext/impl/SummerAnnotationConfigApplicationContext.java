package com.vfd.summer.applicationContext.impl;

import com.vfd.summer.annotation.Bean;
import com.vfd.summer.annotation.Configuration;
import com.vfd.summer.aop.bean.ProxyMethod;
import com.vfd.summer.aop.proxyFactory.impl.CGLibProxyFactory;
import com.vfd.summer.aop.proxyFactory.impl.JDKProxyFactory;
import com.vfd.summer.aop.annotation.*;
import com.vfd.summer.aop.proxyFactory.ProxyFactory;
import com.vfd.summer.applicationContext.ApplicationContext;
import com.vfd.summer.exception.DataConversionException;
import com.vfd.summer.exception.DuplicateBeanClassException;
import com.vfd.summer.exception.DuplicateBeanNameException;
import com.vfd.summer.exception.NoSuchBeanException;
import com.vfd.summer.extension.Extension;
import com.vfd.summer.ioc.annotation.*;
import com.vfd.summer.ioc.bean.BeanDefinition;
import com.vfd.summer.ioc.tools.MyTools;
import net.sf.cglib.proxy.Enhancer;
import net.sf.cglib.proxy.MethodInterceptor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.annotation.Annotation;
import java.lang.reflect.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @PackageName: com.vfd.summer.applicationContext.impl
 * @ClassName: SummerAnnotationConfigApplicationContext
 * @Description:
 * @author: vfdxvffd
 * @date: 2021/3/14 上午10:57
 */
public class SummerAnnotationConfigApplicationContext implements ApplicationContext {

    // 一级缓存，存放的是最终的对象
    // 缓存ioc里面的对象，key：beanName, value：object
    private final Map<String, Object> iocByName = new ConcurrentHashMap<>(256);

    // 二级缓存，存放半成品对象
    private final Map<String, Object> earlyRealObjects = new ConcurrentHashMap<>(256);

    // 二级缓存，存放半成品的代理对象
    private final Map<String, Object> earlyProxyObjects = new ConcurrentHashMap<>(16);

    // 保存所有的beanDefinition
    private final Set<BeanDefinition> beanDefinitions = new HashSet<>(256);

    // 保存此ioc容器中所有对象的beanName和beanDefinition的对应关系
    private final Map<String, BeanDefinition> allBeansByName = new HashMap<>();

    // 保存此ioc容器中所有对象的beanType和beanDefinition的对应关系
    private final Map<Class<?>, BeanDefinition> allBeansByType = new HashMap<>();

    // 保存bean的type和name的对应关系，采用缓存的形式存在
    private final Map<Class<?>, Set<String>> beanTypeAndName = new HashMap<>();

    // 保存所有类和切它的切面方法的集合
    private final Map<Class<?>, Set<Method>> aspect = new HashMap<>();

    // 对外扩展接口实现类的对象
    private List<? extends Extension> extensions = new ArrayList<>();

    // property配置文件的位置
    private final String propertyFile;

    // 记录了所有注解对应所有标注了这个注解的类
    private Map<Class<?>, List<Class<?>>> annotationType2Clazz = new HashMap<>();

    // 标注了needBeProxyed中的注解的类需要被代理
    private List<Class<?>> needBeProxyed = new ArrayList<>();

    // 记录关键位置的日志
    private final Logger logger = LoggerFactory.getLogger(getClass());

    /**
     * 加载的时候就扫描并创建对象，没有配置文件需要加载
     * @param basePackages 需要被ioc管理的包
     */
    public SummerAnnotationConfigApplicationContext(String... basePackages) throws Exception {
        this (null, basePackages);
    }

    /**
     * 加载的时候就扫描并创建对象，需要加载配置文件
     * @param basePackages 需要被ioc管理的包
     */
    public SummerAnnotationConfigApplicationContext(String propertyFile, String[] basePackages) throws Exception {

        this.propertyFile = propertyFile;

        //遍历包，找到目标类(原材料)
        findBeanDefinitions(basePackages);
        //根据原材料创建bean
        createObject();
        //先将需要代理的对象进行动态代理
        proxyObject();
        //自动装载并将切面类中的方法横切目标方法并装入ioc容器中
        autowireObject();
        // 注入配置类
        addConfig();
        //容器初始化日志
        logger.info("IOC容器初始化完成");
    }

    /**
     * 带有扩展性的构建ioc容器，需要加载配置文件，且有对此框架的扩展框架的适配
     * @param basePackages 需要被ioc管理的包
     */
    public SummerAnnotationConfigApplicationContext(String propertyFile, List<? extends Extension> extensions,
                                                    String... basePackages) throws Exception {
        this.extensions = extensions;
        this.propertyFile = propertyFile;
        for (Extension extension : this.extensions) {
            extension.doOperation0(this);
        }
        //遍历包，找到目标类(原材料)
        findBeanDefinitions(basePackages);
        for (Extension extension : this.extensions) {
            extension.doOperation1(this);
        }
        //根据原材料创建bean
        createObject();
        for (Extension extension : this.extensions) {
            extension.doOperation2(this);
        }
        //先将需要代理的对象进行动态代理
        proxyObject();
        for (Extension extension : this.extensions) {
            extension.doOperation3(this);
        }
        //自动装载并将切面类中的方法横切目标方法并装入ioc容器中
        autowireObject();
        for (Extension extension : this.extensions) {
            extension.doOperation4(this);
        }
        // 检查所有标注了@Configuration注解的类
        addConfig();
        for (Extension extension : this.extensions) {
            extension.doOperation9(this);
        }
        //容器初始化日志
        logger.info("IOC容器初始化完成");
    }

    /**
     * 将配置类加入到容器
     * 对一级缓存做检查，如果是配置类，则需要执行其中标注了@Bean的方法，然后将方法的返回结果加入一级缓存
     */
    private void addConfig () throws DuplicateBeanNameException, InvocationTargetException, IllegalAccessException {
        final Set<Map.Entry<String, Object>> entrySet = iocByName.entrySet();
        for (Map.Entry<String, Object> objectEntry : entrySet) {
            final Object o = checkConfig(objectEntry.getValue());
            if (o != null) {
                iocByName.put(objectEntry.getKey(), o);
            }
        }
    }

    /**
     * 检查是否是配置类，如果是就执行其中的标注了@Bean的方法，并将返回结果加入一级缓存
     * 如果配置需要代理，则将代理类以同样的beanName加入一级缓存覆盖掉之前的原对象
     * @param obj
     * @return
     */
    private Object checkConfig (Object obj) throws DuplicateBeanNameException, InvocationTargetException, IllegalAccessException {
        final Class<?> clazz = obj.getClass();
        final Configuration configuration = clazz.getAnnotation(Configuration.class);
        if (configuration != null) {
            for (Method method : clazz.getDeclaredMethods()) {
                final Bean bean = method.getAnnotation(Bean.class);
                if (bean != null) {
                    // 执行这个方法， 并将执行后的结果加入到IOC容器中
                    Class<?> aClass = method.getReturnType();
                    String beanName = bean.name();
                    if ("".equals(beanName)) {
                        beanName = method.getName();
                    }
                    // 也许只要一个allBeansByName来判断就足够了，但由于是||，就算iocByName不是必要的也不会影响效率
                    if (allBeansByName.containsKey(beanName) || iocByName.containsKey(beanName)) {
                        throw new DuplicateBeanNameException(beanName);
                    }
                    final Object[] args = new Object[]{};
                    final Object result = method.invoke(obj, args);
                    iocByName.put(beanName, result);
                    BeanDefinition beanDefinition = new BeanDefinition(beanName, aClass, false, true);
                    allBeansByName.put(beanName, beanDefinition);
                }
            }

            // 如果需要代理则将此标注了@Configuration的类的代理类加入ioc容器
            if (configuration.proxyBeanMethods())
                return setConfigProxy(obj);
        }
        return null;
    }

    /**
     * 对二级缓存中的realObj对象中需要进行代理的对象进行代理设置
     */
    private void proxyObject() throws Exception {
        for (Map.Entry<String, Object> objectEntry : earlyRealObjects.entrySet()) {
            proxyObject(objectEntry.getValue());
        }
    }

    /**
     * 服务于容器初始化时，对二级缓存中的realObj对象中需要进行代理的对象进行代理设置
     * @param obj
     * @return
     */
    private Object proxyObject(Object obj) throws Exception {
        Class<?> clazz = obj.getClass();
        // 由于是对特定对象的代理设置，所以通过beanTyp获取beanName时候不能使用getNameByType方法
        // getNameByType方法会通过此类型找出所有此类型及此类型派生类的所有的bean的beanName
        String beanName = allBeansByType.get(clazz).getBeanName();
        // 确保这个类需要被代理但是还没有被代理
        if (isNeedBeProxy(clazz, beanName)) {
            return setProxy(obj);
        }
        return obj;
    }

    private boolean isNeedBeProxy (Class<?> clazz, String beanName) {
        if (aspect.containsKey(clazz) && !earlyProxyObjects.containsKey(beanName)) {
            return true;
        }
        for (Method method : clazz.getDeclaredMethods()) {
            if (hasIntersection(method.getAnnotations())) {
                return true;
            }
        }
        return false;
    }

    private boolean hasIntersection (Annotation[] annotations) {
        Set<Class<?>> allElements = new HashSet<>(this.needBeProxyed);
        for (Annotation annotation : annotations) {
            allElements.add(annotation.annotationType());
        }
        return allElements.size() < (annotations.length + this.needBeProxyed.size());
    }

    /**
     * 向二级缓存的对象中自动注入依赖，当所有依赖都注入完成后即可将对象加入一级缓存，成为一个完整的对象
     * @throws Exception
     */
    private void autowireObject() throws Exception {
        for (Map.Entry<String, Object> objectEntry : earlyRealObjects.entrySet()) {
            autowireObject(objectEntry.getValue());
        }
        logger.info("所有单例模式且非懒加载模式的bean初始化完成");
    }

    /**
     * 为对象里标注了@Autowired的域注入值
     * @param object
     */
    private void autowireObject (Object object) throws Exception {
        final Class<?> clazz = object.getClass();
        for (Field field : clazz.getDeclaredFields()) {
            final Autowired autowired = field.getAnnotation(Autowired.class);
            final Qualifier qualifier = field.getAnnotation(Qualifier.class);
            field.setAccessible(true);
            if (field.get(object) != null) {
                continue;
            }
            if (autowired != null) {            //这个对象还有域需要注入
                Object bean;
                if (qualifier != null) {
                    //根据beanName进行注入
                    bean = getObject(qualifier.value());
                } else {
                    //根据beanType进行注入
                    bean = getObject(field.getType());
                }
                field.set(object, bean);
            }
        }
        // 检查此对象是否是单例、非懒加载的，如果是就将其加入一级缓存中，并从二级缓存中删除
        final BeanDefinition beanDefinition = allBeansByType.get(clazz);
        if (beanDefinition.getSingleton()) {
            String beanName = beanDefinition.getBeanName();
            iocByName.put(beanName, getObject(beanName));
            earlyRealObjects.remove(beanName);
            earlyProxyObjects.remove(beanName);
        }
    }

    /**
     * 对于AOP中被切面类横切的类进行代理设置，如果类实现了接口就采用JDK动态代理，如果没有实现接口就使用CGLib进行代理
     * 将代理对象加入到二级缓存中的earlyProxyObjects
     * @param object
     * @return
     * @throws Exception
     */
    @SuppressWarnings("all")
    private Object setProxy(Object object) throws Exception {
        Class<?> clazz = object.getClass();
        Object proxy = object;
        boolean whichProxy;
        Set<Method> methods = aspect.getOrDefault(clazz, new HashSet<>());
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
        }

        Map<Method, ProxyMethod> method2ProxyMethod = new HashMap<>();

        for (Method declaredMethod : clazz.getDeclaredMethods()) {
            List<Method> beforeMethods = new LinkedList<>(aspectBefore.getOrDefault(declaredMethod, new HashSet<>()));
            List<Object> beforeObject = getObjByAspect(beforeMethods);

            List<Method> returningMethods = new LinkedList<>(aspectAfterReturning.getOrDefault(declaredMethod, new HashSet<>()));
            List<Object> returningObject = getObjByAspect(returningMethods);

            List<Method> throwingMethods = new LinkedList<>(aspectAfterThrowing.getOrDefault(declaredMethod, new HashSet<>()));
            List<Object> throwingObject = getObjByAspect(throwingMethods);

            List<Method> afterMethods = new LinkedList<>(aspectAfter.getOrDefault(declaredMethod, new HashSet<>()));
            List<Object> afterObject = getObjByAspect(afterMethods);

            for (Extension extension : extensions) {
                extension.doOperationWhenProxy(this, declaredMethod, beforeMethods, beforeObject,
                        afterMethods, afterObject, throwingMethods, throwingObject,
                        returningMethods, returningObject);
            }

            if (beforeMethods.size() != 0 || returningMethods.size() != 0 || throwingMethods.size() != 0 || afterMethods.size() != 0) {
                method2ProxyMethod.put(declaredMethod, new ProxyMethod(beforeMethods, beforeObject,
                        returningMethods, returningObject, throwingMethods, throwingObject, afterMethods, afterObject));
            }
        }

        if (method2ProxyMethod.size() > 0) {
            ProxyFactory proxyFactory;
            if (clazz.getInterfaces().length == 0) {     //若没有实现接口则采用cglib实现动态代理
                proxyFactory = new CGLibProxyFactory(object);
                whichProxy = true;
            } else {
                proxyFactory = new JDKProxyFactory(object);
                whichProxy = false;
            }

            proxy = proxyFactory.getProxyInstance(method2ProxyMethod);

            // 检查此对象是否是单例，如果是则需要加入二级缓存中，后面继续对起进行注入工作
            final BeanDefinition beanDefinition = allBeansByType.get(clazz);
            if (beanDefinition.getSingleton()) {
                earlyProxyObjects.put(beanDefinition.getBeanName(), proxy);
            }
            logger.debug("class:[{}]代理对象设置完成，采用的代理方式为：{}",clazz,whichProxy ? "CGLib":"JDK");
        }
        return proxy;
    }

    /**
     * 设置配置类对象的代理，如果proxyBeanMethods属性为true,则表示需要代理
     * 代理的原则逻辑：如果此Bean已经存在与IOC容器（一级缓存）中，则直接从容器中获取并返回，如果不存在则执行后加入容器并返回
     * @param object
     * @return
     */
    private Object setConfigProxy (Object object) {
        Enhancer enhancer = new Enhancer();
        enhancer.setSuperclass(object.getClass());
        enhancer.setCallback((MethodInterceptor) (o, method, args, methodProxy) -> {
            Object result;
            final Bean bean = method.getAnnotation(Bean.class);
            if (bean != null) {
                Class<?> clazz = method.getReturnType();
                String beanName = bean.name();
                if ("".equals(beanName)) {
                   beanName = method.getName();
                }
                // String beanName = checkBeanName(bean.name(), clazz);
                if (iocByName.containsKey(beanName)) {
                    result = iocByName.get(beanName);
                } else {
                    result = methodProxy.invoke(object , args);
                    iocByName.put(beanName, result);
                    BeanDefinition beanDefinition = new BeanDefinition(beanName, clazz, false, true);
                    allBeansByName.put(beanName, beanDefinition);
                    // allBeansByType.put(clazz, beanDefinition);
                }
            } else {
                result = methodProxy.invoke(object , args);
            }
            return result;
        });
        return enhancer.create();
    }

    /**
     * proxyMethod是被代理的方法（通过注解中的value和clazz获取），将所有要切它的方法加入到集合中再放入以它为key的map中
     * @param value 为切面方法注解的value,表示切的哪个方法
     * @param clazz 被代理方法的类
     * @param method
     * @param map
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
     */
    private List<Object> getObjByAspect(List<Method> methods) throws Exception {
        List<Object> objects = new LinkedList<>();
        for (Method method : methods) {
            objects.add(getObject(method.getDeclaringClass()));
        }
        return objects;
    }

    /**
     * 对每个非懒加载且是单例模式bean创建对象
     */
    private void createObject() throws DataConversionException, DuplicateBeanNameException, NoSuchMethodException, IllegalAccessException, InstantiationException, InvocationTargetException {
        for (BeanDefinition beanDefinition : beanDefinitions) {
            if (!beanDefinition.getLazy() && beanDefinition.getSingleton()) {        //如果是懒加载模式则先不将其放到ioc容器中
                createObject(beanDefinition);
            }
        }
        logger.info("所有单例模式且非懒加载模式的bean实例化完成");
    }

    /**
     * 为bean创建对象，如果有@Value注解则需要为其赋值
     * 为了防止忘记加set方法的问题，所以summer摒弃了spring的选择性的set方法注入，而是全局采用直接对属性设置访问权限并直接赋值
     * 如果是单例模式的创建则在检查完beanName和beanClass的冲突无误后加入IOC容器中，如果非单例则直接返回
     * @param beanDefinition
     * @return
     */
    private Object createObject(BeanDefinition beanDefinition) throws DataConversionException, DuplicateBeanNameException, NoSuchMethodException, IllegalAccessException, InvocationTargetException, InstantiationException {
        Class<?> beanClass = beanDefinition.getBeanClass();
        String beanName = beanDefinition.getBeanName();
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
            if (earlyRealObjects.containsKey(beanName)) {
                throw new DuplicateBeanNameException(beanName);
            }
            //加入二级缓存的realObj中
            earlyRealObjects.put(beanName, object);
        }
        return object;
    }

    /**
     * 将@Value注解中String类型的值转化为相应的值
     * @param value
     * @param field
     * @return
     */
    private Object convertVal(String value, Field field) throws DataConversionException {
        Object val;
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
     * 通过beanType获取beanName，调用此方法必须保证此beanType对应的是唯一的一个beanName
     * 如果此beanType在容器中还有对应派生类的对象、或者此beanType是接口类型，在容器中有多个实现类对象则会抛出DuplicateBeanClassException异常
     * @param beanType
     * @return
     * @throws DuplicateBeanClassException
     * @throws NoSuchBeanException
     */
    private String getNameByType(Class<?> beanType) throws DuplicateBeanClassException, NoSuchBeanException {
        final Set<String> namesByType = getNamesByType(beanType);
        if (namesByType.size() == 1) {
            return namesByType.iterator().next();
        } else if (namesByType.size() > 1) {
            throw new DuplicateBeanClassException(beanType);
        } else {
            throw new NoSuchBeanException();
        }
    }

    /**
     * 通过beanType获取beanName
     * 此方法可以获取此类型对应的所有派生类或者实现类（对于接口而言）的对象的beanName
     * @param beanType
     * @return
     */
    private Set<String> getNamesByType (Class<?> beanType) {
        if (beanTypeAndName.containsKey(beanType)) {
            return beanTypeAndName.get(beanType);
        } else {
            // 缓存中没有 so检查后加入缓存中
            Set<String> set = new HashSet<>();
            for (Map.Entry<String, BeanDefinition> entry : allBeansByName.entrySet()) {
                //如果beanType是当前entry.getKey()的父类或者实现的接口或者父接口或者本身
                if (beanType.isAssignableFrom(entry.getValue().getBeanClass())) {
                    set.add(entry.getKey());
                }
            }
            beanTypeAndName.put(beanType, set);
            return set;
        }
    }

    /**
     * 获取某个包下的所有类
     * 将标注了@Aspect的类中的切面方法和切的对应的类以“类->方法集合”的映射方式加入map中
     * @param basePackages
     * @return
     */
    private void findBeanDefinitions(String... basePackages) throws IllegalStateException, ClassNotFoundException, DuplicateBeanNameException {
        for (String basePackage : basePackages) {
            //1、获取包下的所有类
            Set<Class<?>> classes = MyTools.getClasses(basePackage);
            for (Class<?> clazz : classes) {
                //2、遍历这些类，找到添加了注解的类
                for (Annotation annotation : clazz.getAnnotations()) {
                    List<Class<?>> clazzList = annotationType2Clazz.getOrDefault(annotation.annotationType(), new ArrayList<>());
                    clazzList.add(clazz);
                    annotationType2Clazz.put (annotation.annotationType(), clazzList);
                }
                //先将带有Aspect注解的类保存起来
                Aspect aspect = clazz.getAnnotation(Aspect.class);
                if (aspect != null) {
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
                final Configuration configuration = clazz.getAnnotation(Configuration.class);
                String beanName = null;
                if (componentAnnotation != null)    beanName = componentAnnotation.value();
                if (repository != null)    beanName = repository.value();
                if (service != null)    beanName = service.value();
                if (controller != null)    beanName = controller.value();
                if (configuration != null)  beanName = configuration.value();
                if (aspect != null)    beanName = aspect.value();
                if (beanName != null) {      //如果此类带了@Component、@Repository、@Service、@Controller注解之一
                    beanName = checkBeanName(beanName, clazz);
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
                    //确保对所有的beanDefinition都有记录
                    beanDefinitions.add(beanDefinition);
                    allBeansByName.put(beanName, beanDefinition);
                    allBeansByType.put(clazz, beanDefinition);
                }
            }
            logger.info("扫描package:[{}]完成",basePackage);
        }
    }

    private String checkBeanName (String beanName, Class<?> clazz) throws DuplicateBeanNameException {
        if ("".equals(beanName)) {    //没有添加beanName则默认是类的首字母小写
            //获取类名首字母小写
            String className = clazz.getName().replaceAll(clazz.getPackage().getName() + ".", "");
            beanName = className.substring(0, 1).toLowerCase() + className.substring(1);
        }
        if (allBeansByName.containsKey(beanName)) {
            throw new DuplicateBeanNameException(beanName);
        }
        return beanName;
    }

    /**
     * 对于含有@Before、@After、@AfterThrowing注解的方法，将其对应的类以“类->方法集合”存入map中
     * @param method
     * @param value2
     */
    private void keepAspectMethod(Method method, String value2) throws ClassNotFoundException {
        String className = value2.substring(0, value2.substring(0, value2.indexOf("(")).lastIndexOf("."));
        Class<?> aClass = Class.forName(className);
        Set<Method> set = aspect.getOrDefault(aClass, new HashSet<>());
        set.add(method);
        aspect.put(aClass, set);
    }

    /**
     * 根据方法全方法名获取方法对象，通过对value,即@Before、@After、@AfterThrowing注解中的方法全方法名进行处理获取到方法
     * @param value
     * @param aClass
     * @return
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
        Method proxyMethod;
        if (!"".equals(argStr)) {
            proxyMethod = aClass.getDeclaredMethod(methodName, argType);
        } else {
            proxyMethod = aClass.getDeclaredMethod(methodName);
        }
        return proxyMethod;         //返回被代理的方法
    }

    /**
     * 对于非单例或者延迟加载的bean在此创建实例化、代理、初始化
     * @param beanDefinition
     * @return
     */
    private Object createBean(BeanDefinition beanDefinition) throws Exception {
        for (Extension extension : extensions) {
            extension.doOperation5(this, beanDefinition);
        }
        //实例化一个对象，但并未初始化
        final Object object = createObject(beanDefinition);
        for (Extension extension : extensions) {
            extension.doOperation6(this, object);
        }
        //对刚刚实例化的对象进行代理处理，需要先判断是否需要代理
        final Object o = proxyObject(object);
        for (Extension extension : extensions) {
            extension.doOperation7(this, object);
        }
        //对代理后的对象（如果需要）进行注入工作
        autowireObject(object);
        for (Extension extension : extensions) {
            extension.doOperation8(this, object);
        }
        return o;
    }

    /**
     * ioc容器构造过程中获取某个bean,可能是二级缓存中的demo
     * @param beanName
     * @return
     */
    private Object getObject(String beanName) throws Exception {
        final Object o = iocByName.get(beanName);       //从一级缓存中获取
        if ( o != null) {
            return o;
        }
        if (earlyProxyObjects.containsKey(beanName)) {  //从二级缓存中获取
            return earlyProxyObjects.get(beanName);
        }
        if (earlyRealObjects.containsKey(beanName)) {
            return earlyRealObjects.get(beanName);
        }
        //如果缓存中都没有则表示该bean为非单例或者懒加载的，则为其创建一个对象，并根据是否为单例而决定是否加入ioc容器
        final BeanDefinition beanDefinition = allBeansByName.get(beanName);
        if (beanDefinition == null)         //这个类并没有被ioc容器管理，可以考虑抛出异常提示用户
            return null;
        final Object bean = createBean(beanDefinition);
        if (beanDefinition.getSingleton()) {
            return iocByName.get(beanName);
        }
        return bean;
    }

    private Object getObject(Class<?> beanType) throws Exception {
        return getObject(getNameByType(beanType));
    }

    /**
     * 根据beanName获取IOC容器中的bean
     * @param beanName
     * @return
     */
    @Override
    public Object getBean(String beanName) throws Exception {
        // 先尝试在一级缓存中获取
        final Object o = iocByName.get(beanName);
        if (o != null) {
            return o;
        }
        // 考虑可能是懒加载或者原型模式
        final BeanDefinition beanDefinition = allBeansByName.get(beanName);
        if (beanDefinition == null)         // 这个bean并未由ioc容器管理
            throw new NoSuchBeanException();
        final Object bean = createBean(beanDefinition);     // 必定为懒加载或者原型模式
        if (beanDefinition.getSingleton()) {
            return iocByName.get(beanName);
        }
        return bean;
    }

    /**
     * 根据beanType获取IOC中的bean,支持传入接口，然后返回该接口实现类对应的bean
     * @param beanType
     * @param <T>
     * @return
     */
    @SuppressWarnings("all")
    @Override
    public <T> T getBean(Class<T> beanType) throws Exception {
        return (T) getBean(getNameByType(beanType));
    }

    @SuppressWarnings("all")
    @Override
    public <T> T getBean(String name, Class<T> beanType) throws Exception {
        final Object o = getBean(name); //iocByName.get(name);
        if (beanType.isInstance(o)) {
            return (T) o;
        } else {
            throw new NoSuchBeanException();
        }
    }

    @Override
    public Class<?> getType(String name) throws NoSuchBeanException {
        if (allBeansByName.containsKey(name)) {
            return allBeansByName.get(name).getBeanClass();
        } else {
            throw new NoSuchBeanException();
        }
    }

    @SuppressWarnings("all")
    @Override
    public <T> Map<String, T> getBeansOfType(Class<T> beanType) throws Exception {
        Map<String, T> map = new HashMap<>();
        for (String s : getNamesByType(beanType)) {
            map.put(s, (T) getBean(s));
        }
        return map;
    }

    @Override
    public int getBeanDefinitionCount() {
        return beanDefinitions.size();
    }

    @Override
    public String[] getBeanDefinitionNames() {
        String[] result = new String[beanDefinitions.size()];
        int i = 0;
        for (BeanDefinition beanDefinition:beanDefinitions) {
            result[i++] = beanDefinition.getBeanName();
        }
        return result;
    }

    @Override
    public boolean containsBean(String name) {
        return allBeansByName.containsKey(name);
    }

    @Override
    public boolean containsBeanDefinition(String beanName) {
        return allBeansByName.containsKey(beanName);
    }

    public Map<Class<?>, List<Class<?>>> getAnnotationType2Clazz() {
        return annotationType2Clazz;
    }

    public List<Class<?>> getNeedBeProxyed() {
        return needBeProxyed;
    }

    public Map<String, Object> getIocByName() {
        return iocByName;
    }

    public Map<String, Object> getEarlyRealObjects() {
        return earlyRealObjects;
    }

    public Map<String, Object> getEarlyProxyObjects() {
        return earlyProxyObjects;
    }

    public Set<BeanDefinition> getBeanDefinitions() {
        return beanDefinitions;
    }

    public Map<String, BeanDefinition> getAllBeansByName() {
        return allBeansByName;
    }

    public Map<Class<?>, BeanDefinition> getAllBeansByType() {
        return allBeansByType;
    }

    public Map<Class<?>, Set<String>> getBeanTypeAndName() {
        return beanTypeAndName;
    }

    public Map<Class<?>, Set<Method>> getAspect() {
        return aspect;
    }

    public String getPropertyFile() {
        return propertyFile;
    }
}
