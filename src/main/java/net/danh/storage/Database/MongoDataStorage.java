package net.danh.storage.Database;

import com.mongodb.ConnectionString;
import com.mongodb.MongoClientSettings;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.ReplaceOptions;
import net.danh.storage.Storage;
import net.danh.storage.Utils.File;
import org.bson.Document;
import org.bukkit.configuration.file.FileConfiguration;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;

/**
 * Loaded through the isolated driver classloader by DatabaseFactory —
 * do not reference this class directly from plugin code.
 */
public class MongoDataStorage implements IDataStorage {

    private final Storage plugin;
    private MongoClient client;
    private MongoCollection<Document> collection;

    public MongoDataStorage(Storage plugin) {
        this.plugin = plugin;
    }

    @Override
    public void load() {
        FileConfiguration config = File.getConfig();
        String uri = config.getString("database.mongodb.connection-uri", "");
        String database = config.getString("database.mongodb.database", "storage");

        ConnectionString connectionString;
        if (uri != null && !uri.isEmpty()) {
            connectionString = new ConnectionString(uri);
            if (connectionString.getDatabase() != null) {
                database = connectionString.getDatabase();
            }
        } else {
            connectionString = new ConnectionString(buildUri(config, database));
        }

        client = MongoClients.create(MongoClientSettings.builder()
                .applyConnectionString(connectionString)
                .build());
        MongoDatabase db = client.getDatabase(database);
        String prefix = config.getString("database.table-prefix", "");
        collection = db.getCollection(prefix + "players");
        plugin.getLogger().info("Loaded MongoDB Data Storage (collection: " + prefix + "players)");
    }

    private String buildUri(FileConfiguration config, String database) {
        String host = config.getString("database.mongodb.host", "localhost");
        int port = config.getInt("database.mongodb.port", 27017);
        String username = config.getString("database.mongodb.username", "");
        String password = config.getString("database.mongodb.password", "");
        if (username != null && !username.isEmpty()) {
            return "mongodb://" + username + ":" + password + "@" + host + ":" + port + "/" + database;
        }
        return "mongodb://" + host + ":" + port + "/" + database;
    }

    @Override
    @Nullable
    public PlayerData getData(@NotNull String player) {
        Document doc = collection.find(Filters.eq("_id", player)).first();
        return doc == null ? null : fromDocument(doc);
    }

    @Override
    @NotNull
    public List<PlayerData> getAllData() {
        List<PlayerData> result = new ArrayList<>();
        for (Document doc : collection.find()) {
            result.add(fromDocument(doc));
        }
        return result;
    }

    private PlayerData fromDocument(Document doc) {
        return new PlayerData(
                doc.getString("_id"),
                doc.getString("data") == null ? "{}" : doc.getString("data"),
                doc.get("max") instanceof Number ? ((Number) doc.get("max")).intValue() : 0,
                Boolean.TRUE.equals(doc.getBoolean("autopickup")));
    }

    private Document toDocument(PlayerData playerData) {
        return new Document("_id", playerData.player())
                .append("data", playerData.data())
                .append("max", playerData.max())
                .append("autopickup", playerData.autoPickup());
    }

    @Override
    public void createTable(@NotNull PlayerData playerData) {
        save(playerData);
    }

    @Override
    public void updateTable(@NotNull PlayerData playerData) {
        save(playerData);
    }

    private void save(PlayerData playerData) {
        try {
            collection.replaceOne(Filters.eq("_id", playerData.player()), toDocument(playerData),
                    new ReplaceOptions().upsert(true));
        } catch (Exception ex) {
            plugin.getLogger().log(Level.SEVERE, "Could not save player data to MongoDB: " + playerData.player(), ex);
        }
    }

    @Override
    public void deleteData(@NotNull String player) {
        try {
            collection.deleteOne(Filters.eq("_id", player));
        } catch (Exception ex) {
            plugin.getLogger().log(Level.SEVERE, "Could not delete player data from MongoDB: " + player, ex);
        }
    }

    @Override
    public String getType() {
        return "MongoDB";
    }

    @Override
    public void close() {
        if (client != null) {
            client.close();
        }
    }
}