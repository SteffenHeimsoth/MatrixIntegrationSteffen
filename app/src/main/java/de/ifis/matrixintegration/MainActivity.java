package de.ifis.matrixintegration;


import android.net.Uri;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.widget.Toast;

import org.matrix.androidsdk.HomeserverConnectionConfig;
import org.matrix.androidsdk.MXDataHandler;
import org.matrix.androidsdk.MXSession;
import org.matrix.androidsdk.data.Room;
import org.matrix.androidsdk.data.store.MXMemoryStore;
import org.matrix.androidsdk.rest.callback.ApiCallback;

import org.matrix.androidsdk.rest.client.LoginRestClient;
import org.matrix.androidsdk.rest.model.Event;
import org.matrix.androidsdk.rest.model.MatrixError;
import org.matrix.androidsdk.rest.model.Message;
import org.matrix.androidsdk.rest.model.login.Credentials;

public class MainActivity  extends AppCompatActivity {

    public static final String LOG_TAG = "MainActivity";

    MXDataHandler dataHandler;
    MXSession mxSession;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        final HomeserverConnectionConfig hsConfig = new HomeserverConnectionConfig(Uri.parse("https://demo.quvert.if-is.net"));
        new LoginRestClient(hsConfig).loginWithUser("sheimsoth", "matrix", new ApiCallback<Credentials>() {
            @Override
            public void onSuccess(Credentials credentials) {
                hsConfig.setCredentials(credentials);
                Log.i("login"," Success");
                createSession(hsConfig);
            }

            @Override
            public void onNetworkError(Exception e) {
                Log.e("login"," Network error");
            }

            @Override
            public void onMatrixError(MatrixError matrixError) {
                Log.e("login"," Matrix error");
            }

            @Override
            public void onUnexpectedError(Exception e) {
                Log.e("login"," Unexpected error");
            }
        });



    }
    private void createSession(HomeserverConnectionConfig hsConfig){


        dataHandler = new MXDataHandler(new MXMemoryStore(hsConfig.getCredentials(),this), hsConfig.getCredentials(), new MXDataHandler.InvalidTokenListener() {
            @Override
            public void onTokenCorrupted() {Log.e("dataHandler"," Token corrupted");}
        });


        mxSession = new MXSession(hsConfig,dataHandler, this);

        mxSession.startEventStream(null);

        mxSession.createRoom("Steffen","Bewerbung","Heimsoth", new ApiCallback<String>() {
            @Override
            public void onSuccess(String s) {
                sendMessageToRoom(mxSession.getDataHandler().getRoom(s),mxSession,"Test");
            }

            @Override
            public void onNetworkError(Exception e) {
                Log.e("Room"," Network error");
            }

            @Override
            public void onMatrixError(MatrixError matrixError) {
                Log.e("Room"," Matrix error");
            }

            @Override
            public void onUnexpectedError(Exception e) {
                Log.e("Room"," Unexpected error");
            }
        });

//        mxSession.createRoom(new ApiCallback<String>() {
//            @Override
//            public void onSuccess(String s) {
//                sendMessageToRoom(mxSession.getDataHandler().getRoom(s),mxSession,"Test");
//            }
//
//            @Override
//            public void onNetworkError(Exception e) {
//                Log.e("Room"," Network error");
//            }
//
//            @Override
//            public void onMatrixError(MatrixError matrixError) {
//                Log.e("Room"," Matrix error");
//            }
//
//            @Override
//            public void onUnexpectedError(Exception e) {
//                Log.e("Room"," Unexpected error");
//            }
//        });
    }
    /**
     * Sends a message to the provided room.
     *
     * @param room      the room the message is sent to
     * @param session   the matrix session
     * @param message   the message which will be sent to the room
     */



    private void sendMessageToRoom(Room room, MXSession session, String message) {

        Event event = buildTextEvent(message, session, room.getRoomId());
        room.sendEvent(event, new ApiCallback<Void>() {

            @Override
            public void onSuccess(Void aVoid) {
                Log.d(LOG_TAG, "onSuccess()");
            }

            @Override
            public void onNetworkError(Exception e) {
                onError(e.getLocalizedMessage());
            }

            @Override
            public void onMatrixError(MatrixError matrixError) {
                onError(matrixError.getLocalizedMessage());
            }

            @Override
            public void onUnexpectedError(Exception e) {
                onError(e.getLocalizedMessage());
            }

            private void onError(String msg) {
                Toast.makeText(getApplicationContext(), msg, Toast.LENGTH_SHORT).show();
            }
        });
    }

    /**
     * Creates an text {@link Event} for the provided room id.
     *
     * @param text      the text message for this event
     * @param session   the matrix session
     * @param mRoomId   the id of the room the message is sent to
     *
     * @return An event which contains the text message
     */
    private Event buildTextEvent(String text, MXSession session, String mRoomId) {

        Message message = new Message();
        message.msgtype = Message.MSGTYPE_TEXT;
        message.body = text;

        return new Event(message, session.getCredentials().userId, mRoomId);
    }


}
