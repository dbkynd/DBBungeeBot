package com.dbkynd.DBBungeeBot.bot.command;

import com.dbkynd.DBBungeeBot.Main;
import com.dbkynd.DBBungeeBot.bot.Command;
import com.dbkynd.DBBungeeBot.bot.ServerBot;
import com.dbkynd.DBBungeeBot.http.ImageDownloader;
import com.dbkynd.DBBungeeBot.mojang.MojangJSON;
import com.dbkynd.DBBungeeBot.http.WebRequest;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;

import java.util.logging.Level;

public class AddMeCommand implements Command {

    private ServerBot bot;

    private final String HELP;

    Main plugin;
    WebRequest request = new WebRequest();

    public AddMeCommand(ServerBot bot) {
        this.bot = bot;
        plugin = bot.getPlugin();
        HELP = "USAGE: " + plugin.getCommandPrefix() + plugin.getAddMeCommand() + " <username>";
    }

    @Override
    public boolean called(String[] args, MessageReceivedEvent event) {
        return true;
    }

    @Override
    public void action(String[] args, MessageReceivedEvent event) {
        if (args.length == 1) {
            String name = args[0];
            if (name.length() > 16) {
                return;
            }
            bot.log(Level.INFO, event.getAuthor().getName() + " issued a Discord Bot command: " + plugin.getCommandPrefix() + plugin.getAddMeCommand() + " " + name);

            // Get the user data from the Mojang API
            MojangJSON mojang = request.getMojangData(name);

            // Tell the discord member we cannot find any data for the username they submitted
            if (mojang == null || mojang.getId() == null) {
                event.getChannel().sendMessage("Unable to get Mojang data for username **" + args[0] + "**.").queue();
                return;
            }

            String thumbnail = "https://crafatar.com/renders/body/" + mojang.getId();
            // Try to download the image so that it's in craftatar's cache for when Discord asks for it to embed.
            // I think when discord tries to embed the image it doesn't wait for it to be generated by craftatar
            // like it would for a web client. And therefor even though the url is valid, Discord was not showing the thumbnail
            // reliably, but now seems to. :D
            ImageDownloader.main(thumbnail);

            // Save to database
            try {
                if (bot.getSQL().itemExists("DiscordID", event.getAuthor().getId(), bot.getTableName())) {
                    bot.getSQL().set("MinecraftName", mojang.getName().toLowerCase(), "DiscordID", "=", event.getAuthor().getId(), bot.getTableName());
                    bot.getSQL().set("UUID", mojang.getUUID(), "DiscordID", "=", event.getAuthor().getId(), bot.getTableName());
                } else {
                    bot.getSQL().update("INSERT INTO " + bot.getTableName() + " (DiscordID,MinecraftName,UUID) VALUES (\'" + event.getAuthor().getId() + "\',\'" + mojang.getName().toLowerCase() + "\',\'" + mojang.getUUID().toString() + "\');");
                }
            } catch (Exception e) {
                e.printStackTrace();
                event.getChannel().sendMessage("There was an error updating the Minecraft user database with username **" + mojang.getName() + "**.").queue();
            }

            // Tell the Discord member that everything worked as expected!
            EmbedBuilder builder = new EmbedBuilder();
            builder.setDescription("```" + mojang.getName() + "```\nhas been added to the Minecraft user database!");
            builder.setThumbnail(thumbnail);
            builder.setColor(48640);
            MessageEmbed embed = builder.build();
            event.getChannel().sendMessage(embed).queue();
        }
    }

    @Override
    public String help() {
        return HELP;
    }

    @Override
    public void executed(boolean success, MessageReceivedEvent event) {
        return;
    }
}