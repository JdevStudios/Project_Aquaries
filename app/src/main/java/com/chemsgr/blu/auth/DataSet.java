package com.chemsgr.blu.auth;

import android.Manifest;
import android.content.Intent;
import android.content.pm.ShortcutManager;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.net.Uri;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import android.os.Build;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import com.google.android.gms.tasks.Continuation;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;
import com.karumi.dexter.Dexter;
import com.karumi.dexter.MultiplePermissionsReport;
import com.karumi.dexter.PermissionToken;
import com.karumi.dexter.listener.PermissionRequest;
import com.karumi.dexter.listener.multi.MultiplePermissionsListener;
import com.squareup.picasso.Picasso;
import com.theartofdev.edmodo.cropper.CropImage;
import com.theartofdev.edmodo.cropper.CropImageView;
import com.vanniktech.emoji.EmojiEditText;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.chemsgr.blu.MainActivity;
import com.chemsgr.blu.R;
import com.chemsgr.blu.global.AppBack;
import com.chemsgr.blu.global.Global;
import com.chemsgr.blu.lists.UserData;

import net.khirr.android.privacypolicy.PrivacyPolicyDialog;

import de.hdodenhof.circleimageview.CircleImageView;
import dmax.dialog.SpotsDialog;
import id.zelory.compressor.Compressor;

public class DataSet extends AppCompatActivity {

    //View
    EmojiEditText name, statue;
    CircleImageView avatar;
    Button next;
    PrivacyPolicyDialog dialogPriv;
    //Vars
    String nameS, statueS, avaS;
    //Uri imgLocalpath;

