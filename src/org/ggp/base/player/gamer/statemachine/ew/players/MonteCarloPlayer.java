package org.ggp.base.player.gamer.statemachine.ew.players;

import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

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

public class MonteCarloPlayer extends PlayerBase {

	private Random r = new Random();
	private double epsilon = 1e-6;

	class NodeData {
		public Move move = null;
		public int utility = 0;
		public int visitNum = 0;
		public MachineState state = null;
		public boolean isTerminal = false;
	}

	private TreeNode<NodeData> expandGameTree(TreeNode<NodeData> nodeToExpand)
			throws MoveDefinitionException, TransitionDefinitionException {
		if (nodeToExpand.children.size() == 0) {
			Role myRole = this.getRole();
			StateMachine stateMachine = this.getStateMachine();
			Set<Map.Entry<Move, List<MachineState>>> nextStates = this
					.getStateMachine()
					.getNextStates(nodeToExpand.value.state, myRole).entrySet();

			for (Map.Entry<Move, List<MachineState>> e : nextStates) {
				List<MachineState> states = e.getValue();
				Move m = e.getKey();
				for (MachineState s : states) {
					TreeNode<NodeData> child = new TreeNode<NodeData>();
					child.parent = nodeToExpand;
					child.value = new NodeData();
					child.value.move = m;
					child.value.state = s;
					child.value.isTerminal = stateMachine.isTerminal(s);
					nodeToExpand.children.add(child);
				}
			}
		}

		return nodeToExpand;
	}

	private TreeNode<NodeData> selectNodeToExpand(TreeNode<NodeData> nodeToStart, int maxVisitNum) {
		if (nodeToStart.value.visitNum <= maxVisitNum)
			return nodeToStart;

		if (nodeToStart.value.isTerminal)
			return null;

		for (TreeNode<NodeData> c : nodeToStart.children) {
			if (c.value.visitNum <= maxVisitNum)
				return c;
		}

		double maxSelectability = -1;
		TreeNode<NodeData> selectedNode = null;

		for (TreeNode<NodeData> c : nodeToStart.children) {
			double currentNodeSelectability = calculateSelectabilityFor(c);
			if (currentNodeSelectability > maxSelectability) {
				maxSelectability = currentNodeSelectability;
				selectedNode = c;
			}
		}

		return selectNodeToExpand(selectedNode, maxVisitNum);
	}

	private double calculateSelectabilityFor(TreeNode<NodeData> node) {
		int avgUtility = node.value.visitNum == 0 ? 0 : node.value.utility / node.value.visitNum;
		double result = avgUtility
				+ Math.sqrt(Math.log(node.parent.value.visitNum)
						/ node.value.visitNum) * r.nextDouble() * epsilon;
		return result;
	}

	private void backPropagateFrom(TreeNode<NodeData> node, int score) {
		node.value.visitNum++;
		node.value.utility += score;

		if (node.parent != null) {
			backPropagateFrom(node.parent, score);
		}
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

		int maxVisitNum = 0;
		while (true) {
			if (!monteCarloSimulate(nodeToSearch, maxVisitNum))
				maxVisitNum++;

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
		int minUtilityForMove = Integer.MAX_VALUE;
		Move currentMove = nodeToSearch.children.get(0).value.move;
		Move bestMove = null;

		long rcs = System.currentTimeMillis();
		for (TreeNode<NodeData> c : nodeToSearch.children) {
			int averageUtility = c.value.visitNum == 0 ? 0 : c.value.utility
					/ c.value.visitNum;

			if (c.value.move != currentMove) {
				if (maxUtility < minUtilityForMove) {
					bestMove = currentMove;
					maxUtility = minUtilityForMove;
				}

				currentMove = c.value.move;
				minUtilityForMove = Integer.MAX_VALUE;
			}
			if (averageUtility < minUtilityForMove) {
				minUtilityForMove = averageUtility;
			}
		}

		if (maxUtility < minUtilityForMove) {
			bestMove = currentMove;
			maxUtility = minUtilityForMove;
		}

		System.out.println("Time consumed for finding move: "
				+ (System.currentTimeMillis() - rcs));

		System.out.println("Got " + (timeout - startClock)
				+ " ms to simulate. Simulated " + simulationCount
				+ " times. Average simulation time: "
				+ totalTimeConsumedForSimulation / simulationCount + ".");

		return bestMove;
	}

	private boolean monteCarloSimulate(TreeNode<NodeData> start, int maxVisitCount)
			throws MoveDefinitionException, GoalDefinitionException,
			TransitionDefinitionException {
		TreeNode<NodeData> selection = this.selectNodeToExpand(start, maxVisitCount);
		boolean shouldContinue = false;

		if (selection != null) {
			int score = depthCharge(selection.value.state, -1);
			backPropagateFrom(selection, score);
			this.expandGameTree(selection);

			shouldContinue = true;
		}

		return shouldContinue;
	}

	private int depthCharge(MachineState state, int depthLeft)
			throws MoveDefinitionException, GoalDefinitionException,
			TransitionDefinitionException {
		MachineState mState= state;
	    while(!this.getStateMachine().isTerminal(mState)){
	        mState= this.getStateMachine().getRandomNextState(mState);
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
