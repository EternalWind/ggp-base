package org.ggp.base.util.propnet.architecture.components;

import java.util.Set;

import org.ggp.base.util.gdl.grammar.GdlSentence;
import org.ggp.base.util.propnet.architecture.Component;

/**
 * The Proposition class is designed to represent named latches.
 */
@SuppressWarnings("serial")
public final class Proposition extends Component
{
	/** The name of the Proposition. */
	private GdlSentence name;
	/** The value of the Proposition. */
	private boolean value;

	private boolean isChanged = false;

	public boolean getIsChanged() {
		return isChanged;
	}

	/**
	 * Creates a new Proposition with name <tt>name</tt>.
	 *
	 * @param name
	 *            The name of the Proposition.
	 */
	public Proposition(GdlSentence name)
	{
		this.name = name;
		this.value = false;
	}

	/**
	 * Getter method.
	 *
	 * @return The name of the Proposition.
	 */
	public GdlSentence getName()
	{
		return name;
	}

    /**
     * Setter method.
     *
     * This should only be rarely used; the name of a proposition
     * is usually constant over its entire lifetime.
     *
     * @return The name of the Proposition.
     */
    public void setName(GdlSentence newName)
    {
        name = newName;
    }

	/**
	 * Returns the current value of the Proposition.
	 *
	 * @see org.ggp.base.util.propnet.architecture.Component#getValue()
	 */
	@Override
	public boolean getValue()
	{
		return value;
	}

	/**
	 * Setter method.
	 *
	 * @param value
	 *            The new value of the Proposition.
	 */
	public void setValue(boolean value, boolean isPropagationDeferred)
	{
		if (value != this.value) {
			this.value = value;
			isChanged = !isChanged;

			if (!isPropagationDeferred) {
				propagate();
			}
		}
	}

	@Override
	public void onInputUpdated(Component updatedInput) {
		this.setValue(updatedInput.getValue(), false);
	}

	public void propagate()
	{
		if (isChanged) {
			Set<Component> outputs = this.getOutputs();
			for (Component o : outputs) {
				o.onInputUpdated(this);
			}

			isChanged = false;
		}
	}

	/**
	 * @see org.ggp.base.util.propnet.architecture.Component#toString()
	 */
	@Override
	public String toString()
	{
		return toDot("circle", value ? "red" : "white", name.toString());
	}

	@Override
	public void forceUpdate() {
		// TODO Auto-generated method stub
		Component i = this.getSingleInput();

		if (!(i instanceof Proposition)) {
			i.forceUpdate();
		}

		value = i.getValue();
	}
}