package com.example.firebasetest;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.util.Log;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

public class gameBase extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        String playerName = getIntent().getStringExtra("userName");
        String playerRole = getIntent().getStringExtra("role");
        String gameName = getIntent().getStringExtra("roomName");

        setContentView(R.layout.activity_game_base);
        FirebaseDatabase db = FirebaseDatabase.getInstance();
        DatabaseReference allGames = db.getReference().child("Games");
        DatabaseReference thisGame = db.getReference().child("Games").child(gameName);
        List<ChildEventListener> openChildEvents = new ArrayList<>();
        List<ValueEventListener> openValueEvents = new ArrayList<>();
        int defaultLife = 3;
        int defaultCoins = 0;
        Button leaveGame = findViewById(R.id.leave_game);
        TextView lifeDisplay = findViewById(R.id.lifeDisplayTv);
        TextView coinDisplay = findViewById(R.id.coinsDisplayTv);
        boolean fuckthis = true; // TODO: explain to eran !

        //////////////////////////////////////////////////////////////////////////
        // set up perks list view.
        //////////////////////////////////////////////////////////////////////////

        List<String> perks = new ArrayList<>();
        perks.add("freeze timer");
        perks.add("drop one wrong answer");
        perks.add("speed up the timer of other players");
        perks.add("one more life");
        perks.add("swap question");
        ArrayAdapter<String> perksToDisplayAdapter =
                new ArrayAdapter<String>(this,
                        android.R.layout.simple_list_item_1,
                        perks);

        ListView perksDisplay = findViewById(R.id.perks);
        perksDisplay.setAdapter(perksToDisplayAdapter);

        //////////////////////////////////////////////////////////////////////////
        // set up players list view.
        //////////////////////////////////////////////////////////////////////////

        List<String> players = new ArrayList<>();
        ArrayAdapter<String> namesToDisplayAdapter =
                new ArrayAdapter<String>(this,
                        android.R.layout.simple_list_item_1,
                        players);

        ListView playersDisplay = findViewById(R.id.players);
        playersDisplay.setAdapter(namesToDisplayAdapter);

        //////////////////////////////////////////////////////////////////////////
        // local DB event Listeners, both singleRun and Persistent.
        //////////////////////////////////////////////////////////////////////////

        // (all games in DB) -->
        // (this game in DB)->(this player)->(life).addListener(onChangesToThisPlayersLife) = new value
        ValueEventListener onChangesToThisPlayersLife = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if(snapshot.getValue() != null) {
                    Log.d("MyApp", "life changed to "+snapshot.getValue().toString());
                    lifeDisplay.setText(snapshot.getValue().toString());
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(gameBase.this, "Error!", Toast.LENGTH_LONG).show();
            }
        };

        // (all games in DB) -->
        // (this game in DB)->(this player)->(life).addListener(onChangesToThisPlayersLife) = new value
        ValueEventListener onChangesToThisPlayersCoins = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if(snapshot.getValue() != null) {
                    Log.d("MyApp", "coins changed to "+snapshot.getValue().toString());
                    coinDisplay.setText(snapshot.getValue().toString());
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(gameBase.this, "Error!", Toast.LENGTH_LONG).show();
            }
        };

        // (all games in DB) -->
        // (this game in DB)->(players in room).addListener(updatePlayersInGameQuery) = value of "players
        ValueEventListener updatePlayersInGameQuery = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                thisGame.child("players").setValue(Integer.parseInt(snapshot.getValue().toString())-1);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(gameBase.this, "Error!", Toast.LENGTH_LONG).show();
            }
        };

        // (all games in DB) -->
        // (this game in DB).addListener(guestNamesUpdate) = Children that had some event.
        ChildEventListener onPlayersEventInDb = new ChildEventListener() {

            @Override
            public void onChildAdded(@NonNull DataSnapshot snapshot, @Nullable String previousChildName) {
                if(snapshot.getKey().contains("guest") || snapshot.getKey().contains("host")) {
                    players.add(snapshot.child("name").getValue().toString());
                    namesToDisplayAdapter.notifyDataSetChanged();
                    thisGame.child(snapshot.getKey()).child("life").setValue(defaultLife);
                    thisGame.child(snapshot.getKey()).child("coins").setValue(defaultCoins);
                }
            }
            @Override
            public void onChildChanged(@NonNull DataSnapshot snapshot, @Nullable String previousChildName) {/*don't care right now*/}
            @Override
            public void onChildRemoved(@NonNull DataSnapshot snapshot) {
                if(snapshot.getKey().contains("guest")) {
                    players.remove(snapshot.getValue().toString());
                    namesToDisplayAdapter.notifyDataSetChanged();
                    thisGame.child("players").addListenerForSingleValueEvent(updatePlayersInGameQuery);
                }
            }
            @Override
            public void onChildMoved(@NonNull DataSnapshot snapshot, @Nullable String previousChildName) {/*don't care right now*/}
            @Override
            public void onCancelled(@NonNull DatabaseError error) {/*don't care right now*/}
        };

        //////////////////////////////////////////////////////////////////////////
        // set up update "player display" and db presence for each player in game
        //////////////////////////////////////////////////////////////////////////
        openChildEvents.add(thisGame.addChildEventListener(onPlayersEventInDb));

        //////////////////////////////////////////////////////////////////////////
        // set up update life and coin of this player
        //////////////////////////////////////////////////////////////////////////
        openValueEvents.add(thisGame.child(playerRole).child("life").addValueEventListener(onChangesToThisPlayersLife));
        openValueEvents.add(thisGame.child(playerRole).child("coins").addValueEventListener(onChangesToThisPlayersCoins));

        //////////////////////////////////////////////////////////////////////////
        // press on leave button
        //////////////////////////////////////////////////////////////////////////
        leaveGame.setOnClickListener(v-> {
            //TODO: room should closed when there are 0 players left
            //TODO: wining condition is all players only 1 player with >0 hearts
            //TODO: draw is when all players have 0 hearts
            //TODO: host leaves, guest becomes host
        });


    }
}