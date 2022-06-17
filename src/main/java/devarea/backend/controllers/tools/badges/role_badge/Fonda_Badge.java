package devarea.backend.controllers.tools.badges.role_badge;

import devarea.Main;
import devarea.bot.Init;

public class Fonda_Badge extends RolesBadges {
    public Fonda_Badge() {
        super("Fondateur", Main.domainName + "assets/images/badges/roles_badges/fonda_badge.png", "Ce membre est le " +
                "fondateur de Dev'Area !", Init.fonda_badge);
    }
}