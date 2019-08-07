/*******************************************************************************
* Licensed under the Apache License, Version 2.0 (the "License");
* you may not use this file except in compliance with the License.
* You may obtain a copy of the License at
*
*      https://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing, software
* distributed under the License is distributed on an "AS IS" BASIS,
* WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
* See the License for the specific language governing permissions and
* limitations under the License.
*******************************************************************************/
package net.adoptopenjdk.casa.util.format;
import java.util.ArrayList;

/**
 * Formats data in a human-readable table. 
 * 
 *  
 */
public class Table extends AbstractRow
{	
	private final static char HORIZONTAL_LINE_CHARACTER = '-';
	private final static String COLUMN_SEPARATOR = "|"; 
	private final static String HORIZONTAL_PADDING = " "; 
	private final static String NEWLINE = "\n";

	// A row with column headings. Dictates the width of columns 
	private Headings headings; 
	
	// All rows besides the headings. 
	private final ArrayList<AbstractRow> rows; 
			
	/**
	 * Creates a new table with null headings and an empty list of rows. 
	 * 
	 * Must call addHeadings() and then addRow() to construct table.  
	 */
	public Table() 
	{						
		headings = null; 
		rows = new ArrayList<AbstractRow>(); 
	}
	
	/**
	 * Adds the given row as headings. 
	 * 
	 * @param headings
	 */
	public void addHeadings(Headings headings) throws RowException
	{	
		if (this.headings != null)
			throw new RowException("headings have already been added; may not add them again. ");
		
		this.headings = headings;  	
	}
	
	/**
	 * Adds the given row. Must call addHeadings() before calling addRow()
	 * 
	 * @param row
	 * @throws RowException
	 * @throws CellException 
	 */
	public void addRow(AbstractRow row) throws RowException, CellException 
	{
		if (headings == null)
			throw new RowException("headings have not been added");
				
		rows.add(row);		
	}
		
	/**
	 * Adds a new line. 
	 */
	public void addLine()
	{
		rows.add(new Line());
	}
	
	/**
	 * Appends the table to the given string builder. 
	 * 
	 * @param builder - A valid StringBuilder object 
	 */
	public void appendToStringBuilder(StringBuilder builder)
	{
		headings.appendToStringBuilder(builder); 
		
		for (AbstractRow row : rows) 
		{	
			builder.append(NEWLINE);
			row.appendToStringBuilder(builder); 			
		}
	}
		
	/**
	 * A horizontal line that can be added as a Row to the table. 
	 * 
	 *  
	 */
	public class Line extends AbstractRow 
	{							
		/**
		 * Appends the line to the given string builder. 
		 */
		public void appendToStringBuilder(StringBuilder builder)
		{
			for (int i = 0; i < headings.getCharWidth(); i++)
				builder.append(HORIZONTAL_LINE_CHARACTER); 
		}
	}
	
	/**
	 * Headings is a Row which is added to the table before any 
	 * other rows. Each cell has an associated width and 
	 * that width governs the width of the corresponding cells in 
	 * subsequent rows. 
	 * 
	 *  
	 */
	public class Headings extends Row
	{
		private final boolean lineBefore; 
		private final boolean lineAfter; 
		private final int[] widths; 
		
		/**
		 * Heading with per-cell alignments and optional lines before and after. 
		 * 
		 * @param cellContents
		 * @param widths
		 * @param alignments
		 * @param lineBefore
		 * @param lineAfter
		 * @throws RowException
		 * @throws CellException
		 */
		public Headings(String[] cellContents, int[] widths, Alignment[] alignments, boolean lineBefore, boolean lineAfter) throws RowException, CellException
		{
			super(widths, alignments);			
			this.widths = widths;
			this.lineBefore = lineBefore; 
			this.lineAfter = lineAfter;
							
			setCellContents(cellContents);
		}
		
		/**
		 * Create a heading row with a single common alignment, a line before and a line after. 
		 * 
		 * @param cellContents
		 * @param widths
		 * @param alignment
		 * @throws RowException
		 * @throws CellException
		 */
		public Headings(String[] cellContents, int[] widths, Alignment alignment) throws RowException, CellException
		{
			super(widths, new Alignment[] {alignment});			
			this.widths = widths;
			lineBefore = true; 
			lineAfter = true; 
										
			setCellContents(cellContents);
		}
		
		/**
		 * Gets the number of cells in this Row. 
		 * 
		 * @return
		 */
		private int getNumCells()
		{
			return widths.length; 
		}
		
		/**
		 * Gets the widths of the columns 
		 * 
		 * @return
		 */
		private int[] getWidths()
		{
			return widths; 
		}
	
		/**
		 * Gets the width of the row in number of chars.  
		 * 
		 * @return
		 */
		private int getCharWidth()
		{
			int length = COLUMN_SEPARATOR.length();
			
			for (int i = 0; i < getNumCells(); i++ ) 			
				length += HORIZONTAL_PADDING.length() + getWidths()[i] + HORIZONTAL_PADDING.length() + COLUMN_SEPARATOR.length(); 
		
			return length;  					
		}
		
		/**
		 * Appends the heading row to the given string builder. 
		 */
		public void appendToStringBuilder(StringBuilder builder)
		{
			Line line = new Line(); 
			
			if (lineBefore) 
			{
				line.appendToStringBuilder(builder);
				builder.append(NEWLINE);
			}

			super.appendToStringBuilder(builder);

			if (lineAfter) 
			{
				builder.append(NEWLINE);
				line.appendToStringBuilder(builder);				
			}
		}
	}
	
	/**
	 * Encapsulates and formats cells comprising a row. 
	 * 
	 *  
	 */
	public class Row extends AbstractRow 
	{
		private final Cell[] cells;
				
