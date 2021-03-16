package ioc.test;

import ioc.annotation.Autowired;
import ioc.annotation.Component;
import ioc.annotation.Lazy;
import ioc.annotation.Value;

/**
 * @PackageName: ioc.test
 * @ClassName: Pen
 * @Description:
 * @author: vfdxvffd
 * @date: 2021/3/14 下午4:37
 */
@Component
@Lazy
public class Pen {
    @Value("钢笔")
    private String name;
    @Value("2.5f")
    private float price;
    @Autowired
    private Student student;

    public Pen() {
    }

    @Override
    public String toString() {
        return "Pen{" +
                "name='" + name + '\'' +
                ", price=" + price +
                ", student=" + student +
                '}';
    }

    public Student getStudent() {
        return student;
    }

    public void setStudent(Student student) {
        this.student = student;
    }

    public Pen(String name, float price, Student student) {
        this.name = name;
        this.price = price;
        this.student = student;
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

}
