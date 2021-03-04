package com.vauff.maunzdiscord.commands.templates;

import discord4j.common.util.Snowflake;

/**
 * Holds information about a message that needs a reaction/reply to continue further execution
 */
public class Await
{
	private Snowflake id;
	private AbstractCommand command;

	/**
	 * @param anID An ID of a user who triggered the message or a message to be removed later on
	 * @param cmd  The command with which to continue execution upon adding a reaction
	 */
	public Await(Snowflake anID, AbstractCommand cmd)
	{
		id = anID;
		command = cmd;
	}

	/**
	 * @return The ID of the user who triggered the message or of the message to be removed later on
	 */
	public Snowflake getID()
	{
		return id;
	}

	/**
	 * @return The command with which to continue execution upon adding a reaction
	 */
	public AbstractCommand getCommand()
	{
		return command;
	}
}
