/*******************************************************************************
 * Copyright (c) 2003, 2005 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - Initial API and implementation
 *******************************************************************************/
package org.eclipse.ui.internal.browser;

import java.io.File;

import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.PlatformUI;
/**
 * 
 */
public class BrowserDescriptorDialog extends Dialog {
	protected IBrowserDescriptorWorkingCopy browser;
	protected boolean isEdit;
	protected Button newPageCheckbox;
	protected Button clearHistoryCheckbox;
	protected Button browseButton;
	protected Text browserNameTextfield;
	protected Text browserLocationTextfield;
	protected Text browserParametersTextfield;
	private Button okButton;
	
	interface StringModifyListener {
		public void valueChanged(String s);
	}
	
	/**
	 * @param parentShell
	 */
	public BrowserDescriptorDialog(Shell parentShell, IBrowserDescriptorWorkingCopy browser) {
		super(parentShell);
		this.browser = browser;
		isEdit = true;
	}

	public BrowserDescriptorDialog(Shell parentShell) {
		super(parentShell);
		browser = BrowserManager.getInstance().createExternalWebBrowser();
		isEdit = false;
	}

	protected void configureShell(Shell shell) {
		super.configureShell(shell);
		
		if (isEdit)
			shell.setText(Messages.editExternalBrowser);
		else
			shell.setText(Messages.createBrowser);
	}

	protected Text createText(Composite comp, String txt, final StringModifyListener listener) {
		final Text text = new Text(comp, SWT.BORDER);
		if (txt != null)
			text.setText(txt);
		GridData data = new GridData(GridData.HORIZONTAL_ALIGN_FILL | GridData.VERTICAL_ALIGN_BEGINNING);
		data.widthHint = 250;
		text.setLayoutData(data);
		if (listener != null)
			text.addModifyListener(new ModifyListener() {
				public void modifyText(ModifyEvent e) {	
					listener.valueChanged(text.getText());
				}
			});
		return text;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jface.dialogs.Dialog#createDialogArea(org.eclipse.swt.widgets.Composite)
	 */
	protected Control createDialogArea(Composite parent) {
		Composite composite = (Composite) super.createDialogArea(parent);
		((GridLayout)composite.getLayout()).numColumns = 3;
		
		PlatformUI.getWorkbench().getHelpSystem().setHelp(composite, ContextIds.PREF_BROWSER_DIALOG);
		
		SWTUtil.createLabel(composite, Messages.name);
		browserNameTextfield = createText(composite, browser.getName(), new StringModifyListener() {
			public void valueChanged(String s) {
				browser.setName(s);
				validateFields();
			}
		});
		
		new Label(composite, SWT.NONE);
	
		SWTUtil.createLabel(composite, Messages.location);
		browserLocationTextfield = createText(composite, browser.getLocation(), new StringModifyListener() {
			public void valueChanged(String s) {
				browser.setLocation(s);
				validateFields();
			}
		});		
		
		browseButton = SWTUtil.createButton(composite, Messages.browse);
		browseButton.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				FileDialog dialog = new FileDialog(getShell(), SWT.OPEN);
				dialog.setText(Messages.browseMessage);
				
				String fname = browserLocationTextfield.getText();
				
				dialog.setFileName(fname);
				fname = dialog.open();
				
				if (fname != null)
					browserLocationTextfield.setText(fname);
			}
		});
		
		SWTUtil.createLabel(composite, Messages.parameters);
		browserParametersTextfield = createText(composite, browser.getParameters(), new StringModifyListener() {
			public void valueChanged(String s) {
				browser.setParameters(s);
			}
		});

		new Label(composite, SWT.NONE);
		
		new Label(composite, SWT.NONE);
		Label urlLabel = new Label(composite, SWT.NONE);
		urlLabel.setText(NLS.bind(Messages.parametersMessage, IBrowserDescriptor.URL_PARAMETER));
		
		return composite;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jface.dialogs.Dialog#okPressed()
	 */
	protected void okPressed() {
		// do simple field validation to at least ensure target directory entered is valid pathname
		try {
		File file = new File(browser.getLocation());
			if(!file.isFile()){
				WebBrowserUtil.openError(Messages.locationInvalid);
				return;
			}
		}
		catch(Exception e){
			WebBrowserUtil.openError(Messages.locationInvalid);
			return;
		}
		
		browser.save();
		super.okPressed();
	}
	
	private void setOKButtonEnabled(boolean curIsEnabled) {
		if (okButton == null)
			okButton = getButton(IDialogConstants.OK_ID);
		
		if (okButton != null)
			okButton.setEnabled(curIsEnabled);
	}
	
	protected Control createButtonBar(Composite parent) {
		Control buttonControl = super.createButtonBar(parent);
		validateFields();
		return buttonControl;
	}
	
	protected void validateFields() {
		boolean valid = true;
		
		String name = browserNameTextfield.getText();
		if (name == null || name.trim().length() < 1)
			valid = false;
		
		String location = browserLocationTextfield.getText();
		if (location == null || location.trim().length() < 1)
			valid = false;
		
		setOKButtonEnabled(valid);
	}
}