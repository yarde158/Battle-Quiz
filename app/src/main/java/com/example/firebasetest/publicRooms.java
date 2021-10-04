package com.example.firebasetest;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.database.annotations.NotNull;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class publicRooms extends AppCompatActivity {

    // TODO: exit from screen by backspace (or even destruction) should remove guest from DB and decrease "player amount".
    // TODO: when host closes room before game start - guest should get "yo dwag, host killed da room" message.
    // TODO: read and implement "Transaction serializability and isolation", right now resource can be overwhelmed by concurrent increments or write attempts.
    // ^ https://firebase.google.com/docs/database/android/read-and-write#save_data_as_transactions .
    // ^ https://firebase.google.com/docs/firestore/transaction-data-contention .
    // ^ https://firebase.google.com/docs/firestore/manage-data/transactions.
    // TODO: if game started - no one should be able to join.

    private boolean hasJoined = false;
    private boolean isReady = false;
    private final List<int[]> layout = new ArrayList<>();
    private final int hardLimitOfPlayersInRoom = 5;
    private ValueEventListener dbQueryOfRoomsByTimer;
    private ValueEventListener dbQueryOfRoomsByFilter;
    private List <ValueEventListener> openEnds;
    private String userName = "";
    private String nameOfRoomWithFocus = "";
    private String nameOfNewPlayerKeyInRoomWithFocus = "";

    /////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    // class that encapsulates the logic that needed to join this player to an existing room.
    /////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    private class clickOnJoin implements View.OnClickListener {

        private final DatabaseReference specificRoomInDb;
        private final DatabaseReference allRoomsInDb;
        private final String guestName;
        private final int roomLayoutId;
        private final int readyButtonId;
        private boolean displayJoinOption;
        private String nameOfNewGuestKey;

        /**
         * a Constructor of class that contains a Listener for the "player clicked on join room button" event.
         * @param dbRef a DatabaseReference to all rooms in the DB.
         * @param guestName guestName String, the name of the guest.
         * @param roomName guestName String, the name of the room that we want to join/
         * @param roomLayoutId int id that represents the layout that contains room info.
         * @param readyButtonId int id that represents the "i am ready" checkbox inside the "room layout".
         */
        public clickOnJoin (@NotNull DatabaseReference dbRef, @NotNull String guestName, String roomName,
                            @NotNull int roomLayoutId,@NotNull int readyButtonId) {
            this.specificRoomInDb = dbRef.child(roomName);
            this.allRoomsInDb = dbRef;
            this.guestName = guestName;
            this.readyButtonId = readyButtonId;
            this.roomLayoutId = roomLayoutId;
            this.displayJoinOption = true;
        }

        @Override
        public void onClick(View v) {
            Button sayReadyBtn = findViewById(readyButtonId);

            ValueEventListener onAddAnewPlayerQuery = new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    String currentGuestNumber = snapshot.getValue().toString();
                    if(currentGuestNumber.matches( "[0-9]+")) {
                        int i = Integer.parseInt(currentGuestNumber);
                        if(hardLimitOfPlayersInRoom > i) {
                            nameOfNewGuestKey = "guest"+i;
                            nameOfNewPlayerKeyInRoomWithFocus = nameOfNewGuestKey;
                            specificRoomInDb.child(nameOfNewGuestKey).setValue(null);
                            specificRoomInDb.child(nameOfNewGuestKey).child("name").setValue(guestName);
                            specificRoomInDb.child(nameOfNewGuestKey).child("ready").setValue(false);
                            i++;
                            specificRoomInDb.child("players").setValue(i);
                            hasJoined = true;
                            displayJoinOption = false;
                            hideRoomLayouts(roomLayoutId); // function call.
                            sayReadyBtn.setVisibility(View.VISIBLE);
                            ((Button)v).setText("Leave");
                            nameOfRoomWithFocus = specificRoomInDb.getKey();
                        }
                        else {
                            Toast.makeText(publicRooms.this, "Room is already full", Toast.LENGTH_LONG).show();
                        }
                    }
                }
                @Override
                public void onCancelled(@NonNull DatabaseError error) {
                    Toast.makeText(publicRooms.this, "Error!", Toast.LENGTH_LONG).show();
                }
            };

            //////////////////////////////////////////////////////////////////////////
            // Local Listeners to Events in DB.
            //////////////////////////////////////////////////////////////////////////

            ValueEventListener onRemoveExistingPlayerQuery = new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    String currentGuestNumber = snapshot.getValue().toString();
                    if(currentGuestNumber.matches( "[0-9]+")) {
                        int i = Integer.parseInt(currentGuestNumber);
                        if(i > 0) i--;
                        nameOfNewGuestKey = "";
                        specificRoomInDb.child("players").setValue(i);
                        hasJoined = false;
                        displayJoinOption = true;
                        allRoomsInDb.orderByChild("timer").limitToFirst(5).addListenerForSingleValueEvent(dbQueryOfRoomsByTimer);
                        ((Button)v).setText("Join");
                    }
                }
                @Override
                public void onCancelled(@NonNull DatabaseError error) {
                    Toast.makeText(publicRooms.this, "Error!", Toast.LENGTH_LONG).show();
                }
            };

            ChildEventListener roomMaybeClosedListener = new ChildEventListener() {
                @Override
                public void onChildAdded(@NonNull  DataSnapshot snapshot, @Nullable String previousChildName) {/* don't care*/}
                @Override
                public void onChildChanged(@NonNull DataSnapshot snapshot, @Nullable String previousChildName) {/* don't care*/}
                @Override
                public void onChildRemoved(@NonNull DataSnapshot snapshot) {
                    hasJoined = false;
                    displayJoinOption = true;
                    isReady = false;
                    ((Button)v).setText("Join");
                    allRoomsInDb.orderByChild("timer").limitToFirst(5).addListenerForSingleValueEvent(dbQueryOfRoomsByTimer);
                    specificRoomInDb.removeEventListener(onAddAnewPlayerQuery);
                    allRoomsInDb.removeEventListener(this);
                }
                @Override
                public void onChildMoved(@NonNull DataSnapshot snapshot, @Nullable String previousChildName) {/* don't care*/}
                @Override
                public void onCancelled(@NonNull DatabaseError error) {
                    Toast.makeText(publicRooms.this, "Error!", Toast.LENGTH_LONG).show();
                }
            };

            // (all rooms in DB) -->
            // (this room in DB)->(isGameStarted).addListener(uponGameHasStarted) = value of "isGameStarted"
            ValueEventListener uponGameHasStarted = new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    Object unboxing = snapshot.getValue();
                    if(unboxing != null) {
                        Boolean hasStarted = (Boolean) unboxing;
                        if(hasStarted) {
                            Intent menuScreen = new Intent(publicRooms.this, gameBase.class);
                            menuScreen.putExtra("userName",userName);
                            menuScreen.putExtra("role",nameOfNewPlayerKeyInRoomWithFocus);
                            menuScreen.putExtra("roomName",nameOfRoomWithFocus);
                            startActivity(menuScreen);
                        }
                    }
                }

                @Override
                public void onCancelled(@NonNull DatabaseError error) {

                }
            };

            //////////////////////////////////////////////////////////////////////////
            // Press on Join-Button Logic.
            //////////////////////////////////////////////////////////////////////////

            if(displayJoinOption) {
                specificRoomInDb.child("players").addListenerForSingleValueEvent(onAddAnewPlayerQuery);
                allRoomsInDb.addChildEventListener(roomMaybeClosedListener);
                specificRoomInDb.child("started").addValueEventListener(uponGameHasStarted);
            } else {
                ((Button)v).setText("Leave");
                specificRoomInDb.child(nameOfNewGuestKey).removeValue();
                specificRoomInDb.removeEventListener(uponGameHasStarted);
                specificRoomInDb.child("players").addListenerForSingleValueEvent(onRemoveExistingPlayerQuery);
            }
        }
    }

    /////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    // widely used functions
    /////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    private void resetRoomsInfo (int s, int e) {
        if(layout.isEmpty()) return;
        TextView waitForRoomsToLoadMassage = findViewById(R.id.loading_screen);
        waitForRoomsToLoadMassage.setVisibility(View.GONE);
        LinearLayout searchLayout = findViewById(R.id.search_bar_layout);
        searchLayout.setVisibility(View.VISIBLE);
        EditText inputFilterEt = findViewById(R.id.search_bar_et);
        //inputFilterEt.setText("");
        for(int [] i : layout) {
            LinearLayout tempLayout = findViewById(i[0]);
            TextView tempRoomName = findViewById(i[1]);
            Button tempJoin = findViewById(i[2]);
            Button tempReady = findViewById(i[3]);

            tempRoomName.setText("");
            tempJoin.setVisibility(View.VISIBLE);
            tempReady.setText("Ready?");
            tempReady.setVisibility(View.INVISIBLE);
            //tempLayout.setVisibility(View.GONE);
        }
        //EditText waitForRoomsToLoadMassage = findViewById(R.id.loading_screen);
        //waitForRoomsToLoadMassage.setVisibility(View.VISIBLE);
    }

    private void displaySnapshot (@NotNull DataSnapshot snapshot, @NotNull DatabaseReference dbPathToaAllRooms, @NotNull String userName) {
        resetRoomsInfo(0,4);
        Iterable<DataSnapshot> allRooms = snapshot.getChildren();
        Iterator roomIterator = allRooms.iterator();

        for(int i = 0; i < 5; i++) {

            LinearLayout tempLayout = findViewById(layout.get(i)[0]);
            TextView tempRoomName = findViewById(layout.get(i)[1]);
            TextView tempJoin = findViewById(layout.get(i)[2]);
            Button tempReady = findViewById(layout.get(i)[3]);

            if (roomIterator.hasNext()) {
                tempLayout.setVisibility(View.VISIBLE);
                DataSnapshot room = (DataSnapshot) roomIterator.next();
                tempRoomName.setText(room.getKey());
                tempJoin.setVisibility(View.VISIBLE);
                clickOnJoin tempClick = new clickOnJoin(dbPathToaAllRooms, userName, room.getKey(), layout.get(i)[0], layout.get(i)[3]);
                tempJoin.setOnClickListener(tempClick);
                tempReady.setOnClickListener(v -> {
                    if(!isReady) {
                        isReady = true;
                        Log.d("MyApp", "inside click_on_ready_listener clicked once, ready ? "+ true);
                        dbPathToaAllRooms.child(room.getKey()).child(tempClick.nameOfNewGuestKey).child("ready").setValue(isReady);
                        ((Button)v).setText("Not Ready?");
                    } else {
                        isReady = false;
                        Log.d("MyApp", "inside click_on_ready_listener clicked once, ready ? "+ false);
                        dbPathToaAllRooms.child(room.getKey()).child(nameOfNewPlayerKeyInRoomWithFocus).child("ready").setValue(isReady);
                        ((Button)v).setText("Ready?");
                    }
                });
            } else {
                tempLayout.setVisibility(View.GONE);
            }
        }
    }

    /**
     * hides Layouts that represent rooms from display,
     * has the option to exclude single Layer from hiding
     * @param excludeThisLayout should be null if no room should be excluded,
     *                    OR an int that represent the ID of the layer that should be excluded.
     */
    private void hideRoomLayouts (Integer excludeThisLayout) {
        if(layout.isEmpty()) return;
        int roomId = 0;
        if (excludeThisLayout != null) roomId = excludeThisLayout;
        for(int i = 0; i < layout.size(); i++) {
            if(roomId == findViewById(layout.get(i)[0]).getId()) continue;
            LinearLayout tempLayout = findViewById(layout.get(i)[0]);
            tempLayout.setVisibility(View.GONE);
        }
    }

    /////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    // on create
    /////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_public_rooms);



        FirebaseDatabase dbRepresentation = FirebaseDatabase.getInstance();
        DatabaseReference dbQuery = dbRepresentation.getReference("Rooms");
        DatabaseReference dbChanges = dbRepresentation.getReference("Rooms");
        userName = getIntent().getStringExtra("userName");
        openEnds = new ArrayList<>();

        layout.add(new int[]{R.id.room1_layout, R.id.room1_name, R.id.join_room1_btn, R.id.ready_room1_btn});
        layout.add(new int[]{R.id.room2_layout, R.id.room2_name, R.id.join_room2_btn, R.id.ready_room2_btn});
        layout.add(new int[]{R.id.room3_layout, R.id.room3_name, R.id.join_room3_btn, R.id.ready_room3_btn});
        layout.add(new int[]{R.id.room4_layout, R.id.room4_name, R.id.join_room4_btn, R.id.ready_room4_btn});
        layout.add(new int[]{R.id.room5_layout, R.id.room5_name, R.id.join_room5_btn, R.id.ready_room5_btn});

        //////////////////////////////////////////////////////////////////////////
        // db listeners set up
        //////////////////////////////////////////////////////////////////////////

        dbQueryOfRoomsByTimer = new ValueEventListener() {
            @RequiresApi(api = Build.VERSION_CODES.N)
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if(!hasJoined) {
                    displaySnapshot(snapshot, dbChanges, userName);
                }
            }
            // if shit hits the fan ...
            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(publicRooms.this, "Error!", Toast.LENGTH_LONG).show();
            }
        };

        dbQueryOfRoomsByFilter = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                displaySnapshot(snapshot, dbChanges, userName);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(publicRooms.this, "Error!", Toast.LENGTH_LONG).show();
            }
        };

        //////////////////////////////////////////////////////////////////////////
        // startup this screen display
        //////////////////////////////////////////////////////////////////////////

        openEnds.add(dbQuery.orderByChild("timer").limitToFirst(5).addValueEventListener(dbQueryOfRoomsByTimer));
        EditText filterInputEt = findViewById(R.id.search_bar_et);

        //////////////////////////////////////////////////////////////////////////
        // filtration mechanism
        //////////////////////////////////////////////////////////////////////////
        TextWatcher filterInputEtEventMonitor = new TextWatcher() {

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                Log.d("MyApp","beforeTextChanged");
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                Log.d("MyApp","onTextChanged");
            }

            @Override
            public void afterTextChanged(Editable s) {
                if(!filterInputEt.getText().toString().equals("")){
                    for(ValueEventListener L : openEnds) {
                        dbQuery.removeEventListener(L);
                    }
                    openEnds.clear();
                    openEnds.add(dbQuery.orderByKey().startAt(filterInputEt.getText().toString())
                            .endAt(filterInputEt.getText().toString()+"\uf8ff")
                            .limitToFirst(5).addValueEventListener(dbQueryOfRoomsByFilter));
                }
                else {
                    for(ValueEventListener L : openEnds) {
                        dbQuery.removeEventListener(L);
                    }
                    openEnds.clear();
                    openEnds.add(dbQuery.orderByChild("timer").limitToFirst(5).addValueEventListener(dbQueryOfRoomsByTimer));
                }
            }
        };
        filterInputEt.addTextChangedListener(filterInputEtEventMonitor);
    }
}
