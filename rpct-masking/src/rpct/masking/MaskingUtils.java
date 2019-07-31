package rpct.masking;

import java.io.PrintStream;
import java.text.DateFormat;
import java.util.Date;

import javax.faces.context.FacesContext;

public class MaskingUtils {

	private static final DateFormat TIMEFORMAT = DateFormat
	.getTimeInstance(DateFormat.MEDIUM);
	private static final DateFormat DATEFORMAT = DateFormat
	.getDateInstance(DateFormat.MEDIUM);
	public static PrintStream LOG_STREAM = System.out;

	public static String timeNow() {
		return TIMEFORMAT.format(new Date());
	}
	public static String dateNow() {
		return DATEFORMAT.format(new Date());
	}
	
	public static void log(String message) {
		LOG_STREAM.println(String.format(" *[%s] %s", timeNow(), message));
	}

	public static void removeBeanFromSession(String beanName) {
		Object bean =  FacesContext.getCurrentInstance()
		.getExternalContext().getSessionMap().get(beanName);

		if (bean != null) {
			System.out.println(String.format("Removing #{%s} ...", beanName));
			FacesContext.getCurrentInstance().getExternalContext()
			.getSessionMap().remove(beanName);
		} else
			System.out.println(String.format("#{%s} is null!", beanName));
	}
}
