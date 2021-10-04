package com.example.firebasetest;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.Button;

import androidx.appcompat.app.AppCompatActivity;

public class menu extends AppCompatActivity {

    private String userName;
    private SharedPreferences sp;
    Button publicRoomBtn;
    Animation animationSlide;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_menu);
        publicRoomBtn = findViewById(R.id.public_rooms_btn);
        userName = getIntent().getStringExtra("userName");
        sp = getSharedPreferences("UserDetails", MODE_PRIVATE);
        animationSlide= AnimationUtils.loadAnimation(getApplicationContext(),R.anim.bounce);


        publicRoomBtn.startAnimation(animationSlide);
        publicRoomBtn.setOnClickListener(v -> {
            Intent publicRoomsScreen = new Intent(menu.this, publicRooms.class);
            publicRoomsScreen.putExtra("userName",userName);
            startActivity(publicRoomsScreen);
        });

        Button newRoomBtn = findViewById(R.id.new_room_btn);
        newRoomBtn.setOnClickListener(v -> {
            Intent newRoomScreen = new Intent(menu.this, newRoom.class);
            newRoomScreen.putExtra("userName",userName);
            startActivity(newRoomScreen);
        });
    }

    @Override
    protected void onPause() {
        super.onPause();
        SharedPreferences.Editor editor = sp.edit();
        editor.putString("login",userName);
        editor.apply();
    }
}

