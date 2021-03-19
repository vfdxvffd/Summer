package com.vfd.summer.ioc.exception;

/**
 * @PackageName: com.vfd.summer.ioc.exception
 * @ClassName: DuplicateBeanNameException
 * @Description:
 * @author: vfdxvffd
 * @date: 2021/3/15 上午11:25
 */
public class DuplicateBeanNameException extends Exception {

    private String beanName;

    public DuplicateBeanNameException(String beanName) {
        this.beanName = beanName;
    }

    @Override
    public void printStackTrace() {
        System.out.println("发生beanName重复异常：" + beanName);
        super.printStackTrace();
    }
}
