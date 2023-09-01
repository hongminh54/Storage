package net.danh.storage.Manager.UtilsManager;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import net.danh.storage.Storage;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

public class GitManager {

    public static void checkGitUpdate() {
        Logger logger = Storage.getStorage().getLogger();
        String git_build = getGitBuild();
        if (git_build != null) {
            logger.log(Level.INFO, "Github latest build: " + git_build);
        }
        logger.log(Level.INFO, "Github latest changelog: ");
        List<String> git_msg = getGitMessage();
        if (git_msg != null) {
            git_msg.forEach(message -> {
                logger.log(Level.INFO, message);
            });
        }
    }

    public static String getGitBuild() {
        try (CloseableHttpClient client = HttpClients.createDefault()) {
            HttpGet request = new HttpGet(
                    "https://api.github.com/repos/VoChiDanh/Storage/commits");
            CloseableHttpResponse response = client.execute(request);
            String responseBody = EntityUtils.toString(response.getEntity());

            ObjectMapper mapper = new ObjectMapper();
            JsonNode root = mapper.readTree(responseBody);
            if (root != null) {
                JsonNode latestCommit = root.get(0);
                if (latestCommit != null) {
                    String sha = latestCommit.get("sha").textValue();
                    return sha.substring(0, Math.min(7, sha.length()));
                }
            }
        } catch (IOException ignored) {
        }
        return null;
    }

    public static List<String> getGitMessage() {
        try (CloseableHttpClient client = HttpClients.createDefault()) {
            HttpGet request = new HttpGet(
                    "https://api.github.com/repos/VoChiDanh/Storage/commits");
            CloseableHttpResponse response = client.execute(request);
            String responseBody = EntityUtils.toString(response.getEntity());

            ObjectMapper mapper = new ObjectMapper();
            JsonNode root = mapper.readTree(responseBody);
            if (root != null) {
                JsonNode latestCommit = root.get(0);
                if (latestCommit != null) {
                    String sha = latestCommit.get("commit").get("message").textValue();
                    if (sha != null) {
                        return new ArrayList<>(Arrays.asList(sha.split("\\n")));
                    }
                }
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return null;
    }

}
