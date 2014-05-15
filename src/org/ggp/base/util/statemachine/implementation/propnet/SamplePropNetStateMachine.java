package org.ggp.base.util.statemachine.implementation.propnet;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.ggp.base.util.gdl.grammar.Gdl;
import org.ggp.base.util.gdl.grammar.GdlConstant;
import org.ggp.base.util.gdl.grammar.GdlRelation;
import org.ggp.base.util.gdl.grammar.GdlSentence;
import org.ggp.base.util.propnet.architecture.Component;
import org.ggp.base.util.propnet.architecture.PropNet;
import org.ggp.base.util.propnet.architecture.components.Proposition;
import org.ggp.base.util.propnet.factory.OptimizingPropNetFactory;
import org.ggp.base.util.statemachine.MachineState;
import org.ggp.base.util.statemachine.Move;
import org.ggp.base.util.statemachine.Role;
import org.ggp.base.util.statemachine.StateMachine;
import org.ggp.base.util.statemachine.exceptions.GoalDefinitionException;
import org.ggp.base.util.statemachine.exceptions.MoveDefinitionException;
import org.ggp.base.util.statemachine.exceptions.TransitionDefinitionException;
import org.ggp.base.util.statemachine.implementation.prover.query.ProverQueryBuilder;


@SuppressWarnings("unused")
public class SamplePropNetStateMachine extends StateMachine {
    /** The underlying proposition network  */
    private PropNet propNet;
    /** The topological ordering of the propositions */
    private List<Proposition> ordering;
    /** The player roles */
    private List<Role> roles;

    private MachineState initialState = null;

    /**
     * Initializes the PropNetStateMachine. You should compute the topological
     * ordering here. Additionally you may compute the initial state here, at
     * your discretion.
     */
	@Override
	public void initialize(List<Gdl> description) {
		try {
			this.beginProfile();

			propNet = OptimizingPropNetFactory.create(description);
			roles = propNet.getRoles();
			ordering = getOrdering();

			Proposition initProp = propNet.getInitProposition();

			for (Proposition p : ordering) {
				p.forceUpdate();
			}

			initProp.setValue(true, false);

			initialState = this.getStateFromBase();

			initProp.setValue(false, false);

			this.endProfile("initialize");
		} catch (InterruptedException e) {
			System.out.println("!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!"
					+ e.getMessage());
		}
	}

	/**
	 * Computes if the state is terminal. Should return the value of the
	 * terminal proposition for the state.
	 */
	@Override
	public boolean isTerminal(MachineState state) {
		this.beginProfile();

		this.setBasePropositionsFor(state);
		this.propagate();

		Proposition terminalProp = propNet.getTerminalProposition();

		this.endProfile("isTerminal");

		return terminalProp.getValue();
	}

	/**
	 * Computes the goal for a role in the current state.
	 * Should return the value of the goal proposition that
	 * is true for that role. If there is not exactly one goal
	 * proposition true for that role, then you should throw a
	 * GoalDefinitionException because the goal is ill-defined.
	 */
	@Override
	public int getGoal(MachineState state, Role role)
	throws GoalDefinitionException {
		this.beginProfile();

		this.setBasePropositionsFor(state);
		this.propagate();

		Set<Proposition> goalProps = propNet.getGoalPropositions().get(role);
		Proposition goalProp = null;
		for (Proposition g : goalProps) {
			if (g.getValue()) {
				if (goalProp == null) {
					goalProp = g;
				} else {
					throw new GoalDefinitionException(state, role);
				}
			}
		}

		if (goalProp == null)
			throw new GoalDefinitionException(state, role);

		this.endProfile("getGoal");

		return this.getGoalValue(goalProp);
	}

	/**
	 * Returns the initial state. The initial state can be computed
	 * by only setting the truth value of the INIT proposition to true,
	 * and then computing the resulting state.
	 */
	@Override
	public MachineState getInitialState() {
		return initialState;
	}

