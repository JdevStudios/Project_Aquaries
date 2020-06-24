package com.chemsgr.blu.custom;

import android.app.Activity;
import android.app.Dialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.media.MediaMetadataRetriever;
import android.media.ThumbnailUtils;
import android.net.Uri;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;

import android.provider.MediaStore;
import android.view.View;
import android.view.Window;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Toast;


import com.google.android.gms.tasks.Continuation;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ServerValue;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.iid.FirebaseInstanceId;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;
import com.squareup.picasso.Picasso;
import com.stfalcon.chatkit.me.Message;
import com.stfalcon.chatkit.me.MessageFav;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import com.chemsgr.blu.Chat;
import com.chemsgr.blu.Forward;
import com.chemsgr.blu.Groups.Group;
import com.chemsgr.blu.R;
import com.chemsgr.blu.global.AppBack;
import com.chemsgr.blu.global.Global;
import com.chemsgr.blu.lists.Tokens;
import com.chemsgr.blu.notify.FCM;
import com.chemsgr.blu.notify.FCMresp;
import com.chemsgr.blu.notify.Sender;
import de.hdodenhof.circleimageview.CircleImageView;
import id.zelory.compressor.Compressor;
import io.reactivex.disposables.Disposable;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import se.simbio.encryption.Encryption;

/**
 * Created by CodeSlu on 05/03/19.
 */


public class MessageSelectD extends Dialog {

    private Activity c;
    public Dialog d;
    private ImageView copy, delete, deleteall, retry, fav, forward, reply;
    CircleImageView avatar;
    // private boolean deleted;
    private String messId, friendid, messagetxt, type;
    public long time;
    Message message;
    private int position;
    private String lastM = "Message Deleted";
    private Encryption encryption;
    //0 for me 1 for others
    private int indicat;
    private boolean deletedChat;
    //delete chat
    boolean deleted = false;
    FirebaseAuth mAuth;
    DatabaseReference mData, userD, mTime, mFav, mMess, mGroup;
    Map<String, Object> map;
    Map<String, Object> mapd;
    LinearLayout dialogM;
    boolean favB = false;
    FCM fcm;

    private Disposable disposable;
    //indicate 1 other // 0 me
    public MessageSelectD(Activity a, Message message, String friendid, int indicat, int position) {
        super(a);
        // TODO Auto-generated constructor stub
        this.c = a;
        this.friendid = friendid;
        this.message = message;
        this.indicat = indicat;
        this.position = position;
    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.select_dialog);
        copy = findViewById(R.id.copy);
        avatar = findViewById(R.id.avatarSet);
        delete = findViewById(R.id.delete);
        retry = findViewById(R.id.retry);
        fav = findViewById(R.id.fav);
        forward = findViewById(R.id.forward);
        reply = findViewById(R.id.reply);
        deleteall = findViewById(R.id.deleteall);
        dialogM = findViewById(R.id.dialogM);
        if (Global.DARKSTATE)
            dialogM.setBackground(ContextCompat.getDrawable(Global.conA, R.drawable.react_bg_d));
        else
            dialogM.setBackground(ContextCompat.getDrawable(Global.conA, R.drawable.react_bg));

        fcm = Global.getFCMservies();
        mAuth = FirebaseAuth.getInstance();
        mMess = FirebaseDatabase.getInstance().getReference(Global.CHATS);
        mFav = FirebaseDatabase.getInstance().getReference(Global.FAV);
        mGroup = FirebaseDatabase.getInstance().getReference(Global.GROUPS);
        userD = FirebaseDatabase.getInstance().getReference(Global.USERS);

//set data
        messId = message.getMessid();
        messagetxt = message.getText();
        type = message.getType();
        time = message.getCreatedAt().getTime();
        deleted = message.isDeleted();

        retry.setVisibility(View.GONE);


        try {
            if (Global.avaLocal.isEmpty() || Global.avaLocal.contains("no") || Global.avaLocal == null) {
                Picasso.get()
                        .load(R.drawable.profile)
                        .placeholder(R.drawable.placeholder_gray).error(R.drawable.errorimg)
                        .into(avatar);
            } else {
                Picasso.get()
                        .load(Global.avaLocal)
                        .placeholder(R.drawable.placeholder_gray).error(R.drawable.errorimg)
                        .into(avatar);
            }
        } catch (NullPointerException e) {
            Picasso.get()
                    .load(R.drawable.profile)
                    .placeholder(R.drawable.placeholder_gray).error(R.drawable.errorimg)

                    .into(avatar);
        }


        //encryption
        byte[] iv = new byte[16];
        encryption = Encryption.getDefault(Global.keyE, Global.salt, iv);
        if (!type.equals("text") || deleted)
            copy.setVisibility(View.GONE);


        if (message.getStatus().equals("..") || !message.isChat()) {
            delete.setVisibility(View.GONE);
            retry.setVisibility(View.GONE);

        } else if (message.getStatus().equals("X")) {
            retry.setVisibility(View.VISIBLE);
            delete.setVisibility(View.VISIBLE);
        } else {
            retry.setVisibility(View.GONE);

        }


        if (message.isDeleted() || message.getStatus().equals("..") || message.getStatus().equals("X") || !Global.check_int(Global.conA) || message.getType().equals("video") || message.getType().equals("voice"))
            fav.setVisibility(View.GONE);
        else
            fav.setVisibility(View.VISIBLE);


