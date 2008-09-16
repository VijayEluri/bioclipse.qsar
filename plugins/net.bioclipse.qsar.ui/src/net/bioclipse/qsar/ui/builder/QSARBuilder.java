/*******************************************************************************
 *Copyright (c) 2008 The Bioclipse Team and others.
 *All rights reserved. This program and the accompanying materials
 *are made available under the terms of the Eclipse Public License v1.0
 *which accompanies this distribution, and is available at
 *http://www.eclipse.org/legal/epl-v10.html
 *
 *Contributors:
 *    Ola Spjuth - initial API and implementation
 *******************************************************************************/
package net.bioclipse.qsar.ui.builder;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;

import net.bioclipse.cdk.business.Activator;
import net.bioclipse.cdk.business.ICDKManager;
import net.bioclipse.cdk.domain.ICDKMolecule;
import net.bioclipse.core.business.BioclipseException;
import net.bioclipse.core.domain.IMolecule;
import net.bioclipse.qsar.DescriptorType;
import net.bioclipse.qsar.DescriptorimplType;
import net.bioclipse.qsar.DescriptorlistType;
import net.bioclipse.qsar.DocumentRoot;
import net.bioclipse.qsar.MoleculeResourceType;
import net.bioclipse.qsar.MoleculelistType;
import net.bioclipse.qsar.QSARConstants;
import net.bioclipse.qsar.QsarPackage;
import net.bioclipse.qsar.QsarType;
import net.bioclipse.qsar.ResponseType;
import net.bioclipse.qsar.business.IQsarManager;
import net.bioclipse.qsar.descriptor.IDescriptorResult;

import org.apache.log4j.Logger;
import org.eclipse.core.resources.ICommand;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IProjectDescription;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceDelta;
import org.eclipse.core.resources.IWorkspaceRunnable;
import org.eclipse.core.resources.IncrementalProjectBuilder;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.resource.ResourceSet;
import org.eclipse.emf.ecore.resource.impl.ResourceSetImpl;
import org.eclipse.emf.ecore.xmi.impl.XMIResourceFactoryImpl;
import org.openscience.cdk.exception.CDKException;
import org.openscience.cdk.io.CMLWriter;
import org.openscience.cdk.libio.cml.QSARCustomizer;
import org.openscience.cdk.qsar.DescriptorSpecification;
import org.openscience.cdk.qsar.DescriptorValue;
import org.openscience.cdk.qsar.result.DoubleArrayResult;

/**
 * A Builder for QSAR projects that synchronizes the project file 
 * qsar.xml with derived resources, e.g. matrix.csv.
 * 
 * @author ola
 *
 */
public class QSARBuilder extends IncrementalProjectBuilder
{
    
    private static final Logger logger = Logger.getLogger(QSARBuilder.class);
    
   public static final int MISSING_KEY_VIOLATION = 1;
   public static final int UNUSED_KEY_VIOLATION = 2;

   /**
    * The unique identifier for this builder
    */
    public static final String BUILDER_ID = "net.bioclipse.qsar.QSARBuilder";
   
   /**
    * The unique identifier for the audit marker
    */
    private static final String MARKER_ID = "net.bioclipse.qsar.qsarMarker";

   // auditMarker attributes
   public static final String KEY = "key";
   public static final String VIOLATION = "violation";
   
   /**
    * Simple data structure class containing a key and the location of
    * that key in a file
    */
   private class Location
   {
      IFile file;
      String key;
      int charStart;
      int charEnd;
   }
   
