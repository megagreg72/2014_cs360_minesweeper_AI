package brian;

/* Copyright (C) 1995 John D. Ramsdell

This file is part of Programmer's Minesweeper (PGMS).

PGMS is free software; you can redistribute it and/or modify
it under the terms of the GNU General Public License as published by
the Free Software Foundation; either version 2, or (at your option)
any later version.

PGMS is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU General Public License for more details.

You should have received a copy of the GNU General Public License
along with PGMS; see the file COPYING.  If not, write to
the Free Software Foundation, 59 Temple Place - Suite 330,
Boston, MA 02111-1307, USA.
*/

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Queue;
import java.util.Set;

//import map.*;








import map.Map;
import map.Strategy;

import com.google.common.collect.*;

/**
 * Maintain a list of updated squares, to consider probing or flagging.
 * Maintain a multi-map of squares to constraints.
 * 
 * When a square is probed, a constraint is added.
 * Try to simplify constraints with the new constraint.
 * Use the multi-map to check for overlapping constraints.
 * Then queue the neighbors of the new constraint for consideration.
 * 
 * When a permanent flag is added, the corresponding constraints from
 * the multi-map are simplified. Discard empty constraints.
 * Then queue elements of these constraints.
 * 
 * When popping from the :
 * Flag the square.
 * If the flag violates a constraint, it is safe to probe it.
 * Else, just remove the flag.
 * Safe-flag the square.
 * If the safe-flag violates a constraint, it should be permanently flagged.
 * Else, just remove the safe-flag.
 * 
 * If and when the queue is empty, begin educated guessing (Step 3 from assignment pdf).
 * 
 * 
 * @see Strategy
 * @see set.Set
 * @version October 1995
 * @author John D. Ramsdell
 */
public final class SmartGuessStrategy implements Strategy {
	
	private boolean debug = false;
	// largest frontier allowed for SmartGuess calculation
	private int maxFrontier = 30;
	
	// when removing from constraints, also remove all pointers to it in sToC
	private Multimap<Square, SumConstraint> sToC;
	private List<SumConstraint> constraints;
	
	// treat these two as one combined datatype, a unique queue
	private Queue<Square> updatedSquares;
	private Set<Square> updatedSquareSet;
	
	// treat these two as one combined datatype, a unique queue
	private Queue<SumConstraint> updatedConstraints;
	private Set<SumConstraint> updatedConstraintSet;
	
	// keep track of guesses(flags) and constraints
	private List<ArrayList<Square>> myMap = new ArrayList<ArrayList<Square>>();
	private int marked;
	private int probed;
	private double bombDensity;
	private boolean bombDensityIsDirty;
	
	private void initGlobals(){
		// when removing from constraints, also remove all pointers to it in sToC
		sToC = LinkedHashMultimap.create();
		constraints = new ArrayList<SumConstraint>();
		
		updatedSquares = new LinkedList<Square>();
		updatedSquareSet = new HashSet<Square>();
		
		updatedConstraints = new LinkedList<SumConstraint>();
		updatedConstraintSet = new HashSet<SumConstraint>();
		bombDensityIsDirty = true;
		marked = 0;
		probed = 0;
	}
	
	// create 2D array of Squares
	private void initMyMap(Map m){
		myMap = new ArrayList<ArrayList<Square>>();
		int r = m.rows();
		int c = m.columns();
		for(int i = 0; i < c; i++){
			ArrayList<Square> col = new ArrayList<Square>();
			for(int j = 0; j < r; j++){
				col.add(new Square(i,j));
			}
			myMap.add(col);
		}
	}
	
	public void play(Map m) {
		initGlobals();
		initMyMap(m);
		while(!m.done()){
			if(debug){
				printSquareQueue();
				printConstraintQueue();
			}
			if(!updatedSquares.isEmpty())
			{
				Square s = updatedSquares.poll();
				checkConstraints(m, s);
				updatedSquareSet.remove(s);
			}
			else if(!updatedConstraints.isEmpty()){
				SumConstraint c = updatedConstraints.poll();
				simplifyConstraints(c);
				updatedConstraintSet.remove(c);
			}
			else{
				if(debug){
					System.out.println("Guessing!");
				}
				probeSafestSquare(m);
				//probeRandomSquare(m);
			}
		}
		if(debug){
			System.out.println("**************Game Over****************");
			printSquareQueue();
			printConstraintQueue();
			printConstraints();
			printSToC();
		}
	}
	
