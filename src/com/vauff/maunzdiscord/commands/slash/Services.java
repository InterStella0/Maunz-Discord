package com.vauff.maunzdiscord.commands.slash;

import com.vauff.maunzdiscord.commands.templates.AbstractSlashCommand;
import discord4j.core.object.command.ApplicationCommandInteraction;
import discord4j.core.object.entity.User;
import discord4j.core.object.entity.channel.MessageChannel;
import discord4j.discordjson.json.ApplicationCommandRequest;

public class Services extends AbstractSlashCommand<ApplicationCommandInteraction>
{
	@Override
	public String exe(ApplicationCommandInteraction interaction, MessageChannel channel, User author) throws Exception
	{
		return "Responding";
	}

	@Override
	public ApplicationCommandRequest getCommand()
	{
		return ApplicationCommandRequest.builder()
				.name(getName())
				.description("Master command for managing services on a guild")
				.build();
	}

	@Override
	public String getName()
	{
		return "services";
	}

	@Override
	public BotPermission getPermissionLevel()
	{
		return BotPermission.GUILD_ADMIN;
	}
}
