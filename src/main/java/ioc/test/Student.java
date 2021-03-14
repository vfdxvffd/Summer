package ioc.test;

import ioc.annotation.Autowired;
import ioc.annotation.Component;
import ioc.annotation.Qualifier;
import ioc.annotation.Value;

/**
 * @PackageName: ioc.test
 * @ClassName: Student
 * @Description:
 * @author: vfdxvffd
 * @date: 2021/3/14 下午3:54
 */
@Component
public class Student {

    @Value("zhangsan")
    private String name;
    @Value("18")
    private int age;
    @Value("178")
    private Integer height;
    @Value("137.33")
    private Double weight;
    @Value("q")
    private char symbol;
    @Value("true")
    private Boolean sex;
    @Value("333.3")
    private float f;
    @Autowired
    @Qualifier("myPen")
    private Pen pen;

    @Override
    public String toString() {
        return "Student{" +
                "name='" + name + '\'' +
                ", age=" + age +
                ", height=" + height +
                ", weight=" + weight +
                ", symbol=" + symbol +
                ", sex=" + sex +
                ", f=" + f +
                ", pen=" + pen +
                '}';
    }

    public Pen getPen() {
        return pen;
    }

    public void setPen(Pen pen) {
        this.pen = pen;
    }

    public Student() {
    }

    public Student(String name, int age, Integer height, Double weight, char symbol, Boolean sex, float f, Pen pen) {
        this.name = name;
        this.age = age;
        this.height = height;
        this.weight = weight;
        this.symbol = symbol;
        this.sex = sex;
        this.f = f;
        this.pen = pen;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getAge() {
        return age;
    }

    public void setAge(int age) {
        this.age = age;
    }

    public Integer getHeight() {
        return height;
    }

    public void setHeight(Integer height) {
        this.height = height;
    }

    public Double getWeight() {
        return weight;
    }

    public void setWeight(Double weight) {
        this.weight = weight;
    }

    public char getSymbol() {
        return symbol;
    }

    public void setSymbol(char symbol) {
        this.symbol = symbol;
    }

    public Boolean getSex() {
        return sex;
    }

    public void setSex(Boolean sex) {
        this.sex = sex;
    }

    public float getF() {
        return f;
    }

    public void setF(float f) {
        this.f = f;
    }
}
