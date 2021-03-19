package applicationContext;

import ioc.exception.DataConversionException;
import ioc.exception.DuplicateBeanClassException;
import ioc.exception.DuplicateBeanNameException;
import ioc.exception.NoSuchBeanException;

/**
 * @PackageName: ioc.applicationContext
 * @ClassName: ApplicationContext
 * @Description:
 * @author: vfdxvffd
 * @date: 2021/3/14 上午10:59
 */
public interface ApplicationContext {

    Object getBean(String beanName) throws NoSuchBeanException, DataConversionException, DuplicateBeanClassException, DuplicateBeanNameException, NoSuchMethodException;

    <T> T getBean(Class<T> beanType) throws NoSuchBeanException, DataConversionException, DuplicateBeanClassException, DuplicateBeanNameException, NoSuchMethodException;
}
