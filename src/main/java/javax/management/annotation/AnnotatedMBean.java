package javax.management.annotation;

import java.lang.annotation.Annotation;
import java.lang.management.ManagementFactory;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

import javax.management.MBeanAttributeInfo;
import javax.management.MBeanConstructorInfo;
import javax.management.MBeanInfo;
import javax.management.MBeanOperationInfo;
import javax.management.MBeanParameterInfo;
import javax.management.MBeanServer;
import javax.management.ObjectName;
import javax.management.StandardMBean;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class AnnotatedMBean extends StandardMBean {
	private static final Logger logger = LoggerFactory.getLogger(AnnotatedMBean.class);
	private static final String MBEAN_NOT_REGISTERED = " not registered";
	private static final String MBEAN_NOT_UNREGISTERED = " not unregistered";
	private String name = null;

	public <T> AnnotatedMBean(Class<T> mbeanInterface, Object... names) {
		super(mbeanInterface, false);
		MBeanName mbeanName = mbeanInterface.getAnnotation(MBeanName.class);
		if (null != mbeanName) {
			register(mbeanName.value(), names);
		}
	}

	@Override
	protected String getDescription(MBeanConstructorInfo info) {
		Constructor<?> constructor = getConstructor(info);
		if (null != constructor) {
			Description annotation = constructor.getAnnotation(Description.class);
			if (annotation != null) {
				return annotation.value();
			}
		}
		return super.getDescription(info);
	}

	@Override
	protected String getDescription(MBeanConstructorInfo info, MBeanParameterInfo param, int sequence) {
		Constructor<?> constructor = getConstructor(info);
		if (null != constructor) {
			Description annotation = constructor.getParameters()[sequence].getAnnotation(Description.class);
			if (annotation != null) {
				return annotation.value();
			}
		}
		return super.getDescription(info, param, sequence);
	}

	@Override
	protected String getParameterName(MBeanConstructorInfo info, MBeanParameterInfo param, int sequence) {
		Constructor<?> constructor = getConstructor(info);
		if (null != constructor) {
			Name annotation = constructor.getParameters()[sequence].getAnnotation(Name.class);
			if (annotation != null) {
				return annotation.value();
			}
		}
		return super.getParameterName(info, param, sequence);
	}

	@Override
	protected String getDescription(MBeanInfo beanInfo) {
		Description annotation = getMBeanInterface().getAnnotation(Description.class);
		if (annotation != null) {
			return annotation.value();
		}
		return beanInfo.getDescription();
	}

	/**
	 * Returns the description of an Attribute
	 */
	@Override
	protected String getDescription(MBeanAttributeInfo info) {
		String attributeName = info.getName();
		String getterName = String.format("get%s%s", attributeName.substring(0, 1).toUpperCase(),
				attributeName.substring(1));
		Method m = methodByName(getMBeanInterface(), getterName, new String[] {});
		if (m != null) {
			Description d = m.getAnnotation(Description.class);
			if (d != null)
				return d.value();
		}
		return info.getDescription();
	}

	/**
	 * Returns the description of an operation
	 */
	@Override
	protected String getDescription(MBeanOperationInfo op) {
		Method m = methodByOperation(getMBeanInterface(), op);
		if (m != null) {
			Description d = m.getAnnotation(Description.class);
			if (d != null)
				return d.value();
		}
		return op.getDescription();
	}

	/**
	 * Returns the impact of an operation
	 */
	@Override
	protected int getImpact(MBeanOperationInfo op) {
		Method m = methodByOperation(getMBeanInterface(), op);
		if (m != null) {
			Impact d = m.getAnnotation(Impact.class);
			if (d != null)
				return d.value();
		}
		return op.getImpact();
	}

	/**
	 * Returns the description of a parameter
	 */
	@Override
	protected String getDescription(MBeanOperationInfo op, MBeanParameterInfo param, int paramNo) {
		Method m = methodByOperation(getMBeanInterface(), op);
		if (m != null) {
			Description pname = getParameterAnnotation(m, paramNo, Description.class);
			if (pname != null)
				return pname.value();
		}
		return getParameterName(op, param, paramNo);
	}

	/**
	 * Returns the name of a parameter
	 */
	@Override
	protected String getParameterName(MBeanOperationInfo op, MBeanParameterInfo param, int paramNo) {
		Method m = methodByOperation(getMBeanInterface(), op);
		if (m != null) {
			Name pname = getParameterAnnotation(m, paramNo, Name.class);
			if (pname != null)
				return pname.value();
		}
		return param.getName();
	}

	private Constructor<?> getConstructor(MBeanConstructorInfo info) {
		try {
			MBeanParameterInfo[] s = info.getSignature();
			Class<?> parameterTypes[] = new Class<?>[s.length];
			int i = 0;
			for (MBeanParameterInfo mBeanParameterInfo : s) {
				parameterTypes[i++] = classForName(mBeanParameterInfo.getType(), getClass().getClassLoader());
			}
			return getImplementationClass().getConstructor(parameterTypes);
		} catch (Exception e) {
			return null;
		}
	}

	/**
	 * Returns the annotation of the given class for the method.
	 */
	private static <A extends Annotation> A getParameterAnnotation(Method m, int paramNo, Class<A> annotation) {
		for (Annotation a : m.getParameterAnnotations()[paramNo]) {
			if (annotation.isInstance(a))
				return annotation.cast(a);
		}
		return null;
	}

	/**
	 * Finds a method within the interface using the method's name and
	 * parameters
	 */
	private static Method methodByName(Class<?> mbeanInterface, String name, String... paramTypes) {
		try {
			final ClassLoader loader = mbeanInterface.getClassLoader();
			final Class<?>[] paramClasses = new Class<?>[paramTypes.length];
			for (int i = 0; i < paramTypes.length; i++)
				paramClasses[i] = classForName(paramTypes[i], loader);
			return mbeanInterface.getMethod(name, paramClasses);
		} catch (RuntimeException e) {
			// avoid accidentally catching unexpected runtime exceptions
			throw e;
		} catch (Exception e) {
			return null;
		}
	}

	/**
	 * Returns the method from the interface for the given bean operation info.
	 */
	private static Method methodByOperation(Class<?> mbeanInterface, MBeanOperationInfo op) {
		final MBeanParameterInfo[] params = op.getSignature();
		final String[] paramTypes = new String[params.length];
		for (int i = 0; i < params.length; i++)
			paramTypes[i] = params[i].getType();

		return methodByName(mbeanInterface, op.getName(), paramTypes);
	}

	/**
	 * Finds the class given its name. <br>
	 * This method also retrieves primitive types (unlike
	 * {@code Class#forName(String)}).
	 */
	private static Class<?> classForName(String name, ClassLoader loader) throws ClassNotFoundException {
		Class<?> c = primitiveClasses.get(name);
		if (c == null) {
			c = Class.forName(name, false, loader);
		}
		return c;
	}

	private static final Map<String, Class<?>> primitiveClasses = new HashMap<>();
	static {
		Class<?>[] primitives = { byte.class, short.class, int.class, long.class, float.class, double.class, char.class,
				boolean.class };
		for (Class<?> clazz : primitives) {
			primitiveClasses.put(clazz.getName(), clazz);
		}
	}

	/**
	 * Register the MBean on MBeanServer
	 * 
	 * @param mbeanName
	 *            StringFormat for ObjectName com.foo:name=%s
	 * @param names
	 *            parameters for ObjectName
	 */
	private void register(String mbeanName, Object... names) {
		name = String.format(mbeanName, names);

		try {
			ObjectName objectName = new ObjectName(name);
			final MBeanServer mbs = ManagementFactory.getPlatformMBeanServer();
			mbs.registerMBean(this, objectName);
		} catch (Exception e) {
			logger.warn(name + MBEAN_NOT_REGISTERED, e);
		}
	}

	public void unregister() {
		try {
			ObjectName objectName = new ObjectName(name);
			final MBeanServer mbs = ManagementFactory.getPlatformMBeanServer();
			mbs.unregisterMBean(objectName);

		} catch (Exception e) {
			logger.warn(name + MBEAN_NOT_UNREGISTERED, e);
		}

	}
}