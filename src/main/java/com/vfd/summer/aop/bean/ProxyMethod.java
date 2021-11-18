package com.vfd.summer.aop.bean;

import java.lang.reflect.Method;
import java.util.LinkedList;
import java.util.List;

/**
 * @PackageName: com.vfd.summer.aop.bean
 * @ClassName: ProxyMethod
 * @Description:
 * @Author: vfdxvffd
 * @date: 2021/11/18 18:39
 */
public class ProxyMethod {

    List<Method> beforeMethods = new LinkedList<>();
    List<Object> beforeObject = new LinkedList<>();

    List<Method> returningMethods = new LinkedList<>();
    List<Object> returningObject = new LinkedList<>();

    List<Method> throwingMethods = new LinkedList<>();
    List<Object> throwingObject = new LinkedList<>();

    List<Method> afterMethods = new LinkedList<>();
    List<Object> afterObject = new LinkedList<>();

    public ProxyMethod(List<Method> beforeMethods, List<Object> beforeObject, List<Method> returningMethods, List<Object> returningObject, List<Method> throwingMethods, List<Object> throwingObject, List<Method> afterMethods, List<Object> afterObject) {
        this.beforeMethods = beforeMethods;
        this.beforeObject = beforeObject;
        this.returningMethods = returningMethods;
        this.returningObject = returningObject;
        this.throwingMethods = throwingMethods;
        this.throwingObject = throwingObject;
        this.afterMethods = afterMethods;
        this.afterObject = afterObject;
    }

    public List<Method> getBeforeMethods() {
        return beforeMethods;
    }

    public void setBeforeMethods(List<Method> beforeMethods) {
        this.beforeMethods = beforeMethods;
    }

    public List<Object> getBeforeObject() {
        return beforeObject;
    }

    public void setBeforeObject(List<Object> beforeObject) {
        this.beforeObject = beforeObject;
    }

    public List<Method> getReturningMethods() {
        return returningMethods;
    }

    public void setReturningMethods(List<Method> returningMethods) {
        this.returningMethods = returningMethods;
    }

    public List<Object> getReturningObject() {
        return returningObject;
    }

    public void setReturningObject(List<Object> returningObject) {
        this.returningObject = returningObject;
    }

    public List<Method> getThrowingMethods() {
        return throwingMethods;
    }

    public void setThrowingMethods(List<Method> throwingMethods) {
        this.throwingMethods = throwingMethods;
    }

    public List<Object> getThrowingObject() {
        return throwingObject;
    }

    public void setThrowingObject(List<Object> throwingObject) {
        this.throwingObject = throwingObject;
    }

    public List<Method> getAfterMethods() {
        return afterMethods;
    }

    public void setAfterMethods(List<Method> afterMethods) {
        this.afterMethods = afterMethods;
    }

    public List<Object> getAfterObject() {
        return afterObject;
    }

    public void setAfterObject(List<Object> afterObject) {
        this.afterObject = afterObject;
    }
}
