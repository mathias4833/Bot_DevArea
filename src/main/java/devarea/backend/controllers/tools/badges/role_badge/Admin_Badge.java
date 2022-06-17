package devarea.backend.controllers.tools.badges.role_badge;

import devarea.Main;
import devarea.bot.Init;

public class Admin_Badge extends RolesBadges {
    public Admin_Badge() {
        super("Admin", Main.domainName + "assets/images/badges/roles_badges/admin_badge.png", "Ce membre est " +
                "administrateur sur le serveur !", Init.admin_badge);
    }
}
