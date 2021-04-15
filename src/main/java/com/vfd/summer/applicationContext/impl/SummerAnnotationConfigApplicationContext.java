package com.vfd.summer.applicationContext.impl;

import com.vfd.summer.aop.proxyFactory.impl.CGLibProxyFactory;
import com.vfd.summer.aop.proxyFactory.impl.JDKProxyFactory;
import com.vfd.summer.aop.annotation.*;
import com.vfd.summer.aop.proxyFactory.ProxyFactory;
import com.vfd.summer.applicationContext.ApplicationContext;
import com.vfd.summer.exception.DataConversionException;
import com.vfd.summer.exception.DuplicateBeanClassException;
import com.vfd.summer.exception.DuplicateBeanNameException;
import com.vfd.summer.exception.NoSuchBeanException;
import com.vfd.summer.ioc.annotation.*;
import com.vfd.summer.ioc.bean.BeanDefinition;
import com.vfd.summer.ioc.tools.MyTools;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @PackageName: iocByName.applicationContext
 * @ClassName: SummerAnnotationConfigApplicationContext
 * @Description:
 * @author: vfdxvffd
 * @date: 2021/3/14 上午10:57
 */
public class SummerAnnotationConfigApplicationContext implements ApplicationContext {

    //一级缓存，存放的是最终的对象
    //缓存ioc里面的对象，key：beanName, value：object
    private final Map<String, Object> iocByName = new HashMap<>(256);

    //二级缓存，存放半成品对象
    private final Map<String, Object> earlyRealObjects = new ConcurrentHashMap<>(256);

    //二级缓存，存放半成品的代理对象
    private final Map<String, Object> earlyProxyObjects = new ConcurrentHashMap<>(16);

    //保存所有的beanDefinition
    Set<BeanDefinition> beanDefinitions = new HashSet<>(256);

    //保存此ioc容器中所有对象的beanName和beanDefinition的对应关系
    private final Map<String, BeanDefinition> allBeansByName = new HashMap<>();

    //保存此ioc容器中所有对象的beanType和beanDefinition的对应关系
    private final Map<Class<?>, BeanDefinition> allBeansByType = new HashMap<>();

    //保存bean的type和name的对应关系，采用缓存的形式存在
    private final Map<Class<?>, Set<String>> beanTypeAndName = new HashMap<>();

    //保存所有类和切它的切面方法的集合
    private final Map<Class<?>, Set<Method>> aspect = new HashMap<>();

    //记录关键位置的日志
    private final Logger logger = LoggerFactory.getLogger(getClass());

    /**
     * 加载的时候就扫描并创建对象
     * @param basePackages
     */
    public SummerAnnotationConfigApplicationContext(String... basePackages) throws ClassNotFoundException, IllegalAccessException, InvocationTargetException, InstantiationException, NoSuchMethodException, DataConversionException, DuplicateBeanNameException, DuplicateBeanClassException, NoSuchBeanException {
        //遍历包，找到目标类(原材料)
        findBeanDefinitions(basePackages);
        //根据原材料创建bean
        createObject();
        //先将需要代理的对象进行动态代理
        proxyObject();
        //自动装载并将切面类中的方法横切目标方法并装入ioc容器中
        autowireObject();
        //容器初始化日志
        logger.info("IOC容器初始化完成");
    }

    /**
     * 对二级缓存中的realObj对象中需要进行代理的对象进行代理设置
     */
    private void proxyObject() throws NoSuchMethodException, ClassNotFoundException, IllegalAccessException, DuplicateBeanNameException, NoSuchBeanException, DataConversionException, DuplicateBeanClassException, InvocationTargetException, InstantiationException {
        for (Map.Entry<String, Object> objectEntry : earlyRealObjects.entrySet()) {
            proxyObject(objectEntry.getValue());
        }
    }

    /**
     * 服务于容器初始化时，对二级缓存中的realObj对象中需要进行代理的对象进行代理设置
     * @param obj
     * @return
     */
    private Object proxyObject(Object obj) throws NoSuchMethodException, IllegalAccessException, DuplicateBeanClassException, DuplicateBeanNameException, NoSuchBeanException, DataConversionException, ClassNotFoundException, InvocationTargetException, InstantiationException {
        Class<?> clazz = obj.getClass();
        String beanName = allBeansByType.get(clazz).getBeanName();
        //确保这个类需要被代理但是还没有被代理
        if (aspect.containsKey(clazz) && !earlyProxyObjects.containsKey(beanName)) {
            return setProxy(obj);
        }
        return obj;
    }

