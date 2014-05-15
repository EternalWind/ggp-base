package org.ggp.base.player.gamer.statemachine.ew.players;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import org.ggp.base.player.gamer.exception.GamePreviewException;
import org.ggp.base.util.game.Game;
import org.ggp.base.util.statemachine.MachineState;
import org.ggp.base.util.statemachine.Move;
import org.ggp.base.util.statemachine.StateMachine;
import org.ggp.base.util.statemachine.exceptions.GoalDefinitionException;
import org.ggp.base.util.statemachine.exceptions.MoveDefinitionException;
import org.ggp.base.util.statemachine.exceptions.TransitionDefinitionException;

class Pair<T1, T2> {
	public T1 key;
	public T2 value;

	public static <T1, T2> Pair<T1, T2> createPair(T1 key, T2 value) {
		return new Pair<T1, T2>(key, value);
	}

	public Pair(T1 key, T2 value) {
		this.key = key;
		this.value = value;
	}
}

public class DeliberationPlayer extends PlayerBase {

	private Pair<Integer, LinkedList<Move>> bestPlan = null;
	private int currentStep = 0;

	private Pair<Integer, LinkedList<Move>> searchForBestPlan(MachineState state) throws GoalDefinitionException, MoveDefinitionException, TransitionDefinitionException {
		StateMachine sm = getStateMachine();

		if (sm.isTerminal(state)) {
			int goal = sm.getGoal(state, this.getRole());
			return Pair.createPair(goal, new LinkedList<Move>());
		}

		int maxGoal = 0;
		LinkedList<Move> actions = null;
		List<Move> legalActions = sm.getLegalMoves(state, this.getRole());

		for (Move legalAction : legalActions) {
			List<Move> actionToPerform = new ArrayList<Move>();
			actionToPerform.add(legalAction);

			MachineState nextState = sm.getNextState(state, actionToPerform);
			Pair<Integer, LinkedList<Move>> result = searchForBestPlan(nextState);
			if (maxGoal < result.key) {
				maxGoal = result.key;
				actions = result.value;
				actions.add(legalAction);
			}
		}

		return Pair.createPair(maxGoal, actions);
	}

	@Override
	public Move onSelectMove(long timeout)
			throws TransitionDefinitionException, MoveDefinitionException,
			GoalDefinitionException {
		// TODO Auto-generated method stub
		return bestPlan.value.get(currentStep++);
	}

	@Override
	public void stateMachineMetaGame(long timeout)
			throws TransitionDefinitionException, MoveDefinitionException,
			GoalDefinitionException {
		// TODO Auto-generated method stub
		bestPlan = searchForBestPlan(this.getCurrentState());
		currentStep = 0;

		System.out.println("Best plan: " + bestPlan.key + " (" + bestPlan.value.size() + ")");
		for (Move move : bestPlan.value) {
			System.out.println(move.toString());
		}
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
