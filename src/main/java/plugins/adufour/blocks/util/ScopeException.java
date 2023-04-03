package plugins.adufour.blocks.util;

public class ScopeException extends BlocksException {
    public ScopeException() {
        super("Two variables located in different loops or workflows cannot be directly linked.\n"
                + "Expose the inner-most variable(s) by right-clicking on them and select \"expose variable\"", true);
    }

}
