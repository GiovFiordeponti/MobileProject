
/** BASIC SETUP */
const express = require('express');
const cors = require('cors');
const fs = require('fs')
const app = express();
app.use(express.json());
const https = require('https');
app.use(cors({ origin: true }));
const router = express.Router();
/** FIRESTONE */
const admin = require('firebase-admin');
const serviceAccount = require("./cert/permissions.json");
/** CRYPTO */
const crypto = require("crypto");
/** STREAM (for image loading) */
const stream = require('stream');
admin.initializeApp({
    credential: admin.credential.cert(serviceAccount),
    databaseURL: "https://mobileproject-7e882-default-rtdb.firebaseio.com",
    storageBucket: "mobileproject-7e882.appspot.com"
});

const db = admin.firestore();
const realtimeDatabase = admin.database();
const auth = admin.auth();
const bucket = admin.storage().bucket();

const constants = require("./constants");
const { debug } = require('console');

/**
 * Get bearer token from http request
 * @param {*} headers 
 */
function getToken(headers) {
    let token = null;
    let bearerToken = headers.authorization;
    if (bearerToken != null && bearerToken.split(' ')[0] == 'Bearer') {
        if (bearerToken.split(' ').length > 1 && bearerToken.split(' ')[1] != null) {
            token = bearerToken.split(' ')[1];
        }
    }
    return token;
}

/**
 * 
 * @param {*} collection 
 * @param {*} resourceId 
 * @param {*} body 
 */
function createItem(collection, resourceId, body) {
    return new Promise((function (resolve, reject) {
        db.collection(collection).doc('/' + resourceId + '/')
            .create(body)
            .then(() => {
                resolve(true);
            })
            .catch(err => {
                reject(err);
            })
    }).bind(this))
}


function createUser(userId, token) {
    return new Promise((async function (resolve, reject) {

        let batch = db.batch();
        // create user data
        let userRef = db.collection(constants.USER_COLLECTION).doc("/" + userId + "/");
        let userData = {
            token: token,
            money: 0,
            statistics: {
                win: 0,
                losses: 0
            },
            friends: [],
            requests: [],
            matches: {}
        };
        // generate friend code
        let friendCode = "";
        // create friends code data
        let friendCodeFound = false;
        let friendsCodeRef = null
        while (!friendCodeFound) {
            // try to generate friend code until no collision happens
            friendCode = crypto.randomBytes(10).readUIntBE(0, 5).toString();
            // check collision on db
            friendsCodeRef = db.collection(constants.FRIENDS_CODES_COLLECTION).doc("/" + friendCode + "/");
            let existingFriendCode = (await friendsCodeRef.get().catch((err) => { reject(err); })).data();
            friendCodeFound = existingFriendCode == null;
        }
        userData["friendCode"] = friendCode;

        let friendsCodeData = {
            user: userId
        };
        // add data to batch
        batch.create(userRef, userData);
        batch.create(friendsCodeRef, friendsCodeData);
        // Commit the batch
        await batch.commit().catch((err) => { reject(err); });
        console.log("created user %s with friend code %s", userId, friendCode)
        resolve(userData);

    }).bind(this))
}

function getItem(collection, resourceId) {
    return new Promise((function (resolve, reject) {
        db.collection(collection).doc('/' + resourceId + '/').get()
            .then(function (dbRes) {
                console.log("retrieve object %o", dbRes.data());
                resolve(dbRes.data());
            })
            .catch(function (error) {
                console.log("retrieve object %o", error);
                reject(error);
            })
    }).bind(this))
}

function updateItem(collection, resourceId, updateItem) {
    return new Promise((function (resolve, reject) {
        db.collection(collection).doc('/' + resourceId + '/').update(updateItem)
            .then(function () {
                console.log("update ok");
                resolve(true);
            })
            .catch(function (error) {
                console.log("update error: %o", error);
                reject();
            })
    }).bind(this));
}

function deleteItemField(collection, resourceId, ...fields) {
    let fieldValue = admin.firestore.FieldValue;
    let deleteObject = {};
    fields.forEach((field, index, array) => {
        if (index == array.length - 1) {
            // if it is last field, it's the one to delete
            deleteObject[field] = fieldValue.delete();
        }
        else {
            // otherwise we create a nested item inside deleteObject
            deleteObject[field] = {};
        }
    })

    return new Promise((function (resolve, reject) {
        db.collection(collection).doc('/' + resourceId + '/').update(deleteObject)
            .then(function () {
                console.log("update ok");
                resolve(true);
            })
            .catch(function (error) {
                console.log("update error: %o", error);
                reject();
            })
    }).bind(this));
}

