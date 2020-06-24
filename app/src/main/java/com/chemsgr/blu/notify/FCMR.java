package com.chemsgr.blu.notify;

import android.app.Activity;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.TaskStackBuilder;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.res.AssetFileDescriptor;
import android.database.Cursor;
import android.graphics.Color;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Build;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.core.app.NotificationCompat;

import android.provider.MediaStore;
import android.view.View;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

import com.chemsgr.blu.Chat;
import com.chemsgr.blu.Groups.Group;
import com.chemsgr.blu.R;
import com.chemsgr.blu.calls.IncAudioActivity;
import com.chemsgr.blu.calls.IncCallActivity;
import com.chemsgr.blu.global.AppBack;
import com.chemsgr.blu.global.Global;
import com.chemsgr.blu.lists.OnlineGetter;
import com.chemsgr.blu.lists.Usercalldata;
import me.leolin.shortcutbadger.ShortcutBadger;
import se.simbio.encryption.Encryption;

import com.stfalcon.chatkit.me.GroupIn;
import com.stfalcon.chatkit.me.UserIn;
import com.tapadoo.alerter.Alerter;
import com.tapadoo.alerter.OnHideAlertListener;
import com.tapadoo.alerter.OnShowAlertListener;

/**
 * Created by CodeSlu on 9/4/2018.
 */

public class FCMR extends FirebaseMessagingService {
    String title, message, name, ava, id, Mid, react = "", prefixR, messageReact = "", calltype, TOID;
    String[] array;
    int[] noUnread = {0};
    Encryption encryption;
    PendingIntent pIntent;
    TaskStackBuilder stackBuilder;
    boolean online = false, deleted = false;
    Context conn;
    //notifi id
    int oneTimeID;
    String notifiID[];
    //firebase
    FirebaseAuth mAuth;
    boolean add = false;
    DatabaseReference mlogs;

