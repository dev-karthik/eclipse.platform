/*******************************************************************************
 * Copyright (c) 2000, 2003 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.update.internal.ui.properties;
import org.eclipse.swt.widgets.*;
import org.eclipse.ui.dialogs.PropertyPage;
import org.eclipse.ui.help.WorkbenchHelp;
import org.eclipse.ui.IWorkbenchPropertyPage;
import org.eclipse.swt.*;
import org.eclipse.swt.layout.*;
import org.eclipse.update.internal.ui.model.*;
import org.eclipse.swt.events.*;
import org.eclipse.update.internal.ui.UpdateUI;
import org.eclipse.update.configuration.*;
import org.eclipse.update.core.*;
import org.eclipse.core.runtime.CoreException;

/**
 * Insert the type's description here.
 * @see PropertyPage
 */
public class ConfigurationPropertyPage extends PropertyPage implements IWorkbenchPropertyPage {
	private static final String KEY_NAME = "ConfigurationPropertyPage.name"; //$NON-NLS-1$
	private Text nameText;
	private boolean changed;
	/**
	 * The constructor.
	 */
	public ConfigurationPropertyPage() {
	}

	/**
	 * Insert the method's description here.
	 * @see PropertyPage#createContents
	 */
	protected Control createContents(Composite parent)  {
		Composite container = new Composite(parent, SWT.NULL);
		GridLayout layout = new GridLayout();
		layout.numColumns = 2;
		container.setLayout(layout);
		
		Label label = new Label(container, SWT.NULL);
		label.setText(UpdateUI.getString(KEY_NAME));
		nameText = new Text(container, SWT.SINGLE|SWT.BORDER);
		GridData gd = new GridData(GridData.FILL_HORIZONTAL);
		nameText.setLayoutData(gd);
		initializeFields();
		WorkbenchHelp.setHelp(container, "org.eclipse.update.ui.ConfigurationPropertyPage");
		return container;
	}
	
	public void performDefaults() {
		IInstallConfiguration config = getConfiguration();
		nameText.setText(config.getLabel());
		super.performDefaults();
	}
	
	public boolean performOk() {
		if (changed) {
			IInstallConfiguration config = getConfiguration();
			config.setLabel(nameText.getText());
			UpdateModel model = UpdateUI.getDefault().getUpdateModel();
			model.fireObjectChanged(getElement(), null);
			try {
				SiteManager.getLocalSite().save();
			}
			catch (CoreException e) {
				UpdateUI.logException(e);
			}
		}
		return true;
	}
	
	private IInstallConfiguration getConfiguration() {
		Object obj = getElement();
		if (obj instanceof PreservedConfiguration)
		   return ((PreservedConfiguration)obj).getConfiguration();
		return (IInstallConfiguration)obj;
	}
	
	private void initializeFields() {
		IInstallConfiguration config = getConfiguration();
		nameText.setText(config.getLabel());
		nameText.addModifyListener(new ModifyListener() {
			public void modifyText(ModifyEvent e) {
				checkFields();
			}
		});
	}
	private void checkFields() {
		boolean valid = true;
		String error = null;
		// check the value
		String text = nameText.getText();
		if (text.length()==0) valid = false;
		else if (text.charAt(0)=='@') {
			error = UpdateUI.getString("ConfigurationPropertyPage.invalidCharacter"); //$NON-NLS-1$
			valid=false;
		}
		setValid(valid);		
		setErrorMessage(error);
		changed=true;
	}
}