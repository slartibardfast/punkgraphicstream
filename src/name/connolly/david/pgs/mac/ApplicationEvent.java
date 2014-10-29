/*
 * ApplicationEvent.java
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
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * 
 * @author slarti
 */
public class ApplicationEvent {
	private final Object event;
	private Method getFilename;
	private Method isHandled;
	private Method setHandled;

	public ApplicationEvent(Object event) {
		this.event = event;

		try {
			getFilename = event.getClass().getDeclaredMethod("getFilename",
					(Class[]) null);
			isHandled = event.getClass().getDeclaredMethod("isHandled",
					(Class[]) null);
			setHandled = event.getClass().getDeclaredMethod("setHandled",
					new Class[] { boolean.class });
		} catch (final NoSuchMethodException ex) {
			Logger.getLogger(ApplicationEvent.class.getName()).log(
					Level.SEVERE, null, ex);
		} catch (final SecurityException ex) {
			Logger.getLogger(ApplicationEvent.class.getName()).log(
					Level.SEVERE, null, ex);
		}

	}

	public String getFilename() {
		try {
			return (String) getFilename.invoke(event, (Object[]) null);
		} catch (final IllegalAccessException ex) {
			Logger.getLogger(ApplicationEvent.class.getName()).log(
					Level.SEVERE, null, ex);
		} catch (final IllegalArgumentException ex) {
			Logger.getLogger(ApplicationEvent.class.getName()).log(
					Level.SEVERE, null, ex);
		} catch (final InvocationTargetException ex) {
			Logger.getLogger(ApplicationEvent.class.getName()).log(
					Level.SEVERE, null, ex);
		}

		return null;
	}

	public boolean isHandled() {
		try {
			return (Boolean) isHandled.invoke(event, (Object[]) null);
		} catch (final IllegalAccessException ex) {
			Logger.getLogger(ApplicationEvent.class.getName()).log(
					Level.SEVERE, null, ex);
		} catch (final IllegalArgumentException ex) {
			Logger.getLogger(ApplicationEvent.class.getName()).log(
					Level.SEVERE, null, ex);
		} catch (final InvocationTargetException ex) {
			Logger.getLogger(ApplicationEvent.class.getName()).log(
					Level.SEVERE, null, ex);
		}

		return false;
	}

	public void setHandled(boolean handled) {
		try {
			setHandled.invoke(event, new Object[] { handled });
		} catch (final IllegalAccessException ex) {
			Logger.getLogger(ApplicationEvent.class.getName()).log(
					Level.SEVERE, null, ex);
		} catch (final IllegalArgumentException ex) {
			Logger.getLogger(ApplicationEvent.class.getName()).log(
					Level.SEVERE, null, ex);
		} catch (final InvocationTargetException ex) {
			Logger.getLogger(ApplicationEvent.class.getName()).log(
					Level.SEVERE, null, ex);
		}
	}
}
