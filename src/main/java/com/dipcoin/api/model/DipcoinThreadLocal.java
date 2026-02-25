package com.dipcoin.api.model;

public class DipcoinThreadLocal {
	
	@SuppressWarnings("rawtypes")
	public static final ThreadLocal dipcoinThreadLocal = new ThreadLocal();

	@SuppressWarnings("unchecked")
	public static void set(AspectContext user) {
	  dipcoinThreadLocal.set(user);
	}
	
	public static void unset() {
	  dipcoinThreadLocal.remove();
	}

	public static AspectContext get() {
		return (AspectContext)dipcoinThreadLocal.get();
	}
}