	// assume it is a bomb, or assume it is not a bomb
	// then, check for broken constraint i.e. a contradiction
	private Set<SumConstraint> getContradictions(Map m, Square s, boolean isBomb){
		Collection<SumConstraint> cs = sToC.get(s);
		s.setFlagged(isBomb);
		s.setSafeFlagged(!isBomb);
		Set<SumConstraint> violated = new HashSet<SumConstraint>();
		for(SumConstraint c : cs){
			if(c.isViolated()){
				violated.add(c);
				if(debug){
//					System.out.println("Violated: " + c);
				}
			}
		}
		// end assumptions
		s.setFlagged(false);
		s.setSafeFlagged(false);
		return violated;
	}
	
	private void checkConstraints(Map m, Square s){
		Collection<SumConstraint> cs = sToC.get(s);
		
		if(m.look(s.getX(), s.getY()) != Map.MARKED){
			// temporarily assume it is a bomb
			Set<SumConstraint> violated = getContradictions(m,s,true);
			// contradiction ==> safe
			if(violated.size() > 0){
				Probe(m, s.getX(), s.getY());
			}
			
			// temporarily assume it is not a bomb
			violated = getContradictions(m,s,false);
			// contradiction ==> permanent flag
			if(violated.size() > 0){
				// update constraints
				s.setFlagged(true);
				m.mark(s.getX(), s.getY());
				Flagged(m, s.getX(), s.getY());
			}
			// end assumption
			s.setSafeFlagged(false);
		}
	}
	
	private Set<Square> getNeighbors(Map m, int x, int y){
		Set<Square> neighbors = new HashSet<Square>();
		for(int i = x-1; i <= x+1; i++){
			for(int j = y-1; j <= y+1; j++){
				if(m.look(i, j) == Map.UNPROBED
						|| m.look(i, j) == Map.MARKED){
					Square s = myMap.get(i).get(j);
					//System.out.println(s);
					neighbors.add(s);
				}
			}
		}
		return neighbors;
	}
	
	private void printConstraints(){
		System.out.println("-------------Constraints---------------------");
		for(SumConstraint c : constraints){
			System.out.println(c);
		}
	}
	
	private void printSToC(){
		System.out.println("-------------SToC---------------------");
		Set<Square> keys = new HashSet<Square>(sToC.keys());
		for(Square s : keys){
			System.out.println("---S(" + s.getX() + "," + s.getY() + "):---");
			for(SumConstraint c : sToC.get(s)){
				System.out.println(c);
			}
			System.out.println("--------");
		}
	}
	
	private void printSquareQueue(){
		System.out.println("-----------------Square Queue-------------------------");
		for(Square s : updatedSquares){
			System.out.println(s);
		}
	}
	
	private void printConstraintQueue(){
		System.out.println("-----------------Constraint Queue-------------------------");
		for(SumConstraint s : updatedConstraints){
			System.out.println(s);
		}
	}
	
	private void addConstraint(Map m, SumConstraint c){
		constraints.add(c);
		Set<Square> bombs = new HashSet<Square>();
		for(Square n : c.vars){
			// immediately simplify if one or more is flagged
			if(m.look(n.getX(), n.getY()) == Map.MARKED){
				bombs.add(n);
			}
			else{
				sToC.put(n, c);
				if(updatedSquareSet.add(n)){
					updatedSquares.add(n);
				}
			}
		}
		trySubstitution(c, bombs, bombs.size());
		if(updatedConstraintSet.add(c)){
			updatedConstraints.add(c);
		}
	}
	
	// only called on empty constraints
	private void removeConstraint(SumConstraint c){
		for(Square n : c.vars){
			System.out.println("oops2");
			sToC.remove(n, c);
			if(updatedSquareSet.add(n)){
				updatedSquares.add(n);
			}
		}
		constraints.remove(c);
	}
	
