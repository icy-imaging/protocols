package plugins.adufour.blocks.util;

public class NoSuchLinkException extends BlocksException {
    public NoSuchLinkException(String message) {
        super(message, false);
    }
}
