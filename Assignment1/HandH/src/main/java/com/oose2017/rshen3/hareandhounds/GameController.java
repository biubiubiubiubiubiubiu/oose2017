//-------------------------------------------------------------------------------------------------------------//
// Code based on a tutorial by Shekhar Gulati of SparkJava at
// https://blog.openshift.com/developing-single-page-web-applications-using-java-8-spark-mongodb-and-angularjs/
//-------------------------------------------------------------------------------------------------------------//

package com.oose2017.rshen3.hareandhounds;

import com.oose2017.rshen3.hareandhounds.model.GameState;
import com.oose2017.rshen3.hareandhounds.model.PieceInfo;
import com.oose2017.rshen3.hareandhounds.model.PlayerInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.List;

import static spark.Spark.*;

public class GameController {

    private static final String API_PREFIX = "/hareandhounds/api/games";

    private final GameService gameService;

    private final Logger logger = LoggerFactory.getLogger(GameController.class);

    public GameController(GameService gameService) {
        this.gameService = gameService;
        setupEndpoints();
    }

    private void setupEndpoints() {
        post(API_PREFIX, "application/json", (request, response) -> {
            try {
                logger.info("Creating a new game for" + request.body());
                PlayerInfo playerInfo = gameService.createGame(request.body());
                response.status(201);
                return playerInfo;
            } catch (GameService.GameServiceException ex) {
                logger.error("Failed to create a new game!");
                response.status(400);
                return Collections.EMPTY_MAP;
            }
        }, new JsonTransformer());

        put(API_PREFIX + "/:gameId", "application/json", (request, response)->{
            try {
                logger.info("another player is trying to join the game, id:" + request.params("gameId"));
                PlayerInfo playerInfo = gameService.joinGame(request.params("gameId"));
                response.status(200);
                logger.info("joining game success!");
                return playerInfo;
            } catch (GameService.FullPlayersException e) {
                logger.error("Failed to join the game! The game has two players!");
                response.status(410);
            } catch (GameService.WrongGameIDException e) {
                logger.error("Failed to join the game! The game ID doesn't exist!");
                response.status(404);
            } catch (GameService.GameServiceException e) {
                logger.error("Failed to join the game!");
                response.status(400);
            }
            return Collections.EMPTY_MAP;
        }, new JsonTransformer());

        get(API_PREFIX + "/:gameId" + "/state", "application/json", (request, response) -> {
            try {
                GameState gameState = gameService.fetchState(request.params("gameId"));
                response.status(200);
                return gameState;
            } catch (GameService.WrongGameIDException ex) {
                logger.error("Failed to fetch game state: gameId does not exist!");
                response.status(404);
            } catch (GameService.GameServiceException ex) {
                response.status(400);
                logger.error("Failed to fetch game state");
            }
            return Collections.EMPTY_MAP;
        }, new JsonTransformer());

        get(API_PREFIX + "/:gameId" + "/board", "application/json", (request, response) -> {
            try {
                List<PieceInfo> pieceInfoList = gameService.fetchBoard(request.params("gameId"));
                response.status(200);
                return pieceInfoList;
            } catch (GameService.WrongGameIDException ex) {
                logger.error("Failed to fetch game board: gameId does not exist!");
                response.status(404);
            } catch (GameService.GameServiceException ex) {
                logger.error("Failed to fetch game board");
                response.status(400);
            }
            return Collections.EMPTY_MAP;
        }, new JsonTransformer());

        post(API_PREFIX + "/:gameId" + "/turns", "application/json", (request, response) -> {
            try {
                PlayerInfo playerInfo = gameService.makeMove(request.body());
                response.status(200);
                return playerInfo;
            } catch (GameService.WrongGameIDException ex) {
                logger.error("Failed to make a move: gameId does not exist!");
                response.status(404);
                return ("INVALID_GAME_ID");
            } catch (GameService.WrongPlayerIDException ex) {
                logger.error("Failed to make a move, incorrect playerId");
                response.status(404);
                return ("INVALID_PLAYER_ID");
            } catch (GameService.IllegalMove ex) {
                logger.error("Failed to make a move, illegal move");
                response.status(422);
                return ("ILLEGAL_MOVE");
            } catch (GameService.IncorrectTurn ex) {
                logger.error("Failed to make a move, incorrect turn");
                response.status(422);
                return ("INCORRECT_TURN");
            } catch (GameService.GameServiceException ex) {
                logger.error("Failed to make a move.");
                response.status(400);
                return ("BAD_REQUEST");
            }
        }, new JsonTransformer());
    }
}
