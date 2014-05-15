package org.ggp.base.player.gamer.statemachine.ew.players;

import java.util.List;
import java.util.Random;

import org.ggp.base.player.gamer.exception.GamePreviewException;
import org.ggp.base.util.TreeNode;
import org.ggp.base.util.game.Game;
import org.ggp.base.util.statemachine.MachineState;
import org.ggp.base.util.statemachine.Move;
import org.ggp.base.util.statemachine.Role;
import org.ggp.base.util.statemachine.StateMachine;
import org.ggp.base.util.statemachine.exceptions.GoalDefinitionException;
import org.ggp.base.util.statemachine.exceptions.MoveDefinitionException;
import org.ggp.base.util.statemachine.exceptions.TransitionDefinitionException;

public class SimpleMonteCarloPlayer extends PlayerBase {

	private Random r = new Random();
	private double epsilon = 1e-6;

	class NodeData {
		public Move move = null;
		public int utility = 0;
		public int visitNum = 0;
		public MachineState state = null;
	}

	private TreeNode<NodeData> expandGameTree(TreeNode<NodeData> nodeToExpand)
			throws MoveDefinitionException, TransitionDefinitionException {
		if (nodeToExpand.children.size() == 0) {
			Role myRole = this.getRole();
			StateMachine stateMachine = this.getStateMachine();
			List<Move> legalMoves = stateMachine.getLegalMoves(nodeToExpand.value.state, myRole);

			for (Move m : legalMoves) {
				TreeNode<NodeData> node = new TreeNode<NodeData>();
				node.value = new NodeData();
				node.value.move = m;
				node.parent = nodeToExpand;

				nodeToExpand.children.add(node);
			}
		}

		return nodeToExpand;
	}

	private Move monteCarloSearch(TreeNode<NodeData> nodeToSearch, long timeout)
			throws MoveDefinitionException, GoalDefinitionException,
			TransitionDefinitionException {
		long startClock = System.currentTimeMillis();
		long stepClock = startClock;
		final long STOP_THRESHOLD = 100;
		final long TIME_CONSUMED_LOWER_BOUND = 5;
		long totalTimeConsumedForSimulation = 0;
		long simulationCount = 0;
		Role role = this.getRole();

		this.expandGameTree(nodeToSearch);
		int childCount = nodeToSearch.children.size();

		while (true) {
			TreeNode<NodeData> c = nodeToSearch.children.get(r.nextInt(childCount));
			c.value.utility += this.depthCharge(this.getStateMachine().getRandomNextState(nodeToSearch.value.state, role, c.value.move), -1);
			c.value.visitNum += 1;

			long currentClock = System.currentTimeMillis();
			long timeConsumed = currentClock - stepClock;
			stepClock = currentClock;
			totalTimeConsumedForSimulation += timeConsumed;
			simulationCount++;

			long averageTimeConsumed = totalTimeConsumedForSimulation
					/ simulationCount;
			if (averageTimeConsumed < TIME_CONSUMED_LOWER_BOUND)
				averageTimeConsumed = TIME_CONSUMED_LOWER_BOUND;

			if ((currentClock + averageTimeConsumed) > (timeout - STOP_THRESHOLD)) {
				break;
			}
		}

		int maxUtility = -1;
		Move bestMove = null;

		long rcs = System.currentTimeMillis();
		for (TreeNode<NodeData> c : nodeToSearch.children) {
			int averageUtility = c.value.visitNum == 0 ? 0 : c.value.utility
					/ c.value.visitNum;

			if (maxUtility < averageUtility) {
				bestMove = c.value.move;
				maxUtility = averageUtility;
			}
		}

		System.out.println("Time consumed for finding move: "
				+ (System.currentTimeMillis() - rcs));

		System.out.println("Got " + (timeout - startClock)
				+ " ms to simulate. Simulated " + simulationCount
				+ " times. Average simulation time: "
				+ totalTimeConsumedForSimulation / simulationCount + ".");

		return bestMove;
	}

	private int depthCharge(MachineState state, int depthLeft)
			throws MoveDefinitionException, GoalDefinitionException,
			TransitionDefinitionException {
		MachineState mState= state;
	    while(!this.getStateMachine().isTerminal(mState)){
	        mState = this.getStateMachine().getRandomNextState(mState);
	    }
	    return this.getStateMachine().getGoal(mState, this.getRole());
	}

	@Override
	public Move onSelectMove(long timeout)
			throws TransitionDefinitionException, MoveDefinitionException,
			GoalDefinitionException {
		// TODO Auto-generated method stub
		long startClock = System.currentTimeMillis();

		List<Move> legalMoves = this.getStateMachine().getLegalMoves(
				this.getCurrentState(), this.getRole());
		if (legalMoves.size() < 2)
			return legalMoves.get(0);

		TreeNode<NodeData> root = new TreeNode<NodeData>();
		root.value = new NodeData();
		root.value.state = this.getCurrentState();

		return monteCarloSearch(root, timeout
				- (System.currentTimeMillis() - startClock));
	}

	@Override
	public void stateMachineMetaGame(long timeout)
			throws TransitionDefinitionException, MoveDefinitionException,
			GoalDefinitionException {
		// TODO Auto-generated method stub

	}

	@Override
	public void stateMachineStop() {
		// TODO Auto-generated method stub

	}

	@Override
	public void stateMachineAbort() {
		// TODO Auto-generated method stub

	}

	@Override
	public void preview(Game g, long timeout) throws GamePreviewException {
		// TODO Auto-generated method stub

	}

}
