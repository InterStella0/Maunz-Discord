package com.vauff.maunzdiscord.threads;

import com.vauff.maunzdiscord.commands.templates.AbstractSlashCommand;
import com.vauff.maunzdiscord.core.Logger;
import com.vauff.maunzdiscord.core.Main;
import discord4j.core.event.domain.interaction.ButtonInteractionEvent;
import discord4j.core.event.domain.interaction.ChatInputInteractionEvent;
import discord4j.core.object.component.Button;

import java.util.Random;

public class ButtonInteractionThread implements Runnable
{
	private ButtonInteractionEvent event;
	private Thread thread;
	private String name;

	public ButtonInteractionThread(ButtonInteractionEvent passedEvent, String passedName)
	{
		name = passedName;
		event = passedEvent;
	}

	public void start()
	{
		if (thread == null)
		{
			thread = new Thread(this, name);
			thread.start();
		}
	}

	public void run()
	{
		try
		{
			event.deferReply().block();

			String buttonId = event.getCustomId();

			for (AbstractSlashCommand<ChatInputInteractionEvent> cmd : Main.slashCommands)
			{
				for (Button button : cmd.getButtons())
				{
					if (!button.getCustomId().get().equals(buttonId))
						continue;

					if (event.getMessage().isPresent())
						event.getMessage().get().delete().block();

					try
					{
						cmd.buttonExe(event, buttonId);
					}
					catch (Exception e)
					{
						Random rnd = new Random();
						int code = 100000000 + rnd.nextInt(900000000);

						event.editReply(":exclamation:  |  **An error has occured!**" + System.lineSeparator() + System.lineSeparator() + "If this was an unexpected error, please report it to Vauff in the #bugreports channel at http://discord.gg/MDx3sMz with the error code " + code).block();
						Logger.log.error(code, e);
					}

					return;
				}
			}
		}
		catch (Exception e)
		{
			Logger.log.error("", e);
		}
	}
}