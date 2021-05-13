# 如何适配Summer

​		Summer是一款为了应用程序各个部件之间解耦合而诞生的`Java`框架，通过`IOC`的容器化管理对象，很方便的解决了各个对象之间依赖问题，同时通过`AOP`可以在外部无侵入式地将一些日志记录，权限控制等等和核心业务无关的操作插入代码中。

​		为了打造Summer的生态，所以需要Summer能被其他应用场景的框架适配，从而达到更好的扩展性。所以在[![](https://img.shields.io/badge/Release-v1.0-important)](https://github.com/vfdxvffd/Summer/releases/v1.0)的更新中，Summer对外开放了扩展接口`Extension`，通过有选择性地实现此接口中的八个方法，可以在`IOC`容器构造的各个阶段插入需要扩展的代码，从而使Summer可以和其他应用框架适配起来。

`Summer`对外提供的`Extension`接口如下定义：

```java
/**
 * @PackageName: com.vfd.summer.extension
 * @ClassName: Extension
 * @Description: 为此框架的扩展向外提供一个接口
 * @author: vfdxvffd
 * @date: 2021/5/12 下午1:28
 */
public interface Extension {

    void doOperation0 (SummerAnnotationConfigApplicationContext context) throws Exception;

    void doOperation1 (SummerAnnotationConfigApplicationContext context) throws Exception;

    void doOperation2 (SummerAnnotationConfigApplicationContext context) throws Exception;

    void doOperation3 (SummerAnnotationConfigApplicationContext context) throws Exception;

    void doOperation4 (SummerAnnotationConfigApplicationContext context) throws Exception;

    void doOperation5 (SummerAnnotationConfigApplicationContext context, BeanDefinition beanDefinition) throws Exception;

    void doOperation6 (SummerAnnotationConfigApplicationContext context, Object o) throws Exception;

    void doOperation7 (SummerAnnotationConfigApplicationContext context, Object o) throws Exception;

    void doOperation8 (SummerAnnotationConfigApplicationContext context, Object o) throws Exception;
}

```

接口的方法会在`ioc`容器构造的各个阶段进行织入，织入的时机如下：

```java
/**
     * 带有扩展性的构建ioc容器
     * @param propertyFile 类路径下的配置文件
     * @param extensions 外部扩展对象的列表
     * @param basePackages 需要被ioc管理的包
     */
public SummerAnnotationConfigApplicationContext(String propertyFile, List<? extends Extension> extensions, String... basePackages) throws Exception {
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
    //容器初始化日志
    logger.info("IOC容器初始化完成");
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
```

​		在`ioc`初始化之前，扫描包结束之后，bean的实例化结束之后，bean的代理设置完成之后，bean的初始化完成之后这五个阶段，以及对于`原型模式`或者`懒加载`的bean，在对象实例化之前，对象设置代理之前，对象初始化之前以及最终构造完成的时候这四个阶段。

​		实现扩展接口的类的方法可以获得`SummerAnnotationConfigApplicationContext`对象，通过插入自己的代码，修改其中的多级缓存中的内容、`ioc`容器、`beanDefenation`等等内容，即可实现对`Summer`的扩展。

## 示例

​		这里我们通过一个例子来说明具体如何适配。以我的另一个项目`vrpc`为例。

​		`vrpc`是一款`rpc`框架，底层采用`netty`进行网络通信，通过动态代理生成接口的代理对象，接口的具体实现类可能在远程某个主机上，在调用时调用代理类的方法，向远程主机发送请求信息，在远程执行对应对象的方法后，将执行结果再通过`netty`发送响应消息，最终得到远程执行的结果。

​		`vrpc`项目的具体内容可以查看仓库：[https://github.com/vfdxvffd/vrpc](https://github.com/vfdxvffd/vrpc)

## 适配vrpc到Summer

`Summer`和`vrpc`的适配后的仓库在：[https://github.com/vfdxvffd/summer-vrpc](https://github.com/vfdxvffd/summer-vrpc)，这里只做简要的说明。

### 容器构造过程中代码的织入

`vrpc`主要是对于需要远程调用的对象通过`@Reference`注解，通过指定远程主机的`ip`地址和端口号，以及消息传输过程中的序列化方式（可选）来创建代理对象注入到`IOC`容器中。既然是对对象域的操作，那就要等到对象实例化之后，对象初始化之前进行，我们选取了`doOperation3`这个时机进行对对象中引用远程对象的域的初始化工作。

```java
// 对二级缓存中标注了@Reference注解的域进行注入
@Override
public void doOperation3 (SummerAnnotationConfigApplicationContext context) throws Exception {
    for (Map.Entry<String, Object> objectEntry : context.getEarlyRealObjects().entrySet()) {
        referenceObject(objectEntry.getValue());
    }
    logger.info("远程调用的代理对象设置完成");
}

// 这个函数就是对具体某个对象进行检查以及对其标注了@Reference的域进行初始化，具体实现可以忽略
private void referenceObject (Object object) throws Exception {
    final Class<?> clazz = object.getClass();
    for (Field field : clazz.getDeclaredFields()) {
        final Reference reference = field.getAnnotation(Reference.class);
        if (reference != null) {
            field.setAccessible(true);
            if (field.get(object) != null) {
                continue;
            }
            String host = "".equals(reference.host())? Config.getDestHost():reference.host();
            int port = reference.port() == -1? Config.getDestPort():reference.port();
            Serializer serializer = reference.serializer()==Serializer.class? Config.getDestSerializer():reference.serializer().newInstance();
            final Object remoteObj = getRemoteObj(host, port, serializer, reference.beanName(), field.getType());
            field.set(object, remoteObj);
        }
    }
}
```

通过接口获取到`context`对象，通过`context`对象拿到二级缓存保存实际对象的容器，遍历这个容器，检查如果有对象的域标注了`@Reference`注解，就对其进行代理对象的设置，并赋值回这个对象的域。

### 原型模式和懒加载模式的对象

对于原型模式或者懒加载模式的对象，由于不会在容器构造的时候就进行创建，所以需要在`createBean`方法中进行织入，同样选择对象实例化结束以及初始化之前的时机`doOperation7`。

```java
// 对此对象进行检查，如果其中的域包含了@Reference注解，则对其进行注入
@Override
public void doOperation7 (SummerAnnotationConfigApplicationContext context, Object o) throws Exception {
    referenceObject(o);
}
```

### 如何向IOC容器中插入操作的句柄

由于用户可能需要有选择性的控制连接的开启和关闭，所以需要为用户的操作提供一个句柄，例如spring中对于`jdbc`的`JdbcTemplate`，我们这里的操作句柄为`VRpcHandler`，这个需求其实很简单，只是向`IOC`容器中加入一个对象，并将此`beanType`和`beanName`对应起来即可，通过获取`context`的ioc容器即可操作，这个操作的时机可以选择在IOC容器构建之前，也可以选择在构建之后，都是比较合适的时机。这里我们选择`doOperation2`这个时机。

```java
// 把控制、操作、修改rpc配置的句柄装配到ioc容器中
@Override
public void doOperation2 (SummerAnnotationConfigApplicationContext context) {
    context.getIocByName().put("vRpcHandler", new VRpcHandler(this));
    final HashSet<String> set = new HashSet<>();
    set.add("vRpcHandler");
    context.getBeanTypeAndName().put(VRpcHandler.class, set);
}
```

### rpc在远程调用的时候如何向外提供服务

既然要向外提供服务，那么就应该等本地IOC容器构建完成之后，这时对象都交由IOC容器来管理，且已经全部构建完毕，我们可以选择`doOperation4`这个时机。

```java
// 开启服务端监听端口，对外提供服务
@Override
public void doOperation4 (SummerAnnotationConfigApplicationContext context) throws Exception {
    if (!Config.providerServer()) {
        return;
    }
    int port = Config.getServerPort();
    final Serializer serializer = Config.getServerSerializer();
    provide0(port, serializer);
}
```

至此就已经完成了`vrpc`对`Summer`的适配，构建`Summer`时只需要传入`vrpc`的对象实例即可，参考如下的构建示例：

```java
public class APP {
    public static void main(String[] args) throws Exception {
        ApplicationContext ioc = new SummerAnnotationConfigApplicationContext(
            "/application.properties" , Collections.singletonList(VRpcAdapter.class.newInstance()), "com.vfd.service"
        );
        final VRpcHandler handler = ioc.getBean(VRpcHandler.class);
        handler.startProvideServer(9090);
    }
}

```

最后，如果您对`Summer`感兴趣，欢迎将您自己的框架适配到`Summer`，或者让我和您一起构建！