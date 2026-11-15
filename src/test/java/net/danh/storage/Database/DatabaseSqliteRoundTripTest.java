package net.danh.storage.Database;

import net.danh.storage.Database.connection.SqliteConnectionFactory;
import net.danh.storage.Storage;
import net.danh.storage.Utils.File;
import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.MockedStatic;

import java.nio.file.Path;
import java.util.List;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

class DatabaseSqliteRoundTripTest {

    @TempDir
    Path tempDir;

    @Test
    void playerDataCrudRoundTrip() {
        YamlConfiguration config = new YamlConfiguration();
        config.set("database.table-prefix", "");

        Storage plugin = mock(Storage.class);
        when(plugin.getLogger()).thenReturn(Logger.getLogger("db-test"));
        when(plugin.getResource(anyString())).thenAnswer(inv ->
                DatabaseSqliteRoundTripTest.class.getResourceAsStream("/" + inv.getArgument(0)));

        try (MockedStatic<File> fileMock = mockStatic(File.class);
             MockedStatic<Storage> storageMock = mockStatic(Storage.class)) {
            fileMock.when(File::getConfig).thenReturn(config);
            storageMock.when(Storage::getStorage).thenReturn(plugin);

            Database db = new Database(new SqliteConnectionFactory(tempDir.resolve("test.db")));
            db.load();

            PlayerData steve = new PlayerData("Steve", "{\"STONE\":64}", 5000, true);
            db.createTable(steve);
            assertEquals(steve, db.getData("Steve"));

            PlayerData updated = new PlayerData("Steve", "{\"STONE\":128}", 9000, false);
            db.updateTable(updated);
            assertEquals(updated, db.getData("Steve"));

            PlayerData alex = new PlayerData("Alex", "{}", 100000, false);
            PlayerData steveAgain = new PlayerData("Steve", "{\"DIRT\":1}", 7000, true);
            db.bulkSave(List.of(alex, steveAgain));
            assertEquals(alex, db.getData("Alex"));
            assertEquals(steveAgain, db.getData("Steve"), "bulkSave must upsert existing rows");
            assertEquals(2, db.getAllData().size());

            db.deleteData("Alex");
            assertNull(db.getData("Alex"));
            assertEquals(1, db.getAllData().size());

            db.close();
        }
    }
}