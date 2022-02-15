package devarea.bot.commands.created;

import devarea.bot.commands.CommandManager;
import devarea.bot.commands.PermissionCommand;
import devarea.bot.commands.ShortCommand;
import devarea.bot.data.ColorsUsed;
import discord4j.core.event.domain.message.MessageCreateEvent;
import discord4j.core.spec.EmbedCreateSpec;
import discord4j.rest.util.PermissionSet;

public class LeftThisCommand extends ShortCommand implements PermissionCommand {

    public LeftThisCommand(PermissionCommand comm) {
        super();
    }

    public LeftThisCommand(final MessageCreateEvent event) {
        super(event);
        if (CommandManager.hasCommand(event.getMember().get().getId())) {
            sendEmbed(EmbedCreateSpec.builder()
                    .title("Admin")
                    .description("Vous venez de vous détacher de la commande de <@" + CommandManager.getCommandOf(event.getMember().get().getId()).getMember().getId().asString() + ">")
                    .color(ColorsUsed.same).build(), false);
            CommandManager.unbindMemberToMember(event.getMember().get().getId());
        }

    }

    @Override
    public PermissionSet getPermissions() {
        return new JoinThisCommand(() -> null).getPermissions();
    }
}
