// File: app/src/main/java/com/example/mychat/MainActivity.java
package com.example.mychat;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.format.DateFormat;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.firebase.ui.auth.AuthUI;
import com.firebase.ui.database.FirebaseListAdapter;
import com.firebase.ui.database.FirebaseListOptions;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.util.Collections;

public class MainActivity extends AppCompatActivity {

    // ... (class variables are unchanged)
    private static final int SIGN_IN_REQUEST_CODE = 1;
    private static final int PICK_IMAGE_REQUEST = 2;
    private FirebaseListAdapter<ChatMessage> adapter;
    private static final int VIEW_TYPE_MESSAGE_SENT = 0;
    private static final int VIEW_TYPE_MESSAGE_RECEIVED = 1;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // ... (sign-in logic is unchanged)
        if (FirebaseAuth.getInstance().getCurrentUser() == null) {
            startActivityForResult(AuthUI.getInstance().createSignInIntentBuilder()
                    .setAvailableProviders(Collections.singletonList(new AuthUI.IdpConfig.EmailBuilder().build()))
                    .build(), SIGN_IN_REQUEST_CODE);
        } else {
            Toast.makeText(this, "Welcome " + getDisplayName(), Toast.LENGTH_LONG).show();
            displayChatMessages();
        }

        FloatingActionButton fab = findViewById(R.id.fab);
        EditText input = findViewById(R.id.input);
        ImageButton attachButton = findViewById(R.id.attach_button);

        // Text message sending logic
        fab.setOnClickListener(view -> {
            String message = input.getText().toString().trim();
            if (!message.isEmpty()) {
                // *** FIX #1: Use the new factory method for text messages ***
                FirebaseDatabase.getInstance().getReference().push().setValue(
                        ChatMessage.createTextMessage(message, getDisplayName())
                );
                input.setText("");
            }
        });

        // Image message sending logic
        attachButton.setOnClickListener(view -> {
            Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
            intent.setType("image/*");
            startActivityForResult(Intent.createChooser(intent, "Select Picture"), PICK_IMAGE_REQUEST);
        });
    }

    // ... (displayChatMessages method is unchanged)
    private void displayChatMessages() {
        ListView listOfMessages = findViewById(R.id.list_of_messages);

        Query query = FirebaseDatabase.getInstance().getReference();
        FirebaseListOptions<ChatMessage> options = new FirebaseListOptions.Builder<ChatMessage>()
                .setQuery(query, ChatMessage.class)
                .setLayout(R.layout.message) // Dummy layout
                .build();

        adapter = new FirebaseListAdapter<ChatMessage>(options) {

            @Override
            public int getViewTypeCount() {
                return 2;
            }

            @Override
            public int getItemViewType(int position) {
                // Ensure current user and display name are not null to prevent crashes
                if (FirebaseAuth.getInstance().getCurrentUser() != null &&
                        getItem(position).getMessageUser().equals(getDisplayName())) {
                    return VIEW_TYPE_MESSAGE_SENT;
                }
                return VIEW_TYPE_MESSAGE_RECEIVED;
            }

            @Override
            public View getView(int position, View convertView, ViewGroup parent) {
                int viewType = getItemViewType(position);
                View view = convertView;

                if (view == null || (int)view.getTag() != viewType) {
                    if (viewType == VIEW_TYPE_MESSAGE_SENT) {
                        view = getLayoutInflater().inflate(R.layout.item_chat_outgoing, parent, false);
                    } else {
                        view = getLayoutInflater().inflate(R.layout.item_chat_incoming, parent, false);
                    }
                    view.setTag(viewType);
                }

                ChatMessage model = getItem(position);

                // Logic to handle both text and image messages
                TextView messageText = view.findViewById(R.id.message_text);
                ImageView messageImage = view.findViewById(R.id.message_image);
                TextView messageTime = view.findViewById(R.id.message_time);

                if (model.getMessageImageUrl() != null) {
                    // This is an image message
                    messageText.setVisibility(View.GONE);
                    messageImage.setVisibility(View.VISIBLE);
                    Glide.with(MainActivity.this)
                            .load(model.getMessageImageUrl())
                            .into(messageImage);
                } else {
                    // This is a text message
                    messageText.setVisibility(View.VISIBLE);
                    messageImage.setVisibility(View.GONE);
                    messageText.setText(model.getMessageText());
                }

                messageTime.setText(DateFormat.format("h:mm a", model.getMessageTime()));

                if (viewType == VIEW_TYPE_MESSAGE_RECEIVED) {
                    TextView messageUser = view.findViewById(R.id.message_user);
                    messageUser.setText(model.getMessageUser());
                }

                return view;
            }

            @Override
            protected void populateView(@NonNull View v, @NonNull ChatMessage model, int position) {
                // This method is correctly empty because we've moved all the logic to getView().
            }
        };

        listOfMessages.setTranscriptMode(ListView.TRANSCRIPT_MODE_ALWAYS_SCROLL);
        listOfMessages.setStackFromBottom(true);
        listOfMessages.setAdapter(adapter);
    }

    // ... (getDisplayName method is unchanged)
    private String getDisplayName() {
        if (FirebaseAuth.getInstance().getCurrentUser() == null) {
            return "Anonymous";
        }
        String displayName = FirebaseAuth.getInstance().getCurrentUser().getDisplayName();
        return (displayName == null || displayName.trim().isEmpty()) ? "Anonymous" : displayName;
    }

    private void uploadImageToFirebase(Uri imageUri) {
        final StorageReference photoRef = FirebaseStorage.getInstance().getReference().child("chat_photos/" + System.currentTimeMillis());
        Toast.makeText(this, "Uploading image...", Toast.LENGTH_SHORT).show();

        photoRef.putFile(imageUri).continueWithTask(task -> {
            if (!task.isSuccessful()) {
                throw task.getException();
            }
            return photoRef.getDownloadUrl();
        }).addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                Uri downloadUri = task.getResult();
                // *** FIX #2: Use the new factory method for image messages ***
                FirebaseDatabase.getInstance().getReference().push().setValue(
                        ChatMessage.createImageMessage(getDisplayName(), downloadUri.toString())
                );
                Toast.makeText(MainActivity.this, "Image sent!", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(MainActivity.this, "Upload failed: " + task.getException().getMessage(), Toast.LENGTH_LONG).show();
            }
        });
    }

    // ... (onStart, onStop, onActivityResult, onCreateOptionsMenu, onOptionsItemSelected are unchanged)
    @Override
    protected void onStart() {
        super.onStart();
        if (adapter != null) adapter.startListening();
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (adapter != null) adapter.stopListening();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == SIGN_IN_REQUEST_CODE) {
            if (resultCode == RESULT_OK) {
                Toast.makeText(this, "Successfully signed in. Welcome!", Toast.LENGTH_LONG).show();
                displayChatMessages();
            } else {
                Toast.makeText(this, "We couldn't sign you in. Please try again later.", Toast.LENGTH_LONG).show();
                finish();
            }
        }
        // Handle the result of the image picker
        else if (requestCode == PICK_IMAGE_REQUEST && resultCode == RESULT_OK && data != null && data.getData() != null) {
            uploadImageToFirebase(data.getData());
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.menu_sign_out) {
            AuthUI.getInstance().signOut(this).addOnCompleteListener(task -> {
                Toast.makeText(MainActivity.this, "You have been signed out.", Toast.LENGTH_LONG).show();
                finish();
            });
        }
        return super.onOptionsItemSelected(item);
    }
}
