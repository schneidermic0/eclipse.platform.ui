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
package org.eclipse.jface.text.contentassist;



import org.eclipse.swt.graphics.Image;


/**
 * The inferface of context information presented to the user and
 * generated by content assist processors.
 * The interface can be implemented by clients. By default, clients use
 * <code>ContextInformation</code> as the standard implementer of this interface. 
 *
 * @see IContentAssistProcessor
 */
public interface IContextInformation {
	
	/**
	 * Returns the string to be displayed in the list of contexts.
	 * This method is used to supply a unique presentation for 
	 * situations where the context is ambiguous. These strings are 
	 * used to allow the user to select the specific context.
	 *
	 * @return the string to be displayed for the context
	 */
	String getContextDisplayString();

	/**
	 * Returns the image for this context information.
	 * The image will be shown to the left of the display string.
	 *
	 * @return the image to be shown or <code>null</code> if no image is desired
	 */
	Image getImage();
	
	/**
	 * Returns the string to be displayed in the tooltip like information popup.
	 *
	 * @return the string to be displayed
	 */
	String getInformationDisplayString();
	
	/**
	 * Compares the given object with this receiver. Two context informations are 
	 * equal if there information display strings and their context display strings 
	 * are equal.
	 *
	 * @see Object#equals(Object)
	 */
	boolean equals(Object object);	
}


