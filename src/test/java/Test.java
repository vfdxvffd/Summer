import applicationContext.ApplicationContext;
import applicationContext.impl.SummerAnnotationConfigApplicationContext;
import ioc.exception.DataConversionException;
import ioc.exception.DuplicateBeanClassException;
import ioc.exception.DuplicateBeanNameException;
import ioc.exception.NoSuchBeanException;

/**
 * @PackageName: PACKAGE_NAME
 * @ClassName: Test
 * @Description:
 * @author: vfdxvffd
 * @date: 2021/3/14 下午3:14
 */
public class Test {

    @org.junit.Test
    public void test() throws NoSuchMethodException, NoSuchBeanException, DuplicateBeanNameException, DuplicateBeanClassException, ClassNotFoundException, DataConversionException {
        ApplicationContext ioc = new SummerAnnotationConfigApplicationContext("ioc.test");
//        Person person = ioc.getBean(Man.class);
//        person.say("hello world", 3);
//        System.out.println("=============");
//        person.say();
    }

    @org.junit.Test
    public void test01() throws NoSuchMethodException {
    }
}