	private int Probe(Map m, int x, int y){
		if(debug){
			System.out.println("Probing " + x + ", " + y);
		}
		//printConstraints();
		//printSquareQueue();
		//printConstraintQueue();
		// add constraint
		int q = m.probe(x, y);
		if(q >= 0){
			int sum = q;
			Set<Square> neighbors = getNeighbors(m, x, y);
			SumConstraint c = new SumConstraint(neighbors, sum);
			addConstraint(m, c);
			probed++;
			bombDensityIsDirty = true;
		}
		// remove old square
		Square s = myMap.get(x).get(y);
		for(SumConstraint c : sToC.get(s)){
			Set<Square> l = new HashSet<Square>();
			l.add(s);
			// substitute a single square for a non-bomb
			trySubstitution(c, l, 0);
			if(c.maxSum < 0){
				System.out.println("mis-shrunk: " + c);
				System.out.println("oops0.0");
			}
		}
		sToC.removeAll(s);
		return q;
	}
	
	private void Flagged(Map m, int x, int y){
		// remove square
		Square s = myMap.get(x).get(y);
		Collection<SumConstraint> constraintsToUpdate = sToC.get(s);
		for(SumConstraint c : constraintsToUpdate){
			Set<Square> l = new HashSet<Square>();
			l.add(s);
			// substitute a single square for a bomb
			trySubstitution(c, l, 1);
			if(c.maxSum < 0){
				System.out.println("mis-shrunk: " + c);
				System.out.println("oops0.1");
			}
		}
		marked++;
		bombDensityIsDirty = true;
		sToC.removeAll(s);
	}
	
	public void simplifyConstraints(SumConstraint new_c){
		// lazy removal
		if(new_c.vars.isEmpty()){
			removeConstraint(new_c);
			return;
		}
		if(debug){
			System.out.println("Simplifying: " + new_c);
		}
		for(SumConstraint c : constraints){
			// substitute
			if(new_c.equals(c)){
				if(debug){
					System.out.println("its me!");
				}
			}
			else{
				trySubstitution(c, new_c.vars, new_c.maxSum);
				if(c.maxSum < 0){
					System.out.println("mis-shrunk: " + c);
					System.out.println("oops0.2");
				}
			}
		}
	}
	
	/*
	 * returns true if the constraint is now empty
	 */
	private boolean trySubstitution(SumConstraint c, Set<Square> ss, int numMines){
		//if(!ss.equals(c.vars)){
		boolean success = false;
		if(c.vars.containsAll(ss)){
			success = true;
			if(debug){
				System.out.println("Shrinking: " + c + " by " + ss + ", subtracting " + numMines);
			}
			for(Square s : ss){
				c.vars.remove(s);
				//sToC.remove(s, c);
			}
			c.maxSum = c.maxSum-numMines;
			c.minSum = c.minSum-numMines;
			if(c.maxSum < 0){
				System.out.println("mis-shrunk: " + c);
			}
			else{
				if(updatedConstraintSet.add(c)){
					updatedConstraints.add(c);
				}
				for(Square s : c.vars){
					if(updatedSquareSet.add(s)){
						updatedSquares.add(s);
					}
				}
			}
		}
		return success;
	}
	
	private void calcBombDensity(Map m){
		// remaining unmarked bombs
		int b = m.mines_minus_marks();
		// remaining uprobed squares
		int r = m.rows();
		int c = m.columns();
		int mapSize = r*c;
		int n = (mapSize - marked) - probed;
		double p = (double)b/(double)n;
		
		bombDensity = p;
		bombDensityIsDirty = false;
	}
	
	private double getMapBombDensity(Map m){
		if(bombDensityIsDirty){
			calcBombDensity(m);
			return bombDensity;
		}
		else{
			return bombDensity;
		}
	}
	
	// get rough probability without enumerating over all possible board states
	private double getBombProbability(Map m, Square s){
		double mp = getMapBombDensity(m);
		Collection<SumConstraint> cs = sToC.get(s);
		double cp = 0.0;
		for (SumConstraint c : cs){
			int b = c.maxSum;
			int n = c.vars.size();
			double density = (double)b/(double)n;
			if(density > cp){
				cp = density;
			}
		}
		if(!cs.isEmpty()){
			if(cp == 0.0){
				System.out.println("whoops");
			}
			return cp;
		}
		else{
			return mp;
		}
	}
	
