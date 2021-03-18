package ioc.test;

import aop.annotation.After;
import aop.annotation.AfterThrowing;
import aop.annotation.Aspect;
import aop.annotation.Before;
import ioc.annotation.Component;

/**
 * @PackageName: ioc.test
 * @ClassName: Test
 * @Description:
 * @author: vfdxvffd
 * @date: 2021/3/18 上午11:48
 */
@Component
@Aspect
public class Test {

    @Before("ioc.test.Man.say(java.lang.String, java.lang.Integer)")
    public void before() {
        System.out.println("before");
    }

    @After("ioc.test.Man.say()")
    public void after() {
        System.out.println("after");
    }

    @AfterThrowing("ioc.test.Man.say(java.lang.String, java.lang.Integer)")
    public void throwing() {
        System.out.println("exception acc");
    }
}
