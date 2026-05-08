package net.minecraftforge.network;

public enum NetworkDirection {
    PLAY_TO_CLIENT(NetworkSide.CLIENT),
    PLAY_TO_SERVER(NetworkSide.SERVER);

    private final NetworkSide receptionSide;

    NetworkDirection(NetworkSide receptionSide) {
        this.receptionSide = receptionSide;
    }

    public NetworkSide getReceptionSide() {
        return receptionSide;
    }
}
