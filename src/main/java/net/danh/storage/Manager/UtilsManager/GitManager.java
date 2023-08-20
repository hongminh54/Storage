package net.danh.storage.Manager.UtilsManager;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import net.danh.storage.Storage;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

public class GitManager {

    public static void checkGitUpdate() {
        Logger logger = Storage.getStorage().getLogger();
        logger.log(Level.INFO, "Github latest build: " + getGitBuild());
        logger.log(Level.INFO, "Github latest changelog: ");
        getGitMessage().forEach(message -> {
            logger.log(Level.INFO, message);
        });
    }

    public static @NotNull String getGitBuild() {
        try (CloseableHttpClient client = HttpClients.createDefault()) {
            HttpGet request = new HttpGet(
                    "https://api.github.com/repos/VoChiDanh/Storage/commits");
            CloseableHttpResponse response = client.execute(request);
            String responseBody = EntityUtils.toString(response.getEntity());

            ObjectMapper mapper = new ObjectMapper();
            JsonNode root = mapper.readTree(responseBody);
            JsonNode latestCommit = root.get(0);
            String sha = latestCommit.get("sha").textValue();
            return sha.substring(0, Math.min(7, sha.length()));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static @NotNull List<String> getGitMessage() {
        try (CloseableHttpClient client = HttpClients.createDefault()) {
            HttpGet request = new HttpGet(
                    "https://api.github.com/repos/VoChiDanh/Storage/commits");
            CloseableHttpResponse response = client.execute(request);
            String responseBody = EntityUtils.toString(response.getEntity());

            ObjectMapper mapper = new ObjectMapper();
            JsonNode root = mapper.readTree(responseBody);
            JsonNode latestCommit = root.get(0);
            String sha = latestCommit.get("commit").get("message").textValue();
            return new ArrayList<>(Arrays.asList(sha.split("\\n")));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

}
