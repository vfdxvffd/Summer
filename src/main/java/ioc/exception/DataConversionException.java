package ioc.exception;

/**
 * @PackageName: ioc.exception
 * @ClassName: DataConversionException
 * @Description: 通过@Value注入值的时候可能发生的转化异常
 * @author: vfdxvffd
 * @date: 2021/3/14 下午3:36
 */
public class DataConversionException extends Exception{

    private final Object val1;
    private final Object val2;

    public DataConversionException(Object val1, Object val2) {
        this.val1 = val1;
        this.val2 = val2;
    }

    @Override
    public void printStackTrace() {
        System.out.println("发生数据转化异常：" + val1 + "转化到" + val2);
        super.printStackTrace();
    }
}
