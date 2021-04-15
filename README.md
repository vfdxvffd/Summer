# Summer

[![](https://img.shields.io/badge/Release-v0.5-orange)](https://github.com/vfdxvffd/Summer/releases/tag/v0.5) &nbsp;&nbsp;&nbsp;[![](https://img.shields.io/badge/%E4%BD%BF%E7%94%A8%E6%96%87%E6%A1%A3-Summer-informational)](Summer使用文档.md) &nbsp;&nbsp;&nbsp;[![](https://img.shields.io/badge/%E6%9B%B4%E6%96%B0%E6%97%A5%E5%BF%97-log-important)](Summer更新日志.md)

​		参考`Spring`框架实现一个简易类似的`Java`框架。计划陆续实现`IOC`、`AOP`、以及`数据访问模块和事务控制模块`。项目持续维护中...欢迎Star！Thanks~~~

项目计划：

- [x] IOC容器
- [x] AOP切面
- [ ] 数据访问集成模块（JDBC、事务控制）

​		关于对IOC和AOP功能`为什么要使用（why）`，以及`应该如何使用（how）`请移步[使用文档](Summer使用文档.md)，要了解每个版本更新的内容请移步[更新日志](Summer更新日志.md)。

## 运行环境

JDK 8

## 如何使用

​		下载最新的jar包[![](https://img.shields.io/badge/Release-v0.5-orange)](https://github.com/vfdxvffd/Summer/releases/tag/v0.5) ，将其导入项目中，即可使用，目录结构如下图，蓝色框内为`summer`的核心代码，`ch`包下为`logback`日志依赖，`net.sf.cglib`下为cglib动态代理的依赖，`org.slf4j`下为`slf4j`的日志门面依赖。

![](img/2021-04-11_00-45.png)

## Version 0.5(Pre-release)

<center><b><font size = "6" color = "red">一次重大更新</font></b></center>

>  bug描述：循环依赖的问题复现出来		

​		因为之前`v0.1`更新中引入的一个解决bug的方法导致了这个重大的bug，这次通过设置二级缓存来解决`循环依赖`的问题，具体bug的产生原因详情可见[更新日志](Summer更新日志.md)，更新日志对这次bug的出现原因以及解决方法做了详细的说明。

**bug解决：**

​		针对目前掌握的代理方面的知识，对之前的做法做出一些调整。设置二级缓存，一级缓存一个（即真正的ioc容器），二级缓存两个，都是负责存放实例化但未初始化的对象，但一个是存放原对象，另一个负责存放代理对象，二级缓存的示意图如下：

![](img/summerDoubleCache.svg)

将ioc容器的构造过程分为四步来进行：

1. 遍历包，找到所有需要被IOC管理的类，封装成`BeanDefinition`
2. 根据第一步获取到的`BeanDefinition`实例化那些单例且非延迟加载的对象，并将其加入到二级缓存的`earlyRealObjects`中
3. 对第二步得到的`earlyRealObjects`中的对象进行检查，看是否需要设置代理，如果需要则对其进行代理，并将代理对象加入到二级缓存中的`earlyProxyObjects`中（并不删除`earlyRealObjects`中对应的真正的对象）。
4. 对第二步得到的`earlyRealObjects`中的对象进行注入工作（即开始进行初始化），检查每个对象的每个域，如果标注了`@Autowired`注解且值为`null`，则对其进行注入工作，现在一级缓存中查找，如果有直接取出为其注入，如果没有检查二级缓存的`earlyProxyObjects`，如果有则取出为其注入，如果没有则接着检查二级缓存的`earlyRealObjects`，找到后为其注入，此时如果还没有则说明这个域对应的bean是非单例（prototype）模式或者懒加载模式的，则为其实例化并设置代理（如果需要），并初始化，然后注入其中。如果是非ioc容器管理的域，则直接注入`null`，也可以考虑改为抛出异常给用户提示。

## Version 0.4(Pre-release)

本次更新加入了新功能，修改了一个已知的bug

- 本次更新引入`CGLib`依赖，增加动态代理的方式，对于实现了接口的方法采用`JDK`动态代理来实现切面功能，对于没有实现接口的类采用`CGLib`来实现切面。

    ![](img/2021-04-11_00-39.png)

- 修改bug，之前版本中的`判断当前类是否已经完成了实例对象全部的创建注入工作`的方法，判断没有包含所有情况。

    > bug描述：对于一个没有任何域`且`需要代理的对象，进行注入工作的时候会由于没有域需要注入，从而直接判断其已经完成注入，而跳过了代理阶段。

## Version 0.3(Pre-release)

- 本次更新引入了日志依赖，增加了对ioc构造过程中的日志记录

    ![](img/2021-03-22_15-12.png)

- 对于标注了`@Aspect`注解的类自动将其加入IOC容器中，不用再重复标注注解

## Version 0.2(Pre-release)

本次更新加入了一些新功能，修复了一些bug

1. 更新功能：

    - aop增加了一种切入方式，目前有以下切入方式

        `@Before`、`@AfterReturning`、`@AfterThrowing`、`@After`

        以上对应的切入时机如下：

        ```java
        try {
            @Before
            fun.invoke();
            @AfterReturning
        } catch (Throwable t) {
            @AfterThrowing
        } finally {
            @After
        }
        ```

    - 切面方法可以通过`JoinPoint`类获取被切的方法的参数、方法名、返回值类型。对于`@AfterReturning`的切入方式可以获取返回值，类型为`Object`，而`@AfterThrowing`可以获取抛出的异常，类型为`Throwable`。

2. 修复了重复代理的bug

    > bug描述：当一个待注入bean中有超过一个需要注入的域（带有注解@Autowired且未完成赋值），如果对它中的方法进行切面，这时切面方法会重复执行

## Version 0.1(Pre-release)

​		本次更新主要修复了一些bug，以及优化了代码的结构

1. 修复对于注入对象的切面方法失效的bug

    > bug描述：在controller中注入service，但是如果有对于service的切面方法，则切面方法无法被调用

2. 修复延迟加载的对象注入失败的bug

    > bug描述：对于标注了延迟加载的类注入时会发生异常

3. 修复对非单例的bean注入失败的bug

    > bug描述：对于标注了非单例的类注入时会发生异常，且会调用多次构造函数的问题

4. 增加核心代码的注释。

5. 优化代码结构，重构了大部分冗余的代码块

6. 抽取可重用方法。

## Version 0.0(Pre-release)

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
