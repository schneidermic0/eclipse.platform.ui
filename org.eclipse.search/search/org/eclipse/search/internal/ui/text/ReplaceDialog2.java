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
package org.eclipse.search.internal.ui.text;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import org.eclipse.core.filebuffers.FileBuffers;
import org.eclipse.core.filebuffers.ITextFileBuffer;
import org.eclipse.core.filebuffers.ITextFileBufferManager;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceRuleFactory;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.IWorkspaceDescription;
import org.eclipse.core.resources.IncrementalProjectBuilder;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.SubProgressMonitor;
import org.eclipse.core.runtime.jobs.ISchedulingRule;
import org.eclipse.jface.contentassist.SubjectControlContentAssistant;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.resource.JFaceColors;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.DefaultInformationControl;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IInformationControl;
import org.eclipse.jface.text.IInformationControlCreator;
import org.eclipse.jface.text.Position;
import org.eclipse.jface.text.contentassist.IContentAssistProcessor;
import org.eclipse.jface.text.contentassist.IContentAssistant;
import org.eclipse.jface.util.Assert;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.search.internal.ui.ISearchHelpContextIds;
import org.eclipse.search.internal.ui.SearchMessages;
import org.eclipse.search.internal.ui.SearchPlugin;
import org.eclipse.search.internal.ui.util.ExceptionHandler;
import org.eclipse.search.internal.ui.util.ExtendedDialogWindow;
import org.eclipse.search.ui.NewSearchUI;
import org.eclipse.search.ui.text.Match;
import org.eclipse.search2.internal.ui.InternalSearchUI;
import org.eclipse.search2.internal.ui.text.PositionTracker;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.IEditorDescriptor;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IEditorReference;
import org.eclipse.ui.IReusableEditor;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.actions.GlobalBuildAction;
import org.eclipse.ui.contentassist.ContentAssistHandler;
import org.eclipse.ui.help.WorkbenchHelp;
import org.eclipse.ui.ide.IDE;
import org.eclipse.ui.part.FileEditorInput;
import org.eclipse.ui.texteditor.ITextEditor;

class ReplaceDialog2 extends ExtendedDialogWindow {
		
	private abstract static class ReplaceOperation implements IRunnableWithProgress {

		public void run(IProgressMonitor monitor) throws InvocationTargetException {
			try {
				doReplace(monitor);
			} catch (BadLocationException e) {
				throw new InvocationTargetException(e);
			} catch (CoreException e) {
				throw new InvocationTargetException(e);
			} catch (IOException e) {
				throw new InvocationTargetException(e);
			}
		}
		
		protected abstract void doReplace(IProgressMonitor pm) throws BadLocationException, CoreException, IOException;
	}
		
	// various widget related constants
	private static final int REPLACE= IDialogConstants.CLIENT_ID + 1;
	private static final int REPLACE_ALL_IN_FILE= IDialogConstants.CLIENT_ID + 2;
	private static final int REPLACE_ALL= IDialogConstants.CLIENT_ID + 3;
	private static final int SKIP= IDialogConstants.CLIENT_ID + 4;
	private static final int SKIP_FILE= IDialogConstants.CLIENT_ID + 5;
	private static final int SKIP_ALL= IDialogConstants.CLIENT_ID + 6;
	
	// Widgets
	private Text fTextField;
	private Button fReplaceWithRegex;
	private Button fReplaceButton;
	private Button fReplaceAllInFileButton;
	private Button fReplaceAllButton;
	private Button fSkipButton;
	private Button fSkipFileButton;

	
	private List fMarkers;
	private boolean fSkipReadonly= false;
	
	// reuse editors stuff
	private IReusableEditor fEditor;
	private FileSearchPage fPage;
	private ContentAssistHandler fReplaceContentAssistHandler;
	private Label fStatusLabel;

	private boolean fSaved= false;

	protected ReplaceDialog2(Shell parentShell, IFile[] entries, FileSearchPage page) {
		super(parentShell);
		Assert.isNotNull(entries);
		Assert.isNotNull(page.getInput());
		fPage= page;
		fMarkers= new ArrayList();
		initializeMarkers(entries);
	}
	
