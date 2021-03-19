package com.vfd.summer.ioc.exception;

/**
 * @PackageName: com.vfd.summer.ioc.exception
 * @ClassName: DuplicateBeanClassException
 * @Description:
 * @author: vfdxvffd
 * @date: 2021/3/15 上午11:30
 */
public class DuplicateBeanClassException extends Exception {

    private Class<?> clazz;

    public DuplicateBeanClassException(Class<?> clazz) {
        this.clazz = clazz;
    }

    @Override
    public void printStackTrace() {
        System.out.println("发生beanClass重复异常：" + clazz.getName());
        super.printStackTrace();
    }
}
