# Summer使用文档

> ​		Summer —— 一个参考Spring框架，并计划在其上做出一些创新的Java框架。接触到Spring框架后被其IOC和AOP对业务的解耦效果吸引，所以为了深入学习决定以Spring为参考，实现一个类似框架，开发过程中计划根据自己的理解加入一些创新。

## IOC

​		IOC——控制反转，旨在将对象之间的依赖关系和对象的创建过程都交由一个第三方的角色来管理，这个第三方就是`IOC容器`。对象创建的控制权发生了反转，通过IOC我们不必再自己去控制对象如何创建。

​		我们设想一个平时的很常见的一个设计场景，很多人开发一个项目都喜欢`dao、service、controller`一把梭（只是做一个引子，并无涵盖所有人或者不认同此分层设计的意思），而这里面：

- `dao（Data Access Object）`意为“数据访问对象”，指的是直接操作对象，一般为直接和数据库的交互层。

- `service`层表示服务层，不只对数据库的简单操作，还需要包含一些逻辑处理，比如判断要插入的数据是否合法，控制事务保持一致性等等。
- `controller`层表示控制层，这层负责根据用户的业务需求调用相应服务来完成业务。

​		以上三层相互协作，当一个用户请求发过来后，我们controller拿到用户请求，根据请求的业务需求调用不同的service来相互配合完成任务，而service又要调用相应的dao来对数据库进行数据的`CRUD`，以完成最终的业务需求。

​		而上述听起来很容易的几句话，就是三层之间相互配合，但是当业务逐渐复杂起来，假如一个controller需要十几二十个service对象，而一个service又需要十几二十个dao对象，这样一个controller对象所依赖的对象是十分多的。又或是某一个对象的创建过程非常复杂，通常需要很多个步骤才能创建完成，这种情况下如果使用传统的自己`new`对象的方式，那对象之间的依赖关系将会十分复杂，很难管理起来。所以就有了IOC这个第三方容器来帮助管理对象之间依赖。

### 如何将对象交由IOC容器管理

​		我们只需将需要交由IOC容器管理的类加上诸如`@Component`、`@Controller`、`@Service`、`@Repository`这样的注解就可以将其交由IOC容器来管理。加入IOC容器后我们将其称为一个一个的`bean`。

​		上述几个注解其实功能都是一样的，但是通常为了代码可读性，我们将对应的注解标到对应的分层上。

​		还可以选择性的通过向注解中传入一个字符串来为这个bean起一个名字，bean的名字默认为类的名字首字母小写。如下示例：

```java
@Component
public class Book {
}

@Component("book")
public class Book {
}
```

### 如何为bean中的基本类型及其包装类赋值

​		我们可以选择性向bean中的基本类型赋一些初始值，使用`@Value`注解可以轻松办到，注解中传入一个`String`类型的参数，Summer会自动将这个字符串转成对应类型的值并赋给对应的域。底层采用JDK提供的转化函数（如`Integer.parseInt(str)`）进行转化，如果不能成功转化，则会抛出`DataConversionException`异常。如下示例：

```java
@Component
public class Book {

    @Value("西游记")
    private String bookName;

    @Value("TC312C")
    private String isbn;

    @Value("88.88")
    private Double price;

    @Value("8")
    private Integer number;

    @Value("吴承恩")
    private String author;
}
```

​		当然你可以选择不标注`@Value`注解，而是给这个域赋上默认值（null）。

### 如何给bean中的对象赋值

​		基本类型及其包装类可以通过`@Value`注解赋值，那对于一些其他对象呢？通常这些对象需要是交由IOC容器管理的对象，我们通过`@Autowired`注解为这些对象赋值，这个注解会去IOC中找这个类型的bean，找到后将赋值给它，或者我们可以通过使用`@Qualifier("")`注解指定要注入的bean的`beanName`，如果找不到相应的bean，将会抛出`NoSuchBeanException`异常。如下示例：

```java
@Component
public class Student {

    @Value("812738728")
    private String stuNumber;

    @Value("某某")
    private String name;

    @Value("20")
    private Integer age;

    @Value("118")
    private String classRoom;

    @Autowired
    @Qualifier("book")
    private Book book;
}
```

### 如何控制bean在IOC中是单例模式还是原型模式

​		Summer默认每个bean都是单例模式，如果是单例模式，你通过IOC容器获取无数次bean，它们的`==`运算返回结果将永远是`true`，但是你也可以通过一些注解来使其变为非单例模式。非单例模式下，你的每次获取都会得到一个不同的对象，而且这个对象永远不会被加入到IOC容器中。`@Scope`注解很乐意完成这个任务，默认采用单例模式，但如果使用到`@Scope`注解，你可以通过传入一个String类型的参数来指定是否单例。

​		`@Scope("prototype")`表示为非单例模式，而`@Scope("singleton")`表示为单例模式，如果输入非法字符串将会抛出异常。如下示例：

```java
@Service("service")
@Scope("prototype")
public class StudentServiceImpl implements StudentService {

    @Autowired
    private StudentDao studentDao;
}
```

