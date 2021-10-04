package com.example.firebasetest;
import android.app.Dialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class logIn extends AppCompatActivity {
    private MediaPlayer buttonsSound;
    private MediaPlayer loginSong;
    ImageButton stopButton;
    ImageView profileIcon;
    IconAdapter iconAdapter;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);
        ImageView title=findViewById(R.id.titleid);
        Animation animation = AnimationUtils.loadAnimation(getApplicationContext(),R.anim.fade_title);
        title.startAnimation(animation);




        profileIcon=findViewById(R.id.profileImage);
        loginSong= MediaPlayer.create(logIn.this,R.raw.msbackround);
        loginSong.setLooping(true);
        loginSong.start();

       /* setContentView(R.layout.activity_login);
        Resources res = getResources();
        String[] question1 = res.getStringArray(R.array.q1);
        //String te = res.getIdentifier("q1',")
        ArrayList<String[]> sendThisToDbForGame = new ArrayList();
        sendThisToDbForGame.add(question1);*/

        Button loginButton = findViewById(R.id.button);
        EditText userNameInput = findViewById(R.id.inputField);



        stopButton=findViewById(R.id.mute_btn);
        stopButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (loginSong.isPlaying()){
                    stopButton.setImageResource(R.drawable.mute2_24);
                    loginSong.pause();}
                else{
                    loginSong.start();
                    stopButton.setImageResource(R.drawable.volume_up_24);
                }
            }
        });
        profileIcon.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Dialog dialog = new Dialog(logIn.this);
                dialog.setContentView(R.layout.icon_dialog);
                dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));

                RecyclerView iconRecycler = dialog.findViewById(R.id.iconRecycler);
                iconRecycler.setHasFixedSize(true);
                LinearLayoutManager layoutManager = new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false);
                iconRecycler.setLayoutManager(layoutManager);
                iconAdapter = new IconAdapter(this, iconList);
                iconRecycler.setAdapter(iconAdapter);



            }
        });


        stopButton=findViewById(R.id.mute_btn);
        stopButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (loginSong.isPlaying()){
                    stopButton.setImageResource(R.drawable.mute2_24);
                    loginSong.pause();}
                else{
                    loginSong.start();
                    stopButton.setImageResource(R.drawable.volume_up_24);
                }
            }
        });


        profileIcon.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(logIn.this, ChooseIconActivity.class);
                startActivity(intent);
            };
        });


        SharedPreferences sp = getSharedPreferences("UserDetails", MODE_PRIVATE);
        if(sp.contains("login")) {
            userNameInput.setText(sp.getString("login",""));
        }

        loginButton.setOnClickListener(v -> {
            buttonsSound=MediaPlayer.create(this,R.raw.burronclickk);
            buttonsSound.start();
            // TODO: 2. add a logout function that clears stored login nickname.
            String userInput = userNameInput.getText().toString();
            if(!userInput.equals("")) {
                Intent menuScreen = new Intent(logIn.this, menu.class);
                menuScreen.putExtra("userName",userInput);
                startActivity(menuScreen);
            }
        });
    }


    @Override
    public void onResume() {
        super.onResume();
        loginSong.start();
    }

    @Override
    protected void onPause() {
        super.onPause();
        loginSong.pause();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        loginSong.stop();
    }
}