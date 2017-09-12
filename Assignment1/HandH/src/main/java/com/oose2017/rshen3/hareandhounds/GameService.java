//-------------------------------------------------------------------------------------------------------------//
// Code based on a tutorial by Shekhar Gulati of SparkJava at
// https://blog.openshift.com/developing-single-page-web-applications-using-java-8-spark-mongodb-and-angularjs/
//-------------------------------------------------------------------------------------------------------------//

package com.oose2017.rshen3.hareandhounds;
import com.fasterxml.uuid.Generators;
import com.google.gson.Gson;
import com.oose2017.rshen3.model.PieceInfo;
import com.oose2017.rshen3.model.PlayerInfo;
import com.oose2017.rshen3.utils.BoardHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sql2o.Connection;
import org.sql2o.Query;
import org.sql2o.Sql2o;
import org.sql2o.Sql2oException;

import javax.sql.DataSource;
import java.util.List;
import java.util.UUID;

public class GameService {

    private Sql2o db;

    private final Logger logger = LoggerFactory.getLogger(GameService.class);

    /**
     * Construct the model with a pre-defined datasource. The current implementation
     * also ensures that the DB schema is created if necessary.
     *
     * @param dataSource
     */
    public GameService(DataSource dataSource) throws GameServiceException {
        db = new Sql2o(dataSource);

        //Create the schema for the database if necessary. This allows this
        //program to mostly self-contained. But this is not always what you want;
        //sometimes you want to create the schema externally via a script.
        try (Connection conn = db.open()) {
            String sqlCreatePlayerInfos = "CREATE TABLE IF NOT EXISTS `PlayerInfos` ( `gameId` INTEGER NOT NULL PRIMARY KEY AUTOINCREMENT, " +
                                                     "`playerId` TEXT NOT NULL, " +
                                                     "`pieceType` TEXT NOT NULL )";

            String sqlCreatePieceInfo = "CREATE TABLE IF NOT EXISTS `PieceInfos` ( `gameId` TEXT NOT NULL, " +
                                                "`pieceType` TEXT NOT NULL, " +
                                                "`x` INTEGER NOT NULL, `y` INTEGER NOT NULL )";
            String sqlCreateGameStatus = "CREATE TABLE IF NOT EXISTS `GameStatus` ( `gameId` TEXT NOT NULL, " +
                                                "`status` TEXT NOT NULL )";

            conn.createQuery(sqlCreatePlayerInfos).executeUpdate();
            conn.createQuery(sqlCreatePieceInfo).executeUpdate();
            conn.createQuery(sqlCreateGameStatus).executeUpdate();
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
        String sqlCreateGameStatus = "INSERT INTO GameStatus (`gameId`, `status`) " +
                                    "VALUES (:gameId, :status)";

        try (Connection conn = db.open()) {
            conn.createQuery(sqlCreateNewGame)
                    .bind(playerInfo)
                    .executeUpdate();
            // Initialize the game status
            conn.createQuery(sqlCreateGameStatus)
                    .addParameter("gameId", uuid.toString())
                    .addParameter("status", "WAITING_FOR_SECOND_PLAYER")
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
        String sqlFetch = "SELECT * FROM PlayerInfos WHERE gameId = :gameId";
        String sqlInsert = "INSERT INTO PlayerInfos (`gameId`, `playerId`, `pieceType`) " +
                                "VALUES (:gameId, :playerId, :pieceType)";
        try (Connection conn = db.open()) {
            // Validate the join game requese
            List<PlayerInfo> playerInfos = conn.createQuery(sqlFetch)
                                                .bind(newPlayer)
                                                .executeAndFetch(PlayerInfo.class);
            if (playerInfos.size() == 0) {
                // No such game ID before
                throw new WrongGameIDException("GameService.joinGame: the game ID does not exist!");
            }
            if (playerInfos.size() == 2) {
                // Two players already
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
                // Update the status of the game
                String sqlUpdateStatus = "UPDATE GameStatus SET status = :status WHERE gameId = :gameId";
                conn.createQuery(sqlUpdateStatus)
                        .addParameter("status", "TURN_HOUND")
                        .addParameter("gameId", gameId)
                        .executeUpdate();
            }
            return newPlayer;
        } catch (Sql2oException ex) {
            logger.error("GameService.joinGame: Failed to query database to join the game", ex);
            throw new GameServiceException("GameService.joinGame: Failed to query database to join the game", ex);
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
}
