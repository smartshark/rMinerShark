package de.ugoe.cs.smartshark.rMineSHARK.rMineSHARK.model;

import org.bson.types.ObjectId;
import org.mongodb.morphia.annotations.Id;
import org.mongodb.morphia.annotations.Property;

import de.ugoe.cs.smartshark.model.Hunk;

public class RefactoringHunk {

	@Property("hunk_id")
	private ObjectId hunkId;

	@Property("mode")
	private String mode;
	
	@Property("length")	
	private int length;

	@Property("start_line")	
	private int startLine;	

	@Property("start_column")	
	private int startColumn;
	
	@Property("end_line")	
	private int endLine;

	@Property("end_column")
	private int endColumn;	

	public ObjectId getHunkId() {
		return hunkId;
	}

	public void setHunkId(ObjectId hunkId) {
		this.hunkId = hunkId;
	}
	
	public void setMode(String mode) {
		this.mode = mode;
	}
	
	public String getModification(String mode) {
		return this.mode;
	}

	public int getLength() {
		return length;
	}

	public void setLength(int length) {
		this.length = length;
	}

	public int getStartLine() {
		return startLine;
	}

	public void setStartLine(int startLine) {
		this.startLine = startLine;
	}

	public int getStartColumn() {
		return startColumn;
	}

	public void setStartColumn(int startColumn) {
		this.startColumn = startColumn;
	}

	public int getEndLine() {
		return endLine;
	}

	public void setEndLine(int endLine) {
		this.endLine = endLine;
	}

	public int getEndColumn() {
		return endColumn;
	}

	public void setEndColumn(int endColumn) {
		this.endColumn = endColumn;
	}

}
