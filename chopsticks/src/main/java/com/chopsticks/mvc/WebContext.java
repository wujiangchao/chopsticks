package com.chopsticks.mvc;

import io.netty.util.concurrent.FastThreadLocal;

public class WebContext {
	/**
	 * ThreadLocal, used netty fast theadLocal
	 */
	private static final FastThreadLocal<WebContext> fastThreadLocal = new FastThreadLocal<>();

}