	private void setBasePropositionsFor(MachineState state) {
		this.beginProfile();

		Set<GdlSentence> sentences = state.getContents();
		Map<GdlSentence, Proposition> baseMaps = propNet.getBasePropositions();
		Set<Map.Entry<GdlSentence, Proposition>> baseMapEntries = baseMaps.entrySet();

		for (Map.Entry<GdlSentence, Proposition> e : baseMapEntries) {
			e.getValue().setValue(false, true);
		}

		for (GdlSentence s : sentences) {
			baseMaps.get(s).setValue(true, true);
		}

		int count = 0;
		for (Map.Entry<GdlSentence, Proposition> e : baseMapEntries) {
			if (e.getValue().getIsChanged())
				++count;
		}

		this.endProfile("setBasePropositionsFor");
	}

	private void setInputPropositionsFor(List<Move> moves) {
		this.beginProfile();

		Map<GdlSentence, Proposition> inputProps = propNet.getInputPropositions();
		Set<Map.Entry<GdlSentence, Proposition>> inputPropEntries = inputProps.entrySet();
		for (Map.Entry<GdlSentence, Proposition> e : inputPropEntries) {
			e.getValue().setValue(false, true);
		}

		List<GdlSentence> moveSentences = this.toDoes(moves);
		for (GdlSentence s : moveSentences) {
			inputProps.get(s).setValue(true, true);
		}

		this.endProfile("setInputPropositionsFor");
	}

	private void propagate()
	{
		Map<GdlSentence, Proposition> baseMaps = propNet.getBasePropositions();
		Set<Map.Entry<GdlSentence, Proposition>> baseMapEntries = baseMaps.entrySet();
		for (Map.Entry<GdlSentence, Proposition> e : baseMapEntries) {
			e.getValue().propagate();
		}

		Map<GdlSentence, Proposition> inputProps = propNet.getInputPropositions();
		Set<Map.Entry<GdlSentence, Proposition>> inputPropEntries = inputProps.entrySet();
		for (Map.Entry<GdlSentence, Proposition> e : inputPropEntries) {
			e.getValue().propagate();
		}
	}

	/**
	 * Computes the legal moves for role in state.
	 */
	@Override
	public List<Move> getLegalMoves(MachineState state, Role role)
	throws MoveDefinitionException {
		this.beginProfile();

		this.setBasePropositionsFor(state);
		this.propagate();

		Set<Proposition> legalProps = propNet.getLegalPropositions().get(role);
		Set<Proposition> trues = new HashSet<Proposition>();
		for (Proposition p : legalProps) {
			if (p.getValue())
				trues.add(p);
		}

		Map<Proposition, Proposition> legalInputMaps = propNet.getLegalInputMap();
		List<Move> legalMoves = new ArrayList<Move>();

		for (Proposition p : trues) {
			Proposition legalInputProp = legalInputMaps.get(p);
			legalMoves.add(getMoveFromProposition(legalInputProp));
		}

		this.endProfile("getLegalMoves");

		return legalMoves;
	}

	/**
	 * Computes the next state given state and the list of moves.
	 */
	@Override
	public MachineState getNextState(MachineState state, List<Move> moves)
	throws TransitionDefinitionException {
		this.beginProfile();

		this.setBasePropositionsFor(state);
		this.setInputPropositionsFor(moves);
		this.propagate();

		this.endProfile("getNextState");

		return this.getStateFromBase();
	}