function findMatch(userId, debugMode, friendId) {
    debugMode = false;
    return new Promise((async function (resolve, reject) {
        let userData = await getItem(constants.USER_COLLECTION, userId)
            .catch(err => reject(err))
        let userFirebase = await auth.getUser(userId).catch(err => reject(err));
        // start transaction
        let batch = db.batch();
        // get lobby
        let lobbyRef = db.collection(constants.GAME_COLLECTION).doc("/" + constants.LOBBY_RESOURCE + "/");
        let lobby = (await lobbyRef.get().catch((err) => { reject(err); })).data();
        // pick random adversary
        let adversaryUser = debugMode ? "debug" : "";

        if (!debugMode) {
            if (!friendId) {
                // get other free users by not considering current user 
                let users = Object.keys(lobby).filter(user => {
                    return user != userId && !lobby[user].onMatch;
                });

                if (users.length > 0) {
                    adversaryUser = users[Math.floor(Math.random() * users.length)];
                }
            }
            else {
                adversaryUser = friendId
                lobby[userId] = {
                    debug: false,
                    randomMode: false
                };
                lobby[adversaryUser] = {
                    debug: false,
                    randomMode: false
                };
            }
        }

        if ((lobby[adversaryUser] && !lobby[adversaryUser].onMatch) || debugMode || friendId) {
            // get adversary data
            let adversaryData = await getItem(constants.USER_COLLECTION, adversaryUser)
                .catch(err => reject(err))
            let adversaryFirebase = !debugMode ? await auth.getUser(adversaryUser).catch(err => reject(err)) : { displayName: "debugPlayer" };
            // create matchId
            let userStrings = [userId, adversaryUser].sort((a, b) => a - b)
            let matchId = !debugMode ? crypto.createHmac("sha256", "password")
                .update(userStrings[0] + userStrings[1])
                .digest("hex") : "debug";
            console.log("created match %s involving players %s and %s ", matchId, userId, adversaryUser);
            // create match on realtime database
            let gameInstance = {
                "gameId": matchId,
                "gameOver": false,
                "hasStarted": false,
                "maxScore": 3,
                "player1": {
                    "label": "",
                    "position": 0,
                    "ready": false,
                    "score": 0,
                    "nickname": userFirebase.displayName,
                    "friendCode": userData!=null ? userData.friendCode : ""
                },
                "player2": {
                    "label": "",
                    "position": 0,
                    "ready": debugMode ? true : false,
                    "score": 0,
                    "nickname": adversaryFirebase.displayName,
                    "friendCode": adversaryData!=null ? adversaryData.friendCode : ""
                },
                "playerTurn": "player1",
                "ready": false,
                "isFriendGame": friendId != null
            }
            await realtimeDatabase.ref("games").child(matchId).set(gameInstance).catch((err) => { reject(err); });
            // create custom token to access game
            let gameToken = { game: matchId };
            let userToken = await auth.createCustomToken(userId, gameToken)
                .catch((err) => { reject(err); });

            lobby[userId].onMatch = {
                token: userToken,
                matchId: matchId,
                playerRole: "player1"
            };
            if (!debugMode) {
                let adversaryToken = await auth.createCustomToken(adversaryUser, gameToken)
                    .catch((err) => { reject(err); });
                lobby[adversaryUser].onMatch = {
                    token: adversaryToken,
                    matchId: matchId,
                    playerRole: "player2"
                };
            }
            batch.update(lobbyRef, lobby);
            // Commit the batch
            await batch.commit().catch((err) => { reject(err); });
            resolve(friendId != null ? [lobby[userId].onMatch, lobby[adversaryUser].onMatch] : lobby[userId].onMatch);
        }
        else {
            batch.commit()
            resolve(null);
        }



    }).bind(this))

}

function deleteGame(userId, matchId, win, draw) {

    return new Promise((async function (resolve, reject) {
        let userRef = db.collection(constants.USER_COLLECTION).doc("/" + userId + "/");
        let userData = (await userRef.get().catch((err) => { reject(err); })).data();
        // start transaction
        let batch = db.batch();
        // delete game
        //await realtimeDatabase.ref("games").child(matchId).remove();

        if (!draw) {
            if (win) {
                userData.statistics.win++
            }
            else {
                userData.statistics.losses++
            }
        }
        else{
            userData.statistics.losses++
        }

        batch.update(userRef, userData);
        // Commit the batch
        await batch.commit().catch((err) => { reject(err); });
        resolve(true);

    }).bind(this))
}

