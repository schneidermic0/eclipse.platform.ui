/*******************************************************************************
 * Copyright (c) 2000, 2004 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.jface.text.source;

import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Canvas;

/**
 * Interface for annotations that know how to represent themselves.
 *
 * @since 3.0
 */
public interface IAnnotationPresentation {
	
	/**
	 * The default annotation layer.
	 */
	static final int DEFAULT_LAYER= 0;

	
	/**
	 * Returns the annotations drawing layer.
	 *
	 * @return the annotations drawing layer
	 */
	int getLayer();
	
	/**
	 * Implement this method to draw a graphical representation 
	 * of this annotation within the given bounds.
	 *
	 * @param gc the drawing GC
	 * @param canvas the canvas to draw on
	 * @param bounds the bounds inside the canvas to draw on
	 */
	void paint(GC gc, Canvas canvas, Rectangle bounds);
}
