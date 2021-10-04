package com.example.firebasetest;

import android.content.Intent;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;
// TODO design comment for clicking on button
// TODO design text
// TODO: if program crashes, there should be a deletion of open room.
// TODO: hide the "guest" part when no guest are in.
// TODO: fix "guests in room" tv, right now there is a bad allocation mechanism.
// TODO: i shouldn't be able to start a public game with 0 guests.
// TODO: close all open listeners upon room closure.

public class newRoom extends AppCompatActivity {

    private String roomName = "";
    private FirebaseDatabase dbRepresentation = FirebaseDatabase.getInstance();
    private DatabaseReference allRoomsInDb = dbRepresentation.getReference().child("Rooms");
    private DatabaseReference allGames = dbRepresentation.getReference().child("Games");
    private boolean openToPublic = false;
    private TextView minutesTimerTv;
    private TextView secondsTimerTv;
    List <ChildEventListener> openChildEvents;
    List <ValueEventListener> openValueEvents; private long startTime = 0;
    // i stole the next explanation text from stackoverflow.
    // stolen explanation --> runs without a timer by reposting this handler at the end of the runnable.
    private Handler timerHandler;
    private Handler dbTimerUpdateHandler;
    private EditText userInputEt;
    private TextInputLayout roomNameLayout;
    private Button makeRoomPublic;
    private Button startTheGame;
    private TextView roomNameDisplay;
    private Button closeRoomBtn;
    private HashMap<Integer, String> guestDisplayRegistry;

    private MediaPlayer buttonsSound;
    Animation slideRight;
    Animation fall;
    /*
    those are the needed explanations for room "standby timer" implementation:
        https://stackoverflow.com/questions/4597690/how-to-set-timer-in-android
        https://stackoverflow.com/questions/1877417/how-to-set-a-timer-in-android?noredirect=1&lq=1
        https://developer.android.com/reference/android/os/Handler
        */

    /////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    // update timer to display and DB by runnable
    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////


    // code that will be executed at some queue in some thread.
    private Runnable displayTimerInRoom = new Runnable() {
        @Override
        public void run() {
            long mills = System.currentTimeMillis() - startTime;
            int seconds = (int) (mills / 1000);
            int minutes = seconds / 60;
            seconds = seconds % 60;

            minutesTimerTv.setText(String.format("%d", minutes));
            secondsTimerTv.setText(String.format("%02d",seconds));

            // as far as I understand this:
            // each half a second, put this runnable in a queue of the thread for executing
            // so basically if left alive, it keeps going "forever".
            timerHandler.postDelayed(this, 500);
        }
    };

    private Runnable updateTimerInDb = new Runnable() {

        @Override
        public void run() {
            String Test = roomName;
            if(openToPublic) {
                allRoomsInDb.child(roomName).child("timer").setValue(minutesTimerTv.getText().toString());
                timerHandler.postDelayed(this, 500);
            }
        }
    };

    /////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    // on create
    /////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    TextView ownGameTitle;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
       ///////////////////////////////////////////////////////////////
        setContentView(R.layout.activity_new_room);
        /////////////////////////////////////////////////////////////
        ownGameTitle=findViewById(R.id.makeyourowngame);
        timerHandler = new Handler();
        dbTimerUpdateHandler = new Handler();
        dbRepresentation = FirebaseDatabase.getInstance();
        openChildEvents = new ArrayList<>();
        openValueEvents = new ArrayList<>();
        minutesTimerTv = findViewById(R.id.timer_minutes_tv);
        secondsTimerTv = findViewById(R.id.timer_seconds_tv);
        userInputEt = findViewById(R.id.nameInput);
        roomNameLayout = findViewById(R.id.nameInputLayer);
        makeRoomPublic = findViewById(R.id.make_public_btn);
        startTheGame = findViewById(R.id.start_the_game);
        roomNameDisplay = findViewById(R.id.roomNameTv);
        closeRoomBtn = findViewById(R.id.close_room_btn);
        guestDisplayRegistry = new HashMap<>();
        guestDisplayRegistry.put(R.id.guest_name_01,"");
        guestDisplayRegistry.put(R.id.guest_name_02,"");
        guestDisplayRegistry.put(R.id.guest_name_03,"");
        guestDisplayRegistry.put(R.id.guest_name_04,"");
        String userName = getIntent().getStringExtra("userName");
        AtomicInteger counter = new AtomicInteger(0);