### 如何控制bean转载入IOC的时机

​		你可以在容器初始化的时候将所有标有注解的bean装载如IOC中，这是默认的。当然如果你觉得这样启动太耗费时间，你也可以设置`延迟加载`，这样的话只有当这个bean第一次被使用的时候才会被加载入IOC容器中。设置延迟加载，或者称为`懒加载`，可以使用`@Lazy`注解。如下示例：

```java
@Service
@Lazy
public class BookServiceImpl implements BookService {

    @Autowired
    private BookDao bookDao;
}
```

### 如何获取一个bean

​		要从IOC中获取到一个bean，通过需要先拿到IOC容器，通过`applicationContext`接口来接收这个IOC, 使用下述代码：

```java
ApplicationContext ioc = new SummerAnnotationConfigApplicationContext("com.vfd.summer");
```

​		可以看出我们需要传入一个字符串，这个字符串是一个包名，表示了IOC容器的作用域，IOC只可以去扫描和控制这个包下的所有类。

​		`ApplicationContext`接口暂时只提供了三个方法来获取bean，一个是通过`beanName`，一个是通过`beanType`，还有一个同时传入`beanName`和`beanType`。以及一些其他的获取`IOC`容器信息属性的方法。

```java
public interface ApplicationContext {

    /**
     * 根据bean的name获取对象
     */
    Object getBean(String beanName) throws Exception;

    /**
     * 根据bean的类型获取对象
     */
    <T> T getBean(Class<T> beanType) throws Exception;

    /**
     * 同时根据bean的类型和name获取对象，若一个接口多个实现类，则可以通过接口类型和name去获取
     */
    <T> T getBean(String name, Class<T> beanType) throws Exception;

    /**
     * 根据name获取相应的类型
     */
    Class<?> getType(String name) throws NoSuchBeanException;

    /**
     * 根据类型获取该类型对应的所有bean,比如一个接口和它的所有实现类，用map返回<beanName, Object>
     */
    <T> Map<String, T> getBeansOfType(Class<T> beanType) throws Exception;

    /**
     * 获取BeanDefinition的数量，即ioc中管理的所有类的数量
     */
    int getBeanDefinitionCount();

    /**
     * 获取所有BeanDefinition的name属性
     */
    String[] getBeanDefinitionNames();

    /**
     * 判断是否存在name为传入参数的bean
     */
    boolean containsBean(String name);

    /**
     * 判断是否存在name为传入参数的BeanDefinition
     * 与上一个方法不同的是上一个判断的是实际的对象，而这个方法判断的是BeanDefinition
     * 若某个类为原型模式（非单例），则它的对象不会存储在ioc容器中，但BeanDefinition是一直存在的
     */
    boolean containsBeanDefinition(String beanName);
}

```

​		我们可以通过调用上述两个接口来获取对象：

```java
StudentController controller = ioc.getBean(StudentController.class);
BookController bookController = (BookController) ioc.getBean("bookController");
```

ps：我们可以通过接口来获取实现类的bean，因为ioc中无法保存接口的实例，或者说接口没有实例。

​		这个获取到的对象，不管其中有多么复杂的依赖，只要它的依赖都在ioc的控制范围内，且标了相应正确的注解，那么它里面的依赖关系就全由ioc容器处理好了。只需要直接调用使用它即可。

### 如果想容器中加入一个配置类

​		使用`@Configuration`注解标注在配置类上，在配置类上使用方法，在方法上标注`@Bean`注解，则会将方法的返回值作为bean加入到ioc容器中。

​		`@Configuration`注解有一个参数`proxyBeanMethods`指示是否需要代理该配置类，默认为true即需要代理，如果为true，则该配置类中的标注了`@Bean`的方法的返回值为单例模式，如果为false则为原型模式。

​		对于`@Configuration`和`@Bean`两个注解，指定`value`的值来为配置类设置`beanName`，如果缺省则默认为类名首字母小写，指定`name`的值来为方法返回值的`bean`指定`beanName`，如果缺省则为方法名。

```java
@Configuration (proxyBeanMethods = true)
public class MyConfig {

    @Bean(name = "book")			// 缺省name则beanName为book1
    public Book book1 () {
        return new Book("三国演义", "a-1");
    }
}
```

## AOP

​		AOP——面向切面编程，不同于面向对象（OOP），AOP更加关注的是一个方法的切面。假如我们此时有一个日志记录的需求，日志需要加入到一段业务中去，如果业务开始阶段、结束阶段和抛出异常的时候要加入相应的日志记录，如果我们直接在业务方法中添加日志，那么日志和核心的业务代码就会耦合在一起，代码可读性差，不方便日后维护。AOP可以使我们无侵入地将日志的记录穿插在业务过程中。虽然日志记录的代码和核心业务的代码不在一个地方，但它们确实一起穿插执行了。