    //Firebase
    FirebaseAuth mAuth;
    DatabaseReference mData;
    //compress
    private Bitmap compressedImageFile;
    android.app.AlertDialog dialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_data_set);
        dialogPriv = new PrivacyPolicyDialog(this,
                "http://35.232.152.247/term",
                "http://35.232.152.247/priv");
        name = findViewById(R.id.nameE);
        statue = findViewById(R.id.statueE);
        avatar = findViewById(R.id.avatarSet);
        next = findViewById(R.id.nextS);
        //firebase init
        mAuth = FirebaseAuth.getInstance();
        mData = FirebaseDatabase.getInstance().getReference(Global.USERS);
        Global.currentactivity = this;
        //dark mode init
        if (mAuth.getCurrentUser() != null) {
            if (!((AppBack) getApplication()).shared().getBoolean("dark" + mAuth.getCurrentUser().getUid(), false)) {
                getDelegate().setLocalNightMode(AppCompatDelegate.MODE_NIGHT_NO);
            } else {
                getDelegate().setLocalNightMode(AppCompatDelegate.MODE_NIGHT_YES);
            }
        }
        //loader
        if (Global.DARKSTATE) {
            dialog = new SpotsDialog.Builder()
                    .setContext(this)
                    .setMessage(R.string.pleasW)
                    .setTheme(R.style.darkDialog)
                    .setCancelable(false)
                    .setCancelable(true)
                    .build();
        } else {
            dialog = new SpotsDialog.Builder()
                    .setContext(this)
                    .setMessage(R.string.pleasW)
                    .setCancelable(true)
                    .setCancelable(false)
                    .build();
        }
        dialog.show();







        /////////////////////////////
        mData.child(mAuth.getCurrentUser().getUid()).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                UserData userData = dataSnapshot.getValue(UserData.class);
                try{if (userData.getName() != null) {
                    Global.phoneLocal = userData.getPhone();
                    name.setText(userData.getName());
                    statue.setText(userData.getStatue());
                    avaS = userData.getAvatar();
                    if (avaS.equals("no")) {
                        Picasso.get()
                                .load(R.drawable.profile)
                                .placeholder(R.drawable.placeholder_gray) .error(R.drawable.errorimg)

                                .into(avatar);
                    } else {
                        Picasso.get()
                                .load(avaS)
                                .placeholder(R.drawable.placeholder_gray) .error(R.drawable.errorimg)

                                .into(avatar);
                    }

                    Intent intent = new Intent(DataSet.this, MainActivity.class);

                    dialogPriv.setOnClickListener(new PrivacyPolicyDialog.OnClickListener() {
                        @Override
                        public void onAccept(boolean isFirstTime) {
                            Log.e("MainActivity", "Policies accepted");
                            startActivity(intent);


                            Toast.makeText(DataSet.this, R.string.signin_succ, Toast.LENGTH_SHORT).show();
                            finish();
                        }
                        @RequiresApi(api = Build.VERSION_CODES.N_MR1)
                        public void disableshourtcuts()
                        {
                            List<String> idds = new ArrayList<>();
                            idds.add("addstory");
                            idds.add("group");
                            idds.add("user1");
                            idds.add("user2");
                            ShortcutManager shortcutManager2 = getSystemService(ShortcutManager.class);
                            shortcutManager2.disableShortcuts(idds);
                        }
                        @Override
                        public void onCancel() {
                            Map<String, Object> map = new HashMap<>();
                            map.put(Global.Online, false);
                            mData.child(mAuth.getCurrentUser().getUid()).updateChildren(map).addOnSuccessListener(new OnSuccessListener<Void>() {
                                @Override
                                public void onSuccess(Void aVoid) {
                                    Global.local_on = false;
                                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N_MR1)
                                        disableshourtcuts();

                                    mAuth.signOut();

                                    finish();
                                }
                            });
                            Log.e("MainActivity", "Policies not accepted");

                        }
                    });

                    dialogPriv.addPoliceLine("This application uses a unique user identifier for advertising purposes, it is shared with third-party companies.");
                    dialogPriv.addPoliceLine("This application sends error reports, installation and send it to a server of the Fabric.io company to analyze and process it.");
                    dialogPriv.addPoliceLine("This application requires internet access and must collect the following information: Installed applications and history of installed applications, Contacts, Phone Numbers, location from gps, ip address, unique installation id, token to send notifications, version of the application, time zone and information about the language of the device.");
                    dialogPriv.addPoliceLine("All details about the use of data are available in our Privacy Policies, as well as all Terms of Service links below.");

                    //  Customizing (Optional)
                    dialogPriv.setTitleTextColor(Color.parseColor("#222222"));
                    dialogPriv.setAcceptButtonColor(ContextCompat.getColor(DataSet.this, R.color.colorAccent));

                    //  Title
                    dialogPriv.setTitle("Terms of Service");

                    //  {terms}Terms of Service{/terms} is replaced by a link to your terms
                    //  {privacy}Privacy Policy{/privacy} is replaced by a link to your privacy policy
                    dialogPriv.setTermsOfServiceSubtitle("If you click on {accept}, you acknowledge that it makes the content present and all the content of our {terms}Terms of Service{/terms} and implies that you have read our {privacy}Privacy Policy{privacy}.");

                    //  Set Europe only
                    dialogPriv.setEuropeOnly(false);

                    dialogPriv.show();



                }}catch(IllegalArgumentException e){
                    dialog.dismiss();
                }
                dialog.dismiss();


            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });
        next.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                next.setEnabled(false);
                dialog.show();
                if (!TextUtils.isEmpty(name.getText().toString().trim())) {
                    nameS = name.getText().toString().trim();
                    statueS = statue.getText().toString();
                    if (avaS == null || TextUtils.isEmpty(avaS)) {
                        avaS = "no";
                    }

                    if (statueS == null || TextUtils.isEmpty(statueS))
                        statueS = Global.DEFAULT_STATUE;


                    if (avaS == null || !avaS.contains("file://")) {
                        statueS = statueS.trim();
                        Map<String, Object> map = new HashMap<>();
                        map.put("name", nameS);
                        map.put("statue", statueS);
                        map.put("avatar", avaS);
                        map.put("id", mAuth.getCurrentUser().getUid());

                        mData.child(mAuth.getCurrentUser().getUid()).updateChildren(map).addOnCompleteListener(new OnCompleteListener<Void>() {
                            @RequiresApi(api = Build.VERSION_CODES.N_MR1)
                            public void disableshourtcuts()
                            {
                                List<String> idds = new ArrayList<>();
                                idds.add("addstory");
                                idds.add("group");
                                idds.add("user1");
                                idds.add("user2");
                                ShortcutManager shortcutManager2 = getSystemService(ShortcutManager.class);
                                shortcutManager2.disableShortcuts(idds);
                            }
                            @Override
                            public void onComplete(@NonNull Task<Void> task) {
                                if (task.isSuccessful()) {
                                    Intent intent = new Intent(DataSet.this, MainActivity.class);

                                    dialogPriv.setOnClickListener(new PrivacyPolicyDialog.OnClickListener() {
                                        @Override
                                        public void onAccept(boolean isFirstTime) {
                                            Log.e("MainActivity", "Policies accepted");
                                            startActivity(intent);

                                            finish();
                                            Toast.makeText(DataSet.this, R.string.signup_succ, Toast.LENGTH_SHORT).show();
                                        }

                                        @Override
                                        public void onCancel() {
                                            Log.e("MainActivity", "Policies not accepted");
                                            Map<String, Object> map = new HashMap<>();
                                            map.put(Global.Online, false);
                                            mData.child(mAuth.getCurrentUser().getUid()).updateChildren(map).addOnSuccessListener(new OnSuccessListener<Void>() {
                                                @Override
                                                public void onSuccess(Void aVoid) {
                                                    Global.local_on = false;
                                                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N_MR1)
                                                        disableshourtcuts();

                                                    mAuth.signOut();

                                                    finish();
                                                }
                                            });
                                        }
                                    });

                                    dialogPriv.addPoliceLine("This application uses a unique user identifier for advertising purposes, it is shared with third-party companies.");
                                    dialogPriv.addPoliceLine("This application sends error reports, installation and send it to a server of the Fabric.io company to analyze and process it.");
                                    dialogPriv.addPoliceLine("This application requires internet access and must collect the following information: Installed applications and history of installed applications, Contacts, Phone Numbers, location from gps, ip address, unique installation id, token to send notifications, version of the application, time zone and information about the language of the device.");
                                    dialogPriv.addPoliceLine("All details about the use of data are available in our Privacy Policies, as well as all Terms of Service links below.");

                                    //  Customizing (Optional)
                                    dialogPriv.setTitleTextColor(Color.parseColor("#222222"));
                                    dialogPriv.setAcceptButtonColor(ContextCompat.getColor(DataSet.this, R.color.colorAccent));

                                    //  Title
                                    dialogPriv.setTitle("Terms of Service");

                                    //  {terms}Terms of Service{/terms} is replaced by a link to your terms
                                    //  {privacy}Privacy Policy{/privacy} is replaced by a link to your privacy policy
                                    dialogPriv.setTermsOfServiceSubtitle("If you click on {accept}, you acknowledge that it makes the content present and all the content of our {terms}Terms of Service{/terms} and implies that you have read our {privacy}Privacy Policy{privacy}.");

                                    //  Set Europe only
                                    dialogPriv.setEuropeOnly(false);

                                    dialogPriv.show();


                                } else
                                    Toast.makeText(DataSet.this, R.string.error, Toast.LENGTH_SHORT).show();
                            }
                        });

                    } else {
                        //compress the photo
                        File newImageFile = new File(Uri.parse(avaS).getPath());
                        try {
                            compressedImageFile = new Compressor(DataSet.this)
                                    .setMaxHeight(500)
                                    .setMaxWidth(500)
                                    .setQuality(50)
                                    .compressToBitmap(newImageFile);

                        } catch (IOException e) {
                            e.printStackTrace();
                        }

                        ByteArrayOutputStream baos = new ByteArrayOutputStream();
                        compressedImageFile.compress(Bitmap.CompressFormat.JPEG, 100, baos);
                        byte[] thumbData = baos.toByteArray();
                        ////
                        StorageReference mStorageRef = FirebaseStorage.getInstance().getReference();
                        StorageReference riversRef = mStorageRef.child(Global.AvatarS + "/Ava_" + mAuth.getCurrentUser().getUid() + ".jpg");
                        UploadTask uploadTask = riversRef.putBytes(thumbData);


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
                                    Map<String, Object> map = new HashMap<>();
                                    map.put("name", nameS);
                                    statueS = statueS.trim();
                                    map.put("statue", statueS);
                                    map.put("avatar", String.valueOf(downloadUrl));
                                    map.put("id", mAuth.getCurrentUser().getUid());
                                    mData.child(mAuth.getCurrentUser().getUid()).updateChildren(map).addOnCompleteListener(new OnCompleteListener<Void>() {
                                        @Override
                                        public void onComplete(@NonNull Task<Void> task) {
                                            if (task.isSuccessful()) {
                                                /////////////////////////////





                                              ////
                                                Intent intent = new Intent(DataSet.this, MainActivity.class);

                                                dialogPriv.setOnClickListener(new PrivacyPolicyDialog.OnClickListener() {
                                                    @Override
                                                    public void onAccept(boolean isFirstTime) {
                                                        Log.e("MainActivity", "Policies accepted");

                                                        Toast.makeText(DataSet.this, R.string.signup_succ, Toast.LENGTH_SHORT).show();
                                                        startActivity(intent);
                                                        finish();
                                                    }

                                                    @Override
                                                    public void onCancel() {
                                                        Log.e("MainActivity", "Policies not accepted");
                                                        finish();
                                                    }
                                                });

                                                dialogPriv.addPoliceLine("This application uses a unique user identifier for advertising purposes, it is shared with third-party companies.");
                                                dialogPriv.addPoliceLine("This application sends error reports, installation and send it to a server of the Fabric.io company to analyze and process it.");
                                                dialogPriv.addPoliceLine("This application requires internet access and must collect the following information: Installed applications and history of installed applications, ip address, unique installation id, token to send notifications, version of the application, time zone and information about the language of the device.");
                                                dialogPriv.addPoliceLine("All details about the use of data are available in our Privacy Policies, as well as all Terms of Service links below.");

                                                //  Customizing (Optional)
                                                dialogPriv.setTitleTextColor(Color.parseColor("#222222"));
                                                dialogPriv.setAcceptButtonColor(ContextCompat.getColor(DataSet.this, R.color.colorAccent));

                                                //  Title
                                                dialog.setTitle("Terms of Service");

                                                //  {terms}Terms of Service{/terms} is replaced by a link to your terms
                                                //  {privacy}Privacy Policy{/privacy} is replaced by a link to your privacy policy
                                                dialogPriv.setTermsOfServiceSubtitle("If you click on {accept}, you acknowledge that it makes the content present and all the content of our {terms}Terms of Service{/terms} and implies that you have read our {privacy}Privacy Policy{privacy}.");

                                                //  Set Europe only
                                                dialogPriv.setEuropeOnly(false);

                                                dialogPriv.show();

                                                  } else
                                                Toast.makeText(DataSet.this, R.string.error, Toast.LENGTH_SHORT).show();
                                        }
                                    });

                                } else {

                                }
                            }
                        });
                    }

                } else {
                    next.setEnabled(true);
                    Toast.makeText(DataSet.this, R.string.plz_name, Toast.LENGTH_SHORT).show();
                }

            }
        });
    }

    public void changeprofile(View view) {

        Dexter.withActivity(DataSet.this)
                .withPermissions(Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.CAMERA)
                .withListener(new MultiplePermissionsListener() {
                    @Override
                    public void onPermissionsChecked(MultiplePermissionsReport report) {

                        if (report.areAllPermissionsGranted()) {
                            CropImage.activity()
                                    .setGuidelines(CropImageView.Guidelines.ON)
                                    .setMinCropResultSize(400, 400)
                                    .setAspectRatio(1, 1)
                                    .start(DataSet.this);
                        } else
                            Toast.makeText(DataSet.this, getString(R.string.acc_per), Toast.LENGTH_SHORT).show();


                    }

                    @Override
                    public void onPermissionRationaleShouldBeShown(List<PermissionRequest> permissions, PermissionToken token) {

                        token.continuePermissionRequest();

                    }
                }).check();
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == CropImage.CROP_IMAGE_ACTIVITY_REQUEST_CODE) {
            CropImage.ActivityResult result = CropImage.getActivityResult(data);
            if (resultCode == RESULT_OK) {
                avaS = String.valueOf(result.getUri());
                Picasso.get()
                        .load(avaS)
                        .placeholder(R.drawable.placeholder_gray).error(R.drawable.errorimg)

                        .into(avatar);
            } else if (resultCode == CropImage.CROP_IMAGE_ACTIVITY_RESULT_ERROR_CODE) {
                Exception error = result.getError();
            }
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        Global.currentactivity = this;
    }


}

