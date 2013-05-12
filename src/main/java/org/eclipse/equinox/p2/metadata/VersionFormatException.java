/*******************************************************************************
 * Copyright (c) 2009, 2010 Cloudsmith Inc. and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Cloudsmith Inc. - initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.p2.metadata;

/**
 * Exception thrown when parsing Omni Version formats.
 * @noextend This class is not intended to be subclassed by clients.
 * @since 2.0
 */
public class VersionFormatException extends Exception {

	private static final long serialVersionUID = -867104101610941043L;

	public VersionFormatException(String message) {
		super(message);
	}
}