    @Override
    public void onNewToken(String token) {
        try {
            if (mAuth.getCurrentUser() != null) {
                Map<String, Object> map = new HashMap<>();
                map.put("tokens", token);
                DatabaseReference mToken = FirebaseDatabase.getInstance().getReference(Global.tokens);
                mToken.child(mAuth.getCurrentUser().getUid()).updateChildren(map).addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void aVoid) {

                    }
                });
            }
        } catch (NullPointerException e) {

        }
    }

    @Override
    public void onMessageReceived(RemoteMessage remoteMessage) {
        if (remoteMessage.getData() != null) {
            //get data
            Map<String, String> map = remoteMessage.getData();
            title = map.get("title");
            message = map.get("message");
            calltype = map.get("type");
            TOID = map.get("to");
            String[] array = title.split("#");
            name = array[2];
            id = array[1];
            ava = array[3];
            Mid = array[4];

            mAuth = FirebaseAuth.getInstance();
            notifiID = Mid.split("_");
            conn = this;


            if (mAuth.getCurrentUser() != null) {

                if (mAuth.getCurrentUser().getUid().equals(TOID)) {


                    //dark mode init
                    if (mAuth.getCurrentUser() != null) {
                        if (!((AppBack) getApplication()).shared().getBoolean("dark" + mAuth.getCurrentUser().getUid(), false))

                            Global.DARKSTATE = false;
                        else
                            Global.DARKSTATE = true;

                    }

                    ((AppBack) getApplication()).getBlock();
                    ((AppBack) getApplication()).getMute();


                    //decrypt
                    byte[] iv = new byte[16];
                    encryption = Encryption.getDefault(Global.keyE, Global.salt, iv);

                    if (!Global.blockList.contains(id)) {
                        if (!id.contains("groups-") && calltype == null) {
                            oneTimeID = (int) Long.parseLong(notifiID[2]);
                            try {
                                if (array.length > 6) {
                                    react = array[5];
                                    messageReact = array[6];
                                }
                            } catch (NullPointerException e) {
                                react = "";
                                messageReact = "";
                            }
                            if (mAuth.getCurrentUser() != null) {
                                message = encryption.decryptOrNull(message);
                                messageReact = encryption.decryptOrNull(messageReact);
                                if (react.equals("react")) {
                                    if (message.contains("react//like//"))
                                        prefixR = getResources().getString(R.string.likeR);
                                    else if (message.contains("react//funny//"))
                                        prefixR = getResources().getString(R.string.funnyR);
                                    else if (message.contains("react//love//"))
                                        prefixR = getResources().getString(R.string.loveR);
                                    else if (message.contains("react//sad//"))
                                        prefixR = getResources().getString(R.string.sadR);
                                    else if (message.contains("react//angry//"))
                                        prefixR = getResources().getString(R.string.angryR);
                                    try {
                                        if (messageReact.isEmpty())
                                            message = name + " " + prefixR;
                                        else
                                            message = name + " " + prefixR + " , " + messageReact;
                                    } catch (NullPointerException e) {

                                    }

                                }
                                if (!id.equals("ID")) {

                                    //check online
                                    DatabaseReference chatDelete = FirebaseDatabase.getInstance().getReference(Global.CHATS);
                                    Query query = chatDelete.child(mAuth.getCurrentUser().getUid()).child(id).child(Global.Messages).child(Mid);
                                    query.keepSynced(true);
                                    query.addListenerForSingleValueEvent(new ValueEventListener() {
                                        @Override
                                        public void onDataChange(DataSnapshot dataSnapshot) {
                                            if (dataSnapshot.exists()) {
                                                OnlineGetter onlineGetter = dataSnapshot.getValue(OnlineGetter.class);
                                                deleted = onlineGetter.isDeleted();
                                                if (!deleted) {
                                                    try {
                                                        if (Global.currentactivity != null || Global.currentfragment != null) {
                                                            online = true;
                                                            tawgeh();
                                                        } else {
                                                            online = false;
                                                            tawgeh();

                                                        }

                                                    } catch (NullPointerException e) {
                                                        online = false;
                                                        tawgeh();
                                                    }
                                                }
                                            }

                                        }

                                        @Override
                                        public void onCancelled(DatabaseError databaseError) {

                                        }
                                    });

                                } else {
                                    //clear all notifications
                                    NotificationManager notificationManager = (NotificationManager) this.getSystemService(Context.NOTIFICATION_SERVICE);
                                    try {
                                        if (notificationManager != null) {
                                            notificationManager.cancel(oneTimeID);
                                            countInverse();
                                        }
                                    } catch (NullPointerException e) {
                                        //nothing
                                    }
                                }
                            }
                        } else if (id.contains("groups-") && calltype == null) {
                            //groups
                            if (mAuth.getCurrentUser() != null) {
                                oneTimeID = (int) Long.parseLong(notifiID[2]);
                                try {
                                    if (array.length >= 6)
                                        add = array[5].equals("add");
                                    else
                                        add = false;

                                } catch (NullPointerException e) {
                                    add = false;
                                }
                                message = encryption.decryptOrNull(message);


                                if (!id.equals("groups-ID")) {
                                    //check online
                                    if (add) {
                                        try {
                                            if (Global.currentactivity != null) {
                                                online = true;
                                                tawgehG();
                                            } else {
                                                online = false;
                                                tawgehG();

                                            }

                                        } catch (NullPointerException e) {
                                            online = false;
                                            tawgehG();
                                        }

                                    } else {
                                        DatabaseReference groupdata = FirebaseDatabase.getInstance().getReference(Global.GROUPS);
                                        Query query = groupdata.child(id).child(Global.Messages).child(Mid);
                                        query.keepSynced(true);
                                        query.addListenerForSingleValueEvent(new ValueEventListener() {
                                            @Override
                                            public void onDataChange(DataSnapshot dataSnapshot) {
                                                if (dataSnapshot.exists()) {
                                                    OnlineGetter onlineGetter = dataSnapshot.getValue(OnlineGetter.class);
                                                    deleted = onlineGetter.isDeleted();
                                                    if (!deleted) {
                                                        try {
                                                            if (Global.currentactivity != null) {
                                                                online = true;
                                                                tawgehG();
                                                            } else {
                                                                online = false;
                                                                tawgehG();

                                                            }

                                                        } catch (NullPointerException e) {
                                                            online = false;
                                                            tawgehG();
                                                        }
                                                    }
                                                }

                                            }

                                            @Override
                                            public void onCancelled(DatabaseError databaseError) {

                                            }
                                        });
                                    }

                                } else {

                                    //clear all notifications
                                    NotificationManager notificationManager = (NotificationManager) this.getSystemService(Context.NOTIFICATION_SERVICE);
                                    try {
                                        if (notificationManager != null) {
                                            notificationManager.cancel(oneTimeID);
                                            countInverseG();
                                        }
                                    } catch (NullPointerException e) {
                                        //nothing
                                    }
                                }
                            }


                            //groups
                        } else {
                            try {
                                DatabaseReference mcall = FirebaseDatabase.getInstance().getReference(Global.CALLS);
                                String namec, callerid, channelid, avac;
                                String[] callD;
                                callD = encryption.decryptOrNull(message).split("#plax!!ah#&&plax#");
                                channelid = callD[0];
                                callerid = callD[1];
                                namec = callD[2];
                                avac = callD[3];

                                mlogs = FirebaseDatabase.getInstance().getReference(Global.CALLS);

                                if (!Global.mutelist.contains(id)) {

                                    mcall.child(mAuth.getCurrentUser().getUid()).child(callerid).child(channelid).addListenerForSingleValueEvent(new ValueEventListener() {
                                        @Override
                                        public void onDataChange(@NonNull DataSnapshot dataSnapshot) {


                                            try {

                                                Usercalldata usercalldata = dataSnapshot.getValue(Usercalldata.class);
                                                if (usercalldata.isIncall()) {

                                                    if (Global.IncAActivity == null && Global.IncVActivity == null) {
                                                        if (calltype.equals("video")) {
                                                            Intent jumptocall = new Intent(conn, IncCallActivity.class);
                                                            jumptocall.putExtra("name", namec);
                                                            jumptocall.putExtra("ava", avac);
                                                            jumptocall.putExtra("out", false);
                                                            jumptocall.putExtra("channel_id", channelid);
                                                            jumptocall.putExtra("id", callerid);
                                                            jumptocall.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                                                            startActivity(jumptocall);
                                                        } else {
                                                            Intent jumptocall = new Intent(conn, IncAudioActivity.class);
                                                            jumptocall.putExtra("name", namec);
                                                            jumptocall.putExtra("ava", avac);
                                                            jumptocall.putExtra("out", false);
                                                            jumptocall.putExtra("channel_id", channelid);
                                                            jumptocall.putExtra("id", callerid);
                                                            jumptocall.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                                                            startActivity(jumptocall);
                                                        }
                                                    } else {
                                                        Map<String, Object> map = new HashMap<>();
                                                        map.put("incall", false);
                                                        mlogs.child(mAuth.getCurrentUser().getUid()).child(callerid).child(channelid).updateChildren(map).addOnSuccessListener(new OnSuccessListener<Void>() {
                                                            @Override
                                                            public void onSuccess(Void aVoid) {
                                                                mlogs.child(callerid).child(mAuth.getCurrentUser().getUid()).child(channelid).updateChildren(map).addOnSuccessListener(new OnSuccessListener<Void>() {
                                                                    @Override
                                                                    public void onSuccess(Void aVoid) {
                                                                    }
                                                                });
                                                            }
                                                        });
                                                    }
                                                }
                                            } catch (NullPointerException e) {
                                                Map<String, Object> map = new HashMap<>();
                                                map.put("incall", false);
                                                mlogs.child(mAuth.getCurrentUser().getUid()).child(callerid).child(channelid).updateChildren(map).addOnSuccessListener(new OnSuccessListener<Void>() {
                                                    @Override
                                                    public void onSuccess(Void aVoid) {
                                                        mlogs.child(callerid).child(mAuth.getCurrentUser().getUid()).child(channelid).updateChildren(map).addOnSuccessListener(new OnSuccessListener<Void>() {
                                                            @Override
                                                            public void onSuccess(Void aVoid) {
                                                            }
                                                        });
                                                    }
                                                });
                                            }


                                        }

                                        @Override
                                        public void onCancelled(@NonNull DatabaseError databaseError) {

                                        }
                                    });


                                }

                            } catch (NullPointerException e) {

                            }


                        }
                    }
                }
            }
        }
    }

    public void tawgeh() {
        //go activity
        Intent intent = getBaseContext().getPackageManager().getLaunchIntentForPackage(getBaseContext().getPackageName());
        intent.putExtra("name", name);
        intent.putExtra("id", id);
        intent.putExtra("ava", ava);
        intent.putExtra("codetawgeh", 1);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        stackBuilder = TaskStackBuilder.create(this);
        stackBuilder.addNextIntentWithParentStack(intent);
        pIntent = PendingIntent.getActivity(this, 11, intent,
                PendingIntent.FLAG_UPDATE_CURRENT);
        //tawgeh
        //if chat app is not running
        if (!online) {


            //Delivered
            deliver();

            if (!Global.mutelist.contains(id)) {

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                    CustomNotAPI25(message, name, oneTimeID);
                else
                    CustomNot(message, name, oneTimeID);
                int count = 0;
                //get data
                count = ((AppBack) getApplication()).shared().getInt("numN" + mAuth.getCurrentUser().getUid(), 0);
                //increment
                count++;
                //store it again
                ((AppBack) getApplication()).editSharePrefs().putInt("numN" + mAuth.getCurrentUser().getUid(), count);
                ((AppBack) getApplication()).editSharePrefs().apply();
                ShortcutBadger.applyCount(this, count);


                //get message count
                count();
            }

        }
        //if app is running
        else {
            if (title.contains("voicecall")) {
                //insideAcall();
            } else if (title.contains("videocall")) {
                //  insideVcall();
            } else {

                //Delivered
                deliver();

                if (Global.currentpageid.equals(id)) {

                    if (!Global.mutelist.contains(id)) {
                        if (((AppBack) getApplication()).shared().getBoolean("sound", false))
                            if (react.equals("react")) {
                                if (Global.currentactivity != null) {
                                    playNotSound(Global.currentactivity);
                                    Alerter.create(Global.currentactivity)
                                            .setTitle(name)
                                            .setText(message)
                                            .setIcon(ava)
                                            .enableSwipeToDismiss()
                                            .setBackgroundColorRes(Global.DARKSTATE)
                                            .setDuration(Global.NOTIFYTIME)
                                            .show();
                                } else {
                                    playNotSound(Global.currentfragment);
                                    Alerter.create(Global.currentfragment)
                                            .setTitle(name)
                                            .setText(message)
                                            .setIcon(ava)
                                            .enableSwipeToDismiss()
                                            .setBackgroundColorRes(Global.DARKSTATE)
                                            .setDuration(Global.NOTIFYTIME)
                                            .show();
                                }
                            }
                    }

                    seenInpage();


                } else {
                    if (!Global.mutelist.contains(id)) {
                        count();
//inside notification


                        if (Global.currentactivity != null) {
                            playNotSound(Global.currentactivity);
                            Alerter.create(Global.currentactivity)
                                    .setTitle(name)
                                    .setText(message)
                                    .setIcon(ava)
                                    .enableSwipeToDismiss()
                                    .setBackgroundColorRes(Global.DARKSTATE)
                                    .setDuration(Global.NOTIFYTIME)
                                    .setOnClickListener(new View.OnClickListener() {
                                        @Override
                                        public void onClick(View view) {

                                            if (Global.currentactivity == Global.chatactivity)
                                                Global.currentactivity.finish();


                                            Global.currname = name;
                                            Global.currentpageid = id;
                                            Global.currFid = id;
                                            Global.currAva = ava;
                                            Alerter.hide();
                                            Intent intent = new Intent(Global.currentactivity, Chat.class);
                                            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                                            intent.putExtra("name", name);
                                            intent.putExtra("id", id);
                                            intent.putExtra("ava", ava);
                                            startActivity(intent);
                                        }
                                    })
                                    .show();
                        } else {
                            playNotSound(Global.currentfragment);
                            Alerter.create(Global.currentfragment)
                                    .setTitle(name)
                                    .setText(message)
                                    .setIcon(ava)
                                    .enableSwipeToDismiss()
                                    .setBackgroundColorRes(Global.DARKSTATE)
                                    .setDuration(Global.NOTIFYTIME)
                                    .setOnClickListener(new View.OnClickListener() {
                                        @Override
                                        public void onClick(View view) {

                                            if ((Global.currentactivity == Global.chatactivity) && Global.currentactivity != null)
                                                Global.currentactivity.finish();


                                            Global.currname = name;
                                            Global.currentpageid = id;
                                            Global.currFid = id;
                                            Global.currAva = ava;
                                            Alerter.hide();
                                            Intent intent = new Intent(Global.currentfragment, Chat.class);
                                            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                                            intent.putExtra("name", name);
                                            intent.putExtra("id", id);
                                            intent.putExtra("ava", ava);
                                            startActivity(intent);
                                        }
                                    })
                                    .show();
                        }
                    }
                }
            }

        }
    }

    public void tawgehG() {
        //go activity
        Intent intent = getBaseContext().getPackageManager().getLaunchIntentForPackage(getBaseContext().getPackageName());
        intent.putExtra("name", name);
        intent.putExtra("id", id);
        intent.putExtra("ava", ava);
        intent.putExtra("codetawgeh", 2);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        stackBuilder = TaskStackBuilder.create(this);
        stackBuilder.addNextIntentWithParentStack(intent);
        pIntent = PendingIntent.getActivity(this, 22, intent,
                PendingIntent.FLAG_UPDATE_CURRENT);
        //tawgeh
        //if chat app is not running
        if (!online) {
            if (!Global.mutelist.contains(id)) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                    CustomNotAPI25(message, name, oneTimeID);
                else
                    CustomNot(message, name, oneTimeID);
                int count = 0;
                //get data
                count = ((AppBack) getApplication()).shared().getInt("numN" + mAuth.getCurrentUser().getUid(), 0);
                //increment
                count++;
                //store it again
                ((AppBack) getApplication()).editSharePrefs().putInt("numN" + mAuth.getCurrentUser().getUid(), count);
                ((AppBack) getApplication()).editSharePrefs().apply();
                ShortcutBadger.applyCount(this, count);

                //get message count
                countG();
            }
        }
        //if app is running
        else {
            if (Global.currentpageid.equals(id)) {

                if (!Global.mutelist.contains(id)) {
                    if (((AppBack) getApplication()).shared().getBoolean("sound", false)) {
                        if (Global.currentactivity != null)
                            playNotSound(Global.currentactivity);
                        else
                            playNotSound(Global.currentfragment);


                    }
                }


            } else {
                if (!Global.mutelist.contains(id)) {
                    countG();
//inside notification
                    if (Global.currentactivity != null) {
                        playNotSound(Global.currentactivity);
                        Alerter.create(Global.currentactivity)
                                .setTitle(name)
                                .setText(message)
                                .enableSwipeToDismiss()
                                .setBackgroundColorRes(Global.DARKSTATE)
                                .setDuration(Global.NOTIFYTIME)
                                .setOnClickListener(new View.OnClickListener() {
                                    @Override
                                    public void onClick(View view) {

                                        if (Global.currentactivity == Global.chatactivity)
                                            Global.currentactivity.finish();


                                        Global.currname = name;
                                        Global.currentpageid = id;
                                        Global.currFid = id;
                                        Global.currAva = ava;
                                        Alerter.hide();
                                        Intent intent = new Intent(Global.currentactivity, Group.class);
                                        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                                        intent.putExtra("name", name);
                                        intent.putExtra("id", id);
                                        intent.putExtra("ava", ava);
                                        startActivity(intent);
                                    }
                                })
                                .show();

                    } else {
                        playNotSound(Global.currentfragment);
                        Alerter.create(Global.currentfragment)
                                .setTitle(name)
                                .setText(message)
                                .enableSwipeToDismiss()
                                .setBackgroundColorRes(Global.DARKSTATE)
                                .setDuration(Global.NOTIFYTIME)
                                .setOnClickListener(new View.OnClickListener() {
                                    @Override
                                    public void onClick(View view) {

                                        if (Global.currentactivity == Global.chatactivity)
                                            Global.currentactivity.finish();


                                        Global.currname = name;
                                        Global.currentpageid = id;
                                        Global.currFid = id;
                                        Global.currAva = ava;
                                        Alerter.hide();
                                        Intent intent = new Intent(Global.currentfragment, Group.class);
                                        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                                        intent.putExtra("name", name);
                                        intent.putExtra("id", id);
                                        intent.putExtra("ava", ava);
                                        startActivity(intent);
                                    }
                                })
                                .show();
                    }
                }
            }


        }
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    private void CustomNotAPI25(String body, String string, int i) {
        int color = ((AppBack) getApplication()).shared().getInt("colorN", Color.BLUE);
        Uri sound = Uri.parse(((AppBack) getApplication()).shared().getString("ringU", "no"));
        NotificationChann notificationChann = new NotificationChann(getBaseContext(), color, sound);
        Notification.Builder builder = notificationChann.getPLAXNot(string, body, pIntent, sound);
        notificationChann.getManager().notify(i, builder.build());
    }

    public void CustomNot(String body, String title, int id) {
        int color = ((AppBack) getApplication()).shared().getInt("colorN", Color.BLUE);
        Uri sound = Uri.parse(((AppBack) getApplication()).shared().getString("ringU", "no"));
        NotificationCompat.Builder builder = new NotificationCompat.Builder(getApplicationContext());
        builder.setContentTitle(title)
                .setContentText(body)
                .setSound(sound)
                .setWhen(System.currentTimeMillis())
                .setSmallIcon(R.drawable.logo)
                .setOngoing(false)
                .setLights(color, 1000, 1000)
                .setContentIntent(pIntent);
        NotificationManager manager = (NotificationManager) getBaseContext().getSystemService(Context.NOTIFICATION_SERVICE);
        manager.notify(id, builder.build());
    }

    public void seenInpage() {
        final FirebaseAuth mAuth = FirebaseAuth.getInstance();
        final DatabaseReference mData = FirebaseDatabase.getInstance().getReference(Global.CHATS);
        mData.child(id).child(mAuth.getCurrentUser().getUid()).child(Global.Messages).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                for (DataSnapshot ds : dataSnapshot.getChildren()) {
                    ds.child("statue").getRef().setValue("seen ✔✔");
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });
    }

    public void deliver() {
        final FirebaseAuth mAuth = FirebaseAuth.getInstance();
        final DatabaseReference mData = FirebaseDatabase.getInstance().getReference(Global.CHATS);
        mData.child(id).child(mAuth.getCurrentUser().getUid()).child(Global.Messages).child(Mid).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists()) {
                    Map<String, Object> map = new HashMap<String, Object>();
                    map.put("statue", "delivered ✔");
                    mData.child(id).child(mAuth.getCurrentUser().getUid()).child(Global.Messages).child(Mid).updateChildren(map).addOnCompleteListener(new OnCompleteListener<Void>() {
                        @Override
                        public void onComplete(@NonNull Task<Void> task) {

                        }
                    });
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });
    }

    public void count() {
        final FirebaseAuth mAuth = FirebaseAuth.getInstance();
        final DatabaseReference mData = FirebaseDatabase.getInstance().getReference(Global.CHATS);
        final DatabaseReference mData2 = FirebaseDatabase.getInstance().getReference(Global.CHATS);
        mData.child(mAuth.getCurrentUser().getUid()).child(id).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                try {
                    UserIn data = dataSnapshot.getValue(UserIn.class);
                    noUnread[0] = data.getNoOfUnread();
                    //message count
                    noUnread[0] = noUnread[0] + 1;
                    Map<String, Object> map2 = new HashMap<>();
                    map2.put("noOfUnread", noUnread[0]);
                    mData2.child(mAuth.getCurrentUser().getUid()).child(id).updateChildren(map2).addOnSuccessListener(new OnSuccessListener<Void>() {
                        @Override
                        public void onSuccess(Void aVoid) {

                        }
                    });
                }catch (NullPointerException e)
                {

                }


            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });

    }

    public void countG() {
        final FirebaseAuth mAuth = FirebaseAuth.getInstance();
        final DatabaseReference mData = FirebaseDatabase.getInstance().getReference(Global.USERS);
        final DatabaseReference mData2 = FirebaseDatabase.getInstance().getReference(Global.USERS);
        mData.child(mAuth.getCurrentUser().getUid()).child(Global.GROUPS).child(id).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                try {
                    GroupIn data = dataSnapshot.getValue(GroupIn.class);
                    noUnread[0] = data.getNoOfUnread();
                    //message count
                    noUnread[0] = noUnread[0] + 1;
                    Map<String, Object> map2 = new HashMap<>();
                    map2.put("noOfUnread", noUnread[0]);
                    mData2.child(mAuth.getCurrentUser().getUid()).child(Global.GROUPS).child(id).updateChildren(map2).addOnSuccessListener(new OnSuccessListener<Void>() {
                        @Override
                        public void onSuccess(Void aVoid) {

                        }
                    });
                } catch (NullPointerException e) {

                }


            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });

    }

    public void countInverse() {
        int count = 0;
        //get data
        count = ((AppBack) getApplication()).shared().getInt("numN" + mAuth.getCurrentUser().getUid(), 0);
        //increment
        count = count - 1;
        //store it again
        ((AppBack) getApplication()).editSharePrefs().putInt("numN" + mAuth.getCurrentUser().getUid(), count);
        ((AppBack) getApplication()).editSharePrefs().apply();
        ShortcutBadger.applyCount(this, count);

        id = notifiID[0];
        final FirebaseAuth mAuth = FirebaseAuth.getInstance();
        final DatabaseReference mData = FirebaseDatabase.getInstance().getReference(Global.CHATS);
        final DatabaseReference mData2 = FirebaseDatabase.getInstance().getReference(Global.CHATS);
        mData.child(mAuth.getCurrentUser().getUid()).child(id).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                UserIn data = dataSnapshot.getValue(UserIn.class);
                noUnread[0] = data.getNoOfUnread();
                //message count
                if (noUnread[0] > 0)
                    noUnread[0] = noUnread[0] - 1;
                else
                    noUnread[0] = 0;


                Map<String, Object> map2 = new HashMap<>();
                map2.put("noOfUnread", noUnread[0]);
                mData2.child(mAuth.getCurrentUser().getUid()).child(id).updateChildren(map2).addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void aVoid) {

                    }
                });

            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });

    }

    public void countInverseG() {
        int count = 0;
        //get data
        count = ((AppBack) getApplication()).shared().getInt("numN" + mAuth.getCurrentUser().getUid(), 0);
        //increment
        count = count - 1;
        //store it again
        ((AppBack) getApplication()).editSharePrefs().putInt("numN" + mAuth.getCurrentUser().getUid(), count);
        ((AppBack) getApplication()).editSharePrefs().apply();
        ShortcutBadger.applyCount(this, count);

        final FirebaseAuth mAuth = FirebaseAuth.getInstance();
        final DatabaseReference mData = FirebaseDatabase.getInstance().getReference(Global.USERS);
        final DatabaseReference mData2 = FirebaseDatabase.getInstance().getReference(Global.USERS);
        mData.child(mAuth.getCurrentUser().getUid()).child(Global.GROUPS).child(name).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                try {
                    UserIn data = dataSnapshot.getValue(UserIn.class);
                    noUnread[0] = data.getNoOfUnread();
                    //message count
                    if (noUnread[0] > 0)
                        noUnread[0] = noUnread[0] - 1;
                    else
                        noUnread[0] = 0;


                    Map<String, Object> map2 = new HashMap<>();
                    map2.put("noOfUnread", noUnread[0]);
                    mData2.child(mAuth.getCurrentUser().getUid()).child(Global.GROUPS).child(name).updateChildren(map2).addOnSuccessListener(new OnSuccessListener<Void>() {
                        @Override
                        public void onSuccess(Void aVoid) {

                        }
                    });
                } catch (NullPointerException e) {
                }


            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });

    }

    public void insideAcall() {
        Alerter.create(Global.currentactivity)
                .setTitle(name)
                .setText(message)
                .setIcon(ava)
                .setBackgroundColorRes(Global.DARKSTATE)
                .setDuration(2500)
                .addButton("Accept", R.style.AlertButton, new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {

                    }
                })
                .addButton("Cancel", R.style.AlertButton, new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {

                    }
                })
                .setOnShowListener(new OnShowAlertListener() {
                    @Override
                    public void onShow() {

                    }
                })
                .setOnHideListener(new OnHideAlertListener() {
                    @Override
                    public void onHide() {

                    }
                })
                .setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        Alerter.hide();
                        Intent intent = new Intent(Global.currentactivity, Chat.class);
                        intent.putExtra("name", name);
                        intent.putExtra("id", id);
                        intent.putExtra("ava", ava);
                        startActivity(intent);
                    }
                })
                .show();
    }

    public void insideVcall() {
        Alerter.create(Global.currentactivity)
                .setTitle(name)
                .setIcon(ava)
                .setText(message)
                .setBackgroundColorRes(Global.DARKSTATE)
                .setDuration(2500)
                .addButton("Accept", R.style.AlertButton, new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {

                    }
                })
                .addButton("Cancel", R.style.AlertButton, new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {

                    }
                })
                .setOnShowListener(new OnShowAlertListener() {
                    @Override
                    public void onShow() {

                    }
                })
                .setOnHideListener(new OnHideAlertListener() {
                    @Override
                    public void onHide() {

                    }
                })
                .setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        Alerter.hide();
                        Intent intent = new Intent(Global.currentactivity, Chat.class);
                        intent.putExtra("name", name);
                        intent.putExtra("id", id);
                        intent.putExtra("ava", ava);
                        startActivity(intent);
                    }
                })
                .show();
    }

    public void playNotSound(Activity acc) {
        try {
            AudioManager audioManager = (AudioManager) acc.getApplicationContext().getSystemService(Context.AUDIO_SERVICE);


            switch (audioManager.getRingerMode()) {
                case AudioManager.RINGER_MODE_NORMAL:
                    MediaPlayer mediaPlayer = new MediaPlayer();

                    AssetFileDescriptor descriptor = Global.currentactivity.getAssets().openFd("notsound.mp3");
                    mediaPlayer.setDataSource(descriptor.getFileDescriptor(), descriptor.getStartOffset(), descriptor.getLength());
                    descriptor.close();

                    mediaPlayer.prepare();
                    mediaPlayer.setVolume(1f, 1f);
                    mediaPlayer.setLooping(false);
                    mediaPlayer.start();
                    break;
            }


        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public Uri setMyNotification(String path) {
        File sdfile = new File(path);
        ContentValues values = new ContentValues();
        values.put(MediaStore.MediaColumns.DATA, sdfile.getAbsolutePath());
        values.put(MediaStore.MediaColumns.TITLE, "38");
        values.put(MediaStore.MediaColumns.MIME_TYPE, "audio/*");
        values.put(MediaStore.Audio.Media.IS_RINGTONE, false);
        values.put(MediaStore.Audio.Media.IS_NOTIFICATION, true);
        values.put(MediaStore.Audio.Media.IS_ALARM, false);
        values.put(MediaStore.Audio.Media.IS_MUSIC, false);

        Uri uri = MediaStore.Audio.Media.getContentUriForPath(sdfile.getAbsolutePath());
        Uri newUri;
        ContentValues cv = new ContentValues();
        Cursor cursor = this.getContentResolver().query(uri, null, MediaStore.MediaColumns.DATA + "=?", new String[]{path}, null);
        if (cursor.moveToNext() && cursor.getCount() > 0) {
            String _id = cursor.getString(0);
            cv.put(MediaStore.MediaColumns.DATA, sdfile.getAbsolutePath());
            cv.put(MediaStore.MediaColumns.TITLE, "38");
            cv.put(MediaStore.MediaColumns.MIME_TYPE, "audio/*");
            cv.put(MediaStore.Audio.Media.IS_NOTIFICATION, true);
            cv.put(MediaStore.Audio.Media.IS_RINGTONE, true);
            cv.put(MediaStore.Audio.Media.IS_ALARM, false);
            cv.put(MediaStore.Audio.Media.IS_MUSIC, false);
            getContentResolver().update(uri, cv, MediaStore.Audio.Media.DATA + "=?", new String[]{path});
            newUri = ContentUris.withAppendedId(uri, Long.valueOf(_id));
        } else {
            newUri = getApplicationContext().getContentResolver().insert(uri, values);
        }
        cursor.close();

        //  RingtoneManager.setActualDefaultRingtoneUri(getApplicationContext(), RingtoneManager.TYPE_NOTIFICATION, newUri);
        return newUri;

    }
}
