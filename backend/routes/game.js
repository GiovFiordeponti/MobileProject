module.exports = function (router, admin, db, auth, realtimeDatabase, crypto,
    createItem, getItem, updateItem, deleteItemField, findMatch, deleteGame) {

    const constants = require("./../constants");

    /**
     * check for game status
     */
    router.post('/game', async function (req, res) {
        try {
            console.log("post: received %o", req.body)
            let decodedToken = res.locals.decodedToken;
            let lobby = await getItem(constants.GAME_COLLECTION, constants.LOBBY_RESOURCE)
                .catch(err => {
                    console.log("received error %o", err);
                    res.status(404).send(err);
                });

            if (lobby[decodedToken.uid]) {

                if (!lobby[decodedToken.uid].onMatch) {
                    let gameToken = await findMatch(decodedToken.uid, req.body.debug);
                    return res.status(200).send({ result: gameToken != null, created: false, delete: false, info: gameToken })
                }
                else {
                    return res.status(200).send({ result: true, created: false, delete: false, info: lobby[decodedToken.uid].onMatch })
                }
            }
            else {

            }

        } catch (error) {
            console.log(error);
            return res.status(500).send(error);
        }
    });

    /**
     * add a player to a game
     */
    router.put('/game', async function (req, res) {
        try {
            console.log("put: received %o", req.body)
            let decodedToken = res.locals.decodedToken;
            let lobby = await getItem(constants.GAME_COLLECTION, constants.LOBBY_RESOURCE)
                .catch(err => {
                    console.log("received error %o", err);
                    res.status(404).send(err);
                })

            if (!lobby[decodedToken.uid]) {
                req.body["onMatch"] = null;
                lobby[decodedToken.uid] = req.body;
                let result = await updateItem(constants.GAME_COLLECTION, constants.LOBBY_RESOURCE, lobby)
                    .catch(err => {
                        console.log("received error %o", err);
                        res.status(404).send(err);
                    })
                return res.status(200).send({ result: result, delete: false, created: true });
            }
            else {
                // user  is already in a game
                if (lobby[decodedToken.uid]["randomMode"] == false) {
                    return res.status(200).send({ result: true, delete: false })
                }
                else {
                    return res.status(401).send({ error: 401 });
                }
            }
        } catch (error) {
            console.log(error);
            return res.status(500).send(error);
        }
    });

    router.post("/game/finish", async function (req, res) {
        try {
            let body = req.body
            let decodedToken = res.locals.decodedToken;
            console.log("delete: received %o", body)
            // delete game
            if (decodedToken.firebase.sign_in_provider != "anonymous") {
                await deleteGame(decodedToken.uid, body.gameInfo.matchId, body.win, body.draw)
                    .catch(err => {
                        res.status(500).send(err)
                    })
            }
            res.status(200).send({ result: true });
        }
        catch (err) {
            console.log(err);
            return res.status(500).send(err);
        }
    })

    /**
     * delete a game
     */
    router.delete('/game', async function (req, res) {
        try {
            let body = req.body
            let decodedToken = res.locals.decodedToken;
            console.log("delete: received %o", body)
            let lobby = await getItem(constants.GAME_COLLECTION, constants.LOBBY_RESOURCE)
                .catch(err => {
                    console.log("received error %o", err);
                    res.status(404).send({ error: 404 });
                })

            if (lobby[decodedToken.uid]) {
                let result = await deleteItemField(constants.GAME_COLLECTION, constants.LOBBY_RESOURCE, decodedToken.uid)
                    .catch(err => {
                        console.log("received error %o", err);
                        res.status(404).send({ error: 404 });
                    })
                return res.status(200).send({ result: result, delete: true })
            }
        } catch (error) {
            console.log(error);
            return res.status(500).send(error);
        }
    });

}