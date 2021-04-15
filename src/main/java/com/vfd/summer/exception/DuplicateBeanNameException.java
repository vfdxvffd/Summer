package com.vfd.summer.exception;

/**
 * @PackageName: com.vfd.summer.exception
 * @ClassName: DuplicateBeanNameException
 * @Description:
 * @author: vfdxvffd
 * @date: 2021/4/15 下午3:13
 */
public class DuplicateBeanNameException extends Exception {
    private final String beanName;

    public DuplicateBeanNameException(String beanName) {
        System.err.println("发生beanName重复异常：" + beanName);
        this.beanName = beanName;
    }

    @Override
    public void printStackTrace() {
        System.err.println("发生beanName重复异常：" + beanName);
    }
}
