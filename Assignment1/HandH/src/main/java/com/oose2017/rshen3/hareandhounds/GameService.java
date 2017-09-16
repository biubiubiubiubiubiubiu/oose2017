//-------------------------------------------------------------------------------------------------------------//
// Code based on a tutorial by Shekhar Gulati of SparkJava at
// https://blog.openshift.com/developing-single-page-web-applications-using-java-8-spark-mongodb-and-angularjs/
//-------------------------------------------------------------------------------------------------------------//

package com.oose2017.rshen3.hareandhounds;
import com.fasterxml.uuid.Generators;
import com.google.gson.Gson;
import com.oose2017.rshen3.hareandhounds.model.GameState;
import com.oose2017.rshen3.hareandhounds.model.MovePiece;
import com.oose2017.rshen3.hareandhounds.model.PieceInfo;
import com.oose2017.rshen3.hareandhounds.model.PlayerInfo;
import com.oose2017.rshen3.hareandhounds.utils.BoardHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sql2o.Connection;
import org.sql2o.Sql2o;
import org.sql2o.Sql2oException;

import javax.sql.DataSource;
import java.util.*;

public class GameService {

    private Sql2o db;

    private final Logger logger = LoggerFactory.getLogger(GameService.class);
    private HashMap<String, String> stateMap;
    /**
     * Construct the model with a pre-defined datasource. The current implementation
     * also ensures that the DB schema is created if necessary.
     *
     * @param dataSource
     */
    public GameService(DataSource dataSource) throws GameServiceException {
        stateMap = new HashMap<>();
        stateMap.put("HARE_player", "TURN_HARE");
        stateMap.put("HOUND_player", "TURN_HOUND");
        db = new Sql2o(dataSource);

        //Create the schema for the database if necessary. This allows this
        //program to mostly self-contained. But this is not always what you want;
        //sometimes you want to create the schema externally via a script.
        try (Connection conn = db.open()) {
            String sqlCreatePlayerInfos = "CREATE TABLE IF NOT EXISTS `PlayerInfos` ( `gameId` TEXT NOT NULL, " +
                                                     "`playerId` TEXT NOT NULL, " +
                                                     "`pieceType` TEXT NOT NULL )";

            String sqlCreatePieceInfo = "CREATE TABLE IF NOT EXISTS `PieceInfos` ( `gameId` TEXT NOT NULL, " +
                                                "`pieceType` TEXT NOT NULL, " +
                                                "`x` INTEGER NOT NULL, `y` INTEGER NOT NULL )";
            String sqlCreateGameStatus = "CREATE TABLE IF NOT EXISTS `GameStates` ( `gameId` TEXT NOT NULL, " +
                                                "`state` TEXT NOT NULL )";
            String sqlCreateGameRecord = "CREATE TABLE IF NOT EXISTS `GameRecord` ( `gameId` TEXT NOT NULL, " +
                    "`moveRecord` TEXT NOT NULL )";

            conn.createQuery(sqlCreatePlayerInfos).executeUpdate();
            conn.createQuery(sqlCreatePieceInfo).executeUpdate();
            conn.createQuery(sqlCreateGameStatus).executeUpdate();
            conn.createQuery(sqlCreateGameRecord).executeUpdate();
        } catch(Sql2oException ex) {
            logger.error("Failed to create schema at startup", ex);
            throw new GameServiceException("Failed to create schema at startup", ex);
        }
    }

