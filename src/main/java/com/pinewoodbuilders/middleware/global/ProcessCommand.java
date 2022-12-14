/*
 * Copyright (c) 2018.
 *
 * This file is part of Xeus.
 *
 * Xeus is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Xeus is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Xeus.  If not, see <https://www.gnu.org/licenses/>.
 *
 *
 */

package com.pinewoodbuilders.middleware.global;

import com.avairebot.shared.SentryConstants;
import com.pinewoodbuilders.Xeus;
import com.pinewoodbuilders.chat.ConsoleColor;
import com.pinewoodbuilders.commands.AliasCommandContainer;
import com.pinewoodbuilders.commands.CommandMessage;
import com.pinewoodbuilders.contracts.middleware.Middleware;
import com.pinewoodbuilders.factories.MessageFactory;
import com.pinewoodbuilders.metrics.Metrics;
import com.pinewoodbuilders.middleware.MiddlewareStack;
import com.pinewoodbuilders.utilities.ArrayUtil;
import com.pinewoodbuilders.utilities.CheckPermissionUtil;
import com.pinewoodbuilders.utilities.RestActionUtil;
import io.prometheus.client.Histogram;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.exceptions.InsufficientPermissionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.slf4j.MDC.MDCCloseable;

import javax.annotation.Nonnull;
import java.util.Arrays;
import java.util.concurrent.TimeUnit;

public class ProcessCommand extends Middleware {

    private static final Logger log = LoggerFactory.getLogger(ProcessCommand.class);

    private final static String commandOutput = ConsoleColor.format(
        "%cyanExecuting Command \"%reset%command%%cyan\" in \"%reset%category%%cyan\" category in shard %reset%shard%:%reset"
            + "\n\t\t%cyanUser:\t %author%"
            + "\n\t\t%cyanServer:\t %server%"
            + "\n\t\t%cyanChannel: %channel%"
            + "\n\t\t%cyanMessage: %reset%message%");

    private final static String propertyOutput = ConsoleColor.format(
        "%reset%s %cyan[%reset%s%cyan]"
    );

    public ProcessCommand(Xeus avaire) {
        super(avaire);
    }

    @Override
    public boolean handle(@Nonnull Message message, @Nonnull MiddlewareStack stack, String... args) {
        CheckPermissionUtil.PermissionCheckType permissionType = CheckPermissionUtil.canSendMessages(message.getChannel());
        if (!stack.getCommandContainer().getCategory().getName().equals("System") && !permissionType.canSendEmbed()) {
            if (!permissionType.canSendMessage()) {
                return false;
            }

            return runMessageCheck(message, () -> {
                message.getTextChannel().sendMessage("I don't have the `Embed Links` permission, the permission is required for all of my commands to work.\nhttps://xeus.pinewood-builders.com/missing-embed-permissions.png\nThis error can sometimes occur when the `everyone` role has it disabled and no other roles enables it.\nThis message will be automatically deleted in 30 seconds.")
                    .queue(newMessage -> newMessage.delete().queueAfter(30, TimeUnit.SECONDS, null, RestActionUtil.ignore));

                return false;
            });
        }

        String[] arguments = ArrayUtil.toArguments(message.getContentRaw());

        Xeus.getLogger().info(commandOutput
            .replace("%command%", stack.getCommand().getName())
            .replace("%category%", stack.getCommandContainer().getCategory().getName())
            .replace("%author%", generateUsername(message))
            .replace("%server%", generateServer(message))
            .replace("%channel%", generateChannel(message))
            .replace("%message%", message.getContentRaw())
            .replace("%shard%", message.getJDA().getShardInfo().getShardString())
        );

        String[] commandArguments = Arrays.copyOfRange(arguments, stack.isMentionableCommand() ? 2 : 1, arguments.length);
        if (stack.getCommandContainer() instanceof AliasCommandContainer) {
            AliasCommandContainer container = (AliasCommandContainer) stack.getCommandContainer();

            return runCommand(stack,
                new CommandMessage(
                    stack.getCommandContainer(),
                    stack.getDatabaseEventHolder(),
                    message,
                    stack.isMentionableCommand(),
                    container.getAliasArguments()
                ),
                container.getAliasArguments()
            );
        }

        Metrics.commandsExecuted.labels(stack.getCommand().getClass().getSimpleName()).inc();

        return runCommand(stack,
            new CommandMessage(
                stack.getCommandContainer(),
                stack.getDatabaseEventHolder(),
                message,
                stack.isMentionableCommand(),
                new String[0]
            ),
            commandArguments
        );
    }

    private boolean runCommand(MiddlewareStack stack, CommandMessage context, String[] args) {
        try (
            MDCCloseable _guild = MDC.putCloseable(SentryConstants.SENTRY_MDC_TAG_GUILD, context.isGuildMessage() ? context.getGuild().getId() : "PRIVATE");
            MDCCloseable _shard = MDC.putCloseable(SentryConstants.SENTRY_MDC_TAG_SHARD, context.getJDA().getShardInfo().getShardString());
            MDCCloseable _channel = MDC.putCloseable(SentryConstants.SENTRY_MDC_TAG_CHANNEL, context.isGuildMessage() ? context.getChannel().getId() : "PRIVATE");
            MDCCloseable _author = MDC.putCloseable(SentryConstants.SENTRY_MDC_TAG_AUTHOR, context.getAuthor().getId());
            MDCCloseable _message = MDC.putCloseable(SentryConstants.SENTRY_MDC_TAG_MESSAGE, context.getMessage().getContentRaw())
        ) {
            Histogram.Timer timer = Metrics.executionTime.labels(stack.getCommand().getClass().getSimpleName()).startTimer();

            try {
                return stack.getCommand().onCommand(context, args);
            } catch (Exception ex) {
                Metrics.commandExceptions.labels(ex.getClass().getSimpleName()).inc();

                if (ex instanceof InsufficientPermissionException) {
                    MessageFactory.makeError(context.getMessage(), "Error: " + ex.getMessage())
                        .queue(newMessage -> newMessage.delete().queueAfter(30, TimeUnit.SECONDS, null, RestActionUtil.ignore));

                    return false;
                }

                log.error("An error occurred while running the " + stack.getCommand().getName(), ex);
                return false;
            } finally {
                if (timer != null) {
                    timer.observeDuration();
                }
            }
        }
    }

    private String generateUsername(Message message) {
        return String.format(propertyOutput,
            message.getAuthor().getName() + "#" + message.getAuthor().getDiscriminator(),
            message.getAuthor().getId()
        );
    }

    private String generateServer(Message message) {
        if (!message.getChannelType().isGuild()) {
            return "PRIVATE";
        }

        return String.format(propertyOutput,
            message.getGuild().getName(),
            message.getGuild().getId()
        );
    }

    private CharSequence generateChannel(Message message) {
        if (!message.getChannelType().isGuild()) {
            return "PRIVATE";
        }

        return String.format(propertyOutput,
            message.getChannel().getName(),
            message.getChannel().getId()
        );
    }

    private String[] combineArguments(String[] aliasArguments, String[] userArguments) {
        int length = aliasArguments.length + userArguments.length;

        String[] result = new String[length];

        System.arraycopy(aliasArguments, 0, result, 0, aliasArguments.length);
        System.arraycopy(userArguments, 0, result, aliasArguments.length, userArguments.length);

        return aliasArguments;
    }
}
