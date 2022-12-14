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

package com.pinewoodbuilders.handlers;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.pinewoodbuilders.Environment;
import com.pinewoodbuilders.Xeus;
import com.pinewoodbuilders.contracts.handlers.EventHandler;
import com.pinewoodbuilders.database.controllers.PlayerController;
import com.pinewoodbuilders.handlers.adapter.*;
import com.pinewoodbuilders.metrics.Metrics;
import com.pinewoodbuilders.pinewood.adapter.WhitelistEventAdapter;
import com.pinewoodbuilders.utilities.CacheUtil;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.GenericEvent;
import net.dv8tion.jda.api.events.ReadyEvent;
import net.dv8tion.jda.api.events.ReconnectedEvent;
import net.dv8tion.jda.api.events.ResumedEvent;
import net.dv8tion.jda.api.events.channel.text.TextChannelCreateEvent;
import net.dv8tion.jda.api.events.channel.text.TextChannelDeleteEvent;
import net.dv8tion.jda.api.events.channel.text.update.TextChannelUpdateNameEvent;
import net.dv8tion.jda.api.events.channel.text.update.TextChannelUpdatePositionEvent;
import net.dv8tion.jda.api.events.channel.voice.VoiceChannelDeleteEvent;
import net.dv8tion.jda.api.events.channel.voice.update.VoiceChannelUpdateRegionEvent;
import net.dv8tion.jda.api.events.emote.EmoteRemovedEvent;
import net.dv8tion.jda.api.events.guild.*;
import net.dv8tion.jda.api.events.guild.invite.GuildInviteCreateEvent;
import net.dv8tion.jda.api.events.guild.invite.GuildInviteDeleteEvent;
import net.dv8tion.jda.api.events.guild.member.GuildMemberJoinEvent;
import net.dv8tion.jda.api.events.guild.member.GuildMemberRemoveEvent;
import net.dv8tion.jda.api.events.guild.update.GuildUpdateNameEvent;
import net.dv8tion.jda.api.events.guild.voice.GuildVoiceJoinEvent;
import net.dv8tion.jda.api.events.guild.voice.GuildVoiceLeaveEvent;
import net.dv8tion.jda.api.events.guild.voice.GuildVoiceMoveEvent;
import net.dv8tion.jda.api.events.interaction.ButtonClickEvent;
import net.dv8tion.jda.api.events.interaction.SlashCommandEvent;
import net.dv8tion.jda.api.events.message.GenericMessageEvent;
import net.dv8tion.jda.api.events.message.MessageBulkDeleteEvent;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.events.message.MessageUpdateEvent;
import net.dv8tion.jda.api.events.message.guild.GuildMessageDeleteEvent;
import net.dv8tion.jda.api.events.message.guild.react.GenericGuildMessageReactionEvent;
import net.dv8tion.jda.api.events.message.guild.react.GuildMessageReactionAddEvent;
import net.dv8tion.jda.api.events.message.react.GenericMessageReactionEvent;
import net.dv8tion.jda.api.events.message.react.MessageReactionAddEvent;
import net.dv8tion.jda.api.events.message.react.MessageReactionRemoveEvent;
import net.dv8tion.jda.api.events.role.GenericRoleEvent;
import net.dv8tion.jda.api.events.role.RoleCreateEvent;
import net.dv8tion.jda.api.events.role.RoleDeleteEvent;
import net.dv8tion.jda.api.events.role.update.RoleUpdateNameEvent;
import net.dv8tion.jda.api.events.role.update.RoleUpdatePermissionsEvent;
import net.dv8tion.jda.api.events.role.update.RoleUpdatePositionEvent;
import net.dv8tion.jda.api.events.user.update.UserUpdateAvatarEvent;
import net.dv8tion.jda.api.events.user.update.UserUpdateDiscriminatorEvent;
import net.dv8tion.jda.api.events.user.update.UserUpdateNameEvent;
import net.dv8tion.jda.api.utils.concurrent.Task;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class MainEventHandler extends EventHandler {

    private final RoleEventAdapter roleEvent;
    private final MemberEventAdapter memberEvent;
    private final ChannelEventAdapter channelEvent;
    private final MessageEventAdapter messageEvent;
    private final GuildStateEventAdapter guildStateEvent;
    private final JDAStateEventAdapter jdaStateEventAdapter;
    private final ChangelogEventAdapter changelogEventAdapter;
    private final ReactionEmoteEventAdapter reactionEmoteEventAdapter;
    private final GuildEventAdapter guildEventAdapter;
    private final WhitelistEventAdapter whitelistEventAdapter;
    private final ButtonClickEventAdapter buttonClickEventAdapter;
    private final SlashCommandEventAdapter slashCommandEventAdapter;
    private final VoiceChannelHandler voiceChannelHandler;

    public static final Cache<Long, Boolean> cache = CacheBuilder.newBuilder()
        .recordStats()
        .expireAfterWrite(15, TimeUnit.MINUTES)
        .build();

    private static final Logger log = LoggerFactory.getLogger(MainEventHandler.class);

    private int invites = 0;

    /**
     * Instantiates the event handler and sets the avaire class instance.
     *
     * @param avaire The Xeus application class instance.
     */
    public MainEventHandler(Xeus avaire) {
        super(avaire);

        this.roleEvent = new RoleEventAdapter(avaire);
        this.memberEvent = new MemberEventAdapter(avaire);
        this.channelEvent = new ChannelEventAdapter(avaire);
        this.messageEvent = new MessageEventAdapter(avaire);
        this.guildStateEvent = new GuildStateEventAdapter(avaire);
        this.jdaStateEventAdapter = new JDAStateEventAdapter(avaire);
        this.changelogEventAdapter = new ChangelogEventAdapter(avaire);
        this.reactionEmoteEventAdapter = new ReactionEmoteEventAdapter(avaire);
        this.guildEventAdapter = new GuildEventAdapter(avaire);
        this.whitelistEventAdapter = new WhitelistEventAdapter(avaire, avaire.getVoiceWhitelistManager());
        this.buttonClickEventAdapter = new ButtonClickEventAdapter(avaire);
        this.slashCommandEventAdapter = new SlashCommandEventAdapter(avaire);
        this.voiceChannelHandler = new VoiceChannelHandler(avaire);
    }

    @Override
    public void onGenericEvent(GenericEvent event) {
        prepareGuildMembers(event);

        Metrics.jdaEvents.labels(event.getClass().getSimpleName()).inc();
    }

    @Override
    public void onReady(ReadyEvent event) {
        jdaStateEventAdapter.onConnectToShard(event.getJDA());
        Guild guild = event.getJDA().getGuildById("438134543837560832");
        if (guild != null) {
            guild.retrieveInvites().queue(invites -> {
                this.invites = invites.size();
            });
        }
    }

    @Override
    public void onResumed(ResumedEvent event) {
        jdaStateEventAdapter.onConnectToShard(event.getJDA());
    }

    @Override
    public void onReconnected(ReconnectedEvent event) {
        jdaStateEventAdapter.onConnectToShard(event.getJDA());
    }

    @Override
    public void onVoiceChannelUpdateRegion(VoiceChannelUpdateRegionEvent event) {
        guildStateEvent.onGuildUpdateRegion(event);
    }

    @Override
    public void onGuildUpdateName(GuildUpdateNameEvent event) {
        guildStateEvent.onGuildUpdateName(event);
    }

    @Override
    public void onGuildJoin(GuildJoinEvent event) {
        guildStateEvent.onGuildJoin(event);
    }

    @Override
    public void onGuildLeave(GuildLeaveEvent event) {
        guildStateEvent.onGuildLeave(event);
    }

    @Override
    public void onTextChannelDelete(TextChannelDeleteEvent event) {
        channelEvent.updateChannelData(event.getGuild());
        channelEvent.onTextChannelDelete(event);
    }

    @Override
    public void onTextChannelCreate(TextChannelCreateEvent event) {
        channelEvent.updateChannelData(event.getGuild());
    }

    @Override
    public void onTextChannelUpdateName(TextChannelUpdateNameEvent event) {
        channelEvent.updateChannelData(event.getGuild());
    }

    @Override
    public void onTextChannelUpdatePosition(TextChannelUpdatePositionEvent event) {
        channelEvent.updateChannelData(event.getGuild());
    }

    @Override
    public void onGuildVoiceJoin(@Nonnull GuildVoiceJoinEvent event) {
        whitelistEventAdapter.whitelistCheckEvent(event);
        voiceChannelHandler.createPrivateChannelOnLobbyJoin(event);
    }

    @Override
    public void onGuildVoiceMove(@Nonnull GuildVoiceMoveEvent event) {
        whitelistEventAdapter.whitelistCheckEvent(event);
        voiceChannelHandler.createPrivateChannelOnLobbyJoinFromMove(event);
    }

    @Override
    public void onGuildVoiceLeave(@Nonnull GuildVoiceLeaveEvent event) {
        voiceChannelHandler.removePrivateChannelOn(event);
    }

    @Override
    public void onVoiceChannelDelete(@Nonnull VoiceChannelDeleteEvent event) {
        voiceChannelHandler.onVoiceDelete(event);
    }

    @Override
    public void onGuildMemberJoin(@NotNull GuildMemberJoinEvent event) {
        memberEvent.onGuildMemberJoin(event);
        if (event.getGuild().getId().equals("438134543837560832")) {
            checkInviteAndRole(event);
        }

    }


    // keep in mind that invite events will only fire for channels which your bot has MANAGE_CHANNEL perm in
    @Override
    public void onGuildInviteCreate(final GuildInviteCreateEvent event)                               // gets fired when an invite is created, lets cache it
    {
        event.getGuild().retrieveInvites().queue(l -> {
            invites = l.size();
        });                                                          // put code as a key and InviteData object as a value into the map; cache
    }

    // keep in mind that invite events will only fire for channels which your bot has MANAGE_CHANNEL perm in
    @Override
    public void onGuildInviteDelete(final GuildInviteDeleteEvent event)                               // gets fired when an invite is created, lets cache it
    {
        event.getGuild().retrieveInvites().queue(l -> {
            invites = l.size();
        });                                                          // put code as a key and InviteData object as a value into the map; cache
    }

    public void checkInviteAndRole(final GuildMemberJoinEvent event)                                   // gets fired when a member has joined, lets try to get the invite the member used
    {
        final Guild guild = event.getGuild();                                                         // get the guild a member joined to
        if (!guild.getId().equals("438134543837560832")) return;

        final User user = event.getUser();                                                            // get the user who joined
        final Member selfMember = guild.getSelfMember();                                              // get your bot's member object for this guild

        if (!selfMember.hasPermission(Permission.MANAGE_SERVER) || user.isBot())                      // check if your bot doesn't have MANAGE_SERVER permission and the user who joined is a bot, if either of those is true, return
            return;

        guild.retrieveInvites().queue(retrievedInvites ->                                             // retrieve all guild's invites
        {
            if (retrievedInvites.size() == invites) return;
            if (retrievedInvites.size() > invites) return;
            List<Role> roles = event.getGuild().getRolesByName("Pizza Delivery", true);
            if (roles.size() == 0) {
                System.out.println("Role does not exist");
                return;
            }
            Role r = roles.get(0);
            event.getGuild().addRoleToMember(event.getMember(), r).queue();
            invites = retrievedInvites.size();
        });
    }

    @Override
    public void onGuildReady(final GuildReadyEvent event)                                             // gets fired when a guild has finished setting up upon booting the bot, lets try to cache its invites
    {
        final Guild guild = event.getGuild();
        attemptInviteCaching(guild);                                                                  // attempt to store guild's invites
    }

    private void attemptInviteCaching(final Guild guild)                                              // helper method to prevent duplicate code for GuildReadyEvent and GuildJoinEvent
    {
        if (!guild.getId().equals("438134543837560832")) return;// get the guild that has finished setting up
        final Member selfMember = guild.getSelfMember();                                              // get your bot's member object for this guild

        if (!selfMember.hasPermission(Permission.MANAGE_SERVER))                                      // check if your bot doesn't have MANAGE_SERVER permission to retrieve the invites, if true, return
            return;

        guild.retrieveInvites().queue(retrievedInvites ->                                             // retrieve all guild's invites
        {
            invites = retrievedInvites.size();
        });
    }

    @Override
    public void onGenericGuild(@Nonnull GenericGuildEvent event) {
        guildEventAdapter.onGenericGuildEvent(event);
        guildEventAdapter.onJoinLogsEvent(event);
    }

    @Override
    public void onGuildMemberRemove(@Nonnull GuildMemberRemoveEvent event) {
        memberEvent.onGuildMemberRemove(event);

    }


    @Override
    public void onSlashCommand(@NotNull SlashCommandEvent event) {
        slashCommandEventAdapter.runSlashCommandCheck(event);
    }

    @Override
    public void onMessageReceived(MessageReceivedEvent event) {
        if (changelogEventAdapter.isChangelogMessage(event.getChannel())) {
            changelogEventAdapter.onMessageReceived(event);
        }
        messageEvent.onMessageReceived(event);

        if (event.getChannel().getId().equals("691337332661420082")) {
            if (!event.getMessage().getContentRaw().contains("img.pizzabyte.xyz")) {
                event.getMessage().delete()
                    .flatMap(l -> event.getMember().getUser().openPrivateChannel())
                    .flatMap(privateChannel -> privateChannel.sendMessage("Hi there, we only allow messages that contain the " +
                        "**official https://img.pizzabyte.xyz/ domain.**, your message has been deleted. Sorry for the inconvenience. " +
                        "If you'd like to ask any questions, please follow the steps in the topic of <#691337332661420082>"))
                    .queue();
            }
            return;
        }

        if (Xeus.getEnvironment().getName().equals(Environment.DEVELOPMENT.getName())) {
            return;
        }

        if (event.isFromGuild()) {
            if (!event.getAuthor().isBot()) {
                messageEvent.onLocalFilterMessageReceived(event);
                messageEvent.onGlobalFilterMessageReceived(event);
                messageEvent.onNoLinksFilterMessageReceived(event);

                if (event.getChannel().getId().equals("769274801768235028") || event.getChannel().getId().equals("777903149511082005")) {
                    messageEvent.onEventGalleryMessageSent(event);
                }
            }
            if (event.getChannel().getId().equals("871890084121673738")) {
                messageEvent.sendPBACRaidVoteEmojis(event);
            }
            if (event.getChannel().getId().equals("744982672228745267")) {
                messageEvent.onPIAAdminMessageEvent(event);
            }

        }

    }

    @Override
    public void onGuildMessageDelete(GuildMessageDeleteEvent event) {
        if (changelogEventAdapter.isChangelogMessage(event.getChannel())) {
            changelogEventAdapter.onMessageDelete(event);
        }

        messageEvent.onMessageDelete(event.getChannel(), Collections.singletonList(event.getMessageId()));
    }

    @Override
    public void onMessageBulkDelete(MessageBulkDeleteEvent event) {
        messageEvent.onMessageDelete(event.getChannel(), event.getMessageIds());
    }

    @Override
    public void onMessageUpdate(MessageUpdateEvent event) {
        if (changelogEventAdapter.isChangelogMessage(event.getChannel())) {
            changelogEventAdapter.onMessageUpdate(event);
        }
        messageEvent.onMessageUpdate(event);

        if (Xeus.getEnvironment().getName().equals(Environment.DEVELOPMENT.getName())) {
            return;
        }
        if (event.isFromGuild()) {
            messageEvent.onGuildMessageUpdate(event);
            messageEvent.onGlobalFilterEditReceived(event);
            messageEvent.onLocalFilterEditReceived(event);
        }
    }

    @Override
    public void onRoleUpdateName(RoleUpdateNameEvent event) {
        roleEvent.updateRoleData(event.getGuild());
        roleEvent.onRoleUpdateName(event);
    }

    @Override
    public void onRoleDelete(RoleDeleteEvent event) {
        roleEvent.updateRoleData(event.getGuild());
        roleEvent.onRoleDelete(event);
    }

    @Override
    public void onRoleCreate(RoleCreateEvent event) {
        roleEvent.updateRoleData(event.getGuild());
    }

    @Override
    public void onRoleUpdatePosition(RoleUpdatePositionEvent event) {
        roleEvent.updateRoleData(event.getGuild());
    }

    @Override
    public void onRoleUpdatePermissions(RoleUpdatePermissionsEvent event) {
        roleEvent.updateRoleData(event.getGuild());
    }

    @Override
    public void onUserUpdateDiscriminator(UserUpdateDiscriminatorEvent event) {
        PlayerController.updateUserData(event.getUser());

    }

    @Override
    public void onUserUpdateAvatar(UserUpdateAvatarEvent event) {
        PlayerController.updateUserData(event.getUser());

    }

    @Override
    public void onUserUpdateName(UserUpdateNameEvent event) {
        PlayerController.updateUserData(event.getUser());

    }

    @Override
    public void onEmoteRemoved(EmoteRemovedEvent event) {
        reactionEmoteEventAdapter.onEmoteRemoved(event);
    }

    @Override
    public void onMessageReactionAdd(MessageReactionAddEvent event) {
        if (isValidMessageReactionEvent(event)) {
            reactionEmoteEventAdapter.onMessageReactionAdd(event);
        }
    }

    @Override
    public void onGuildMessageReactionAdd(@Nonnull GuildMessageReactionAddEvent event) {
        if (isValidMessageReactionEvent(event)) {

            reactionEmoteEventAdapter.onPBSTRequestRewardMessageAddEvent(event);
            reactionEmoteEventAdapter.onGuildSuggestionValidation(event);
            reactionEmoteEventAdapter.onReportsReactionAdd(event);
            reactionEmoteEventAdapter.onFeedbackMessageEvent(event);
        }

    }

    @Override
    public void onButtonClick(@Nonnull ButtonClickEvent event) {
        buttonClickEventAdapter.onPatrolRemittanceButtonClickEvent(event);
        buttonClickEventAdapter.onReportsButtonClickEvent(event);
        buttonClickEventAdapter.onFeedbackButtonClickEvent(event);
        buttonClickEventAdapter.onQuizButtonClickEvent(event);
    }

    @Override
    public void onMessageReactionRemove(@Nonnull MessageReactionRemoveEvent event) {
        if (isValidMessageReactionEvent(event)) {
            reactionEmoteEventAdapter.onMessageReactionRemove(event);
        }
    }

    private boolean isValidMessageReactionEvent(GenericMessageReactionEvent event) {
        return event.isFromGuild() && event.getReactionEmote().isEmote();
    }

/*    private boolean isValidReportChannel(GuildMessageReactionAddEvent event) {
        return event.getChannel().getId().equals(Constants.PBST_REPORT_CHANNEL) || event.getChannel().getId().equals(Constants.PET_REPORT_CHANNEL)
                || event.getChannel().getId().equals(Constants.TMS_REPORT_CHANNEL) || event.getChannel().getId().equals(Constants.PB_REPORT_CHANNEL) || event.getChannel().getName().equals("handbook-violator-reports");
    }*/

    private void prepareGuildMembers(GenericEvent event) {
        if (event instanceof GenericMessageEvent) {
            GenericMessageEvent genericMessageEvent = (GenericMessageEvent) event;

            if (genericMessageEvent.isFromGuild()) {
                loadGuildMembers(genericMessageEvent.getGuild());
            }

        } else if (event instanceof GenericRoleEvent) {
            GenericRoleEvent genericRoleEvent = (GenericRoleEvent) event;

            loadGuildMembers(genericRoleEvent.getGuild());
        } else if (event instanceof GenericGuildMessageReactionEvent) {
            GenericGuildMessageReactionEvent genericGuildMessageReactionEvent = (GenericGuildMessageReactionEvent) event;

            loadGuildMembers(genericGuildMessageReactionEvent.getGuild());
        } else if (event instanceof GenericGuildEvent) {
            GenericGuildEvent genericGuildEvent = (GenericGuildEvent) event;

            loadGuildMembers(genericGuildEvent.getGuild());
        }
    }


    public final ArrayList<String> guilds = new ArrayList<String>() {{
        add("495673170565791754"); // Aerospace
        add("438134543837560832"); // PBST
        add("791168471093870622"); // Kronos Dev
        add("371062894315569173"); // Official PB Server
        add("514595433176236078"); // PBQA
        add("436670173777362944"); // PET
        add("505828893576527892"); // MMFA
        add("498476405160673286"); // PBM
        add("572104809973415943"); // TMS
        add("758057400635883580"); // PBOP
        add("669672893730258964"); // PB Dev
    }};


    public void onGuildUnban(@Nonnull GuildUnbanEvent e) {
        if (guilds.contains(e.getGuild().getId())) {
            guildEventAdapter.onGuildPIAMemberBanEvent(e);
        }
    }

    private void loadGuildMembers(Guild guild) {
        if (guild.isLoaded()) {
            return;
        }

        CacheUtil.getUncheckedUnwrapped(cache, guild.getIdLong(), () -> {
            log.debug("Lazy-loading members for guild: {} (ID: {})", guild.getName(), guild.getIdLong());
            Task<List<Member>> task = guild.loadMembers();

            guild.getMemberCount();

            task.onSuccess(members -> {
                log.debug("Lazy-loading for guild {} is done, loaded {} members",
                    guild.getId(), members.size()
                );

                cache.invalidate(guild.getIdLong());
            });

            task.onError(throwable -> log.error("Failed to lazy-load guild members for {}, error: {}",
                guild.getIdLong(), throwable.getMessage(), throwable
            ));

            return true;
        });
    }

    private boolean isValidMessageReactionEvent(MessageReactionAddEvent event) {
        return event.isFromGuild()
            && event.getReactionEmote().isEmote()
            && !event.getMember().getUser().isBot();
    }

    private boolean isValidMessageReactionEvent(MessageReactionRemoveEvent event) {
        return event.isFromGuild()
            && event.getReactionEmote().isEmote();
    }

    private boolean isValidMessageReactionEvent(GuildMessageReactionAddEvent event) {
        return !event.getUser().isBot();
    }

}
