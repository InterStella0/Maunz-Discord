package com.vauff.maunzdiscord.threads;

import com.github.koraktor.steamcondenser.servers.GameServer;
import com.github.koraktor.steamcondenser.servers.SourceServer;
import com.vauff.maunzdiscord.core.Logger;
import com.vauff.maunzdiscord.core.Main;
import com.vauff.maunzdiscord.timers.ServerTimer;
import org.bson.Document;
import org.bson.types.ObjectId;

import java.net.InetAddress;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;

import static com.mongodb.client.model.Filters.eq;

public class ServerRequestThread implements Runnable
{
	/**
	 * Cached GameServer objects to avoid constant construction, because it's *very* expensive due to an unavoidable bottleneck in InetAddress.getByName
	 */
	private static HashMap<String, GameServer> servers = new HashMap<>();
	private Thread thread;
	private ObjectId id;
	private String ipPort;
	private GameServer server;

	public ServerRequestThread(ObjectId id, String ipPort)
	{
		this.id = id;
		this.ipPort = ipPort;
	}

	public void start()
	{
		if (thread == null)
		{
			thread = new Thread(this, "servertracking-" + ipPort);
			thread.start();
		}
	}

	public void run()
	{
		try
		{
			Document doc = Main.mongoDatabase.getCollection("servers").find(eq("_id", id)).first();
			int attempts = 0;
			boolean validServer = false;
			boolean serverInfoSuccess = false;
			boolean retriedForCsgoPlayerCount = false;

			while (true)
			{
				try
				{
					if (servers.containsKey(ipPort))
					{
						this.server = servers.get(ipPort);
					}
					else
					{
						server = new SourceServer(InetAddress.getByName(doc.getString("ip")), doc.getInteger("port"));
						servers.put(ipPort, server);
					}

					validServer = true;

					if (!serverInfoSuccess)
					{
						server.updateServerInfo();
						serverInfoSuccess = true;
					}

					// CS:GO servers by default use host_info_show 1 which uses a game-specific A2S_INFO implementation, only host_info_show 2 uses SteamWorks
					// Unfortunately this implementation is incapable of providing a correct player count during a map change (returns 0), so we work around this by double-checking "empty" CS:GO servers are actually empty a little while after
					// See https://github.com/perilouswithadollarsign/cstrike15_src/blob/master/engine/baseserver.cpp#L1261, GetNumPlayers() uses m_pUserInfoTable which is emptied during a map change
					if (doc.getInteger("appId") == 730 && server.getServerInfo().containsKey("numberOfPlayers") && !Objects.isNull(server.getServerInfo().get("numberOfPlayers")) && ((Byte) server.getServerInfo().get("numberOfPlayers")).intValue() == 0 && !retriedForCsgoPlayerCount)
					{
						Thread.sleep(5000);
						retriedForCsgoPlayerCount = true;
						continue;
					}

					server.updatePlayers();
					Main.mongoDatabase.getCollection("servers").updateOne(eq("_id", id), new Document("$set", new Document("players", server.getPlayers().keySet())));
					break;
				}
				catch (Exception e)
				{
					attempts++;

					if (!validServer || attempts >= 5 || (!serverInfoSuccess && doc.getInteger("downtimeTimer") >= doc.getInteger("failedConnectionsThreshold")))
					{
						if (!serverInfoSuccess)
						{
							int downtimeTimer = doc.getInteger("downtimeTimer") + 1;

							Logger.log.warn("Failed to connect to the server " + ipPort + ", automatically retrying in 1 minute");
							Main.mongoDatabase.getCollection("servers").updateOne(eq("_id", id), new Document("$set", new Document("downtimeTimer", downtimeTimer)));

							if (downtimeTimer >= 10080)
								Main.mongoDatabase.getCollection("servers").updateOne(eq("_id", id), new Document("$set", new Document("enabled", false)));

							cleanup(true);
							return;
						}
						else
						{
							Logger.log.warn("Failed to retrieve player information from " + ipPort + ", automatically retrying in 1 minute");
							Main.mongoDatabase.getCollection("servers").updateOne(eq("_id", id), new Document("$set", new Document("players", List.of("SERVER_UPDATEPLAYERS_FAILED"))));
							break;
						}
					}
					else
					{
						Thread.sleep(1000);
					}
				}
			}

			HashMap<String, Object> serverInfo = server.getServerInfo();
			long timestamp = 0;
			int appId = 0;
			String map = "";
			String name = "N/A";
			int currentPlayers = 0;
			int maxPlayers = 0;

			if (serverInfo.containsKey("mapName") && !Objects.isNull(serverInfo.get("mapName")))
			{
				map = serverInfo.get("mapName").toString();
			}
			else
			{
				Logger.log.warn("Null mapname received for server " + ipPort + ", automatically retrying in 1 minute");
				cleanup(false);
				return;
			}

			// 24-bit app id within 64-bit game id, may not be available
			if (serverInfo.containsKey("gameId") && !Objects.isNull(serverInfo.get("gameId")))
				appId = (int) (((long) serverInfo.get("gameId")) & (1L << 24) - 1L);

			// 16-bit app id, possibly truncated but (theoretically) always available
			else if (serverInfo.containsKey("appId") && !Objects.isNull(serverInfo.get("appId")))
				appId = (short) serverInfo.get("appId");

			if (serverInfo.containsKey("serverName") && !Objects.isNull(serverInfo.get("serverName")))
				name = serverInfo.get("serverName").toString();

			if (serverInfo.containsKey("numberOfPlayers") && !Objects.isNull(serverInfo.get("numberOfPlayers")))
				currentPlayers = ((Byte) serverInfo.get("numberOfPlayers")).intValue();

			if (serverInfo.containsKey("maxPlayers") && !Objects.isNull(serverInfo.get("maxPlayers")))
				maxPlayers = ((Byte) serverInfo.get("maxPlayers")).intValue();

			if (currentPlayers > maxPlayers && maxPlayers >= 0)
				currentPlayers = maxPlayers;

			String playerCount = currentPlayers + "/" + maxPlayers;

			if (!map.equals("") && !map.equalsIgnoreCase(doc.getString("map")))
			{
				Main.mongoDatabase.getCollection("servers").updateOne(eq("_id", id), new Document("$set", new Document("map", map)));
				timestamp = System.currentTimeMillis();

				boolean mapFound = false;

				for (int i = 0; i < doc.getList("mapDatabase", Document.class).size(); i++)
				{
					String dbMap = doc.getList("mapDatabase", Document.class).get(i).getString("map");

					if (dbMap.equalsIgnoreCase(map))
					{
						mapFound = true;
						Main.mongoDatabase.getCollection("servers").updateOne(eq("_id", id), new Document("$set", new Document("mapDatabase." + i + ".lastPlayed", timestamp)));
						break;
					}
				}

				if (!mapFound)
				{
					Document mapDoc = new Document();
					mapDoc.put("map", map);
					mapDoc.put("firstPlayed", timestamp);
					mapDoc.put("lastPlayed", timestamp);
					Main.mongoDatabase.getCollection("servers").updateOne(eq("_id", id), new Document("$push", new Document("mapDatabase", mapDoc)));
				}
			}

			if (appId != 0 && appId != doc.getInteger("appId"))
				Main.mongoDatabase.getCollection("servers").updateOne(eq("_id", id), new Document("$set", new Document("appId", appId)));

			if (!playerCount.equals("") && !playerCount.equals(doc.getString("playerCount")))
				Main.mongoDatabase.getCollection("servers").updateOne(eq("_id", id), new Document("$set", new Document("playerCount", playerCount)));

			if (timestamp != 0)
				Main.mongoDatabase.getCollection("servers").updateOne(eq("_id", id), new Document("$set", new Document("timestamp", timestamp)));

			if (!name.equals("") && !name.equals(doc.getString("name")))
				Main.mongoDatabase.getCollection("servers").updateOne(eq("_id", id), new Document("$set", new Document("name", name)));

			if (doc.getInteger("downtimeTimer") != 0)
				Main.mongoDatabase.getCollection("servers").updateOne(eq("_id", id), new Document("$set", new Document("downtimeTimer", 0)));

			cleanup(true);
		}
		catch (Exception e)
		{
			Logger.log.error("", e);
			cleanup(false);
		}
		finally
		{
			ServerTimer.threadRunning.put(id, false);
		}
	}

	private void cleanup(boolean success)
	{
		List<ServiceProcessThread> processThreads = new ArrayList<>(ServerTimer.waitingProcessThreads.get(id));

		if (success)
		{
			for (ServiceProcessThread processThread : processThreads)
			{
				processThread.start();

				// TODO: replace this awful workaround with a new scheduler
				// only start ~10 threads per second
				try
				{
					Thread.sleep(100);
				}
				catch (InterruptedException e)
				{
					Logger.log.error("", e);
				}
			}
		}
		else
		{
			for (ServiceProcessThread processThread : processThreads)
				ServerTimer.threadRunning.put(processThread.id, false);
		}

		ServerTimer.waitingProcessThreads.get(id).clear();
	}
}
