// Board.java
package tetris;

import java.util.Arrays;

import javax.swing.text.StyledEditorKit.ForegroundAction;

/**
 CS108 Tetris Board.
 Represents a Tetris board -- essentially a 2-d grid
 of booleans. Supports tetris pieces and row clearing.
 Has an "undo" feature that allows clients to add and remove pieces efficiently.
 Does not do any drawing or have any idea of pixels. Instead,
 just represents the abstract 2-d board.
*/
public class Board	{
	// Some ivars are stubbed out for you:
	private int width;
	private int height;
	private boolean[][] grid;
	private boolean DEBUG = true;
	boolean committed;
	
	private int[] widths;
	private int[] heights;
	private int[] oldW;
	private int[] oldH;
	private boolean[][] oldG;
	
	/**
	 Creates an empty board of the given width and height
	 measured in blocks.
	*/
	public Board(int width, int height) {
		this.width = width;
		this.height = height;
		grid = new boolean[width][height];
		oldG = new boolean[width][height];
		committed = true;
		
		widths = new int[height];
		heights = new int[width];
		oldW = new int[height];
		oldH = new int[height];
		for (int i = 0; i < width; i++) {
			heights[i]= 0;
		}
		for (int i = 0; i < height; i++) {
			widths[i] = 0;
		}
	}
	
	
	/**
	 Returns the width of the board in blocks.
	*/
	public int getWidth() {
		return width;
	}
	
	
	/**
	 Returns the height of the board in blocks.
	*/
	public int getHeight() {
		return height;
	}
	
	
	/**
	 Returns the max column height present in the board.
	 For an empty board this is 0.
	*/
	public int getMaxHeight() {	
		/*int max = 0;
		for (int i = 0; i < width; i++) {
			if(heights[i] > max) {
				max = heights[i];
			}
		}
		return max;
		*/
		int maxHeight = 0;
		for (int i = 0 ; i<heights.length;i++)
		{
			if(maxHeight<heights[i])
				maxHeight = heights[i];
		}
		return maxHeight;

	}
	
	
	/**
	 Checks the board for internal consistency -- used
	 for debugging.
	*/
	public void sanityCheck() {
		if (DEBUG) {
			int [] widthlen = new int[height];
			int maxH = 0;
			for (int i = 0; i < width; i++) {
				int heightlen = 0;
				for (int j=0; j<height; j++) {
					if(grid[i][j]) {
						heightlen = j+1;
						widthlen[j]++;
						if(maxH < j+1) {
							maxH = j+1;
						}
					}
				}
				if(heightlen != heights[i]) {
					throw new RuntimeException("heights array is incorrect");
				}
			}
			if(!Arrays.equals(widthlen, widths)) {
				throw new RuntimeException("heights array is incorrect");
			}
			if(maxH != getMaxHeight()) {
				throw new RuntimeException("max height is incorrect");
			}
		}
	}
	
	/**
	 Given a piece and an x, returns the y
	 value where the piece would come to rest
	 if it were dropped straight down at that x.
	 
	 <p>
	 Implementation: use the skirt and the col heights
	 to compute this fast -- O(skirt length).
	*/
	public int dropHeight(Piece piece, int x) {
		int [] sk = piece.getSkirt();
		int maxY = -1;
		int index = -1;
		for (int i = 0; i < piece.getWidth(); i++) {
			if(heights[x+i] > maxY) {
				maxY = heights[x+i];
				index = i;
			}
		}
		int res = maxY-(Math.abs(sk[index]-sk[0]));
		while(true) {
			if(res >= heights[x]) {
				return res;
			} 
			res++;
		}
	}
	
	
	/**
	 Returns the height of the given column --
	 i.e. the y value of the highest block + 1.
	 The height is 0 if the column contains no blocks.
	*/
	public int getColumnHeight(int x) {
		return heights[x];
	}
	
	
	/**
	 Returns the number of filled blocks in
	 the given row.
	*/
	public int getRowWidth(int y) {
		return widths[y];
	}
	
	
	/**
	 Returns true if the given block is filled in the board.
	 Blocks outside of the valid width/height area
	 always return true.
	*/
	public boolean getGrid(int x, int y) {
		if(x < width || x >= 0 || y<height || y>=0 || grid[x][y]) {
				return true;
		}
	
		return false;
	}
	
	
	public static final int PLACE_OK = 0;
	public static final int PLACE_ROW_FILLED = 1;
	public static final int PLACE_OUT_BOUNDS = 2;
	public static final int PLACE_BAD = 3;
	
