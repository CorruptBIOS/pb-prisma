package com.avairebot.commands.administration;

import com.avairebot.AvaIre;
import com.avairebot.commands.CommandMessage;
import com.avairebot.contracts.commands.Command;
import com.avairebot.utilities.NumberUtil;
import net.dv8tion.jda.core.entities.Message;
import net.dv8tion.jda.core.entities.MessageHistory;
import net.dv8tion.jda.core.entities.User;
import net.dv8tion.jda.core.requests.RestAction;
import net.dv8tion.jda.core.utils.MiscUtil;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

public class PurgeCommand extends Command {

    private static final int MAX_HISTORY_LOOPS = 25;

    public PurgeCommand(AvaIre avaire) {
        super(avaire, false);
    }

    @Override
    public String getName() {
        return "Purge Command";
    }

    @Override
    public String getDescription() {
        return "Deletes up to 100 chat messages in any channel, you can mention a user if you only want to delete messages by the mentioned user.";
    }

    @Override
    public List<String> getUsageInstructions() {
        return Arrays.asList(
            "`:command` - Deletes the last 5 messages.",
            "`:command [number]` - Deletes the given number of messages.",
            "`:command [number] [user]` - Deletes the given number of messages for the mentioned users."
        );
    }

    @Override
    public List<String> getExampleUsage() {
        return Arrays.asList(
            "`:command 56`",
            "`:command 30 @Senither`"
        );
    }

    @Override
    public List<String> getTriggers() {
        return Arrays.asList("purge", "clear");
    }

    @Override
    public List<String> getMiddleware() {
        return Arrays.asList(
            "require:user,text.manage_messages",
            "require:bot,text.manage_messages,text.read_message_history",
            "throttle:channel,1,5"
        );
    }

    @Override
    public boolean onCommand(CommandMessage context, String[] args) {
        int toDelete = 100;
        if (args.length > 0) {
            toDelete = NumberUtil.getBetween(NumberUtil.parseInt(args[0]), 1, 100);
        }

        context.getChannel().sendTyping().queue();
        if (context.getMentionedUsers().isEmpty()) {
            loadMessages(context.getChannel().getHistory(), toDelete, new ArrayList<>(), null, 1, messages -> {
                if (messages.isEmpty()) {
                    sendNoMessagesMessage(context);
                    return;
                }

                deleteMessages(context, messages).queue(aVoid ->
                    context.makeSuccess(":white_check_mark: `:number` messages has been deleted!")
                        .set("number", messages.size())
                        .queue(successMessage -> successMessage.delete().queueAfter(8, TimeUnit.SECONDS)));
            });
            return true;
        }

        List<Long> userIds = new ArrayList<>();
        for (User user : context.getMentionedUsers()) {
            userIds.add(user.getIdLong());
        }

        loadMessages(context.getChannel().getHistory(), toDelete, new ArrayList<>(), userIds, 1, messages -> {
            if (messages.isEmpty()) {
                sendNoMessagesMessage(context);
                return;
            }

            deleteMessages(context, messages).queue(aVoid -> {
                List<String> users = new ArrayList<>();
                for (Long userId : userIds) {
                    users.add(String.format("<@%s>", userId));
                }

                context.makeSuccess(":white_check_mark: `:number` messages has been deleted from :users")
                    .set("number", messages.size())
                    .set("users", String.join(", ", users))
                    .queue(successMessage -> successMessage.delete().queueAfter(8, TimeUnit.SECONDS));
            });
        });
        return true;
    }

    private void loadMessages(MessageHistory history, int toDelete, List<Message> messages, List<Long> userIds, int loops, Consumer<List<Message>> consumer) {
        long maxMessageAge = (System.currentTimeMillis() - TimeUnit.DAYS.toMillis(14) - MiscUtil.DISCORD_EPOCH) << MiscUtil.TIMESTAMP_OFFSET;

        history.retrievePast(100).queue(historyMessages -> {
            if (historyMessages.isEmpty()) {
                consumer.accept(messages);
                return;
            }

            for (Message historyMessage : historyMessages) {
                if (historyMessage.isPinned() || historyMessage.getIdLong() < maxMessageAge) {
                    continue;
                }

                if (userIds != null && !userIds.contains(historyMessage.getAuthor().getIdLong())) {
                    continue;
                }

                if (messages.size() >= toDelete || loops > MAX_HISTORY_LOOPS) {
                    consumer.accept(messages);
                    return;
                }

                messages.add(historyMessage);
            }

            loadMessages(history, toDelete, messages, userIds, loops + 1, consumer);
        });
    }

    private void sendNoMessagesMessage(CommandMessage context) {
        context.makeSuccess(
            ":x: Nothing to delete, I am unable to delete messages older than 14 days."
        ).queue(successMessage -> successMessage.delete().queueAfter(8, TimeUnit.SECONDS));
    }

    private RestAction<Void> deleteMessages(CommandMessage context, List<Message> messages) {
        if (messages.size() == 1) {
            return messages.get(0).delete();
        }
        return context.getChannel().deleteMessages(messages);
    }
}
