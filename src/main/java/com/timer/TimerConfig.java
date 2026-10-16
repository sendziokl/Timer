package com.timer;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.block.Block;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ServerInfo;
import net.minecraft.registry.Registries;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class TimerConfig {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path FILE = FabricLoader.getInstance().getConfigDir().resolve("timer.json");
    private static TimerConfig instance = new TimerConfig();

    public boolean hudEnabled = true;
    /** Na serwerach z listy ponizej sledzone sa WSZYSTKIE bloki, ktore znikaja i wracaja. */
    public boolean autoOnServers = true;
    public List<String> autoServers = new ArrayList<>(List.of("minestar.gg"));
    /** Zasieg (w blokach), w jakim wykrywane sa cudze wykopane bloki. */
    public int radius = 48;
    /** Margines od krawedzi ekranu (w pikselach). */
    public int hudX = 6;
    public int hudY = 6;
    /** 0 = lewy gorny, 1 = prawy gorny, 2 = lewy dolny, 3 = prawy dolny. */
    public int hudCorner = 0;
    /** Rozmiar HUD (0.5 - 3.0). */
    public float hudScale = 1.0f;
    public int maxRows = 8;
    /** id bloku -> czas recznie ustawiony w sekundach (0 = ucz sie automatycznie). */
    public Map<String, Double> tracked = new LinkedHashMap<>();
    /** id bloku -> ostatni zmierzony czas regeneracji w sekundach. */
    public Map<String, Double> learned = new LinkedHashMap<>();

    public static TimerConfig get() {
        return instance;
    }

    public static void load() {
        try {
            if (Files.exists(FILE)) {
                TimerConfig c = GSON.fromJson(Files.readString(FILE), TimerConfig.class);
                if (c != null) {
                    if (c.tracked == null) c.tracked = new LinkedHashMap<>();
                    if (c.learned == null) c.learned = new LinkedHashMap<>();
                    if (c.autoServers == null) c.autoServers = new ArrayList<>(List.of("minestar.gg"));
                    instance = c;
                }
            } else {
                save();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void save() {
        try {
            Files.writeString(FILE, GSON.toJson(instance));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static String idOf(Block b) {
        return Registries.BLOCK.getId(b).toString();
    }

    public boolean autoActive() {
        if (!autoOnServers) return false;
        ServerInfo s = MinecraftClient.getInstance().getCurrentServerEntry();
        if (s == null || s.address == null) return false;
        String a = s.address.toLowerCase();
        for (String x : autoServers) {
            if (!x.isBlank() && a.contains(x.toLowerCase())) return true;
        }
        return false;
    }

    public boolean isTracked(Block b) {
        return autoActive() || tracked.containsKey(idOf(b));
    }

    /** Zwraca czas regeneracji w sekundach albo -1 jesli jeszcze nieznany. */
    public double durationOf(Block b) {
        String id = idOf(b);
        Double manual = tracked.get(id);
        if (manual != null && manual > 0) return manual;
        Double l = learned.get(id);
        return l != null ? l : -1;
    }

    public void learn(Block b, double seconds) {
        learned.put(idOf(b), Math.round(seconds * 10.0) / 10.0);
        save();
    }

    /** @return true jesli dodano, false jesli usunieto */
    public boolean toggle(Block b) {
        String id = idOf(b);
        if (tracked.containsKey(id)) {
            tracked.remove(id);
            save();
            return false;
        }
        tracked.put(id, 0.0);
        save();
        return true;
    }
}
