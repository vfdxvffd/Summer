package ioc.applicationContext.impl;

import ioc.annotation.*;
import ioc.applicationContext.ApplicationContext;
import ioc.bean.BeanDefinition;
import ioc.exception.*;
import ioc.exception.IllegalStateException;
import ioc.tools.MyTools;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
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
    private final Map<Class<?>, Object> iocByType = new HashMap<>();
    //保存此ioc容器中所有对象的beanName和beanDefinition的对应关系
    private final Map<String, BeanDefinition> allBeansByName = new HashMap<>();
    //保存此ioc容器中所有对象的beanType和beanDefinition的对应关系
    private final Map<Class<?>, BeanDefinition> allBeansByType = new HashMap<>();

    /**
     * 加载的时候就扫描并创建对象，日后计划加入懒加载模式
     * @param basePackages
     */
    public SummerAnnotationConfigApplicationContext(String... basePackages) throws DataConversionException, DuplicateBeanClassException, DuplicateBeanNameException, IllegalStateException {
        //遍历包，找到目标类(原材料)
        Set<BeanDefinition> beanDefinitions = findBeanDefinitions(basePackages);
        //根据原材料创建bean
        createObject(beanDefinitions);
        //自动装载
        autowireObject(beanDefinitions);
    }

    private void autowireObject (Set<BeanDefinition> beanDefinitions) {
        for (BeanDefinition beanDefinition : beanDefinitions) {
            if (!beanDefinition.getLazy() && beanDefinition.getSingleton()) {
                autowireObject(beanDefinition);
            }
        }
    }

    private void autowireObject (BeanDefinition beanDefinition) {
        Class<?> beanClass = beanDefinition.getBeanClass();
        String beanName = beanDefinition.getBeanName();
        Field[] declaredFields = beanClass.getDeclaredFields();
        for (Field field : declaredFields) {
            Autowired annotation = field.getAnnotation(Autowired.class);
            Qualifier annotation1 = field.getAnnotation(Qualifier.class);
            if (annotation != null) {
                //对标注了@Autowired的域进行赋值操作
                Method declaredMethod = getSetMethod(beanClass, field);
                if (annotation1 != null) {
                    //标注了Qualifier的域，有自己的beanName
                    String qualifier = annotation1.value();
                    try {
                        declaredMethod.invoke(getBean(beanName),getBean(qualifier));
                    } catch (IllegalAccessException | InvocationTargetException | NoSuchBeanException | DataConversionException | DuplicateBeanClassException | DuplicateBeanNameException e) {
                        e.printStackTrace();
                    }
                } else {
                    //by Type
                    try {
                        declaredMethod.invoke(getBean(beanName),getBean(field.getType()));
                    } catch (IllegalAccessException | InvocationTargetException | NoSuchBeanException | DuplicateBeanNameException | DataConversionException | DuplicateBeanClassException e) {
                        e.printStackTrace();
                    }
                }
            }
        }
    }

    private void autowireObject (Object object) {
        Class<?> beanClass = object.getClass();
        Field[] declaredFields = beanClass.getDeclaredFields();
        for (Field field : declaredFields) {
            Autowired annotation = field.getAnnotation(Autowired.class);
            Qualifier annotation1 = field.getAnnotation(Qualifier.class);
            if (annotation != null) {
                //对标注了@Autowired的域进行赋值操作
                Method declaredMethod = getSetMethod(beanClass, field);
                if (annotation1 != null) {
                    //标注了Qualifier的域，有自己的beanName
                    String qualifier = annotation1.value();
                    try {
                        declaredMethod.invoke(object,getBean(qualifier));
                    } catch (IllegalAccessException | InvocationTargetException | NoSuchBeanException | DataConversionException | DuplicateBeanClassException | DuplicateBeanNameException e) {
                        e.printStackTrace();
                    }
                } else {
                    //by Type
                    try {
                        declaredMethod.invoke(object,getBean(field.getType()));
                    } catch (IllegalAccessException | InvocationTargetException | NoSuchBeanException | DuplicateBeanNameException | DataConversionException | DuplicateBeanClassException e) {
                        e.printStackTrace();
                    }
                }
            }
        }
    }

    private Method getSetMethod(Class<?> beanClass, Field field) {
        String fieldName = field.getName();
        Class<?> fieldType = field.getType();
        String methodName = "set" + fieldName.substring(0, 1).toUpperCase() + fieldName.substring(1);
        try {
            return beanClass.getDeclaredMethod(methodName, fieldType);
        } catch (NoSuchMethodException e) {
            e.printStackTrace();
        }
        return null;
    }

    private void createObject(Set<BeanDefinition> beanDefinitions) throws DataConversionException, DuplicateBeanNameException, DuplicateBeanClassException {
        for (BeanDefinition beanDefinition : beanDefinitions) {
            if (!beanDefinition.getLazy() && beanDefinition.getSingleton()) {        //如果是懒加载模式则先不将其放到ioc容器中
                createObject(beanDefinition);
            }
        }
    }

    private Object createObject(BeanDefinition beanDefinition) throws DataConversionException, DuplicateBeanNameException, DuplicateBeanClassException {
        Class<?> beanClass = beanDefinition.getBeanClass();
        String beanName = beanDefinition.getBeanName();
        try {
            Object object = beanClass.getConstructor().newInstance();
            //对对象的属性赋值
            Field[] fields = beanClass.getDeclaredFields(); //拿到所有的域
            for (Field field : fields) {
                //使用set方法注入标记了@Value的值
                Value annotation = field.getAnnotation(Value.class);
                if (annotation != null) {
                    String value = annotation.value();
                    String fieldName = field.getName();
                    String methodName = "set" + fieldName.substring(0, 1).toUpperCase() + fieldName.substring(1);
                    Method declaredMethod = beanClass.getDeclaredMethod(methodName, field.getType());
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
                    declaredMethod.invoke(object, val);
                }
            }
            if (beanDefinition.getSingleton()) {    //如果是单例模式则加入ioc容器中
                if (iocByName.containsKey(beanName)) {
                    throw new DuplicateBeanNameException(beanName);
                } else {
                    iocByName.put(beanName, object);       //将创建的对象存入以beanName为键的缓存中
                }
                if (iocByType.containsKey(beanClass)) {
                    throw new DuplicateBeanClassException(beanClass);
                } else {
                    iocByType.put(beanClass, object);       //将创建的对象存入以beanType为键的缓存中
                }
            } else {
                //非单例则直接返回
                return object;
            }
        } catch (InstantiationException | IllegalAccessException | InvocationTargetException | NoSuchMethodException e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * 获取某个包下的所有类
     * @param basePackages
     * @return
     */
    private Set<BeanDefinition> findBeanDefinitions(String... basePackages) throws IllegalStateException {
        Set<BeanDefinition> beanDefinitions = new HashSet<>();
        for (String basePackage : basePackages) {
            //1、获取包下的所有类
            Set<Class<?>> classes = MyTools.getClasses(basePackage);
            for (Class<?> clazz : classes) {
                //2、遍历这些类，找到添加了注解的类
                Component componentAnnotation = clazz.getAnnotation(Component.class);
                if (componentAnnotation != null) {      //如果此类带了@Component注解
                    //获取Component注解的值
                    String beanName = componentAnnotation.value();
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
                        if ("prototype".equals(value)) {        //指定为非单例模式
//                            if (lazy) {
//                                throw
//                            }
                            singleton = false;
                        } else if (!"singleton".equals(value)) { //非法值
                            throw new IllegalStateException();
                        }
                    }
                    BeanDefinition beanDefinition = new BeanDefinition(beanName, clazz, lazy, singleton);
                    beanDefinitions.add(beanDefinition);
                    allBeansByName.put(beanName, beanDefinition);
                    allBeansByType.put(clazz, beanDefinition);
                }
            }
        }
        return beanDefinitions;
    }


    @Override
    public Object getBean(String beanName) throws NoSuchBeanException, DataConversionException, DuplicateBeanClassException, DuplicateBeanNameException {
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

    @Override
    public <T> T getBean(Class<T> beanType) throws NoSuchBeanException, DataConversionException, DuplicateBeanClassException, DuplicateBeanNameException {
        Object o = iocByType.getOrDefault(beanType, null);
        if (o == null) {
            BeanDefinition beanDefinition = allBeansByType.get(beanType);
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
