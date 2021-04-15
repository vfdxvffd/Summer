package com.vfd.summer.exception;

/**
 * @PackageName: com.vfd.summer.exception
 * @ClassName: BeanCreateException
 * @Description:
 * @author: vfdxvffd
 * @date: 2021/4/15 下午2:57
 */
public class DataConversionException extends Exception {

    private final Object val1;
    private final Object val2;

    public DataConversionException(Object val1, Object val2) {
        System.err.println("发生数据转化异常：" + val1 + "转化到" + val2);
        this.val1 = val1;
        this.val2 = val2;
    }

    @Override
    public void printStackTrace() {
        System.err.println("发生数据转化异常：" + val1 + "转化到" + val2);
    }
}
