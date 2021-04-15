package com.vfd.summer.exception;

/**
 * @PackageName: com.vfd.summer.exception
 * @ClassName: DuplicateBeanClassException
 * @Description:
 * @author: vfdxvffd
 * @date: 2021/4/15 下午3:22
 */
public class DuplicateBeanClassException extends Exception {
    private final Class<?> clazz;

    public DuplicateBeanClassException(Class<?> clazz) {
        this.clazz = clazz;
    }

    @Override
    public void printStackTrace() {
        System.out.println("发生beanClass重复异常：" + clazz.getName());
        super.printStackTrace();
    }
}
