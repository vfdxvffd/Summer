package com.vfd.summer.aop.bean;

import java.lang.reflect.Type;
import java.util.Arrays;

/**
 * @PackageName: com.vfd.summer.aop.bean
 * @ClassName: JoinPoint
 * @Description: 对切面方法传递被切的方法参数的类，可以在切点方法中传递此类的对象，以获得目标方法的参数、方法名、返回值类型
 * @author: vfdxvffd
 * @date: 2021/3/21 上午10:17
 */
public class JoinPoint {

    private String methodName;

    private Object[] parameters;

    private Type returnType;

    public JoinPoint() {
    }

    public JoinPoint(String methodName, Object[] parameters, Type returnType) {
        this.methodName = methodName;
        this.parameters = parameters;
        this.returnType = returnType;
    }

    public String getMethodName() {
        return methodName;
    }

    public void setMethodName(String methodName) {
        this.methodName = methodName;
    }

    public Object[] getParameters() {
        return parameters;
    }

    public void setParameters(Object[] parameters) {
        this.parameters = parameters;
    }

    public Type getReturnType() {
        return returnType;
    }

    public void setReturnType(Type returnType) {
        this.returnType = returnType;
    }

    @Override
    public String toString() {
        return "JoinPoint{" +
                "methodName='" + methodName + '\'' +
                ", parameters=" + Arrays.toString(parameters) +
                ", returnType=" + returnType +
                '}';
    }
}