	// return success (i.e. did you blow up)
		private boolean fastGuess(Map m){
			double safestP = 9999.0;
			Square safestS = null;
			for(int i = 0; i < m.columns(); i++){
				for(int j = 0; j < m.rows(); j++){
					Square s = myMap.get(i).get(j);
					if(m.look(i, j) == Map.UNPROBED){
						double ps = getBombProbability(m, s);
						if(ps > 1.0){
							System.out.println("whoops");
						}
						if(ps < safestP){
							safestP = ps;
							safestS = s;
						}
						// use centrality as tie-breaker
						else if(ps == safestP){
							if(distToCenter(m,i,j) > distToCenter(m, safestS.getX(), safestS.getY())){
								safestS = s;
							}
						}
					}
				}
			}
			if(safestS == null){
				System.out.println("No legal guess!");
				return false;
			}
			else{
				Collection<SumConstraint> cs = sToC.get(safestS);
				if(debug){
					printSToC();
					System.out.println("Guessing at " + safestS.getX() + "," + safestS.getY()
						+ " with p=" + safestP);
					System.out.println("Guessed Square has constraints:\n" + cs);
				}
				
				int q = Probe(m, safestS.getX(), safestS.getY());
			    if (Map.BOOM == q){
			    	return false;
			    }
			    else if(q >= 0){
			    	return true;
			    }
			    else{
			    	if(debug){
			    		System.out.println("illegal return from Probe!");
			    	}
			    	return false;
			    }
			}
		}
	
	// centrality = manhattan distance to center
	private double distToCenter(Map m, int x, int y){
		int minX = 0;
		int maxX = m.columns() - 1;
		double midX = (minX + maxX)/2.0;
		
		int minY = 0;
		int maxY = m.rows() - 1;
		double midY = (minY + maxY)/2.0;
		
		double dx = Math.abs(x-midX);
		double dy = Math.abs(y-midY);
		double d = dx+dy;
		return d;
	}
	
	// recursion invariant: Squares to the left of "index" have been resolved
	private int countStates(Map m, int index, int remainingBombs, List<Square> toResolve, int[] bombStateCounts, int[] emptyStateCounts){
		if(toResolve.size() == 0){
			return 0;
		}
		Square s = toResolve.get(index);
		boolean bombAllowed = (getContradictions(m,s,true).size() == 0) && remainingBombs > 0;
		boolean emptyAllowed = getContradictions(m,s,false).size() == 0;
		int bombStates = 0;
		int emptyStates = 0;
		if(toResolve.size() > index+1){
			if(bombAllowed){
				s.setFlagged(true);
				bombStates = countStates(m, index+1, remainingBombs-1, toResolve, bombStateCounts, emptyStateCounts);
				s.setFlagged(false);
			}
			if(emptyAllowed){
				s.setSafeFlagged(true);
				emptyStates = countStates(m, index+1, remainingBombs, toResolve, bombStateCounts, emptyStateCounts);
				s.setSafeFlagged(false);
			}
		}
		else{
			if(bombAllowed){
				bombStates = 1;
			}
			if(emptyAllowed){
				emptyStates = 1;
			}
		}
		// persist state counts for this square
		bombStateCounts[index] += bombStates;
		emptyStateCounts[index] += emptyStates;
		return bombStates + emptyStates;
	}
	
