package de.bananaco.bpermissions.imp;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;

import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerListener;
import org.bukkit.event.player.PlayerLoginEvent;
import org.bukkit.permissions.PermissionAttachment;
import org.bukkit.plugin.Plugin;

import de.bananaco.bpermissions.api.World;
import de.bananaco.bpermissions.api.WorldManager;
/**
 * Handles all the superperms registering/unregistering
 * for PermissionAttachments (it's basically just somewhere
 * to stick all the nasty SuperPerms stuff that wouldn't exist
 * if SuperPerms was a more flexible system.
 * 
 * What's wrong with a PermissionProvider interface where we can
 * register a single PermissionProvider?!
 */
public class SuperPermissionHandler extends PlayerListener {

	WorldManager wm = WorldManager.getInstance();
	Map<Player, PermissionAttachment> attachments = new HashMap<Player, PermissionAttachment>();
	Plugin plugin;

	static Field permissions;

	static {
		try {
			permissions = PermissionAttachment.class.getDeclaredField("permissions");
		} catch (SecurityException e) {
			e.printStackTrace();
		} catch (NoSuchFieldException e) {
			e.printStackTrace();
		}
	}

	/**
	 * This is put in place until such a time as Bukkit pull 466 is implemented
	 * https://github.com/Bukkit/Bukkit/pull/466
	 */
	@SuppressWarnings("unchecked")
	public static void setPermissions(PermissionAttachment att, Map<String, Boolean> perm) throws IllegalArgumentException, IllegalAccessException {
		// Grab a reference to the original object
		Map<String, Boolean> orig = (Map<String, Boolean>) permissions.get(att);
		// Then whack our map into there
		orig.putAll(perm);
		// That's all folks!
	}

	protected SuperPermissionHandler(Plugin plugin) {
		this.plugin = plugin;
	}

	/**
	 * Set up the Player via the specified World object
	 * (note this is a bPermissions world, not a Bukkit world)
	 * @param player
	 * @param world
	 */
	public void setupPlayer(Player player, World world) {
		// Does the player have an attachment that we've assigned already?
		if(attachments.containsKey(player))
			player.removeAttachment(attachments.get(player));
		// Then we add a new one
		PermissionAttachment att = player.addAttachment(plugin);
		// Grab the pre-calculated effectivePermissions from the User object
		Map<String, Boolean> perms = world.getUser(player.getName()).getMappedPermissions();
		// Then whack it onto the player
		// TODO wait for the bukkit team to get their finger out, we'll use our reflection here!
		try {
			setPermissions(att, perms);
		} catch (IllegalArgumentException e) {
			e.printStackTrace();
		} catch (IllegalAccessException e) {
			e.printStackTrace();
		}
		// And store it so we can remove it again later
		attachments.put(player, att);
	}

	@Override
	public void onPlayerChangedWorld(PlayerChangedWorldEvent event) {
		// In theory this should be all we need to detect world, it isn't cancellable so... should be fine?
		setupPlayer(event.getPlayer(), wm.getWorld(event.getPlayer().getWorld().getName()));
	}

	@Override
	public void onPlayerLogin(PlayerLoginEvent event) {
		// Likewise, in theory this should be all we need to detect when a player joins
		setupPlayer(event.getPlayer(), wm.getWorld(event.getPlayer().getWorld().getName()));		
	}

}