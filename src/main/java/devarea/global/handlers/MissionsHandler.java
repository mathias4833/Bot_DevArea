package devarea.global.handlers;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import devarea.Main;
import devarea.backend.controllers.rest.requestContent.RequestHandlerGlobal;
import devarea.bot.Init;
import devarea.bot.commands.Command;
import devarea.bot.commands.commandTools.MessageSeria;
import devarea.bot.commands.commandTools.Mission;
import devarea.bot.presets.ColorsUsed;
import devarea.global.cache.ChannelCache;
import devarea.global.cache.MemberCache;
import devarea.global.handlers.handlerData.MissionHandlerData;
import discord4j.common.util.Snowflake;
import discord4j.core.event.domain.interaction.ButtonInteractionEvent;
import discord4j.core.object.PermissionOverwrite;
import discord4j.core.object.component.ActionRow;
import discord4j.core.object.component.Button;
import discord4j.core.object.entity.Member;
import discord4j.core.object.entity.Message;
import discord4j.core.object.entity.channel.GuildMessageChannel;
import discord4j.core.object.entity.channel.TextChannel;
import discord4j.core.object.reaction.ReactionEmoji;
import discord4j.core.spec.*;
import discord4j.discordjson.possible.Possible;
import discord4j.rest.util.AllowedMentions;
import discord4j.rest.util.Permission;
import discord4j.rest.util.PermissionSet;

import java.io.File;
import java.io.IOException;
import java.util.*;

import static devarea.bot.commands.Command.delete;
import static devarea.bot.commands.Command.send;
import static devarea.bot.presets.TextMessage.*;
import static devarea.global.utils.ThreadHandler.*;

public class MissionsHandler {
    /*
        Message at the bottom of the channel
     */
    public static Message bottomMessage;

    private static LinkedHashMap<String, Mission> missions = new LinkedHashMap<>();
    private static ArrayList<MissionHandlerData.MissionFollow> missionsFollow = new ArrayList<>();
    private static GuildMessageChannel missionChannel;
    private static int missionFollowId = 0;
    /*
        ID generator
     */
    private static final RequestHandlerGlobal.PasswordGenerator IDgenerator =
            new RequestHandlerGlobal.PasswordGenerator(new RequestHandlerGlobal.PasswordGenerator.PasswordGeneratorBuilder()
                    .useDigits(true)
                    .useLower(true));

    /*
        Initialise MissionsHandler
     */
    public static void init() {
        load();

        // Fetch message at the bottom
        setupBottomMessage();

        repeatEachMillis(() -> {
            if (!Main.developing)
                checkForUpdate();
        }, 3600000, false);
    }

    /*
        Setup the message at the Bottom of the channel
     */
    private static void setupBottomMessage() {
        missionChannel = (GuildMessageChannel) ChannelCache.fetch(Init.initial.paidMissions_channel.asString());
        Message currentAtBottom = missionChannel.getLastMessage().block();
        if (currentAtBottom.getEmbeds().size() == 0 || currentAtBottom.getEmbeds().get(0).getTitle().equals("Créer " +
                "une mission."))
            sendLastMessage();
        else bottomMessage = currentAtBottom;
    }

    /*
        Resend message at the bottom of the channel
     */
    public static void updateMessage() {
        delete(false, bottomMessage);
        sendLastMessage();
    }

    /*
        Send the message at the bottom of the channel
     */
    private static void sendLastMessage() {
        bottomMessage = Command.send(missionChannel, missionBottomMessage, true);
    }

    /*
        Dispatch Interactions
     */
    public static boolean interact(final ButtonInteractionEvent event) {
        if (event.getCustomId().startsWith("mission"))
            return actionToUpdateMessage(event);
        else if (event.getCustomId().equals("took_mission"))
            return actionToTookMission(event);
        else if (event.getCustomId().equals("followMission_close"))
            return actionToCloseFollowedMission(event);

        return false;
    }

    /*
        delete Mission and close missionsFollow who contain the member -> userID
     */
    public static void left(String userID, Collection<Mission> userMissions) {
        for (Mission current : userMissions)
            clearThisMission(current);

        // Check and close missions follow
        for (MissionHandlerData.MissionFollow follow : missionsFollow)
            if (follow.clientID.equals(userID) || follow.devID.equals(userID))
                closeFollowedMission(userID, follow);
    }