		/**
		 * Creates a row with a common alignment. 
		 * 
		 * @param cellContents
		 * @param alignment
		 * @throws RowException
		 * @throws CellException
		 */
		public Row(String[] cellContents, Alignment alignment) throws RowException, CellException
		{	
			this(new Alignment[] {alignment});
			
			if (cellContents != null)
			{
				if (cellContents.length != headings.getNumCells())
					throw new RowException("Number of cells does not match number of headings.");
			}
						
			setCellContents(cellContents);
		}
				
		/**
		 * Creates a row with individual alignments. 
		 * 
		 * @param cellContents
		 * @param alignments
		 * @throws RowException
		 * @throws CellException
		 */
		public Row(String[] cellContents, Alignment[] alignments) throws RowException, CellException
		{	
			this(alignments);
			
			if (cellContents != null)
			{
				if (cellContents.length != alignments.length)
					throw new RowException("Differing numbers of cell contents and alignemnts.");
				
				if (cellContents.length != headings.getNumCells())
					throw new RowException("Number of cells does not match number of headings.");
			}
						
			setCellContents(cellContents);			
		}
		
		/**
		 * Creates cells with the given widths and alignments for headings  
		 * 
		 * @param widths
		 * @param alignments
		 * 
		 * @throws RowException
		 * @throws CellException
		 */
		private Row(int[] widths, Alignment[] alignments) throws RowException, CellException
		{
			if (widths == null)
				throw new RowException("Null widths.");
			
			if (alignments == null)
				throw new RowException("Null alignments.");
						
			if (alignments.length != 1 && alignments.length != widths.length)
				throw new RowException("Invalid number of alignments.");
			
			cells = new Cell[widths.length];	
			
			for (int i = 0; i < widths.length; i++) 			
				cells[i] = new Cell(i, widths[i], (alignments.length == 1)?alignments[0]:alignments[i]);
		}
		
		/**
		 * Creates cells with the width of the header row. 
		 * 
		 * @param alignments
		 * 
		 * @throws RowException
		 * @throws CellException
		 */
		private Row(Alignment[] alignments) throws RowException, CellException
		{			
			if (headings == null)
				throw new RowException("Headings is null.");
			
			if (alignments == null)
				throw new RowException("Null alignment.");
			
			if (alignments.length != 1 && alignments.length != headings.getNumCells())
				throw new RowException("Invalid number of alignments.");

			// Create new cells.
			cells = new Cell[headings.getNumCells()];
			
			for (int i = 0; i < headings.getNumCells(); i++) 			
				cells[i] = new Cell(i, (alignments.length == 1)?alignments[0]:alignments[i]);			
		}
		
		/**
		 * Changes the contents of the cells.  
		 * 
		 * @param cellContents
		 * @throws RowException
		 * @throws CellException
		 */
		public void setCellContents(String[] cellContents) throws RowException, CellException
		{	
			if (cells == null)
				throw new RowException("cells is null, cannot set contents.");
			
			// Blank the cells if contents is null. 
			if (cellContents == null)
			{
				for (Cell cell : cells) 				
					cell.setContents(null);
			}
			else 
			{
				if (cellContents.length != cells.length)
					throw new RowException("Differing numbers of cell contents and cells in setCellContents().");
						
				for (int i = 0; i < cells.length; i++) 				
					cells[i].setContents(cellContents[i]);						
			}
		}
		
		/**
		 * Appends the row to the given string builder. 
		 * 
		 * @param builder
		 */
		public void appendToStringBuilder(StringBuilder builder)
		{
			for (Cell cell : cells) 			
				cell.appendToStringBuilder(builder);
		}
	}
	
	/**
	 * A cell in the table. 
	 * 
	 *  
	 *
	 */
	private class Cell extends AbstractRow
	{		
		private final int columnNumber;		
		private final Alignment alignment;
		private final int width; 
		private String cellContents;
		
		/**
		 * Constructs a new cell with the given alignment but no data.  
		 * 
		 * @param columnNumber
		 * @param alignment
		 * @throws CellException
		 */
		private Cell(int columnNumber, Alignment alignment) throws CellException
		{
			this(columnNumber, headings.getWidths()[columnNumber], alignment);			
		}
		
		/**
		 * Constructs a new cell with the given width, alignment and 
		 * data. Alignment may only be supplied for headings. 
		 * 
		 * @param columnNumber
		 * @param width
		 * @param alignment		
		 * @throws CellException
		 */
		private Cell(int columnNumber, int width, Alignment alignment) throws CellException
		{
			if (width < 1)
				throw new CellException("Invalid headings: Cells may not be less than 1 character wide.");
						
			this.columnNumber = columnNumber;		
			this.alignment = alignment; 
			cellContents = null;
			this.width = width; 
		}
			
		/**
		 * Replaces the cell's data with the given string. 
		 * 
		 * @param data
		 */
		private void setContents(String data) throws CellException
		{
			if (data == null)
				data = ""; 
			
			// If the data is too wide for the cell, shorten it. 			
			if (data.length() > width) 
			{
				if (width > 3)
					data = data.substring(0, width-3) + "...";
				else 
					data = data.substring(0, width);
			}
			
			cellContents = String.format("%" + (alignment.equals(Alignment.LEFT)?"-":"") + width + "s", data);
		}
		
		/**
		 * Appends this cell, formatted, to the given builder. 
		 * 
		 * @param builder
		 */
		public void appendToStringBuilder(StringBuilder builder)
		{
			if (columnNumber == 0)
				builder.append(COLUMN_SEPARATOR); 
							
			builder.append(HORIZONTAL_PADDING + cellContents + HORIZONTAL_PADDING + COLUMN_SEPARATOR);
		}
	}
}
