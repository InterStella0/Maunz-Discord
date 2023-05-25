package com.vauff.maunzdiscord.commands;

import com.vauff.maunzdiscord.commands.templates.AbstractCommand;
import com.vauff.maunzdiscord.core.Util;
import discord4j.core.event.domain.interaction.ChatInputInteractionEvent;
import discord4j.core.object.command.ApplicationCommandInteraction;
import discord4j.core.object.command.ApplicationCommandOption;
import discord4j.core.object.entity.Attachment;
import discord4j.core.object.entity.User;
import discord4j.core.object.entity.channel.MessageChannel;
import discord4j.core.spec.EmbedCreateSpec;
import discord4j.discordjson.json.ApplicationCommandOptionData;
import discord4j.discordjson.json.ApplicationCommandRequest;
import discord4j.rest.util.Color;

import java.net.MalformedURLException;

public class Colour extends AbstractCommand<ChatInputInteractionEvent>
{
	@Override
	public void exe(ChatInputInteractionEvent event, MessageChannel channel, User user) throws Exception
	{
		ApplicationCommandInteraction interaction = event.getInteraction().getCommandInteraction().get();
		String url = "";

		if (interaction.getOption("image").isPresent())
		{
			for (Attachment attachment : interaction.getResolved().get().getAttachments().values())
			{
				if (attachment.getUrl().endsWith(".png") || attachment.getUrl().endsWith(".jpg") || attachment.getUrl().endsWith(".jpeg"))
				{
					url = attachment.getUrl();
				}
			}
		}
		else if (interaction.getOption("link").isPresent())
		{
			url = interaction.getOption("link").get().getValue().get().asString();
		}
		else
		{
			Util.editReply(event, "You need to provide an image attachment or link!");
			return;
		}

		if (!url.equals(""))
		{
			Color colour;

			try
			{
				colour = Util.averageColourFromURL(url, false);

				if (colour == null)
				{
					Util.editReply(event, "Could not get an image from the provided attachment or link!");
					return;
				}
			}
			catch (MalformedURLException e)
			{
				Util.editReply(event, "Could not get an image from the provided attachment or link!");
				return;
			}

			EmbedCreateSpec embed = EmbedCreateSpec.builder()
				.color(colour)
				.thumbnail(url)
				.title("Average Image Colour")
				.addField("RGB", colour.getRed() + ", " + colour.getGreen() + ", " + colour.getBlue(), true)
				.addField("HTML/Hex", String.format("#%02X%02X%02X", colour.getRed(), colour.getGreen(), colour.getBlue()), true)
				.build();

			Util.editReply(event, "", embed);
		}
		else
		{
			Util.editReply(event, "Could not get an image from the provided attachment or link!");
		}
	}

	@Override
	public ApplicationCommandRequest getCommandRequest()
	{
		return ApplicationCommandRequest.builder()
			.name(getName())
			.description("Returns the average RGB and HTML/Hex colour codes of an image attachment or link")
			.addOption(ApplicationCommandOptionData.builder()
				.name("image")
				.description("Image attachment")
				.type(ApplicationCommandOption.Type.ATTACHMENT.getValue())
				.build())
			.addOption(ApplicationCommandOptionData.builder()
				.name("link")
				.description("Image link")
				.type(ApplicationCommandOption.Type.STRING.getValue())
				.build())
			.build();
	}

	@Override
	public String getName()
	{
		return "colour";
	}

	@Override
	public BotPermission getPermissionLevel()
	{
		return BotPermission.EVERYONE;
	}
}