    /*
        Remove without deleting message
     */
    public static void remove(Mission mission) {
        missions.remove(mission.getId(), mission);
        save();
    }

    /*
        Remove with deleting message bind
     */
    public static void clearThisMission(Mission mission) {
        missions.remove(mission.getId(), mission);
        startAway(() -> delete(true, mission.getMessage().getMessage()));
        save();
    }

    /*
        Load missionsData and fetch missions from UserDataHandler
     */
    private static void load() {
        ObjectMapper mapper = new ObjectMapper();
        File file = new File("./mission.json");
        if (!file.exists()) save();
        try {

            // Load config
            MissionHandlerData missionData = mapper.readValue(file, new TypeReference<>() {
            });

            // Load Missions From UserDataHandler and sorting it.
            List<Map.Entry<String, Mission>> temporalListBeforeSort = new ArrayList<>();
            UserDataHandler.getMissionList().forEach((mission) -> temporalListBeforeSort.add(Map.entry(mission.getId(), mission)));

            temporalListBeforeSort.sort((a, b) -> Long.compare(b.getValue().getCreatedAt(),
                    a.getValue().getCreatedAt()));

            // Set to MissionsHandler
            for (Map.Entry<String, Mission> entry : temporalListBeforeSort)
                missions.put(entry.getKey(), entry.getValue());

            missionFollowId = missionData.missionFollowId;
            missionsFollow = missionData.missionsFollow;

            System.out.println("Missions loaded : " + missions.size() + " detected !");

            save();

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /*
        Saves MissionData and transfer to UserDataHandler
     */
    public static void save() {
        ObjectMapper mapper = new ObjectMapper();
        try {
            // Save to ./mission.json
            MissionHandlerData missionData = new MissionHandlerData(missionFollowId, missionsFollow);
            mapper.writeValue(new File("./mission.json"), missionData);

            // transfer to UserDataHandler
            UserDataHandler.setMissionList(missions);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /*
        Get missions of Member -> id
     */
    public static ArrayList<Mission> getOf(Snowflake id) {
        ArrayList<Mission> newArray = new ArrayList<>();
        for (Mission mission : missions.values())
            if (mission.getMemberId().equals(id.asString()))
                newArray.add(mission);

        return newArray;
    }

    /*
        Do not use this method
     */
    public static ArrayList<Mission> getMissions() {
        return new ArrayList<>(missions.values());
    }

    /*
        Check status Missions :
        -> AskValidate
        -> Spoil
     */
    public static void checkForUpdate() {
        ArrayList<Mission> spoiled_missions = new ArrayList<>();

        for (Mission mission : missions.values())
            if (mission.getMessage_verification() == null && System.currentTimeMillis() - mission.getLast_update() > 604800000)
                askValidate(mission);
            else if (mission.getMessage_verification() != null && System.currentTimeMillis() - mission.getLast_update() > 864000000)
                spoiled_missions.add(mission);

        for (Mission mission : spoiled_missions)
            validateSpoilAction(mission);

    }

    /*
        React to the response to askValidate :
        -> mission_yes = revalidate mission
        -> mission_no = delete mission
     */
    public static boolean actionToUpdateMessage(final ButtonInteractionEvent event) {
        Mission current_mission = null;
        for (Mission mission : missions.values())
            if (mission.getMessage_verification() != null && mission.getMessage_verification().getMessageID().equals(event.getMessageId()))
                current_mission = mission;

        System.out.println("Mission : " + current_mission);

        if (current_mission != null) {
            if (event.getCustomId().equals("mission_yes")) {
                sendMissionRevalidateSuccessful(event, current_mission);
                save();
            } else if (event.getCustomId().equals("mission_no")) {
                sendMissionDeleteSuccessful(event, current_mission);
                clearThisMission(current_mission);
            }
            return true;
        }
        return false;
    }

    /*
        Message for mission_no response
     */
    private static void sendMissionDeleteSuccessful(ButtonInteractionEvent event, Mission current_mission) {
        // This action is private channel ChannelCache cannot be used !
        event.getInteraction().getChannel().block().getMessageById(current_mission.getMessage_verification().getMessageID()).block().edit(MessageEditSpec.builder().addEmbed(EmbedCreateSpec.builder()
                        .title("Mission supprimée !")
                        .description("La mission : **" + current_mission.getTitle() + "**, a été " +
                                "définitivement supprimée !")
                        .color(ColorsUsed.just).build())
                .components(Possible.of(Optional.of(new ArrayList<>())))
                .build()).block();
    }

    /*
        Message for mission_yes response
     */
    private static void sendMissionRevalidateSuccessful(ButtonInteractionEvent event, Mission current_mission) {
        // This action is private channel ChannelCache cannot be used !
        event.getInteraction().getChannel().block().getMessageById(current_mission.getMessage_verification().getMessageID()).block().edit(MessageEditSpec.builder().addEmbed(EmbedCreateSpec.builder()
                        .title("Mission actualisée !")
                        .description("La mission : **" + current_mission.getTitle() + "**, a été définie comme" +
                                " valide pour encore 7 jours.\n\nVous recevrez une nouvelle demande de " +
                                "validation dans 7 jours.")
                        .color(ColorsUsed.just).build())
                .components(Possible.of(Optional.of(new ArrayList<>())))
                .build()).block();
        current_mission.update();
        current_mission.setMessage_verification(null);
    }

    /*
        Ask validate for his mission to the mission owner
     */
    public static void askValidate(Mission mission) {
        Member mission_member = MemberCache.get(mission.getMemberId());
        Message message = mission_member.getPrivateChannel().block().createMessage(MessageCreateSpec.builder()
                .addEmbed(EmbedCreateSpec.builder()
                        .title("Vérification de la validité d'une mission.")
                        .description("Vous avez une mission actuellement active !\n\nLe titre de cette mission est : " +
                                "**" + mission.getTitle() + "**\n\nIl vous reste 3 jours pour nous confirmer ou non " +
                                "si cette mission est toujours d'actualité.\n\nSi oui : <:ayy:" + Init.idYes.getId().asString() + "> si non : <:ayy:" + Init.idNo.getId().asString() + ">.")
                        .color(ColorsUsed.same).build())
                .addComponent(ActionRow.of(Button.primary("mission_yes", ReactionEmoji.custom(Init.idYes)),
                        Button.primary("mission_no", ReactionEmoji.custom(Init.idNo))))
                .build()).block();
        mission.setLast_update(System.currentTimeMillis() - 604800000);
        mission.setMessage_verification(new MessageSeria(message));
        save();
    }

    /*
        The member owner of the mission didn't respond before the 3 days.
        The mission is deleted and a message is sent
     */
    public static void validateSpoilAction(Mission mission) {
        Member mission_member = MemberCache.get(mission.getMemberId());
        mission_member.getPrivateChannel().block().getMessageById(mission.getMessage_verification().getMessageID()).block()
                .edit(MessageEditSpec.builder()
                        .addEmbed(EmbedCreateSpec.builder()
                                .title("Mission supprimée !")
                                .description("Le délai des 3 jours a expiré. La mission : **" + mission.getTitle() +
                                        "**, a été définitivement supprimée !")
                                .color(ColorsUsed.wrong).build())
                        .components(new ArrayList<>()).build())
                .subscribe();
        clearThisMission(mission);
    }

    /*
        Reaction to the push button "Prendre la mission" on the mission embed.
     */
    public static boolean actionToTookMission(final ButtonInteractionEvent event) {
        Mission mission = getMissionOfMessage(event.getMessageId());
        Snowflake member_react_id = event.getInteraction().getMember().get().getId();
        if (mission != null) {
            if (mission.getMemberId().equals(member_react_id.asString())) {
                event.reply(cannotFollowYourOwnMission).subscribe();
                return true;
            }
            if (alreadyHaveAChannel(mission.getMemberId(), member_react_id.asString())) {
                event.reply(alreadyFollowingThisMission).subscribe();
                return true;
            }
            followThisMission(mission, member_react_id);
            return true;
        }
        return false;
    }

    /*
        Response to the tookMission request
     */
    public static String tookMissionFromWeb(final String missionId, final Member member) {
        Mission mission = get(missionId);
        Snowflake member_react_id = member.getId();
        if (mission != null) {
            if (mission.getMemberId().equals(member_react_id.asString()))
                return "Vous ne pouvez pas prendre votre propre mission !";
            if (alreadyHaveAChannel(mission.getMemberId(), member_react_id.asString()))
                return "Vous avez déjà pris cette commande !";

            followThisMission(mission, member_react_id);
            return "Vous avez pris cette mission !";
        }
        return "La mission n'a pas été trouvée !";
    }

    /*
        Follow the mission :
        -> open a new channel with special perms
        -> send basics information
     */
    private static void followThisMission(Mission mission, Snowflake member_react_id) {
        // Create a channel
        Set<PermissionOverwrite> set = getPermissionsOverrideCreatePrivateChannel(mission, member_react_id);
        GuildMessageChannel channel = Init.devarea.createTextChannel(TextChannelCreateSpec.builder()
                .parentId(Snowflake.of("964757205184299028"))
                .name("Suivis de mission n°" + ++missionFollowId)
                .permissionOverwrites(set)
                .build()).block();

        // Send basics information
        int id = missionFollowId;
        send(channel, missionFollowMissionPreview(mission), false);

        Message message =
                channel.createMessage(missionFollowedCreateMessageExplication(member_react_id, mission)).block();

        missionsFollow.add(new MissionHandlerData.MissionFollow(id, new MessageSeria(message),
                mission.getMemberId(), member_react_id.asString()));

        save();
    }

    private static Set<PermissionOverwrite> getPermissionsOverrideCreatePrivateChannel(Mission mission,
                                                                                       Snowflake member_react_id) {
        Set<PermissionOverwrite> set = new HashSet<>();
        set.add(PermissionOverwrite.forRole(Init.initial.rulesAccepted_role, PermissionSet.of(),
                PermissionSet.of(Permission.VIEW_CHANNEL)));
        set.add(PermissionOverwrite.forRole(Init.devarea.getEveryoneRole().block().getId(), PermissionSet.of(),
                PermissionSet.of(Permission.VIEW_CHANNEL)));
        set.add(PermissionOverwrite.forMember(member_react_id, PermissionSet.of(Permission.VIEW_CHANNEL,
                Permission.READ_MESSAGE_HISTORY, Permission.SEND_MESSAGES), PermissionSet.of()));
        set.add(PermissionOverwrite.forMember(Snowflake.of(mission.getMemberId()),
                PermissionSet.of(Permission.VIEW_CHANNEL, Permission.READ_MESSAGE_HISTORY,
                        Permission.SEND_MESSAGES), PermissionSet.of()));
        set.add(PermissionOverwrite.forRole(Snowflake.of("777782222920744990"),
                PermissionSet.of(Permission.VIEW_CHANNEL), PermissionSet.of()));
        set.add(PermissionOverwrite.forRole(Snowflake.of("768383784571240509"),
                PermissionSet.of(Permission.VIEW_CHANNEL), PermissionSet.of()));
        return set;
    }

    /*
        Try to find a mission from a mission message !
     */
    public static Mission getMissionOfMessage(final Snowflake message_id) {
        for (Mission mission : missions.values())
            if (mission.getMessage().getMessageID().equals(message_id))
                return mission;
        return null;
    }

    /*
        Act to delete a follow channel from interaction, variant of closeFollowedMission(String, MissionFollow)
     */
    public static boolean actionToCloseFollowedMission(final ButtonInteractionEvent event) {
        return closeFollowedMission(event.getInteraction().getMember().get().getId().asString(),
                getMissionFollowByMessageID(event.getMessageId()));
    }

    /*
        Main function to delete followed Mission
     */
    private static boolean closeFollowedMission(String memberRequest, MissionHandlerData.MissionFollow missionFollow) {
        if (missionFollow != null) {
            send(((GuildMessageChannel) ChannelCache.watch(missionFollow.messageSeria.getChannelID().asString())),
                    missionFollowedCloseIn1Hour(memberRequest), false);

            missionFollow.messageSeria.getMessage().edit(MessageEditSpec.builder()
                    .components(new ArrayList<>())
                    .build()).subscribe();

            startAwayIn(() -> {

                missionsFollow.remove(missionFollow);

                Set<PermissionOverwrite> set = getPermissionOverwritesToHideChannel(missionFollow);

                // Rename channel and set Perms
                ((TextChannel) ChannelCache.watch(missionFollow.messageSeria.getChannelID().asString())).edit(TextChannelEditSpec.builder()
                        .name("Closed n°" + missionFollow.missionID)
                        .permissionOverwrites(set)
                        .build()).subscribe();

                // Create the MP Embed
                EmbedCreateSpec embed = EmbedCreateSpec.builder()
                        .title("Clôture du suivi n°" + missionFollow.missionID + " !")
                        .description("Le suivi de mission n°" + missionFollow.missionID + " a été clôturé à la " +
                                "demande de <@" + memberRequest + ">.")
                        .color(ColorsUsed.just)
                        .build();

                startAway(() -> MemberCache.get(missionFollow.clientID).getPrivateChannel().block().createMessage(embed).subscribe());
                startAway(() -> MemberCache.get(missionFollow.devID).getPrivateChannel().block().createMessage(embed).subscribe());

                save();
            }, 3600000L, false);
        }
        return false;
    }

    /*
        C'est moche putain !!!
        Il faut que je trouve un moyen plus propre pour créer des permissions set ! Car ceux de base c'est vraiment
        pas beau !
     */
    private static Set<PermissionOverwrite> getPermissionOverwritesToHideChannel(MissionHandlerData.MissionFollow missionFollow) {
        Set<PermissionOverwrite> set = new HashSet<>();
        set.add(PermissionOverwrite.forRole(Init.initial.rulesAccepted_role, PermissionSet.of(),
                PermissionSet.of(Permission.VIEW_CHANNEL)));
        set.add(PermissionOverwrite.forRole(Init.devarea.getEveryoneRole().block().getId(), PermissionSet.of(),
                PermissionSet.of(Permission.VIEW_CHANNEL)));
        set.add(PermissionOverwrite.forMember(Snowflake.of(missionFollow.clientID), PermissionSet.of(),
                PermissionSet.of(Permission.VIEW_CHANNEL)));
        set.add(PermissionOverwrite.forMember(Snowflake.of(missionFollow.devID), PermissionSet.of(),
                PermissionSet.of(Permission.VIEW_CHANNEL)));
        set.add(PermissionOverwrite.forRole(Snowflake.of("777782222920744990"),
                PermissionSet.of(Permission.VIEW_CHANNEL), PermissionSet.of()));
        set.add(PermissionOverwrite.forRole(Snowflake.of("768383784571240509"),
                PermissionSet.of(Permission.VIEW_CHANNEL), PermissionSet.of()));
        return set;
    }

    /*
        Create a new mission by parameters
     */
    public static Mission createMission(final String title, final String description, final String prix,
                                        final String dateRetour, final String langage, final String support,
                                        final String niveau, final Member member) {

        Mission createdMission = new Mission(title, description, prix, dateRetour, langage, support, niveau,
                member.getId().asString(), null);

        Message missionMessage = send((GuildMessageChannel) ChannelCache.watch(Init.initial.paidMissions_channel.asString()),
                MessageCreateSpec.builder()
                        .content("**Mission proposée par <@" + member.getId().asString() + "> :**")
                        .allowedMentions(AllowedMentions.suppressAll())
                        .addEmbed(createdMission.getPrefabricatedEmbed())
                        .addComponent(ActionRow.of(Button.link(Main.domainName + "mission?id=" + createdMission.getId(),
                                "devarea.fr"), Button.secondary("took_mission", "Prendre la mission")))
                        .build(),
                true);

        createdMission.setMessage(new MessageSeria(missionMessage));

        MissionsHandler.add(createdMission);
        MissionsHandler.updateMessage();
        return createdMission;
    }

    /*
        Generate a new ID
     */
    public static String generateID() {
        return IDgenerator.generate(20);
    }


    // ------------------ UTILS ------------------

    /*
    Add new mission
 */
    public static void add(Mission mission) {
        // add on the top of the map
        LinkedHashMap<String, Mission> temp = (LinkedHashMap<String, Mission>) missions.clone();
        missions.clear();
        missions.put(mission.getId(), mission);
        missions.putAll(temp);

        save();
    }

    /*
        Get mission by ID
    */
    public static Mission get(final String id) {
        return missions.get(id);
    }

    /*
        Get the MissionFollow from a messageID
    */
    public static MissionHandlerData.MissionFollow getMissionFollowByMessageID(final Snowflake messageID) {
        for (MissionHandlerData.MissionFollow mission : missionsFollow)
            if (mission.messageSeria.getMessageID().equals(messageID))
                return mission;
        return null;
    }

    /*
    Check if a dev and a client have already a follow channel !
 */
    public static boolean alreadyHaveAChannel(final String clientID, final String devID) {
        for (MissionHandlerData.MissionFollow mission : missionsFollow)
            if (mission.clientID.equals(clientID) && mission.devID.equals(devID))
                return true;
        return false;
    }
}
