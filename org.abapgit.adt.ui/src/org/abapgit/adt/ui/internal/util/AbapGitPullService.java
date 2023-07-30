package org.abapgit.adt.ui.internal.util;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.abapgit.adt.backend.IRepositoryService;
import org.abapgit.adt.backend.RepositoryServiceFactory;
import org.abapgit.adt.backend.model.abapObjects.IAbapObject;
import org.abapgit.adt.backend.model.abapObjects.IAbapObjects;
import org.abapgit.adt.backend.model.abapgitrepositories.IRepository;
import org.abapgit.adt.backend.model.agitpullmodifiedobjects.IAbapGitObject;
import org.abapgit.adt.backend.model.agitpullmodifiedobjects.IAbapGitPullModifiedObjects;
import org.abapgit.adt.backend.model.agitpullmodifiedobjects.IAgitpullmodifiedobjectsFactory;
import org.abapgit.adt.backend.model.agitpullmodifiedobjects.IOverwriteObjects;
import org.abapgit.adt.backend.model.agitpullmodifiedobjects.IPackageWarningObjects;
import org.abapgit.adt.ui.internal.repositories.IRepositoryModifiedObjects;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.ui.IEditorReference;
import org.eclipse.ui.IFileEditorInput;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;

import com.sap.adt.tools.core.sfs.util.AdtSfsUtilFactory;

public class AbapGitPullService implements IAbapGitPullService {

	@Override
	public Map<String, IAbapGitPullModifiedObjects> getSelectedObjectsToPullforRepo(
			Set<IRepositoryModifiedObjects> overWriteObjectsSelectedToPull,
			Set<IRepositoryModifiedObjects> packageWarningObjectsSelectedToPull) {

		Map<String, IAbapGitPullModifiedObjects> repoToSelectedObjects = new HashMap<String, IAbapGitPullModifiedObjects>();

		//Loop over selected overwrite objects for all repositories and insert in repoToSelectedObjectsMap
		for (IRepositoryModifiedObjects obj : overWriteObjectsSelectedToPull) {
			IAbapGitPullModifiedObjects objectsToPull = IAgitpullmodifiedobjectsFactory.eINSTANCE.createAbapGitPullModifiedObjects();

			if (!obj.getModifiedObjects().isEmpty()) {
				IOverwriteObjects overwriteObjects = IAgitpullmodifiedobjectsFactory.eINSTANCE.createOverwriteObjects();
				overwriteObjects.getAbapgitobjects().addAll(obj.getModifiedObjects());
				objectsToPull.setOverwriteObjects(overwriteObjects);
			}

			repoToSelectedObjects.put(obj.getRepositoryURL(), objectsToPull);
		}

		//Loop over selected package warning objects for all repositories and insert in repoToSelectedObjectsMap
		for (IRepositoryModifiedObjects obj : packageWarningObjectsSelectedToPull) {
			IAbapGitPullModifiedObjects objectsToPull = IAgitpullmodifiedobjectsFactory.eINSTANCE.createAbapGitPullModifiedObjects();

			if (!obj.getModifiedObjects().isEmpty()) {
				IPackageWarningObjects packageWarningObjects = IAgitpullmodifiedobjectsFactory.eINSTANCE.createPackageWarningObjects();
				packageWarningObjects.getAbapgitobjects().addAll(obj.getModifiedObjects());
				objectsToPull.setPackageWarningObjects(packageWarningObjects);

				// if the repoToSelectedObjectsMap doesn't already have an entry for the repository from filling in overwrite objects, create an entry in the map
				// else fill in the package warning objects
				if (repoToSelectedObjects.get(obj.getRepositoryURL()) == null) {
					repoToSelectedObjects.put(obj.getRepositoryURL(), objectsToPull);
				} else {
					repoToSelectedObjects.get(obj.getRepositoryURL())
							.setPackageWarningObjects(packageWarningObjects);
				}

			}

		}

		return repoToSelectedObjects;

	}

	@Override
	public void refreshOpenEditorsAfterPull(String destination, IProgressMonitor monitor, IRepository repository, IAbapGitPullModifiedObjects objectsRequestedToPull ) {
		//Get objects log
		IRepositoryService repoService = RepositoryServiceFactory.createRepositoryService(destination, monitor);
		IAbapObjects abapObjects = repoService.getRepoObjLog(monitor, repository);

		//Get the successfully pulled objects
		List<String> succesfullyPulledObjects = new ArrayList<>();
		for (IAbapObject object : abapObjects.getAbapObjects()) {
			if (object.getStatus().equalsIgnoreCase("S") && objectSelectedToPull(object.getName(), objectsRequestedToPull)) { //$NON-NLS-1$
				succesfullyPulledObjects.add(object.getName().toLowerCase());
			}
		}

		//refresh editor for the the successfully pulled objects

		if (PlatformUI.getWorkbench() != null && PlatformUI.getWorkbench().getActiveWorkbenchWindow() != null
				&& PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage() != null
				&& PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage().getEditorReferences() != null) {

			IEditorReference[] editorReferences = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage()
					.getEditorReferences();

			for (IEditorReference editorReference : editorReferences) {
				try {

					if (editorReference.getEditorInput() != null) {
						IFile file = ((IFileEditorInput) editorReference.getEditorInput()).getFile();

						//TODO Match the type as well

						if (file != null && file.getName() != null && file.getProject() != null) {
							String[] objectName = file.getName().split("\\."); //$NON-NLS-1$

							if (file.getProject().getName().equalsIgnoreCase(destination) && objectName.length > 0
									&& succesfullyPulledObjects.contains(objectName[0].toLowerCase())) {
								AdtSfsUtilFactory.createAdtSfsUtil().synchronizeDevelopmentObject(file, false, monitor);
							}

						}

					}

				} catch (PartInitException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}

		}


	}

	private boolean objectSelectedToPull(String objectName, IAbapGitPullModifiedObjects objectsSelectedToBePulled) {

		IOverwriteObjects overWriteObjects = objectsSelectedToBePulled.getOverwriteObjects();
		IPackageWarningObjects packageWarningObjects = objectsSelectedToBePulled.getPackageWarningObjects();

		if (overWriteObjects != null) {
			for (IAbapGitObject object : overWriteObjects.getAbapgitobjects()) {
				if (object.getName().equalsIgnoreCase(objectName)) {
					return true;
				}
			}
		}

		if (packageWarningObjects != null) {
			for (IAbapGitObject object : packageWarningObjects.getAbapgitobjects()) {
				if (object.getName().equalsIgnoreCase(objectName)) {
					return true;
				}
			}
		}

		return false;
	}

}
