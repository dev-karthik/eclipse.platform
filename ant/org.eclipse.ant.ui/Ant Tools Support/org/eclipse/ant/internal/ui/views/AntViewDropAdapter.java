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
package org.eclipse.ant.internal.ui.views;

import org.eclipse.ant.internal.ui.model.AntUtil;
import org.eclipse.ant.internal.ui.views.elements.ProjectNode;
import org.eclipse.core.resources.IFile;
import org.eclipse.swt.custom.BusyIndicator;
import org.eclipse.swt.dnd.DropTargetAdapter;
import org.eclipse.swt.dnd.DropTargetEvent;

/**
 * A drop adapter which adds files to the Ant view.
 */
public class AntViewDropAdapter extends DropTargetAdapter {
	
	private AntView view;
	
	/**
	 * Creates a new drop adapter for the given Ant view.
	 * @param view the view which dropped files will be added to
	 */
	public AntViewDropAdapter(AntView view) {
		this.view= view;
	}

	/**
	 * @see
	 * org.eclipse.swt.dnd.DropTargetListener#drop(org.eclipse.swt.dnd.DropTargetEvent)
	 */
	public void drop(DropTargetEvent event) {
		Object data = event.data;
		if (data instanceof String[]) {
			final String[] strings = (String[]) data;
			BusyIndicator.showWhile(null, new Runnable() {
				public void run() {
					for (int i = 0; i < strings.length; i++) {
						processString(strings[i]);
					}
				}
			});
		}
	}
	
	/**
	 * Attempts to process the given string as a path to an
	 * XML file. If the string is determined to be a path to an
	 * XML file in the workspace, that file is added to the Ant
	 * view.
	 * @param buildFileName the string to process
	 */
	private void processString(String buildFileName) {
		IFile buildFile = AntUtil.getFileForLocation(buildFileName, null);
		if (buildFile == null || !buildFileName.toLowerCase().endsWith(".xml")) { //$NON-NLS-1$
			return;
		}
		buildFileName = buildFile.getFullPath().toString();
		ProjectNode[] existingProjects = view.getProjects();
		for (int j = 0; j < existingProjects.length; j++) {
			ProjectNode existingProject = existingProjects[j];
			if (existingProject.getBuildFileName().equals(buildFileName)) {
				// Don't parse projects that have already been added.
				return;
			}
		}
		ProjectNode project = new ProjectNode(buildFileName);
		view.addProject(project);
	}
}