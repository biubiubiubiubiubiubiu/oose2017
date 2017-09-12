package com.oose2017.rshen3.utils;

import com.oose2017.rshen3.model.PieceInfo;

import java.util.LinkedList;
import java.util.List;

public class BoardHelper {
    private static final int[] HOUNDX = {1, 0, 1};
    private static final int[] HOUNDY = {0, 1, 2};
    private static final int HAREX = 4;
    private static final int HAREY = 1;
    public static List<PieceInfo> generatePieces(String gameID) {
        List<PieceInfo> pieceInfos = new LinkedList<>();
        // Initialize the pieces' location
        for (int i = 0; i < HOUNDX.length; i++) {
            pieceInfos.add(new PieceInfo(gameID, "HOUND", HOUNDX[i], HOUNDY[i]));
        }
        pieceInfos.add(new PieceInfo(gameID, "HARE", HAREX, HAREY));
        return pieceInfos;
    }
}