​		AOP的核心其实还是使用代理来实现，我们对一段业务函数进行代理， 在代理的类中，对它执行的`开始`、`返回`、`异常`、`结束`阶段分别做切入，插入日志的记录，真正执行的时候我们执行的并不是执行的真实对象的方法，而是代理对象的方法，实际上被切的类在IOC中保存的并不是实际的对象，而是代理的对象。

​		项目初期采用了JDK的动态代理来实现此功能，所以强制要求被切的方法的类需要实现一个接口。

​		ps:  2021/04/10 完成了对`cglib`方式的添加，现在对于实现了接口的类采用JDK的动态代理，对于没有实现接口的类采用cglib。

​		面向切面编程有三个必需的元素：切面类（切面类中有切面方法，或者称为通知方法）、目标方法（被切的方法）、以及切入点。

### 如何将一个类设置为切面类

​		通过一个`@Aspect`注解可以将一个类设置为切面类，启动的过程中会扫描所有带有此注解的类，并将它们中的`通知方法`保存起来，留作后用。也就是只有标注了`@Aspect`注解的类中的通知方法才能被感知到。如下示例：

```java
@Aspect
@Component
public class AspectDemo {
    
}
```

​		切面类通常也需要作为组件出现在IOC容器中，因为需要它的实例对象来执行`通知方法`，目前版本（v1.2）中需要额外添加`@Component`注解来将其添加进去，后面计划直接将带有`@Aspect`注解的类直接加入IOC。

### 如何定义通知方法，并设置此方法的切入点

​		切入点分为四种，`前置通知`、`返回通知`、`异常通知`、`后置通知`。我们分别使用`@Before`、`@AfterReturning`、`@AfterThrowing`、`@After`四个注解来标识它们。也只有标注了这四个注解的方法被称为通知方法。而每个注解也相应地标识了它们的切入点。

​		此通知方法需要去切的目标方法就在那四个注解中以`String`的形式传入，需要传入全方法名，方法的参数也需要传入参数类型的全类名。如下示例：

```java
@Aspect
@Component
public class AspectDemo {

    @Before("com.vfd.controller.impl.StudentControllerImpl.getStudent(java.lang.Integer, java.lang.Integer)")
    public void logBefore() {
        System.out.println("this is before");
    }

    @After("com.vfd.controller.impl.StudentControllerImpl.getStudent(java.lang.Integer, java.lang.Integer)")
    public void logAfter() {
        System.out.println("this is after");
    }
    
    @AfterThrowing("com.vfd.controller.impl.StudentControllerImpl.getStudent(java.lang.Integer, java.lang.Integer)")
    public void logThrowing() {
        System.out.println("this is throwing");
    }

   @AfterReturning("com.vfd.controller.impl.StudentControllerImpl.getStudent(java.lang.Integer, java.lang.Integer)")
    public void logReturning() {
        System.out.println("this is return");
    }
}
```

​		上述表示这些方法分别去切`com.vfd.controller.impl`包下的`StudentControllerImpl`类中的`getStudent`方法的`前置通知`、`返回通知`、`异常通知`、`后置通知`，方法的两个参数都是`java.lang`包下的`Integer`类。前提得保证目标方法的类也在IOC容器中。

### 如何让通知方法接收参数

​		对于四种通知有不同的参数被允许接收。

- `前置通知`可以接收一个`JoinPoint`对象，通过这个对象可以获取到目标方法的`参数列表`、`方法名`、`方法返回值类型`。
- `返回通知`可以接收一个`JoinPoint`对象，作用和上述一样。同时可以接收一个`Object`类型的参数，用来接收目标方法的返回值。
- `异常通知`可以接收一个`JoinPoint`对象，作用同上。可以接收一个`Throwable`类型的对象，表示目标方法抛出的异常。
- `后置通知`也仅接收一个`JoinPoint`对象，作用同上。

示例如下：

```java
@Aspect
@Component
public class AspectDemo {

    @Before("com.vfd.controller.impl.StudentControllerImpl.getStudent(java.lang.Integer, java.lang.Integer)")
    public void logBefore(JoinPoint joinPoint) {
        System.out.println("this is before");
        if (joinPoint != null) {
            System.out.println(joinPoint);
        }
    }

    @AfterReturning("com.vfd.controller.impl.StudentControllerImpl.getStudent(java.lang.Integer, java.lang.Integer)")
    public void logReturning(Object o) {
        System.out.println("this is return");
        if (o != null) {
            System.out.println(o);
        }
    }

    @AfterThrowing("com.vfd.controller.impl.StudentControllerImpl.getStudent(java.lang.Integer, java.lang.Integer)")
    public void logThrowing(Throwable e) {
        System.out.println("this is throwing");
        if (e != null) {
            e.printStackTrace();
        }
    }

    @After("test.controller.impl.StudentControllerImpl.getStudent(java.lang.Integer, java.lang.Integer)")
    public void logAfter(JoinPoint joinPoint) {
        System.out.println("this is after");
        if (joinPoint != null) {
            System.out.println(joinPoint);
        }
    }
}
```

