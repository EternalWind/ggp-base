package org.ggp.base.player.gamer.statemachine.ew.players;

import java.util.List;

import org.ggp.base.player.gamer.event.GamerSelectedMoveEvent;
import org.ggp.base.player.gamer.statemachine.StateMachineGamer;
import org.ggp.base.util.statemachine.Move;
import org.ggp.base.util.statemachine.StateMachine;
import org.ggp.base.util.statemachine.exceptions.GoalDefinitionException;
import org.ggp.base.util.statemachine.exceptions.MoveDefinitionException;
import org.ggp.base.util.statemachine.exceptions.TransitionDefinitionException;
import org.ggp.base.util.statemachine.implementation.propnet.SamplePropNetStateMachine;

public abstract class PlayerBase extends StateMachineGamer {
	@Override
	public StateMachine getInitialStateMachine() {
		// TODO Auto-generated method stub
		//return new ProverStateMachine();
		return new SamplePropNetStateMachine();
	}

	@Override
	public Move stateMachineSelectMove(long timeout)
			throws TransitionDefinitionException, MoveDefinitionException,
			GoalDefinitionException {
		long start = System.currentTimeMillis();

		StateMachine stateMachine = this.getStateMachine();
		List<Move> legalMoves = stateMachine.getLegalMoves(this.getCurrentState(), this.getRole());
		Move selectedMove = onSelectMove(timeout);

		long end = System.currentTimeMillis();
		notifyObservers(new GamerSelectedMoveEvent(legalMoves, selectedMove, end - start));

		this.getStateMachine().showProfiles();

		return selectedMove;
	}

	@Override
	public String getName() {
		// TODO Auto-generated method stub
		return this.getClass().getSimpleName();
	}

	public abstract Move onSelectMove(long timeout) throws TransitionDefinitionException, MoveDefinitionException,
	GoalDefinitionException;
}