   /**
    * When called by Eclipse, this builder should perform an audit as
    * necessary. If the build kind is <code>INCREMENTAL_BUILD</code>
    * or <code>AUTO_BUILD</code>, the <code>getDelta</code>
    * method can be used during the invocation of this method to
    * obtain information about what changes have occurred since the
    * last invocation of this method. After completing a build, this
    * builder may return a list of projects for which it requires a
    * resource delta the next time it is run.
    * 
    * @param kind the kind of build being requested. Valid values are
    *           <ul>
    *           <li><code>FULL_BUILD</code>- indicates a full
    *           build.</li>
    *           <li><code>INCREMENTAL_BUILD</code>- indicates an
    *           incremental build. </li>
    *           <li><code>AUTO_BUILD</code>- indicates an
    *           automatically triggered incremental build
    *           (autobuilding on).</li>
    *           </ul>
    * @param args a table of builder-specific arguments keyed by
    *           argument name (key type: <code>String</code>, value
    *           type: <code>String</code>); <code>null</code> is
    *           equivalent to an empty map
    * @param monitor a progress monitor, or <code>null</code> if
    *           progress reporting and cancellation are not desired
    * @return the list of projects for which this builder would like
    *         deltas the next time it is run or <code>null</code> if
    *         none
    * @exception CoreException if this build fails.
    * @see IProject#build(int, String, Map, IProgressMonitor)
    */
   protected IProject[] build(
      int kind, 
      Map args,
      IProgressMonitor monitor
   ) throws CoreException
   {
      if (shouldBuild(kind)) {
         ResourcesPlugin.getWorkspace().run(
            new IWorkspaceRunnable() {
               public void run(IProgressMonitor monitor)
                  throws CoreException
               {
                  buildQSARfile(monitor);
               }
            }, 
            monitor
         );
      }
      return null;
   }
   
   /**
    * Clean is an opportunity for a builder to discard any additional state that has 
    * been computed as a result of previous builds. It is recommended that builders 
    * override this method to delete all derived resources created by previous builds, 
    * and to remove all markers of type <code>IMarker.PROBLEM</code> that 
    * were created by previous invocations of the builder. The platform will
    * take care of discarding the builder's last built state (there is no need
    * to call <code>forgetLastBuiltState</code>).
    * 
    * In our case, we already scrub the project as part of the FULL_BUILD
    * so no additional work needed.
    */
   protected void clean(IProgressMonitor monitor) throws CoreException {
      // no additional work needed here
   }

   /**
    * Determines whether files should be built by checking for
    * FULL_BUILD, or if the qsar.xml has changed.
    * 
    * @param kind the kind of build
    * @return <code>true</code> if file should be audited, else
    *         <code>false</code>.
    */
   private boolean shouldBuild(int kind) {
      if (kind == FULL_BUILD)
         return true;
      IResourceDelta delta = getDelta(getProject());
      if (delta == null)
         return false;
      IResourceDelta[] children = delta.getAffectedChildren();
      for (int i = 0; i < children.length; i++) {
         IResourceDelta child = children[i];
         String fileName = child.getProjectRelativePath().lastSegment();
         if (fileName.equals("qsar.xml"))
            return true;
      }
      return false;
   }
   
