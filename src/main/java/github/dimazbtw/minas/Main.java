package github.dimazbtw.minas;

import github.dimazbtw.minas.actionbar.ActionBarManager;
import github.dimazbtw.minas.commands.MinaAdminCommand;
import github.dimazbtw.minas.commands.MinaCommand;
import github.dimazbtw.minas.commands.MinasCommand;
import github.dimazbtw.minas.commands.XpCommand;
import github.dimazbtw.minas.listeners.*;
import github.dimazbtw.minas.managers.*;
import lombok.Getter;
import me.saiintbrisson.bukkit.command.BukkitFrame;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;

@Getter
public class Main extends JavaPlugin {

    private static Main instance;

    private MineManager mineManager;
    private SelectionManager selectionManager;
    private LanguageManager languageManager;
    private VaultManager vaultManager;
    private SessionManager sessionManager;
    private PlayerLocationTracker locationTracker;
    private DatabaseManager databaseManager;
    private PickaxeManager pickaxeManager;
    private EnchantManager enchantManager;
    private InputManager inputManager;
    private RankingManager rankingManager;
    private SkinManager skinManager;
    private BonusManager bonusManager;

    private MineScoreboardManager scoreboardManager;
    private ActionBarManager actionBarManager;
    private PickaxeBossBarManager pickaxeBossBarManager;

    @Override
    public void onEnable() {
        instance = this;

        // Criar pasta de minas
        File minasFolder = new File(getDataFolder(), "minas");
        if (!minasFolder.exists()) {
            minasFolder.mkdirs();
        }

        // Salvar configs padrão
        saveDefaultConfig();
        saveResource("lang.yml", false);
        saveResource("enchants.yml", false);
        saveResource("skins.yml", false);

        // Inicializar managers (ordem importa!)
        this.languageManager = new LanguageManager(this);
        this.vaultManager = new VaultManager(this);
        this.databaseManager = new DatabaseManager(this);
        this.rankingManager = new RankingManager(this);
        this.enchantManager = new EnchantManager(this);
        this.skinManager = new SkinManager(this);
        this.bonusManager = new BonusManager(this);
        this.pickaxeManager = new PickaxeManager(this);
        this.inputManager = new InputManager(this);
        this.mineManager = new MineManager(this);
        this.sessionManager = new SessionManager(this);
        this.selectionManager = new SelectionManager(this);
        this.locationTracker = new PlayerLocationTracker(this);
        this.scoreboardManager = new MineScoreboardManager(this);
        this.actionBarManager = new ActionBarManager(this);
        this.pickaxeBossBarManager = new PickaxeBossBarManager(this);

        // Registrar comandos
        BukkitFrame frame = new BukkitFrame(this);
        frame.registerCommands(
                new MinasCommand(this),
                new MinaCommand(this),
                new MinaAdminCommand(this),
                new XpCommand(this)
        );

        // Registrar listeners
        Bukkit.getPluginManager().registerEvents(new MineSelectionListener(this), this);
        Bukkit.getPluginManager().registerEvents(new MineProtectionListener(this), this);
        Bukkit.getPluginManager().registerEvents(new MineBlockBreakListener(this), this);
        Bukkit.getPluginManager().registerEvents(new PlayerMoveListener(this), this);
        Bukkit.getPluginManager().registerEvents(new SessionProtectionListener(this), this);
        Bukkit.getPluginManager().registerEvents(new PickaxeListener(this), this);
        Bukkit.getPluginManager().registerEvents(new InputListener(this), this);
        Bukkit.getPluginManager().registerEvents(new SkinActivationListener(this), this);
        Bukkit.getPluginManager().registerEvents(new WorldLoadListener(this), this);


        // Carregar minas
        mineManager.loadAllMines();

        getLogger().info("§acah-Minas carregado com sucesso!");
        getLogger().info("§aEventos customizados: PlayerJoinMineEvent, PlayerQuitMineEvent, MineBlockBreakEvent");
    }

    @Override
    public void onDisable() {
        // Limpar rastreamento de localizações
        if (locationTracker != null) {
            locationTracker.clear();
        }

        // Salvar todas as minas
        if (mineManager != null) {
            mineManager.saveAllMines();
        }

        // Fechar conexão com banco de dados
        if (databaseManager != null) {
            databaseManager.close();
        }

        if (getSessionManager() != null) {
            getSessionManager().removeAllSessions();
        }

        if (scoreboardManager != null) {
            scoreboardManager.removeAll();
        }

        getLogger().info("§ccah-Minas descarregado!");
    }

    public void reload() {
        reloadConfig();
        languageManager.reload();
        enchantManager.reload();
        skinManager.reload();
        bonusManager.reload();
        mineManager.reloadAllMines();
        if (rankingManager != null) {
            rankingManager.clearCache();
        }

        if (getSessionManager() != null) {
            getSessionManager().removeAllSessions();
        }

        if (scoreboardManager != null) {
            scoreboardManager.reload();
        }
    }

    public static Main getInstance() {
        return instance;
    }
}