package com.avairebot.commands.music.playlist;

import com.avairebot.AvaIre;
import com.avairebot.Constants;
import com.avairebot.cache.CacheType;
import com.avairebot.commands.CommandMessage;
import com.avairebot.commands.music.PlaylistCommand;
import com.avairebot.contracts.commands.playlist.PlaylistSubCommand;
import com.avairebot.database.collection.Collection;
import com.avairebot.database.collection.DataRow;
import com.avairebot.database.controllers.PlaylistController;
import com.avairebot.database.transformers.GuildTransformer;
import com.avairebot.utilities.NumberUtil;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class CreatePlaylist extends PlaylistSubCommand {

    public CreatePlaylist(AvaIre avaire, PlaylistCommand command) {
        super(avaire, command);
    }

    @Override
    public boolean onCommand(CommandMessage context, String[] args, GuildTransformer guild, Collection playlists) {
        String name = args[0].trim().split(" ")[0];
        List<DataRow> playlistItems = playlists.whereLoose("name", name);
        if (!playlistItems.isEmpty()) {
            context.makeWarning("The `:playlist` playlist already exists!")
                .set("playlist", name).queue();
            return false;
        }

        if (NumberUtil.isNumeric(name)) {
            context.makeWarning("The playlist can't only be numbers, you have to include some letters in the name!")
                .queue();
            return false;
        }

        int playlistLimit = guild.getType().getLimits().getPlaylist().getPlaylists();
        if (playlists.size() >= playlistLimit) {
            context.makeWarning("The server doesn't have any more playlist slots, you can delete existing playlists to free up slots.").queue();
            return false;
        }

        try {
            storeInDatabase(context, name);

            context.makeSuccess(
                "The `:playlist` playlist has been been created successfully!\nYou can start adding songs to it with `:command :playlist add <song>`"
            ).set("playlist", name).set("command", command.generateCommandTrigger(context.getMessage())).queue();

            return true;
        } catch (SQLException e) {
            e.printStackTrace();
            context.makeError("Error: " + e.getMessage()).queue();
        }

        return false;
    }

    private void storeInDatabase(CommandMessage context, String name) throws SQLException {
        avaire.getDatabase().newQueryBuilder(Constants.MUSIC_PLAYLIST_TABLE_NAME)
            .insert(statement -> {
                statement.set("guild_id", context.getGuild().getId());
                statement.set("name", name, true);
                statement.set("amount", 0);
                statement.set("songs", AvaIre.GSON.toJson(new ArrayList<>()));
            });

        avaire.getCache().getAdapter(CacheType.MEMORY)
            .forget(PlaylistController.getCacheString(context.getGuild()));
    }
}
