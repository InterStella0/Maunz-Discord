package com.vauff.maunzdiscord.commands.slash;

import com.vauff.maunzdiscord.commands.templates.AbstractSlashCommand;
import com.vauff.maunzdiscord.core.Main;
import discord4j.core.event.domain.interaction.ChatInputInteractionEvent;
import discord4j.core.object.command.ApplicationCommandOption;
import discord4j.core.object.entity.Guild;
import discord4j.core.object.entity.User;
import discord4j.core.object.entity.channel.MessageChannel;
import discord4j.discordjson.json.ApplicationCommandOptionData;
import discord4j.discordjson.json.ApplicationCommandRequest;
import org.jsoup.Jsoup;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.URI;

/**
 * Created by Ramon on 03-Apr-17.
 */

public class IsItDown extends AbstractSlashCommand<ChatInputInteractionEvent>
{
	@Override
	public void exe(ChatInputInteractionEvent event, Guild guild, MessageChannel channel, User author) throws Exception
	{
		String hostname = event.getInteraction().getCommandInteraction().get().getOption("hostname").get().getValue().get().asString();
		boolean isUp;
		String cleanedUri = hostname.replaceAll("(^\\w+:|^)\\/\\/", "").split("/")[0];
		int port;

		try
		{
			URI uri = new URI("my://" + cleanedUri);
			String host = uri.getHost();

			if (uri.getPort() == -1)
			{
				port = getPortByProtocol(hostname);
			}
			else
			{
				port = uri.getPort();
			}

			if (host == null)
			{
				event.editReply("Please specify a valid hostname or URI.").block();
				return;
			}

			isUp = pingHost(host, port, cleanedUri);
		}
		catch (Exception e)
		{
			event.editReply("Please specify a valid hostname or URI.").block();
			return;
		}

		event.editReply((isUp ? ":white_check_mark:" : ":x:") + "**  |  " + cleanedUri + "** is currently **" + (isUp ? "UP**" : "DOWN**")).block();
	}

	/**
	 * Pings a host at a specific port. The ping will be deemed unsuccessful if the socket couldn't connect
	 * to the host within the given timeframe
	 *
	 * @param host The host to ping
	 * @param port The port to ping the host at
	 * @return true if the connection was successful, false otherwise (aka the socket could not connect to the host/port after timeout amount of milliseconds
	 */
	private static boolean pingHost(String host, int port, String uri) throws Exception
	{
		Socket socket = new Socket();

		try
		{
			socket.connect(new InetSocketAddress(host, port), 4000);

			if (port == 80 || port == 443)
			{
				Jsoup.connect((port == 80 ? "http" : "https") + "://" + uri).userAgent(Main.cfg.getUserAgent()).get();
			}

			return true;
		}
		catch (IOException e)
		{
			return false; // Either timeout, unreachable, failed DNS lookup, or bad HTTP status code.
		}
		finally
		{
			socket.close();
		}
	}

	/**
	 * Finds the appropriate port number for the given connection protocol
	 *
	 * @param uri The host to determine the port number for
	 * @return 443 if the protocol is https, 21 if the protocol is ftp, and 80 otherwise
	 */
	private int getPortByProtocol(String uri)
	{
		if (uri.startsWith("https"))
		{
			return 443;
		}

		if (uri.startsWith("ftp"))
		{
			return 21;
		}

		// Assume http
		return 80;
	}

	@Override
	public ApplicationCommandRequest getCommand()
	{
		return ApplicationCommandRequest.builder()
			.name(getName())
			.description("Tells you if the given hostname is down or not")
			.addOption(ApplicationCommandOptionData.builder()
				.name("hostname")
				.description("Hostname to ping")
				.type(ApplicationCommandOption.Type.STRING.getValue())
				.required(true)
				.build())
			.build();
	}

	@Override
	public String getName()
	{
		return "isitdown";
	}

	@Override
	public BotPermission getPermissionLevel()
	{
		return BotPermission.EVERYONE;
	}
}