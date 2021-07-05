package devarea.bot.commands.created;

import devarea.bot.Init;
import devarea.bot.commands.*;
import devarea.bot.commands.with_out_text_starter.JoinCommand;
import devarea.bot.data.ColorsUsed;
import discord4j.core.event.domain.message.MessageCreateEvent;
import discord4j.core.object.entity.Member;
import discord4j.core.object.entity.channel.TextChannel;
import discord4j.rest.util.Permission;
import discord4j.rest.util.PermissionSet;

public class Join extends ShortCommand implements PermissionCommand {

    public Join(PermissionCommand permissionCommand) {
        super();
    }

    public Join(MessageCreateEvent message) {
        super(message);
        if (message.getMessage().getAuthor().get().getId().asString().equals("321673326105985025")) {
            if (message.getMessage().getUserMentions().buffer().count().block() > 0) {
                Member member = message.getMessage().getUserMentions().blockFirst().asMember(Init.devarea.getId()).block();
                assert member != null;
                CommandManager.addManualCommand(member, new ConsumableCommand((TextChannel) message.getMessage().getChannel().block(), JoinCommand.class) {
                    @Override
                    protected Command command() {
                        return new JoinCommand(member);
                    }
                });
                sendEmbed(embed -> {
                    embed.setTitle("Vous avez fait join " + member.getDisplayName() + " !");
                    embed.setColor(ColorsUsed.just);
                }, false);
            }
        } else {
            sendError("Vous n'avez pas la permission d'utiliser cette commande !");
        }
    }

    @Override
    public PermissionSet getPermissions() {
        return PermissionSet.of(Permission.ADMINISTRATOR);
    }
}