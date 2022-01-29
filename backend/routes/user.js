const fs = require('fs');
const { USER_COLLECTION } = require('./../constants');
module.exports = function (router, admin, auth, bucket, db, crypto,
    getItem, updateItem, createUser, createItem, uploadPicture, getFriendData) {

    const constants = require("./../constants");

    /**
     * Creates a new user.
     */
    router.put('/users', async function (req, res) {
        let decodedToken = res.locals.decodedToken;
        try {
            let token = req.body.token;
            let user = await getItem(constants.USER_COLLECTION, decodedToken.uid)
                .catch(err => {
                    console.log("users put: error %o", err);
                    res.status(404).send(err);
                });
            let userRecord = await admin.auth().getUser(decodedToken.uid)
                .catch(err => {
                    console.log("put err: error retrieving user", err);
                    res.status(500).send(err);
                });
            let result = null
            if (decodedToken.firebase.sign_in_provider != "anonymous") {
                // if user is not anonymous, you can accept token
                if (!user) {
                    // create new user
                    result = await createUser(decodedToken.uid, token)
                        .catch(function (error) {
                            console.log("put user: create user error %o", error);
                            return res.status(500).send(error);
                        })
                    // update picture for user 
                    await uploadPicture(decodedToken.uid)
                        .catch(err => {
                            console.log("err while loading default picture", err);
                            res.status(500).send(err);
                        })
                    await admin.auth().updateUser(decodedToken.uid, {
                        displayName: null
                    })

                        .catch((error) => {
                            console.log('Error updating user:', error);
                            res.status(500).send(error);
                        });
                }
                else {
                    if (user.token != token) {
                        // update just token
                        await updateItem(constants.USER_COLLECTION, decodedToken.uid, { ['token']: token })
                            .catch(function (error) {
                                console.log(error);
                                return res.status(500).send(error);
                            });
                    }

                }
                // retrieve user friends
                let friendsDataList = [];
                let matchList = [];
                if (user && user["friends"].length > 0) {
                    console.log("put user: retrieving friends data")
                    // if user  has friends, get their data
                    friendsDataList = await Promise.all(user["friends"].map(async function (friendId) {
                        let friendData = await getFriendData(friendId)
                            .catch(err => {
                                console.log(err);
                                res.status(500).send(err);
                            })
                        // if friend has match with player, add to list
                        if (user.matches[friendId]) {
                            matchList.push({
                                gameInfo: user.matches[friendId],
                                friend: friendData
                            })
                        }
                        return friendData
                    }));
                    user["friendsDataList"] = friendsDataList

                }
                if (user) {
                    result = user;
                }
                result.matches = matchList;
                delete result["token"];
            }
            else {
                console.log("user %s is anonymous. no need to store any data", decodedToken.uid);
                if (!userRecord.displayName || (userRecord.displayName && userRecord.displayName=="")) {
                    await admin.auth().updateUser(decodedToken.uid, {
                        displayName: 'anonymous'
                    })
                        .catch((error) => {
                            console.log('Error updating user:', error);
                            res.status(500).send(error);
                        });
                }
            }

            return res.status(200).send(result);

        } catch (error) {
            console.log(error);
            return res.status(500).send(error);
        }
    });

    /**
     * check user nickname (if exists)
     */
    router.post('/users/check', async function (req, res) {

        try {
            let decodedToken = res.locals.decodedToken;

            let nickname = req.body.nickname;
            let userRes = await getItem(constants.USERNAMES_COLLECTION, nickname.trim().toLowerCase())
                .catch(function (error) {
                    console.log("received error %o", error);
                    return res.status(404).send(error);
                })
            let result = false;
            if (!req.body.check && (!userRes || (userRes && userRes.taken == null))) {
                // if nickname is taken or dismissed, can assign to user
                result = await createItem(constants.USERNAMES_COLLECTION, nickname.trim().toLowerCase(), { taken: decodedToken.uid })
                    .catch(function (error) {
                        console.log("received error %o", error);
                        return res.status(404).send(error);
                    })
                if (result) {
                    await admin.auth().updateUser(decodedToken.uid, { displayName: nickname })
                }
            }
            else if (req.body.check && (!userRes || (userRes && userRes.taken == null))) {
                result = true;
            }

            let response = { result: result, check: req.body.check };
            console.log("sending response %o", response);
            return res.status(200).send(response);
        } catch (error) {
            console.log(error);
            return res.status(500).send(error);
        }

    });
}