    private void autowireObject() throws NoSuchMethodException, DuplicateBeanClassException, IllegalAccessException, DuplicateBeanNameException, NoSuchBeanException, DataConversionException, ClassNotFoundException, InvocationTargetException, InstantiationException {
        for (Map.Entry<String, Object> objectEntry : earlyRealObjects.entrySet()) {
            autowireObject(objectEntry.getValue());
        }
    }

    /**
     *
     * @param object
     * @throws NoSuchMethodException
     * @throws IllegalAccessException
     * @throws ClassNotFoundException
     */
    private void autowireObject (Object object) throws NoSuchMethodException, IllegalAccessException, ClassNotFoundException, DataConversionException, DuplicateBeanNameException, NoSuchBeanException, DuplicateBeanClassException, InvocationTargetException, InstantiationException {
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
        final BeanDefinition beanDefinition = allBeansByType.get(clazz);
        if (beanDefinition.getSingleton()) {
            String beanName = beanDefinition.getBeanName();
            iocByName.put(beanName, getObject(beanName));
            earlyRealObjects.remove(beanName);
            earlyProxyObjects.remove(beanName);
        }
    }

    @SuppressWarnings("all")
    private Object setProxy(Object object) throws NoSuchMethodException, ClassNotFoundException, NoSuchBeanException, DuplicateBeanNameException, DuplicateBeanClassException, IllegalAccessException, DataConversionException, InvocationTargetException, InstantiationException {
        Class<?> clazz = object.getClass();
        Object proxy = object;
        if (aspect.containsKey(clazz)) {         //判断这个注入的对象是否需要被代理，如果需要则代理
            boolean whichProxy;
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
                    ProxyFactory proxyFactory;
                    if (clazz.getInterfaces().length == 0) {     //若没有实现接口则采用cglib实现动态代理
                        proxyFactory = new CGLibProxyFactory(object);
                        whichProxy = true;
                    } else {
                        proxyFactory = new JDKProxyFactory(object);
                        whichProxy = false;
                    }
                    proxy = proxyFactory.getProxyInstance(declaredMethod, beforeMethods, beforeObject,
                            afterMethods, afterObject, throwingMethods, throwingObject,
                            returningMethods, returningObject);
                    final BeanDefinition beanDefinition = allBeansByType.get(clazz);
                    if (beanDefinition.getSingleton()) {
                        earlyProxyObjects.put(beanDefinition.getBeanName(), proxy);
                    }
                    logger.debug("class:[{}]代理对象设置完成，采用的代理方式为：{}",clazz,whichProxy ? "CGLib":"JDK");
                }
            }
        }
        return proxy;
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
    private List<Object> getObjByAspect(List<Method> methods) throws NoSuchMethodException, ClassNotFoundException, DuplicateBeanNameException, IllegalAccessException, DuplicateBeanClassException, NoSuchBeanException, DataConversionException, InvocationTargetException, InstantiationException {
        List<Object> objects = new LinkedList<>();
        for (Method method : methods) {
            objects.add(getObject(method.getDeclaringClass()));
        }
        return objects;
    }

    /**
     * 对每个非懒加载且是单例模式bean创建对象
     * @throws DataConversionException
     * @throws DuplicateBeanClassException
     * @throws DuplicateBeanNameException
     * @throws NoSuchMethodException
     */
    private void createObject() throws DataConversionException, DuplicateBeanNameException, NoSuchMethodException, IllegalAccessException, InstantiationException, InvocationTargetException {
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
     * @throws DataConversionException
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

    private String getNameByType(Class<?> beanType) throws DuplicateBeanClassException {
        final Set<String> namesByType = getNamesByType(beanType);
        if (namesByType.size() == 1) {
            return namesByType.iterator().next();
        } else {
            throw new DuplicateBeanClassException(beanType);
        }
    }

    private Set<String> getNamesByType(Class<?> beanType) {
        if (beanTypeAndName.containsKey(beanType)) {
            return beanTypeAndName.get(beanType);
        } else {
            // 缓存中没有 so检查后加入缓存中
            Set<String> set = new HashSet<>();
            for (Map.Entry<Class<?>, BeanDefinition> entry : allBeansByType.entrySet()) {
                //如果beanType是当前entry.getKey()的父类或者实现的接口或者父接口或者本身
                if (beanType.isAssignableFrom(entry.getKey())) {
                    set.add(entry.getValue().getBeanName());
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
                String beanName = null;
                if (componentAnnotation != null)    beanName = componentAnnotation.value();
                if (repository != null)    beanName = repository.value();
                if (service != null)    beanName = service.value();
                if (controller != null)    beanName = controller.value();
                if (aspect != null)    beanName = aspect.value();
                if (beanName != null) {      //如果此类带了@Component、@Repository、@Service、@Controller注解之一
                    if ("".equals(beanName)) {    //没有添加beanName则默认是类的首字母小写
                        //获取类名首字母小写
                        String className = clazz.getName().replaceAll(clazz.getPackage().getName() + ".", "");
                        beanName = className.substring(0, 1).toLowerCase() + className.substring(1);
                    }
                    if (allBeansByName.containsKey(beanName)) {
                        throw new DuplicateBeanNameException(beanName);
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
                    //确保对所有的beanDefinition都有记录
                    beanDefinitions.add(beanDefinition);
                    allBeansByName.put(beanName, beanDefinition);
                    allBeansByType.put(clazz, beanDefinition);
                }
            }
            logger.info("扫描package:[{}]完成",basePackage);
        }
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
     * @throws NoSuchMethodException
     * @throws DuplicateBeanNameException
     * @throws DuplicateBeanClassException
     * @throws DataConversionException
     * @throws IllegalAccessException
     * @throws NoSuchBeanException
     * @throws ClassNotFoundException
     */
    private Object createBean(BeanDefinition beanDefinition) throws NoSuchMethodException, DuplicateBeanNameException, DuplicateBeanClassException, DataConversionException, IllegalAccessException, NoSuchBeanException, ClassNotFoundException, InvocationTargetException, InstantiationException {
        //实例化一个对象，但并未初始化
        final Object object = createObject(beanDefinition);
        //对刚刚实例化的对象进行代理处理，需要先判断是否需要代理
        final Object o = proxyObject(object);
        //对代理后的对象（如果需要）进行注入工作
        autowireObject(object);
        return o;
    }

    /**
     * ioc容器构造过程中获取某个bean,可能是二级缓存中的demo
     * @param beanName
     * @return
     */
    private Object getObject(String beanName) throws NoSuchMethodException, NoSuchBeanException, DataConversionException, DuplicateBeanNameException, IllegalAccessException, ClassNotFoundException, DuplicateBeanClassException, InvocationTargetException, InstantiationException {
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

    private Object getObject(Class<?> beanType) throws NoSuchMethodException, ClassNotFoundException, IllegalAccessException, DuplicateBeanNameException, NoSuchBeanException, DuplicateBeanClassException, DataConversionException, InvocationTargetException, InstantiationException {
        return getObject(getNameByType(beanType));
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
    public Object getBean(String beanName) throws NoSuchBeanException, DataConversionException, DuplicateBeanClassException, DuplicateBeanNameException, NoSuchMethodException, IllegalAccessException, ClassNotFoundException, InvocationTargetException, InstantiationException {
        //先尝试在一级缓存中获取
        final Object o = iocByName.get(beanName);
        if (o != null) {
            return o;
        }
        final BeanDefinition beanDefinition = allBeansByName.get(beanName);
        if (beanDefinition == null)
            throw new NoSuchBeanException();
        final Object bean = createBean(beanDefinition);
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
     * @throws NoSuchBeanException
     * @throws DataConversionException
     * @throws DuplicateBeanClassException
     * @throws DuplicateBeanNameException
     * @throws NoSuchMethodException
     * @throws IllegalAccessException
     * @throws ClassNotFoundException
     */
    @SuppressWarnings("all")
    @Override
    public <T> T getBean(Class<T> beanType) throws NoSuchBeanException, DataConversionException, DuplicateBeanClassException, DuplicateBeanNameException, NoSuchMethodException, IllegalAccessException, ClassNotFoundException, InvocationTargetException, InstantiationException {
        return (T) getBean(getNameByType(beanType));
    }

    @SuppressWarnings("all")
    @Override
    public <T> T getBean(String name, Class<T> beanType) throws NoSuchBeanException, NoSuchMethodException, ClassNotFoundException, IllegalAccessException, InstantiationException, DuplicateBeanNameException, DuplicateBeanClassException, InvocationTargetException, DataConversionException {
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
    public <T> Map<String, T> getBeansOfType(Class<T> beanType) throws NoSuchBeanException, InstantiationException, InvocationTargetException, NoSuchMethodException, IllegalAccessException, DuplicateBeanNameException, DataConversionException, DuplicateBeanClassException, ClassNotFoundException {
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
}
