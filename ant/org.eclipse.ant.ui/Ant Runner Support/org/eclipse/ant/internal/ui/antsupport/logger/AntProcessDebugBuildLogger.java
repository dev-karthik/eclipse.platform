/*******************************************************************************
 * Copyright (c) 2004, 2005 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.ant.internal.ui.antsupport.logger;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.apache.tools.ant.BuildEvent;
import org.apache.tools.ant.Location;
import org.eclipse.ant.internal.ui.antsupport.logger.util.AntDebugState;
import org.eclipse.ant.internal.ui.antsupport.logger.util.AntDebugUtil;
import org.eclipse.ant.internal.ui.antsupport.logger.util.IDebugBuildLogger;
import org.eclipse.ant.internal.ui.debug.IAntDebugController;
import org.eclipse.ant.internal.ui.debug.model.AntDebugTarget;
import org.eclipse.ant.internal.ui.debug.model.AntThread;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.debug.core.DebugEvent;
import org.eclipse.debug.core.ILaunch;
import org.eclipse.debug.core.model.IBreakpoint;
import org.eclipse.debug.core.model.ILineBreakpoint;
import org.eclipse.debug.core.model.IProcess;

public class AntProcessDebugBuildLogger extends AntProcessBuildLogger implements IAntDebugController, IDebugBuildLogger {
	
	private AntDebugState fDebugState= null;
	
	private List fBreakpoints= null;
    
	private AntDebugTarget fAntDebugTarget;
	
	/* (non-Javadoc)
	 * @see org.apache.tools.ant.BuildListener#buildStarted(org.apache.tools.ant.BuildEvent)
	 */
	public void buildStarted(BuildEvent event) {
		fDebugState= new AntDebugState(this);
		super.buildStarted(event);
		IProcess process= getAntProcess(fProcessId);
		ILaunch launch= process.getLaunch();
		fAntDebugTarget= new AntDebugTarget(launch, process, this);
		launch.addDebugTarget(fAntDebugTarget);
        
        fAntDebugTarget.buildStarted();
	}

	/* (non-Javadoc)
	 * @see org.apache.tools.ant.BuildListener#taskFinished(org.apache.tools.ant.BuildEvent)
	 */
	public void taskFinished(BuildEvent event) {
		super.taskFinished(event);
		AntDebugUtil.taskFinished(fDebugState);
	}
	
	/* (non-Javadoc)
	 * @see org.apache.tools.ant.BuildListener#taskStarted(org.apache.tools.ant.BuildEvent)
	 */
	public void taskStarted(BuildEvent event) {
        super.taskStarted(event);
        AntDebugUtil.taskStarted(event, fDebugState);
	}
	
	public synchronized void waitIfSuspended() {
		IBreakpoint breakpoint= breakpointAtLineNumber(getBreakpointLocation());
		if (breakpoint != null) {
			 fAntDebugTarget.breakpointHit(breakpoint);
			 try {
				 wait();
			 } catch (InterruptedException e) {
			 }
		} else if (fDebugState.getCurrentTask() != null) {
			int detail= -1;
	        boolean shouldSuspend= true;
	        if (fDebugState.isStepIntoSuspend()) {
	            detail= DebugEvent.STEP_END;
	            fDebugState.setStepIntoSuspend(false);               
	        } else if ((fDebugState.getLastTaskFinished() != null && fDebugState.getLastTaskFinished() == fDebugState.getStepOverTask()) || fDebugState.shouldSuspend()) {
				detail= DebugEvent.STEP_END;
				fDebugState.setShouldSuspend(false);
				fDebugState.setStepOverTask(null);
	        } else if (fDebugState.isClientSuspend()) {
	            detail= DebugEvent.CLIENT_REQUEST;
	            fDebugState.setClientSuspend(false);
	        } else {
	            shouldSuspend= false;
	        }
	        if (shouldSuspend) {
                fAntDebugTarget.suspended(detail);
	            try {
	                wait();
	            } catch (InterruptedException e) {
	            }
	        }
	    }
	}

	/* (non-Javadoc)
	 * @see org.eclipse.ant.internal.ui.debug.IAntDebugController#resume()
	 */
	public synchronized void resume() {
        notifyAll();
	}

	/* (non-Javadoc)
	 * @see org.eclipse.ant.internal.ui.debug.IAntDebugController#suspend()
	 */
	public synchronized void suspend() {
		fDebugState.setClientSuspend(true);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.ant.internal.ui.debug.IAntDebugController#stepInto()
	 */
	public synchronized void stepInto() {
		fDebugState.setStepIntoSuspend(true);
		notifyAll();
	}

	/* (non-Javadoc)
	 * @see org.eclipse.ant.internal.ui.debug.IAntDebugController#stepOver()
	 */
	public synchronized void stepOver() {
		AntDebugUtil.stepOver(fDebugState);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.ant.internal.ui.debug.IAntDebugController#handleBreakpoint(org.eclipse.debug.core.model.IBreakpoint, boolean)
	 */
	public void handleBreakpoint(IBreakpoint breakpoint, boolean added) {
		if (added) {
			if (fBreakpoints == null) {
				fBreakpoints= new ArrayList();
			}
			if (!fBreakpoints.contains(breakpoint)) {
				fBreakpoints.add(breakpoint);
			}
		} else {
			fBreakpoints.remove(breakpoint);
		}
	}

	/* (non-Javadoc)
	 * @see org.eclipse.ant.internal.ui.debug.IAntDebugController#getProperties()
	 */
	public void getProperties() {
		if (!fAntDebugTarget.isSuspended()) {
			return;
		}
	    StringBuffer propertiesRepresentation= new StringBuffer();
		fDebugState.marshalProperties(propertiesRepresentation, true);
		if (fAntDebugTarget.getThreads().length > 0) {
			((AntThread) fAntDebugTarget.getThreads()[0]).newProperties(propertiesRepresentation.toString());
		}
	}

	/* (non-Javadoc)
	 * @see org.eclipse.ant.internal.ui.debug.IAntDebugController#getStackFrames()
	 */
	public void getStackFrames() {
		StringBuffer stackRepresentation= new StringBuffer();
		AntDebugUtil.marshalStack(stackRepresentation, fDebugState);
		((AntThread) fAntDebugTarget.getThreads()[0]).buildStack(stackRepresentation.toString());
	}
    
    private IBreakpoint breakpointAtLineNumber(Location location) {
        if (fBreakpoints == null || location == null || location == Location.UNKNOWN_LOCATION) {
            return null;
        }
        int lineNumber= AntDebugUtil.getLineNumber(location);
        File locationFile= new File(AntDebugUtil.getFileName(location));
        for (int i = 0; i < fBreakpoints.size(); i++) {
            ILineBreakpoint breakpoint = (ILineBreakpoint) fBreakpoints.get(i);
            int breakpointLineNumber;
            try {
            	if (!breakpoint.isEnabled()) {
                	continue;
                }
            	breakpointLineNumber = breakpoint.getLineNumber();
            } catch (CoreException e) {
               return null;
            }
            IFile resource= (IFile) breakpoint.getMarker().getResource();
            if (breakpointLineNumber == lineNumber && resource.getLocation().toFile().equals(locationFile)) {
                return breakpoint;
            }
        }
        return null;
    }
    
    /* (non-Javadoc)
     * @see org.apache.tools.ant.BuildListener#targetStarted(org.apache.tools.ant.BuildEvent)
     */
    public void targetStarted(BuildEvent event) {
		AntDebugUtil.targetStarted(event, fDebugState);
		waitIfSuspended();
		super.targetStarted(event);
    }
	
	/* (non-Javadoc)
	 * @see org.apache.tools.ant.BuildListener#targetFinished(org.apache.tools.ant.BuildEvent)
	 */
	public void targetFinished(BuildEvent event) {
		super.targetFinished(event);
		fDebugState.setTargetExecuting(null);
	}	
	
	private Location getBreakpointLocation() {
		if (fDebugState.getCurrentTask() != null) {
			return fDebugState.getCurrentTask().getLocation();
		}
		if (fDebugState.considerTargetBreakpoints() && fDebugState.getTargetExecuting() != null) {
			return AntDebugUtil.getLocation(fDebugState.getTargetExecuting());
		}
		return null;
	}
}