    /**
     * Create a new game for the input piece type
     *
     * @return the gameId, playerId, pieceType
     */
    public PlayerInfo createGame(String body) throws GameServiceException {
        UUID uuid = Generators.timeBasedGenerator().generate();
        PlayerInfo playerInfo = new Gson().fromJson(body, PlayerInfo.class);
        playerInfo.setPlayerId(playerInfo.getPieceType() + "_player");
        playerInfo.setGameId(uuid.toString());
        // Insert the new game info into the database
        String sqlCreateNewGame = "INSERT INTO PlayerInfos (`gameId`, `playerId`, `pieceType`) " +
                                    "VALUES (:gameId, :playerId, :pieceType)";
        String sqlCreatePieces = "INSERT INTO PieceInfos (`gameId`, `pieceType`, `x`, `y`) " +
                                    "VALUES (:gameId, :pieceType, :x, :y)";
        String sqlCreateGameStatus = "INSERT INTO GameStates (`gameId`, `state`) " +
                                    "VALUES (:gameId, :state)";

        try (Connection conn = db.open()) {
            conn.createQuery(sqlCreateNewGame)
                    .bind(playerInfo)
                    .executeUpdate();
            // Initialize the game state
            conn.createQuery(sqlCreateGameStatus)
                    .addParameter("gameId", uuid.toString())
                    .addParameter("state", "WAITING_FOR_SECOND_PLAYER")
                    .executeUpdate();
            // Initialize the piece location in the board
            List<PieceInfo> pieceInfos = BoardHelper.generatePieces(uuid.toString());
            for (PieceInfo pieceInfo: pieceInfos) {
                conn.createQuery(sqlCreatePieces)
                        .bind(pieceInfo)
                        .executeUpdate();
            }
            return playerInfo;
        } catch(Sql2oException ex) {
            logger.error("GameService.createGame: Failed to query database to create the new game", ex);
            throw new GameServiceException("GameService.createGame: Failed to query database to create the new game", ex);
        }
    }

    /**
     * Join the game with specific ID
     *
     * @return the gameId, playerId, pieceType
     */
    public PlayerInfo joinGame(String gameId) throws FullPlayersException, WrongGameIDException, GameServiceException {
        PlayerInfo newPlayer = new PlayerInfo();
        newPlayer.setGameId(gameId);
        String sqlFetchPlayers = "SELECT * FROM PlayerInfos WHERE gameId = :gameId";
        String sqlInsert = "INSERT INTO PlayerInfos (`gameId`, `playerId`, `pieceType`) " +
                                "VALUES (:gameId, :playerId, :pieceType)";
        String sqlFetchPieces = "SELECT * FROM PieceInfos WHERE gameId = :gameId";
        String sqlInsertPieceStates = "INSERT INTO GameRecord (`gameId`, `moveRecord`) " +
                                "VALUES (:gameId, :moveRecord)";
        try (Connection conn = db.open()) {
            // Validate the join game requese
            List<PlayerInfo> playerInfos = conn.createQuery(sqlFetchPlayers)
                                                .bind(newPlayer)
                                                .executeAndFetch(PlayerInfo.class);
            if (playerInfos.size() == 0) {
                // No such game ID before
                logger.error("GameService.joinGame: the game ID does not exist!");
                throw new WrongGameIDException("GameService.joinGame: the game ID does not exist!");
            }
            if (playerInfos.size() == 2) {
                // Two players already
                logger.error("GameService.joinGame: Already two players exist!");
                throw new FullPlayersException("GameService.joinGame: Already two players exist!");
            }
            if (playerInfos.size() == 1) {
                if (playerInfos.get(0).getPieceType().equals("HOUND")) {
                    newPlayer.setPlayerId("HARE_player");
                    newPlayer.setPieceType("HARE");
                } else {
                    newPlayer.setPlayerId("HOUND_player");
                    newPlayer.setPieceType("HOUND");
                }
                // Insert the joined player into database
                conn.createQuery(sqlInsert)
                        .bind(newPlayer)
                        .executeUpdate();
                // Update the state of the game
                String sqlUpdateStatus = "UPDATE GameStates SET state = :state WHERE gameId = :gameId";
                conn.createQuery(sqlUpdateStatus)
                        .addParameter("state", "TURN_HOUND")
                        .addParameter("gameId", gameId)
                        .executeUpdate();
            }
            // Record the initial piece status
            List<PieceInfo> pieceInfos = conn.createQuery(sqlFetchPieces)
                                                .addParameter("gameId", gameId)
                                                .executeAndFetch(PieceInfo.class);
            String pieceStates = BoardHelper.getPieceStates(pieceInfos);
            conn.createQuery(sqlInsertPieceStates)
                    .addParameter("gameId", gameId)
                    .addParameter("moveRecord", pieceStates)
                    .executeUpdate();
            return newPlayer;
        } catch (Sql2oException ex) {
            logger.error("GameService.joinGame: Failed to query database to join the game", ex);
            throw new GameServiceException("GameService.joinGame: Failed to query database to join the game", ex);
        }
    }

