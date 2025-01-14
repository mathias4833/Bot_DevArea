package devarea.bot.commands.outLine;

import devarea.Main;
import devarea.backend.controllers.rest.requestContent.RequestHandlerAuth;
import devarea.bot.Init;
import devarea.bot.commands.ShortCommand;
import devarea.bot.presets.ColorsUsed;
import devarea.bot.presets.TextMessage;
import devarea.global.cache.ChannelCache;
import devarea.global.cache.MemberCache;
import discord4j.core.object.entity.Member;
import discord4j.core.object.entity.Message;
import discord4j.core.object.entity.channel.GuildMessageChannel;
import discord4j.core.object.entity.channel.PrivateChannel;
import discord4j.core.spec.EmbedCreateSpec;
import discord4j.core.spec.MessageCreateSpec;
import discord4j.core.spec.MessageEditSpec;

import java.util.ArrayList;

import static devarea.global.utils.ThreadHandler.startAway;

public class JoinCommand extends ShortCommand {

    public JoinCommand() {
    }

    public JoinCommand(Member member) {
        super(member);
        this.member.addRole(Init.initial.rulesAccepted_role).subscribe();

        member.addRole(Init.initial.rulesAccepted_role).subscribe();

        try {
            PrivateChannel privateChannel = member.getPrivateChannel().block();
            startAway(() -> {
                privateChannel.createMessage(TextMessage.helpEmbed).block();

                final String code = RequestHandlerAuth.getCodeForMember(member.getId().asString());

                final Message message_at_edit = privateChannel.createMessage(MessageCreateSpec.builder()
                        .addEmbed(EmbedCreateSpec.builder()
                                .title("Authentification au site de Dev'area !")
                                .description("Vous venez de vous authentifier sur le site de dev'area" +
                                        " !\n\nPour vous connecter utilisez ce lien :\n\n" + Main
                                        .domainName + "?code" +
                                        "=" + code + "\n\nCe message sera supprimé d'ici **5 " +
                                        "minutes** pour sécuriser l'accès. Si vous avez besoin de le " +
                                        "retrouver exécutez de nouveau la commande !")
                                .color(ColorsUsed.just)
                                .build())
                        .build()).block();

                final ArrayList<EmbedCreateSpec> embeds = new ArrayList<>();

                embeds.add(EmbedCreateSpec.builder()
                        .title("Authentification au site de Dev'area !")
                        .description("Si vous voulez retrouver le lien d'authentification vous pouvez" +
                                " exécuter la commande `/auth` à nouveau !")
                        .color(ColorsUsed.same)
                        .build());

                startAway(() -> {
                    try {
                        Thread.sleep(300000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    } finally {
                        message_at_edit.edit(MessageEditSpec.builder()
                                .addAllEmbeds(embeds)
                                .build()).subscribe();
                    }
                });

            });

        } catch (Exception e) {
        }

        startAway(() -> ((GuildMessageChannel) ChannelCache.watch(Init.initial.welcome_channel.asString()))
                .createMessage(MessageCreateSpec.builder()
                        .addEmbed(EmbedCreateSpec.builder()
                                .title("Salut ! " + member.getTag() + ", bienvenue sur **Dev'Area**, amuse " +
                                        "toi bien !")
                                .description("Membre n°" + MemberCache.cacheSize())
                                .image(member.getAvatarUrl())
                                .color(ColorsUsed.same)
                                .build())
                        .build()).subscribe());

        ((GuildMessageChannel) ChannelCache.watch(Init.initial.general_channel.asString()))
                .createMessage(msg -> msg
                        .setContent("<@" + member.getId().asString() +
                                "> vient de rejoindre le serveur ! Pour en savoir plus sur le serveur <#1004324078531907594> \uD83D\uDE09 !"))
                .subscribe();

/*
        this.deletedCommand(600000L, () -> {
            // Remove Before kick to prevent double start of endCommand !
            CommandManager.removeCommand(this.member.getId(), this);

            this.member.kick("Didn't finish QCM !").subscribe();
        });


        this.createLocalChannel(
                member.getDisplayName(),
                Init.initial.join_category,
                false);

        Step roles = new Step() {
            @Override
            protected boolean onCall(Message message) {
                final PermissionOverwrite over = PermissionOverwrite.forMember(member.getId(),
                        PermissionSet.of(Permission.VIEW_CHANNEL, Permission.READ_MESSAGE_HISTORY), PermissionSet.of());
                startAway(() -> ((TextChannel) ChannelCache.watch(Init.initial.roles_channel.asString()))
                .addMemberOverwrite(member.getId(), over).subscribe());
                setMessage(MessageEditSpec.builder()
                        .addEmbed(EmbedCreateSpec.builder()
                                .title("Et le plus important...")
                                .description(TextMessage.roles)
                                .color(ColorsUsed.just).build())
                        .components(getEmptyButton())
                        .build());

                new Thread(() -> {
                    try {
                        Thread.sleep(30000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }

                    if (MemberCache.contain(member.getId().asString())) {
                        member.addRole(Init.initial.rulesAccepted_role).subscribe();
                        endCommand();

                        try {
                            PrivateChannel privateChannel = member.getPrivateChannel().block();
                            startAway(() -> {
                                privateChannel.createMessage(TextMessage.helpEmbed).block();

                                final String code = RequestHandlerAuth.getCodeForMember(member.getId().asString());

                                final Message message_at_edit = privateChannel.createMessage(MessageCreateSpec.builder()
                                        .addEmbed(EmbedCreateSpec.builder()
                                                .title("Authentification au site de Dev'area !")
                                                .description("Vous venez de vous authentifier sur le site de dev'area" +
                                                        " !\n\nPour vous connecter utilisez ce lien :\n\n" + Main
                                                        .domainName + "?code" +
                                                        "=" + code + "\n\nCe message sera supprimé d'ici **5 " +
                                                        "minutes** pour sécuriser l'accès. Si vous avez besoin de le " +
                                                        "retrouver exécutez de nouveau la commande !")
                                                .color(ColorsUsed.just)
                                                .build())
                                        .build()).block();

                                final ArrayList<EmbedCreateSpec> embeds = new ArrayList<>();

                                embeds.add(EmbedCreateSpec.builder()
                                        .title("Authentification au site de Dev'area !")
                                        .description("Si vous voulez retrouver le lien d'authentification vous pouvez" +
                                                " exécuter la commande `/auth` à nouveau !")
                                        .color(ColorsUsed.same)
                                        .build());

                                startAway(() -> {
                                    try {
                                        Thread.sleep(300000);
                                    } catch (InterruptedException e) {
                                        e.printStackTrace();
                                    } finally {
                                        message_at_edit.edit(MessageEditSpec.builder()
                                                .embeds(embeds)
                                                .build()).subscribe();
                                    }
                                });

                            });

                        } catch (Exception e) {
                        }

                        startAway(() -> ((TextChannel) ChannelCache.watch(Init.initial.welcome_channel.asString()))
                        .createMessage(MessageCreateSpec.builder()
                                .addEmbed(EmbedCreateSpec.builder()
                                        .title("Salut ! " + member.getTag() + ", bienvenue sur **Dev'Area**, amuse " +
                                                "toi bien !")
                                        .description("Membre n°" + MemberCache.cacheSize())
                                        .image(member.getAvatarUrl())
                                        .color(ColorsUsed.same)
                                        .build())
                                .build()).subscribe());

                        ((TextChannel) ChannelCache.watch(Init.initial.general_channel.asString()))
                                .createMessage(msg -> msg
                                        .setContent("<@" + member.getId().asString() + "> a passé le petit " +
                                                "questionnaire d'arrivée ! Vous pouvez lui souhaiter la bienvenue !"))
                                .subscribe();
                    }
                }).start();
                return false;
            }
        };

        Step presentation = new Step(roles) {
            @Override
            protected boolean onCall(Message message) {
                final PermissionOverwrite over = PermissionOverwrite.forMember(member.getId(),
                        PermissionSet.of(Permission.VIEW_CHANNEL, Permission.READ_MESSAGE_HISTORY), PermissionSet.of());
                startAway(() -> ((TextChannel) ChannelCache.watch(Init.initial.presentation_channel.asString()))
                .addMemberOverwrite(member.getId(), over).subscribe());
                setText(EmbedCreateSpec.builder()
                        .title("Avant de commencer !")
                        .description(TextMessage.presentation)
                        .color(ColorsUsed.just).build());
                return false;
            }

            @Override
            protected boolean onReceiveInteract(ButtonInteractionEvent event) {
                if (isYes(event))
                    return callStep(0);
                return super.onReceiveInteract(event);
            }
        };

        Step rules = new Step(presentation) {
            @Override
            protected boolean onCall(Message message) {
                setText(EmbedCreateSpec.builder()
                        .title("Règles")
                        .description(TextMessage.rules)
                        .color(ColorsUsed.just).build());
                return false;
            }

            @Override
            protected boolean onReceiveInteract(ButtonInteractionEvent event) {
                if (isYes(event))
                    return callStep(0);
                return super.onReceiveInteract(event);
            }
        };

        Step needDevInfo = new Step(rules) {
            @Override
            protected boolean onCall(Message message) {
                final PermissionOverwrite over = PermissionOverwrite.forMember(member.getId(),
                        PermissionSet.of(Permission.VIEW_CHANNEL, Permission.READ_MESSAGE_HISTORY), PermissionSet.of());
                startAway(() -> ((TextChannel) ChannelCache.watch(Init.initial.freeMissions_channel.asString()))
                .addMemberOverwrite(member.getId(), over).subscribe());
                startAway(() -> ((TextChannel) ChannelCache.watch(Init.initial.paidMissions_channel.asString()))
                .addMemberOverwrite(member.getId(), over).subscribe());
                startAway(() -> ((TextChannel) ChannelCache.watch(Init.initial.freelance_channel.asString()))
                .addMemberOverwrite(member.getId(), over).subscribe());
                setMessage(MessageEditSpec.builder()
                        .addEmbed(EmbedCreateSpec.builder()
                                .title("Conseils pour demander du code (missions)")
                                .description(TextMessage.rulesForAskCode)
                                .color(ColorsUsed.just).build())
                        .addComponent(getYesButton())
                        .build());
                return false;
            }

            @Override
            protected boolean onReceiveInteract(ButtonInteractionEvent event) {
                if (isYes(event))
                    return callStep(0);
                return super.onReceiveInteract(event);
            }
        };

        Step devInfo = new Step(rules) {
            @Override
            protected boolean onCall(Message message) {
                setMessage(MessageEditSpec.builder()
                        .addEmbed(EmbedCreateSpec.builder()
                                .title("Conseils de communication du code")
                                .description(TextMessage.rulesForSpeakCode)
                                .color(ColorsUsed.just).build())
                        .addComponent(getYesButton())
                        .build());
                return false;
            }

            @Override
            protected boolean onReceiveInteract(ButtonInteractionEvent event) {
                if (isYes(event))
                    return callStep(0);
                return super.onReceiveInteract(event);
            }
        };

        Step DevOrNeedDev = new Step(devInfo, needDevInfo) {
            @Override
            protected boolean onCall(Message message) {
                setMessage(MessageEditSpec.builder()
                        .addEmbed(EmbedCreateSpec.builder()
                                .title("Pour quoi es-tu là ?")
                                .description("    - Tu es développeur ou tu es ici pour apprendre à développer -> " +
                                        "<:ayy:" + Init.idYes.getId().asString() + ">\n    - Tu es là car tu as " +
                                        "besoin de développeurs, tu as une mission à donner -> <:ayy:" + Init.idNo
                                        .getId().asString() + ">")
                                .color(ColorsUsed.just).build())
                        .addComponent(getYesNoButton())
                        .build());
                return next;
            }

            @Override
            protected boolean onReceiveInteract(ButtonInteractionEvent event) {
                if (isYes(event)) {
                    return callStep(0);
                } else if (isNo(event)) {
                    return callStep(1);
                }
                return super.onReceiveInteract(event);
            }
        };

        this.firstStep = new FirstStep(this.channel, DevOrNeedDev) {
            @Override
            public void onFirstCall(MessageCreateSpec deleteThisVariableAndSetYourOwnMessage) {
                startAway(() -> ((TextChannel) ChannelCache.watch("843823896222629888")).addMemberOverwrite(member
                .getId(), PermissionOverwrite.forMember(member.getId(), PermissionSet.of(), PermissionSet.of
                (Permission.VIEW_CHANNEL))).subscribe());
                super.onFirstCall(MessageCreateSpec.builder().addEmbed(EmbedCreateSpec.builder()
                                .title("Bienvenue " + member.getDisplayName() + " sur Dev'Area !")
                                .description(TextMessage.firstText)
                                .color(ColorsUsed.just).build())
                        .addComponent(getYesButton())
                        .build());
            }

            @Override
            protected boolean onReceiveInteract(ButtonInteractionEvent event) {
                if (isYes(event))
                    return callStep(0);
                return super.onReceiveInteract(event);
            }
        };
        this.lastMessage = this.firstStep.getMessage();*/
    }

}
