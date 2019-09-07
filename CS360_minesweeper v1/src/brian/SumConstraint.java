package brian;

import java.util.HashSet;
import java.util.Set;


public class SumConstraint {
	public Set<Square> vars = new HashSet<Square>();
	public int minSum;
	public int maxSum;
	
	public SumConstraint(Set<Square> vars, int sum) {
	    this.vars = vars;
	    this.minSum = sum;
	    this.maxSum = sum;
	}
	
	public SumConstraint(Set<Square> vars, int minSum, int maxSum) {
	    this.vars = vars;
	    this.minSum = minSum;
	    this.maxSum = maxSum;
	}
	
	public boolean isViolated(){
		int minSum = 0;
		int maxSum = 0;
		for (Square var : vars){
			if(var.isFlagged()){
				minSum++;
				maxSum++;
			}
			else if(var.isSafeFlagged()){
				// neither sum increases
			}
			else{
				maxSum++;
			}
		}
		if(this.minSum == 0){
			//System.out.println("foo");
		}
		if(this.minSum < minSum)
			return true;
		else if(maxSum < this.maxSum)
			return true;
		else
			return false;
	}

	@Override
	public String toString() {
		return "C: [vars=" + vars + ", minSum=" + minSum
				+ ", maxSum=" + maxSum + "]";
	}
}