function uploadPicture(userId) {
    return new Promise((resolve, reject) => {
        if (!userId) {
            reject("news.provider#uploadPicture - Could not upload picture because at least one param is missing.");
        }

        // await user update (set default picture to storage)
        let base64 = fs.readFileSync('./res/profile_picture.png', { encoding: 'base64' }).substr(22);
        let bufferStream = new stream.PassThrough();
        bufferStream.end(Buffer.from(base64, 'base64'));

        // Create a reference to the new image file
        let file = bucket.file(`${userId}/profile_picture.png`);

        bufferStream.pipe(file.createWriteStream({
            contentType: "image/png",
            gzip: true
        }))
            .on('error', error => {
                reject(`news.provider#uploadPicture - Error while uploading picture ${JSON.stringify(error)}`);
            })
            .on('finish', () => {
                // The file upload is complete.
                console.log("news.provider#uploadPicture - Image successfully uploaded");
                resolve(true);

            });
    })
}

function getFriendData(friendId) {
    return new Promise(async function (resolve, reject) {
        let friendData = await getItem(constants.USER_COLLECTION, friendId)
            .catch(err => {
                console.log("error while retrieving friend");
                reject(err);
            })
        // add informations from firebase
        if (friendId != "debug") {

            let friendFireData = await admin.auth().getUser(friendId)
                .catch(err => {
                    console.log("error retrieving friend: ", err);
                    reject(err);
                })
            friendData["nickname"] = friendFireData.displayName
            friendData["email"] = friendFireData.email
        }
        else {
            friendData["nickname"] = "debugfriend";
            friendData["email"] = "debug@mail.com"
        }
        // update fields 
        friendData["id"] = friendId;
        delete friendData["token"];
        delete friendData["friends"];
        delete friendData["matches"];
        resolve(friendData);
    })
}

function sendToken(fcmTokens, msg) {

    let data = {
        notification: {},
        data: {
            msg: JSON.stringify(msg),
            topic: "default_channel"
        },
        tokens: fcmTokens
    };

    let titleNotification = ""
    let bodyNotification = ""
    switch(msg.type){
        case constants.REQUESTS_FRIEND:
            titleNotification = "notifications_friend_topic"
            bodyNotification = "notifications_friend_body"
            break;
        case constants.REQUESTS_GAME:
            titleNotification = "notifications_game_topic"
            bodyNotification = "notifications_game_body"
            break;
        case constants.REQUEST_ACCEPT_FRIEND, constants.REQUEST_ACCEPT_GAME:
            titleNotification = "notifications_accept_topic"
            bodyNotification = "notifications_accept_body"
            break;
    }
    let notification = {
        android: {
            notification: {
                titleLocKey: "notifications_friend_topic",
                bodyLocKey: "notifications_friend_body",
                bodyLocArgs: [msg.user.nickname]
            }
        },
        tokens: fcmTokens
    };

    return new Promise(async function (resolve, reject) {
        //Send a message to the device corresponding to the provided
        //registration token.
        let responseData = await admin.messaging().sendMulticast(data)
            .catch((error) => {
                console.log('Error sending message:', error);
                reject(error);
            });
        let responseNotification = await admin.messaging().sendMulticast(notification)
            .catch((error) => {
                console.log('Error sending message:', error);
                reject(error);
            });

        resolve({ responseData: responseData, responseNotification: responseNotification })
    });
}

/**
 * a middleware function with no mount path. This code is executed for every request to the router.
 * It checks whether the user is authenticated by firebase auth.
 */
router.use(async function (req, res, next) {
    // idToken comes from the client app
    let idToken = getToken(req.headers);
    let route = req.originalUrl.replace(req.baseUrl, "");
    if (route.includes("/user") || route.includes("/game") || route.includes("/request")) {
        let decodedToken = await admin.auth().verifyIdToken(idToken)
            .catch(function (error) {
                // Handle error
                console.log(error);
                return res.status(401).send(error); // unauthorized
            });
        if (decodedToken.uid) {
            res.locals.decodedToken = decodedToken;
            console.log("token is %o for route %s with method %s and body %o", decodedToken, route, req.method, req.body);
        }
        else {
            return res.status(401).send({ error: "unauthorized" });
        }
    }
    next();
});

router.post("/debug", async function (req, res) {
    // idToken comes from the client app
    result = await uploadPicture("prova")
        .catch(err => {
            console.log(err);
            res.status(500).send(err);
        })
    return res.status(200).send({ result: result })
});

require('./routes/user')(
    router, admin, auth, bucket, db, crypto,
    getItem, updateItem, createUser, createItem, uploadPicture, getFriendData
);

require('./routes/request')(
    router, admin, auth, db,
    getItem, updateItem, sendToken, getFriendData, findMatch
);

require('./routes/game')(
    router, admin, db, auth, realtimeDatabase, crypto,
    createItem, getItem, updateItem, deleteItemField, findMatch, deleteGame
);

// mount the router on the app
app.use('/', router);
const port = process.env.PORT || 3000;

app.listen(port, function () {
    console.log('Server running at http://127.0.0.1:' + port + '/');
});