   /**
    * Scan the plugin.xml and plugin.properties files and correlate
    * the key/value pairs; any keys appearing in plugin.xml should
    * have a corresponding key/value pair in plugin.properties. Before
    * each lengthy operation, we check to see if the build has been
    * interrupted or canceled. After each lengthy operation, we report
    * progress to the user; while this is not strictly necessary, it
    * is certainly polite. If you do prematurely exit your build
    * process, you may need to call forgetLastBuildState() before
    * exiting so that a full rebuild will be performed the next time.
    * 
    * @param monitor the progress monitor
    */
   private void buildQSARfile(IProgressMonitor monitor) {
      monitor.beginTask("Building QSAR project", 4);

      if (!deleteAuditMarkers(getProject()))
         return; 
      
      if (checkCancel(monitor))
         return;
      
      //Scan plugin for molecules and descriptors to build
      logger.debug("Building descriptors...");
      scanQsarFile();
      
      if (checkCancel(monitor))
         return;
      
      logger.debug("Building descriptors completed.");
      
      monitor.done();
   }

private void scanQsarFile() {

	   //We need to read in qsar.xml and pasre it with EMF
	   //=================================================

	   // Create a resource set.
	   ResourceSet resourceSet = new ResourceSetImpl();

	   // Register the default resource factory -- only needed for stand-alone!
	   resourceSet.getResourceFactoryRegistry().getExtensionToFactoryMap().put(
			   Resource.Factory.Registry.DEFAULT_EXTENSION, new XMIResourceFactoryImpl());

	   // Register the package -- only needed for stand-alone!
	   @SuppressWarnings("unused")
	   QsarPackage qsarPackage=QsarPackage.eINSTANCE;

	   IFile qsarfile = getProject().getFile("qsar.xml");
	  logger.debug("QSAR file: " + qsarfile.getRawLocation().toOSString());

	   // Get the URI of the model file.
	   URI fileURI = URI.createFileURI(qsarfile.getRawLocation().toOSString());

	   // Demand load the resource for this file.
	   Resource resource = resourceSet.getResource(fileURI, true);

	   DocumentRoot root=(DocumentRoot) resource.getContents().get(0);

	   QsarType qsarType=root.getQsar();
	   

	   
	   //Extract molecules from qsar model
	   //=================================
	   //Verify and load mols
	   if (qsarType.getMoleculelist()==null){
		   logger.debug("No moleculesList.");
		   return;
	   }
	   
	   MoleculelistType mollist = qsarType.getMoleculelist();
	   
	   if (mollist.getMoleculeResource()==null || mollist.getMoleculeResource().size()<=0){
		   logger.debug("No molecules in MoleculesList.");
		   return;
	   }
	   
	   ICDKManager cdk=Activator.getDefault().getCDKManager();
	   
	   //Get list of IMolecules from MoleculeResources
	   Map<ICDKMolecule, String> molMap=new HashMap<ICDKMolecule, String>();

	   //Get list of responses from qsar.xml matching resources
//	   List<String> responseStrings=new ArrayList<String>();

	   //For all IMolecules, add to list with responses as properties in AC
	   for (MoleculeResourceType mol : mollist.getMoleculeResource()){
		   logger.debug(" ++ Mol: " + mol.getId() + " [" + mol.getFile()+"]");
		   try {
			List<ICDKMolecule> newMols=cdk.loadMolecules(mol.getFile());
			logger.debug("    - contained " + newMols.size() + " imols");
			
			//We need to add responses to the molecules from the qsarType
			//Add response values as property
			for (ResponseType response : qsarType.getResponselist().getResponse()){
				if (response.getMoleculeResource().equals(mol.getId())){
					for (int i=0;i<newMols.size();i++){
						if (response.getResourceIndex()==i){

							//If we have array value, us it
							if (response.getArrayValues()!=null){
								newMols.get(i).getAtomContainer().setProperty(
										QSARConstants.QSAR_RESPONSE_PROPERTY, 
										response.getArrayValues());
								molMap.put(newMols.get(i), response.getArrayValues());
//								responseStrings.add(response.getArrayValues());
								logger.debug("Added value: " + 
										response.getArrayValues() + 
										"to mol: " + mol + " ix " + i);
							}else{

								//If we have no array value, us float value
								newMols.get(i).getAtomContainer().setProperty(
										QSARConstants.QSAR_RESPONSE_PROPERTY, 
										response.getValue());
								logger.debug("Added value: " + 
										response.getValue() + 
										" to mol: " + mol.getId() + " ix " + i);
								molMap.put(newMols.get(i), ""+response.getValue());
//								responseStrings.add(""+response.getValue());
							}
						}
						if (!(molMap.containsKey(newMols.get(i)))){
							molMap.put(newMols.get(i), ""+Float.NaN);
						}
					}
				}

			}

//			mols.addAll(newMols);
		   } catch (IOException e) {
			   e.printStackTrace();
		   } catch (BioclipseException e) {
			   e.printStackTrace();
		   } catch (CoreException e) {
			   e.printStackTrace();
		   }
	   }
	   
	   logger.debug("==== Total loaded mols: " + molMap.size());
	   
	   //Get lists from molsMap
	   List<ICDKMolecule> sortedMols=new ArrayList<ICDKMolecule>();
	   sortedMols.addAll((Collection<? extends ICDKMolecule>) molMap.keySet());
	   
	   //Sort the mol based on name
	   Collections.sort(sortedMols,new Comparator<ICDKMolecule>(){
		   public int compare(ICDKMolecule o1, ICDKMolecule o2) {
			   return o1.getName().compareTo(o2.getName());
		   }
	   });
	   
	   //Set up straucture for dataset
	   List<String> columnLabels=new ArrayList<String>();
	   List<String> rowLabels=new ArrayList<String>();
	   double[][] dataset;
	   
	   //Get Descriptors from qsar model
	   //===============================
	   if (qsarType.getDescriptorlist()==null){
		   logger.debug("No descriptorList.");
		   return;
	   }
	   
	   DescriptorlistType desclist = qsarType.getDescriptorlist();
	   
	   if (desclist.getDescriptor()==null || desclist.getDescriptor().size()<=0){
		   logger.debug("No descriptors in descriptorList.");
		   return;
	   }

	   List<String> descriptorIDs=new ArrayList<String>();
	   for (DescriptorType desc : desclist.getDescriptor()){
		   descriptorIDs.add(desc.getId());
		   DescriptorimplType impl = desc.getDescriptorimpl();
		   
		   logger.debug(" ** desc: " + desc.getId() + " [" + impl.getId() + "]");
	   }
	   

	   //Calculate descriptor for mols
	   //=============================
	   IQsarManager qsar = net.bioclipse.qsar.init.Activator.getDefault().getQsarManager();
//	   Map<? extends IMolecule, List<IDescriptorResult>> resultMap = qsar.calculateNoParams(sortedMols, descriptorIDs);
	   Map<? extends IMolecule, List<IDescriptorResult>> resultMap = qsar.calculate(sortedMols, desclist.getDescriptor());


	   
	   
	   //Calculate dimension for dataset
	   //================================
	   int molSize=resultMap.keySet().size();
	   int descSize=0;
	   List<IDescriptorResult> molres2=(List<IDescriptorResult>) resultMap.values().toArray()[0];
	   for (IDescriptorResult res : molres2){
		   descSize=descSize+res.getLabels().length;
	   }
	   
	   logger.debug("************* We have " + molSize + " mols and " + descSize +" descriptor results");

	   //Make room in dataset
	   dataset=new double[molSize][descSize];

	   //Store descriptor results
	   //================================
	   int cnt=0;
	   for (IMolecule mol : sortedMols){
		   double[] datasetRow=dataset[cnt];
		   cnt++;
		   org.openscience.cdk.interfaces.IMolecule cdkcasted=null;
		   ICDKMolecule cdkmol=null;
		   try {
			   cdkmol=cdk.create(mol);
			   if (cdkmol.getName()==null){
				   cdkmol.setName("molecule"+cnt);
			   }
			   logger.debug("Molecule: " + cdkmol.getName());
			   rowLabels.add(cdkmol.getName());
			   cdkcasted=(org.openscience.cdk.interfaces.IMolecule) cdkmol.getAtomContainer();
		   } catch (BioclipseException e) {
			   e.printStackTrace();
		   }

		   int datasetrowindex=0;
		   List<IDescriptorResult> molres=resultMap.get(mol);
		   for (IDescriptorResult dres : molres){
			   logger.debug("  Descriptor: " + dres.getDescriptorId());

			   //Hold cdk results here for persistence
			   DoubleArrayResult result=new DoubleArrayResult(dres.getLabels().length);
			   for (int i=0; i<dres.getLabels().length;i++){
				   logger.debug("    Result " + i + ": " + dres.getLabels()[i] + 
						   " - " + dres.getValues()[i]);

				   result.add(dres.getValues()[i]);
				   datasetRow[datasetrowindex]=dres.getValues()[i];
				   
				   //Assume same for all molecules as we calculate the same descriptors!
				   if (cnt==1)
					   columnLabels.add(dres.getLabels()[i]);

				   //Increase index in datasetrow
				   datasetrowindex++;

			   }
			   
			   DescriptorSpecification spec=new DescriptorSpecification(dres.getDescriptorId(),"impl", "impl id","cdk");
			   
			   //Create the DescriptorValue that's used for serialization
			   DescriptorValue value=new DescriptorValue(spec,null,null,result,dres.getLabels());
			   
			   cdkcasted.setProperty(spec, value);

		   }
		   

		   //Serialize to CML files
		   //======================
		   try {

			   //First serialize to string
			   ByteArrayOutputStream bos=new ByteArrayOutputStream();
			   
//				StringWriter writer = new StringWriter();
				CMLWriter cmlWriter = new CMLWriter(bos);
				cmlWriter.registerCustomizer(new QSARCustomizer());
				cmlWriter.write(cdkcasted);

				ByteArrayInputStream bis=new ByteArrayInputStream(bos.toByteArray());
			   
			   //Path to results
			   IFolder processedFolder= getProject().getFolder("processedMols");
			   if (!(processedFolder.exists())){
				   processedFolder.create(true, true, new NullProgressMonitor());
				   processedFolder.setDerived(true);
			   }
			   IFile molfile=processedFolder.getFile(cdkmol.getName()+"_"+cnt+".cml");
			   logger.debug("Attempting to write file with descr res: " + molfile);
			   
			   if (!(molfile.exists())){
				   //Create file and add contents
				   molfile.create(bis, true, new NullProgressMonitor());
				   molfile.setDerived(true);
			   }
			   else{
				   //Overwrite contents
				   molfile.setContents(bis, true, true, new NullProgressMonitor());
				   molfile.setDerived(true);
			   }
				
			} catch (CDKException e) {
				e.printStackTrace();
			} catch (CoreException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

	   }

	   
	   //Serialize dataset to file
	   //=========================
	   logger.debug("============================================");
	   logger.debug("============================================");
	   String SEPARATOR=",";
	   String NEWLINE="\n";
	   DecimalFormat formatter = new DecimalFormat("0.00");
	    
	   StringBuffer buffer=new StringBuffer();	   

	   //We start with response values, hence RESPONSE label is first
	   buffer.append(SEPARATOR + "RESPONSE");

	   //Serialize one row at a time
	   //Start with colheaders
	   for (String colheader : columnLabels){
		   buffer.append(SEPARATOR + colheader);
	   }
	   buffer.append(NEWLINE);
	   for (int i=0;i< dataset.length; i++){
		   
		   //Add label first in line
		   buffer.append(rowLabels.get(i));
		   
		   //Then add response string from map containing it
		   if (molMap.get(sortedMols.get(i))!=null){
			   buffer.append(SEPARATOR + molMap.get(sortedMols.get(i)));
		   }else{
			   //Backup: NaN
			   buffer.append(SEPARATOR + Float.NaN);
		   }
//		   if (responseStrings.size()>i){
//			   buffer.append(SEPARATOR + responseStrings.get(i));
//		   }else{
//			   buffer.append(SEPARATOR + Float.NaN);
//		   }
			   
		   
		   //Add descriptor values
		   for (int j=0;j< dataset[i].length; j++){
			   buffer.append(SEPARATOR + formatter.format(dataset[i][j]));
		   }
		   buffer.append(NEWLINE);
	   }
	   
	   logger.debug(buffer.toString());

	   logger.debug("============================================");
	   logger.debug("============================================");

	   //Write this String to file
	   InputStream is=new ByteArrayInputStream(buffer.toString().getBytes());
	   BufferedInputStream source=new BufferedInputStream(is);
	   
	   try {
		   IFile datasetFile=getProject().getFile("dataset.csv");
		   if (datasetFile.exists()){
			   datasetFile.setContents(source, true,true, new NullProgressMonitor());
		   }else{
			   datasetFile.create(source, true, new NullProgressMonitor());
		   }
		   datasetFile.setDerived(true);
		   logger.debug("Wrote file dataset.csv");
	   } catch (CoreException e) {
		   logger.error("Writing of file dataset.csv FAILED");
		   e.printStackTrace();
	   }
	   logger.debug("============================================");

}

/**
    * Check to see if the build operation in progress was canceled by
    * the user or should be canceled because another builder needs
    * access to the workspace.
    * 
    * @param monitor the progress monitor
    * @return <code>true</code> if the build operation should stop
    */
   private boolean checkCancel(IProgressMonitor monitor) {
      if (monitor.isCanceled()) {
         // Discard build state if necessary.
         throw new OperationCanceledException();
      }

      if (isInterrupted()) {
         // Discard build state if necessary.
         return true;
      }
      return false;
   }
   
 
   

   

   ////////////////////////////////////////////////////////////////////////////
   //
   // Marker methods
   //
   ////////////////////////////////////////////////////////////////////////////
   
   /**
    * Delete all audit markers in the specified project
    * 
    * @param project the project to be modified
    * @return <code>true</code> if successful, else
    *         <code>false</code>
    */
   public static boolean deleteAuditMarkers(IProject project) {
      try {
         project.deleteMarkers(
            MARKER_ID, false, IResource.DEPTH_INFINITE);
         return true;
      }
      catch (CoreException e) {
          logger.error(e);
         return false;
      }
   }
   
   ////////////////////////////////////////////////////////////////////////////
   //
   // Utility methods
   //
   ////////////////////////////////////////////////////////////////////////////

   /**
    * Add this builder to the specified project if possible.
    * Do nothing if the builder has already been added.
    *  
    * @param project the project (not <code>null</code>)
    */
   @SuppressWarnings("unchecked")
public static void addBuilderToProject(IProject project) {
      
      // Cannot modify closed projects.
      if (!project.isOpen())
         return;

      // Get the description.
      IProjectDescription description;
      try {
         description = project.getDescription();
      }
      catch (CoreException e) {
          logger.error(e);
         return;
      }
      
      // Look for builder already associated.
      ICommand[] cmds = description.getBuildSpec();
      for (int j = 0; j < cmds.length; j++)
         if (cmds[j].getBuilderName().equals(BUILDER_ID))
            return;

      // Associate builder with project.
      ICommand newCmd = description.newCommand();
      newCmd.setBuilderName(BUILDER_ID);
      List newCmds = new ArrayList();
      newCmds.addAll(Arrays.asList(cmds));
      newCmds.add(newCmd);
      description.setBuildSpec(
         (ICommand[]) newCmds.toArray(
            new ICommand[newCmds.size()]));
      try {
         project.setDescription(description, null);
      }
      catch (CoreException e) {
          logger.error(e);
      }
   }
   
   /**
    * Determine if the specified project has the receiver associated
    * with it.
    * 
    * @param project the project to test
    * @return <code>true</code> if the specified project is open and
    *         accessible and has the builder associated with it, else
    *         <code>false</code>
    */
   public static boolean hasBuilder(IProject project) {
      
      // Cannot modify closed projects.
      if (!project.isOpen())
         return false;

      // Get the description.
      IProjectDescription description;
      try {
         description = project.getDescription();
      }
      catch (CoreException e) {
          logger.error(e);
         return false;
      }
      
      // Look for builder already associated.
      ICommand[] cmds = description.getBuildSpec();
      for (int j = 0; j < cmds.length; j++)
         if (cmds[j].getBuilderName().equals(BUILDER_ID))
            return true;
      return false;
   }
   
   /**
    * Remove this builder from the specified project if possible.
    * Do nothing if the builder has already been removed.
    *  
    * @param project the project (not <code>null</code>)
    */
   @SuppressWarnings("unchecked")
public static void removeBuilderFromProject(IProject project) {

      // Cannot modify closed projects.
      if (!project.isOpen())
         return;
      
      // Get the description.
      IProjectDescription description;
      try {
         description = project.getDescription();
      }
      catch (CoreException e) {
          logger.error(e);
         return;
      }
      
      // Look for builder.
      int index = -1;
      ICommand[] cmds = description.getBuildSpec();
      for (int j = 0; j < cmds.length; j++) {
         if (cmds[j].getBuilderName().equals(BUILDER_ID)) {
            index = j;
            break;
         }
      }
      if (index == -1)
         return;
         
      // Remove builder from project.
      List newCmds = new ArrayList();
      newCmds.addAll(Arrays.asList(cmds));
      newCmds.remove(index);
      description.setBuildSpec(
         (ICommand[]) newCmds.toArray(
            new ICommand[newCmds.size()]));
      try {
         project.setDescription(description, null);
      }
      catch (CoreException e) {
          logger.error(e);
      }
   }
}
