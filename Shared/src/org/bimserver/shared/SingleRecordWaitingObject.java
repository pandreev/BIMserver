package org.bimserver.shared;

import org.eclipse.emf.ecore.EStructuralFeature;

public class SingleRecordWaitingObject extends RecordWaitingObject{
	public SingleRecordWaitingObject(int lineNumber, Record record, EStructuralFeature structuralFeature) {
		super(lineNumber, record, structuralFeature);
	}
}
