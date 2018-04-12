import java.awt.Component;
import java.awt.List;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;

import sun.java2d.cmm.ColorTransform;

public class PlayerSkeleton {
	public static final int COLS = State.COLS;
	public static final int ROWS = State.ROWS;
	public static final int N_PIECES = State.N_PIECES;
	public static final int LOOKAHEAD = 0;
	public static final int PIECESNUM = StateTrain.PIECESNUM;

	//implement this function to have a working system
	public int pickMove(State s, int[][] legalMoves, Ai ai) {

		int [][] currentField = s.getField();
		// System.out.println("CURRENT STATE: ");
		// printField(currentField);
		
		int nextPiece = s.getNextPiece();
		int [][] allPHeight = s.getpHeight();
		int [][] allPWidth = s.getpWidth();
		int [][][] allPTop = s.getpTop();
		int [][][] allPBottom = s.getpBottom();
		int turn = s.getTurnNumber();
		int [] top = s.getTop();
		int [] pOrients = s.getpOrients();
		int[][][] legalCopy = new int[N_PIECES][][];
		
		//initialize legalCopy
		//for each piece type
		for(int i = 0; i < N_PIECES; i++) {
			//figure number of legal moves
			int n = 0;
			for(int j = 0; j < pOrients[i]; j++) {
				//number of locations in this orientation
				n += COLS+1-allPWidth[i][j];
			}
			//allocate space
			legalCopy[i] = new int[n][2];
			//for each orientation
			n = 0;
			for(int j = 0; j < pOrients[i]; j++) {
				//for each slot
				for(int k = 0; k < COLS+1-allPWidth[i][j];k++) {
					legalCopy[i][n][0] = j;
					legalCopy[i][n][1] = k;
					n++;
				}
			}
		}
		
		// Edit the number after ai for the number of turns to look ahead
		IndexScore finalResult = lookAhead(turn, top, allPHeight, allPWidth, allPTop, allPBottom, nextPiece, currentField, legalMoves, legalCopy, ai, LOOKAHEAD, new IndexScore(0, Integer.MAX_VALUE));
		return finalResult.index;
	}
	
	public int pickMove(StateTrain s, int[][] legalMoves, Ai ai) {

		int [][] currentField = s.getField();
		// System.out.println("CURRENT STATE: ");
		// printField(currentField);
		
		int nextPiece = s.getNextPiece();
		int [][] allPHeight = s.getpHeight();
		int [][] allPWidth = s.getpWidth();
		int [][][] allPTop = s.getpTop();
		int [][][] allPBottom = s.getpBottom();
		int turn = s.getTurnNumber();
		int [] top = s.getTop();
		int [] pOrients = s.getpOrients();
		int[][][] legalCopy = new int[N_PIECES][][];
		
		//initialize legalCopy
		//for each piece type
		for(int i = 0; i < N_PIECES; i++) {
			//figure number of legal moves
			int n = 0;
			for(int j = 0; j < pOrients[i]; j++) {
				//number of locations in this orientation
				n += COLS+1-allPWidth[i][j];
			}
			//allocate space
			legalCopy[i] = new int[n][2];
			//for each orientation
			n = 0;
			for(int j = 0; j < pOrients[i]; j++) {
				//for each slot
				for(int k = 0; k < COLS+1-allPWidth[i][j];k++) {
					legalCopy[i][n][0] = j;
					legalCopy[i][n][1] = k;
					n++;
				}
			}
		}
		
		// Edit the number after ai for the number of turns to look ahead
		IndexScore finalResult = lookAhead(turn, top, allPHeight, allPWidth, allPTop, allPBottom, nextPiece, currentField, legalMoves, legalCopy, ai, LOOKAHEAD, new IndexScore(0, Integer.MAX_VALUE));
		return finalResult.index;
	}
	
	public int[] topClone(int[] oldTop) {
		int[] clone = new int[oldTop.length];
		clone = oldTop.clone();
		return clone;
	}
	
	public int[][] fieldClone(int[][] oldfield) {
		int [][] nextState = new int[ROWS][COLS];

		for(int a = 0; a < oldfield.length; a++) {
			nextState[a] = oldfield[a].clone();
		}
		return nextState;
	}
	
