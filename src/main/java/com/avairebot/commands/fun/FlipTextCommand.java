package com.avairebot.commands.fun;

import com.avairebot.AvaIre;
import com.avairebot.commands.CommandMessage;
import com.avairebot.contracts.commands.Command;

import java.util.Collections;
import java.util.List;

public class FlipTextCommand extends Command {

    private static final String NORMAL = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz_,;.?!/\\'0123456789";
    private static final String FLIPPED = "∀qϽᗡƎℲƃHIſʞ˥WNOԀὉᴚS⊥∩ΛMXʎZɐqɔpǝɟbɥıظʞןɯuodbɹsʇnʌʍxʎz‾'؛˙¿¡/\\,0ƖᄅƐㄣϛ9ㄥ86 ";

    public FlipTextCommand(AvaIre avaire) {
        super(avaire);
    }

    @Override
    public String getName() {
        return "Flip Text Command";
    }

    @Override
    public String getDescription() {
        return "Flips the given message upside down.";
    }

    @Override
    public List<String> getUsageInstructions() {
        return Collections.singletonList("`:command <message>` - Flips the given message.");
    }

    @Override
    public List<String> getExampleUsage() {
        return Collections.singletonList("`:command This is some random message`");
    }

    @Override
    public List<String> getTriggers() {
        return Collections.singletonList("flip");
    }

    @Override
    public boolean onCommand(CommandMessage context, String[] args) {
        if (args.length == 0) {
            return sendErrorMessage(context, "Missing argument `message`, you must include a message.");
        }

        String string = String.join(" ", args);
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < string.length(); i++) {
            char letter = string.charAt(i);

            int a = NORMAL.indexOf(letter);
            builder.append((a != -1) ? FLIPPED.charAt(a) : letter);
        }

        context.makeInfo(builder.toString()).queue();
        return true;
    }
}
