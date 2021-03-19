package com.vfd.summer.ioc.bean;

/**
 * @PackageName: com.vfd.summer.ioc.bean
 * @ClassName: BeanDefinition
 * @Description:
 * @author: vfdxvffd
 * @date: 2021/3/11 下午2:26
 */
public class BeanDefinition {

    private String beanName;
    private Class<?> beanClass;
    private Boolean lazy;
    private Boolean singleton;

    public BeanDefinition() {
    }

    public BeanDefinition(String beanName, Class<?> beanClass, Boolean lazy, Boolean singleton) {
        this.beanName = beanName;
        this.beanClass = beanClass;
        this.lazy = lazy;
        this.singleton = singleton;
    }

    public String getBeanName() {
        return beanName;
    }

    public void setBeanName(String beanName) {
        this.beanName = beanName;
    }

    public Class<?> getBeanClass() {
        return beanClass;
    }

    public void setBeanClass(Class<?> beanClass) {
        this.beanClass = beanClass;
    }

    public Boolean getLazy() {
        return lazy;
    }

    public void setLazy(Boolean lazy) {
        this.lazy = lazy;
    }

    public Boolean getSingleton() {
        return singleton;
    }

    public void setSingleton(Boolean singleton) {
        this.singleton = singleton;
    }
}