        //////////////////////////////////////////////////////////////////////////
        // Local Listeners to Events in DB.
        //////////////////////////////////////////////////////////////////////////

        // from path to specific room, upon addition of a new child, display said child in this room
        ChildEventListener onGuestActivity = new ChildEventListener() {
            @Override
            public void onChildAdded(@NonNull DataSnapshot snapshot, @Nullable String previousChildName) {
                if((Objects.requireNonNull(snapshot.getKey()).contains("guest")) && (counter.get() < 4)) {
                    counter.getAndIncrement();
                    for (Map.Entry<Integer, String> tempEntry : guestDisplayRegistry.entrySet()) {
                        if (tempEntry.getValue().equals("")) {
                            DataSnapshot guest = snapshot.child("name");
                            String guestName = guest.getValue().toString();
                            tempEntry.setValue(guestName);
                            TextView tempView = findViewById(tempEntry.getKey());
                            tempView.setText(tempEntry.getValue());
                            break;
                        }
                    }
                }
            }

            @Override
            public void onChildChanged(@NonNull DataSnapshot snapshot, @Nullable String previousChildName) {/* don't care*/}

            @Override
            public void onChildRemoved(@NonNull DataSnapshot snapshot) {
                if(Objects.requireNonNull(snapshot.getKey()).contains("guest"))
                counter.decrementAndGet();
                for (Map.Entry<Integer, String> tempEntry : guestDisplayRegistry.entrySet()) {
                    if (tempEntry.getValue().equals(Objects.requireNonNull(snapshot.getValue()).toString())) {
                        tempEntry.setValue("");
                        TextView tempView = findViewById(tempEntry.getKey());
                        tempView.setText("");
                        break;
                    }
                }
            }

            @Override
            public void onChildMoved(@NonNull DataSnapshot snapshot, @Nullable String previousChildName) {/* don't care*/}

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(newRoom.this, "Error!", Toast.LENGTH_LONG).show();
            }
        };

        // from path to rooms in DB, query existing rooms, if possible make a new room, upon success open room to public.
        ValueEventListener onQueryOfRoomNameExistence  = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if(snapshot.hasChild(roomName)) {
                    roomNameLayout.setError("this name is taken - chose another name");
                } else {
                    HashMap <String, Object> newRoom = new HashMap<>();
                    Object hostNameObj = userName, standbyTimerObj = "0", amountOfPlayersObj = "1",
                            gameStartedFlagObj = false;
                    newRoom.put("host",null);
                    newRoom.put("timer",standbyTimerObj);
                    newRoom.put("players",amountOfPlayersObj);
                    newRoom.put("started",gameStartedFlagObj);
                    allRoomsInDb.child(roomName).updateChildren(newRoom).addOnSuccessListener( v -> {
                        allRoomsInDb.child(roomName).child("host").child("name").setValue(userName);
                        startTime = System.currentTimeMillis();
                        timerHandler.postDelayed(displayTimerInRoom, 0);
                        dbTimerUpdateHandler.postDelayed(updateTimerInDb, 0);
                        openToPublic = true;
                        roomNameDisplay.setText(roomName);
                        roomNameLayout.setVisibility(View.GONE);
                        makeRoomPublic.setVisibility(View.GONE);
                        openChildEvents.add(allRoomsInDb.child(roomName).addChildEventListener(onGuestActivity));
                    });
                }
               makeRoomPublic.setText("open room to public");
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(newRoom.this, "Error!", Toast.LENGTH_LONG).show();
            }
        };

        // from path to specific room, query all children, send results to the path of Games->(Current game), upon success switch screen
        ValueEventListener moveRoomToGames = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                allRoomsInDb.child(roomName).child("started").setValue(true).addOnSuccessListener(l-> {
                    allGames.child(roomName).setValue(snapshot.getValue()).addOnSuccessListener(v -> {
                        Intent menuScreen = new Intent(newRoom.this, gameBase.class);
                        menuScreen.putExtra("userName",userName);
                        menuScreen.putExtra("role","host");
                        menuScreen.putExtra("roomName",roomName);
                        startActivity(menuScreen);
                        detachRoom();
                    });
                });
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(newRoom.this, "Error!", Toast.LENGTH_LONG).show();
            }
        };

        /////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
        // screen element listeners.
        /////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
        slideRight= AnimationUtils.loadAnimation(getApplicationContext(),R.anim.bounce);
        fall= AnimationUtils.loadAnimation(getApplicationContext(),R.anim.falling);
        ownGameTitle.startAnimation(fall);
        makeRoomPublic.startAnimation(slideRight);
        closeRoomBtn.startAnimation(slideRight);
        startTheGame.startAnimation(slideRight);
        ///////////////////////////////////////////////////////////////////////////////////////////////////////////////////
        makeRoomPublic.setOnClickListener(click -> {
            roomNameLayout.setError(null);
            roomName = userInputEt.getText().toString();
            if(!roomName.equals("")) {
                makeRoomPublic.setText("checking room name");
                allRoomsInDb.addListenerForSingleValueEvent(onQueryOfRoomNameExistence);
                //////////////////////////////////////////////////////////////////////////
                buttonsSound=MediaPlayer.create(this,R.raw.burronclickk);
                buttonsSound.start();
                /////////////////////////////////////////////////////////////////////
            }
        });

        closeRoomBtn.setOnClickListener(click -> {
            if(openToPublic) {
                closeRoom();
            }
        });

        startTheGame.setOnClickListener(click -> {
            allRoomsInDb.child(roomName).addListenerForSingleValueEvent(moveRoomToGames);
        });
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        if(openToPublic) {
            openToPublic = false;
            timerHandler.removeCallbacks(displayTimerInRoom);
            dbTimerUpdateHandler.removeCallbacks(updateTimerInDb);
            for(ChildEventListener L : openChildEvents) {
                allRoomsInDb.removeEventListener(L);
            }
            for(ValueEventListener L : openValueEvents) {
                allRoomsInDb.removeEventListener(L);
            }
            allRoomsInDb.child(roomName).removeValue();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        timerHandler.removeCallbacks(displayTimerInRoom);
        dbTimerUpdateHandler.removeCallbacks(updateTimerInDb);
        for(ChildEventListener L : openChildEvents) {
            allRoomsInDb.removeEventListener(L);
        }
        for(ValueEventListener L : openValueEvents) {
            allRoomsInDb.removeEventListener(L);
        }
        allRoomsInDb.child(roomName).removeValue();
    }


    private void detachRoom () {
        openToPublic = false;
        timerHandler.removeCallbacks(displayTimerInRoom);
        dbTimerUpdateHandler.removeCallbacks(updateTimerInDb);
        minutesTimerTv.setText("0");
        secondsTimerTv.setText("00");
        roomNameLayout.setVisibility(View.VISIBLE);
        makeRoomPublic.setVisibility(View.VISIBLE);
        roomNameDisplay.setText("");
        userInputEt.setText("");
        for (Map.Entry<Integer, String> tempEntry : guestDisplayRegistry.entrySet()) {
            if (!tempEntry.getValue().equals("")) {
                tempEntry.setValue("");
                TextView tempView = findViewById(tempEntry.getKey());
                tempView.setText("");
            }
        }
        for(ChildEventListener L : openChildEvents) {
            allRoomsInDb.removeEventListener(L);
        }
        for(ValueEventListener L : openValueEvents) {
            allRoomsInDb.removeEventListener(L);
        }
    }

    private void closeRoom () {
        detachRoom();
        allRoomsInDb.child(roomName).removeValue();
    }
}