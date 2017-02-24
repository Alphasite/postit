package CLI;

/**
 * Created by nishadmathur on 22/2/17.
 */
public class CommandLineParser {
    public static void parse(String[] args) {
        if (args.length > 0 && args[0].equals("init")) {
            CommandLineParser.init();
        }
    }

    private static void init() {

    }
}
