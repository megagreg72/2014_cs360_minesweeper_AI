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
public final class BrianStrategy implements Strategy {
	
	
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
	
	private void initGlobals(){
		// when removing from constraints, also remove all pointers to it in sToC
		sToC = HashMultimap.create();
		constraints = new ArrayList<SumConstraint>();
		
		updatedSquares = new LinkedList<Square>();
		updatedSquareSet = new HashSet<Square>();
		
		updatedConstraints = new LinkedList<SumConstraint>();
		updatedConstraintSet = new HashSet<SumConstraint>();
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
			printSquareQueue();
			printConstraintQueue();
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
				System.out.println("Guessing!");
				probeRandomSquare(m);
			}
		}
	}
	
	private void checkConstraints(Map m, Square s){
		Collection<SumConstraint> cs = sToC.get(s);
		
		if(m.look(s.getX(), s.getY()) != Map.MARKED){
			// temporarily assume it is a bomb
			s.setFlagged(true);
			Set<SumConstraint> violated = new HashSet<SumConstraint>();
			for(SumConstraint c : cs){
				if(c.isViolated()){
					violated.add(c);
					System.out.println("Violated: " + c);
				}
			}
			// contradiction ==> safe
			if(violated.size() > 0){
				Probe(m, s.getX(), s.getY());
			}
			// end assumption
			s.setFlagged(false);
			
			// temporarily assume it is not a bomb
			s.setSafeFlagged(true);
			violated = new HashSet<SumConstraint>();
			for(SumConstraint c : cs){
				if(c.isViolated()){
					violated.add(c);
					System.out.println("Violated: " + c);
				}
			}
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
		System.out.println("Probing " + x + ", " + y);
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
		}
		// remove old square
		Square s = myMap.get(x).get(y);
		for(SumConstraint c : sToC.get(s)){
			Set<Square> l = new HashSet<Square>();
			l.add(s);
			// substitute a single square for a non-bomb
			trySubstitution(c, l, 0);
			if(c.vars.isEmpty()){
				removeConstraint(c);
			}
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
			if(c.vars.isEmpty()){
				removeConstraint(c);
			}
			if(c.maxSum < 0){
				System.out.println("mis-shrunk: " + c);
				System.out.println("oops0.1");
			}
		}
		//sToC.removeAll(s);
	}
	
	public void simplifyConstraints(SumConstraint new_c){
		if(new_c.vars.isEmpty()){
			removeConstraint(new_c);
			return;
		}
		System.out.println("Simplifying: " + new_c);
		List<SumConstraint> toRemove = new ArrayList<SumConstraint>();
		for(SumConstraint c : constraints){
			// substitute
			if(new_c.equals(c)){
				System.out.println("its me!");
			}
			else{
				trySubstitution(c, new_c.vars, new_c.maxSum);
				if(c.vars.isEmpty()){
					toRemove.add(c);
				}
				if(c.maxSum < 0){
					System.out.println("mis-shrunk: " + c);
					System.out.println("oops0.2");
				}
			}
		}
		for(SumConstraint c : toRemove){
			removeConstraint(c);
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
			System.out.println("Shrinking: " + c + " by " + ss + ", subtracting " + numMines);
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

