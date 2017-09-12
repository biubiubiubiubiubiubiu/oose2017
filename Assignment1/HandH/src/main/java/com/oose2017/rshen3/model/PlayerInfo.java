package com.oose2017.rshen3.model;

/**
 * Created by ASUS on 2017/9/12.
 */
public class PlayerInfo {
    private int gameID;
    private String playerId;
    private String pieceType;

    public int getGameID() {
        return gameID;
    }

    public void setGameID(int gameID) {
        this.gameID = gameID;
    }

    public String getPlayerId() {
        return playerId;
    }

    public void setPlayerId(String playerId) {
        this.playerId = playerId;
    }

    public String getPieceType() {
        return pieceType;
    }

    public void setPieceType(String pieceType) {
        this.pieceType = pieceType;
    }
}