        if (message.isDeleted() || message.getStatus().equals("..") || message.getStatus().equals("X")) {
            reply.setVisibility(View.INVISIBLE);
            forward.setVisibility(View.INVISIBLE);
        } else {
            reply.setVisibility(View.VISIBLE);
            forward.setVisibility(View.VISIBLE);
        }

        if (!Global.check_int(Global.conA)) {
            delete.setVisibility(View.GONE);
            deleteall.setVisibility(View.GONE);
            retry.setVisibility(View.GONE);

        } else {
            deleteall.setVisibility(View.GONE);
            mTime = FirebaseDatabase.getInstance().getReference(Global.TIME);
            Map<String, Object> map = new HashMap<>();
            map.put("time", ServerValue.TIMESTAMP);
            mTime.updateChildren(map).addOnSuccessListener(new OnSuccessListener<Void>() {
                @Override
                public void onSuccess(Void aVoid) {
                    mTime.child("time").addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                            long now = dataSnapshot.getValue(Long.class);
                            int hours = (int) TimeUnit.MILLISECONDS.toHours(now - time);
                            if (hours >= 24 || indicat != 0 || message.getStatus().equals("..") || message.getStatus().equals("X") || deleted)
                                deleteall.setVisibility(View.GONE);
                            else
                                deleteall.setVisibility(View.VISIBLE);
                        }

                        @Override
                        public void onCancelled(@NonNull DatabaseError databaseError) {
                            Toast.makeText(c, c.getResources().getString(R.string.timeerror) + " " + c.getResources().getString(R.string.check_int), Toast.LENGTH_SHORT).show();

                        }
                    });
                }
            }).addOnFailureListener(new OnFailureListener() {
                @Override
                public void onFailure(@NonNull Exception e) {
                    Toast.makeText(c, c.getResources().getString(R.string.timeerror) + " " + c.getResources().getString(R.string.check_int), Toast.LENGTH_SHORT).show();
                }
            });


        }

        try {
            if (messId != null) {
                ((AppBack) c.getApplication()).getFav();
                if (halbine(Global.FavMess, messId) == -1) {
                    fav.setImageResource(R.drawable.ic_notstar);
                    favB = false;
                } else {
                    fav.setImageResource(R.drawable.ic_star);
                    favB = true;
                }
            } else
                fav.setVisibility(View.GONE);
        } catch (NullPointerException e) {

        }

        fav.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                dismiss();

                if (favB) {

                    mFav.child(mAuth.getCurrentUser().getUid()).child(messId).removeValue().addOnSuccessListener(new OnSuccessListener<Void>() {
                        @Override
                        public void onSuccess(Void aVoid) {
                            ((AppBack) c.getApplication()).getFav();
                            if (halbine(Global.FavMess, messId) != -1)
                                Global.FavMess.remove(halbine(Global.FavMess, messId));

                            ((AppBack) c.getApplication()).setFav();

                        }
                    });

                } else {
                    mMess.child(mAuth.getCurrentUser().getUid()).child(friendid).child(Global.Messages).child(messId).addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                            mFav.child(mAuth.getCurrentUser().getUid()).child(messId).setValue(dataSnapshot.getValue()).addOnSuccessListener(new OnSuccessListener<Void>() {
                                @Override
                                public void onSuccess(Void aVoid) {
                                    Map<String, Object> map = new HashMap<>();
                                    map.put("favtime", ServerValue.TIMESTAMP);
                                    mFav.child(mAuth.getCurrentUser().getUid()).child(messId).updateChildren(map).addOnSuccessListener(new OnSuccessListener<Void>() {
                                        @Override
                                        public void onSuccess(Void aVoid) {
                                            mFav.child(mAuth.getCurrentUser().getUid()).child(messId).addListenerForSingleValueEvent(new ValueEventListener() {
                                                @Override
                                                public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                                                    MessageFav message = dataSnapshot.getValue(MessageFav.class);
                                                    ((AppBack) c.getApplication()).getFav();
                                                    Global.FavMess.add(message);
                                                    ((AppBack) c.getApplication()).setFav();
                                                }

                                                @Override
                                                public void onCancelled(@NonNull DatabaseError databaseError) {
                                                    Toast.makeText(c, c.getString(R.string.error), Toast.LENGTH_SHORT).show();
                                                }
                                            });


                                        }
                                    })
                                            .addOnFailureListener(new OnFailureListener() {
                                                @Override
                                                public void onFailure(@NonNull Exception e) {
                                                    Toast.makeText(c, c.getString(R.string.error), Toast.LENGTH_SHORT).show();
                                                }
                                            });


                                }
                            });


                        }

                        @Override
                        public void onCancelled(@NonNull DatabaseError databaseError) {

                        }
                    });
                }


            }
        });

        forward.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dismiss();
                Global.forwardMessage = message;
                c.startActivity(new Intent(c, Forward.class));
            }
        });
        reply.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dismiss();
                if (message.isChat())
                    Chat.replyMOut(message, c);
                else
                    Group.replyMOut(message, c);

            }
        });


        copy.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                ClipboardManager clipboard = (ClipboardManager) c.getSystemService(Context.CLIPBOARD_SERVICE);
                ClipData clip = ClipData.newPlainText("Message", messagetxt);
                clipboard.setPrimaryClip(clip);
                Toast.makeText(Global.conA, R.string.mssg_cpd, Toast.LENGTH_SHORT).show();
                dismiss();
            }
        });
        //add message deleted
        lastM = encryption.encryptOrNull(lastM);
        mapd = new HashMap<>();

        mapd.put("lastmessage", lastM);
        delete.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                dismiss();
                if (message.isChat()) {
                    final FirebaseAuth mAuth = FirebaseAuth.getInstance();
                    final DatabaseReference mData = FirebaseDatabase.getInstance().getReference(Global.CHATS);
                    final Map<String, Object> map = new HashMap<String, Object>();
                    map.put("type", "delete");

                    mData.child(mAuth.getCurrentUser().getUid()).child(friendid).child(Global.Messages).child(messId).updateChildren(map).addOnCompleteListener(new OnCompleteListener<Void>() {
                        @Override
                        public void onComplete(@NonNull Task<Void> task) {
                            if (position == 0) {
                                mData.child(mAuth.getCurrentUser().getUid()).child(friendid).updateChildren(mapd).addOnCompleteListener(new OnCompleteListener<Void>() {
                                    @Override
                                    public void onComplete(@NonNull Task<Void> task) {
                                        Toast.makeText(Global.conA, R.string.mss_dlt, Toast.LENGTH_SHORT).show();

                                    }
                                });
                            } else
                                Toast.makeText(Global.conA, R.string.mss_dlt, Toast.LENGTH_SHORT).show();
                        }
                    });


                }
            }
        });
        deleteall.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                dismiss();
                mAuth = FirebaseAuth.getInstance();
                if (message.isChat()) {
                    mData = FirebaseDatabase.getInstance().getReference(Global.CHATS);
                    map = new HashMap<String, Object>();
                    map.put("deleted", true);
                    map.put("message", lastM);
                    map.put("type", "text");
                    mData.child(mAuth.getCurrentUser().getUid()).child(friendid).child(Global.Messages).child(messId).updateChildren(map).addOnCompleteListener(new OnCompleteListener<Void>() {
                        @Override
                        public void onComplete(@NonNull Task<Void> task) {
                            mData.child(friendid).child(mAuth.getCurrentUser().getUid()).child(Global.Messages).child(messId).addListenerForSingleValueEvent(new ValueEventListener() {
                                @Override
                                public void onDataChange(DataSnapshot dataSnapshot) {
                                    if (dataSnapshot.exists()) {
                                        deletedChat = false;
                                        kamelDelete();
                                    } else {
                                        deletedChat = true;
                                        kamelDelete();
                                    }
                                }

                                @Override
                                public void onCancelled(DatabaseError databaseError) {

                                }
                            });
                        }
                    });
                } else {
                    mData = FirebaseDatabase.getInstance().getReference(Global.GROUPS);
                    map = new HashMap<String, Object>();
                    map.put("deleted", true);
                    map.put("message", lastM);
                    map.put("type", "text");
                    mData.child(friendid).child(Global.Messages).child(messId).updateChildren(map).addOnCompleteListener(new OnCompleteListener<Void>() {
                        @Override
                        public void onComplete(@NonNull Task<Void> task) {
                            if (position == 0)
                                kameDeleteGG(lastM);
                            else {
                                Global.lastDeletedMessage = message;
                                Group.timerForDeleted();
                                sendMessNotifyG();
                            }
                        }
                    });
                }
            }

        });

        retry.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                Map<String, Object> map = new HashMap<>();

                Object currT = ServerValue.TIMESTAMP;

                dismiss();

                switch (message.getType()) {
                    case "text":
                        message.setStatue("..");
                        if (message.isChat()) {
                            Chat.refreshAdapter();
                            ((AppBack) c.getApplication()).setchatsdb(friendid);
                        } else {
                            Group.refreshAdapter();
                            ((AppBack) c.getApplication()).setchatsdbG(friendid);
                        }

                        map.put("message", encryption.encryptOrNull(message.getText()));
                        map.put("time", currT);
                        map.put("messId", message.getMessid());
                        map.put("react", "no");
                        map.put("avatar", encryption.encryptOrNull(message.getAvatar()));
                        map.put("chat", message.isChat());
                        map.put("seen", false);
                        map.put("type", "text");
                        map.put("deleted", false);
                        map.put("statue", "✔");
                        map.put("reply", encryption.encryptOrNull(message.getReply()));
                        map.put("forw", message.isForw());
                        map.put("call", message.isCall());
                        map.put("from", message.getId());

                        updateData(encryption.encryptOrNull(message.getText()), message.getMessid(), currT, map);

                        break;
                    case "map":
                        message.setStatue("..");
                        if (message.isChat()) {
                            Chat.refreshAdapter();
                            ((AppBack) c.getApplication()).setchatsdb(friendid);
                        } else {
                            Group.refreshAdapter();
                            ((AppBack) c.getApplication()).setchatsdbG(friendid);
                        }
                        map.put("location", encryption.encryptOrNull(message.getMap().getLocation()));
                        map.put("time", currT);
                        map.put("messId", message.getMessid());
                        map.put("react", "no");
                        map.put("avatar", encryption.encryptOrNull(message.getAvatar()));
                        map.put("chat", message.isChat());
                        map.put("seen", false);
                        map.put("type", "map");
                        map.put("deleted", false);
                        map.put("statue", "✔");
                        map.put("reply", encryption.encryptOrNull(message.getReply()));
                        map.put("forw", message.isForw());
                        map.put("call", message.isCall());
                        map.put("from", message.getId());

                        updateData(encryption.encryptOrNull(c.getString(R.string.map_location)), message.getMessid(), currT, map);

                        break;
                    case "voice":
                        message.setStatue("..");
                        if (message.isChat()) {
                            Chat.refreshAdapter();
                            ((AppBack) c.getApplication()).setchatsdb(friendid);
                        } else {
                            Group.refreshAdapter();
                            ((AppBack) c.getApplication()).setchatsdbG(friendid);
                        }

                        StorageReference mStorageRefV = FirebaseStorage.getInstance().getReference();
                        StorageReference riversRefV;


                        if (message.isChat())
                            riversRefV = mStorageRefV.child(Global.Mess + "/" + mAuth.getCurrentUser().getUid() + "/Audio/" + mAuth.getCurrentUser().getUid() + friendid + System.currentTimeMillis() + ".m4a");
                        else
                            riversRefV = mStorageRefV.child(Global.GROUPS + "/" + friendid + "/" + mAuth.getCurrentUser().getUid() + "/Audio/" + mAuth.getCurrentUser().getUid() + friendid + System.currentTimeMillis() + ".m4a");


                        UploadTask uploadTaskV = riversRefV.putFile(Uri.parse(message.getVoice().getUrl()));

                        Task<Uri> urlTaskV = uploadTaskV.continueWithTask(new Continuation<UploadTask.TaskSnapshot, Task<Uri>>() {
                            @Override
                            public Task<Uri> then(@NonNull Task<UploadTask.TaskSnapshot> task) throws Exception {
                                if (!task.isSuccessful()) {
                                    throw task.getException();
                                }

                                // Continue with the task to get the download URL
                                return riversRefV.getDownloadUrl();
                            }
                        }).addOnCompleteListener(new OnCompleteListener<Uri>() {
                            @Override
                            public void onComplete(@NonNull Task<Uri> task) {
                                if (task.isSuccessful()) {
                                    Uri downloadUrl = task.getResult();
                                    MediaMetadataRetriever retriever2 = new MediaMetadataRetriever();
                                    retriever2.setDataSource(c, Uri.parse(message.getVoice().getUrl()));
                                    String time2 = retriever2.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION);
                                    long timeInMillisec2 = Long.parseLong(time2);
                                    retriever2.release();
                                    map.put("linkV", encryption.encryptOrNull(String.valueOf(downloadUrl)));
                                    map.put("time", currT);
                                    map.put("messId", message.getMessid());
                                    map.put("react", "no");
                                    map.put("avatar", encryption.encryptOrNull(message.getAvatar()));
                                    map.put("chat", message.isChat());
                                    map.put("duration", getHumanTimeText(timeInMillisec2));
                                    map.put("seen", false);
                                    map.put("type", "voice");
                                    map.put("deleted", false);
                                    map.put("statue", "✔");
                                    map.put("reply", encryption.encryptOrNull(message.getReply()));
                                    map.put("forw", message.isForw());
                                    map.put("call", message.isCall());
                                    map.put("from", message.getId());

                                    updateData(encryption.encryptOrNull("Voice " + getHumanTimeText(timeInMillisec2)), message.getMessid(), currT, map);


                                }

                            }
                        });
                        break;
                    case "video":
                        message.setStatue("..");
                        if (message.isChat()) {
                            Chat.refreshAdapter();
                            ((AppBack) c.getApplication()).setchatsdb(friendid);
                        } else {
                            Group.refreshAdapter();
                            ((AppBack) c.getApplication()).setchatsdbG(friendid);
                        }
                        StorageReference mStorageRef = FirebaseStorage.getInstance().getReference();


                        StorageReference riversRef;
                        if (message.isChat())
                            riversRef = mStorageRef.child(Global.Mess + "/" + mAuth.getCurrentUser().getUid() + "/Audio/" + mAuth.getCurrentUser().getUid() + friendid + System.currentTimeMillis() + ".m4a");
                        else
                            riversRef = mStorageRef.child(Global.GROUPS + "/" + friendid + "/" + mAuth.getCurrentUser().getUid() + "/Video/" + mAuth.getCurrentUser().getUid() + friendid + System.currentTimeMillis() + ".mp4");


                        UploadTask uploadTask = riversRef.putFile(Uri.parse(message.getVideo().getUrl()));

                        Task<Uri> urlTask = uploadTask.continueWithTask(new Continuation<UploadTask.TaskSnapshot, Task<Uri>>() {
                            @Override
                            public Task<Uri> then(@NonNull Task<UploadTask.TaskSnapshot> task) throws Exception {
                                if (!task.isSuccessful()) {
                                    throw task.getException();
                                }

                                // Continue with the task to get the download URL
                                return riversRef.getDownloadUrl();
                            }
                        }).addOnCompleteListener(new OnCompleteListener<Uri>() {
                            @Override
                            public void onComplete(@NonNull Task<Uri> task) {
                                if (task.isSuccessful()) {
                                    Uri downloadUrl = task.getResult();


                                    Bitmap thumb = ThumbnailUtils.createVideoThumbnail(message.getVideo().getUrl().replace("file:///", ""), MediaStore.Video.Thumbnails.MINI_KIND);
                                    ByteArrayOutputStream bao = new ByteArrayOutputStream();
                                    thumb.compress(Bitmap.CompressFormat.PNG, 100, bao);
                                    byte[] byteArray = bao.toByteArray();
                                    StorageReference mStorageRef = FirebaseStorage.getInstance().getReference();
                                    StorageReference riversRef;
                                    if (message.isChat())
                                        riversRef = mStorageRef.child(Global.Mess + "/" + mAuth.getCurrentUser().getUid() + "/Video/" + "Thumb/" + mAuth.getCurrentUser().getUid() + friendid + System.currentTimeMillis() + ".png");
                                    else
                                        riversRef = mStorageRef.child(Global.GROUPS + "/" + friendid + "/" + mAuth.getCurrentUser().getUid() + "/Video/" + "Thumb/" + mAuth.getCurrentUser().getUid() + friendid + System.currentTimeMillis() + ".png");

                                    UploadTask uploadTask = riversRef.putBytes(byteArray);

                                    Task<Uri> urlTask = uploadTask.continueWithTask(new Continuation<UploadTask.TaskSnapshot, Task<Uri>>() {
                                        @Override
                                        public Task<Uri> then(@NonNull Task<UploadTask.TaskSnapshot> task) throws Exception {
                                            if (!task.isSuccessful()) {
                                                throw task.getException();
                                            }

                                            // Continue with the task to get the download URL
                                            return riversRef.getDownloadUrl();
                                        }
                                    }).addOnCompleteListener(new OnCompleteListener<Uri>() {
                                        @Override
                                        public void onComplete(@NonNull Task<Uri> task) {
                                            if (task.isSuccessful()) {
                                                Uri thumbD = task.getResult();

                                                MediaMetadataRetriever retriever = new MediaMetadataRetriever();
                                                retriever.setDataSource(c, Uri.parse(message.getVideo().getUrl()));
                                                String time = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION);
                                                long timeInMillisec = Long.parseLong(time);
                                                retriever.release();
                                                map.put("linkVideo", encryption.encryptOrNull(String.valueOf(downloadUrl)));
                                                map.put("time", currT);
                                                map.put("messId", message.getMessid());
                                                map.put("react", "no");
                                                map.put("avatar", encryption.encryptOrNull(message.getAvatar()));
                                                map.put("chat", message.isChat());
                                                map.put("thumb", String.valueOf(thumbD));
                                                map.put("duration", getHumanTimeText(timeInMillisec));
                                                map.put("seen", false);
                                                map.put("type", "video");
                                                map.put("deleted", false);
                                                map.put("statue", "✔");
                                                map.put("reply", encryption.encryptOrNull(message.getReply()));
                                                map.put("forw", message.isForw());
                                                map.put("call", message.isCall());
                                                map.put("from", message.getId());

                                                updateData(encryption.encryptOrNull("Video " + getHumanTimeText(timeInMillisec)), message.getMessid(), currT, map);


                                            }
                                        }
                                    });


                                }

                            }
                        });

                        break;
                    case "file":
                        message.setStatue("..");
                        if (message.isChat()) {
                            Chat.refreshAdapter();
                            ((AppBack) c.getApplication()).setchatsdb(friendid);
                        } else {
                            Group.refreshAdapter();
                            ((AppBack) c.getApplication()).setchatsdbG(friendid);
                        }
                        StorageReference mStorageRefF = FirebaseStorage.getInstance().getReference();
                        StorageReference riversRefF;

                        if (message.isChat())
                            riversRefF = mStorageRefF.child(Global.Mess + "/" + mAuth.getCurrentUser().getUid() + "/Files/" + mAuth.getCurrentUser().getUid() + friendid + System.currentTimeMillis() + message.getFile().getFilename());
                        else
                            riversRefF = mStorageRefF.child(Global.GROUPS + "/" + friendid + "/" + mAuth.getCurrentUser().getUid() + "/Files/" + mAuth.getCurrentUser().getUid() + friendid + System.currentTimeMillis() + message.getFile().getFilename());

                        UploadTask uploadTaskF = riversRefF.putFile(Uri.parse(message.getFile().getUrl()));

                        Task<Uri> urlTaskF = uploadTaskF.continueWithTask(new Continuation<UploadTask.TaskSnapshot, Task<Uri>>() {
                            @Override
                            public Task<Uri> then(@NonNull Task<UploadTask.TaskSnapshot> task) throws Exception {
                                if (!task.isSuccessful()) {
                                    throw task.getException();
                                }

                                // Continue with the task to get the download URL
                                return riversRefF.getDownloadUrl();
                            }
                        }).addOnCompleteListener(new OnCompleteListener<Uri>() {
                            @Override
                            public void onComplete(@NonNull Task<Uri> task) {
                                if (task.isSuccessful()) {
                                    Uri downloadUrl = task.getResult();
                                    map.put("linkF", encryption.encryptOrNull(String.valueOf(downloadUrl)));
                                    map.put("time", currT);
                                    map.put("messId", message.getMessid());
                                    map.put("react", "no");
                                    map.put("avatar", encryption.encryptOrNull(message.getAvatar()));
                                    map.put("chat", message.isChat());
                                    map.put("filename", message.getFile().getFilename());
                                    map.put("seen", false);
                                    map.put("type", "file");
                                    map.put("deleted", false);
                                    map.put("statue", "✔");
                                    map.put("reply", encryption.encryptOrNull(message.getReply()));
                                    map.put("forw", message.isForw());
                                    map.put("call", message.isCall());
                                    map.put("from", message.getId());

                                    updateData(encryption.encryptOrNull("File " + message.getFile().getFilename()), message.getMessid(), currT, map);

                                }

                            }
                        });

                        break;
                    case "image":
                        message.setStatue("..");
                        if (message.isChat()) {
                            Chat.refreshAdapter();
                            ((AppBack) c.getApplication()).setchatsdb(friendid);
                        } else {
                            Group.refreshAdapter();
                            ((AppBack) c.getApplication()).setchatsdbG(friendid);
                        }
                        Bitmap compressedImageFile = null;
                        byte[] thumbData;
                        StorageReference mStorageRefI = FirebaseStorage.getInstance().getReference();
                        StorageReference riversRefI;
                        UploadTask uploadTaskI;
                        //      //compress the photo


                        File newImageFile = new File(message.getImageUrl().replace("file://", ""));
                        if (!message.getImageUrl().contains("android.resource://" + R.class.getPackage().getName() + "/")) {
                            try {
                                compressedImageFile = new Compressor(c)
                                        .setMaxHeight(500)
                                        .setMaxWidth(500)
                                        .setQuality(50)
                                        .compressToBitmap(newImageFile);

                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                            ByteArrayOutputStream baos = new ByteArrayOutputStream();
                            compressedImageFile.compress(Bitmap.CompressFormat.JPEG, 100, baos);
                            thumbData = baos.toByteArray();
                            if (message.isChat())
                                riversRefI = mStorageRefI.child(Global.Mess + "/" + mAuth.getCurrentUser().getUid() + "/Images/" + mAuth.getCurrentUser().getUid() + friendid + System.currentTimeMillis() + ".jpg");
                            else
                                riversRefI = mStorageRefI.child(Global.GROUPS + "/" + friendid + "/" + mAuth.getCurrentUser().getUid() + "/Images/" + mAuth.getCurrentUser().getUid() + friendid + System.currentTimeMillis() + ".jpg");

                            uploadTaskI = riversRefI.putBytes(thumbData);

                        } else {
                            if (message.isChat())
                                riversRefI = mStorageRefI.child(Global.Mess + "/" + mAuth.getCurrentUser().getUid() + "/Images/" + mAuth.getCurrentUser().getUid() + friendid + System.currentTimeMillis() + ".png");
                            else
                                riversRefI = mStorageRefI.child(Global.GROUPS + "/" + friendid + "/" + mAuth.getCurrentUser().getUid() + "/Images/" + mAuth.getCurrentUser().getUid() + friendid + System.currentTimeMillis() + ".png");

                            uploadTaskI = riversRefI.putFile(Uri.parse(message.getImageUrl()));

                        }

                        Task<Uri> urlTaskI = uploadTaskI.continueWithTask(new Continuation<UploadTask.TaskSnapshot, Task<Uri>>() {
                            @Override
                            public Task<Uri> then(@NonNull Task<UploadTask.TaskSnapshot> task) throws Exception {
                                if (!task.isSuccessful()) {
                                    throw task.getException();
                                }

                                // Continue with the task to get the download URL
                                return riversRefI.getDownloadUrl();
                            }
                        }).addOnCompleteListener(new OnCompleteListener<Uri>() {
                            @Override
                            public void onComplete(@NonNull Task<Uri> task) {
                                if (task.isSuccessful()) {
                                    Uri downloadUrl = task.getResult();

                                    map.put("linkI", encryption.encryptOrNull(String.valueOf(downloadUrl)));
                                    map.put("time", currT);
                                    map.put("messId", message.getMessid());
                                    map.put("react", "no");
                                    map.put("avatar", encryption.encryptOrNull(message.getAvatar()));
                                    map.put("chat", message.isChat());
                                    map.put("seen", false);
                                    map.put("type", "image");
                                    map.put("deleted", false);
                                    map.put("statue", "✔");
                                    map.put("reply", encryption.encryptOrNull(message.getReply()));
                                    map.put("forw", message.isForw());
                                    map.put("call", message.isCall());
                                    map.put("from", message.getId());

                                    if (!message.getImageUrl().contains("android.resource://" + R.class.getPackage().getName() + "/"))
                                        updateData(encryption.encryptOrNull("Image"), message.getMessid(), currT, map);
                                    else
                                        updateData(encryption.encryptOrNull("Sticker"), message.getMessid(), currT, map);

                                }

                            }
                        });

                        break;


                }


            }
        });


    }

    private int getAvasCount = 0;

    private void kameDeleteGG(String mDeleteT) {

        Map<String, Object> map = new HashMap<>();
        map.put("lastmessage", mDeleteT);
        map.put("lastsender", mAuth.getCurrentUser().getUid());
        map.put("lastsenderava", Global.avaLocal);


        for (int i = 0; i < Global.currGUsers.size(); i++) {
            int finalI = i;
            userD.child(Global.currGUsers.get(i)).child(Global.GROUPS).child(friendid).updateChildren(map).addOnCompleteListener(new OnCompleteListener<Void>() {
                @Override
                public void onComplete(@NonNull Task<Void> task) {

                    if (finalI == Global.currGUsers.size() - 1) {
                        Global.lastDeletedMessage = message;
                        Group.timerForDeleted();
                        sendMessNotifyG();
                    }
                }
            });
        }
    }

    private void kamelDelete() {
        if (!deletedChat) {
            mData.child(friendid).child(mAuth.getCurrentUser().getUid()).child(Global.Messages).child(messId).updateChildren(map).addOnCompleteListener(new OnCompleteListener<Void>() {
                @Override
                public void onComplete(@NonNull Task<Void> task) {
                    if (position == 0) {
                        mData.child(mAuth.getCurrentUser().getUid()).child(friendid).updateChildren(mapd).addOnCompleteListener(new OnCompleteListener<Void>() {
                            @Override
                            public void onComplete(@NonNull Task<Void> task) {
                                if (!deletedChat) {
                                    mData.child(friendid).child(mAuth.getCurrentUser().getUid()).updateChildren(mapd).addOnCompleteListener(new OnCompleteListener<Void>() {
                                        @Override
                                        public void onComplete(@NonNull Task<Void> task) {
                                            Global.lastDeletedMessage = message;
                                            Chat.timerForDeleted();
                                            sendMessNotify();

                                        }
                                    });
                                }
                            }
                        });
                    } else {
                        Global.lastDeletedMessage = message;

                        Chat.timerForDeleted();
                        sendMessNotify();
                    }

                }
            });
        } else {
            if (position == 0) {
                mData.child(mAuth.getCurrentUser().getUid()).child(friendid).updateChildren(mapd).addOnCompleteListener(new OnCompleteListener<Void>() {
                    @Override
                    public void onComplete(@NonNull Task<Void> task) {
                        Global.lastDeletedMessage = message;

                        Chat.timerForDeleted();
                        Toast.makeText(Global.conA, R.string.mss_dlt, Toast.LENGTH_SHORT).show();
                    }
                });
            } else {
                Toast.makeText(Global.conA, R.string.mss_dlt, Toast.LENGTH_SHORT).show();
                Global.lastDeletedMessage = message;
                Chat.timerForDeleted();
            }
        }
    }


    private void sendMessNotify() {

        //fcm notify
        final FCM fcm = Global.getFCMservies();
        DatabaseReference mTokenget = FirebaseDatabase.getInstance().getReference(Global.tokens);
        mTokenget.child(friendid).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                Tokens tokens = dataSnapshot.getValue(Tokens.class);
                String Ptoken = FirebaseInstanceId.getInstance().getToken();
                Map<String, String> map = new HashMap<>();
                map.put("title", Ptoken + "#" + "ID" + "#" + "name" + "#" + "AVAU" + "#" + messId);
                map.put("to", friendid);
                map.put("message", lastM);
                Sender sender = new Sender(tokens.getTokens(), map);
                fcm.send(sender)
                        .enqueue(new Callback<FCMresp>() {
                            @Override
                            public void onResponse(Call<FCMresp> call, Response<FCMresp> response) {
                                Toast.makeText(Global.conA, R.string.mss_dlt, Toast.LENGTH_SHORT).show();
                            }

                            @Override
                            public void onFailure(Call<FCMresp> call, Throwable t) {
                            }
                        });
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });

    }


    private void sendMessNotifyG() {
        //fcm notify
        final FCM fcm = Global.getFCMservies();

        for (int i = 0; i < Global.currGUsers.size(); i++) {
            if (!Global.currGUsers.get(i).equals(mAuth.getCurrentUser().getUid())) {
                DatabaseReference mTokenget = FirebaseDatabase.getInstance().getReference(Global.tokens);
                int finalI = i;
                mTokenget.child(Global.currGUsers.get(i)).addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                        Tokens tokens = dataSnapshot.getValue(Tokens.class);
                        Map<String, String> map = new HashMap<>();
                        map.put("title", tokens + "#" + "groups-ID" + "#" + friendid + "#" + "AVAU" + "#" + messId);
                        map.put("to", Global.currGUsers.get(finalI));
                        map.put("message", lastM);
                        Sender sender = new Sender(tokens.getTokens(), map);
                        fcm.send(sender)
                                .enqueue(new Callback<FCMresp>() {
                                    @Override
                                    public void onResponse(Call<FCMresp> call, Response<FCMresp> response) {
                                    }

                                    @Override
                                    public void onFailure(Call<FCMresp> call, Throwable t) {
                                    }
                                });

                    }


                    @Override
                    public void onCancelled(@NonNull DatabaseError databaseError) {

                    }
                });
            }
        }

    }

    public int halbine(ArrayList<MessageFav> ml, String id) {
        int j = 0, i = 0;
        for (i = 0; i < ml.size(); i++) {
            if (ml.get(i).getMessId().equals(id)) {
                j = 1;
                break;
            }

        }
        if (j == 1)
            return i;
        else
            return -1;

    }

    private String getHumanTimeText(long milliseconds) {
        return String.format("%02d:%02d",
                TimeUnit.MILLISECONDS.toMinutes(milliseconds),
                TimeUnit.MILLISECONDS.toSeconds(milliseconds) -
                        TimeUnit.MINUTES.toSeconds(TimeUnit.MILLISECONDS.toMinutes(milliseconds)));
    }

    public void updateData(String mess, String messid, Object currTime, Map<String, Object> MessMap) {
        if (mAuth.getCurrentUser() != null) {
            if (message.isChat()) {
                Map<String, Object> map = new HashMap<>();
                Map<String, Object> map2 = new HashMap<>();

                map.put("avatar", Global.avaLocal);
                map.put("name", Global.nameLocal);
                map.put("nameL", Global.nameLocal);
                map.put("phone", Global.phoneLocal);
                map.put("id", mAuth.getCurrentUser().getUid());
                map.put("screen", Global.myscreen);
                map.put("lastmessage", mess);
                map.put("lastsender", mAuth.getCurrentUser().getUid());
                map.put("lastsenderava", Global.avaLocal);
                map.put("messDate", currTime);

                map2.put("avatar", Global.currAva);
                map2.put("name", Global.currname);
                map2.put("nameL", Global.currname);
                map2.put("phone", Global.currphone);
                map2.put("id", friendid);
                map2.put("screen", Global.currscreen);
                map2.put("lastmessage", mess);
                map2.put("lastsender", mAuth.getCurrentUser().getUid());
                map2.put("lastsenderava", Global.avaLocal);
                map2.put("messDate", currTime);


                mMess.child(mAuth.getCurrentUser().getUid()).child(friendid).updateChildren(map2).addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void aVoid) {
                        mMess.child(friendid).child(mAuth.getCurrentUser().getUid()).updateChildren(map).addOnSuccessListener(new OnSuccessListener<Void>() {
                            @Override
                            public void onSuccess(Void aVoid) {
                                mMess.child(mAuth.getCurrentUser().getUid()).child(friendid).child(Global.Messages).child(message.getMessid()).updateChildren(MessMap).addOnSuccessListener(new OnSuccessListener<Void>() {
                                    @Override
                                    public void onSuccess(Void aVoid) {
                                        mMess.child(friendid).child(mAuth.getCurrentUser().getUid()).child(Global.Messages).child(message.getMessid()).updateChildren(MessMap).addOnSuccessListener(new OnSuccessListener<Void>() {
                                            @Override
                                            public void onSuccess(Void aVoid) {

                                                sendMessNotify(mess, messid);

                                            }
                                        });
                                    }
                                });
                            }
                        });
                    }
                });

            } else {
                mData.child(friendid).child(Global.Messages).child(message.getMessid())
                        .updateChildren(MessMap).addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void aVoid) {
                        Map<String, Object> map2 = new HashMap<>();
                        map2.put("lastmessage", mess);
                        map2.put("lastsender", mAuth.getCurrentUser().getUid());
                        map2.put("lastsenderava", Global.avaLocal);
                        map2.put("messDate", currTime);

                        mData.child(friendid).updateChildren(map2).addOnSuccessListener(new OnSuccessListener<Void>() {
                            @Override
                            public void onSuccess(Void aVoid) {
                                userD.child(mAuth.getCurrentUser().getUid()).child(Global.GROUPS).child(friendid).updateChildren(map2).addOnSuccessListener(new OnSuccessListener<Void>() {
                                    @Override
                                    public void onSuccess(Void aVoid) {
                                        sendMessNotifyG(mess, messid);
                                        for (int i = 0; i < Global.currGUsers.size(); i++) {
                                            int j = i;
                                            if (!Global.currGUsers.get(i).equals(mAuth.getCurrentUser().getUid())) {

                                                userD.child(Global.currGUsers.get(i)).child(Global.GROUPS).child(friendid).updateChildren(map2).addOnSuccessListener(new OnSuccessListener<Void>() {
                                                    @Override
                                                    public void onSuccess(Void aVoid) {
                                                    }
                                                });
                                            }

                                        }
                                    }
                                });
                            }
                        });
                    }
                }).addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {

                    }
                });


            }


        }
    }

    private void sendMessNotify(final String message, final String Mid) {

        DatabaseReference mTokenget = FirebaseDatabase.getInstance().getReference(Global.tokens);
        mTokenget.child(friendid).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if (mAuth.getCurrentUser() != null) {

                    Tokens tokens = dataSnapshot.getValue(Tokens.class);
                    Map<String, String> map = new HashMap<>();
                    map.put("title", tokens + "#" + mAuth.getCurrentUser().getUid() + "#" + Global.nameLocal + "#" + Global.avaLocal + "#" + Mid);
                    map.put("to", friendid);
                    map.put("message", message);
                    Sender sender = new Sender(tokens.getTokens(), map);
                    fcm.send(sender)
                            .enqueue(new Callback<FCMresp>() {
                                @Override
                                public void onResponse(Call<FCMresp> call, Response<FCMresp> response) {
                                }

                                @Override
                                public void onFailure(Call<FCMresp> call, Throwable t) {

                                }
                            });
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });

    }

    private void sendMessNotifyG(final String message, final String Mid) {

        for (int i = 0; i < Global.currGUsers.size(); i++) {
            if (!Global.currGUsers.get(i).equals(mAuth.getCurrentUser().getUid())) {
                DatabaseReference mTokenget = FirebaseDatabase.getInstance().getReference(Global.tokens);
                int finalI = i;
                mTokenget.child(Global.currGUsers.get(i)).addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                        Tokens tokens = dataSnapshot.getValue(Tokens.class);
                        Map<String, String> map = new HashMap<>();
                        map.put("title", tokens + "#" + friendid + "#" + Global.currname + "#" + Global.currAva + "#" + Mid);
                        map.put("to", Global.currGUsers.get(finalI));
                        map.put("message", message);
                        Sender sender = new Sender(tokens.getTokens(), map);
                        fcm.send(sender)
                                .enqueue(new Callback<FCMresp>() {
                                    @Override
                                    public void onResponse(Call<FCMresp> call, Response<FCMresp> response) {
                                    }

                                    @Override
                                    public void onFailure(Call<FCMresp> call, Throwable t) {
                                    }
                                });

                    }


                    @Override
                    public void onCancelled(@NonNull DatabaseError databaseError) {

                    }
                });
            }
        }


    }


}