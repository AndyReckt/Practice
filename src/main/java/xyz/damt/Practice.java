package xyz.damt;

import lombok.Getter;
import me.vaperion.blade.Blade;
import me.vaperion.blade.command.bindings.impl.BukkitBindings;
import me.vaperion.blade.command.bindings.impl.DefaultBindings;
import me.vaperion.blade.command.container.impl.BukkitCommandContainer;
import org.bukkit.Material;
import org.bukkit.plugin.java.JavaPlugin;
import xyz.damt.api.PracticeAPI;
import xyz.damt.arena.Arena;
import xyz.damt.arena.ArenaHandler;
import xyz.damt.arena.ArenaListener;
import xyz.damt.commands.*;
import xyz.damt.commands.providers.ArenaCommandProvider;
import xyz.damt.commands.providers.KitCommandProvider;
import xyz.damt.commands.providers.MaterialCommandProvider;
import xyz.damt.commands.providers.QueueCommandProvider;
import xyz.damt.config.ConfigHandler;
import xyz.damt.handler.MongoHandler;
import xyz.damt.handler.ServerHandler;
import xyz.damt.kit.Kit;
import xyz.damt.kit.KitHandler;
import xyz.damt.listener.InteractListener;
import xyz.damt.listener.ServerListener;
import xyz.damt.listener.StatsListener;
import xyz.damt.match.MatchHandler;
import xyz.damt.match.listener.MatchListener;
import xyz.damt.menu.MenuHandler;
import xyz.damt.placeholder.PracticePlaceHolderHook;
import xyz.damt.profile.ProfileHandler;
import xyz.damt.profile.ProfileListener;
import xyz.damt.queue.Queue;
import xyz.damt.queue.QueueHandler;
import xyz.damt.queue.listener.QueueListener;
import xyz.damt.scoreboard.Adapter;
import xyz.damt.tasks.MongoSaveTask;
import xyz.damt.util.assemble.Assemble;
import xyz.damt.util.assemble.AssembleStyle;

@Getter
public class Practice extends JavaPlugin {

    @Getter private static Practice instance;

    private ConfigHandler configHandler;
    private MongoHandler mongoHandler;
    private ProfileHandler profileHandler;
    private KitHandler kitHandler;
    private ArenaHandler arenaHandler;
    private MatchHandler matchHandler;
    private ServerHandler serverHandler;
    private QueueHandler queueHandler;
    private MenuHandler menuHandler;
    private PracticeAPI practiceAPI;

    @Override
    public void onLoad() {
        instance = this;
        this.saveDefaultConfig();
    }

    @Override
    public void onEnable() {
        this.configHandler = new ConfigHandler(this);
        this.mongoHandler = new MongoHandler(this);
        this.matchHandler = new MatchHandler(this);
        this.serverHandler = new ServerHandler(this);
        this.queueHandler = new QueueHandler(this);
        this.practiceAPI = new PracticeAPI(this);
        this.menuHandler = new MenuHandler(this);

        this.profileHandler = new ProfileHandler(this);
        this.profileHandler.load();

        this.kitHandler = new KitHandler(this);
        this.kitHandler.load();

        this.arenaHandler = new ArenaHandler(this);
        this.arenaHandler.load();

        this.registerPlugin();
        new Assemble(this, new Adapter(this), AssembleStyle.KOHI, 10);
        new MongoSaveTask(this).runTaskTimerAsynchronously(this, 500 * 20L, 500 * 20L);
    }

    private void registerPlugin() {
        this.getServer().getPluginManager().registerEvents(new ProfileListener(), this);
        this.getServer().getPluginManager().registerEvents(new ArenaListener(), this);
        this.getServer().getPluginManager().registerEvents(new QueueListener(), this);
        this.getServer().getPluginManager().registerEvents(new MatchListener(), this);
        this.getServer().getPluginManager().registerEvents(new StatsListener(), this);
        this.getServer().getPluginManager().registerEvents(new ServerListener(), this);
        this.getServer().getPluginManager().registerEvents(new InteractListener(), this);

        Blade.of().binding(new BukkitBindings()).binding(new DefaultBindings()).containerCreator(BukkitCommandContainer.CREATOR)
                .bind(Kit.class, new KitCommandProvider(this))
                .bind(Arena.class, new ArenaCommandProvider(this))
                .bind(Queue.class, new QueueCommandProvider(this))
                .bind(Material.class, new MaterialCommandProvider())
                .fallbackPrefix("practice").tabCompleter(bladeCommandService -> {
        }).build().register(new ViewCommand()).register(new RankedCommand()).register(new UnrankedCommand())
        .register(new LeaveQueueCommand()).register(new KitCommand()).register(new ArenaCommand()).register(new EssentialCommands()).register(new DuelCommand());

        if (configHandler.getSettingsHandler().USE_PLACEHOLDER) {
            new PracticePlaceHolderHook(this).register();
        }
    }

    @Override
    public void onDisable() {
        if (!kitHandler.getKits().isEmpty()) kitHandler.getKits().forEach(kit -> kit.save(false));
        profileHandler.getProfiles().forEach(profile -> profile.save(false));
        arenaHandler.getArenas().forEach(arena -> {
            arena.save(false);
            arena.rollback();
        });

    }



}
