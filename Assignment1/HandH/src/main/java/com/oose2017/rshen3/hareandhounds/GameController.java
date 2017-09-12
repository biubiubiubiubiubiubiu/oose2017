//-------------------------------------------------------------------------------------------------------------//
// Code based on a tutorial by Shekhar Gulati of SparkJava at
// https://blog.openshift.com/developing-single-page-web-applications-using-java-8-spark-mongodb-and-angularjs/
//-------------------------------------------------------------------------------------------------------------//

package com.oose2017.rshen3.hareandhounds;

import com.oose2017.rshen3.model.PlayerInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;

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

        put(API_PREFIX + "/:gameId", "application/json", (request, responce)->{
            try {
                logger.info("another player joined the game, id:" + request.params("gameId"));
                PlayerInfo playerInfo = gameService.joinGame(request.params("gameId"));
                responce.status(200);
                return playerInfo;
            } catch (GameService.FullPlayersException e) {
                logger.error("Failed to join the game! The game has two players!");
                responce.status(410);
            } catch (GameService.WrongGameIDException e) {
                logger.error("Failed to join the game! The game ID doesn't exist!");
                responce.status(404);
            } catch (GameService.GameServiceException e) {
                logger.error("Failed to join the game!");
                responce.status(400);
            }
            return Collections.EMPTY_MAP;
        }, new JsonTransformer());



    }
}
