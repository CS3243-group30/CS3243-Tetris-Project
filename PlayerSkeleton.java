import java.util.Arrays;

public class PlayerSkeleton {
	public static final int COLS = State.COLS;
	public static final int ROWS = State.ROWS;

	Ai ai = new Ai();

	//implement this function to have a working system
	public int pickMove(State s, int[][] legalMoves) {
		State currentState = s;

		int [][] currentField = currentState.getField();
		// System.out.println("CURRENT STATE: ");
		// printField(currentField);


		int nextPiece = currentState.getNextPiece();
		int [][] allPHeight = currentState.getpHeight();
		int [][] allPWidth = currentState.getpWidth();
		int [][][] allPTop = currentState.getpTop();
		int [][][] allPBottom = currentState.getpBottom();
		int turn = currentState.getTurnNumber();
		int [] top = currentState.getTop();
		// System.out.println("TOP : ");
		// System.out.println(Arrays.toString(top));

		// System.out.println(nextPiece);

		// System.out.println("Legal Moves:");
		// System.out.println(Arrays.deepToString(legalMoves));
		// System.out.println(legalMoves.length);

		double [] scoreArray = new double[legalMoves.length];
		for(int i = 0; i < legalMoves.length; i++) {

			//Clone the current state and determine next state
			int [][] nextState = new int[ROWS][COLS];

			for(int a = 0; a < currentField.length; a++) {
    			nextState[a] = currentField[a].clone();
			}

			int [] topClone = new int[top.length];
			topClone = top.clone();

			nextState = tryMove(turn,topClone, allPHeight, allPWidth, allPTop, allPBottom, nextPiece, nextState, legalMoves[i][0], legalMoves[i][1]);

			//Calculate current score for the state
			scoreArray[i] = ai.calculateScore(nextState);
		}

		//Pick the move with the highest score
		double highestScore = scoreArray[0];
		int bestMoveIndex = 0;
		for(int i = 0; i < scoreArray.length; i++){
			if(scoreArray[i] > highestScore) {
				highestScore = scoreArray[i];
				bestMoveIndex = i;
			}
		}

		//Return the move
		return bestMoveIndex;
	}

	//This method is taken from the State.java class
	public int[][] tryMove(int turn, int[] top, int[][] pHeight, int[][]pWidth, int[][][] pTop, int[][][] pBottom, int nextPiece, int [][] field, int orient, int slot) {
		turn++;
		//height if the first column makes contact
		int height = top[slot]-pBottom[nextPiece][orient][0];
		//for each column beyond the first in the piece
		for(int c = 1; c < pWidth[nextPiece][orient];c++) {
			height = Math.max(height,top[slot+c]-pBottom[nextPiece][orient][c]);
		}

		//check if game ended
		if(height+pHeight[nextPiece][orient] >= ROWS) {
			// lost = true;
			int[][] gameOver = new int [ROWS][COLS];
			for(int i = 0 ; i < ROWS; i++) {
				for(int j = 0; j < COLS; j++) {
					gameOver[i][j] = 1;
				}
			}
			System.out.println("Game Over State");
			return gameOver;
		}


		//for each column in the piece - fill in the appropriate blocks
		for(int i = 0; i < pWidth[nextPiece][orient]; i++) {

			//from bottom to top of brick
			for(int h = height+pBottom[nextPiece][orient][i]; h < height+pTop[nextPiece][orient][i]; h++) {
				field[h][i+slot] = turn;
			}
		}

		//adjust top
		for(int c = 0; c < pWidth[nextPiece][orient]; c++) {
			top[slot+c]=height+pTop[nextPiece][orient][c];
		}

		int rowsCleared = 0;

		//check for full rows - starting at the top
		for(int r = height+pHeight[nextPiece][orient]-1; r >= height; r--) {
			//check all columns in the row
			boolean full = true;
			for(int c = 0; c < COLS; c++) {
				if(field[r][c] == 0) {
					full = false;
					break;
				}
			}
			//if the row was full - remove it and slide above stuff down
			if(full) {
				rowsCleared++;
				// cleared++;
				//for each column
				for(int c = 0; c < COLS; c++) {

					//slide down all bricks
					for(int i = r; i < top[c]; i++) {
						field[i][c] = field[i+1][c];
					}
					//lower the top
					top[c]--;
					while(top[c]>=1 && field[top[c]-1][c]==0)	top[c]--;
				}
			}
		}

		printField(field);

		return field;
	}

