/**********************************************************************
 * Copyright (c) 2004 IBM Corporation and others. All rights reserved.   This
 * program and the accompanying materials are made available under the terms of
 * the Common Public License v1.0 which accompanies this distribution, and is
 * available at http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors: 
 * IBM - Initial API and implementation
 **********************************************************************/
package org.eclipse.ui.internal.ide.actions;
import org.eclipse.core.resources.IProject;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.actions.ActionFactory;
import org.eclipse.ui.internal.ide.IDEWorkbenchMessages;
import org.eclipse.ui.internal.ide.dialogs.CleanDialog;
/**
 * The clean action replaces the rebuild actions. Clean will discard all built
 * state for all projects in the workspace, and deletes all problem markers.
 * The next time a build is run, projects will have to be built from scratch.
 * Technically this is only necessary if an incremental builder misbehaves.
 * 
 * @since 3.0
 */
public class BuildCleanAction extends Action implements ActionFactory.IWorkbenchAction {
	private IWorkbenchWindow window;
	public void dispose() {
	}
	public BuildCleanAction(IWorkbenchWindow window) {
		super(IDEWorkbenchMessages.getString("Workbench.buildClean")); //$NON-NLS-1$
		this.window = window;
	}
	public void run() {
		ISelection selection = window.getSelectionService().getSelection();
		IProject[] selected;
		if (selection != null && !selection.isEmpty() && selection instanceof IStructuredSelection) {
			selected = BuildSetAction.extractProjects(((IStructuredSelection)selection).toArray());
		} else {
			selected = new IProject[0];
		}
		new CleanDialog(window.getShell(), selected).open();
	}
}