	/**
	 Attempts to add the body of a piece to the board.
	 Copies the piece blocks into the board grid.
	 Returns PLACE_OK for a regular placement, or PLACE_ROW_FILLED
	 for a regular placement that causes at least one row to be filled.
	 
	 <p>Error cases:
	 A placement may fail in two ways. First, if part of the piece may falls out
	 of bounds of the board, PLACE_OUT_BOUNDS is returned.
	 Or the placement may collide with existing blocks in the grid
	 in which case PLACE_BAD is returned.
	 In both error cases, the board may be left in an invalid
	 state. The client can use undo(), to recover the valid, pre-place state.
	*/

	public int place(Piece piece, int x, int y) {
		// flag !committed problem
		if (!committed) throw new RuntimeException("place commit problem");
		
		committed = false;
		int result = PLACE_OK;
		saveGrid();
		saveW();
		saveH();
		TPoint [] body = piece.getBody();
		int xcor = -1;
		int ycor = -1;
		for (int i = 0; i < body.length; i++) {
			xcor = x + body[i].x;
			ycor = y + body[i].y;
			if(xcor<0 || xcor >= width || ycor>= height || ycor<0) {
				sanityCheck();
				return PLACE_OUT_BOUNDS;
			}
			if(grid[xcor][ycor]) {
				sanityCheck();
				return PLACE_BAD;
			}
			grid[xcor][ycor] = true;
			widths[ycor]++;
			if(heights[xcor]<ycor+1) {
				heights[xcor] = ycor+1;
			}
			if(widths[ycor] == width) {
				result = PLACE_ROW_FILLED;
			}
		}
		sanityCheck();
		return result;
	}
	
	
	/**
	 Deletes rows that are filled all the way across, moving
	 things above down. Returns the number of rows cleared.
	*/
	public int clearRows() {
		int rowsCleared = 0;
		if (committed) {
			committed=false;
			saveGrid();
			saveH();
			saveW();
		}

		for(int i=0; i<height; i++) {
			if(widths[i] == width) {
				rowsCleared++;
				clearRow(i);
			}
		}
		for (int i = 0; i < width; i++) {
			heights[i] = heights[i] - rowsCleared;
		}

		sanityCheck();
		return rowsCleared;
	}
	
	private void clearRow(int index) {
		for (int i = 0; i < width; i++) {
			for (int j = index; j < getMaxHeight(); j++) {
				widths[j] = widths[j+1];
				grid[i][j] = grid[i][j+1];
			}
		}
	}
	/**
	 Reverts the board to its state before up to one place
	 and one clearRows();
	 If the conditions for undo() are not met, such as
	 calling undo() twice in a row, then the second undo() does nothing.
	 See the overview docs.
	*/
	public void undo() {
		if(committed == false) {
			boolean [][] tmp = new boolean[width][height];
			for (int i = 0; i < width; i++) {
				for (int j = 0; j < height; j++) {
					tmp[i][j] = oldG[i][j];
				}
			}
			saveGrid();
			for (int i = 0; i < width; i++) {
				for (int j = 0; j < height; j++) {
					grid[i][j] = tmp[i][j];
				}
			}
			
			int []tmpW = new int[widths.length];
			for (int i = 0; i < widths.length; i++) {
				tmpW[i] = oldW[i];
			}
			for (int i = 0; i < widths.length; i++) {
				oldW[i] = widths[i];
			}
			for (int i = 0; i < widths.length; i++) {
				widths[i] = tmpW[i];
			}
			
			int []tmpH = new int[heights.length];
			for (int i = 0; i < heights.length; i++) {
				tmpH[i] = oldH[i];
			}
			for (int i = 0; i < heights.length; i++) {
				oldH[i] = heights[i];
			}
			for (int i = 0; i < heights.length; i++) {
				heights[i] = tmpH[i];
			}
		}
		commit();
		sanityCheck();
	}
	
	
	/**
	 Puts the board in the committed state.
	*/
	public void commit() {
		committed = true;
	}


	
	/*
	 Renders the board state as a big String, suitable for printing.
	 This is the sort of print-obj-state utility that can help see complex
	 state change over time.
	 (provided debugging utility) 
	 */
	public String toString() {
		StringBuilder buff = new StringBuilder();
		for (int y = height-1; y>=0; y--) {
			buff.append('|');
			for (int x=0; x<width; x++) {
				if (getGrid(x,y)) buff.append('+');
				else buff.append(' ');
			}
			buff.append("|\n");
		}
		for (int x=0; x<width+2; x++) buff.append('-');
		return(buff.toString());
	}
	
	
	private void saveGrid() {
		for(int i =0;i<width;i++) {
			for (int j = 0; j < height; j++) {
				oldG[i][j] = grid[i][j];
			}
		}
	}
	private void saveW() {
		System.arraycopy(widths, 0, oldW, 0, widths.length);
	}
	private void saveH() {
		System.arraycopy(heights, 0, oldH, 0, heights.length);
	}
}


