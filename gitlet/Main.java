package gitlet;

/** Driver class for Gitlet, a subset of the Git version-control system.
 *  @author TODO
 */
public class Main {

    /** Usage: java gitlet.Main ARGS, where ARGS contains
     *  <COMMAND> <OPERAND1> <OPERAND2> ... 
     */
    public static void main(String[] args) {
        if (args.length <= 1) {
            exitWithError("Please enter a command.");
        }
        Repository.setupRepository(args[0]);
        String firstArg = args[1];
        /** if the command is not init and the repository is not initialized, exit with error. */
        if (!firstArg.equals("init") && !Repository.isInitialized()) {
            exitWithError("Not in an initialized Gitlet directory.");
        }
        switch (firstArg) {
            case "init":
                Repository.init();
                break;
            case "add":
                Repository.add(args);
                break;
            case "commit":
                Repository.commit(args);
                break;
            case "rm":
                Repository.remove(args);
                break;
            case "log":
                Repository.log();
                break;
            case "global-log":
                Repository.globalLog();
                break;
            case "find":
                Repository.find(args);
                break;
            case "status":
                Repository.status();
                break;
            case "checkout":
                Repository.checkout(args);
                break;
            case "branch":
                Repository.branch(args);
                break;
            case "rm-branch":
                Repository.rmBranch(args);
                break;
            case "reset":
                Repository.reset(args);
                break;
            default:
                exitWithError("No command with that name exists.");
        }
    }

    /**
     * Prints out MESSAGE and exits with error code 0.
     * @param message message to print
     */
    public static void exitWithError(String message) {
        if (message != null && !message.equals("")) {
            System.out.println(message);
        }
        System.exit(0);
    }

}
