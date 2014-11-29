/*
 * ApplicationHandler.java
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

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;

/**
 * 
 * @author slarti
 */
public abstract class ApplicationHandler implements InvocationHandler {
	private final String methodName;

	public ApplicationHandler(final String methodName) {
		this.methodName = methodName;
	}

	public Object invoke(Object proxy, Method method, Object[] args)
			throws Throwable {
		final boolean valid = methodName.equals(method.getName())
				&& args.length == 1;

		if (valid) {
			handle(new ApplicationEvent(args[0]));
		}
		return null;
	}

	public abstract void handle(ApplicationEvent event);
}
