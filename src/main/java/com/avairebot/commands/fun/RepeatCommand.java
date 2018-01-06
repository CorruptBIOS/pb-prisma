package com.avairebot.commands.fun;

import com.avairebot.AvaIre;
import com.avairebot.contracts.commands.Command;
import net.dv8tion.jda.core.entities.Message;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class RepeatCommand extends Command {

    public RepeatCommand(AvaIre avaire) {
        super(avaire);
    }

    @Override
    public String getName() {
        return "Repeat Command";
    }

    @Override
    public String getDescription() {
        return "I will repeat anything you say.";
    }

    @Override
    public List<String> getUsageInstructions() {
        return Collections.singletonList("`:command <message>` - Repeats the given message");
    }

    @Override
    public List<String> getExampleUsage() {
        return Collections.singletonList("`:command I am a BOT`");
    }

    @Override
    public List<String> getTriggers() {
        return Arrays.asList("repeat", "echo");
    }

    @Override
    public boolean onCommand(Message message, String[] args) {
        if (args.length == 0) {
            return sendErrorMessage(message, "Missing `message` argument, the `message` argument is required!");
        }

        String[] split = message.getContent().split(" ");
        message.getChannel().sendMessage(String.join(" ", Arrays.copyOfRange(split, 1, split.length))).queue();

        return true;
    }
}