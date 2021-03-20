# Summer

[![](https://img.shields.io/badge/Release-v1.1-orange)](https://github.com/vfdxvffd/Summer/releases/tag/v1.1) &nbsp;&nbsp;&nbsp;

参考Spring框架实现一个简易类似的Java框架
项目计划：

* IOC容器
* AOP切面

## Version 1.0

1. 完成IOC容器的初步搭建

2. 完成AOP功能的简单使用（还需修改）

3. 支持`@Component`、`@Autowired`、`@Qualifier`、`@Value`、`@Repository`、`@Service`、`@Controller`注解的使用
    - @Component（同@Respository、@Service、@Controller）：标注在类上，将此类注册到ioc容器中
    - @Autowired：自动注入ioc容器中的对象
    - @Qualifier：自动注入ioc中对象的时候指定`beanName`，如不指定则按照`beanType`注入
    - @Value：指定将类注入到容器是基本类型（包括包装类）字段的值

4. 支持根据`beanName`、`beanType`获取ioc中的对象

5. 自定义类型转化异常，`@Value`接受`String`类型，如果传入的值并不能正确转化，就抛出`DataConversionException`异常。

6. 增加单例模式与非单例模式的配置注解`@Scope`，以及增加延迟加载的配置注解`@Lazy`

7. 可以使用接口来接受IOC中返回的对象

8. AOP可以对方法进行`@Before`、`@After`、`@AfterThrowing`的切面，需要配置方法的全方法名

9. AOP使用JDK的动态代理，`set`可以不添加，内部实现是直接通过设置域的可访问属性，然后直接设置值

10. 后续计划：
    - 支持根据`xml`配置ioc容器中的对象
    - 对于运行过程可能发生的异常使其尽可能可控，且明确的抛出或处理
    - 对于AOP可选择性的加入`CGLIB`代理
    - 对于AOP一些已注入对象的代理失效bug进行修复（已定位）
    - etc...   for more...


## Version 1.1

​		本次更新主要修复了一些bug，以及优化了代码的结构

1. 修复对于注入对象的切面方法失效的bug

    > bug描述：在controller中注入service，但是如果有对于service的切面方法，则切面方法无法被调用

    **分析：**

    ​		因为在之前的IOC容器初始化时的顺序是：`获取所有标了注解的类并保存起来——>创建对象并对标注了@value的域赋值——>用容器中的bean注入标注了@Autowired的对象属性——>将被切面切的类包装为代理对象重新覆盖IOC容器中的bean`

    ​		从上述的顺序其实可以看出，在第三步已经将service注入了controller中，第四步再更新容器中的service（将其变为代理对象）已经改变不了controller中的service了，而且更重要的是：**破坏了单例模式**。所以这属于是一个极为严重的bug.

    ​		对其做出以下修正：改变IOC容器的初始化顺序，第一步和第二步不变，将第三步和第四步混合为一步。对一个对象的域进行注入时（比如对controller中的service），先检查该对象（controller）是否已经完成了注入，如果完成了则直接继续循环其他对象。如果没完成，则循环检查它的每个标注了`@Autowired`的域对应的对象（service）是否已经完成了所有注入工作，如果没完成就递归调用`注入方法`为该域所对应的对象进行注入工作，如果完成了就对该对象（controller）的每个域进行注入工作。对该对象（controller）注入完成后检查它是否需要被包装为代理对象，如果需要则将包装的代理对象覆盖如IOC容器，如果不需要就直接加入IOC容器。

    ```java
    public void autoWired(Set<Class<?>> classes) {
        for (Class<?> clazz : classes) {
            autoWired(clazz);
        }
    }
    
    public void autoWired(clazz) {
        if (clazz 已经完成了注入工作) {
            return;
        }
        for (Field field : clazz中所有标注@AutoWired的域) {
            if (field.getType()未完成注入工作)	{	//即容器中还没有完整的该类的bean
                autoWired(field.getType());
            }
            set(field, ioc.getBean(field.getType()));
        }
        if (需要包装为代理对象) {
            Object obj = getProxyObj(clazz);
            ioc.put(obj);		//覆盖
        } else {
            ioc.add (clazz.toObj());
        }
    }
    ```

2. 修复延迟加载的对象注入失败的bug

    > bug描述：对于标注了延迟加载的类注入时会发生异常

    **分析：**

    ​		因为有一个方法将接口转化为实现类对象，之前是在ioc容器中找实现类，而对于延迟加载的类来说，ioc中初始并没有该类，所以获取不到，返回null就抛出异常。

    ​		之后修改不从ioc容器中找，而是从整个标注了组件表示的类中找，避免了此bug。

3. 修复对非单例的bean注入失败的bug

    > bug描述：对于标注了非单例的类注入时会发生异常，且会调用多次构造函数的问题

    **分析：**

    ​		判断某个域是否完成注入的时候会调用`Proxy.isProxyClass(getBean(beanClass).getClass()`方法判断是否为代理类，对于`非单例`的bean调用`getBean`的时候回去创建bean，而创建bean的时候又会来判断是否已经完成注入，这样造成循环调用出现异常。

    ​		修改bug：需要在判断是否为代理对象前判断是否为`非单例`的bean,如果是直接返回`未完全注入所有域`。

    ​		对于调用多次构造函数的问题，举个例子：controller里面有service域为`非单例`需要注入，将controller保存到容器的时候回去递归注入service，但这时候调用的`autowireObject (BeanDefinition beanDefinition)`**在创建完bean之后并不会将其装入IOC容器中**，因为它默认传入的是上次调用`createObject(BeanDefinition beanDefinition)`时已经装入容器的对象，所以调用结束后这个对象相当于没有创建（未装入容器，也未保存）。而当运行到下述代码时候（即将刚刚创建的service注入到controller中），调用`getBean`方法发现IOC中并没有期望的bean，所有又会去创建，此时会以`非单例`模式创建，所以这个对象又会被创建一次。

    ```java
    //by Type
    try {
        field.setAccessible(true);
        field.set(getBean(beanDefinition.getBeanName()), getBean(field.getType()));
    } catch (Exception e) {
        e.printStackTrace();
    }
    ```

    修改bug：对于`autowireObject (BeanDefinition beanDefinition)`，如果传入的是`非单例`模式的则不予创建对象，直接返回，因为后面`getBean`被调用的时候还会创建bean，而且是以`非单例`的方式，更加合适。

    ps：非单例创建bean的方法我贴在下面：

    ```java
    //非单例则不需要将其加入ioc容器
    Object object = createObject(beanDefinition);
    if (object != null) {
        autowireObject(object);
    } else {
        return null;
    }
    return (T) object;
    ```

4. 增加核心代码的注释。

5. 优化代码结构，重构了大部分冗余的代码块

6. 抽取可重用方法。