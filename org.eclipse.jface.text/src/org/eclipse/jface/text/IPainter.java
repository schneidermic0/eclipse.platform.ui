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

package org.eclipse.jface.text;


/**
 * A painter is responsible for creating, managing, updating, and removing visual decorations of an <code>ITextViewer</code>. Examples are
 * the highlighting of the cursor line, the print marging, or the highlighting of matching peer characters such as pairs of brackets. Clients
 * may implement this interface. Painters should be registered with a <code>PaintManager</code>. The paint manager tracks several classes 
 * of events issued by an <code>ITextViewer</code> and reacts  by appropriately invoking the registered painters.<p>
 * Painters can be activated or deactivated. Usually, painters are initially deactivated and are activated by the first call to their <code>paint</code>
 * method. Painters can be deactivated by calling <code>deactivate</code>. Deactivated painter can be reactivated by calling <code>paint</code>
 * again.<p>
 * Painters usually have to manage state information. E.g.,  a painter painting a cursor line highlight must redraw the previous and the actual cursor
 * line in the advent of a change of the cursor position. This state information must usually be adapted to changes of the viewer's content. In order
 * to support this common scenario, the <code>PaintManager</code> gives a painter access to a <code>IPaintPositionUpdater</code>. The painter
 * can use this updater to manage its state information.<p>
 *  
 * @see org.eclipse.jface.text.PaintManager
 * @since 2.1
 */
public interface IPainter {
	
	/**
	 * Constant describing the reason of a repaint request: selection changed.
	 */
	int SELECTION= 0;
	/**
	 * Constant describing the reason of a repaint request: text changed.
	 */
	int TEXT_CHANGE= 1;
	/**
	 * Constant describing the reason of a repaint request: key pressed.
	 */
	int KEY_STROKE= 2;
	/**
	 * Constant describing the reason of a repaint request: mouse button pressed.
	 */
	int MOUSE_BUTTON= 4;
	/**
	 * Constant describing the reason of a repaint request: paint manager internal change.
	 */
	int INTERNAL= 8;
	/**
	 * Constant describing the reason of a repaint request: paint manager or painter configuration changed.
	 */
	int CONFIGURATION= 16;
	

	/**	
	 * Disposes this painter. Prior to disposing, a painter should be deactivated. A disposed
	 * painter can not be reactivated.
	 * 
	 * @see #deactivate(boolean)
	 */
	void dispose();
	
	/**
	 * Requests this painter to repaint because of the given reason. Based on the given reason
	 * the painter can decide whether it will repaint or not. If it repaints and is not yet activated,
	 * it wil activate itself.
	 * 
	 * @param reason the repaint reason, value is one of the constants defined in this interface
	 */
	void paint(int reason);

	/**
	 * Deactivates this painter. If the painter has not been activated before, this call does not
	 * have any effect. <code>redraw</code> indicates whether the painter should remove
	 * any decoration it previously applied. A deactivated painter can be reactivated by calling
	 * <code>paint</code>.
	 * 
	 * @param redraw <code>true</code> if any previously applied decoration should be removed
	 * @see #paint(int) 
	 */
	void deactivate(boolean redraw);
	
	/**
	 * Sets the paint position manager that can be used by this painter or removes any previously
	 * set paint position manager.
	 * 
	 * @param manager the paint position manager or <code>null</code>
	 */
	void setPositionManager(IPaintPositionManager manager);
}
