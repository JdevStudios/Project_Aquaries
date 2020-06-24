package com.chemsgr.blu.fragments;


import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ShortcutInfo;
import android.content.pm.ShortcutManager;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Icon;
import android.os.Build;
import android.os.Bundle;

import com.amulyakhare.textdrawable.TextDrawable;

import com.chemsgr.blu.*;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.fragment.app.Fragment;

import com.amulyakhare.textdrawable.util.ColorGenerator;
import com.etebarian.meowbottomnavigation.MeowBottomNavigation;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.util.DisplayMetrics;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ServerValue;
import com.google.firebase.database.ValueEventListener;
import com.karumi.dexter.Dexter;
import com.karumi.dexter.MultiplePermissionsReport;
import com.karumi.dexter.PermissionToken;
import com.karumi.dexter.listener.PermissionRequest;
import com.karumi.dexter.listener.multi.MultiplePermissionsListener;
import com.squareup.picasso.Picasso;
import com.stfalcon.chatkit.commons.ImageLoader;
import com.stfalcon.chatkit.dialogs.DialogsList;
import com.stfalcon.chatkit.dialogs.DialogsListAdapter;
import com.stfalcon.chatkit.utils.DateFormatter;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import com.chemsgr.blu.Chat;
import com.chemsgr.blu.Contacts;
import com.chemsgr.blu.MainActivity;
import com.chemsgr.blu.R;
import com.chemsgr.blu.adapters.StoryAdapter;
import com.chemsgr.blu.custom.ChatCelect;
import com.chemsgr.blu.datasetters.DialogData;
import com.chemsgr.blu.global.AppBack;
import com.chemsgr.blu.global.Global;
import com.chemsgr.blu.holders.DialogHolder;
import com.chemsgr.blu.lists.StoryList;
import com.chemsgr.blu.lists.StoryListRetr;
import com.chemsgr.blu.lists.UserData;
import com.chemsgr.blu.lists.UserDetailsStory;
import com.chemsgr.blu.models.DefaultDialog;
import com.chemsgr.blu.story.AddStory;
import com.chemsgr.blu.story.StoryView;
import de.hdodenhof.circleimageview.CircleImageView;
import se.simbio.encryption.Encryption;
import uk.co.samuelwall.materialtaptargetprompt.MaterialTapTargetPrompt;
import xute.storyview.StoryModel;

import com.stfalcon.chatkit.me.UserIn;

/**
 * Created by CodeSlu
 */
