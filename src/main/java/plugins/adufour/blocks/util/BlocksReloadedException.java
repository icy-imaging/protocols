package plugins.adufour.blocks.util;

public class BlocksReloadedException extends BlocksException {
    public BlocksReloadedException() {
        super("The plugin list has been updated", true);
    }
}
