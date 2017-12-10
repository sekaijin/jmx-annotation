package javax.management.annotation;

import static java.lang.annotation.ElementType.METHOD;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(METHOD)
public @interface Impact {

    /**
     * Indicates that the operation is read-like: it returns information but
     * does not change any state.
     */
    public static final int INFO = 0;

    /**
     * Indicates that the operation is write-like: it has an effect but does not
     * return any information from the MBean.
     */
    public static final int ACTION = 1;

    /**
     * Indicates that the operation is both read-like and write-like: it has an
     * effect, and it also returns information from the MBean.
     */
    public static final int ACTION_INFO = 2;

    /**
     * Indicates that the impact of the operation is unknown or cannot be
     * expressed using one of the other values.
     */
    public static final int UNKNOWN = 3;

    int value();
}