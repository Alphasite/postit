package postit.server.database;

import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;

/**
 * Created by nishadmathur on 17/4/17.
 */
public class DatabaseUtils {
    public static String getSetupSQL() throws IOException, URISyntaxException {
        URL resource = ClassLoader.getSystemClassLoader().getResource("./database/init_schema.sql");
        byte[] bytes = Files.readAllBytes(Paths.get(resource.toURI()));

        return new String(bytes, StandardCharsets.UTF_8);
    }
}