    public GameState fetchState(String gameId) throws WrongGameIDException, GameServiceException{
        String sqlFetchStatus = "SELECT * FROM GameStates WHERE gameId = :gameId";
        try (Connection conn = db.open()) {
            List<GameState> gameStates = conn.createQuery(sqlFetchStatus)
                    .addParameter("gameId", gameId)
                    .executeAndFetch(GameState.class);
            if (gameStates.size() == 0) {
                logger.error("GameService.fetchStatus: Wrong gameId");
                throw new WrongGameIDException("GameService.fetchStatus: Wrong gameId");
            }
            return gameStates.get(0);
        } catch (Sql2oException ex) {
            logger.error("GameService.fetchStatus: Failed to query database to fetch state", ex);
            throw new GameServiceException("GameService.fetchStatus: Failed to query database to fetch state", ex);
        }
    }

    public List<PieceInfo> fetchBoard(String gameId) throws WrongGameIDException, GameServiceException{
        String sqlFetchBoard = "SELECT * FROM PieceInfos WHERE gameId = :gameId";
        try (Connection conn = db.open()) {
            List<PieceInfo> pieceInfos = conn.createQuery(sqlFetchBoard)
                                                .addParameter("gameId", gameId)
                                                .executeAndFetch(PieceInfo.class);
            if (pieceInfos.size() < 4) {
                logger.info("GameService.fetchBoard: Wrong gameId");
                throw new WrongGameIDException("GameService.fetchBoard: Wrong gameId");
            }
            return pieceInfos;
        } catch (Sql2oException ex) {
            logger.error("GameService.fetchBoard: Failed to query database to fetch board", ex);
            throw new GameServiceException("GameService.fetchBoard: Failed to query database to fetch board", ex);
        }
    }

