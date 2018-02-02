package com.avairebot.commands.administration;

import com.avairebot.AvaIre;
import com.avairebot.commands.CommandMessage;
import com.avairebot.contracts.commands.Command;
import com.avairebot.utilities.MentionableUtil;
import net.dv8tion.jda.core.entities.User;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class UserIdCommand extends Command {

    public UserIdCommand(AvaIre avaire) {
        super(avaire, false);
    }

    @Override
    public String getName() {
        return "User ID Command";
    }

    @Override
    public String getDescription() {
        return "Shows your Discord account user ID, or the ID of the user tagged in the command.";
    }

    @Override
    public List<String> getUsageInstructions() {
        return Collections.singletonList("`:command [user]`");
    }

    @Override
    public List<String> getExampleUsage() {
        return Arrays.asList(
            "`:command @Senither`",
            "`:command alexis`",
            "`:command 88739639380172800`"
        );
    }

    @Override
    public List<String> getTriggers() {
        return Arrays.asList("userid", "uid");
    }

    @Override
    public boolean onCommand(CommandMessage context, String[] args) {
        User user = context.getAuthor();
        if (args.length > 0) {
            user = MentionableUtil.getUser(context.getMessage(), args);
        }

        if (user == null) {
            return sendErrorMessage(context, "I found no users with the name or ID of `%s`", args[0]);
        }

        context.makeSuccess(":id: of the user **:target** is `:targetid`")
            .set("target", user.getAsMention())
            .set("targetid", user.getId())
            .queue();
        
        return true;
    }
}