public class Chats extends Fragment
        implements DialogsListAdapter.OnDialogClickListener<DefaultDialog>,
        DialogsListAdapter.OnDialogLongClickListener<DefaultDialog> {

    //view
    FloatingActionButton fab;
    View view;
    //arrays
    //firebase
    DatabaseReference mData, mBlock, mMute;
    FirebaseAuth mAuth;
    ArrayList<DefaultDialog> dialogs;
    //new
    ImageLoader imageLoader;
    static DialogsListAdapter<DefaultDialog> dialogsAdapter;
    DialogsList dialogsList;
    static String idR = "";
    //get Message query

    //stories
    RecyclerView storyList;
    StoryAdapter adapter;
    ArrayList<StoryList> array;
    StoryView storyView;
    ImageView avaStory;
    ProgressBar progall;
    ImageView sora;
    TextView you;
    //Firebase
    DatabaseReference myData, mUserDB, mOtherData, mTime;
    ArrayList<StoryListRetr> storytemplist;

    //vars
    ArrayList<StoryModel> otherS;
    ArrayList<UserIn> tempUser;

    ArrayList<UserData> userList, contactList, searchL;
    String name = "", ava = "", id = "";
    int enter = 0;
    long now = 0;

    //enc
    Encryption encryption;
    int count = 0;
    String idTemp;

    boolean first = true;
    MeowBottomNavigation meowBottomNavigation;
    private Activity mActivity;

    public Chats() {
        // Required empty public constructor
    }
    ColorGenerator generator = ColorGenerator.MATERIAL;
    static String printInitials(String name)
    {
        String ret = "";

        try {
            ret= ret+ Character.toUpperCase(name.charAt(0));

            // Traverse rest of the string and
            // print the characters after spaces.
            for (int i = 1; i < name.length() - 1; i++) {
                if (name.charAt(i) == ' ') {
                    ret += Character.toUpperCase(
                            name.charAt(i + 1));
                }
            }
        }catch(StringIndexOutOfBoundsException e){
            ret=name;
        }

        return ret.replace(" ","");
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        view = inflater.inflate(R.layout.fragment_chats, container, false);
        fab = (FloatingActionButton) view.findViewById(R.id.fab);
        sora = view.findViewById(R.id.sora);
        you = view.findViewById(R.id.you);
        dialogs = new ArrayList<>();
        //chat screen list
        dialogsList = (DialogsList) view.findViewById(R.id.dialogsList);
        if (isAdded())
            meowBottomNavigation = mActivity.findViewById(R.id.meownav);
        mAuth = FirebaseAuth.getInstance();
        mMute = FirebaseDatabase.getInstance().getReference(Global.MUTE);
        mBlock = FirebaseDatabase.getInstance().getReference(Global.BLOCK);
        sora.setVisibility(View.GONE);

        imageLoader = new ImageLoader() {
            @Override
            public void loadImage(ImageView imageView, String url, Object payload) {

            }
        };


        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Dexter.withActivity(mActivity)
                        .withPermissions(Manifest.permission.READ_CONTACTS)
                        .withListener(new MultiplePermissionsListener() {
                            @Override
                            public void onPermissionsChecked(MultiplePermissionsReport report) {

                                if (report.areAllPermissionsGranted())
                                    startActivity(new Intent(mActivity, Contacts.class));

                                else
                                    Toast.makeText(mActivity, mActivity.getString(R.string.acc_per), Toast.LENGTH_SHORT).show();


                            }

                            @Override
                            public void onPermissionRationaleShouldBeShown(List<PermissionRequest> permissions, PermissionToken token) {

                                token.continuePermissionRequest();

                            }
                        }).check();
            }
        });
        dialogsList.addOnScrollListener(new RecyclerView.OnScrollListener() {

            @Override
            public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
                if (dy > 0 || dy < 0 && fab.isShown())
                    fab.hide();
            }

            @Override
            public void onScrollStateChanged(RecyclerView recyclerView, int newState) {
                if (newState == RecyclerView.SCROLL_STATE_IDLE)
                    fab.show();
                super.onScrollStateChanged(recyclerView, newState);
            }
        });

        if (mAuth.getCurrentUser() != null) {

            //dark mode init
            if (mAuth.getCurrentUser() != null) {
                if (!((AppBack) mActivity.getApplication()).shared().getBoolean("dark" + mAuth.getCurrentUser().getUid(), false)) {
                    you.setTextColor(Color.BLACK);
                } else {
                    you.setTextColor(Color.WHITE);
                }
            }

            if (((AppBack) mActivity.getApplication()).shared().getBoolean("hinter" + mAuth.getCurrentUser().getUid(), true)) {
                ((AppBack) mActivity.getApplication()).editSharePrefs().putBoolean("hinter" + mAuth.getCurrentUser().getUid(), false);
                ((AppBack) mActivity.getApplication()).editSharePrefs().apply();
                view.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
                    @Override
                    public void onGlobalLayout() {
                        view.getViewTreeObserver().removeOnGlobalLayoutListener(this);
                        new MaterialTapTargetPrompt.Builder(mActivity)
                                .setTarget(R.id.fab)
                                .setBackgroundColour(ContextCompat.getColor(getContext(), R.color.colorPrimary))
                                .setPrimaryText(getString(R.string.contacts))
                                .setSecondaryText(getString(R.string.you_can_hint))
                                .setPromptStateChangeListener(new MaterialTapTargetPrompt.PromptStateChangeListener() {
                                    @Override
                                    public void onPromptStateChanged(MaterialTapTargetPrompt prompt, int state) {
                                        if (state == MaterialTapTargetPrompt.STATE_FOCAL_PRESSED) {
                                            prompt.finish();
                                            ((AppBack) mActivity.getApplication()).editSharePrefs().putBoolean("hinter" + mAuth.getCurrentUser().getUid(), false);
                                            ((AppBack) mActivity.getApplication()).editSharePrefs().apply();
                                        }
                                    }
                                })
                                .show();
                    }
                });

            }

            getChats();

            getStories();
        }
        return view;
    }


    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        mActivity = (Activity) context;
        Global.currentfragment = mActivity;
    }

    private void getChats() {
        final long[] keyOnce = {0};
        final int[] i = {0};
        //getride of old data
        if (mAuth.getCurrentUser() != null) {
            if (isAdded()) {
                ((AppBack) mActivity.getApplication()).getdialogdb(mAuth.getCurrentUser().getUid());
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N_MR1)
                    shortcuts();
            }
        }
        mData = FirebaseDatabase.getInstance().getReference(Global.CHATS).child(FirebaseAuth.getInstance().getCurrentUser().getUid());
        Query query = mData.orderByChild("messDate");
        query.keepSynced(true);
        initAdapter();
        //if offline

        try {
            if (Global.diaG.size() == 0) {
                sora.setVisibility(View.VISIBLE);
                dialogsList.setVisibility(View.GONE);
            } else {
                sora.setVisibility(View.GONE);
                dialogsList.setVisibility(View.VISIBLE);

            }
        } catch (NullPointerException e) {

        }


        if (!Global.check_int(mActivity)) {
            //update the list
            dialogsAdapter.clear();
            dialogsAdapter.setItems(DialogData.getDialogs(Global.diaG));
            dialogsAdapter.notifyDataSetChanged();
            mActivity.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    count = 0;
                    for (int j = 0; j < Global.diaG.size(); j++) {
                        count += Global.diaG.get(j).getNoOfUnread();
                    }
                    try {
                        if (count == 0)
                            meowBottomNavigation.setCount(1, "empty");
                        else if (count > 99)
                            meowBottomNavigation.setCount(1, String.valueOf(99));
                        else
                            meowBottomNavigation.setCount(1, String.valueOf(count));
                    } catch (NullPointerException e) {

                    }
                }
            });


        } else {
            tempUser = new ArrayList<>(Global.diaG);
            Global.diaG.clear();
            dialogsAdapter.clear();
            dialogsAdapter.setItems(DialogData.getDialogs(tempUser));
            dialogsAdapter.notifyDataSetChanged();


            mActivity.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    count = 0;
                    for (int j = 0; j < tempUser.size(); j++) {
                        count += tempUser.get(j).getNoOfUnread();
                    }
                    try {
                        if (count == 0)
                            meowBottomNavigation.setCount(1, "empty");
                        else if (count > 99)
                            meowBottomNavigation.setCount(1, String.valueOf(99));
                        else
                            meowBottomNavigation.setCount(1, String.valueOf(count));
                    } catch (NullPointerException e) {

                    }
                }
            });


        }

        //if online
        query.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                keyOnce[0] = dataSnapshot.getChildrenCount();
                Global.diaG.clear();
                query.addChildEventListener(new ChildEventListener() {
                    @Override
                    public void onChildAdded(DataSnapshot dataSnapshot, String s) {
                        if (dataSnapshot.exists()) {
                            UserIn chats = dataSnapshot.getValue(UserIn.class);
                            try {
                                if (chats.getId() != null) {
                                    if (dialogsAdapter.halbine(Global.diaG, chats.getId()) == -1)
                                        Global.diaG.add(chats);
                                    else
                                        Global.diaG.set(dialogsAdapter.halbine(Global.diaG, chats.getId()), chats);

                                    dialogsAdapter.notifyDataSetChanged();
                                }
                            } catch (NullPointerException e) {
                            }

                            //check only in global list range
                            if (i[0] >= keyOnce[0] - 1) {
                                if (mAuth.getCurrentUser() != null) {
                                    if (isAdded()) {
                                        ((AppBack) mActivity.getApplication()).setdialogdb(mAuth.getCurrentUser().getUid());
                                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N_MR1)
                                            shortcuts();
                                    }
                                }
                                //update the list
                                dialogsAdapter.clear();
                                dialogsAdapter.setItems(DialogData.getDialogs(Global.diaG));
                                dialogsAdapter.notifyDataSetChanged();
                                count = 0;
                                for (int j = 0; j < Global.diaG.size(); j++) {
                                    count += Global.diaG.get(j).getNoOfUnread();
                                }
                                try {
                                    if (count == 0)
                                        meowBottomNavigation.setCount(1, "empty");
                                    else if (count > 99)
                                        meowBottomNavigation.setCount(1, String.valueOf(99));
                                    else
                                        meowBottomNavigation.setCount(1, String.valueOf(count));

                                    if (Global.diaG.size() == 0) {
                                        sora.setVisibility(View.VISIBLE);
                                        dialogsList.setVisibility(View.GONE);
                                    } else {
                                        sora.setVisibility(View.GONE);
                                        dialogsList.setVisibility(View.VISIBLE);
                                    }
                                } catch (NullPointerException e) {

                                }


                            }

                            i[0]++;
                        }
                    }

                    @Override
                    public void onChildChanged(DataSnapshot dataSnapshot, String s) {
                        UserIn chats = dataSnapshot.getValue(UserIn.class);
                        try {

                            if (chats.getId() != null && chats.getName() != null) {
                                if (dialogsAdapter.halbine(Global.diaG, chats.getId()) == -1)
                                    Global.diaG.add(chats);
                                else
                                    Global.diaG.set(dialogsAdapter.halbine(Global.diaG, chats.getId()), chats);


                                if (mAuth.getCurrentUser() != null) {
                                    if (isAdded()) {
                                        ((AppBack) mActivity.getApplication()).setdialogdb(mAuth.getCurrentUser().getUid());
                                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N_MR1)
                                            shortcuts();
                                    }
                                }
                                dialogsAdapter.notifyDataSetChanged();


                                if (mAuth.getCurrentUser() != null) {
                                    if (isAdded()) {
                                        ((AppBack) mActivity.getApplication()).setdialogdb(mAuth.getCurrentUser().getUid());
                                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N_MR1)
                                            shortcuts();
                                    }
                                }

                                //update the list
                                dialogsAdapter.clear();
                                dialogsAdapter.setItems(DialogData.getDialogs(Global.diaG));
                                dialogsAdapter.notifyDataSetChanged();
                                count = 0;
                                for (int j = 0; j < Global.diaG.size(); j++) {
                                    count += Global.diaG.get(j).getNoOfUnread();
                                }
                                try {
                                    if (count == 0)
                                        meowBottomNavigation.setCount(1, "empty");
                                    else if (count > 99)
                                        meowBottomNavigation.setCount(1, String.valueOf(99));
                                    else
                                        meowBottomNavigation.setCount(1, String.valueOf(count));

                                    if (Global.diaG.size() == 0) {
                                        sora.setVisibility(View.VISIBLE);
                                        dialogsList.setVisibility(View.GONE);
                                    } else {
                                        sora.setVisibility(View.GONE);
                                        dialogsList.setVisibility(View.VISIBLE);

                                    }
                                } catch (NullPointerException e) {

                                }

                            }


                        } catch (
                                NullPointerException e) {

                        }


                    }

                    @Override
                    public void onChildRemoved(DataSnapshot dataSnapshot) {
                        UserIn chats = dataSnapshot.getValue(UserIn.class);

                        if (Global.diaG.size() != 0) {
                            if (dialogsAdapter.halbine(Global.diaG, chats.getId()) != -1)
                                Global.diaG.remove(dialogsAdapter.halbine(Global.diaG, chats.getId()));
                        }
                        //local store
                        if (mAuth.getCurrentUser() != null) {
                            if (isAdded()) {
                                ((AppBack) mActivity.getApplication()).setdialogdb(mAuth.getCurrentUser().getUid());
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N_MR1)
                                    shortcuts();
                            }
                        }
                        dialogsAdapter.clear();
                        dialogsAdapter.setItems(DialogData.getDialogs(Global.diaG));
                        dialogsAdapter.notifyDataSetChanged();

                        count = 0;
                        for (int j = 0; j < Global.diaG.size(); j++) {
                            count += Global.diaG.get(j).getNoOfUnread();
                        }
                        try {
                            if (count == 0)
                                meowBottomNavigation.setCount(1, "empty");
                            else if (count > 99)
                                meowBottomNavigation.setCount(1, String.valueOf(99));
                            else
                                meowBottomNavigation.setCount(1, String.valueOf(count));

                            if (Global.diaG.size() == 0) {
                                sora.setVisibility(View.VISIBLE);
                                dialogsList.setVisibility(View.GONE);
                            } else {
                                sora.setVisibility(View.GONE);
                                dialogsList.setVisibility(View.VISIBLE);

                            }
                        } catch (NullPointerException e) {

                        }


                    }

                    @Override
                    public void onChildMoved(DataSnapshot dataSnapshot, String s) {

                    }

                    @Override
                    public void onCancelled(DatabaseError databaseError) {

                    }
                });
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });


        mBlock.child(mAuth.getCurrentUser().

                getUid()).

                addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                        if (mAuth.getCurrentUser() != null)
                            dialogsAdapter.notifyDataSetChanged();
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError databaseError) {

                    }
                });

        mMute.child(mAuth.getCurrentUser().

                getUid()).

                addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                        if (mAuth.getCurrentUser() != null)
                            dialogsAdapter.notifyDataSetChanged();
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError databaseError) {

                    }
                });


    }


    @Override
    public void onDialogClick(DefaultDialog dialog) {
        //firebase init
        Intent intent = new Intent(mActivity, Chat.class);
        intent.putExtra("id", dialog.getId());
        intent.putExtra("name", dialog.getDialogName());
        intent.putExtra("ava", dialog.getDialogPhoto());
        intent.putExtra("phone", dialog.getDialogPhone());
        intent.putExtra("screen", dialog.isDialogscreen());
        Global.currphone = dialog.getDialogPhone();
        mActivity.startActivity(intent);
    }


    public void onSwipeRight  (DefaultDialog dialog) {
        idR = dialog.getId();
        if (Global.check_int(mActivity)) {
            ChatCelect cdd = new ChatCelect(mActivity, dialog.getId());
            cdd.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            cdd.getWindow().getAttributes().windowAnimations = R.style.CustomDialogAnimation;
            cdd.show();
        } else
            Toast.makeText(mActivity, R.string.check_int, Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onDialogLongClick(DefaultDialog dialog) {
        idR = dialog.getId();
        if (Global.check_int(mActivity)) {
            ChatCelect cdd = new ChatCelect(mActivity, dialog.getId());
            cdd.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            cdd.getWindow().getAttributes().windowAnimations = R.style.CustomDialogAnimation;
            cdd.show();
        } else
            Toast.makeText(mActivity, R.string.check_int, Toast.LENGTH_SHORT).show();
    }

    private void initAdapter() {

        dialogsAdapter = new DialogsListAdapter<>(R.layout.item_dialog_custom, DialogHolder.class, imageLoader);
        dialogsAdapter.setOnDialogClickListener(this);

        dialogsAdapter.setOnDialogLongClickListener(this);
        dialogsList.setAdapter(dialogsAdapter);
        dialogsAdapter.setDatesFormatter(new DateFormatter.Formatter() {
            @Override
            public String format(Date date) {
                if (DateFormatter.isToday(date)) {
                    DateFormat format = new SimpleDateFormat("hh:mm aa");
                    String timee = format.format(date);
                    return timee;
                } else if (DateFormatter.isYesterday(date)) {
                    return mActivity.getString(R.string.date_header_yesterday);
                } else {
                    return DateFormatter.format(date, DateFormatter.Template.STRING_DAY_MONTH_YEAR);
                }
            }
        });

        Global.globalChatsAdapter = dialogsAdapter;

    }


    public void updatedialog(Activity conn) {
        mAuth = FirebaseAuth.getInstance();

        ((AppBack) conn.getApplication()).getdialogdb(mAuth.getCurrentUser().getUid());

        if (dialogsAdapter.halbine(Global.diaG, Global.Dialogonelist.get(0).getId()) == -1)
            Global.diaG.add(Global.Dialogonelist.get(0));
        else
            Global.diaG.set(dialogsAdapter.halbine(Global.diaG, Global.Dialogonelist.get(0).getId()), Global.Dialogonelist.get(0));


        if (mAuth.getCurrentUser() != null) {
            if (isAdded()) {
                ((AppBack) mActivity.getApplication()).setdialogdb(mAuth.getCurrentUser().getUid());
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N_MR1)
                    shortcuts();
            }
        }
        dialogsAdapter.notifyDataSetChanged();


        if (mAuth.getCurrentUser() != null) {
            ((AppBack) conn.getApplication()).setdialogdb(mAuth.getCurrentUser().getUid());
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N_MR1)
                shortcuts();
        }

        //update the list
        dialogsAdapter.clear();
        dialogsAdapter.setItems(DialogData.getDialogs(Global.diaG));
        dialogsAdapter.notifyDataSetChanged();

        count = 0;
        for (int j = 0; j < Global.diaG.size(); j++) {
            count += Global.diaG.get(j).getNoOfUnread();
        }
        try {
            try {
                if (count == 0)
                    meowBottomNavigation.setCount(1, "empty");
                else if (count > 99)
                    meowBottomNavigation.setCount(1, String.valueOf(99));
                else
                    meowBottomNavigation.setCount(1, String.valueOf(count));
            } catch (NullPointerException e) {

            }

            if (Global.diaG.size() == 0) {
                sora.setVisibility(View.VISIBLE);
                dialogsList.setVisibility(View.GONE);
            } else {
                sora.setVisibility(View.GONE);
                dialogsList.setVisibility(View.VISIBLE);

            }
        } catch (NullPointerException e) {

        }


    }

    //for example
    private void onNewDialog(DefaultDialog dialog) {
        dialogsAdapter.addItem(dialog);
    }

    public static void refreshL() {
        dialogsAdapter.deleteById(idR);
    }


    public void getStories() {
        storyList = view.findViewById(R.id.storylist);
        progall = view.findViewById(R.id.progall);
        storyView = view.findViewById(R.id.storyView);
        avaStory = view.findViewById(R.id.avaStory);
        //arrays init
        otherS = new ArrayList<>();
        contactList = new ArrayList<>();
        userList = new ArrayList<>();
        searchL = new ArrayList<>();

        //enc
        //encryption
        byte[] iv = new byte[16];
        encryption = Encryption.getDefault(Global.keyE, Global.salt, iv);


        DisplayMetrics displaymetrics = new DisplayMetrics();
        mActivity.getWindowManager().getDefaultDisplay().getMetrics(displaymetrics);
        int imagewidth = (int) Math.round(displaymetrics.widthPixels * 0.1556);

        avaStory.getLayoutParams().width = imagewidth;
        avaStory.getLayoutParams().height = imagewidth;
//firebase


        if (Global.check_int(mActivity))
            progall.setVisibility(View.VISIBLE);
        else
            progall.setVisibility(View.GONE);


        if (!Global.avaLocal.isEmpty()) {
            if (Global.avaLocal.equals("no")) {
                // Picasso.get()
                //        .load(R.drawable.profile)
                //      .placeholder(R.drawable.placeholder_gray).error(R.drawable.errorimg)

                //    .into(avaStory);

                TextDrawable drawable2 = TextDrawable.builder().beginConfig().fontSize(45).bold().endConfig().buildRound(printInitials("name"), generator.getRandomColor());


                avaStory.setImageDrawable(drawable2);
            } else {
                Picasso.get()
                        .load(Global.avaLocal)
                        .placeholder(R.drawable.placeholder_gray).error(R.drawable.errorimg)

                        .into(avaStory);
            }
        }


        myData = FirebaseDatabase.getInstance().getReference(Global.USERS);
        mTime = FirebaseDatabase.getInstance().getReference(Global.TIME);
        mOtherData = FirebaseDatabase.getInstance().getReference(Global.USERS);
        mUserDB = FirebaseDatabase.getInstance().getReference().child(Global.USERS);


        //fetch my stories

        ((AppBack) mActivity.getApplication()).getMyStory();
        ((AppBack) mActivity.getApplication()).getStory();


        if (Global.StoryList.size() == 0)
            storyList.setVisibility(View.GONE);
        else
            storyList.setVisibility(View.VISIBLE);


        if (Global.myStoryList.size() == 0) {
            storyView.setVisibility(View.GONE);
            avaStory.setVisibility(View.VISIBLE);

        } else {
            storyView.setImageUris(Global.myStoryList, mActivity);
            storyView.calculateSweepAngle(Global.myStoryList.size(), getContext());
            storyView.setVisibility(View.VISIBLE);
            avaStory.setVisibility(View.GONE);

        }
        //////

        Query query = myData.child(mAuth.getCurrentUser().getUid()).child(Global.myStoryS);
        query.keepSynced(true);

        final long[] keyOnce = {0};
        final int[] i = {0};
        final int[] onceOnce = {0};

        query.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                keyOnce[0] = dataSnapshot.getChildrenCount();
                if (keyOnce[0] == 0) {
                    Global.myStoryList.clear();
                    ((AppBack) mActivity.getApplication()).setMyStory();
                    storyView.setVisibility(View.GONE);
                    avaStory.setVisibility(View.VISIBLE);


                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });

        Map<String, Object> map = new HashMap<>();
        map.put("time", ServerValue.TIMESTAMP);
        mTime.updateChildren(map).addOnSuccessListener(new OnSuccessListener<Void>() {
            @Override
            public void onSuccess(Void aVoid) {

                mTime.child("time").addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                        now = dataSnapshot.getValue(Long.class);
                        Global.myStoryList.clear();
                        getOtherStories();
                        query.addChildEventListener(new ChildEventListener() {
                            @Override
                            public void onChildAdded(@NonNull DataSnapshot dataSnapshot, @Nullable String s) {
                                StoryList post = dataSnapshot.getValue(StoryList.class);
                                int hours = (int) TimeUnit.MILLISECONDS.toHours(now - post.getTime());
                                if (hours < 24) {
                                    try {
                                        if (post.getLink() != null) {
                                            if (halbineAAA(Global.myStoryList, post.getId()) == -1)
                                                Global.myStoryList.add(new StoryModel(encryption.decryptOrNull(post.getLink()), mActivity.getString(R.string.me), AppBack.getTimeAgo(post.getTime(), mActivity), post.getTime(), post.getId()));
                                        }
                                    } catch (NullPointerException e) {
                                        storyView.setVisibility(View.GONE);
                                        avaStory.setVisibility(View.VISIBLE);

                                    }
                                }
                                //check only in global list range
                                if (i[0] >= keyOnce[0] - 1) {
                                    if (Global.check_int(mActivity)) {
                                        //local store
                                        ((AppBack) mActivity.getApplication()).setMyStory();
                                    }
                                    if (isAdded()) {

                                        if (Global.myStoryList.size() > 0) {
                                            storyView.setImageUris(Global.myStoryList, mActivity.getApplicationContext());
                                            storyView.calculateSweepAngle(Global.myStoryList.size(), getContext());
                                        }
                                    }
                                    if (Global.myStoryList.size() == 0) {
                                        storyView.setVisibility(View.GONE);
                                        avaStory.setVisibility(View.VISIBLE);

                                    } else {
                                        storyView.setVisibility(View.VISIBLE);
                                        avaStory.setVisibility(View.GONE);


                                    }

                                    if (onceOnce[0] == 0) {
                                        keyOnce[0]++;
                                    }


                                }

                                i[0]++;


                            }

                            @Override
                            public void onChildChanged(@NonNull DataSnapshot dataSnapshot, @Nullable String s) {

                            }

                            @Override
                            public void onChildRemoved(@NonNull DataSnapshot dataSnapshot) {
                                StoryList post = dataSnapshot.getValue(StoryList.class);
                                if (post.getLink() != null) {
                                    if (halbineAAA(Global.myStoryList, post.getId()) != -1)
                                        Global.myStoryList.remove(halbineAAA(Global.myStoryList, post.getId()));
                                }
                                ((AppBack) mActivity.getApplication()).setMyStory();
                                if (isAdded()) {

                                    if (Global.myStoryList.size() > 0) {
                                        storyView.setImageUris(Global.myStoryList, mActivity.getApplicationContext());
                                        storyView.calculateSweepAngle(Global.myStoryList.size(), getContext());

                                    }
                                }
                                if (Global.myStoryList.size() == 0) {
                                    storyView.setVisibility(View.GONE);
                                    avaStory.setVisibility(View.VISIBLE);
                                } else {
                                    storyView.setVisibility(View.VISIBLE);
                                    avaStory.setVisibility(View.GONE);
                                }


                            }

                            @Override
                            public void onChildMoved(@NonNull DataSnapshot dataSnapshot, @Nullable String s) {

                            }

                            @Override
                            public void onCancelled(@NonNull DatabaseError databaseError) {

                            }
                        });
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError databaseError) {

                    }
                });

            }
        });

        array = new ArrayList<>();

        storytemplist = new ArrayList<>(Global.StoryList);
        adapter = new StoryAdapter(storytemplist);
        storyList.setAdapter(adapter);
        storyList.setLayoutManager(new LinearLayoutManager(mActivity, RecyclerView.HORIZONTAL, false));
        Global.StoryList.clear();
        avaStory.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(mActivity, AddStory.class));

            }
        });
    }


    private void getOtherStories() {
        mUserDB.child(mAuth.getCurrentUser().getUid()).child(Global.StoryS).addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if (mAuth.getCurrentUser() != null) {

                    progall.setVisibility(View.GONE);
                    if (dataSnapshot.exists()) {
                        ((AppBack) mActivity.getApplication()).getBlock();
                        ((AppBack) mActivity.getApplication()).getMute();
                        for (DataSnapshot postSnapshot : dataSnapshot.getChildren()) {
                            mUserDB.child(mAuth.getCurrentUser().getUid()).child(Global.StoryS).child(postSnapshot.getKey()).addListenerForSingleValueEvent(new ValueEventListener() {
                                @Override
                                public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                                    for (DataSnapshot data : dataSnapshot.getChildren()) {
                                        try {
                                            StoryList post = data.getValue(StoryList.class);
                                            String[] ids = post.getId().split("_");
                                            idTemp = ids[0];

                                            if (!Global.blockList.contains(idTemp) && !Global.mutelist.contains(idTemp)) {

                                                name = post.getName();
                                                ava = post.getAva();
                                                int hours = (int) TimeUnit.MILLISECONDS.toHours(now - post.getTime());
                                                if (hours < 24) {
                                                    try {
                                                        if (post.getLink() != null) {

                                                            if (halbineAAA(otherS, post.getId()) == -1)
                                                                otherS.add(new StoryModel(encryption.decryptOrNull(post.getLink()), name, AppBack.getTimeAgo(post.getTime(), mActivity), post.getTime(), post.getId()));


                                                        }
                                                    } catch (NullPointerException e) {

                                                    }
                                                } else {
                                                    if (halbineAAA(otherS, post.getId()) != -1) {
                                                        adapter = new StoryAdapter(Global.StoryList);
                                                        otherS.remove(halbineAAA(otherS, post.getId()));
                                                        adapter.notifyDataSetChanged();
                                                        storyList.setAdapter(adapter);
                                                    }
                                                }


                                            }
                                        } catch (NullPointerException e) {

                                        }
                                    }
                                    if (!Global.blockList.contains(idTemp) && !Global.mutelist.contains(idTemp)) {
                                        setStory(new ArrayList<>(otherS), name, ava, idTemp);
                                        otherS.clear();
                                        name = "";
                                        ava = "";
                                        idTemp = "";
                                    }
                                }

                                @Override
                                public void onCancelled(@NonNull DatabaseError databaseError) {

                                }
                            });
                        }
                    } else {
                        Global.StoryList.clear();
                        ((AppBack) mActivity.getApplication()).setStory();
                        adapter = new StoryAdapter(Global.StoryList);
                        adapter.notifyItemRangeRemoved(0, Global.StoryList.size());
                        adapter.notifyDataSetChanged();
                        storyList.setAdapter(adapter);
                        storyList.setVisibility(View.GONE);


                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }


        });


    }


    private void setStory(ArrayList<StoryModel> otherSS, String name, String ava, String id) {
        storyList.setVisibility(View.VISIBLE);

        if (otherSS.size() > 0) {
            long lastTime = otherSS.get(otherS.size() - 1).timeL;

            if (biggerhalbine(Global.StoryList, id) == -1)
                Global.StoryList.add(new StoryListRetr(otherSS, name, ava, id, lastTime));

            else
                Global.StoryList.set(biggerhalbine(Global.StoryList, id), new StoryListRetr(otherSS, name, ava, id, lastTime));

        } else {
            if (biggerhalbine(Global.StoryList, id) != -1) {
                Global.StoryList.remove(biggerhalbine(Global.StoryList, id));
                adapter.notifyItemRangeRemoved(0, Global.StoryList.size());
            }
        }

        arrange();
        ((AppBack) mActivity.getApplication()).setStory();

        adapter = new StoryAdapter(Global.StoryList);
        adapter.notifyDataSetChanged();
        storyList.setAdapter(adapter);

    }

    private void arrange() {
        StoryListRetr temp;
        for (int i = 0; i < Global.StoryList.size(); i++) {
            if (i != Global.StoryList.size() - 1) {
                if (Global.StoryList.get(i).getLastTime() < Global.StoryList.get(i + 1).getLastTime()) {
                    temp = Global.StoryList.get(i);
                    Global.StoryList.set(i, Global.StoryList.get(i + 1));
                    Global.StoryList.set(i + 1, temp);
                    arrange();
                    break;
                }
            }

        }

        adapter.notifyDataSetChanged();

    }


    public int halbinedetails(ArrayList<UserDetailsStory> ml, String id) {
        int j = 0, i = 0;
        for (i = 0; i < ml.size(); i++) {
            if (ml.get(i).getId().equals(id)) {
                j = 1;
                break;
            }

        }
        if (j == 1)
            return i;
        else
            return -1;

    }

    public String halbinedetails2awy(ArrayList<UserDetailsStory> ml, String longid) {
        for (int i = 0; i < ml.size(); i++) {
            if (longid.contains(ml.get(i).getId())) {
                return ml.get(i).getId();
            }

        }

        return "no";


    }

    public int biggerhalbine(ArrayList<StoryListRetr> ml, String id) {
        int j = 0, i = 0;
        for (i = 0; i < ml.size(); i++) {
            if (ml.get(i).getUID().equals(id)) {
                j = 1;
                break;
            }

        }
        if (j == 1)
            return i;
        else
            return -1;

    }

    public int halbineAAA(ArrayList<StoryModel> ml, String id) {
        int j = 0, i = 0;
        for (i = 0; i < ml.size(); i++) {
            if (ml.get(i).getId().equals(id)) {
                j = 1;
                break;
            }

        }
        if (j == 1)
            return i;
        else
            return -1;

    }


    @Override
    public void onResume() {
        super.onResume();

        try {
            Global.currentfragment = mActivity;
            AppBack myApp = (AppBack) mActivity.getApplication();
            DatabaseReference muse = FirebaseDatabase.getInstance().getReference(Global.USERS);
            if (myApp.wasInBackground) {
                //init data
                Map<String, Object> map = new HashMap<>();
                map.put(Global.Online, true);
                if (mAuth.getCurrentUser() != null)
                    muse.child(mAuth.getCurrentUser().getUid()).updateChildren(map);
                Global.local_on = true;
                //lock screen
                ((AppBack) mActivity.getApplication()).lockscreen(((AppBack) mActivity.getApplication()).shared().getBoolean("lock", false));
            }
            myApp.stopActivityTransitionTimer();


        } catch (NullPointerException e) {

        }

        try {
            if (!Global.avaLocal.isEmpty() && avaStory != null) {
                if (Global.avaLocal.equals("no")) {
                    /*Picasso.get()
                            .load(R.drawable.profile)
                            .placeholder(R.drawable.placeholder_gray).error(R.drawable.errorimg)

                            .into(avaStory);*/
                    TextDrawable drawable2 = TextDrawable.builder().beginConfig().fontSize(45).bold().endConfig().buildRound(printInitials(Global.nameLocal), generator.getRandomColor());

                    avaStory.setImageDrawable(drawable2);
                } else {
                    Picasso.get()
                            .load(Global.avaLocal)
                            .placeholder(R.drawable.placeholder_gray).error(R.drawable.errorimg)

                            .into(avaStory);
                }
            }
        } catch (NullPointerException e) {

        }

    }

    @Override
    public void onPause() {
        super.onPause();
        Global.currentfragment = null;

    }

    List<ShortcutInfo> listS;

    @RequiresApi(api = Build.VERSION_CODES.N_MR1)
    private void shortcuts() {
        try {
            ((AppBack) mActivity.getApplication()).getdialogdb(mAuth.getCurrentUser().getUid());
            ((AppBack) mActivity.getApplication()).getdialogdbG(mAuth.getCurrentUser().getUid());
            listS = new ArrayList<>();
            ShortcutManager shortcutManager = mActivity.getSystemService(ShortcutManager.class);
            Intent intent = new Intent(mActivity, MainActivity.class);
            intent.putExtra("codetawgeh", 3);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            intent.setAction(Intent.ACTION_VIEW);
            ShortcutInfo dynamicShortcut1 = new ShortcutInfo.Builder(mActivity, "addstory")
                    .setShortLabel(getString(R.string.stories))
                    .setLongLabel(getString(R.string.stories))
                    .setIcon(Icon.createWithResource(mActivity, R.drawable.stories))
                    .setIntent(intent)
                    .setRank(3)
                    .build();
            if (Global.diaGGG.size() > 0) {
                Intent intentG = new Intent(mActivity, MainActivity.class);
                intentG.putExtra("name", Global.diaGGG.get(Global.diaGGG.size() - 1).getName());
                intentG.putExtra("id", Global.diaGGG.get(Global.diaGGG.size() - 1).getId());
                intentG.putExtra("ava", Global.diaGGG.get(Global.diaGGG.size() - 1).getAvatar());
                intentG.putExtra("codetawgeh", 2);
                intentG.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK);
                intentG.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                intentG.setAction(Intent.ACTION_VIEW);
                if(!Global.diaGGG.get(Global.diaGGG.size() - 1).getName().isEmpty()) {
                    ShortcutInfo dynamicShortcut2 = new ShortcutInfo.Builder(mActivity, "group")
                            .setShortLabel(Global.diaGGG.get(Global.diaGGG.size() - 1).getName())
                            .setLongLabel(Global.diaGGG.get(Global.diaGGG.size() - 1).getName())
                            .setIcon(Icon.createWithResource(mActivity, R.drawable.ic_group_b))
                            .setIntent(intentG)
                            .setRank(2)
                            .build();
                    listS.add(dynamicShortcut2);
                }
            }
            if (Global.diaG.size() > 0) {
                Intent intentU1 = new Intent(mActivity, MainActivity.class);
                intentU1.putExtra("name", Global.diaG.get(Global.diaG.size() - 1).getName());
                intentU1.putExtra("id", Global.diaG.get(Global.diaG.size() - 1).getId());
                intentU1.putExtra("ava", Global.diaG.get(Global.diaG.size() - 1).getAvatar());
                intentU1.putExtra("codetawgeh", 1);
                intentU1.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK);
                intentU1.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                intentU1.setAction(Intent.ACTION_VIEW);
                if(!Global.diaG.get(Global.diaG.size() - 1).getName().isEmpty()) {

                    ShortcutInfo dynamicShortcut3 = new ShortcutInfo.Builder(mActivity, "user1")
                            .setShortLabel(Global.diaG.get(Global.diaG.size() - 1).getName())
                            .setLongLabel(Global.diaG.get(Global.diaG.size() - 1).getName())
                            .setIcon(Icon.createWithResource(mActivity, R.drawable.ic_person_b))
                            .setIntent(intentU1)
                            .setRank(0)
                            .build();
                    listS.add(dynamicShortcut3);
                }

            }
            if (Global.diaG.size() > 1) {
                Intent intentU2 = new Intent(mActivity, MainActivity.class);
                intentU2.putExtra("name", Global.diaG.get(Global.diaG.size() - 2).getName());
                intentU2.putExtra("id", Global.diaG.get(Global.diaG.size() - 2).getId());
                intentU2.putExtra("ava", Global.diaG.get(Global.diaG.size() - 2).getAvatar());
                intentU2.putExtra("codetawgeh", 1);
                intentU2.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK);
                intentU2.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                intentU2.setAction(Intent.ACTION_VIEW);
                if(!Global.diaG.get(Global.diaG.size() - 2).getName().isEmpty()) {

                    ShortcutInfo dynamicShortcut4 = new ShortcutInfo.Builder(mActivity, "user2")
                            .setShortLabel(Global.diaG.get(Global.diaG.size() - 2).getName())
                            .setLongLabel(Global.diaG.get(Global.diaG.size() - 2).getName())
                            .setIcon(Icon.createWithResource(mActivity, R.drawable.ic_person_b))
                            .setIntent(intentU2)
                            .setRank(1)
                            .build();
                    listS.add(dynamicShortcut4);
                }

            }
            listS.add(dynamicShortcut1);
            shortcutManager.setDynamicShortcuts(listS);
        } catch (NullPointerException e) {

        }

    }

}