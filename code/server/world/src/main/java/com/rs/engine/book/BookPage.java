package com.rs.engine.book;

public class BookPage {

	private final String[] left;
	private final String[] right;

	public BookPage(String[] left, String[] right) {
		if (left.length > 15 || right.length > 15)
			throw new RuntimeException("Cannot create book page with longer than 15 lines of text.");
		this.left = left;
		this.right = right;
	}

	public String getLeftLine(int line) {
		return line >= left.length ? "" : left[line];
	}

	public String getRightLine(int line) {
		return line >= right.length ? "" : right[line];
	}

}
