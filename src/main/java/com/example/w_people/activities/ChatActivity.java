package com.example.w_people.activities;

import android.os.Bundle;
import android.util.Log;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;
import com.google.firebase.firestore.FirebaseFirestore;
import java.util.HashMap;
import java.util.Map;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.w_people.R;
import com.example.w_people.adapters.MessageAdapter;
import com.example.w_people.models.Message;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.firestore.DocumentSnapshot;

import java.util.ArrayList;
import java.util.List;

public class ChatActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private EditText messageEditText;
    private ImageView sendButton;
    private TextView chatWithUsernameTextView;
    private SwipeRefreshLayout swipeRefreshLayout; // Declare swipeRefreshLayout

    private DatabaseReference databaseReference;
    private List<Message> messageList;
    private MessageAdapter messageAdapter;

    private String senderId;
    private String receiverId;
    private String receiverUsername;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);

        // Initialize UI elements
        recyclerView = findViewById(R.id.recyclerViewMessages);
        messageEditText = findViewById(R.id.messageEditText);
        sendButton = findViewById(R.id.sendButton);
        chatWithUsernameTextView = findViewById(R.id.chatWithUsername);
        swipeRefreshLayout = findViewById(R.id.swipeRefreshLayout);  // Initialize swipeRefreshLayout

        messageList = new ArrayList<>();
        messageAdapter = new MessageAdapter(this, messageList);

        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(messageAdapter);

        // Get username from intent
        receiverUsername = getIntent().getStringExtra("receiverUsername");
        chatWithUsernameTextView.setText("Chatting with " + receiverUsername);

        if (receiverUsername == null) {
            Toast.makeText(this, "Receiver username is missing!", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        // Get current Firebase user
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser != null) {
            senderId = currentUser.getUid();
        } else {
            Toast.makeText(this, "User not logged in!", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        // Fetch receiverId from Firestore
        fetchReceiverIdFromFirestore();

        // Swipe-to-refresh listener
        swipeRefreshLayout.setOnRefreshListener(() -> {
            loadMessages();
            swipeRefreshLayout.setRefreshing(false);
        });
    }

    private void fetchReceiverIdFromFirestore() {
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        db.collection("users")
                .whereEqualTo("username", receiverUsername)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    if (!queryDocumentSnapshots.isEmpty()) {
                        DocumentSnapshot document = queryDocumentSnapshots.getDocuments().get(0);
                        receiverId = document.getId(); // or document.getString("uid") if you store it

                        // Proceed to start chat
                        startChat();
                    } else {
                        Toast.makeText(ChatActivity.this, "User not found!", Toast.LENGTH_SHORT).show();
                        finish();
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(ChatActivity.this, "Failed to fetch user: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    finish();
                });
    }

    private void startChat() {
        databaseReference = FirebaseDatabase.getInstance().getReference("Messages");

        loadMessages();

        sendButton.setOnClickListener(v -> sendMessage());
    }

    private void loadMessages() {
        databaseReference.child(senderId).child(receiverId)
                .addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        messageList.clear();
                        for (DataSnapshot dataSnapshot : snapshot.getChildren()) {
                            Message message = dataSnapshot.getValue(Message.class);
                            if (message != null) {
                                messageList.add(message);
                            }
                        }
                        messageAdapter.notifyDataSetChanged();
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        Toast.makeText(ChatActivity.this, "Failed to load messages", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void sendMessage() {
        String messageText = messageEditText.getText().toString().trim();

        if (messageText.isEmpty()) {
            return;  // Don't send empty messages
        }

        long timestamp = System.currentTimeMillis();

        // Create the message object
        Message message = new Message(senderId, receiverId, messageText, timestamp, receiverUsername);

        // Save to Realtime Database
        databaseReference.child(senderId).child(receiverId).push().setValue(message);
        databaseReference.child(receiverId).child(senderId).push().setValue(message);

        // Clear input
        messageEditText.setText("");

        // Firestore reference
        FirebaseFirestore firestore = FirebaseFirestore.getInstance();

        // Sender's view of the conversation
        Map<String, Object> senderConversation = new HashMap<>();
        senderConversation.put("senderId", senderId);
        senderConversation.put("receiverId", receiverId);
        senderConversation.put("lastMessage", messageText);
        senderConversation.put("timestamp", timestamp);
        senderConversation.put("receiverUsername", receiverUsername); // receiver's username from intent

        // Receiver's view of the conversation
        Map<String, Object> receiverConversation = new HashMap<>();
        receiverConversation.put("senderId", receiverId);
        receiverConversation.put("receiverId", senderId);
        receiverConversation.put("lastMessage", messageText);
        receiverConversation.put("timestamp", timestamp);
        receiverConversation.put("receiverUsername", "You"); // or sender's username if known

        // Create a unique doc ID format: senderId_receiverId
        String senderDocId = senderId + "_" + receiverId;
        String receiverDocId = receiverId + "_" + senderId;

        // Save/update conversation for sender
        firestore.collection("conversations")
                .document(senderDocId)
                .set(senderConversation)
                .addOnSuccessListener(unused -> Log.d("ChatActivity", "Sender conversation updated"))
                .addOnFailureListener(e -> Log.e("ChatActivity", "Failed to save sender conversation", e));

        // Save/update conversation for receiver
        firestore.collection("conversations")
                .document(receiverDocId)
                .set(receiverConversation)
                .addOnSuccessListener(unused -> Log.d("ChatActivity", "Receiver conversation updated"))
                .addOnFailureListener(e -> Log.e("ChatActivity", "Failed to save receiver conversation", e));
    }
}