    public PlayerInfo makeMove(String body) throws WrongGameIDException,
                                                   WrongPlayerIDException,
                                                   IncorrectTurn,
                                                   IllegalMove,
                                                   GameServiceException {
        MovePiece movePiece = new Gson().fromJson(body, MovePiece.class);
        PlayerInfo playerInfo = new Gson().fromJson(body, PlayerInfo.class);
        String playerId = movePiece.getPlayerId();
        // Validate the move
        if (!stateMap.containsKey(playerId)) {
            logger.error("GameService.makeMove: Wrong player id");
            throw new WrongPlayerIDException("GameService.makeMove: Wrong player id");
        }
        String sqlFetchState = "SELECT state FROM GameStates WHERE gameId = :gameId";
        String sqlFetchPiece = "SELECT * FROM PieceInfos WHERE gameId = :gameId";
        String sqlFetchPieceStates = "SELECT COUNT(*) FROM GameRecord WHERE gameId = :gameId " +
                                        "AND moveRecord = :moveRecord";
        String sqlUpdatePieceStates = "INSERT INTO GameRecord (`gameId`, `moveRecord`) " +
                                        "VALUES(:gameId, :moveRecord)";
        String sqlDeletePieceStates = "DELETE FROM GameRecord WHERE `gameId` = :gameId";
        try (Connection conn = db.open()) {
            String state = conn.createQuery(sqlFetchState)
                                .addParameter("gameId", movePiece.getGameId())
                                .executeScalar(String.class);
            if (state == null) {
                // Wrong gameId
                logger.error("GameService.makeMove: Wrong game id");
                throw new WrongGameIDException("GameService.makeMove: Wrong game id");
            }
            if (!stateMap.get(playerId).equals(state)) {
                // Wrong turn
                logger.error("GameService.makeMove: it is not your turn!");
                throw new IncorrectTurn("GameService.makeMove: it is not your turn!");
            }
            String pieceType = playerId.split("_")[0];
            playerInfo.setPieceType(pieceType);
            List<PieceInfo> pieceInfos = conn.createQuery(sqlFetchPiece)
                                            .addParameter("gameId", movePiece.getGameId())
                                            .executeAndFetch(PieceInfo.class);
            boolean notFound = true;
            PieceInfo changePiece = null;
            for (PieceInfo pieceInfo: pieceInfos) {
                if (pieceInfo.getX() == movePiece.getFromX() && pieceInfo.getY() == movePiece.getFromY()) {
                    changePiece = pieceInfo;
                    notFound = false;
                }
                if (pieceInfo.getX() == movePiece.getToX() && pieceInfo.getY() == movePiece.getToY()) {
                    // meaning that this location is already occupied
                    logger.error("GameService.makeMove: the destination for that piece is occupied!");
                    throw new IllegalMove("GameService.makeMove: the destination for that piece is occupied!");
                }
            }
            if (!pieceType.equals(changePiece.getPieceType())) {
                // Player picked the wrong piece.
                logger.error("GameService.makeMove: you have picked the wrong piece!");
                throw new IllegalMove("GameService.makeMove: you have picked the wrong piece!");
            }
            if (notFound) {
                // Meaning that the moving piece has a wrong location.
                logger.error("GameService.makeMove: Wrong piece for the from location!");
                throw new IllegalMove("GameService.makeMove: Wrong piece for the from location!");
            }
            // Validate the "to" location
            if (BoardHelper.validateMove(pieceType, movePiece.getFromX(),
                                                movePiece.getFromY(),
                                                movePiece.getToX(),
                                                movePiece.getToY())){
                // Made a validate move:
                String updateSql = "UPDATE PieceInfos SET x = :toX, y = :toY WHERE gameId = :gameId " +
                        "AND x = :fromX " +
                        "AND y = :fromY";
                conn.createQuery(updateSql)
                        .bind(movePiece)
                        .executeUpdate();
            } else {
                logger.error("GameService.makeMove: Probably the piece cannot reach there");
                throw new IllegalMove("GameService.makeMove: Probably the piece cannot reach there");
            }
            // update the game status
            String judgeResult = "";
            changePiece.setX(movePiece.getToX());
            changePiece.setY(movePiece.getToY());
            String pieceStates = BoardHelper.getPieceStates(pieceInfos);
            int count = conn.createQuery(sqlFetchPieceStates)
                                .addParameter("gameId", movePiece.getGameId())
                                .addParameter("moveRecord", pieceStates)
                                .executeScalar(Integer.class);
            if (count == 2) {
                judgeResult = "WIN_HARE_BY_STALLING";
            } else {
                conn.createQuery(sqlUpdatePieceStates)
                        .addParameter("gameId", movePiece.getGameId())
                        .addParameter("moveRecord", pieceStates)
                        .executeUpdate();
                judgeResult = BoardHelper.judge(pieceInfos, state);
            }
            String sqlUpdateState = "UPDATE GameStates set state = :state WHERE gameId = :gameId";
            conn.createQuery(sqlUpdateState)
                    .addParameter("state", judgeResult)
                    .addParameter("gameId", movePiece.getGameId())
                    .executeUpdate();
            return playerInfo;
        } catch (Sql2oException ex) {
            logger.error("GameService.makeMove: Failed to query database to move piece", ex);
            throw new GameServiceException("GameService.makeMove: Failed to query database to move piece", ex);
        }
    }

    //-----------------------------------------------------------------------------//
    // Helper Classes and Methods
    //-----------------------------------------------------------------------------//

    public static class GameServiceException extends Exception {
        public GameServiceException(String message, Throwable cause) {
            super(message, cause);
        }
    }

    /**
     * This Sqlite specific method returns the number of rows changed by the most recent
     * INSERT, UPDATE, DELETE operation. Note that you MUST use the same connection to get
     * this information
     */
    private int getChangedRows(Connection conn) throws Sql2oException {
        return conn.createQuery("SELECT changes()").executeScalar(Integer.class);
    }

    public static class FullPlayersException extends Exception {
        public FullPlayersException(String message) {
            super(message);
        }
    }
    public static class WrongGameIDException extends Exception {
        public WrongGameIDException(String message) {
            super(message);
        }
    }
    public static class WrongPlayerIDException extends Exception {
        public WrongPlayerIDException(String message) { super(message); }
    }
    public static class IncorrectTurn extends Exception {
        public IncorrectTurn(String message) { super(message); }
    }
    public static class  IllegalMove extends Exception {
        public IllegalMove(String message) { super(message); }
    }
}
