package xyz.damt.match;

import lombok.Getter;
import lombok.Setter;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.entity.Player;
import xyz.damt.Practice;
import xyz.damt.arena.Arena;
import xyz.damt.events.MatchEndEvent;
import xyz.damt.events.MatchStartEvent;
import xyz.damt.kit.Kit;
import xyz.damt.profile.Profile;
import xyz.damt.util.CC;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Getter @Setter
public class Match {

    private final Player playerOne;
    private final Player playerTwo;

    private final List<Player> spectators;
    private Arena arena;
    private final Kit kit;

    private boolean hasStarted;
    private boolean isElo;
    private int countdownTime;

    public Match(Player playerOne, Player playerTwo, Kit kit, Arena arena, boolean isElo) {
        this.playerOne = playerOne;
        this.playerTwo = playerTwo;
        this.arena = arena;
        this.isElo = isElo;
        this.kit = kit;

        this.spectators = new ArrayList<>();
        this.start();
    }

    public void start() {
        MatchStartEvent matchStartEvent = new MatchStartEvent(this, playerOne, playerTwo);
        Bukkit.getPluginManager().callEvent(matchStartEvent);

        playerOne.teleport(arena.getPositionOne());
        playerTwo.teleport(arena.getPositionTwo());

        playerOne.getInventory().setContents(kit.getContents());
        playerOne.getInventory().setArmorContents(kit.getArmorContents());

        playerTwo.getInventory().setContents(kit.getContents());
        playerTwo.getInventory().setArmorContents(kit.getArmorContents());

        spectators.forEach(spectator -> spectator.teleport(arena.getCenter()));
        arena.setBusy(true);

        Practice.getInstance().getMatchHandler().getMatchHashMap().put(playerOne.getUniqueId(), this);
        Practice.getInstance().getMatchHandler().getMatchHashMap().put(playerTwo.getUniqueId(), this);
    }

    public void stop(UUID uuid, int time) {
        MatchEndEvent matchEndEvent = playerOne.getUniqueId().equals(uuid) ?
                new MatchEndEvent(this, playerOne, playerTwo) : new MatchEndEvent(this, playerTwo, playerOne);
        Bukkit.getPluginManager().callEvent(matchEndEvent);

        Profile profilePlayerOne = Practice.getInstance().getProfileHandler().getProfile(playerOne.getUniqueId());
        Profile profilePlayerTwo = Practice.getInstance().getProfileHandler().getProfile(playerTwo.getUniqueId());

        profilePlayerOne.setLastInventoryContents(playerOne.getInventory().getContents());
        profilePlayerOne.setLastArmorContents(playerOne.getInventory().getArmorContents());

        profilePlayerTwo.setLastInventoryContents(playerTwo.getInventory().getContents());
        profilePlayerTwo.setLastArmorContents(playerTwo.getInventory().getArmorContents());

        if (playerOne != null) playerOne.setGameMode(GameMode.CREATIVE);
        if (playerTwo != null) playerTwo.setGameMode(GameMode.CREATIVE);

        Bukkit.getScheduler().runTaskLaterAsynchronously(Practice.getInstance(), () -> {
            spectators.clear();
            arena.rollback();

            playersToList().forEach(player -> {
                if (player != null) {
                    player.teleport(Practice.getInstance().getServerHandler().getSpawnLocation());
                    Practice.getInstance().getServerHandler().giveSpawnItems(player);
                }
            });

            Practice.getInstance().getMatchHandler().getMatchHashMap().remove(playerOne.getUniqueId());
            Practice.getInstance().getMatchHandler().getMatchHashMap().remove(playerTwo.getUniqueId());
        }, time * 20L);
    }

    public Player getOpponent(Player player) {
        if (player.getUniqueId().equals(playerOne.getUniqueId())) return playerTwo;
        return playerOne;
    }

    public List<Player> playersToList() {
        List<Player> players = new ArrayList<>(spectators);
        players.add(playerOne);
        players.add(playerTwo);
        return players;
    }

}