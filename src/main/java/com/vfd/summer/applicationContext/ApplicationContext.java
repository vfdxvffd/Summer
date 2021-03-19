package com.vfd.summer.applicationContext;

import com.vfd.summer.ioc.exception.DataConversionException;
import com.vfd.summer.ioc.exception.DuplicateBeanClassException;
import com.vfd.summer.ioc.exception.DuplicateBeanNameException;
import com.vfd.summer.ioc.exception.NoSuchBeanException;

/**
 * @PackageName: com.vfd.summer.ioc.applicationContext
 * @ClassName: ApplicationContext
 * @Description:
 * @author: vfdxvffd
 * @date: 2021/3/14 上午10:59
 */
public interface ApplicationContext {

    Object getBean(String beanName) throws NoSuchBeanException, DataConversionException, DuplicateBeanClassException, DuplicateBeanNameException, NoSuchMethodException, IllegalAccessException, ClassNotFoundException;

    <T> T getBean(Class<T> beanType) throws NoSuchBeanException, DataConversionException, DuplicateBeanClassException, DuplicateBeanNameException, NoSuchMethodException, IllegalAccessException, ClassNotFoundException;
}
