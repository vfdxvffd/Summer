# Summer

参考Spring框架实现一个简易类似的Java框架
项目计划：
* IOC容器
* AOP切面
* 事务控制

## Version 1.0

1. 完成ioc容器的初步搭建
2. 支持`@Component`、`@Autowired`、`@Qualifier`、`@Value`注解的使用
    - @Component：标注在类上，将此类注册到ioc容器中
    - @Autowired：自动注入ioc容器中的对象
    - @Qualifier：自动注入ioc中对象的时候指定`beanName`，如不指定则按照`beanType`注入
    - @Value：指定将类注入到容器是基本类型（包括包装类）字段的值
3. 支持根据`beanName`、`beanType`获取ioc中的对象
4. 自定义类型转化异常，`@Value`接受`String`类型，如果传入的值并不能正确转化，就抛出`DataConversionException`异常。
5. 后续计划：
    - 支持根据`xml`配置ioc容器中的对象
    - 对于运行过程可能发生的异常使其尽可能可控，且明确的抛出或处理
    - etc...   for more...