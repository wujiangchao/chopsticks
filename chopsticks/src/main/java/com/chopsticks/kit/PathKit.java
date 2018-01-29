package com.chopsticks.kit;

import java.util.regex.Pattern;

public class PathKit {
    public static final  String  VAR_REGEXP          = ":(\\w+)";
    public static final  String  VAR_REPLACE         = "([^#/?.]+)";
    private static final String  SLASH               = "/";
    public static final  Pattern VAR_REGEXP_PATTERN  = Pattern.compile(VAR_REGEXP);
    private static final Pattern VAR_FIXPATH_PATTERN = Pattern.compile("\\s");


	public static String fixPath(String path) {
		if (null == path) {
			return SLASH;
		}
		if (path.charAt(0) != '/') {
			path = SLASH + path;
		}
		if (path.length() > 1 && path.endsWith(SLASH)) {
			path = path.substring(0, path.length() - 1);
		}
		if (!path.contains("\\s")) {
			return path;
		}
		return path.replace("\\s", "%20");
	}

	static String cleanPath(String path) {
		if (path == null) {
			return null;
		}
		return path.replaceAll("[/]+", SLASH);
	}
}
