import java.awt.List;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Random;

public class PlayerSkeleton {
	public static final int COLS = State.COLS;
	public static final int ROWS = State.ROWS;
	public static final int N_PIECES = 7;
	public static final int ORIENT = 0;
	public static final int SLOT = 1;
	public static int [] nextTop;

	Ai ai = new Ai();

	//implement this function to have a working system
	public int pickMove(State s, int[][] legalMoves, Ai ai) {
		ArrayList<Double> futureScoreList = new ArrayList<Double>();

		int [][] currentField = s.getField();
		 // System.out.println("CURRENT STATE: ");
		 // printField(currentField);


		int nextPiece = s.getNextPiece();
		int [][] allPHeight = s.getpHeight();
		int [][] allPWidth = s.getpWidth();
		int [][][] allPTop = s.getpTop();
		int [][][] allPBottom = s.getpBottom();
		int [] allPOrients = s.getpOrients();
		int turn = s.getTurnNumber();
		int [] top = s.getTop();

		double [] scoreArray = new double[legalMoves.length];
		for(int i = 0; i < legalMoves.length; i++) {

			//Clone the current state and determine next state
			int [][] nextState = new int[ROWS][COLS];

			for(int a = 0; a < currentField.length; a++) {
    			nextState[a] = currentField[a].clone();
			}

			int [] topClone = new int[top.length];
			topClone = top.clone();

			nextState = tryMove(turn,topClone, allPHeight, allPWidth, allPTop, allPBottom, nextPiece, nextState, legalMoves[i][0], legalMoves[i][1], true);

			// System.out.println("NEXT STATE: ");
 		 // printField(nextState);

			//Lookahead one move
			int[][][] futureLegalMoves = new int[N_PIECES][][];
			for(int j = 0; j < N_PIECES; j++) {
				int n = 0;
				for(int k = 0; k < allPOrients[j]; k++) {
						//number of locations in this orientation
						n += COLS+1-allPWidth[j][k];
				}
				//allocate space
				futureLegalMoves[j] = new int[n][2];
				//for each orientation
				n = 0;
				for(int k = 0; k < allPOrients[j]; k++) {
					//for each slot
					for(int l = 0; l < COLS+1-allPWidth[j][k];l++) {
						futureLegalMoves[j][n][ORIENT] = k;
						futureLegalMoves[j][n][SLOT] = l;
						n++;
					}
				}
			}
			// ArrayList<Double> futureScoreList = new ArrayList<Double>();
			// double [] futureScoreArray = new double[futureLegalMoves.length];

			for(int j = 0; j < N_PIECES; j++) {
				int futurePiece = j;
				int [][] futureLegalMovesSet = futureLegalMoves[futurePiece];
				//Clone the state and determine one state ahead
				// System.out.println(futureLegalMovesSet.length);
				for(int p = 0; p < futureLegalMovesSet.length; p++) {
					int [][] futureState = new int[ROWS][COLS];

					for(int a = 0; a < nextState.length; a++) {
		    			futureState[a] = nextState[a].clone();
					}

					int [] futureTopClone = new int[top.length];
					futureTopClone = nextTop.clone();

					futureState = tryMove(turn+1, futureTopClone, allPHeight, allPWidth, allPTop, allPBottom, futurePiece, futureState, futureLegalMovesSet[p][0], futureLegalMovesSet[p][1], false);
					// System.out.println("FUTURE STATE: ");
		 		 // printField(futureState);
					futureScoreList.add(ai.calculateScore(futureState));
				}
				// System.out.println("futureScoreList");
				// System.out.println(futureScoreList.size());
				// for (int o = 0; o < futureScoreList.size(); o++) {
     		// System.out.println(futureScoreList.get(o));
	 			// }
			}
			// System.out.println("futureScoreList");
			// System.out.println(futureScoreList.size());
			double sum = 0;
			double average;
			for (int o = 0; o < futureScoreList.size(); o++) {
				sum += futureScoreList.get(o);
			}
			average = sum / futureScoreList.size();

			futureScoreList.clear();
			//Calculate current score for the state
			scoreArray[i] = average;
		//	scoreArray[i] = ai.calculateScore(nextState);
		}

		// System.out.println("futureScoreList");
		// System.out.println(futureScoreList.size());
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
	//This method is to print the current field
	public void printField(int [][] field) {
		int [][] printField = new int [ROWS][COLS];
		for(int i = 0; i < printField.length; i++) {
			printField[i] = field[field.length - i - 1];
		}
		System.out.println("Print Field");
		System.out.println(Arrays.deepToString(printField).replace("], ", "]\n"));
	}
	//This method is taken from the State.java class
	public int[][] tryMove(int turn, int[] top, int[][] pHeight, int[][]pWidth, int[][][] pTop, int[][][] pBottom, int nextPiece, int [][] field, int orient, int slot, boolean update) {
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

		if(update) {
			nextTop = top;
		}
		return field;
	}

	public static int playgame(Ai ai) {
		State s = new State();
		//new TFrame(s);
		PlayerSkeleton p = new PlayerSkeleton();
		while(!s.hasLost()) {
			s.makeMove(p.pickMove(s,s.legalMoves(), ai));
			//s.draw();
			//s.drawNext(0,0);

			try {
				Thread.sleep(0);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
		//System.out.println("You have completed "+ s.getRowsCleared() +" rows.");
		return s.getRowsCleared();
	}

	public static void main(String[] args) {
		PlayerSkeleton skele = new PlayerSkeleton();

		//skele.ai = skele.new Ai();
		//playgame(skele.ai);

		/*
		AiTrainer trainer = skele.new AiTrainer();
		trainer.startTrain(50);
		*/

		// AiGeneticTrainer trainer = skele.new AiGeneticTrainer();
		// trainer.startTrain(5000);
		State s = new State();
		new TFrame(s);
		PlayerSkeleton p = new PlayerSkeleton();
		while(!s.hasLost()) {
			s.makeMove(p.pickMove(s,s.legalMoves(),p.ai));
			s.draw();
			s.drawNext(0,0);
			try {
				Thread.sleep(30);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
		System.out.println("You have completed "+s.getRowsCleared()+" rows.");
	}

	public class Ai {
		public double uniqueID;
		public double totalHeightWeight;
		public double maxHeightWeight;
		public double relativeHeightWeight;
		public double linesCompletedWeight;
		public double holesWeight;
		public double absTotalDifferenceHeightWeight;

		public Integer score;

		//Creating an AI with hard coded weight. After running a learning program copy the result here for actual playing
		public Ai() {
			score = 0;

			uniqueID = Math.random();
			totalHeightWeight = -0.01693979306575355;
			maxHeightWeight = 0.12307879867607197;
			relativeHeightWeight = -0.06086013817658331;
			linesCompletedWeight = -0.3529339413572512;
			holesWeight = -0.3695737041508783;
			absTotalDifferenceHeightWeight = -0.07921627617713911;
		}

		//Creating an AI with given weights mainly for learning process.
		public Ai(double a, double b, double c, double d, double e, double f) {
			score = 0;

			this.uniqueID = Math.random();
			this.totalHeightWeight = a;
			this.maxHeightWeight = b;
			this.relativeHeightWeight = c;
			this.linesCompletedWeight = d;
			this.holesWeight = e;
			this.absTotalDifferenceHeightWeight = f;
		}

		//This method is to calculate the score for the next state
		public double calculateScore(int[][] nextState) {

			//Calculate Total Height
			int totalHeight = checkHeight(nextState);

			//Calculate Max Height
			int maxHeight = calculateMaxHeight(nextState);

			//Calculate Relative Height
			int relativeHeight = calculateRelativeHeight(nextState);

			//Calculate number of Completed Lines in the state
			int numLines = completedLines(nextState);

			//Calculate number of holes in the grid
			int numHoles = calculateHoles(nextState);

			//Calculate absolute height difference
			int absTotalHeightDiff = calculateAbsDiff(nextState);

			return totalHeight * this.totalHeightWeight + maxHeight * this.maxHeightWeight +
				+ relativeHeight * this.relativeHeightWeight + numLines * this.linesCompletedWeight +
					numHoles * this.holesWeight + absTotalHeightDiff * this.absTotalDifferenceHeightWeight;
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

		public int calculateRelativeHeight(int[][] nextState) {
			int max = 0;
			int min = 0;
			int currentColHeight = 0;
			for(int i = 0; i < State.COLS; i++) {
				currentColHeight = getColHeight(nextState, i);
				if(currentColHeight > max) {
					max = currentColHeight;
				}
				if(i==0) min = currentColHeight;
				if(currentColHeight < min) {
					min = currentColHeight;
				}
			}
			return max - min;
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

		public int calculateAbsDiff(int[][] nextState) {
			int value = 0;
			for(int i = 0; i < State.COLS - 1; i++) {
				value += Math.abs(getColHeight(nextState, i) - getColHeight(nextState, i+1));
			}
			return value;
		}

	}

	//Trains an Ai
	public class AiGeneticTrainer {
		public AiGeneticTrainer() {
		}

		public double randomWeight() {
			return Math.random()-0.5; //*2 - 1
		}

		//Function for training AI
		public void startTrain(int count) {
			Comparator<Ai> aiSorter = new Comparator<Ai>() {
				public int compare(Ai a1, Ai a2) {
					return (a1.score).compareTo(a2.score);
				};
			};

			//Creates a population of 100 AI
			Ai[] generation = new Ai[100];

			for (int i = 0; i < 100; i++) {
				generation[i] = getRandomAi();
			}

			//Let's all of them play
			for (int i = 0; i < 100; i++) {
				generation[i].score = playgame(generation[i]);
				for(int j = 0; j < 5; j++) { // try 5 times
					int secondTry = playgame(generation[i]);
					if (secondTry > generation[i].score) {
						generation[i].score = secondTry;
					}
				}
			}

			int repeat = 0;
			while (true) {
				System.out.println(" ");
				System.out.println("Generation number: " + repeat);
				System.out.println(" ");

				//Create 50 children
				Ai[] nextgen = new Ai[50];
				//Keep the winning candidate no matter what
				nextgen[0] = generation[99];
				//Randomly select 10 AI from population to have a tournament to see which among the 10 is the best and the 2 best would be used to make children
				//Do this for 49 children
				for (int i = 1; i < 50; i++) {
					Ai[] tourney = selector(generation);
					Arrays.sort(tourney, aiSorter);
					nextgen[i] = mergeAi(tourney[9], tourney[8]);
				}

				Arrays.sort(generation, aiSorter);

				printScore(generation);

				//the 50 worst Ai within the population would get replaced by children
				for (int i = 0; i < 50; i++) {
					generation[i] = nextgen[i];
				}

				//play again to get the score for this new population
				// for (int i = 0; i < 100; i++) {
				// 	generation[i].score = playgame(generation[i]);
				// 	int secondTry = playgame(generation[i]);
				// 	if (secondTry > generation[i].score) {
				// 		generation[i].score = secondTry;
				// 	}
				// }
				for (int i = 0; i < 100; i++) {
					generation[i].score = playgame(generation[i]);
					for(int j = 0; j < 5; j++) { // try 5 times
						int secondTry = playgame(generation[i]);
						if (secondTry > generation[i].score) {
							generation[i].score = secondTry;
						}
					}
				}

				repeat++;
			}
		}

		public void printScore(Ai[] toprint) {
			for (int i = 90; i < 100; i++) {
				System.out.println(toprint[i].score + " Agent ID = " + toprint[i].uniqueID);
			}
			System.out.println(" ");

			System.out.println("ID for top AI is: " + toprint[99].uniqueID
			+ "\ntotalHeightWeight = " + toprint[99].totalHeightWeight
			+ "\nmaxHeightWeight = " + toprint[99].maxHeightWeight
			+ "\nrelativeHeightWeight = " + toprint[99].relativeHeightWeight
			+ "\nlinesCompletedWeight = " + toprint[99].linesCompletedWeight
			+ "\nholesWeight = " + toprint[99].holesWeight
			+ "\nabsTotalDifferenceHeightWeight = " + toprint[99].absTotalDifferenceHeightWeight);
		}

		//Creates a child Ai where the values are based off parents
		//There is a 5% chance that 1 of the herustic would be mutated and become random
		//Otherwise it takes the value in between the 2 parent leaning slightly to the better one.
		// public Ai mergeAi(Ai dad, Ai mom) {
		// 	return new Ai(mergeValue(dad.totalHeightWeight, mom.totalHeightWeight, dad.score, mom.score),
		// 			mergeValue(dad.maxHeightWeight, mom.maxHeightWeight, dad.score, mom.score),
		// 			mergeValue(dad.linesCompletedWeight, mom.linesCompletedWeight, dad.score, mom.score),
		// 			mergeValue(dad.holesWeight, mom.holesWeight, dad.score, mom.score),
		// 			mergeValue(dad.absTotalDifferenceHeightWeight, mom.absTotalDifferenceHeightWeight, dad.score, mom.score));
		// }

		// public double mergeValue(double x, double y, double scorex, double scorey) {
		// 	double mutate = Math.random();
		// 	if (mutate > 0.95) {
		// 		return randomWeight();
		// 	}

		// 	double lean = ((scorex - scorey)/scorex)/2 + 0.5;
		// 	return x*lean + y*(1-lean);
		// }

		//Alternate merging which picks either mom or dad value with 50 /50 chance
		public Ai mergeAi(Ai dad, Ai mom) {
			return new Ai(mergeValue(dad.totalHeightWeight, mom.totalHeightWeight),
					mergeValue(dad.maxHeightWeight, mom.maxHeightWeight),
					mergeValue(dad.relativeHeightWeight, mom.relativeHeightWeight),
					mergeValue(dad.linesCompletedWeight, mom.linesCompletedWeight),
					mergeValue(dad.holesWeight, mom.holesWeight),
					mergeValue(dad.absTotalDifferenceHeightWeight, mom.absTotalDifferenceHeightWeight));
		}

		public double mergeValue(double x, double y) {
			double mutate = Math.random();
			if (mutate < 0.1) { //10% chance to mutate
				return randomWeight();
			}

			return (Math.random() >= 0.5) ? x : y ;
		}

		public Ai getRandomAi() {
			return new Ai(randomWeight(), randomWeight(), randomWeight(), randomWeight(), randomWeight(), randomWeight());
		}

		//Selects 10 Ai out of the given population of 100
		public Ai[] selector(Ai[] population) {
			ArrayList<Object> list = new ArrayList<>();
			Ai[] picked = new Ai[10];
			for (int i = 0; i < 10; i++) {
				int rng = new Random().nextInt(50) + 50;
				//int rng = (int) Math.floor(Math.random()*100);
				while (list.contains(rng)) {
					//rng =  (int) Math.floor(Math.random()*100);
					rng = new Random().nextInt(50) + 50;
				}
				list.add(rng);
				picked[i] = population[rng];
			}
			return picked;
		}
	}
}