	/**
	 * This should compute the topological ordering of propositions.
	 * Each component is either a proposition, logical gate, or transition.
	 * Logical gates and transitions only have propositions as inputs.
	 *
	 * The base propositions and input propositions should always be exempt
	 * from this ordering.
	 *
	 * The base propositions values are set from the MachineState that
	 * operations are performed on and the input propositions are set from
	 * the Moves that operations are performed on as well (if any).
	 *
	 * @return The order in which the truth values of propositions need to be set.
	 */
	public List<Proposition> getOrdering()
	{
	    // List to contain the topological ordering.
	    List<Proposition> order = new LinkedList<Proposition>();

		// All of the components in the PropNet
		List<Component> components = new ArrayList<Component>(propNet.getComponents());

		// All of the propositions in the PropNet.
		List<Proposition> propositions = new ArrayList<Proposition>(propNet.getPropositions());

	    // Compute the topological ordering.
		List<Proposition> bases = new ArrayList<Proposition>(propNet.getBasePropositions().values());
		List<Proposition> inputs = new ArrayList<Proposition>(propNet.getInputPropositions().values());

		Proposition init = propNet.getInitProposition();

		List<Proposition> addingPropositions = new LinkedList<Proposition>();

		order.addAll(bases);
		order.addAll(inputs);
		order.add(init);

		propositions.removeAll(bases);
		propositions.removeAll(inputs);
		propositions.remove(init);

		while (!propositions.isEmpty()) {
			for (Proposition p : propositions) {
				boolean isNextLevelProp = this.isPropositionIn(order,
						p.getSingleInput());

				if (isNextLevelProp)
					addingPropositions.add(p);
			}

			order.addAll(addingPropositions);
			propositions.removeAll(addingPropositions);
			addingPropositions.clear();
		}

		order.removeAll(bases);
		order.removeAll(inputs);
		order.remove(init);

		return order;
	}

	private boolean isPropositionIn(List<Proposition> propositionList, Component c) {
		if (c instanceof Proposition) {
			return propositionList.contains(c);
		} else {
			Set<Component> componentInputs = c.getInputs();
			for (Component ci : componentInputs) {
				if (!isPropositionIn(propositionList, ci))
					return false;
			}

			return true;
		}
	}

	/* Already implemented for you */
	@Override
	public List<Role> getRoles() {
		return roles;
	}

	/* Helper methods */

	/**
	 * The Input propositions are indexed by (does ?player ?action).
	 *
	 * This translates a list of Moves (backed by a sentence that is simply ?action)
	 * into GdlSentences that can be used to get Propositions from inputPropositions.
	 * and accordingly set their values etc.  This is a naive implementation when coupled with
	 * setting input values, feel free to change this for a more efficient implementation.
	 *
	 * @param moves
	 * @return
	 */
	private List<GdlSentence> toDoes(List<Move> moves)
	{
		List<GdlSentence> doeses = new ArrayList<GdlSentence>(moves.size());
		Map<Role, Integer> roleIndices = getRoleIndices();

		for (int i = 0; i < roles.size(); i++)
		{
			int index = roleIndices.get(roles.get(i));
			doeses.add(ProverQueryBuilder.toDoes(roles.get(i), moves.get(index)));
		}
		return doeses;
	}

	/**
	 * Takes in a Legal Proposition and returns the appropriate corresponding Move
	 * @param p
	 * @return a PropNetMove
	 */
	public static Move getMoveFromProposition(Proposition p)
	{
		return new Move(p.getName().get(1));
	}

	/**
	 * Helper method for parsing the value of a goal proposition
	 * @param goalProposition
	 * @return the integer value of the goal proposition
	 */
    private int getGoalValue(Proposition goalProposition)
	{
		GdlRelation relation = (GdlRelation) goalProposition.getName();
		GdlConstant constant = (GdlConstant) relation.get(1);
		return Integer.parseInt(constant.toString());
	}

	/**
	 * A Naive implementation that computes a PropNetMachineState
	 * from the true BasePropositions.  This is correct but slower than more advanced implementations
	 * You need not use this method!
	 * @return PropNetMachineState
	 */
	public MachineState getStateFromBase()
	{
		Set<GdlSentence> contents = new HashSet<GdlSentence>();
		for (Proposition p : propNet.getBasePropositions().values())
		{
			p.setValue(p.getSingleInput().getValue(), true);
			if (p.getValue())
			{
				contents.add(p.getName());
			}

		}
		return new MachineState(contents);
	}
}