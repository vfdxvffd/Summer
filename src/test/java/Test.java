import ioc.applicationContext.impl.SummerAnnotationConfigApplicationContext;
import ioc.exception.*;
import ioc.exception.IllegalStateException;
import ioc.test.Pen;
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
            Student student = ioc.getBean(Student.class);
            Student bean = ioc.getBean(Student.class);
            Pen pen = ioc.getBean(Pen.class);
            System.out.println(student == bean);
            System.out.println(student.getPen() == pen);
            System.out.println(bean.getPen() == pen);
        } catch (DataConversionException | IllegalStateException | DuplicateBeanNameException | DuplicateBeanClassException | NoSuchBeanException e) {
            e.printStackTrace();
        }
    }

    @org.junit.Test
    public void test01() {

    }
}
