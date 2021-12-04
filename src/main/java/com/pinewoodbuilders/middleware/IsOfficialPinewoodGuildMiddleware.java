package com.pinewoodbuilders.middleware;

import com.pinewoodbuilders.Xeus;
import com.pinewoodbuilders.Constants;
import com.pinewoodbuilders.commands.CommandMessage;
import com.pinewoodbuilders.contracts.middleware.Middleware;
import com.pinewoodbuilders.factories.MessageFactory;
import com.pinewoodbuilders.utilities.XeusPermissionUtil;
import com.pinewoodbuilders.utilities.RestActionUtil;
import net.dv8tion.jda.api.entities.Message;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.concurrent.TimeUnit;

public class IsOfficialPinewoodGuildMiddleware extends Middleware {

    public IsOfficialPinewoodGuildMiddleware(Xeus avaire) {
        super(avaire);
    }
    ArrayList<String> guilds = Constants.guilds;

    @Override
    public String buildHelpDescription(@Nonnull CommandMessage context, @Nonnull String[] arguments) {
        return "**This command can only be executed in PB servers! (Bypass for global admins)**";
    }

    @Override
    public boolean handle(@Nonnull Message message, @Nonnull MiddlewareStack stack, String... args) {
        if (!message.getChannelType().isGuild()) {
            return stack.next();
        }

        int permissionLevel = XeusPermissionUtil.getPermissionLevel(stack.getDatabaseEventHolder().getGuildSettings(), message.getGuild(), message.getMember()).getLevel();
        if (permissionLevel >= XeusPermissionUtil.GuildPermissionCheckType.BOT_ADMIN.getLevel()) {
            return stack.next();
        }

        if (isPinewoodGuild(message.getGuild().getId())) {
            return stack.next();
        }

        if (args.length == 0) {
            return sendMustBePinewoodDiscordMessage(message);
        }

        if (!avaire.getBotAdmins().getUserById(message.getAuthor().getIdLong()).isAdmin()) {
            return sendMustBePinewoodDiscordMessage(message);
        }

        return stack.next();
    }

    private boolean isPinewoodGuild(String id) {
        return guilds.contains(id);
    }

    private boolean sendMustBePinewoodDiscordMessage(@Nonnull Message message) {
        return runMessageCheck(message, () -> {
            MessageFactory.makeError(message, "<a:alerta:729735220319748117> This command is only usable in official PB discord's, due to the fact it can modify something specific to Pinewood. If you want access to this command for your group, please inform Stefano#7366!")
                .queue(newMessage -> newMessage.delete().queueAfter(45, TimeUnit.SECONDS), RestActionUtil.ignore);
            return false;
        });
    }
}
