package devarea.backend.controllers.tools.badges.role_badge;

import devarea.Main;
import devarea.bot.Init;

public class Helper_Badge extends RolesBadges {
    public Helper_Badge() {
        super("Helper", Main.domainName + "assets/images/badges/roles_badges/helper_badge.png", "Ce membre est Helper" +
                " sur le serveur !", Init.helper_badge);
    }
}
