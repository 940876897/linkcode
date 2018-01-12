package cn.zhengfuqiang.linkcode.java8;

import org.junit.Test;

import java.time.LocalDate;

/**
 * @author zhengfuqiang
 * @description
 * @create 2018-01-12 9:56
 */
public class JavaTimeTest {

    @Test
    public void localDateTest() {
        LocalDate localDate = LocalDate.now();
        System.out.println("今天的日期：" + localDate);
    }
}
