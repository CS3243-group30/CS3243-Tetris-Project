import java.util.Arrays;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Random;

public class PlayerSkeleton {
	public static final int COLS = State.COLS;
	public static final int ROWS = State.ROWS;

	Ai ai = new Ai();

	//implement this function to have a working system
	public int pickMove(State s, int[][] legalMoves, Ai ai) {
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
			//System.out.println("Game Over State");
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

		//printField(field);

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
		//p.ai.trainAi(); //Comment this out to let it play normally.
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

		public double totalHeightWeight = -0.615272;
		public double maxHeightWeight = -0.0867;
		public double relativeHeightWeight = 0.02834;
		public double linesCompletedWeight = 0.410681;
		public double holesWeight = -0.174377;
		public double absTotalDifferenceHeightWeight = -0.177204;

		public Ai() {}

		public Ai(double totalHeightWeight, double maxHeightWeight, double relativeHeightWeight, double linesCompletedWeight, double holesWeight, double absTotalDifferenceHeightWeight) {
			this.totalHeightWeight = totalHeightWeight;
			this.maxHeightWeight = maxHeightWeight;
			this.relativeHeightWeight = relativeHeightWeight;
			this.linesCompletedWeight = linesCompletedWeight;
			this.holesWeight = holesWeight;
			this.absTotalDifferenceHeightWeight = absTotalDifferenceHeightWeight;
		}
	
		private void trainAi() {
			ArrayList<AiCandidate> candidates = new ArrayList<AiCandidate>();

			//Generate 50 random candidates
			for(int i = 0; i < 50; i++){
				AiCandidate candidate = new AiCandidate();
				candidate.normalize();
			    candidates.add(candidate);
			}

			System.out.println("In Training");
			calculateFitness(candidates, 5, 1000);
			sortCandidates(candidates);
			System.out.println("FITTEST Value = " + candidates.get(0).fitness);

			while(true) {
				ArrayList<AiCandidate> childCandidates = new ArrayList<AiCandidate>();
				candidates.subList((candidates.size()- 25), candidates.size()).clear(); //delete 25 of the lowest performance
				childCandidates.add(candidates.get(0)); //add best performing candidate first

				for(int i = 0; i < 49; i++){ 
					AiCandidate fittest = tournamentSelection(candidates, 10); //choose 5 randomly, then choose the best
					AiCandidate secondFittest = tournamentSelection(candidates, 10);
					while(fittest.fitness == secondFittest.fitness) { //if equal fitness
						secondFittest = tournamentSelection(candidates, 10);
					}
					AiCandidate offspring = crossover(fittest, secondFittest);
					if(Math.random() < 0.1){// 10% chance of mutation
	                    offspring.mutation();
					}
					childCandidates.add(offspring);
				}
				calculateFitness(childCandidates, 5, 1000);
				replacePopulation(candidates, childCandidates);
				System.out.println("FITTEST Value = " + candidates.get(0).fitness);
				System.out.println("totalHeightWeight = " + candidates.get(0).totalHeightWeight);
				System.out.println("maxHeightWeight = " + candidates.get(0).maxHeightWeight);
				System.out.println("relativeHeightWeight = " + candidates.get(0).relativeHeightWeight);
				System.out.println("linesCompletedWeight = " + candidates.get(0).linesCompletedWeight);
				System.out.println("holesWeight = " + candidates.get(0).holesWeight);
				System.out.println("absTotalDifferenceHeightWeight = " + candidates.get(0).absTotalDifferenceHeightWeight);
			}
		}

		private void sortCandidates(ArrayList<AiCandidate> candidates) {
			Collections.sort(candidates, new CandidateFitnessComparator());
		}

		private void calculateFitness(ArrayList<AiCandidate> candidates, int numGamesToPlay, int maxMoves) {
			for(int i = 1; i < candidates.size(); i++){
				AiCandidate candidate = candidates.get(i);
				Ai testAi = new Ai(candidate.totalHeightWeight, candidate.maxHeightWeight, candidate.relativeHeightWeight,
					candidate.linesCompletedWeight, candidate.holesWeight, candidate.absTotalDifferenceHeightWeight);
				int totalScore = 0;

				//play 5 games
				for(int j = 0; j < numGamesToPlay; j++) {
					int score = 0;
					int numMoves = 0;
					State s = new State();
					//TFrame frame = new TFrame(s);
					PlayerSkeleton p = new PlayerSkeleton();
					while(!s.hasLost()) { // && numMoves != maxMoves
						s.makeMove(p.pickMove(s,s.legalMoves(),testAi));
						numMoves++;
						//s.draw();
						//s.drawNext(0,0);
						try {
							Thread.sleep(0);
						} catch (InterruptedException e) {
							e.printStackTrace();
						}
					}
					score = s.getRowsCleared();
					totalScore += score;
					//frame.dispose();
				}
				System.out.println("Fitness Value = " + totalScore + " Agent: " + i);
				candidates.get(i).fitness = totalScore;
			}
		}

		private void replacePopulation(ArrayList<AiCandidate> candidates, ArrayList<AiCandidate> childCandidates) {
			candidates.clear();
			for(int i = 0; i < childCandidates.size(); i++){
	            candidates.add(childCandidates.get(i));
	        }
			sortCandidates(candidates);
		}

		private AiCandidate tournamentSelection(ArrayList<AiCandidate> candidates, int k) {
			Random random = new Random();
			AiCandidate best = null;
			AiCandidate individual = new AiCandidate();
			for(int i = 0; i < k; i++) {
				individual = candidates.get(random.nextInt(candidates.size()));
				if(best == null || (individual.fitness > best.fitness)) {
					best = individual;
				}
			}
			//System.out.println("Winning Fitness = " + best.fitness);
			return best;
		}

		private AiCandidate crossover(AiCandidate candidate1, AiCandidate candidate2) {
			AiCandidate offspring = new AiCandidate();

			// offspring.totalHeightWeight = candidate1.fitness * candidate1.totalHeightWeight + candidate2.fitness * candidate2.totalHeightWeight;
			// offspring.maxHeightWeight = candidate1.fitness * candidate1.maxHeightWeight + candidate2.fitness * candidate2.maxHeightWeight;
			// offspring.linesCompletedWeight = candidate1.fitness * candidate1.linesCompletedWeight + candidate2.fitness * candidate2.linesCompletedWeight;
			// offspring.holesWeight = candidate1.fitness * candidate1.holesWeight + candidate2.fitness * candidate2.holesWeight;
			// offspring.absTotalDifferenceHeightWeight = candidate1.fitness * candidate1.absTotalDifferenceHeightWeight + candidate2.fitness * candidate2.absTotalDifferenceHeightWeight;

			offspring.totalHeightWeight = (Math.random() >= 0.5) ? candidate1.totalHeightWeight : candidate2.totalHeightWeight;
			offspring.maxHeightWeight = (Math.random() >= 0.5) ? candidate1.maxHeightWeight : candidate2.maxHeightWeight;
			offspring.relativeHeightWeight = (Math.random() >= 0.5) ? candidate1.relativeHeightWeight : candidate2.relativeHeightWeight;
			offspring.linesCompletedWeight = (Math.random() >= 0.5) ? candidate1.linesCompletedWeight : candidate2.linesCompletedWeight;
			offspring.holesWeight = (Math.random() >= 0.5) ? candidate1.holesWeight : candidate2.holesWeight;
			offspring.absTotalDifferenceHeightWeight = (Math.random() >= 0.5) ? candidate1.absTotalDifferenceHeightWeight : candidate2.absTotalDifferenceHeightWeight;
			offspring.normalize();

			return offspring;
		}
	
		//This method is to calculate the score for the next state
		public double calculateScore(int[][] nextState) {
			double score = 0;
	
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

		public int calculateRelativeHeight(int[][] nextState) {
			int max = 0;
			int min = 0;
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
			//System.out.print("Number of holes: ");
			//System.out.println(numHoles);
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
			for(; count < State.ROWS && nextState[count][column] == 1; count++);
			return count;
		}
		
	}

	public class AiCandidate {
		public double totalHeightWeight;
		public double maxHeightWeight;
		public double relativeHeightWeight;
		public double linesCompletedWeight;
		public double holesWeight;
		public double absTotalDifferenceHeightWeight;
		public double fitness;

		public AiCandidate() {
			this.totalHeightWeight = Math.random() - 0.5;
			this.maxHeightWeight = Math.random() - 0.5;
			this.relativeHeightWeight = Math.random() - 0.5;
			this.linesCompletedWeight = Math.random() - 0.5;
			this.holesWeight = Math.random() - 0.5;
			this.absTotalDifferenceHeightWeight = Math.random() - 0.5;
			this.fitness = 0.0;
		}

		private void normalize() {
			double norm = Math.sqrt(this.totalHeightWeight * this.totalHeightWeight + this.maxHeightWeight * this.maxHeightWeight 
			+ this.relativeHeightWeight * this.relativeHeightWeight + this.linesCompletedWeight * this.linesCompletedWeight 
			+ this.holesWeight * this.holesWeight + this.absTotalDifferenceHeightWeight * this.absTotalDifferenceHeightWeight);

			this.totalHeightWeight /= norm;
			this.maxHeightWeight /= norm;
			this.relativeHeightWeight /= norm;
			this.linesCompletedWeight /= norm;
			this.holesWeight /= norm;
			this.absTotalDifferenceHeightWeight /= norm;
		}

		private void mutation() {
			Random random = new Random();
			double valueToMutate = Math.random() * 0.2 * 2 - 0.2;
			switch(random.nextInt(6)){
            case 0:
                this.totalHeightWeight += valueToMutate;
                break;
            case 1:
                this.maxHeightWeight += valueToMutate;
                break;
            case 2:
                this.maxHeightWeight += valueToMutate;
                break;
            case 3:
                this.relativeHeightWeight += valueToMutate;
                break;
            case 4:
                this.holesWeight += valueToMutate;
                break;
            case 5:
                this.absTotalDifferenceHeightWeight += valueToMutate;
                break;
			}
		}
	}

	class CandidateFitnessComparator implements Comparator<AiCandidate> {
	    public int compare(AiCandidate candidate1, AiCandidate candidate2) {
	        if(candidate1.fitness < candidate2.fitness) return 1;
	        if(candidate1.fitness > candidate2.fitness) return -1;
	        return 0;
    	}
	}
}