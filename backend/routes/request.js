const { REQUESTS_FRIEND } = require("./../constants");
const user = require("./user");

module.exports = function (router, admin, auth, db,
    getItem, updateItem, sendToken, getFriendData, findMatch) {

    const constants = require("./../constants");

    /**
     * send a request
     */
    router.post('/request', async function (req, res) {
        try {
            console.log("received body ", req.body);
            // get decoded token from middleware
            let decodedToken = res.locals.decodedToken;
            // check if it's game request or friend request
            let isGameRequest = req.body.friendCode == ""
            // get friend code from body
            let friendCode = isGameRequest ? req.body.pongFriend.friendCode : req.body.friendCode;
            // get user data
            let userData = await getItem(constants.USER_COLLECTION, decodedToken.uid)
                .catch((error) => {
                    console.log(error);
                    return res.status(500).send(error); // internal error
                });
            // get user id from friend code
            let friendCodeData = await getItem(constants.FRIENDS_CODES_COLLECTION, friendCode)
                .catch((error) => {
                    console.log(error);
                    return res.status(500).send(error); // internal error
                });
            if (friendCodeData) {
                let friendId = friendCodeData.user // retrieve friend id
                let friendData = await getItem(constants.USER_COLLECTION, friendId)
                    .catch((error) => {
                        console.log(error);
                        return res.status(500).send(error); // internal error
                    });
                if (!friendData) {
                    // user not found
                    return res.status(404).send(err);
                }
                // send request only if friend is not already friend with user / a request is waited to be approved
                let isNotFriend = !userData.friends.includes(friendId) && !friendData.friends.includes(decodedToken.uid)

                if (((isGameRequest && !isNotFriend) || isNotFriend)
                    && (friendData.requests.length == 0 || friendData.requests.filter(req => { return !req.id.includes(friendCode) }).length == 0)
                    && friendId != decodedToken.uid) {
                    let requestFriendData = await getFriendData(decodedToken.uid)
                        .catch(err => {
                            console.log(err);
                            res.status(500).send(err);
                        })
                    let request = {
                        id: decodedToken.uid,
                        ts: Date.now(),
                        user: requestFriendData,
                        gameInfo: null,
                        type: isGameRequest ? constants.REQUESTS_GAME : constants.REQUESTS_FRIEND
                    };

                    friendData.requests.push(request)
                    // update friend requests
                    await updateItem(constants.USER_COLLECTION, friendCodeData.user, { ['requests']: friendData.requests })
                        .catch(err => {
                            return res.status(500).send(err); // internal error
                        });
                    // delete requests from token msg
                    delete request["user"]["requests"];

                    await sendToken([friendData.token], request)
                        .catch(err => {
                            return res.status(500).send(err); // internal error
                        });

                    return res.status(200).send({ result: true })
                }
                else {
                    return res.status(409).send(); // user has already sent this request
                }
            }
            else {
                // user not found
                return res.status(404).send({});
            }

        } catch (error) {
            console.log(error);
            return res.status(500).send(error);
        }
    });

    /**
     * respond to a request
     */
    router.post('/request/respond', async function (req, res) {
        try {
            let decodedToken = res.locals.decodedToken;
            let userData = await getItem(constants.USER_COLLECTION, decodedToken.uid)
                .catch((error) => {
                    console.log(error);
                    return res.status(500).send(error); // internal error
                });
            let result = false;
            console.log("body is ", req.body);
            let accept = req.body.accept;
            let request = userData.requests.filter(request => { return request.id == req.body.friendId && request.ts == req.body.ts })
            let requestsToKeep = userData.requests.filter(request => { return request.id != req.body.friendId && request.ts != req.body.ts })
            console.log("request is ", request);
            if (request.length == 1) {
                request = request[0] // consider first and only item
                // request exist and its unique, handle it
                let friendData = await getItem(constants.USER_COLLECTION, request.user.id)
                    .catch((error) => {
                        console.log(error);
                        return res.status(500).send(error); // internal error
                    });
                if (accept) {
                    // create game tokens
                    let userGameToken, friendGameToken = null;
                    let notificationType = "";
                    switch (request.type) {
                        case constants.REQUESTS_FRIEND:
                            notificationType = constants.REQUEST_ACCEPT_FRIEND; // set notification type
                            // add friend to list
                            if (!userData.friends.includes(request.user.id)) userData.friends.push(request.user.id);
                            if (!friendData.friends.includes(decodedToken.id)) friendData.friends.push(decodedToken.uid);
                            // update friend lists
                            await updateItem(constants.USER_COLLECTION, decodedToken.uid, { ['friends']: userData.friends })
                                .catch(err => {
                                    return res.status(500).send(err); // internal error
                                });
                            await updateItem(constants.USER_COLLECTION, request.user.id, { ['friends']: friendData.friends })
                                .catch(err => {
                                    return res.status(500).send(err); // internal error
                                });
                            break;
                        case constants.REQUESTS_GAME:
                            notificationType = constants.REQUEST_ACCEPT_GAME // set notification type
                            // create game
                            let matches = await findMatch(decodedToken.uid, false, request.user.id);
                            userGameToken = matches[0]
                            friendGameToken = matches[1]
                            console.log("created tokens ", userGameToken, friendGameToken)
                            friendData.matches[request.user.id] = friendGameToken;
                            userData.matches[decodedToken.uid] = userGameToken;
                            // update match lists
                            await updateItem(constants.USER_COLLECTION, decodedToken.uid, { ['matches']: userData.matches })
                                .catch(err => {
                                    return res.status(500).send(err); // internal error
                                });
                            await updateItem(constants.USER_COLLECTION, request.user.id, { ['matches']: friendData.matches })
                                .catch(err => {
                                    return res.status(500).send(err); // internal error
                                });
                            break;
                    }
                    result = true;
                    // sends notifications to user
                    let notificationUserData = await getFriendData(request.user.id)
                        .catch(err => {
                            console.log(err);
                            res.status(500).send(err);
                        });
                    delete notificationUserData["requests"]
                    let userNotification = {
                        id: decodedToken.uid,
                        ts: Date.now(),
                        type: notificationType,
                        user: notificationUserData,
                        gameInfo: userGameToken
                    };
                    // sends notifications to friend 
                    notificationUserData = await getFriendData(decodedToken.uid)
                        .catch(err => {
                            console.log(err);
                            res.status(500).send(err);
                        })
                    delete notificationUserData["requests"]
                    let friendNotification = {
                        id: request.user.id,
                        ts: Date.now(),
                        type: notificationType,
                        user: notificationUserData,
                        gameInfo: friendGameToken
                    };

                    console.log("respond: sending notifications ", userNotification, friendNotification)

                    await sendToken([friendData.token], friendNotification)
                        .catch(err => {
                            return res.status(500).send(err); // internal error
                        });
                    await sendToken([userData.token], userNotification)
                        .catch(err => {
                            return res.status(500).send(err); // internal error
                        });
                }
                // update friend requests
                await updateItem(constants.USER_COLLECTION, decodedToken.uid, { ['requests']: requestsToKeep || [] })
                    .catch(err => {
                        return res.status(500).send(err); // internal error
                    });
            }
            res.status(200).send({ result: result });
        } catch (error) {
            console.log(error);
            return res.status(500).send(error);
        }
    });

}