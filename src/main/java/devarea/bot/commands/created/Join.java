package devarea.bot.commands.created;

import devarea.bot.Init;
import devarea.bot.commands.*;
import devarea.bot.commands.with_out_text_starter.JoinCommand;
import devarea.bot.data.ColorsUsed;
import discord4j.core.event.domain.message.MessageCreateEvent;
import discord4j.core.object.entity.Member;
import discord4j.core.object.entity.channel.TextChannel;
import discord4j.core.spec.EmbedCreateSpec;
import discord4j.rest.util.Permission;
import discord4j.rest.util.PermissionSet;

public class Join extends ShortCommand implements PermissionCommand {

    public Join(PermissionCommand permissionCommand) {
        super();
    }

    public Join(MessageCreateEvent message) {
        super(message);
        if (message.getMessage().getUserMentions().size() > 0) {
            Member memberPinged = message.getMessage().getUserMentions().get(0).asMember(Init.devarea.getId()).block();
            assert memberPinged != null;
            CommandManager.addManualCommand(memberPinged, new ConsumableCommand((TextChannel) message.getMessage().getChannel().block(), JoinCommand.class) {
                @Override
                protected Command command() {
                    return new JoinCommand(memberPinged);
                }
            });
            sendEmbed(EmbedCreateSpec.builder()
                    .title("Vous avez fait join " + memberPinged.getDisplayName() + " !")
                    .color(ColorsUsed.just).build(), false);
        }
    }

    @Override
    public PermissionSet getPermissions() {
        return PermissionSet.of(Permission.ADMINISTRATOR);
    }
}
