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

/**
 * There is no boolean for probed because
 * we no longer care about a square once it is probed;
 * it is then replaced by a constraint.
 * 
 * Safe-flags are only for temporary consideration,
 * to see if any constraints would be violated.
 */
public final class Square{
	private int x, y;
	private boolean flagged;
	private boolean safeFlagged;
	
	public Square(int x, int y) {
		super();
		this.x = x;
		this.y = y;
		this.flagged = false;
		this.safeFlagged = false;
	}
	
	public boolean isFlagged() {
		return flagged;
	}
	public void setFlagged(boolean flagged) {
		this.flagged = flagged;
	}
	public boolean isSafeFlagged() {
		return safeFlagged;
	}
	public void setSafeFlagged(boolean safeFlagged) {
		this.safeFlagged = safeFlagged;
	}
	public int getX() {
		return x;
	}
	public int getY() {
		return y;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + x;
		result = prime * result + y;
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		Square other = (Square) obj;
		if (x != other.x)
			return false;
		if (y != other.y)
			return false;
		return true;
	}

	@Override
	public String toString() {
		return "[" + x + ", " + y + "]";
	}
	
	
}