	// return success (i.e. did you blow up)
	private boolean probeSafestSquare(Map m){
		// make sure frontier only contains unique Squares
		Set<Square> frontierSet = new HashSet<Square>(sToC.keySet());
		
		if(frontierSet.size() > maxFrontier){
			return fastGuess(m);
		}
		else{
			Set<Square> nonFrontierSet = new HashSet<Square>();
			for(int i = 0; i < m.columns(); i++){
				for(int j = 0; j < m.rows(); j++){
					Square s = myMap.get(i).get(j);
					if(!frontierSet.contains(s)){
						if(m.look(s.getX(), s.getY()) == Map.UNPROBED){
							nonFrontierSet.add(s);
						}
					}
				}
			}
			List<Square> frontier = new ArrayList<Square>(frontierSet);
			List<Square> nonFrontier = new ArrayList<Square>(nonFrontierSet);
			int remainingBombs = m.mines_minus_marks();
			
			int[] bombStateCounts = new int[frontier.size()];
			int[] emptyStateCounts = new int[frontier.size()];
			
			countStates(m, 0, remainingBombs, frontier, bombStateCounts, emptyStateCounts);
			
			long totalBombStates = 0;
			long totalEmptyStates = 0;
			for(int i = 0; i < frontier.size(); i++){
				totalBombStates += bombStateCounts[i];
				totalEmptyStates += emptyStateCounts[i];
			}
			double frontierDensity = (double)totalBombStates/(totalEmptyStates+totalBombStates);
			double expectedFrontierBombs;
			if(frontier.size() == 0){
				expectedFrontierBombs = 0;
			}
			else{
				expectedFrontierBombs = frontierDensity*frontier.size();
			}
			double expectedNonFrontierBombs = m.mines_minus_marks() - expectedFrontierBombs;
			double nonFrontierDensity = expectedNonFrontierBombs/nonFrontier.size();
			
			// find nonFrontier Square which is furthest from center
			Square bestNonFrontierS = null;
			double bestNonFrontierD = 0.0;
			for(int i = 0; i < nonFrontier.size(); i++){
				Square s = nonFrontier.get(i);
				double d = distToCenter(m, s.getX(), s.getY());
				if(d > bestNonFrontierD){
					bestNonFrontierD = d;
					bestNonFrontierS = s;
				}
			}
			
			double[] probs = new double[frontier.size()];
			for(int i = 0; i < frontier.size(); i++){
				probs[i] = (double)bombStateCounts[i]/(double)(emptyStateCounts[i]+bombStateCounts[i]);
			}
			
			double bestFrontierP = 9999.0;
			Square bestFrontierS = null;
			for(int i = 0; i < frontier.size(); i++){
				Square s = frontier.get(i);
				double ps = probs[i];
				if(ps > 1.0){
					System.out.println("whoops");
				}
				if(ps < bestFrontierP){
					bestFrontierP = ps;
					bestFrontierS = s;
				}
				// use centrality as tie-breaker
				else if(ps == bestFrontierP){
					if(distToCenter(m,s.getX(),s.getY()) > distToCenter(m, bestFrontierS.getX(), bestFrontierS.getY())){
						bestFrontierS = s;
					}
				}
			}
			
			Square bestS = null;
			double bestP = 9999.0;
			if(frontier.size() == 0){
				bestS = bestNonFrontierS;
				bestP = nonFrontierDensity;
			}
			else if(nonFrontier.size() == 0){
				bestS = bestFrontierS;
				bestP = bestFrontierP;
			}
			else if(bestFrontierP <= nonFrontierDensity){
				bestS = bestFrontierS;
				bestP = bestFrontierP;
			}
			else{
				bestS = bestNonFrontierS;
				bestP = nonFrontierDensity;
			}
			
			if(bestS == null){
				System.out.println("No legal guess!");
				return false;
			}
			else{
				Collection<SumConstraint> cs = sToC.get(bestS);
				if(debug){
					printSToC();
					System.out.println("Guessing at " + bestS.getX() + "," + bestS.getY()
						+ " with p=" + bestP);
					System.out.println("Guessed Square has constraints:\n" + cs);
				}
				
				int q = Probe(m, bestS.getX(), bestS.getY());
			    if (Map.BOOM == q){
			    	return false;
			    }
			    else if(q >= 0){
			    	return true;
			    }
			    else{
			    	if(debug){
			    		System.out.println("illegal return from Probe!");
			    	}
			    	return false;
			    }
			}
		}
	}
	
	// return success (i.e. did you blow up)
	private boolean probeRandomSquare(Map m){
		while(true){
			int y = m.pick(m.rows());
		    int x = m.pick(m.columns());
		    int q = Probe(m, x, y);
		    if (Map.BOOM == q){
		    	return false;
		    }
		    else if(q >= 0){
		    	return true;
		    }
		}
	}
}

