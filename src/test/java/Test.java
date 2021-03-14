import ioc.applicationContext.impl.SummerAnnotationConfigApplicationContext;
import ioc.exception.DataConversionException;
import ioc.test.Student;

/**
 * @PackageName: PACKAGE_NAME
 * @ClassName: Test
 * @Description:
 * @author: vfdxvffd
 * @date: 2021/3/14 下午3:14
 */
public class Test {

    @org.junit.Test
    public void test() {
        try {
            SummerAnnotationConfigApplicationContext ioc = new SummerAnnotationConfigApplicationContext("ioc.test");
            Object bean = ioc.getBean(Student.class);
            System.out.println(bean);
        } catch (DataConversionException e) {
            e.printStackTrace();
        }
    }
}
