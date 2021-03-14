package ioc.test;

import ioc.annotation.Component;
import ioc.annotation.Value;

/**
 * @PackageName: ioc.test
 * @ClassName: Pen
 * @Description:
 * @author: vfdxvffd
 * @date: 2021/3/14 下午4:37
 */
@Component("myPen")
public class Pen {
    @Value("钢笔")
    private String name;
    @Value("2.5f")
    private float price;

    public Pen() {
    }

    public Pen(String name, float price) {
        this.name = name;
        this.price = price;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public float getPrice() {
        return price;
    }

    public void setPrice(float price) {
        this.price = price;
    }

    @Override
    public String toString() {
        return "Pen{" +
                "name='" + name + '\'' +
                ", price=" + price +
                '}';
    }
}
