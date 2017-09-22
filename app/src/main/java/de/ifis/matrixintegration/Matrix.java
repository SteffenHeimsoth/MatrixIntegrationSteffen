package de.ifis.matrixintegration;


import android.app.Activity;
import android.content.Context;
import android.content.Intent;

import org.matrix.androidsdk.HomeserverConnectionConfig;
import org.matrix.androidsdk.MXDataHandler;
import org.matrix.androidsdk.MXSession;
import org.matrix.androidsdk.data.store.IMXStore;
import org.matrix.androidsdk.data.store.MXFileStore;
import org.matrix.androidsdk.db.MXLatestChatMessageCache;
import org.matrix.androidsdk.db.MXMediasCache;
import org.matrix.androidsdk.rest.model.login.Credentials;
import org.matrix.androidsdk.util.Log;

import java.util.ArrayList;
import java.util.Collection;

import javax.xml.transform.ErrorListener;

/**
 * Created by steffen on 19.09.17.
 */

public class Matrix {
    private static final String LOG_TAG ="Matrix";
    private static Matrix instance = null;

    private LoginStorage mlLoginStorage;
    private ArrayList<MXSession> mMXSessions;
    private Context mAppContext;

    public boolean mHasBeenDisconnected=false;

    protected Matrix(Context appContext){
        mAppContext = appContext.getApplicationContext();
        mlLoginStorage = new LoginStorage(mAppContext);
        mMXSessions = new ArrayList<MXSession>();
    }
    public MXSession createSession(HomeserverConnectionConfig hsConfig) {
        return createSession(mAppContext, hsConfig);
    }

    public synchronized static Matrix getInstance(Context appContext) {
        if((instance==null)&&(null!=appContext)){
            instance = new Matrix(appContext);
        }
        return instance;
    }

    public static ArrayList<MXSession> getMXSessions(Context context) {
        if ((null != context) && (null != instance)) {
            return instance.getSessions();
        } else {
            return null;
        }
    }

    public LoginStorage getLoginstorage(){
        return mlLoginStorage;
    }

    public  ArrayList<MXSession> getSessions(){
        ArrayList<MXSession> sessions = new ArrayList<MXSession>();
        synchronized (instance){
            if(null!=mMXSessions){
                sessions= new ArrayList<MXSession>(mMXSessions);
            }

        }
        return sessions;
    }
    public static MXSession getMXSession(String matrixID, Context context){
        return Matrix.getInstance(context.getApplicationContext()).getSession(matrixID);
    }

    public synchronized MXSession getDefaultSession(){
        ArrayList<MXSession> sessions = getSessions();

        if (sessions.size()>0){
            return sessions.get(0);
        }

        ArrayList<HomeserverConnectionConfig> hsConfigList = mlLoginStorage.getCredentialsList();
        if((hsConfigList==null) ||hsConfigList.size() == 0 ){
            return null;
        }
        ArrayList<String> matrixIds = new ArrayList<String>();
        sessions = new ArrayList<MXSession>();
        for(HomeserverConnectionConfig config : hsConfigList){

            if (config.getCredentials() != null && matrixIds.indexOf(config.getCredentials().userId)<0){
                MXSession session = createSession(config);
                sessions.add(session);
                matrixIds.add(config.getCredentials().userId);
            }

            mMXSessions = sessions;
        }
        return sessions.get(0);
    }
    

    public synchronized MXSession getSession(String matrixID){
        if (null!=matrixID) {
            ArrayList<MXSession> sessions;

            synchronized (this){
                sessions= getSessions();
            }
            for (MXSession session : sessions){
                Credentials credentials = session.getCredentials();

                if((null != credentials)&&(credentials.userId.equals(matrixID))){
                    return session;
                }
            }
        }
        return getDefaultSession();
    }


//    public static void setSessionErrorListener(Activity activity){
//        if ((instance!=null)&&(null!=activity)){
//            Collection<MXSession> sessions = getMXSessions(activity);
//
//            for (MXSession session : sessions){
//                if(session.isAlive()){
//                    session.setFailureCallback(new ErrorListener(session,activity));
//                }
//            }
//        }
//    }


    public MXMediasCache getMediasCache(){
        if(getSessions().size()>0){
            return getSessions().get(0).getMediasCache();
        }
        return null;
    }
    public MXLatestChatMessageCache getDefaultLatestMessageCache(){
        if(getSessions().size() > 0){
            return getSessions().get(0).getLatestChatMessageCache();
        }
        return null;
    }

    public static Boolean hasValidSessions() {
        if (null == instance) {
            Log.e(LOG_TAG, "hasValidSessions : has no instance");
            return false;
        }

        Boolean res;

        synchronized (instance) {
            res = (null != instance.mMXSessions) && (instance.mMXSessions.size() > 0);

            if (!res) {
                Log.e(LOG_TAG, "hasValidSessions : has no session");
            } else {
                for(MXSession session : instance.mMXSessions) {
                    res &= (null != session.getDataHandler());
                }

                if (!res) {
                    Log.e(LOG_TAG, "hasValidSessions : one sesssion has no valid data hanlder");
                }
            }
        }

        return res;
    }
    public synchronized void clearSession(Context context, MXSession session, Boolean clearCredentials){
        if(clearCredentials){
            mlLoginStorage.removeCredentials(session.getHomeserverConfig());
        }
        session.clear(context);
        synchronized (instance){
            mMXSessions.remove(session);
        }

    }

    public synchronized void clearSessions(Context context, Boolean clearCredentials){
        synchronized (instance){
            while (mMXSessions.size()>0){
                clearSession(context,mMXSessions.get(0), clearCredentials);
            }
        }
    }

    public synchronized void addSession(MXSession session){
        mlLoginStorage.addCredentials(session.getHomeserverConfig());
        synchronized (instance){
            mMXSessions.add(session);
        }
    }
    public MXSession createSession(Context context, HomeserverConnectionConfig hsConfig){
        IMXStore store;
        Credentials credentials = hsConfig.getCredentials();
        store = new MXFileStore(hsConfig,context);

        return new MXSession(hsConfig, new MXDataHandler(store, credentials, new MXDataHandler.InvalidTokenListener() {
            @Override
            public void onTokenCorrupted() {
//                if (null != ConsoleApplication.getCurrentActivity()) {
//                    CommonActivityUtils . logout(ConsoleApplication.getCurrentActivity());
//                }
            }
        }), mAppContext);

    }

public void reloadSessions(Activity fromActivity){
    ArrayList<MXSession> sessions = getMXSessions(fromActivity);

    for (MXSession session:sessions) {
        Matrix.getInstance(fromActivity).clearSession(fromActivity, session, false);
    }

    clearSessions(fromActivity,false);

    synchronized (instance) {
            ArrayList<HomeserverConnectionConfig> configs = mlLoginStorage.getCredentialsList();

            for (HomeserverConnectionConfig config:configs) {
                MXSession session = createSession(config);
                mMXSessions.add(session);
            }
        }
    Intent intent = new Intent(fromActivity,SplashActivity.class);
    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);

    fromActivity.startActivity(intent);
    fromActivity.finish();
}


}
