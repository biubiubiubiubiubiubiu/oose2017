package com.oose2017.rshen3.hareandhounds.utils;

import com.oose2017.rshen3.hareandhounds.model.PieceInfo;

import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;

public class BoardHelper {
    private static final int[] HOUNDX = {1, 0, 1};
    private static final int[] HOUNDY = {0, 1, 2};
    private static final int HAREX = 4;
    private static final int HAREY = 1;
    private static final int[] NEXTMOVEHOUNDX = {0, 0, 1, 1, 1};
    private static final int[] NEXTMOVEHOUNDY = {-1, 1, 0, -1, 1};
    private static final int[] NEXTMOVEHAREX = {0, 0, 1, -1, 1, 1, -1, -1};
    private static final int[] NEXTMOVEHAREY = {-1, 1, 0, 0, -1, 1, -1, 1};
    static class Loc{
        int x;
        int y;
        public Loc(int x, int y) {
            this.x = x;
            this.y = y;
        }
    }


    public static List<PieceInfo> generatePieces(String gameID) {
        List<PieceInfo> pieceInfos = new LinkedList<>();
        // Initialize the pieces' location
        for (int i = 0; i < HOUNDX.length; i++) {
            pieceInfos.add(new PieceInfo(gameID, "HOUND", HOUNDX[i], HOUNDY[i]));
        }
        pieceInfos.add(new PieceInfo(gameID, "HARE", HAREX, HAREY));
        return pieceInfos;
    }

    public static boolean validateMove(String pieceType, int fromX, int fromY, int toX, int toY) {
        if (fromX == toX && fromY == toY) {
            return false;
        }
        List<Loc> nextMoves = getNextMoves(pieceType, fromX, fromY);
        boolean found = false;
        for (Loc loc: nextMoves) {
            if (toX == loc.x && toY == loc.y) {
                found = true;
                break;
            }
        }
        return found;
    }

    public static List<Loc> getNextMoves(String pieceType, int fromX, int fromY) {
        List<Loc> nextMoves = new LinkedList<>();
        if (pieceType.equals("HOUND")) {
            if ((fromX == 1 && fromY == 1) || (fromX == 3 && fromY == 1)) {
                for (int i = 0; i < 3; i++) {
                    if (validate(fromX + NEXTMOVEHOUNDX[i], fromY + NEXTMOVEHOUNDY[i])) {
                        nextMoves.add(new Loc(fromX + NEXTMOVEHOUNDX[i], fromY + NEXTMOVEHOUNDY[i]));
                    }
                }
            } else if (fromX == 2 && fromY == 0) {
                nextMoves.add(new Loc(fromX + 1, fromY));
                nextMoves.add(new Loc(fromX, fromY + 1));
            } else if (fromX == 2 && fromY == 2) {
                nextMoves.add(new Loc(fromX + 1, fromY));
                nextMoves.add(new Loc(fromX, fromY - 1));
            } else {
                for (int i = 0; i < NEXTMOVEHOUNDX.length; i++) {
                    if (validate(fromX + NEXTMOVEHOUNDX[i], fromY + NEXTMOVEHOUNDY[i])) {
                        nextMoves.add(new Loc(fromX + NEXTMOVEHOUNDX[i], fromY + NEXTMOVEHOUNDY[i]));
                    }
                }
            }
        } else if (pieceType.equals("HARE")) {
            if ((fromX == 1 && fromY == 1) || (fromX == 3 && fromY == 1)) {
                for (int i = 0; i < 4; i++) {
                    if (validate(fromX + NEXTMOVEHAREX[i], fromY + NEXTMOVEHAREY[i])) {
                        nextMoves.add(new Loc(fromX + NEXTMOVEHAREX[i], fromY + NEXTMOVEHAREY[i]));
                    }
                }
            } else if (fromX == 2 && fromY == 0)  {
                nextMoves.add(new Loc(fromX + 1, fromY));
                nextMoves.add(new Loc(fromX, fromY + 1));
                nextMoves.add(new Loc(fromX - 1, fromY));
            } else if (fromX == 2 && fromY == 2) {
                nextMoves.add(new Loc(fromX + 1, fromY));
                nextMoves.add(new Loc(fromX, fromY - 1));
                nextMoves.add(new Loc(fromX - 1, fromY));
            } else {
                for (int i = 0; i < NEXTMOVEHAREX.length; i++) {
                    if (validate(fromX + NEXTMOVEHAREX[i], fromY + NEXTMOVEHAREY[i])) {
                        nextMoves.add(new Loc(fromX + NEXTMOVEHAREX[i], fromY + NEXTMOVEHAREY[i]));
                    }
                }
            }
        }
        return nextMoves;
    }

    public static String judge(List<PieceInfo> pieceInfos, String state) {
        PieceInfo harePiece = getHare(pieceInfos);
        List<PieceInfo> houndPieces = getHounds(pieceInfos);
        boolean fallBack = true;
        for (PieceInfo houndPiece: houndPieces) {
            if (houndPiece.getX() <= harePiece.getX()) {
                fallBack = false;
                break;
            }
        }
        if (fallBack) {
            return "WIN_HARE_BY_ESCAPE";
        }
        if (state.equals("TURN_HOUND")) {
            // find the next move for hare
            List<Loc> nextMoves = getNextMoves(harePiece.getPieceType(), harePiece.getX(), harePiece.getY());
            for (Loc nextMove: nextMoves) {
                boolean occupied = false;
                for (PieceInfo houndPiece: houndPieces) {
                    if (houndPiece.getX() == nextMove.x && houndPiece.getY() == nextMove.y) {
                        occupied = true;
                        break;
                    }
                }
                if (!occupied) {
                    return "TURN_HARE";
                }
            }
            return "WIN_HOUND";
        } else if (state.equals("TURN_HARE")) {
            return "TURN_HOUND";
        }
        return null;
    }
    private static PieceInfo getHare(List<PieceInfo> pieceInfos) {
        for (PieceInfo pieceInfo: pieceInfos) {
            if (pieceInfo.getPieceType().equals("HARE")) {
                return pieceInfo;
            }
        }
        return null;
    }
    private static List<PieceInfo> getHounds(List<PieceInfo> pieceInfos) {
        PieceInfo harePiece = getHare(pieceInfos);
        pieceInfos.remove(harePiece);
        return pieceInfos;
    }

    private static boolean validate(int x, int y) {
        if (x == 0 && y != 1) {
            return false;
        }
        if (x == 4 && y != 1) {
            return false;
        }
        if (x < 0 || x > 4 || y < 0 || y > 2) {
            return false;
        }
        return true;
    }
}