	//This method is to print the current field
	public void printField(int [][] field) {
		int [][] printField = new int [ROWS][COLS];
		for(int i = 0; i < printField.length; i++) {
			printField[i] = field[field.length - i - 1];
		}
		System.out.println("Print Field");
		System.out.println(Arrays.deepToString(printField).replace("], ", "]\n"));
	}

	public static void main(String[] args) {
		State s = new State();
		new TFrame(s);
		PlayerSkeleton p = new PlayerSkeleton();
		while(!s.hasLost()) {
			s.makeMove(p.pickMove(s,s.legalMoves()));
			s.draw();
			s.drawNext(0,0);
			try {
				Thread.sleep(50);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
		System.out.println("You have completed "+s.getRowsCleared()+" rows.");
	}

	public class Ai {

		public static final double totalHeightWeight = -0.6;
		public static final double maxHeightWeight = -0.6;
		public static final double linesCompletedWeight = 0.7;
		public static final double holesWeight = -0.3;
		public static final double absTotalDifferenceHeightWeight = -0.1;
	
		private void trainAi() {
			
		}
	
		//This method is to calculate the score for the next state
		public double calculateScore(int[][] nextState) {
			double score = 0;
	
			//Calculate Total Height
			int totalHeight = checkHeight(nextState);
	
			//Calculate Max Height
			int maxHeight = calculateMaxHeight(nextState);
	
			//Calculate number of Completed Lines in the state
			int numLines = completedLines(nextState);

			//Calculate number of holes in the grid
			int numHoles = calculateHoles(nextState);

			//Calculate absolute height difference
			int absTotalHeightDiff = calculateAbsDiff(nextState);
	
			return totalHeight * totalHeightWeight + maxHeight * maxHeightWeight + numLines * linesCompletedWeight +
					numHoles * holesWeight + absTotalHeightDiff * absTotalDifferenceHeightWeight;

		}
	
		//Calculates the total height of all the coloums
		public int checkHeight(int[][] nextState) {
			int totalHeight = 0;
			int currentColHeight = 0;
			for(int i = 0; i < State.COLS; i++) {
				currentColHeight = 0;
				for(int j = 0; j < State.ROWS; j++) {
	
					if(nextState[j][i] != 0) {
						currentColHeight = j + 1;
						// System.out.print(i);
						// System.out.print(" , ");
						// System.out.print(j);
						// System.out.println();
					}
				}
				// System.out.println(currentColHeight);
				totalHeight += currentColHeight;
			}
	
			// System.out.print("HEIGHT: ");
			// System.out.println(totalHeight);
			return totalHeight;
		}
	
		//Calculates the maximum height of all the coloums
		public int calculateMaxHeight(int[][] nextState) {
			int max = 0;
			int currentColHeight = 0;
			for(int i = 0; i < State.COLS; i++) {
				currentColHeight = 0;
				for(int j = 0; j < State.ROWS; j++) {
	
					if(nextState[j][i] != 0) {
						currentColHeight = j + 1;
					}
				}
				if(currentColHeight > max) {
					max = currentColHeight;
				}
			}
			return max;
		}
	
		public int completedLines(int[][] nextState) {
			int lines = 0;
			for(int i = 0; i < State.ROWS; i++) {
				if(isLine(i, nextState)) {
					lines++;
				}
			}
			return lines;
		}
	
		private boolean isLine(int row, int[][] nextState) {
			for(int i = 0; i < State.COLS; i++) {
				if(nextState[row][i] == 0) {
					return false;
				}
			}
			return true;
		}

		public int calculateHoles(int[][] nextState) {
			int numHoles = 0;
			for(int i = 0; i < State.COLS; i++) {
				boolean isHole = false;
				for(int j = 0; j < State.ROWS; j++) {
					if(nextState[j][i] == 0) {
						isHole = true;
					}
					else if(nextState[j][i] != 0 && isHole) {
						numHoles++;
						isHole = false;
					}
				}
			}
			// System.out.print("Number of holes: ");
			// System.out.println(numHoles);
			return numHoles;
		}

		public int calculateAbsDiff(int[][] nextState) {
			int value = 0;
			for(int i = 0; i < State.COLS - 1; i++) {
				value += Math.abs(columnHeight(i, nextState) - columnHeight(i+1, nextState));
			}
			return value;
		}

		private int columnHeight(int column, int[][] nextState) {
			int count = 0;
			for(; count < State.ROWS && nextState[count][column] == 0; count++);
			return count;
		}
		
	}

}