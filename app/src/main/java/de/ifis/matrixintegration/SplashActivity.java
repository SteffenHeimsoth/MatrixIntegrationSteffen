package de.ifis.matrixintegration;

import android.content.Intent;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;

import org.matrix.androidsdk.MXSession;
import org.matrix.androidsdk.listeners.IMXEventListener;
import org.matrix.androidsdk.listeners.MXEventListener;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;

import javax.xml.transform.ErrorListener;

public class SplashActivity extends AppCompatActivity {




    private static final String LOG_TAG = "SplashActivity";

    private Collection<MXSession> mSessions;
    private boolean mInitialSyncComplete = false;
    private boolean mPusherRegistrationComplete = false;

    private HashMap<MXSession, IMXEventListener> mListeners;
    private HashMap<MXSession, IMXEventListener> mDoneListeners;

    private boolean hasCorruptedStore() {
        boolean hasCorruptedStore = false;
        ArrayList<MXSession> sessions = Matrix.getMXSessions(this);

        for(MXSession session : sessions) {
            if (session.isAlive()) {
                hasCorruptedStore |= session.getDataHandler().getStore().isCorrupted();
            }
        }
        return hasCorruptedStore;
    }
    private void finishIfReady() {
        Log.e(LOG_TAG, "finishIfReady " + mInitialSyncComplete + " " + mPusherRegistrationComplete);

        if (mInitialSyncComplete ) {
            Log.e(LOG_TAG, "finishIfRead start HomeActivity");

            if (!hasCorruptedStore()) {
                // Go to the home page
                startActivity(new Intent(SplashActivity.this, HomeActivity.class));
                SplashActivity.this.finish();
            } else {

                // go to login page
                this.startActivity(new Intent(this, LoginActivity.class));
                SplashActivity.this.finish();
            }
        }
    }



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.e(LOG_TAG, "onCreate");
        setContentView(R.layout.activity_splash);

        mSessions =  Matrix.getInstance(getApplicationContext()).getSessions();

        if (mSessions == null) {
            Log.e(LOG_TAG, "onCreate no Sessions");
            finish();
            return;
        }

        mListeners = new HashMap<MXSession, IMXEventListener>();
        mDoneListeners = new HashMap<MXSession, IMXEventListener>();
        ArrayList<String> matrixIds = new ArrayList<String>();

        for(MXSession session : mSessions) {

            final MXSession fSession = session;
            session.getDataHandler().getStore().open();

            final IMXEventListener eventListener = new MXEventListener() {
                @Override
                public void onInitialSyncComplete(String toToken) {
                    super.onInitialSyncComplete( toToken);
                    Boolean noMoreListener;

                    Log.e(LOG_TAG, "Session " + fSession.getCredentials().userId + " is initialized");

                    synchronized(mListeners) {
                        mDoneListeners.put(fSession, mListeners.get(fSession));


                        mListeners.remove(fSession);
                        noMoreListener = mInitialSyncComplete = (mListeners.size() == 0);
                    }

                   // Analytics.sendEvent("Account", "Loading", fSession.getDataHandler().getStore().getRooms().size() + " rooms", System.currentTimeMillis() - startTime);

                    if (noMoreListener) {
                        finishIfReady();
                    }
                }
            };

            if (!fSession.getDataHandler().isInitialSyncComplete()) {
                mListeners.put(fSession, eventListener);
                fSession.getDataHandler().addListener(eventListener);


                matrixIds.add(session.getCredentials().userId);
            //**
            //Not sure if correct approach
            //**
                fSession.startEventStream(null);
            }
        }
        // when the events stream has been disconnected by the user
        // they must be awoken even if they are initialized
        if (Matrix.getInstance(this).mHasBeenDisconnected) {
            matrixIds = new ArrayList<String>();

            for(MXSession session : mSessions) {
                matrixIds.add(session.getCredentials().userId);
            }

            Matrix.getInstance(this).mHasBeenDisconnected = false;
        }


    }
}
