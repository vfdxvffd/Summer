import com.vfd.summer.applicationContext.ApplicationContext;
import com.vfd.summer.applicationContext.impl.SummerAnnotationConfigApplicationContext;
import com.vfd.summer.ioc.exception.DataConversionException;
import com.vfd.summer.ioc.exception.DuplicateBeanClassException;
import com.vfd.summer.ioc.exception.DuplicateBeanNameException;
import com.vfd.summer.ioc.exception.NoSuchBeanException;

/**
 * @PackageName: PACKAGE_NAME
 * @ClassName: Test
 * @Description:
 * @author: vfdxvffd
 * @date: 2021/3/14 下午3:14
 */
public class Test {

    @org.junit.Test
    public void test() throws NoSuchMethodException, NoSuchBeanException, DuplicateBeanNameException, DuplicateBeanClassException, ClassNotFoundException, DataConversionException, IllegalAccessException {
        ApplicationContext ioc = new SummerAnnotationConfigApplicationContext("com.vfd.summer.ioc.test");
//        Person person = com.vfd.summer.ioc.getBean(Man.class);
//        person.say("hello world", 3);
//        System.out.println("=============");
//        person.say();
    }

    @org.junit.Test
    public void test01() throws NoSuchMethodException {
    }
}
