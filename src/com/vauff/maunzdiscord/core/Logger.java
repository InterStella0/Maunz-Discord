package com.vauff.maunzdiscord.core;

import discord4j.core.event.domain.guild.GuildCreateEvent;
import discord4j.core.event.domain.guild.GuildDeleteEvent;
import discord4j.core.event.domain.message.MessageCreateEvent;
import discord4j.core.event.domain.message.MessageDeleteEvent;
import discord4j.core.event.domain.message.MessageUpdateEvent;
import discord4j.core.event.domain.message.ReactionAddEvent;
import discord4j.core.event.domain.message.ReactionRemoveEvent;
import discord4j.core.object.Embed;
import discord4j.core.object.entity.Attachment;
import discord4j.core.object.entity.GuildChannel;
import discord4j.core.object.entity.PrivateChannel;

public class Logger
{
	public static org.apache.logging.log4j.Logger log;

	public static void onMessageCreate(MessageCreateEvent event)
	{
		if (event.getMessage().getAuthor().isPresent())
		{
			String userName = event.getMessage().getAuthor().get().getUsername();
			String userId = event.getMessage().getAuthor().get().getId().asString();
			String channelId = event.getMessage().getChannelId().asString();
			String messageID = event.getMessage().getId().asString();
			String msg;

			if (!(event.getMessage().getChannel().block() instanceof PrivateChannel))
			{
				String channelName = ((GuildChannel) event.getMessage().getChannel().block()).getName();
				String guildName = event.getGuild().block().getName();
				String guildId = event.getGuild().block().getId().asString();

				msg = messageID + " | " + userName + " (" + userId + ") | " + guildName + " (" + guildId + ") | #" + channelName + " (" + channelId + ") | ";
			}
			else
			{
				PrivateChannel channel = (PrivateChannel) event.getMessage().getChannel().block();
				String recipientName = channel.getRecipients().next().block().getUsername();
				String recipientId = channel.getRecipients().next().block().getId().asString();

				if (recipientId.equals(userId))
				{
					msg = messageID + " | " + userName + " (" + userId + ") | PM (" + channelId + ") | ";
				}
				else
				{
					msg = messageID + " | " + userName + " (" + userId + ") | " + recipientName + " (" + recipientId + ") | PM (" + channelId + ") | ";
				}
			}

			if (event.getMessage().getContent().isPresent())
			{
				msg += event.getMessage().getContent().get();
			}

			for (Attachment attachment : event.getMessage().getAttachments())
			{
				msg += "[Attachment " + attachment.getUrl() + "]";
			}
			for (Embed embed : event.getMessage().getEmbeds())
			{
				msg += "[Embed]";
			}

			Logger.log.debug(msg);
		}
	}

	public static void onMessageUpdate(MessageUpdateEvent event)
	{
		if (event.getMessage().block().getAuthor().isPresent() && event.getMessage().block().getContent().isPresent())
		{
			String userName = event.getMessage().block().getAuthor().get().getUsername();
			String userId = event.getMessage().block().getAuthor().get().getId().asString();
			String messageID = event.getMessage().block().getId().asString();
			String message = event.getMessage().block().getContent().get();

			Logger.log.debug(userName + " (" + userId + ") edited the message ID " + messageID + " to \"" + message + "\"");
		}
	}

	public static void onMessageDelete(MessageDeleteEvent event)
	{
		if (event.getMessage().isPresent() && event.getMessage().get().getAuthor().isPresent())
		{
			String userName = event.getMessage().get().getAuthor().get().getUsername();
			String userId = event.getMessage().get().getAuthor().get().getId().asString();
			String messageID = event.getMessage().get().getId().asString();

			Logger.log.debug(userName + " (" + userId + ") deleted the message ID " + messageID);
		}
	}

	public static void onReactionAdd(ReactionAddEvent event)
	{
		String userName = event.getUser().block().getUsername();
		String userId = event.getUser().block().getId().asString();
		String messageID = event.getMessage().block().getId().asString();
		String reaction = "null";

		if (event.getEmoji().asUnicodeEmoji().isPresent())
		{
			reaction = event.getEmoji().asUnicodeEmoji().get().getRaw();
		}
		else if (event.getEmoji().asCustomEmoji().isPresent())
		{
			reaction = ":" + event.getEmoji().asCustomEmoji().get().getName() + ":";
		}

		Logger.log.debug(userName + " (" + userId + ") added the reaction " + reaction + " to the message ID " + messageID);
	}

	public static void onReactionRemove(ReactionRemoveEvent event)
	{
		String userName = event.getUser().block().getUsername();
		String userId = event.getUser().block().getId().asString();
		String messageID = event.getMessage().block().getId().asString();
		String reaction = "null";

		if (event.getEmoji().asUnicodeEmoji().isPresent())
		{
			reaction = event.getEmoji().asUnicodeEmoji().get().getRaw();
		}
		else if (event.getEmoji().asCustomEmoji().isPresent())
		{
			reaction = ":" + event.getEmoji().asCustomEmoji().get().getName() + ":";
		}

		Logger.log.debug(userName + " (" + userId + ") removed the reaction " + reaction + " from the message ID " + messageID);
	}

	public static void onGuildCreate(GuildCreateEvent event)
	{
		Logger.log.debug("Joined guild " + event.getGuild().getName() + " (" + event.getGuild().getId().asString() + ")");
	}

	public static void onGuildDelete(GuildDeleteEvent event)
	{
		if (!event.isUnavailable())
		{
			Logger.log.debug("Left guild " + event.getGuild().get().getName() + " (" + event.getGuildId().asString() + ")");
		}
	}
}