	private boolean isRegexQuery() {
		return ((FileSearchQuery)fPage.getInput().getQuery()).isRegexSearch();
	}
	
	private void initializeMarkers(IFile[] entries) {
		for (int j= 0; j < entries.length; j++) {
			IFile entry = entries[j];
			Match[] matches= fPage.getDisplayedMatches(entry);
			for (int i= 0; i < matches.length; i++) {
				fMarkers.add(matches[i]);
			}
		}
	}
	
	// widget related stuff -----------------------------------------------------------
	public void create() {
		super.create();
		Shell shell= getShell();
		shell.setText(getDialogTitle());
		gotoCurrentMarker();
		enableButtons();
	}
		
	public int open() {
		boolean wasAutobuild = false;
		try {
			wasAutobuild= disableAutobuild();
		} catch (CoreException e) {
			ExceptionHandler.handle(e, getShell(), getDialogTitle(), SearchMessages.getString("ReplaceDialog2.error.disableAutobuild")); //$NON-NLS-1$
		}
		try {
			return super.open();
		} finally {
			if (wasAutobuild)
				try {
					restoreAutobuild();
				} catch (CoreException e1) {
					ExceptionHandler.handle(e1, getShell(), getDialogTitle(), SearchMessages.getString("ReplaceDialog2.error.restoreAutobuild")); //$NON-NLS-1$
					return CANCEL;
				}
		}
	}	

	private void restoreAutobuild() throws CoreException {
		// this is only called if autobuild was on before.
		IWorkspace workspace= ResourcesPlugin.getWorkspace();
		IWorkspaceDescription description = workspace.getDescription();
		description.setAutoBuilding(true);
		workspace.setDescription(description);

		if (fSaved) {
			new GlobalBuildAction(fPage.getSite().getWorkbenchWindow(), IncrementalProjectBuilder.INCREMENTAL_BUILD).run();
		}
	}

	private boolean disableAutobuild() throws CoreException {
		IWorkspace workspace = ResourcesPlugin.getWorkspace();
		boolean autobuild = workspace.isAutoBuilding();
		if (autobuild) {
			IWorkspaceDescription description = workspace.getDescription();
			description.setAutoBuilding(false);
			workspace.setDescription(description);
		}
		return autobuild;
	}

