/**
 * Copyright (c) 2007-2008 The Bioclipse Project and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * www.eclipse.org���epl-v10.html <http://www.eclipse.org/legal/epl-v10.html>
 * 
 * Contributors:
 *     Ola Spjuth - initial API and implementation
 * 
 *
 * $Id$
 */
package net.bioclipse.qsar;

import org.eclipse.emf.common.util.EList;

import org.eclipse.emf.ecore.EObject;

/**
 * <!-- begin-user-doc -->
 * A representation of the model object '<em><b>Moleculelist Type</b></em>'.
 * <!-- end-user-doc -->
 *
 * <p>
 * The following features are supported:
 * <ul>
 *   <li>{@link net.bioclipse.qsar.MoleculelistType#getMoleculeResource <em>Molecule Resource</em>}</li>
 * </ul>
 * </p>
 *
 * @see net.bioclipse.qsar.QsarPackage#getMoleculelistType()
 * @model extendedMetaData="name='moleculelistType' kind='elementOnly'"
 * @generated
 */
public interface MoleculelistType extends EObject {
	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	String copyright = "Copyright (c) 2007-2008 The Bioclipse Project and others.\nAll rights reserved. This program and the accompanying materials\nare made available under the terms of the Eclipse Public License v1.0\nwhich accompanies this distribution, and is available at\nwww.eclipse.org\u00ef\u00bf\u03a9epl-v10.html <http://www.eclipse.org/legal/epl-v10.html>\n\nContributors:\n    Ola Spjuth - initial API and implementation\n";

	/**
	 * Returns the value of the '<em><b>Molecule Resource</b></em>' containment reference list.
	 * The list contents are of type {@link net.bioclipse.qsar.MoleculeResourceType}.
	 * <!-- begin-user-doc -->
	 * <p>
	 * If the meaning of the '<em>Molecule Resource</em>' containment reference list isn't clear,
	 * there really should be more of a description here...
	 * </p>
	 * <!-- end-user-doc -->
	 * @return the value of the '<em>Molecule Resource</em>' containment reference list.
	 * @see net.bioclipse.qsar.QsarPackage#getMoleculelistType_MoleculeResource()
	 * @model containment="true"
	 *        extendedMetaData="kind='element' name='moleculeResource' namespace='##targetNamespace'"
	 * @generated
	 */
	EList<MoleculeResourceType> getMoleculeResource();

} // MoleculelistType