	public IndexScore lookAhead(int turn, int[] top, int[][] allPHeight, int[][]allPWidth, int[][][] allPTop, int[][][] allPBottom, int nextPiece, int [][] field, int[][] legalMoves, int[][][] legalcopy, Ai ai, int ahead, IndexScore ab) {
		if (ahead == 0) {
			return bestMoveIndex(turn, topClone(top), allPHeight, allPWidth, allPTop, allPBottom, nextPiece, fieldClone(field), legalMoves, ai, ab);
		} else {
			IndexScore[] movesIndexScore = new IndexScore[legalMoves.length];
			IndexScore betaAlpha = new IndexScore(0, Integer.MIN_VALUE);
			ArrayList<IndexScore> store = new ArrayList<IndexScore>();
			boolean isBase = true;
			// Within legalMoves choose best
			for (int i = 0; i < legalMoves.length; i++) {
				MoveResult currentResult = tryMove(turn, topClone(top), allPHeight, allPWidth, allPTop, allPBottom, nextPiece, fieldClone(field), legalMoves[i][0], legalMoves[i][1]);
				if (currentResult.isGameOver) {
					movesIndexScore[i] = new IndexScore(i, Integer.MIN_VALUE);
				} else {
					//within next piece choose worst
					IndexScore alphaBeta = new IndexScore(0, Integer.MAX_VALUE);
					IndexScore[] nextScoreArray = new IndexScore[7];
					store.add(new IndexScore(i, ai.calculateScore(currentResult.fieldState, currentResult.rowsCleared)));
					
					boolean isBroken = false;
					for (int p = 0; p < 7; p++) {
						nextScoreArray[p] = lookAhead(currentResult.turn, topClone(currentResult.topValue), allPHeight, allPWidth, allPTop, allPBottom, p, fieldClone(currentResult.fieldState), legalcopy[p], legalcopy, ai, ahead-1, alphaBeta);
						if (nextScoreArray[p].score < betaAlpha.score) {
							isBroken = true;
							break;
						}
					}
					if (!isBroken) {
						movesIndexScore[i] = nextScoreArray[0];
						for (int p = 1; p < 7; p++) {
							if (nextScoreArray[p].score < movesIndexScore[i].score) {
								movesIndexScore[i] = nextScoreArray[p];
							}
						}
						if (movesIndexScore[i].score > betaAlpha.score) {
							isBase = true;
							betaAlpha.index = i;
							betaAlpha.score = movesIndexScore[i].score;
						}
					}
				}
				
			}
			if (isBase) {
				for (int i = 0; i < store.size(); i++) {
					if (store.get(i).score > betaAlpha.score) {
						betaAlpha.index = store.get(i).index;
						betaAlpha.score = store.get(i).score;
					}
				}
			}
			return betaAlpha;
		}
	}
	
