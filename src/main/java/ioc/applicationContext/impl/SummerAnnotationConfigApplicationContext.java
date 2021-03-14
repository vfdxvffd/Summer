package ioc.applicationContext.impl;

import ioc.annotation.Autowired;
import ioc.annotation.Component;
import ioc.annotation.Qualifier;
import ioc.annotation.Value;
import ioc.applicationContext.ApplicationContext;
import ioc.bean.BeanDefinition;
import ioc.exception.DataConversionException;
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
    //保存此ioc容器中所有对象的beanName
    private final List<String> beanNames = new ArrayList<>();

    /**
     * 加载的时候就扫描并创建对象，日后计划加入懒加载模式
     * @param pack
     */
    public SummerAnnotationConfigApplicationContext(String pack) throws DataConversionException {
        //遍历包，找到目标类(原材料)
        Set<BeanDefinition> beanDefinitions = findBeanDefinitions(pack);
        //根据原材料创建bean
        createObject(beanDefinitions);
        //自动装载
        autowireObject(beanDefinitions);
    }

    private void autowireObject(Set<BeanDefinition> beanDefinitions) {
        for (BeanDefinition beanDefinition : beanDefinitions) {
            Class<?> beanClass = beanDefinition.getBeanClass();
            String beanName = beanDefinition.getBeanName();
            Field[] declaredFields = beanClass.getDeclaredFields();
            for (Field field : declaredFields) {
                Autowired annotation = field.getAnnotation(Autowired.class);
                Qualifier annotation1 = field.getAnnotation(Qualifier.class);
                if (annotation != null) {
                    //对标注了@Autowired的域进行赋值操作
                    String fieldName = field.getName();
                    Class<?> fieldType = field.getType();
                    String methodName = "set" + fieldName.substring(0, 1).toUpperCase() + fieldName.substring(1);
                    Method declaredMethod = null;
                    try {
                        declaredMethod = beanClass.getDeclaredMethod(methodName, fieldType);
                    } catch (NoSuchMethodException e) {
                        e.printStackTrace();
                    }
                    if (annotation1 != null) {
                        //标注了Qualifier的域，有自己的beanName
                        String qualifier = annotation1.value();
                        try {
                            declaredMethod.invoke(getBean(beanName),getBean(qualifier));
                        } catch (IllegalAccessException | InvocationTargetException e) {
                            e.printStackTrace();
                        }
                    } else {
                        //by Type
                        try {
                            declaredMethod.invoke(getBean(beanName),getBean(fieldType));
                        } catch (IllegalAccessException | InvocationTargetException e) {
                            e.printStackTrace();
                        }
                    }
                }
            }
        }
    }

    private void createObject(Set<BeanDefinition> beanDefinitions) throws DataConversionException {
        for (BeanDefinition beanDefinition : beanDefinitions) {
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
                iocByName.put(beanName, object);       //将创建的对象存入以beanName为键的缓存中
                iocByType.put(beanClass,object);       //将创建的对象存入以beanType为键的缓存中
            } catch (InstantiationException | IllegalAccessException | InvocationTargetException | NoSuchMethodException e) {
                e.printStackTrace();
            }

        }

    }

    private Set<BeanDefinition> findBeanDefinitions(String pack) {
        //1、获取包下的所有类
        Set<Class<?>> classes = MyTools.getClasses(pack);
        Iterator<Class<?>> iterator = classes.iterator();   //获取所有类的迭代器
        Set<BeanDefinition> beanDefinitions = new HashSet<>();
        while (iterator.hasNext()) {
            //2、遍历这些类，找到添加了注解的类
            Class<?> clazz = iterator.next();
            Component componentAnnotation = clazz.getAnnotation(Component.class);
            if(componentAnnotation!=null){      //如果此类带了@Component注解
                //获取Component注解的值
                String beanName = componentAnnotation.value();
                if("".equals(beanName)){    //没有添加beanName则默认是类的首字母小写
                    //获取类名首字母小写
                    String className = clazz.getName().replaceAll(clazz.getPackage().getName() + ".", "");
                    beanName = className.substring(0, 1).toLowerCase()+className.substring(1);
                }
                //3、将这些类封装成BeanDefinition，装载到集合中
                beanDefinitions.add(new BeanDefinition(beanName, clazz));
                beanNames.add(beanName);        //此容器中保存的所有的beanName
            }
        }
        return beanDefinitions;
    }


    @Override
    public Object getBean(String beanName) {
        return iocByName.getOrDefault(beanName, null);
    }

    @Override
    public Object getBean(Class<?> beanType) {
        return iocByType.getOrDefault(beanType, null);
    }

}