	protected Control createPageArea(Composite parent) {
		WorkbenchHelp.setHelp(parent, ISearchHelpContextIds.REPLACE_DIALOG);
		Composite result= new Composite(parent, SWT.NULL);
		GridLayout layout= new GridLayout();
		result.setLayout(layout);
		layout.numColumns= 2;
		
		layout.marginHeight = convertVerticalDLUsToPixels(IDialogConstants.VERTICAL_MARGIN);
		layout.marginWidth = convertHorizontalDLUsToPixels(IDialogConstants.HORIZONTAL_MARGIN);
		layout.verticalSpacing = convertVerticalDLUsToPixels(IDialogConstants.VERTICAL_SPACING);
		layout.horizontalSpacing = convertHorizontalDLUsToPixels(IDialogConstants.HORIZONTAL_SPACING);
		
		initializeDialogUnits(result);
		
		Label label= new Label(result, SWT.NONE);
		label.setText(SearchMessages.getString("ReplaceDialog.replace_label")); //$NON-NLS-1$
		Text clabel= new Text(result, SWT.BORDER);
		clabel.setEnabled(false);
		clabel.setText(((FileSearchQuery)fPage.getInput().getQuery()).getSearchString());
		GridData gd= new GridData(GridData.FILL_HORIZONTAL);
		gd.widthHint= convertWidthInCharsToPixels(50);
		clabel.setLayoutData(gd);
		
		
		label= new Label(result, SWT.NONE);
		label.setText(SearchMessages.getString("ReplaceDialog.with_label")); //$NON-NLS-1$
		fTextField= new Text(result, SWT.BORDER);
		gd= new GridData(GridData.FILL_HORIZONTAL);
		gd.widthHint= convertWidthInCharsToPixels(50);
		fTextField.setLayoutData(gd);
		fTextField.setFocus();
		
		
		new Label(result, SWT.NONE);
		fReplaceWithRegex= new Button(result, SWT.CHECK);
		fReplaceWithRegex.setText(SearchMessages.getString("ReplaceDialog.isRegex.label"));//$NON-NLS-1$
		fReplaceWithRegex.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				setContentAssistsEnablement(fReplaceWithRegex.getSelection());
			}
		});
		if (isRegexQuery()) {
			fReplaceWithRegex.setSelection(true);
		} else {
			fReplaceWithRegex.setSelection(false);
			fReplaceWithRegex.setEnabled(false);
		}
	
		
		fStatusLabel= new Label(result, SWT.NULL);
		gd= new GridData(GridData.FILL_HORIZONTAL);
		gd.verticalAlignment= SWT.BOTTOM;
		gd.horizontalSpan= 2;
		fStatusLabel.setLayoutData(gd);

		setContentAssistsEnablement(fReplaceWithRegex.getSelection());
		
		applyDialogFont(result);
		return result;
	}
	
	protected void createButtonsForButtonBar(Composite parent) {
		fReplaceButton= createButton(parent, REPLACE, SearchMessages.getString("ReplaceDialog.replace"), true); //$NON-NLS-1$
		fReplaceAllInFileButton= createButton(parent, REPLACE_ALL_IN_FILE, SearchMessages.getString("ReplaceDialog.replaceAllInFile"), false); //$NON-NLS-1$

		Label filler= new Label(parent, SWT.NONE);
		filler.setLayoutData(new GridData(GridData.FILL_HORIZONTAL | GridData.GRAB_HORIZONTAL));
		
		fReplaceAllButton= createButton(parent, REPLACE_ALL, SearchMessages.getString("ReplaceDialog.replaceAll"), false); //$NON-NLS-1$
		fSkipButton= createButton(parent, SKIP, SearchMessages.getString("ReplaceDialog.skip"), false); //$NON-NLS-1$
		fSkipFileButton= createButton(parent, SKIP_FILE, SearchMessages.getString("ReplaceDialog.skipFile"), false); //$NON-NLS-1$

		filler= new Label(parent, SWT.NONE);
		filler.setLayoutData(new GridData(GridData.FILL_HORIZONTAL | GridData.GRAB_HORIZONTAL));
		super.createButtonsForButtonBar(parent);
		((GridLayout)parent.getLayout()).numColumns= 4;
	}
	
	private void enableButtons() {
		fSkipButton.setEnabled(hasNextMarker());
		fSkipFileButton.setEnabled(hasNextFile());
		fReplaceButton.setEnabled(canReplace());
		fReplaceAllInFileButton.setEnabled(canReplace());
		fReplaceAllButton.setEnabled(canReplace());
	}
	
	protected void buttonPressed(int buttonId) {
		final String replaceText= fTextField.getText();
		statusMessage(false, ""); //$NON-NLS-1$
		try {
			switch (buttonId) {
				case SKIP :
					skip();
					break;
				case SKIP_FILE :
					skipFile();
					break;
				case REPLACE :
					run(new ReplaceOperation() {
						protected void doReplace(IProgressMonitor pm) throws BadLocationException, CoreException {
							replace(pm, replaceText);
						}
					}, (IResource) getCurrentMarker().getElement());
					gotoCurrentMarker();
					break;
				case REPLACE_ALL_IN_FILE :
					run(new ReplaceOperation() {
						protected void doReplace(IProgressMonitor pm) throws BadLocationException, CoreException {
							replaceInFile(pm, replaceText);
						}
					}, (IResource) getCurrentMarker().getElement());
					gotoCurrentMarker();
					break;
				case REPLACE_ALL :
					run(new ReplaceOperation() {
						protected void doReplace(IProgressMonitor pm) throws BadLocationException, CoreException {
							replaceAll(pm, replaceText);
						}
					}, ResourcesPlugin.getWorkspace().getRoot());
					gotoCurrentMarker();
					break;
				default : {
					super.buttonPressed(buttonId);
					return;
				}
			}
		} catch (InvocationTargetException e) {
			Throwable targetException= e.getTargetException();
			if (targetException instanceof PatternSyntaxException) {
				String format= SearchMessages.getString("ReplaceDialog2.regexError.format"); //$NON-NLS-1$
				String message= MessageFormat.format(format, new Object[] { targetException.getLocalizedMessage() });
				statusMessage(true, message);
			} else {
				String message= SearchMessages.getFormattedString("ReplaceDialog.error.unable_to_replace", ((IFile)getCurrentMarker().getElement()).getName()); //$NON-NLS-1$
				ExceptionHandler.handle(e, getParentShell(), getDialogTitle(), message);
			}
		} catch (InterruptedException e) {
			// means operation canceled
		}
		if (!hasNextMarker() && !hasNextFile() && !canReplace())
			close();
		else {
			enableButtons();
		}
	}
	
	private void run(ReplaceOperation operation, IResource resource) throws InvocationTargetException, InterruptedException {
		IResourceRuleFactory ruleFactory = ResourcesPlugin.getWorkspace().getRuleFactory();
		ISchedulingRule rule= ruleFactory.modifyRule(resource);
		
		PlatformUI.getWorkbench().getProgressService().runInUI(this, operation, rule);	
	}
	
	private Match getCurrentMarker() {
		return (Match)fMarkers.get(0);
	}
	
	private void replace(IProgressMonitor pm, String replacementText) throws BadLocationException, CoreException {
		Match marker= getCurrentMarker();
		pm.beginTask(SearchMessages.getString("ReplaceDialog.task.replace"), 10); //$NON-NLS-1$
		replaceInFile(pm, (IFile) marker.getElement(), replacementText, new Match[]{marker});
	}
	
	private void replaceInFile(IProgressMonitor pm, String replacementText) throws BadLocationException, CoreException {
		Match firstMarker= getCurrentMarker();
		Match[] markers= collectMarkers((IFile)firstMarker.getElement());
		pm.beginTask(SearchMessages.getFormattedString("ReplaceDialog.task.replaceInFile", ((IFile)firstMarker.getElement()).getFullPath().toOSString()), 4); //$NON-NLS-1$
		replaceInFile(pm, (IFile) firstMarker.getElement(), replacementText, markers);
	}
	
	private void replaceAll(IProgressMonitor pm, String replacementText) throws BadLocationException, CoreException {
		int resourceCount= countResources();
		pm.beginTask(SearchMessages.getString("ReplaceDialog.task.replace.replaceAll"), resourceCount); //$NON-NLS-1$
		while (fMarkers.size() > 0) {
			replaceInFile(new SubProgressMonitor(pm, 1, 0), replacementText);
		}
		pm.done();
	}
	
	private void replaceInFile(final IProgressMonitor pm, final IFile file, final String replacementText, final Match[] markers) throws BadLocationException, CoreException {
		if (pm.isCanceled())
			throw new OperationCanceledException();
		doReplaceInFile(pm, file, replacementText, markers);
	}
	
	private void doReplaceInFile(IProgressMonitor pm, IFile file, String replacementText, final Match[] markers) throws BadLocationException, CoreException {
		Pattern pattern= null;
		if (fReplaceWithRegex.getSelection()) {
			pattern= createReplacePattern();
		}
		try {
			if (file.isReadOnly()) {
				if (fSkipReadonly) {
					skipFile();
					return;
				}
				int rc= askForSkip(file);
				switch (rc) {
					case CANCEL :
						throw new OperationCanceledException();
					case SKIP_FILE :
						skipFile();
						return;
					case SKIP_ALL :
						fSkipReadonly= true;
						skipFile();
						return;
				}
			}
			ITextFileBufferManager bm= FileBuffers.getTextFileBufferManager();
			try {
				bm.connect(file.getFullPath(), new SubProgressMonitor(pm, 1));
				ITextFileBuffer fb= bm.getTextFileBuffer(file.getFullPath());
				boolean wasDirty= fb.isDirty();
				IDocument doc= fb.getDocument();
					for (int i= 0; i < markers.length; i++) {
						PositionTracker tracker= InternalSearchUI.getInstance().getPositionTracker();
						int offset= markers[i].getOffset();
						int length= markers[i].getLength();
						Position currentPosition= tracker.getCurrentPosition(markers[i]);
						if (currentPosition != null) {
							offset= currentPosition.offset;
							length= currentPosition.length;
						}
						String originalText= doc.get(offset, length);
						String replacementString= computeReplacementString(pattern, originalText, replacementText);
						doc.replace(offset, length, replacementString);
						fMarkers.remove(0);
						fPage.getInput().removeMatch(markers[i]);
					}
				if (!wasDirty) {
					fb.commit(new SubProgressMonitor(pm, 1), true);
					fSaved= true;
				}
			} finally {
				bm.disconnect(file.getFullPath(), new SubProgressMonitor(pm, 1));
			}
		} finally {
			pm.done();
		}
	}
	
	private Pattern createReplacePattern() {
		FileSearchQuery query= (FileSearchQuery)fPage.getInput().getQuery();
		if (!query.isCaseSensitive())
			return Pattern.compile(query.getSearchString(), Pattern.CASE_INSENSITIVE);
		else
			return Pattern.compile(query.getSearchString());
	}

	private String computeReplacementString(Pattern pattern, String originalText, String replacementText) {
		if (pattern != null) {
			try {
				return pattern.matcher(originalText).replaceFirst(replacementText);
			} catch (IndexOutOfBoundsException ex) {
				throw new PatternSyntaxException(ex.getLocalizedMessage(), replacementText, -1);
			}
		}
		return replacementText;
	}

	private int askForSkip(final IFile file) {
		
		String message= SearchMessages.getFormattedString("ReadOnlyDialog.message", file.getFullPath().toOSString()); //$NON-NLS-1$
		String[] buttonLabels= null;
		boolean showSkip= countResources() > 1;
		if (showSkip) {
			String skipLabel= SearchMessages.getString("ReadOnlyDialog.skipFile"); //$NON-NLS-1$
			String skipAllLabel= SearchMessages.getString("ReadOnlyDialog.skipAll"); //$NON-NLS-1$
			buttonLabels= new String[]{skipLabel, skipAllLabel, IDialogConstants.CANCEL_LABEL};
		} else {
			buttonLabels= new String[]{IDialogConstants.CANCEL_LABEL};
			
		}
		
		MessageDialog msd= new MessageDialog(getShell(), getShell().getText(), null, message, MessageDialog.ERROR, buttonLabels, 0);
		int rc= msd.open();
		switch (rc) {
			case 0 :
				return showSkip ? SKIP_FILE : CANCEL;
			case 1 :
				return SKIP_ALL;
			default :
				return CANCEL;
		}
	}
		
	private String getDialogTitle() {
		return SearchMessages.getString("ReplaceDialog.dialog.title"); //$NON-NLS-1$
	}
	
	private void skip() {
		fMarkers.remove(0);
		Assert.isTrue(fMarkers.size() > 0);
		gotoCurrentMarker();
	}
	
	private void skipFile() {
		Match currentMarker= getCurrentMarker();
		if (currentMarker == null)
			return;
		IResource currentFile= (IResource) currentMarker.getElement();
		while (fMarkers.size() > 0 && getCurrentMarker().getElement().equals(currentFile))
			fMarkers.remove(0);
		gotoCurrentMarker();
	}
	
	private void gotoCurrentMarker() {
		if (fMarkers.size() > 0) {
			Match marker= getCurrentMarker();
			Control focusControl= getShell().getDisplay().getFocusControl();
			try {
				selectEntry(marker);
				ITextEditor editor= null;
				if (NewSearchUI.reuseEditor())
					editor= openEditorReuse(marker);
				else
					editor= openEditorNoReuse(marker);
				Position p= InternalSearchUI.getInstance().getPositionTracker().getCurrentPosition(marker);
				if (p != null)
					editor.selectAndReveal(p.getOffset(), p.getLength());
				else
				editor.selectAndReveal(marker.getOffset(), marker.getLength());
				if (focusControl != null && !focusControl.isDisposed())
					focusControl.setFocus();
			} catch (PartInitException e) {
				String message= SearchMessages.getFormattedString("ReplaceDialog.error.unable_to_open_text_editor", ((IFile)marker.getElement()).getName()); //$NON-NLS-1$
				MessageDialog.openError(getParentShell(), getDialogTitle(), message);
			}
		}
	}
	
	private void selectEntry(Match marker) {
		ISelection sel= fPage.getViewer().getSelection();
		if (!(sel instanceof IStructuredSelection))
			return;
		IStructuredSelection ss= (IStructuredSelection) sel;
		IFile file= (IFile) marker.getElement();
		if (ss.size() == 1 && file.equals(ss.getFirstElement()))
			return;
		fPage.getViewer().setSelection(new StructuredSelection(marker.getElement()));
	}

	// opening editors ------------------------------------------
	private ITextEditor openEditorNoReuse(Match marker) throws PartInitException {
		IFile file= (IFile) marker.getElement();
		IWorkbenchPage activePage= SearchPlugin.getActivePage();
		if (activePage == null)
			return null;
		ITextEditor textEditor= showOpenTextEditor(activePage, file);
		if (textEditor != null)
			return textEditor;
		return openNewTextEditor(file, activePage);
	}
	
	private ITextEditor openNewTextEditor(IFile file, IWorkbenchPage activePage) throws PartInitException {
		IEditorDescriptor desc= IDE.getDefaultEditor(file);
		if (desc != null) {
			String editorID= desc.getId();
			IEditorPart editor;
			if (desc.isInternal()) {
				editor= activePage.openEditor(new FileEditorInput(file), editorID);
				if (editor instanceof ITextEditor) {
					if (editor instanceof IReusableEditor)
						fEditor= (IReusableEditor) editor;
					return (ITextEditor)editor;
				} else
					activePage.closeEditor(editor, false);
			}
		}
		IEditorPart editor= activePage.openEditor(new FileEditorInput(file), "org.eclipse.ui.DefaultTextEditor"); //$NON-NLS-1$
		return (ITextEditor)editor;
	}

	private ITextEditor openEditorReuse(Match marker) throws PartInitException {
		IWorkbenchPage page= SearchPlugin.getActivePage();
		IFile file= (IFile) marker.getElement();
		if (page == null)
			return null;

		ITextEditor textEditor= showOpenTextEditor(page, file);
		if (textEditor != null)
			return textEditor;

		String editorId= null;
		IEditorDescriptor desc= IDE.getDefaultEditor(file);
		if (desc != null && desc.isInternal())
			editorId= desc.getId();

		boolean isOpen= isEditorOpen(page, fEditor);

		boolean canBeReused= isOpen && !fEditor.isDirty() && !isPinned(fEditor);
		boolean showsSameInputType= fEditor != null && (editorId == null || fEditor.getSite().getId().equals(editorId));

		if (canBeReused) {
			if (showsSameInputType) {
				fEditor.setInput(new FileEditorInput(file));
				page.bringToTop(fEditor);
				return (ITextEditor) fEditor;
			} else {
				page.closeEditor(fEditor, false);
				fEditor= null;
			}
		}
		return openNewTextEditor(file, page);
	}

	private boolean isEditorOpen(IWorkbenchPage page, IEditorPart editor) {
		if (editor != null) {
			IEditorReference[] parts= page.getEditorReferences();
			int i= 0;
			for (int j = 0; j < parts.length; j++) {
				if (editor == parts[i++].getEditor(false))
					return true;
			}
		}
		return false;
	}

	private ITextEditor showOpenTextEditor(IWorkbenchPage page, IFile file) {
		IEditorPart editor= page.findEditor(new FileEditorInput(file));
		if (editor instanceof ITextEditor) {
			page.bringToTop(editor);
			return (ITextEditor) editor;
		}
		return null;
	}

	private boolean isPinned(IEditorPart editor) {
		if (editor == null)
			return false;
		
		IEditorReference[] editorRefs= editor.getEditorSite().getPage().getEditorReferences();
		int i= 0;
		while (i < editorRefs.length) {
			if (editor.equals(editorRefs[i].getEditor(false)))
				return editorRefs[i].isPinned();
			i++;
		}
		return false;
	}
	
	// resource related  -------------------------------------------------------------
	/**
	 * @return the number of resources referred to in fMarkers
	 */
	private int countResources() {
		IResource r= null;
		int count= 0;
		for (Iterator elements= fMarkers.iterator(); elements.hasNext(); ) {
			Match element= (Match)elements.next();
			if (!element.getElement().equals(r)) {
				count++;
				r= (IResource) element.getElement();
			}
		}
		return count;
	}
	
	private Match[] collectMarkers(IFile resource) {
		List matching= new ArrayList();
		for (int i= 0; i < fMarkers.size(); i++) {
			Match marker= (Match)fMarkers.get(i);
			if (!resource.equals(marker.getElement()))
				break;
			matching.add(marker);
		}
		Match[] markers= new Match[matching.size()];
		return (Match[])matching.toArray(markers);
	}
	
	
	// some queries -------------------------------------------------------------
	private boolean hasNextMarker() {
		return fMarkers.size() > 1;
	}
	
	private boolean hasNextFile() {
		if (!hasNextMarker())
			return false;
		IResource currentFile= (IResource) getCurrentMarker().getElement();
		for (int i= 0; i < fMarkers.size(); i++) {
			if (!((Match)fMarkers.get(i)).getElement().equals(currentFile))
				return true;
		}
		return false;
	}
	
	private boolean canReplace() {
		return fMarkers.size() > 0;
	}
	
	public static SubjectControlContentAssistant createContentAssistant(ArrayList proposalKeys) {
		final SubjectControlContentAssistant contentAssistant= new SubjectControlContentAssistant();
		
		contentAssistant.setRestoreCompletionProposalSize(SearchPlugin.getDefault().getDialogSettings()); //$NON-NLS-1$
		
		IContentAssistProcessor processor= new RegExContentAssistProcessor(proposalKeys);
		contentAssistant.setContentAssistProcessor(processor, IDocument.DEFAULT_CONTENT_TYPE);
		
		contentAssistant.setContextInformationPopupOrientation(IContentAssistant.CONTEXT_INFO_ABOVE);
		contentAssistant.setInformationControlCreator(new IInformationControlCreator() {
			/*
			 * @see org.eclipse.jface.text.IInformationControlCreator#createInformationControl(org.eclipse.swt.widgets.Shell)
			 */
			public IInformationControl createInformationControl(Shell parent) {
				return new DefaultInformationControl(parent);
			}});
		
		return contentAssistant;
	}
	
	private void setContentAssistsEnablement(boolean enable) {
		if (enable) {
			if (fReplaceContentAssistHandler == null) {
				fReplaceContentAssistHandler= ContentAssistHandler.createHandlerForText(fTextField, createContentAssistant(RegExContentAssistProcessor.fgReplaceProposalKeys));
			}
			fReplaceContentAssistHandler.setEnabled(true);
			
		} else {
			if (fReplaceContentAssistHandler == null)
				return;
			fReplaceContentAssistHandler.setEnabled(false);
		}
	}

	private void statusMessage(boolean error, String message) {
		fStatusLabel.setText(message);
	
		if (error)
			fStatusLabel.setForeground(JFaceColors.getErrorText(fStatusLabel.getDisplay()));
		else
			fStatusLabel.setForeground(null);
	
		if (error)
			getShell().getDisplay().beep();
	}


}