	public IndexScore bestMoveIndex(int turn, int[] top, int[][] allPHeight, int[][]allPWidth, int[][][] allPTop, int[][][] allPBottom, int nextPiece, int [][] field, int[][] legalMoves, Ai ai, IndexScore ab) {
		double [] scoreArray = new double[legalMoves.length];
		for(int i = 0; i < legalMoves.length; i++) {

			MoveResult result = tryMove(turn,topClone(top), allPHeight, allPWidth, allPTop, allPBottom, nextPiece, fieldClone(field), legalMoves[i][0], legalMoves[i][1]);

			//Calculate current score for the state
			scoreArray[i] = ai.calculateScore(result.fieldState, result.rowsCleared);
			if (scoreArray[i] > ab.score) {
				return new IndexScore(i, Integer.MAX_VALUE);
			}
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
		if (highestScore < ab.score) {
			ab.score = highestScore;
		}
		//Return the move
		return new IndexScore(bestMoveIndex, highestScore);
	}
	
	//This method is taken from the State.java class
	public MoveResult tryMove(int turn, int[] top, int[][] pHeight, int[][]pWidth, int[][][] pTop, int[][][] pBottom, int nextPiece, int [][] field, int orient, int slot) {
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
			return new MoveResult(gameOver, top, turn, true, 0);
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
		return new MoveResult(field, top, turn, false, rowsCleared);
	}
	
	public static int playgame(Ai ai) {
		State s = new State();
		new TFrame(s);
		PlayerSkeleton p = new PlayerSkeleton();
		while(!s.hasLost()) {
			s.makeMove(p.pickMove(s,s.legalMoves(), ai));
			s.draw();
			s.drawNext(0,0);
			
			int[][] field = s.getField();
			
			/*
			System.out.println("maxHeight: " + ai.calculateMaxHeight(field));
			System.out.println("numHoles: " + ai.calculateHoles(field));
			System.out.println("absTotalHeightDiff: " + ai.calculateAbsDiff(field));
			System.out.println("meanHeightDiff: " + ai.calculateMeanHeightDiff(field));
			System.out.println("pitDepths: " + ai.getPitDepths(field));
			System.out.println("");
			*/
			
			try {
				Thread.sleep(0);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
		//System.out.println("You have completed "+ s.getRowsCleared() +" rows.");
		return s.getRowsCleared();
	}
	
	public static int testgame(Ai ai, StateTrain s) {
		PlayerSkeleton p = new PlayerSkeleton();
		while(!s.hasLost() && s.getTurnNumber() < PIECESNUM) {
			s.makeMove(p.pickMove(s,s.legalMoves(), ai));
			
			try {
				Thread.sleep(0);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
		if (s.getTurnNumber() == PIECESNUM) {
			ai.hitLimit = true;
		}
		return s.getRowsCleared();
	}
	
	public static void main(String[] args) {
		PlayerSkeleton skele = new PlayerSkeleton();
		
		
		//Ai ai = skele.new Ai();
		//for (int i = 0; i < 20; i++) { System.out.println(playgame(ai)); }
		
		
		AiGeneticTrainer trainer = skele.new AiGeneticTrainer();
		trainer.convergeTrain();
	}
	
	public class MoveResult {
		public int[][] fieldState;
		public int[] topValue;
		public int turn;
		public boolean isGameOver;
		public int rowsCleared;
		
		public MoveResult(int[][] fieldState, int[] topValue, int turn, boolean isGameOver, int rowsCleared) {
			this.fieldState = fieldState;
			this.topValue = topValue;
			this.turn = turn;
			this.isGameOver = isGameOver;
			this.rowsCleared = rowsCleared;
		}
	}
	
	public class IndexScore {
		public int index;
		public double score;
		
		public IndexScore(int index, double score) {
			this.index = index;
			this.score = score;
		}
	}
	
	public class Ai {
		public double maxHeightWeight;
		public double linesCompletedWeight ;
		public double holesWeight;
		public double absTotalDifferenceHeightWeight;
		public double meanHeightDifferenceWeight;
		public double pitDepthsWeight;
		public double rowTransWeight;
		public double colTransWeight;
		public double holeDepWeight;
		public double ocpCellWeight;

		public Integer score;
		public boolean isScored;
		public boolean hitLimit;

		//Creating an AI with hard coded weight. After running a learning program copy the result here for actual playing
		public Ai() {
			score = 0;
			isScored = false;
			hitLimit = false;

			maxHeightWeight = 0.09075565115678885;
			linesCompletedWeight = 0.03938722776647241;
			holesWeight = 0.9510449525926056;
			absTotalDifferenceHeightWeight = 0.11901856198101057;
			meanHeightDifferenceWeight = 0.03163098523307584;
			pitDepthsWeight = 0.16510041777601026;
		}
		
		//Creating an AI with given weights mainly for learning process.
		public Ai(double a, double b, double c, double d, double e, double f, double g, double h, double i, double j) {
			score = 0;
			isScored = false;
			hitLimit = false;
			
			maxHeightWeight = a;
			linesCompletedWeight = b;
			holesWeight = c;
			absTotalDifferenceHeightWeight = d;
			meanHeightDifferenceWeight = e;
			pitDepthsWeight = f;
			rowTransWeight = g;
			colTransWeight = h;
			holeDepWeight = i;
			ocpCellWeight = j;
		}
		
		public Ai(Ai other) {
			score = 0;
			isScored = false;
			hitLimit = false;
			
			maxHeightWeight = other.maxHeightWeight;
			linesCompletedWeight = other.linesCompletedWeight;
			holesWeight = other.holesWeight;
			absTotalDifferenceHeightWeight = other.absTotalDifferenceHeightWeight;
			meanHeightDifferenceWeight = other.meanHeightDifferenceWeight;
			pitDepthsWeight = other.pitDepthsWeight;
			rowTransWeight = other.rowTransWeight;
			colTransWeight = other.colTransWeight;
			holeDepWeight = other.holeDepWeight;
		}
		
		public double getHeight() {
			return maxHeightWeight;
		}
		
		public double getLines() {
			return linesCompletedWeight;
		}
		
		public double getHoles() {
			return holesWeight;
		}
		
		public double getTotalDiff() {
			return absTotalDifferenceHeightWeight;
		}
		
		public double getMeanHeight() {
			return meanHeightDifferenceWeight;
		}
		
		public double getPitDepth() {
			return pitDepthsWeight;
		}
		
		public double getRowTransWeight()
		{
			return rowTransWeight;
		}

		public double getColTransWeight(){
			return colTransWeight;
		}

		public double getHoleDepWeight(){return holeDepWeight;}

		public double getOcpCellWeight(){return ocpCellWeight;}

		//This method is to calculate the score for the next state
		public double calculateScore(int[][] nextState, int linesCompleted) {
			//Calculate Max Height
			int maxHeight = calculateMaxHeight(nextState);
	
			//Calculate number of holes in the grid
			int numHoles = calculateHoles(nextState);

			//Calculate absolute height difference
			int absTotalHeightDiff = calculateAbsDiff(nextState);
			
			//Calculate Mean height difference
			double meanHeightDiff = calculateMeanHeightDiff(nextState);


			int pitDepths = getPitDepths(nextState);
			
			int rowTrans = getRowTrans(nextState);

			int colTrans = getColTrans(nextState); 

			int holeDepths = getHoleDepths(nextState);

			int ocpCells = getOccupiedCells(nextState);

			if (isGameOver(nextState)) {
				return Integer.MIN_VALUE;
			}
			
			return maxHeight * -maxHeightWeight
					+ linesCompleted * linesCompletedWeight
					+ numHoles * -holesWeight
					+ absTotalHeightDiff * -absTotalDifferenceHeightWeight
					+ meanHeightDiff * -meanHeightDifferenceWeight
					+ pitDepths * -pitDepthsWeight
					+ rowTrans * -rowTransWeight
					+ colTrans * -colTransWeight
					+ holeDepths* -holeDepWeight
					+ ocpCells * -ocpCellWeight;
		}
		
		public boolean isGameOver(int[][] nextState) {
			for (int col = 0; col < State.COLS; col++) {
				for (int row = 0; row < State.ROWS; row++) {
					if (nextState[row][col] != 1) {
						return false;
					}
				}
			}
			return true;
		}
		
		public int getPitDepths(int[][] nextState) {
			int[] colHeights = new int[State.COLS];
			for (int i = 0; i < State.COLS; i++) {
				colHeights[i] = getColHeight(nextState, i);
			}
			
			int toReturn = 0;
			
			for (int i = 1; i < State.COLS - 1; i++) {
				if (colHeights[i] < colHeights[i-1]-1 && colHeights[i] < colHeights[i+1]-1) {
					toReturn += Math.max(colHeights[i-1], colHeights[i+1]) - colHeights[i];
				}
			}
			
			if (colHeights[0] < colHeights[1]-1) {
				toReturn += colHeights[1] - colHeights[0];
			}
			
			if (colHeights[State.COLS-1] < colHeights[State.COLS-2]-1) {
				toReturn += colHeights[State.COLS-2] - colHeights[State.COLS-1];
			}
			return toReturn;
		}
		
		public int getColHeight(int[][] nextState, int col) {
			int height = State.ROWS;
			for (int i = State.ROWS - 1; i >= 0; i--) {
				if(nextState[i][col] == 0) {
					height--;
				} else {
					break;
				}
			}
			
			return height;
		}
		
		//Calculates the total height of all the coloums
		public int checkHeight(int[][] nextState) {
			int totalHeight = 0;
			for(int i = 0; i < State.COLS; i++) {
				totalHeight += getColHeight(nextState, i);
			}
			
			return totalHeight;
		}
		
		//Calculates the maximum height of all the column
		public int calculateMaxHeight(int[][] nextState) {
			int max = 0;
			int currentColHeight = 0;
			for(int i = 0; i < State.COLS; i++) {
				currentColHeight = getColHeight(nextState, i);
				if (currentColHeight > max) {
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
		
		// Calculate the number of Holes
		public int calculateHoles(int[][] nextState) {
			int numHoles = 0;
			for(int i = 0; i < State.COLS; i++) {
				boolean topPiece = false;
				for(int j = State.ROWS - 1; j >= 0; j--) {
					if(nextState[j][i] != 0) {
						topPiece = true;
					} else if(nextState[j][i] == 0 && topPiece) {
						numHoles++;
					}
				}
			}
			// System.out.print("Number of holes: ");
			// System.out.println(numHoles);
			return numHoles;
		}
		public int getHoleDepths(int[][] nextState){
				//check how many sucessive full cells are above each hole
				int holeDepths = 0;

				for(int i = 0; i < State.COLS; ++i){
					int cur_height = getColHeight(nextState, i);
					for (int j = 0; j < cur_height; ++j){
						if(nextState[j][i] == 0){
							holeDepths += (cur_height - j);
						}

					}

				}
			return holeDepths;
		}
		public int calculateAbsDiff(int[][] nextState) {
			int value = 0;
			for(int i = 0; i < State.COLS - 1; i++) {
				value += Math.abs(getColHeight(nextState, i) - getColHeight(nextState, i+1));
			}
			return value;
		}
		
		public double calculateMeanHeightDiff(int[][] nextState) {
			int max = calculateMaxHeight(nextState);
			double toReturn = 0;
			for (int i = 0; i < State.COLS; i++) {
				toReturn += (max - getColHeight(nextState, i));
			}
		
			return toReturn/(State.COLS - 1);
		}
		//get row transition numbers
		public int getRowTrans(int[][] nextState)
		{
			int lineChane = 0, lastCell ;
			int maxHeight = calculateMaxHeight(nextState);
			for(int i = 0;i<= maxHeight; ++i) {
				int[] row = nextState[i];
				lastCell = 1;
				for(int j = 0; j < row.length; ++j) {
					int currCell = 0;
					if(row[j] > 0){
						currCell = 1;
					}
					if ((lastCell * currCell== 0) && (lastCell != currCell)) {
						lineChane++;
						lastCell = currCell;
					}
					if (j == (row.length- 1) && row[j] == 0) {
						lineChane++;
					}
				}
			}
			//empty rows have 2 transtions each (walls)
			lineChane +=2*(State.ROWS - 1- maxHeight);

			return lineChane;
		}
		//get columnn transition numbers
		public int getColTrans(int[][] nextState)
		{
			int colChane = 0, lastCell;
			for (int i = 0; i < State.COLS; ++i)
			{
				lastCell = 1;
				for (int j = 0;j < nextState.length; j++)
				{
					int currCell = 0;
					if(nextState[j][i] > 0){
						currCell = 1;

					}
					if ((lastCell * currCell == 0) && (lastCell !=currCell))
					{
						colChane++;
						lastCell = currCell;
					}
					if (j == ( nextState.length - 1) && currCell == 0)
					{
						colChane++;
					}
				}
			}
			return colChane;
		}
		//get the number of fully occupied cells
		public int getOccupiedCells(int[][] nextState){
				int maxHeight = calculateMaxHeight(nextState);
				int ocpCells = 0;

				for(int i = 0; i < maxHeight; ++i){
					for (int j = 0; j < State.COLS; ++j){
						if(nextState[i][j] != 0){
							ocpCells++;

						}

					}
				}
			return ocpCells;
		}
		//get the landing height of the last peice

		@Override
		public boolean equals(Object o) {
			if (o == this) {
				return true;
			}
			
			if (!(o instanceof Ai)) {
				return false;
			}
			
			Ai other = (Ai) o;
			return maxHeightWeight == other.maxHeightWeight
					&& linesCompletedWeight == other.linesCompletedWeight
					&& holesWeight == other.holesWeight
					&& absTotalDifferenceHeightWeight == other.absTotalDifferenceHeightWeight
					&& meanHeightDifferenceWeight == other.meanHeightDifferenceWeight
					&& pitDepthsWeight == other.pitDepthsWeight
					&& rowTransWeight == other.rowTransWeight
					&& colTransWeight == other.colTransWeight
					&& holeDepWeight == other.holeDepWeight;
		}
	}
	
	//Trains an Ai
	public class AiGeneticTrainer {
		public AiGeneticTrainer() {
		}
		
		public double randomWeight() {
			return Math.random();
		}
		
		public void convergeTrain() {

			Ai[][] initialgen = new Ai[16][200];
			for (int i = 0; i < 16; i++) {
				for (int j = 0; j < 200; j++) {
					initialgen[i][j] = getRandomAi();
				}
				startTrain(initialgen[i], i, 16);
			}
			
			Ai[][] secondgen = new Ai[4][200];
			for (int i = 0; i < 4; i++) {
				for (int j = 0; j < 4; j++) {
					for (int c = 0; c < 50; c++) {
						secondgen[i][j*50+c] = new Ai(initialgen[i*4+j][150+c]);
					}
				}
				startTrain(secondgen[i], i, 4);
			}
			Ai[] thirdgen = new Ai[200];
			for (int i = 0; i < 4; i++) {
				for (int c = 0; c < 50; c++) {
					thirdgen[i*50+c] = new Ai(secondgen[i][150+c]);
				}
			}
			startTrain(thirdgen, 0, 1);
			
			System.out.println("Training Completed");
			System.out.println(" ");
			System.out.println("maxHeightWeight = " + thirdgen[199].maxHeightWeight
			+ ";\nlinesCompletedWeight = " + thirdgen[199].linesCompletedWeight
			+ ";\nholesWeight = " + thirdgen[199].holesWeight
			+ ";\nabsTotalDifferenceHeightWeight = " + thirdgen[199].absTotalDifferenceHeightWeight
			+ ";\nmeanHeightDifferenceWeight = " + thirdgen[199].meanHeightDifferenceWeight
			+ ";\npitDepthsWeight = " + thirdgen[199].pitDepthsWeight 
			+ ";\nrowTransitionWeight = " + thirdgen[199].rowTransWeight
			+ ";\ncolTransitionWeight = "+ thirdgen[199].colTransWeight
			+ ";\nholeDepWeight = " + thirdgen[199].holeDepWeight);
		}
		
		//Function for training AI
		public void startTrain(Ai[] generation, int setnum, int outof) {
			
			Comparator<Ai> aiSorter = new Comparator<Ai>() {
				public int compare(Ai a1, Ai a2) {
					return (a1.score).compareTo(a2.score);
				};
			};
			
			StateTrain[] baseState = new StateTrain[3];
			for (int i = 0; i < 3; i++) {
				baseState[i] = new StateTrain();
			}
			int highestScore = 0;
			int stagnent = 0;
			
			
			int repeat = 0;
			while (stagnent < 20) {
				System.out.println("Set number " + setnum + " out of " + outof);
				System.out.println("Generation number: " + repeat);
				scoreTest(generation, baseState);
				Arrays.sort(generation, aiSorter);
				
				if (generation[199].score > highestScore) {
					highestScore = generation[199].score;
					stagnent = 0;
				} else {
					stagnent++;
				}
				System.out.println("Stagnent: " + stagnent);
				System.out.println(" ");
				printScore(generation);
				
				//Create 30 children
				Ai[] nextgen = new Ai[150];
				
				ArrayList<Ai> ais = new ArrayList<Ai>();
				int counter = 199;
				while(ais.size() < 50) {
					if (!ais.contains(generation[counter])) {
						ais.add(generation[counter]);
					}
					counter--;
				}

				for (int i = 0; i < 150; i++) {
					Collections.shuffle(ais);
					nextgen[i] = mergeAi(ais.get(0), ais.get(1));
				}
				
				//the 30 worst Ai within the population would get replaced by children
				for (int i = 0; i < 150; i++) {
					generation[i] = nextgen[i];
				}
				
				for (int i = 0; i < 50; i++) {
					generation[i+150] = ais.get(i);
				}

				repeat++;
			}
			scoreTest(generation, baseState);
			Arrays.sort(generation, aiSorter);
		}
		
		public void scoreTest(Ai[] ai, StateTrain[] bs) {
			int notScored = 0;
			for (int i = 0; i < ai.length; i++) {
				if (!(ai[i].isScored)) {
					int scoreSum = 0;
					for (int j = 0; j < 3; j++) {
						StateTrain s = new StateTrain(bs[j].pieceArray);
						scoreSum += testgame(ai[i], s);
					}
					ai[i].score = scoreSum;
					ai[i].isScored = true;
					notScored++;
				}
			}
			
			int numMax = 0;
			for (int i = 0; i < ai.length; i++) {
				if (ai[i].hitLimit) {
					numMax++;
				}
			}
			System.out.println("newly scored AI: " + notScored);
			System.out.println("Number of AI hit limit: " + numMax);
			
		}
		
		public void printScore(Ai[] toprint) {
			for (int i = 0; i < 200; i++) {
				if (i % 25 == 24) {
					System.out.println(toprint[i].score);
				} else {
					System.out.print(toprint[i].score + ", ");
				}
			}
			
				System.out.println(" ");
				
				System.out.println("maxHeightWeight = " + toprint[199].maxHeightWeight
				+ ";\nlinesCompletedWeight = " + toprint[199].linesCompletedWeight
				+ ";\nholesWeight = " + toprint[199].holesWeight
				+ ";\nabsTotalDifferenceHeightWeight = " + toprint[199].absTotalDifferenceHeightWeight
				+ ";\nmeanHeightDifferenceWeight = " + toprint[199].meanHeightDifferenceWeight
				+ ";\npitDepthsWeight = " + toprint[199].pitDepthsWeight 
				+ ";\nrowTransitionsWeight = " + toprint[199].rowTransWeight 
				+ ";\ncolTransitionsWeight = " + toprint[199].colTransWeight
				+ ";\nholeDepWeight = " + toprint[199].holeDepWeight
						+ ";\nocpCellWeight = " + toprint[199].ocpCellWeight
						+ ";\n\n");
		}
		
		//Creates a child Ai where the values are based off parents
		//There is a 5% chance that 1 of the herustic would be mutated and become random
		//Otherwise it takes the value in between the 2 parent leaning slightly to the better one.
		public Ai mergeAi(Ai dad, Ai mom) {
			ArrayList<Integer> numbers = new ArrayList<Integer>();
			for (int i = 0; i < 4; i++) {
				numbers.add(0);
				numbers.add(1);
			}
			Collections.shuffle(numbers);
			Ai child = new Ai(mergeValue(dad.maxHeightWeight, mom.maxHeightWeight, numbers.get(0)), 
					mergeValue(dad.linesCompletedWeight, mom.linesCompletedWeight, numbers.get(1)), 
					mergeValue(dad.holesWeight, mom.holesWeight, numbers.get(2)), 
					mergeValue(dad.absTotalDifferenceHeightWeight, mom.absTotalDifferenceHeightWeight, numbers.get(3)),
					mergeValue(dad.meanHeightDifferenceWeight, mom.meanHeightDifferenceWeight, numbers.get(4)),
					mergeValue(dad.pitDepthsWeight, mom.pitDepthsWeight, numbers.get(5)), 
					mergeValue(dad.rowTransWeight, mom.rowTransWeight, numbers.get(6)),
					mergeValue(dad.colTransWeight, mom.colTransWeight, numbers.get(7)),
					mergeValue(dad.holesWeight, mom.holesWeight, numbers.get(0)),
					mergeValue(dad.ocpCellWeight, mom.ocpCellWeight, numbers.get(1))
			);
			
			return child;
		}
		
		public double mergeValue(double x, double y, Integer i) {
			double rng = Math.random();
			if (rng < 0.1) {
				return randomWeight();
			} else if (i == 1) {
				return x;
			} else {
				return y;
			}
		}
		
		
		
		public Ai getRandomAi() {
			return new Ai(randomWeight(), randomWeight(), randomWeight(), randomWeight(), randomWeight(), randomWeight(), randomWeight(), randomWeight(), randomWeight(), randomWeight());
		}
		
		//Selects 10 Ai out of the given population of 100
		public Ai[] selector(Ai[] population) {
			ArrayList<Object> list = new ArrayList<>();
			Ai[] picked = new Ai[10];
			for (int i = 0; i < 10; i++) {
				int rng = (int) Math.floor(Math.random()*100);
				while (list.contains(rng)) {
					rng =  (int) Math.floor(Math.random()*100);
				}
				list.add(rng);
				picked[i] = population[rng];
			}
			return picked;
		}
	}
}
