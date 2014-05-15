package org.ggp.base.util.propnet.architecture.components;

import java.util.Set;

import org.ggp.base.util.propnet.architecture.Component;

/**
 * The And class is designed to represent logical AND gates.
 */
@SuppressWarnings("serial")
public final class And extends Component
{
	private int trueCount = 0;

	/**
	 * Returns true if and only if every input to the and is true.
	 *
	 * @see org.ggp.base.util.propnet.architecture.Component#getValue()
	 */
	@Override
	public boolean getValue()
	{
		return trueCount == this.getInputs().size();
	}

	/**
	 * @see org.ggp.base.util.propnet.architecture.Component#toString()
	 */
	@Override
	public String toString()
	{
		return toDot("invhouse", "grey", "AND");
	}

	@Override
	public void onInputUpdated(Component updatedInput) {
		boolean origVal = this.getValue();

		// TODO Auto-generated method stub
		if (updatedInput.getValue())
			++trueCount;
		else
			--trueCount;

		boolean newVal = this.getValue();

		if (newVal != origVal) {
			Set<Component> outputs = this.getOutputs();
			for (Component o : outputs) {
				o.onInputUpdated(this);
			}
		}
	}

	@Override
	public void forceUpdate() {
		// TODO Auto-generated method stub
		Set<Component> inputs = this.getInputs();
		int newTrueCount = 0;

		for (Component i : inputs) {
			if (!(i instanceof Proposition)) {
				i.forceUpdate();
			}

			if (i.getValue())
				++newTrueCount;
		}

		trueCount = newTrueCount;
	}

}
