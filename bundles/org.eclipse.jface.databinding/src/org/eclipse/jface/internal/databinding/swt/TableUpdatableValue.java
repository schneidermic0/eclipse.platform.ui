/*******************************************************************************
 * Copyright (c) 2005 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.jface.internal.databinding.swt;

import org.eclipse.jface.databinding.ChangeEvent;
import org.eclipse.jface.databinding.UpdatableValue;
import org.eclipse.jface.databinding.swt.SWTProperties;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.widgets.Table;

/**
 * @since 3.2
 *
 */
public class TableUpdatableValue extends UpdatableValue {

	private final Table table;

	private boolean updating = false;

	/**
	 * @param table
	 * @param attribute
	 */
	public TableUpdatableValue(Table table, String attribute) {
		this.table = table;
		if (attribute.equals(SWTProperties.SELECTION)) {
			table.addSelectionListener(new SelectionListener() {
				public void widgetSelected(SelectionEvent e) {
					if (!updating) {
						fireChangeEvent( ChangeEvent.CHANGE, null, null);
					}
				}

				public void widgetDefaultSelected(SelectionEvent e) {
					widgetSelected(e);
				}
			});
		} else {
			throw new IllegalArgumentException();
		}
	}

	public void setValue(Object value) {
		try {
			updating = true;
			table.setSelection(((Integer) value).intValue());
		} finally {
			updating = false;
		}
	}

	public Object computeValue() {
		return new Integer(table.getSelectionIndex());
	}

	public Class getValueType() {
		return Integer.class;
	}

}
