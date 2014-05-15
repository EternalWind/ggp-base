package org.ggp.base.player.gamer.statemachine.ew.players;

import java.util.List;

import org.ggp.base.player.gamer.exception.GamePreviewException;
import org.ggp.base.util.TreeNode;
import org.ggp.base.util.game.Game;
import org.ggp.base.util.statemachine.MachineState;
import org.ggp.base.util.statemachine.Move;
import org.ggp.base.util.statemachine.exceptions.GoalDefinitionException;
import org.ggp.base.util.statemachine.exceptions.MoveDefinitionException;
import org.ggp.base.util.statemachine.exceptions.TransitionDefinitionException;

public class MinimaxPlayer extends PlayerBase {

	private TreeNode<Pair<Move, Integer>> createGameTree() throws GoalDefinitionException, MoveDefinitionException, TransitionDefinitionException {
		TreeNode<Pair<Move, Integer>> root = createGameTreeMaxNode(this.getCurrentState(), 0, 100);

		return root;
	}

	private TreeNode<Pair<Move, Integer>> createGameTreeMaxNode(MachineState state, int alpha, int beta) throws GoalDefinitionException, MoveDefinitionException, TransitionDefinitionException {
		TreeNode<Pair<Move, Integer>> maxNode = new TreeNode<Pair<Move, Integer>>();

		if (this.getStateMachine().isTerminal(state)) {
			maxNode.value = Pair.createPair(null, this.getStateMachine().getGoal(state, this.getRole()));
			return maxNode;
		}

		boolean isPruned = false;
		List<Move> legalMoves = this.getStateMachine().getLegalMoves(state, this.getRole());
		Pair<Move, Integer> myChoice = Pair.createPair(null, 0);
		for (Move myMove : legalMoves) {
			TreeNode<Pair<Move, Integer>> minNode = createGameTreeMinNode(state, myMove, alpha, beta);
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

	private TreeNode<Pair<Move, Integer>> createGameTreeMinNode(MachineState state, Move myMove, int alpha, int beta) throws GoalDefinitionException, MoveDefinitionException, TransitionDefinitionException {
		TreeNode<Pair<Move, Integer>> minNode = new TreeNode<Pair<Move, Integer>>();

		boolean isPruned = false;
		Pair<Move, Integer> opponentChoice = Pair.createPair(null, 100);
		for (List<Move> jointMoves : this.getStateMachine().getLegalJointMoves(state, this.getRole(), myMove)) {
			TreeNode<Pair<Move, Integer>> nextMaxNode = createGameTreeMaxNode(this.getStateMachine().getNextState(state, jointMoves), alpha, beta);
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

	@Override
	public Move onSelectMove(long timeout)
			throws TransitionDefinitionException, MoveDefinitionException,
			GoalDefinitionException {
		// TODO Auto-generated method stub
		return createGameTree().value.key;
	}

	@Override
	public void stateMachineMetaGame(long timeout) {
		// TODO Auto-generated method stub
		if (this.getStateMachine().getRoles().size() != 2)
			System.out.println("Player count != 2");
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
