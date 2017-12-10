package javax.management.annotation;

import static java.lang.annotation.ElementType.TYPE;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Define the JMX ObjectName.<br />
 * If the MBeanName Annotation is set, each instance of the MBean is
 * automatically registered.<br />
 * You can use the StringFormat %s syntax to specify parameters.<br />
 * In this case it is necessary to pass the values of its parameters to the
 * super constructor.<br />
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ TYPE })
public @interface MBeanName {
	String value();
}