package com.example.w_people.activities;

import android.os.Bundle;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.example.w_people.R;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.DocumentReference;

public class SettingsActivity extends AppCompatActivity {

    LinearLayout layoutChangeUsername, layoutChangeEmail, layoutChangePassword, layoutLogout;
    FirebaseFirestore db;
    FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        // Initialize Firebase
        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();

        // Toolbar
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle("Settings");
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        // Initialize layout elements
        layoutChangeUsername = findViewById(R.id.layoutChangeUsername);
        layoutChangeEmail = findViewById(R.id.layoutChangeEmail);
        layoutChangePassword = findViewById(R.id.layoutChangePassword);
        layoutLogout = findViewById(R.id.layoutLogout);

        // Set click listeners for each setting
        layoutChangeUsername.setOnClickListener(v -> showChangeUsernameDialog());
        layoutChangeEmail.setOnClickListener(v -> showChangeEmailDialog());
        layoutChangePassword.setOnClickListener(v -> showChangePasswordDialog());
        layoutLogout.setOnClickListener(v -> logOut());
    }

    private void showChangeUsernameDialog() {
        EditText input = new EditText(this);
        input.setHint("Enter new username");

        new AlertDialog.Builder(this)
                .setTitle("Change Username")
                .setView(input)
                .setPositiveButton("Save", (dialog, which) -> {
                    String newUsername = input.getText().toString().trim();
                    if (!newUsername.isEmpty()) {
                        // Get the current user
                        String userId = mAuth.getCurrentUser().getUid();

                        // Reference to the user's document in Firestore
                        DocumentReference userRef = db.collection("users").document(userId);

                        // Update the username field in Firestore
                        userRef.update("username", newUsername)
                                .addOnSuccessListener(aVoid -> {
                                    // Success message
                                    Toast.makeText(this, "Username changed to: " + newUsername, Toast.LENGTH_SHORT).show();
                                })
                                .addOnFailureListener(e -> {
                                    // Error handling
                                    Toast.makeText(this, "Failed to change username", Toast.LENGTH_SHORT).show();
                                });
                    } else {
                        Toast.makeText(this, "Username can't be empty", Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void showChangeEmailDialog() {
        EditText input = new EditText(this);
        input.setHint("Enter new email");

        new AlertDialog.Builder(this)
                .setTitle("Change Email")
                .setView(input)
                .setPositiveButton("Save", (dialog, which) -> {
                    String newEmail = input.getText().toString().trim();
                    if (!newEmail.isEmpty()) {
                        FirebaseUser user = mAuth.getCurrentUser();
                        if (user != null) {
                            user.updateEmail(newEmail)
                                    .addOnSuccessListener(aVoid -> {
                                        // Success message
                                        Toast.makeText(this, "Email changed to: " + newEmail, Toast.LENGTH_SHORT).show();
                                    })
                                    .addOnFailureListener(e -> {
                                        // Error handling
                                        Toast.makeText(this, "Failed to change email", Toast.LENGTH_SHORT).show();
                                    });
                        }
                    } else {
                        Toast.makeText(this, "Email can't be empty", Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void showChangePasswordDialog() {
        EditText input = new EditText(this);
        input.setHint("Enter new password");

        new AlertDialog.Builder(this)
                .setTitle("Change Password")
                .setView(input)
                .setPositiveButton("Save", (dialog, which) -> {
                    String newPassword = input.getText().toString().trim();
                    if (!newPassword.isEmpty()) {
                        FirebaseUser user = mAuth.getCurrentUser();
                        if (user != null) {
                            user.updatePassword(newPassword)
                                    .addOnSuccessListener(aVoid -> {
                                        // Success message
                                        Toast.makeText(this, "Password changed successfully", Toast.LENGTH_SHORT).show();
                                    })
                                    .addOnFailureListener(e -> {
                                        // Error handling
                                        Toast.makeText(this, "Failed to change password", Toast.LENGTH_SHORT).show();
                                    });
                        }
                    } else {
                        Toast.makeText(this, "Password can't be empty", Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void logOut() {
        mAuth.signOut();
        Toast.makeText(this, "Logged out successfully", Toast.LENGTH_SHORT).show();
        // Redirect to login screen or previous activity
        finish();  // Or startActivity(new Intent(SettingsActivity.this, LoginActivity.class));
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }
}
