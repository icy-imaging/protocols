package plugins.adufour.blocks.util;

public class StopException extends RuntimeException {
    public StopException() {
        super("This workflow was interrupted prematurely");
    }
}
