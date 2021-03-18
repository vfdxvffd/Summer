package ioc.test;

import ioc.annotation.Component;

/**
 * @PackageName: ioc.test
 * @ClassName: Man
 * @Description:
 * @author: vfdxvffd
 * @date: 2021/3/18 上午11:47
 */
@Component
public class Man implements Person {
    @Override
    public void say() {
        System.out.println("i am a man...~~~");
    }

    @Override
    public void say(String s, Integer i) {
        System.out.println(s);
        System.out.println(i/0);
    }
}
