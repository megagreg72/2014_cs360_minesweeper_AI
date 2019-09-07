Brian Gauch
CS 360
project #2: minesweeper
11/16/2014

See "CS 360 Fall 2014_Prog Assn 2.pdf" for assignment description.

Below is a description of my solution:

--- COMPILATION ---

1) import src and lib folders into a new Eclipse project
2) run PGMS as a java applet, with parameters set in "run configurations"

notes:
- could not figure out how to compile with jmk, as PGMS was originally intended
- "main" is never run, only code in "init"

--- /COMPILATION ---

--- CSP PSUEDO-CODE ---

We maintain a list of all constraints.
We maintain a 2D array of Squares.
We maintain queues of updated Squares and updated constraints.
We maintain a mapping between constraints and Squares.

When a Square is probed, it loses all constraints,
and a corresponding constraint is formed at that square, based on the revealed number.
All constraints containing that square are pushed onto the queue.

When a constraint has been updated, we attempt substitutions between it and all other constraints.
A constraint may be updated by substitution, or by having one of its Squares revealed or flagged.
A reveal or flagging is treated as a 1-Square-constraint substition, so that the constraint no longer
depends on the known variable.

When a constraint has been updated, we attempt 
1) a proof by contradiciton that all of its Squares are bombs
2) a proof by contradiciton that all of its Squares are not bombs

When a constraint is empty, we throw it away.

--- /CSP PSUEDO-CODE ---

--- SPECIAL ---

Let the "frontier" be the set of Squares which are constrained by at least one constraint.
Because counting all possible states is exponential w.r.t. the size of the frontier,
this guessing strategy is infeasible for some large boards.
When the frontier is too large, (chosen as  >30) we fall back on a simpler guessing function.
The fallback guessing function used was not merely true randomness, but looked at the single
most constraining constraint for each Square to determine its rough probablity of being a bomb.

--- /SPECIAL ---
