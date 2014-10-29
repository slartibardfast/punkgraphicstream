/*
 * Application.java
 *
 * Copyright 2008 David Connolly. All rights reserved.
 *
 * This file is part of PunkGraphicStream.
 *
 * PunkGraphicStream is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA
 */
package name.connolly.david.pgs.mac;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Refelectivly provides a link to Apple's EAWT Application
 * 
 * @author slarti
 */
public enum Application {

	INSTANCE;
	AtomicBoolean initalized = new AtomicBoolean(false);
	Object application;
	@SuppressWarnings("unchecked")
	Class listenerClass;
	Method addApplicationListener;

	@SuppressWarnings("unchecked")
	private void init() {
		try {
			final Class applicationClass = Class
					.forName("com.apple.eawt.Application");
			final Method applicationSingleton = applicationClass
					.getDeclaredMethod("getApplication", (Class[]) null);
			application = applicationSingleton.invoke(applicationClass,
					(Object[]) null);
			listenerClass = Class.forName("com.apple.eawt.ApplicationListener");
			addApplicationListener = applicationClass.getDeclaredMethod(
					"addApplicationListener", listenerClass);
		} catch (final IllegalAccessException ex) {
			Logger.getLogger(Application.class.getName()).log(Level.SEVERE,
					null, ex);
		} catch (final IllegalArgumentException ex) {
			Logger.getLogger(Application.class.getName()).log(Level.SEVERE,
					null, ex);
		} catch (final InvocationTargetException ex) {
			Logger.getLogger(Application.class.getName()).log(Level.SEVERE,
					null, ex);
		} catch (final NoSuchMethodException ex) {
			Logger.getLogger(Application.class.getName()).log(Level.SEVERE,
					null, ex);
		} catch (final SecurityException ex) {
			Logger.getLogger(Application.class.getName()).log(Level.SEVERE,
					null, ex);
		} catch (final ClassNotFoundException ex) {
			Logger.getLogger(Application.class.getName()).log(Level.SEVERE,
					null, ex);
		}

		initalized.set(true);
	}

	public void addListener(ApplicationHandler listener) {
		if (initalized.get() == false) {
			init();
		}

		final Object proxy = Proxy.newProxyInstance(listenerClass
				.getClassLoader(), new Class[] { listenerClass }, listener);

		try {
			addApplicationListener.invoke(application, proxy);
		} catch (final IllegalAccessException ex) {
			Logger.getLogger(Application.class.getName()).log(Level.SEVERE,
					null, ex);
		} catch (final IllegalArgumentException ex) {
			Logger.getLogger(Application.class.getName()).log(Level.SEVERE,
					null, ex);
		} catch (final InvocationTargetException ex) {
			Logger.getLogger(Application.class.getName()).log(Level.SEVERE,
					null, ex);
		}
	}
}
