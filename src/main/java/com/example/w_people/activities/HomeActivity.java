package com.example.w_people.activities;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import androidx.appcompat.widget.SearchView;
import androidx.appcompat.widget.Toolbar;

import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.w_people.R;
import com.example.w_people.adapters.UserAdapter;
import com.example.w_people.models.Conversation;
import com.example.w_people.models.User;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class HomeActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private UserAdapter userAdapter;
    private final List<User> userList = new ArrayList<>();
    private final List<User> allUsers = new ArrayList<>();
    private final List<Conversation> conversationList = new ArrayList<>();
    private FirebaseFirestore firestore;
    private final List<User> conversationUsers = new ArrayList<>();
    private final List<User> usersWithConversationsOnly = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        firestore = FirebaseFirestore.getInstance();
        recyclerView = findViewById(R.id.recyclerViewUsers);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        userAdapter = new UserAdapter(this, userList);
        recyclerView.setAdapter(userAdapter);


        setupSearchView();

        // Only fetch all users for search purpose
        fetchAllUsersForSearchOnly();

        // Show only chat users on initial screen
        loadConversations();

        loadAllUsers();

    }
    private void fetchAllUsersForSearchOnly() {
        firestore.collection("users")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    allUsers.clear();
                    for (DocumentSnapshot doc : queryDocumentSnapshots.getDocuments()) {
                        User user = doc.toObject(User.class);
                        if (user != null) {
                            allUsers.add(user);
                        }
                    }
                })
                .addOnFailureListener(e -> Log.e("HomeActivity", "Failed to load all users for search", e));
    }


    private void loadAllUsers() {
        firestore.collection("users")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    allUsers.clear();
                    for (DocumentSnapshot doc : queryDocumentSnapshots.getDocuments()) {
                        User user = doc.toObject(User.class);
                        if (user != null) {
                            allUsers.add(user); // 🔍 Store for searching
                        }
                    }
                })
                .addOnFailureListener(e -> Log.e("HomeActivity", "Failed to load all users", e));
    }


    private void loadConversations() {
        String currentUserId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        conversationList.clear();
        Set<String> userIdsToLoad = new HashSet<>();

        firestore.collection("conversations")
                .whereEqualTo("senderId", currentUserId)
                .get()
                .addOnSuccessListener(senderSnapshots -> {
                    for (DocumentSnapshot doc : senderSnapshots.getDocuments()) {
                        Conversation conversation = doc.toObject(Conversation.class);
                        if (conversation != null) {
                            conversationList.add(conversation);
                            userIdsToLoad.add(conversation.getReceiverId());
                        }
                    }

                    firestore.collection("conversations")
                            .whereEqualTo("receiverId", currentUserId)
                            .get()
                            .addOnSuccessListener(receiverSnapshots -> {
                                for (DocumentSnapshot doc : receiverSnapshots.getDocuments()) {
                                    Conversation conversation = doc.toObject(Conversation.class);
                                    if (conversation != null) {
                                        conversationList.add(conversation);
                                        userIdsToLoad.add(conversation.getSenderId());
                                    }
                                }

                                // After loading all conversations, filter users who are part of conversations
                                loadUsersFromIds(userIdsToLoad);
                            })
                            .addOnFailureListener(e -> Log.e("HomeActivity", "Error loading as receiver", e));
                })
                .addOnFailureListener(e -> Log.e("HomeActivity", "Error loading as sender", e));
    }

    private void loadUsersFromIds(Set<String> userIds) {
        conversationUsers.clear();
        userList.clear();

        for (String uid : userIds) {
            firestore.collection("users").document(uid)
                    .get()
                    .addOnSuccessListener(doc -> {
                        User user = doc.toObject(User.class);
                        if (user != null) {
                            conversationUsers.add(user); // 🟢 Store for reset
                            userList.add(user);          // Display now
                            userAdapter.notifyDataSetChanged();
                        }
                    })
                    .addOnFailureListener(e -> Log.e("HomeActivity", "Error loading user " + uid, e));
        }
    }


    private void setupSearchView() {
        SearchView searchView = findViewById(R.id.searchView);

        // Listener for query submission
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                filterUsers(query);  // Call filter method with query
                return true;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                filterUsers(newText);  // Filter users dynamically on text change
                return true;
            }
        });

        // Listener for search view closing (reset user list)
        searchView.setOnCloseListener(() -> {
            searchView.clearFocus();
            searchView.setQuery("", false); // Clear text
            userList.clear();
            userList.addAll(usersWithConversationsOnly); // restore chat list
            userAdapter.notifyDataSetChanged();
            return false;
        });
    }

    private void filterUsers(String query) {
        if (query == null || query.trim().isEmpty()) {
            userList.clear();
            userList.addAll(conversationUsers); // Restore conversation list
        } else {
            String lowerQuery = query.toLowerCase();
            List<User> filteredList = new ArrayList<>();
            for (User user : allUsers) {
                if (user.getUsername() != null && user.getUsername().toLowerCase().contains(lowerQuery)) {
                    filteredList.add(user);
                }
            }
            userList.clear();
            userList.addAll(filteredList);
        }

        userAdapter.notifyDataSetChanged();
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.home_menu, menu);
        return true;
    }


    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.menu_settings) {
            startActivity(new Intent(HomeActivity.this, SettingsActivity.class));
            return true;
        }
        return super.onOptionsItemSelected(item);
    }


}
