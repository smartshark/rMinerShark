package de.ugoe.cs.smartshark.rMineSHARK.rMineSHARK.internal;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import gr.uom.java.xmi.decomposition.AbstractCodeMapping;
import gr.uom.java.xmi.decomposition.VariableDeclaration;
import gr.uom.java.xmi.decomposition.replacement.Replacement;
import gr.uom.java.xmi.diff.*;
import org.refactoringminer.api.Refactoring;

import de.ugoe.cs.smartshark.rMineSHARK.util.Logger;
import gr.uom.java.xmi.LocationInfo;
import gr.uom.java.xmi.UMLAttribute;
import gr.uom.java.xmi.UMLOperation;

public class RefactoringTypeMatcher {
	
	public static List<List<CodeRange>> getLocationInfoRefactoringSpecific(Refactoring ref) {
		List<LocationInfo> info = new ArrayList<>();
		List<CodeRange> rangesBefore = new LinkedList<>();
		List<CodeRange> rangesAfter = new LinkedList<>();
		if (ref instanceof RenameAttributeRefactoring) {
			RenameAttributeRefactoring refImpl = (RenameAttributeRefactoring) ref;
			rangesBefore.add(refImpl.getOriginalAttribute().codeRange());
			rangesAfter.add(refImpl.getRenamedAttribute().codeRange());
		} else if (ref instanceof MoveOperationRefactoring) {
			MoveOperationRefactoring refImpl = (MoveOperationRefactoring) ref;
			rangesBefore.add(refImpl.getSourceOperationCodeRangeBeforeMove());
			rangesAfter.add(refImpl.getTargetOperationCodeRangeAfterMove());
		} else if (ref instanceof MoveAndRenameClassRefactoring) {
			MoveAndRenameClassRefactoring refImpl = (MoveAndRenameClassRefactoring) ref;
			rangesBefore.add(refImpl.getOriginalClass().codeRange());
			rangesAfter.add(refImpl.getRenamedClass().codeRange());
		} else if (ref instanceof RenameClassRefactoring) {
			RenameClassRefactoring refImpl = (RenameClassRefactoring) ref;
			rangesBefore.add(refImpl.getOriginalClass().codeRange());
			rangesAfter.add(refImpl.getRenamedClass().codeRange());
		} else if (ref instanceof ExtractOperationRefactoring) {
			ExtractOperationRefactoring refImpl = (ExtractOperationRefactoring) ref;
			rangesBefore.add(refImpl.getExtractedCodeRangeFromSourceOperation());
			rangesAfter.add(refImpl.getExtractedOperationCodeRange());
			for( CodeRange range : refImpl.getExtractedOperationInvocationCodeRanges() ) {
				rangesAfter.add(range);
			}
		} else if (ref instanceof MoveAttributeRefactoring) {
			MoveAttributeRefactoring refImpl = (MoveAttributeRefactoring) ref;
			rangesBefore.add(refImpl.getSourceAttributeCodeRangeBeforeMove());
			rangesAfter.add(refImpl.getTargetAttributeCodeRangeAfterMove());
		} else if (ref instanceof ExtractClassRefactoring) {
			ExtractClassRefactoring refImpl = (ExtractClassRefactoring) ref;
			for( UMLOperation operation : refImpl.getExtractedOperations()) {
				rangesBefore.add(operation.codeRange());
			}
			for( UMLAttribute attributes : refImpl.getExtractedAttributes()) {
				rangesBefore.add(attributes.codeRange());
			}
			rangesAfter.add(refImpl.getExtractedClass().codeRange());
		} else if (ref instanceof RenameOperationRefactoring) {
			RenameOperationRefactoring refImpl = (RenameOperationRefactoring) ref;
			rangesBefore.add(refImpl.getSourceOperationCodeRangeBeforeRename());
			rangesAfter.add(refImpl.getTargetOperationCodeRangeAfterRename());
			// TODO missing CodeRanges for Replacements, but this is currently very unreliable and, therefore, ignored
		} else if (ref instanceof RenameVariableRefactoring) {
			RenameVariableRefactoring refImpl = (RenameVariableRefactoring) ref;
			rangesBefore.add(refImpl.getOriginalVariable().codeRange());
			rangesAfter.add(refImpl.getRenamedVariable().codeRange());
			for( AbstractCodeMapping reference : refImpl.getVariableReferences()) {
				rangesBefore.add(reference.getFragment1().codeRange());
				rangesAfter.add(reference.getFragment1().codeRange());
			}
		} else if (ref instanceof MoveClassRefactoring) {
			MoveClassRefactoring refImpl = (MoveClassRefactoring) ref;
			rangesBefore.add(refImpl.getOriginalClass().codeRange());
			rangesAfter.add(refImpl.getMovedClass().codeRange());
		} else if (ref instanceof InlineOperationRefactoring) {
			InlineOperationRefactoring refImpl = (InlineOperationRefactoring) ref;
			rangesBefore.add(refImpl.getInlinedOperationCodeRange());
			for( CodeRange ranges : refImpl.getInlinedOperationInvocationCodeRanges() ) {
				rangesBefore.add(ranges);
			}
			rangesAfter.add(refImpl.getInlinedCodeRangeInTargetOperation());
		} else if (ref instanceof InlineVariableRefactoring) {
			InlineVariableRefactoring refImpl = (InlineVariableRefactoring) ref;
			rangesBefore.add(refImpl.getInlinedVariableDeclarationCodeRange());
			for (AbstractCodeMapping references : refImpl.getReferences()) {
				rangesBefore.add(references.getFragment1().codeRange());
				rangesAfter.add(references.getFragment2().codeRange());
			}
		} else if (ref instanceof MergeVariableRefactoring) {
			MergeVariableRefactoring refImpl = (MergeVariableRefactoring) ref;
			for( VariableDeclaration mergedVariable : refImpl.getMergedVariables() ) {
				rangesBefore.add(mergedVariable.codeRange());
			}
			rangesAfter.add(refImpl.getNewVariable().codeRange());
			for( AbstractCodeMapping reference : refImpl.getVariableReferences()) {
				rangesBefore.add(reference.getFragment1().codeRange());
				rangesAfter.add(reference.getFragment2().codeRange());
			}
		} else if (ref instanceof ConvertAnonymousClassToTypeRefactoring) {
			ConvertAnonymousClassToTypeRefactoring refImpl = (ConvertAnonymousClassToTypeRefactoring) ref;
			rangesBefore.add(refImpl.getAnonymousClass().codeRange());
			rangesAfter.add(refImpl.getAddedClass().codeRange());
		} else if (ref instanceof RenamePackageRefactoring) {
			RenamePackageRefactoring refImpl = (RenamePackageRefactoring) ref;
			for (MoveClassRefactoring classRefactoring:refImpl.getMoveClassRefactorings()) {
				info.add(classRefactoring.getMovedClass().getLocationInfo());
			}
		} else if (ref instanceof ChangeAttributeTypeRefactoring) {
			ChangeAttributeTypeRefactoring refImpl = (ChangeAttributeTypeRefactoring) ref;
			rangesBefore.add(refImpl.getOriginalAttribute().codeRange());
			rangesAfter.add(refImpl.getChangedTypeAttribute().codeRange());
		} else if (ref instanceof ChangeVariableTypeRefactoring) {
			ChangeVariableTypeRefactoring refImpl = (ChangeVariableTypeRefactoring) ref;
			for( AbstractCodeMapping reference : refImpl.getVariableReferences()) {
				rangesBefore.add(reference.getFragment1().codeRange());
				rangesAfter.add(reference.getFragment2().codeRange());
			}
		} else if (ref instanceof SplitVariableRefactoring) {
			SplitVariableRefactoring refImpl = (SplitVariableRefactoring) ref;
			rangesBefore.add(refImpl.getOldVariable().codeRange());
			for( VariableDeclaration splitVariables : refImpl.getSplitVariables()) {
				rangesAfter.add(splitVariables.codeRange());
			}
			for( AbstractCodeMapping references : refImpl.getVariableReferences()) {
				rangesBefore.add(references.getFragment1().codeRange());
				rangesAfter.add(references.getFragment2().codeRange());
			}
			info.add(refImpl.getOperationAfter().getLocationInfo());
		} else if (ref instanceof ExtractVariableRefactoring) {
			ExtractVariableRefactoring refImpl = (ExtractVariableRefactoring) ref;
			rangesAfter.add(refImpl.getExtractedVariableDeclarationCodeRange());
			for (AbstractCodeMapping reference : refImpl.getReferences()) {
				rangesBefore.add(reference.getFragment1().codeRange());
				rangesAfter.add(reference.getFragment2().codeRange());
			}
		} else if (ref instanceof MergeAttributeRefactoring) {
			MergeAttributeRefactoring refImpl = (MergeAttributeRefactoring) ref;
			for( VariableDeclaration attribute : refImpl.getMergedAttributes() ) {
				rangesBefore.add(attribute.codeRange());
			}
			rangesAfter.add(refImpl.getNewAttribute().codeRange());
			for( CandidateMergeVariableRefactoring merges : refImpl.getAttributeMerges() ) {
				for( AbstractCodeMapping reference : merges.getVariableReferences()) {
					rangesBefore.add(reference.getFragment1().codeRange());
					rangesAfter.add(reference.getFragment2().codeRange());
				}
			}
		} else if (ref instanceof MoveSourceFolderRefactoring) {
			MoveSourceFolderRefactoring refImpl = (MoveSourceFolderRefactoring) ref;
			for (MovedClassToAnotherSourceFolder classRefactoring:refImpl.getMovedClassesToAnotherSourceFolder()) {
				info.add(classRefactoring.getMovedClass().getLocationInfo());
			}
		} else if (ref instanceof ExtractAttributeRefactoring) {
			ExtractAttributeRefactoring refImpl = (ExtractAttributeRefactoring) ref;
			info.add(refImpl.getVariableDeclaration().getLocationInfo());
		} else if (ref instanceof SplitAttributeRefactoring) {
			SplitAttributeRefactoring refImpl = (SplitAttributeRefactoring) ref;
			rangesBefore.add(refImpl.getOldAttribute().codeRange());
			for( VariableDeclaration variable : refImpl.getSplitAttributes() ) {
				rangesAfter.add(variable.codeRange());
			}
			// TODO the detected refactoring I had in the example i considered was completely wrong ...
		} else if (ref instanceof ChangeReturnTypeRefactoring) {
			ChangeReturnTypeRefactoring refImpl = (ChangeReturnTypeRefactoring) ref;
			rangesBefore.add(refImpl.getOriginalType().codeRange());
			rangesAfter.add(refImpl.getChangedType().codeRange());
			for (AbstractCodeMapping references : refImpl.getReturnReferences()) {
				rangesBefore.add(references.getFragment1().codeRange());
				rangesAfter.add(references.getFragment2().codeRange());
			}
		}  else if (ref instanceof ExtractSuperclassRefactoring) {
			ExtractSuperclassRefactoring refImpl = (ExtractSuperclassRefactoring) ref;
			rangesAfter.add(refImpl.getExtractedClass().codeRange());
			// TODO do not add extracted code, i.f., state before, because this is missing and references the complete classes.
		} else {
			Logger.log("WARN not implemented refacorting " + ref.getRefactoringType());
		}
		List<List<CodeRange>> result = new LinkedList<>();
		result.add(rangesBefore);
		result.add(rangesAfter);
		return result;
	}
}
