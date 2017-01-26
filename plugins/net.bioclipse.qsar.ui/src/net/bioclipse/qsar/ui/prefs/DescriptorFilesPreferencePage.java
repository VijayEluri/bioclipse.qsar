/*******************************************************************************
 * Copyright (c) 2009 Ola Spjuth.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     Ola Spjuth - initial API and implementation
 ******************************************************************************/
package net.bioclipse.qsar.ui.prefs;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;

import net.bioclipse.core.util.LogUtils;
import net.bioclipse.qsar.QSARConstants;
import net.bioclipse.qsar.init.Activator;
import net.bioclipse.qsar.prefs.QsarPreferenceHelper;

import org.apache.log4j.Logger;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.preference.FieldEditorPreferencePage;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.preference.ListEditor;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;

/**
 * 
 * @author ola
 *
 */
public class DescriptorFilesPreferencePage extends FieldEditorPreferencePage 
                                         implements IWorkbenchPreferencePage {

    private static final Logger logger = Logger.getLogger(DescriptorFilesPreferencePage.class);

    
    public DescriptorFilesPreferencePage() {
        super(FieldEditorPreferencePage.GRID);

        // Set the preference store for the preference page.
        IPreferenceStore store =
            Activator.getDefault().getPreferenceStore();
        setPreferenceStore(store);
    }
    
    
    @Override
    protected void createFieldEditors() {
        
        ListEditor filesEditor = new ListEditor(
                                   QSARConstants.QSAR_ONTOLOGY_FILES_PREFERENCE,
                                   "&Additional Descriptor definition files", 
                                   getFieldEditorParent()){

            @Override
            protected String createList( String[] items ) {
                return QsarPreferenceHelper.
                                    createQsarPreferenceStringFromItems(items);
            }

            @Override
            protected String getNewInputObject() {
                FileDialog dlg=new FileDialog(getShell(), SWT.OPEN);
                dlg.setFilterExtensions( new String[]{"*.owl;*.xml;*.rdf"} );
                String pfile=dlg.open();
                if (pfile==null)
                    return null;
                
                File file=new File(pfile);
                URL url;
                try {
                    url = file.toURL();
                } catch ( MalformedURLException e ) {
                    LogUtils.handleException( e, logger, 
                                     net.bioclipse.qsar.ui.Activator.PLUGIN_ID);
                    return null;
                }
 
                //Check if valid and add if so
                if (isValidDescriptorDefinition(url))
                    return url.toString();
                else{
                    showError("The file: " + file + " is not a valid " +
                    		"'descriptor definition file.");
                    
                    return null;
                }
                
            }

            @Override
            protected String[] parseString( String stringList ) {
              return QsarPreferenceHelper.parseQsarPreferenceString(stringList);
            }
            
        };

        addField(filesEditor);
        GridData gd=new GridData(GridData.FILL_HORIZONTAL);
        gd.heightHint=200;
        filesEditor.getListControl(getFieldEditorParent()).setLayoutData(gd);
        
    }

    private void showError(String message) {
        MessageDialog.openError(getShell(), "Error", message);
    }


    /**
     * Validate that the contents at this location is a valid descriptor 
     * definition and is not empty
     * 
     * @param url
     * @return true if valid, false otherwise
     */
    protected boolean isValidDescriptorDefinition( URL url ) {

        // TODO egonw: Implement (filed as bug 1537)
        return true;
    }
    
    
    @Override
    public boolean performOk() {
    
        // TODO Auto-generated method stub
        boolean ret = super.performOk();
        
        Activator.getDefault().getJavaQsarManager().initializeDescriptorModel();
        
        return ret;
    }


    public void init( IWorkbench workbench ) {
    }

}
