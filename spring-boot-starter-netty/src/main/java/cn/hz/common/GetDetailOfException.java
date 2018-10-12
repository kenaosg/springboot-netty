package cn.hz.common;

public class GetDetailOfException {
	
	public static String getDetailOfException(Exception e) {
		StringBuilder sb = new StringBuilder();
		
		StackTraceElement[] ste = e.getStackTrace();
		int num = ste.length > 10 ? 10 : ste.length;
		for(int i = 0; i < num; i++) {
			sb.append(ste[i]);
			sb.append("\r\n");
		}
		
		return "exception name-cause-trace>>>>>>" 
		+ e.toString() + "\r\n"
		//+ e.getMessage() + "******\r\n" 
		+ e.getCause() + "\r\n"
		+ sb.toString();
	}

	public static String getDetailOfThrowable(Throwable e) {
		StringBuilder sb = new StringBuilder();

		StackTraceElement[] ste = e.getStackTrace();
		int num = ste.length > 10 ? 10 : ste.length;
		for(int i = 0; i < num; i++) {
			sb.append(ste[i]);
			sb.append("\r\n");
		}

		return "exception name-cause-trace>>>>>>"
				+ e.toString() + "\r\n"
				//+ e.getMessage() + "******\r\n"
				+ e.getCause() + "\r\n"
				+ sb.toString();
	}

}
