package org.ggp.base.player.gamer.statemachine.ew.players;

import java.util.ArrayList;
import java.util.List;

import org.ggp.base.player.gamer.exception.GamePreviewException;
import org.ggp.base.util.TreeNode;
import org.ggp.base.util.game.Game;
import org.ggp.base.util.statemachine.MachineState;
import org.ggp.base.util.statemachine.Move;
import org.ggp.base.util.statemachine.Role;
import org.ggp.base.util.statemachine.exceptions.GoalDefinitionException;
import org.ggp.base.util.statemachine.exceptions.MoveDefinitionException;
import org.ggp.base.util.statemachine.exceptions.TransitionDefinitionException;

public class DepthLimitedPlayer extends PlayerBase {

	private int depthLimit = 0;
	private boolean isSinglePlayer = false;

	private enum EvalType {
		GOAL,
		MOBILITY,
		FOCUS
	}

	private EvalType evalType = EvalType.GOAL;

	private TreeNode<Pair<Move, Integer>> createGameTree() throws GoalDefinitionException, MoveDefinitionException, TransitionDefinitionException {
		TreeNode<Pair<Move, Integer>> root = createGameTreeMaxNode(this.getCurrentState(), 0, 100, 0);

		return root;
	}

	private TreeNode<Pair<Move, Integer>> createGameTreeMaxNode(MachineState state, int alpha, int beta, int depth) throws GoalDefinitionException, MoveDefinitionException, TransitionDefinitionException {
		TreeNode<Pair<Move, Integer>> maxNode = new TreeNode<Pair<Move, Integer>>();

		if (this.getStateMachine().isTerminal(state) || depth >= depthLimit) {
			maxNode.value = Pair.createPair(null, this.EvaluateState(state));
			return maxNode;
		}

		boolean isPruned = false;
		List<Move> legalMoves = this.getStateMachine().getLegalMoves(state, this.getRole());
		Pair<Move, Integer> myChoice = Pair.createPair(legalMoves.get(0), 0);
		for (Move myMove : legalMoves) {
			TreeNode<Pair<Move, Integer>> minNode = null;

			if (isSinglePlayer) {
				List<Move> selectedMove = new ArrayList<Move>();
				selectedMove.add(myMove);

				minNode = createGameTreeMaxNode(this.getStateMachine().getNextState(state, selectedMove), alpha, beta, depth + 1);
			}
			else
				minNode = createGameTreeMinNode(state, myMove, alpha, beta, depth);

			minNode.parent = maxNode;

			if (minNode.value.value > alpha) {
				alpha = minNode.value.value;
				myChoice.key = myMove;
			}

			maxNode.children.add(minNode);

			if (alpha >= beta) {
				isPruned = true;
				break;
			}
		}

		if (isPruned)
			myChoice.value = beta;
		else
			myChoice.value = alpha;

		maxNode.value = myChoice;

		return maxNode;
	}

	private TreeNode<Pair<Move, Integer>> createGameTreeMinNode(MachineState state, Move myMove, int alpha, int beta, int depth) throws GoalDefinitionException, MoveDefinitionException, TransitionDefinitionException {
		TreeNode<Pair<Move, Integer>> minNode = new TreeNode<Pair<Move, Integer>>();

		boolean isPruned = false;
		Pair<Move, Integer> opponentChoice = Pair.createPair(null, 100);
		for (List<Move> jointMoves : this.getStateMachine().getLegalJointMoves(state, this.getRole(), myMove)) {
			TreeNode<Pair<Move, Integer>> nextMaxNode = createGameTreeMaxNode(this.getStateMachine().getNextState(state, jointMoves), alpha, beta, depth + 1);
			nextMaxNode.parent = minNode;

			Move opponentMove = null;
			for (Move move : jointMoves) {
				if (move != myMove) {
					opponentMove = move;
					break;
				}
			}

			minNode.children.add(nextMaxNode);

			if (beta > nextMaxNode.value.value) {
				beta = nextMaxNode.value.value;
				opponentChoice.key = opponentMove;
			}

			if (alpha >= beta) {
				isPruned = true;
				break;
			}
		}

		if (isPruned)
			opponentChoice.value = alpha;
		else
			opponentChoice.value = beta;

		minNode.value = opponentChoice;

		return minNode;
	}

	private int EvaluateState(MachineState state)
			throws GoalDefinitionException, MoveDefinitionException {
		int result = 0;

		switch (evalType) {
		case GOAL:
			result = EvalByGoal(state);
			break;
		case MOBILITY:
			result = EvalByMobility(state);
			break;
		case FOCUS:
			result = EvalByFocus(state);
			break;
		default:
			result = 0;
			break;
		}

		return result;
	}

	private int EvalByGoal(MachineState state) throws GoalDefinitionException {
		int goal = 0;

		try {
			goal = this.getStateMachine().getGoal(state, this.getRole());
		} catch (GoalDefinitionException e) {

		} finally {
			return goal;
		}
	}

	private int EvalByMobility(MachineState state) throws MoveDefinitionException {
		Role myRole = this.getRole();
		int availableMovesNum = 0;

		if (isSinglePlayer)
			return availableMovesNum;


		try {
			availableMovesNum = this.getStateMachine().getLegalMoves(state, myRole).size();

			List<Role> roles = this.getStateMachine().getRoles();
			int totalMovesNum = 0;

			for (Role r : roles) {
				totalMovesNum += this.getStateMachine().getLegalMoves(state, r).size();
			}

			return (int)((float)availableMovesNum / totalMovesNum * 100);
		} catch (MoveDefinitionException e) {
			return 0;
		}
	}

	private int EvalByFocus(MachineState state) throws MoveDefinitionException {
		return 100 - EvalByMobility(state);
	}

	@Override
	public Move onSelectMove(long timeout)
			throws TransitionDefinitionException, MoveDefinitionException,
			GoalDefinitionException {
		// TODO Auto-generated method stub
		Move move = createGameTree().value.key;
		return move;
	}

	@Override
	public void stateMachineMetaGame(long timeout) {
		// TODO Auto-generated method stub
		int playerCount = this.getStateMachine().getRoles().size();

		if (playerCount > 2)
			System.out.println("Player count > 2");
		else if (playerCount == 1)
			isSinglePlayer = true;
		else
			isSinglePlayer = false;

		System.out.println(isSinglePlayer);

		depthLimit = 2;
		evalType = EvalType.GOAL;
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
